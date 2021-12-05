package services

import exceptions.ghResponseExceptions
import models._
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.{Configuration, Logging}
import zio.Task
import zio.Runtime.default.unsafeRunToFuture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHub private[services] (ws: WSClient, baseUrl: String, accept: String, userAgent: String, perPage: String)
                               (implicit ec: ExecutionContext) extends Logging {
  @Inject def this(ws: WSClient, config: Configuration, ec: ExecutionContext) =
    this(ws, config.get[String]("github.baseUrl"), config.get[String]("github.accept"),
      config.get[String]("github.userAgent"), config.get[String]("github.perPage"))(ec)

  private lazy val contributorsTaskZero = Task.succeed(SortedByNContributions.empty)
  private val perPageParam = "per_page" -> perPage

  // authentication
  private val ghTokenOpt = sys.env.get("GH_TOKEN")
  private lazy val ghToken = ghTokenOpt.get
  private val anonymousMode = ghTokenOpt.isEmpty || ghToken.isEmpty

  private val acceptHeader = "accept" -> accept
  private val userAgentHeader = "user-agent" -> userAgent

  // avoids determining authentication mode for every get
  private val addHeaders: WSRequest => WSRequest = if (anonymousMode) {
    logger.info("running in anonymous mode")
    _.addHttpHeaders(acceptHeader, userAgentHeader)
  } else {
    logger.info("running in authenticate mode")
    val authHeader = "authorization" -> s"token $ghToken"
    _.addHttpHeaders(authHeader, acceptHeader, userAgentHeader)
  }

  implicit private val repoReads: Reads[Repo] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "owner" \ "login").read[String]
    )(Repo.apply _)
  implicit private val contributorReads: Reads[ContributorInfo] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    )(ContributorInfo.apply _)

  def contributorsByNCommits(orgName: String): Future[Vector[ContributorInfo]] =
    unsafeRunToFuture(for {
      repos <- repos(orgName)
      orgContributors <- contributorsByNCommits(repos)
    } yield orgContributors.sortedContributors)

  private def contributorsByNCommits(repos: Vector[Repo]): Task[SortedByNContributions] = {
    // parallel requests may make GH angry
    // https://docs.github.com/en/rest/overview/resources-in-the-rest-api#secondary-rate-limits
    Task.reduceAll(contributorsTaskZero, repos.map(contributorsByNCommits))(_ ++ _)
  }

  private def repos(orgName: String) =
    get(s"/orgs/$orgName/repos", "Repos")(_.validate[Vector[Repo]])

  private def contributorsByNCommits(repo: Repo) =
    get(s"/repos/${repo.owner}/${repo.name}/contributors", "Contributor infos")(_.validate[Vector[ContributorInfo]])
      .map(new SortedByNContributions(_))

  private def get[T](urlSuffix: String, entity: String)(validate: JsValue => JsResult[Vector[T]]) = {
    val url = s"$baseUrl$urlSuffix"

    def handle(response: WSResponse) = {
      val status = response.status
      val goodResponse = status == OK || status == NO_CONTENT
      if (goodResponse)
        status match {
          case OK => validate(response.json) match {
            case JsSuccess(xs, _) => Task.succeed(xs)
            case e: JsError => handleDeserializationError(e, entity)
          }
          case NO_CONTENT => Task.succeed(Vector.empty)
        }
      else {
        val msg = response.body
        val ex = ghResponseExceptions(status)(msg)
        logger.error(s"$status on $url: $msg")
        Task.fail(ex)
      }
    }

    def loop(page: Int, acc: Vector[T]): Task[Vector[T]] = {
      val request = addHeaders(ws.url(url))
        .withQueryStringParameters(perPageParam, "page" -> page.toString)

      for {
        resp <- Task.fromFuture(implicit ec => request.get())
        recordsFromPage <- handle(resp)
        recordsAcc <- if (recordsFromPage.nonEmpty) {
          val nextPage = loop(page + 1, acc ++ recordsFromPage)
          nextPage
        } else Task.succeed(acc)
      } yield recordsAcc
    }

    loop(1, Vector.empty)
  }

  private def handleDeserializationError(e: JsError, entity: String) = {
    val errMsg = s"$entity deserialization error: ${JsError.toJson(e)}"
    logger.error(errMsg)
    Task.fail(new Exception(errMsg))
  }
}
