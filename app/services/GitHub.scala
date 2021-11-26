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

@Singleton
class GitHub private[services] (ws: WSClient, baseUrl: String, accept: String, userAgent: String, perPage: String)
                               (implicit ec: ExecutionContext) extends Logging {
  @Inject def this(ws: WSClient, config: Configuration, ec: ExecutionContext) =
    this(ws, config.get[String]("github.baseUrl"), config.get[String]("github.accept"),
      config.get[String]("github.userAgent"), config.get[String]("github.perPage"))(ec)

  private lazy val contributorsFutZero = Future.successful(SortedByNContributions.empty)
  private val acceptHeader = "accept" -> accept
  private val userAgentHeader = "user-agent" -> userAgent
  private val queryBase = s"?per_page=$perPage&page="

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

  def repos(orgName: String): Future[Vector[Repo]] =
    get(s"/orgs/$orgName/repos")(_.validate[Vector[Repo]] match {
      case JsSuccess(repos, _) => Future.successful(repos)
      case e: JsError => handleDeserializationError(e, "Repos")
    })

  def contributorsByNCommits(repo: Repo): Future[SortedByNContributions] =
    get(s"/repos/${repo.owner}/${repo.name}/contributors")(_.validate[Vector[ContributorInfo]] match {
      case JsSuccess(contributors, _) => Future.successful(contributors)
      case e: JsError => handleDeserializationError(e, "Contributor infos")
    }).map(new SortedByNContributions(_))

  private def get[T](url: String)(f: JsValue => Future[Vector[T]]) = {
    def loop(page: Int, acc: Vector[T]): Future[Vector[T]] = {
      val baseRequest = ws.url(s"$baseUrl$url$queryBase$page")
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
          logger.error(ex.getMessage)
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

  private def contributorsByNCommits(repos: Vector[Repo]): Future[SortedByNContributions] =
    repos.foldLeft(contributorsFutZero) { (accFut, repo) =>
      for {
        repoContributors <- contributorsByNCommits(repo)
        acc <- accFut
      } yield acc ++ repoContributors
    }

  private def handleDeserializationError(e: JsError, entity: String) = {
    val errMsg = s"$entity deserialization error: ${JsError.toJson(e)}"
    logger.error(errMsg)
    Future.failed(new Exception(errMsg))
  }
}
