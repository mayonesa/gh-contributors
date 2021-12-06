import models.ContributorInfo
import play.api.libs.json.{Json, OWrites}

import scala.concurrent.Future

package object controllers {
  type ContributorsFuture = Future[Vector[ContributorInfo]]
  implicit val contributorWrites: OWrites[ContributorInfo] = Json.writes[ContributorInfo]
}
