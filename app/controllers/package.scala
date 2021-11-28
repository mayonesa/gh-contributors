import models.ContributorInfo

import scala.concurrent.Future

package object controllers {
  type ContributorsFuture = Future[Vector[ContributorInfo]]
}
