package services

import exceptions.ghResponseExceptions
import models._
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Configuration, Logging}
import zio.{Task, ZIO}
import zio.Runtime.default.unsafeRunToFuture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHub private[services] (ws: WSClient,
                                parallelReposLimit: Int,
                                baseUrl: String,
                                accept: String,
                                userAgent: String,
                                perPage: String)
                               (implicit ec: ExecutionContext) extends Logging {
  @Inject def this(ws: WSClient, config: Configuration, ec: ExecutionContext) =
    this(ws, config.get[Int](ParallelReposLimitKey), config.get[String](BaseUrlKey), config.get[String](AcceptKey),
      config.get[String](UserAgentKey), config.get[String](PerPageKey))(ec)

  private val perPageParam = "per_page" -> perPage
  private val userAgentHeader = "user-agent" -> userAgent
  private val acceptHeader = "accept" -> accept

  def contributorsByNCommits(orgName: String): Future[Vector[ContributorInfo]] = {
    logger.info("getting contributors for " + orgName)
    unsafeRunToFuture(for {
      repos <- repos(orgName)
      orgContributors <- contributorsByNCommits(repos)
    } yield orgContributors.sortedContributors)
  }

  private def contributorsByNCommits(repos: Vector[Repo]): Task[SortedByNContributions] = {
    val contributorsByRepo = repos.map(contributorsByNCommits)
    logger.info(s"querying ${repos.size} repos")
    // chunking heuristic to speed things up while attempting to work around GH's serving idiosyncrasies (namely, their
    // aversion to parallel requests)
    ZIO.reduceAllParN[Any, Any, Throwable, SortedByNContributions](parallelReposLimit)(ContributorsTaskZero,
      contributorsByRepo)(_ ++ _)
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
      val request = authenticationMode(ws.url(url).addHttpHeaders(userAgentHeader, acceptHeader))
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
