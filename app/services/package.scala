import models.{ContributorInfo, Repo, SortedByNContributions}
import play.api.Logging
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSRequest
import zio.Task

package object services extends Logging {
  implicit val repoReads: Reads[Repo] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "owner" \ "login").read[String]
    )(Repo.apply _)
  implicit val contributorReads: Reads[ContributorInfo] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "contributions").read[Int]
    )(ContributorInfo.apply _)

  private[services] val ParallelReposLimitKey = "github.parallelReposLimit"
  private[services] val BaseUrlKey = "github.baseUrl"
  private[services] val AcceptKey = "github.accept"
  private[services] val UserAgentKey = "github.userAgent"
  private[services] val PerPageKey = "github.perPage"
  private[services] lazy val ContributorsTaskZero = Task.succeed(SortedByNContributions.empty)

  // authentication
  private val ghTokenOpt = sys.env.get("GH_TOKEN")
  private lazy val ghToken = ghTokenOpt.get
  private val anonymousMode = ghTokenOpt.isEmpty || ghToken.isEmpty

  // avoids determining authentication mode for every get
  private[services] val authenticationMode: WSRequest => WSRequest = if (anonymousMode) {
    logger.info("running in anonymous mode")
    identity
  } else {
    logger.info("running in authenticated mode")
    val authHeader = "authorization" -> s"token $ghToken"
    _.addHttpHeaders(authHeader)
  }
}
