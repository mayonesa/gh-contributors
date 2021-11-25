package controllers

import exceptions.Gh404ResponseException

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api.mvc._
import play.api.libs.json._
import play.api.cache._
import models.{ContributorInfo, GitHub}

import scala.util.{Failure, Success, Try}

@Singleton
class ContributorsController @Inject()(gh: GitHub, cache: AsyncCacheApi, val controllerComponents: ControllerComponents)
                                      (implicit exec: ExecutionContext) extends BaseController {
  private implicit val contributorWrites: OWrites[ContributorInfo] = Json.writes[ContributorInfo]

  /**
   * Creates an Action that, given an organization name, returns a list of its contributors sorted by descending number
   * of contributions.
   * Assumption: number of commits per contributor will not exceed `Int.MaxValue` (2,147,483,647)
   */
  def byNContributions(orgName: String): Action[AnyContent] = Action.async {
    // all other caching-potentials are covered by EhCache on the WS client
    val contributorsFut = cache.getOrElseUpdate[Vector[ContributorInfo]](s"contributors.$orgName") {
      gh.contributorsByNCommits(orgName)
    }

    contributorsFut.transform {
      case Success(contributors) => Try(Ok(Json.toJson(contributors)))
      case Failure(ex) =>
        val msg = ex.getMessage
        Try {
          ex match {
            case _: Gh404ResponseException => NotFound(msg)
            case _ => InternalServerError(msg)
          }
        }
    }
  }
}
