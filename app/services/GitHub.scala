package services

import exceptions.{Gh404ResponseException, OtherThanGh404ErrorException}
import models.{ContributorInfo, Repo, SortedByNContributions}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.{Configuration, Logging}
import zio.Task
import zio.Runtime.default.unsafeRunToFuture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class GitHub private[services] (ws: WSClient, baseUrl: String, accept: String, userAgent: String, perPage: String)
                               (implicit ec: ExecutionContext) extends Logging {
  @Inject def this(ws: WSClient, config: Configuration, ec: ExecutionContext) =
    this(ws, config.get[String]("github.baseUrl"), config.get[String]("github.accept"),
      config.get[String]("github.userAgent"), config.get[String]("github.perPage"))(ec)

  private val acceptHeader = "accept" -> accept
  private val userAgentHeader = "user-agent" -> userAgent
  private val perPageParam = "per_page" -> perPage

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

  private def contributorsByNCommits(repos: Vector[Repo]): Task[SortedByNContributions] =
    // TODO: parallelize
    Task.foldLeft(repos)(SortedByNContributions.empty) { (acc, repo) =>
      contributorsByNCommits(repo).map(acc ++ _)
    }

  private def repos(orgName: String) =
    get(s"/orgs/$orgName/repos")(_.validate[Vector[Repo]] match {
      case JsSuccess(repos, _) => Task.succeed(repos)
      case e: JsError => handleDeserializationError(e, "Repos")
    })

  private def contributorsByNCommits(repo: Repo) =
    get(s"/repos/${repo.owner}/${repo.name}/contributors")(_.validate[Vector[ContributorInfo]] match {
      case JsSuccess(contributors, _) => Task.succeed(contributors)
      case e: JsError => handleDeserializationError(e, "Contributor infos")
    }).map(new SortedByNContributions(_))

  private def get[T](urlSuffix: String)(f: JsValue => Task[Vector[T]]) = {
    val url = s"$baseUrl$urlSuffix"

    def handle(response: WSResponse) = {
      val status = response.status
      if (status == OK) f(response.json)
      else {
        val ex = if (status == NOT_FOUND) new Gh404ResponseException
        else new OtherThanGh404ErrorException(response.body)
        logger.error(s"$status on $url: ${ex.getMessage}")
        Task.fail(ex)
      }
    }

    def loop(page: Int, acc: Vector[T]): Task[Vector[T]] = {
      val request = addHeaders(ws.url(url))
        .withQueryStringParameters(perPageParam, "page" -> page.toString)
        .withRequestTimeout(10000.millis)

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

  private def addHeaders(baseRequest: WSRequest) = {
    val ghTokenOpt = sys.env.get("GH_TOKEN")
    lazy val ghToken = ghTokenOpt.get
    val anonymousMode = ghTokenOpt.isEmpty || ghToken.isEmpty
    if (anonymousMode)
      baseRequest.addHttpHeaders(acceptHeader, userAgentHeader)
    else {
      val authHeader = "authorization" -> s"token $ghToken"
      baseRequest.addHttpHeaders(authHeader, acceptHeader, userAgentHeader)
    }
  }

  private def handleDeserializationError(e: JsError, entity: String) = {
    val errMsg = s"$entity deserialization error: ${JsError.toJson(e)}"
    logger.error(errMsg)
    Task.fail(new Exception(errMsg))
  }
}
