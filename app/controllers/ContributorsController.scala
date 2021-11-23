package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api.mvc._
import play.api.libs.json._
import play.api.cache._
import models.{ContributorInfo, GitHub}

import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param controllerComponents standard controller components
 * @param exec `ExecutionContext` to execute the asynchronous code
 */
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

      // failures at this point will very likely stem from records not being found or user agent not authenticated
      // even if GH does not actually return a 404
      case Failure(ex) => Try(NotFound(ex.getMessage))
    }
  }
}
