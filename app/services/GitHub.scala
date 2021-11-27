package services

import exceptions.{Gh404ResponseException, OtherThanGh404ErrorException}
import models.{ContributorInfo, Repo, SortedByNContributions}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class GitHub private[services] (ws: WSClient, baseUrl: String, accept: String, userAgent: String, perPage: String)
                               (implicit ec: ExecutionContext) extends Logging {
  @Inject def this(ws: WSClient, config: Configuration, ec: ExecutionContext) =
    this(ws, config.get[String]("github.baseUrl"), config.get[String]("github.accept"),
      config.get[String]("github.userAgent"), config.get[String]("github.perPage"))(ec)

  private lazy val contributorsFutZero = Future.successful(SortedByNContributions.empty)
  private val acceptHeader = "accept" -> accept
  private val userAgentHeader = "user-agent" -> userAgent

  implicit private val repoReads: Reads[Repo] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "owner" \ "login").read[String]
    )(Repo.apply _)
  implicit private val contributorReads: Reads[ContributorInfo] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    )(ContributorInfo.apply _)

  def contributorsByNCommits(orgName: String): Future[Vector[ContributorInfo]] =
    for {
      repos <- repos(orgName)
      orgContributors <- contributorsByNCommits(repos)
    } yield orgContributors.sortedContributors

  private def contributorsByNCommits(repos: Vector[Repo]) = {
    // this will continue going through all the repos even after one fails
    repos.foldLeft(contributorsFutZero) { (accFut, repo) =>
      for {
        repoContributors <- contributorsByNCommits(repo)
        acc <- accFut
      } yield acc ++ repoContributors
    }
  }

  private def repos(orgName: String) =
    get(s"/orgs/$orgName/repos")(_.validate[Vector[Repo]] match {
      case JsSuccess(repos, _) => Future.successful(repos)
      case e: JsError => handleDeserializationError(e, "Repos")
    })

  private def contributorsByNCommits(repo: Repo) =
    get(s"/repos/${repo.owner}/${repo.name}/contributors")(_.validate[Vector[ContributorInfo]] match {
      case JsSuccess(contributors, _) => Future.successful(contributors)
      case e: JsError => handleDeserializationError(e, "Contributor infos")
    }).map(new SortedByNContributions(_))

  private def get[T](urlSuffix: String)(f: JsValue => Future[Vector[T]]) = {
    def loop(page: Int, acc: Vector[T]): Future[Vector[T]] = {
      val url = s"$baseUrl$urlSuffix"
      val baseRequest = ws.url(url)
        .withQueryStringParameters("per_page" -> perPage, "page" -> page.toString)
        .withRequestTimeout(10000.millis)
      val ghTokenOpt = sys.env.get("GH_TOKEN")
      lazy val ghToken = ghTokenOpt.get
      val request = if (ghTokenOpt.isEmpty || ghToken.isEmpty)
        baseRequest.addHttpHeaders(acceptHeader, userAgentHeader)
      else {
        val authHeader = "authorization" -> s"token $ghToken"
        baseRequest.addHttpHeaders(authHeader, acceptHeader, userAgentHeader)
      }
      val recordsFromPage = request.get().flatMap { resp =>
        val status = resp.status
        if (status == OK) f(resp.json)
        else {
          val ex = if (status == NOT_FOUND) new Gh404ResponseException
          else new OtherThanGh404ErrorException(resp.body)
          logger.error(s"$status on $url: ${ex.getMessage}")
          Future.failed[Vector[T]](ex)
        }
      }
      recordsFromPage.flatMap { xs =>
        if (xs.nonEmpty) {
          val nextPage = loop(page + 1, acc ++ xs)
          nextPage
        } else Future.successful(acc)
      }
    }

    loop(1, Vector.empty)
  }

  private def handleDeserializationError(e: JsError, entity: String) = {
    val errMsg = s"$entity deserialization error: ${JsError.toJson(e)}"
    logger.error(errMsg)
    Future.failed(new Exception(errMsg))
  }
}
