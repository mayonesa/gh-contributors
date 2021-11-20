package models

import cats.instances.map._ // for Monoid
import cats.syntax.semigroup._ // for |+|

class RepoContributors private (private val contributors: Map[String, Int]) {
  def ++ (that: RepoContributors): RepoContributors = new RepoContributors(contributors |+| that.contributors)
  def descCommits: Seq[ContributorInfo] = contributors.toSeq.sortBy(_._2).map((ContributorInfo.apply _).tupled)
}

object RepoContributors {
  def empty: RepoContributors = new RepoContributors(Map.empty)
}
