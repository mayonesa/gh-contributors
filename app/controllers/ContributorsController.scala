package controllers

import exceptions.GhResponseException

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import play.api.mvc._
import play.api.libs.json._
import play.api.cache._
import services.GitHub

@Singleton
class ContributorsController private[controllers] (contributorsFut: String => ContributorsFuture,
                                                   val controllerComponents: ControllerComponents)
                                                  (implicit exec: ExecutionContext) extends BaseController {
  @Inject def this(gh: GitHub, cache: AsyncCacheApi, cc: ControllerComponents, exec: ExecutionContext) =
    // all other caching-potentials are covered by EhCache on the WS client
    this(orgName =>
      cache.getOrElseUpdate (s"contributors.$orgName")(gh.contributorsByNCommits(orgName)),
      cc)(exec)

  /**
   * Creates an Action that, given an organization name, returns a list of its contributors sorted by descending number
   * of contributions.
   * Assumption: number of commits per contributor will not exceed `Int.MaxValue` (2,147,483,647)
   */
  def byNContributions(orgName: String): Action[AnyContent] = Action.async {
    contributorsFut(orgName).transform {
      case Success(contributors) => Try(Ok(Json.toJson(contributors)))
      case Failure(ex) => Try(ex.asInstanceOf[GhResponseException].result)
    }
  }
}
