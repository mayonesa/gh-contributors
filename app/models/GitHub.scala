package models

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHub @Inject()(implicit exec: ExecutionContext) {
  private val contributorsFutZero = Future.successful(SortedByNContributions.empty)

  def contributorsByNCommits(orgName: String): Future[Seq[ContributorInfo]] =
    for {
      repos <- repos(orgName)
      orgContributors <- repos.foldLeft(contributorsFutZero) { case (accFut, repo) =>
        for {
          repoContributors <- contributorsByNCommits(repo)
          acc <- accFut
        } yield acc ++ repoContributors
      }
    } yield orgContributors.sortedContributors

  def repos(orgName: String): Future[Seq[Repo]] = ???

  def contributorsByNCommits(repo: Repo): Future[SortedByNContributions] = ???
}
