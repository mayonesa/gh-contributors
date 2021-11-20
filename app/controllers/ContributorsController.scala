package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api.mvc._

import models.GitHub

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param controllerComponents standard controller components
 * @param exec `ExecutionContext` to execute the asynchronous code
 */
@Singleton
class ContributorsController @Inject()(gh: GitHub, val controllerComponents: ControllerComponents)
                                      (implicit exec: ExecutionContext) extends BaseController {

  /**
   * Creates an Action that, given an organization name, returns a list of its contributors sorted by descending number
   * of contributions.
   * Assumption: number of commits per contributor will not exceed `Int.MaxValue` (2,147,483,647)
   */
  def byNContributions(orgName: String) = Action.async {
    gh.contributorsByNCommits(orgName).map { msg => Ok(msg) }
  }
}
