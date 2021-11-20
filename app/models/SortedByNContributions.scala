package models

import cats.instances.map._ // for Monoid
import cats.syntax.semigroup._ // for |+|

class SortedByNContributions private(private val contributors: Map[String, Int]) {
  def ++ (that: SortedByNContributions): SortedByNContributions = new SortedByNContributions(contributors |+| that.contributors)
  def descCommits: Seq[ContributorInfo] = contributors.toSeq.sortBy(_._2).map((ContributorInfo.apply _).tupled)
}

object SortedByNContributions {
  def empty: SortedByNContributions = new SortedByNContributions(Map.empty)
}
