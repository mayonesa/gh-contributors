import models.ContributorInfo

import scala.concurrent.Future
import scala.concurrent.duration.Duration

package object controllers {
  type ContributorsFuture = Future[Vector[ContributorInfo]]
}
