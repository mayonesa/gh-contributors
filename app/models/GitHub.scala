package models

import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHub @Inject()(ws: WSClient)(implicit exec: ExecutionContext) extends Logging {
  private lazy val contributorsFutZero = Future.successful(SortedByNContributions.empty)
  private val baseUrl = "https://developer.github.com/v3/"
  private val auth = "Authorization" -> s"token ${sys.env("GH_TOKEN")}"
  private val acceptGhJson = "Accept" -> "application/vnd.github.v3+json"

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
      orgContributors <- repos.foldLeft(contributorsFutZero) { (accFut, repo) =>
        for {
          repoContributors <- contributorsByNCommits(repo)
          acc <- accFut
        } yield acc ++ repoContributors
      }
    } yield orgContributors.sortedContributors

  def repos(orgName: String): Future[Vector[Repo]] =
    get(s"orgs/$orgName/repos")(_.validate[Vector[Repo]] match {
      case JsSuccess(repos, _) => repos
      case e: JsError =>
        logDeserializationError(e, "Repos")
        Vector()
    })

  def contributorsByNCommits(repo: Repo): Future[SortedByNContributions] =
    get(s"repos/${repo.owner}/${repo.name}/contributors")(_.validate[Vector[ContributorInfo]] match {
      case JsSuccess(contributors, _) => new SortedByNContributions(contributors)
      case e: JsError =>
        logDeserializationError(e, "Contributor infos")
        SortedByNContributions.empty
    })


  private def get[T](url: String)(f: JsValue => T) =
    ws.url(s"$baseUrl$url")
      .addHttpHeaders(auth, acceptGhJson)
      .get.map(resp => f(resp.json))

  private def logDeserializationError(e: JsError, entity: String): Unit = {
    logger.error(s"$entity deserialization error: ${JsError.toJson(e)}")
  }
}
