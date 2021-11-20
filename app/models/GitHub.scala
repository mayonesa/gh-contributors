package models

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class GitHub @Inject()() {
  def contributorsByNCommits(orgName: String): Future[Seq[ContributorInfo]] =
    repos(orgName).map(_.foldLeft(Future.successful(SortedByNContributions.empty)) { case (accFut, repo) =>
      for {
        conts <- contributors(repo)
        acc <- accFut
      } yield acc ++ conts
    }).map(_.descCommits)

  def repos(orgName: String): Future[Seq[Repo]] = ???

  def contributors(repo: Repo): Future[SortedByNContributions] = ???
}
