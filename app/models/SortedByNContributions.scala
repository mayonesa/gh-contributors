package models

import scala.annotation.tailrec

import cats.instances.map._     // for Monoid
import cats.syntax.semigroup._  // for |+|

import SortedByNContributions.{NoAndAggDupesZero, contributorOrdering}

class SortedByNContributions private[models] (private[models] val sortedContributors: Vector[ContributorInfo]) {
  // exploits pre-sorted condition of the data structure (`Map.sortBy` does not). time complexity: O(mn)
  def ++ (that: SortedByNContributions): SortedByNContributions = {
    // merge lists of sorted contributors into sorted contributors list where dupes are not aggregated.
    @tailrec
    def loop(l0: Vector[ContributorInfo],
             r0: Vector[ContributorInfo],
             acc: Vector[ContributorInfo],
             allNames0: Set[String],
             dupes0: Set[String]): (Vector[ContributorInfo], Set[String]) =
      if (l0.isEmpty && r0.isEmpty) (acc, dupes0)
      else {
        lazy val rh = r0.head
        lazy val rc = rh.contributions
        lazy val lh = l0.head
        lazy val lc = lh.contributions

        def updatedRecords(ci: ContributorInfo) = {
          val nm = ci.name
          val isDupe = allNames0.contains(nm)
          if (isDupe) (allNames0, dupes0 + nm) else (allNames0 + nm, dupes0)
        }

        val rightSourceNext = l0.isEmpty || (r0.nonEmpty && lc < rc)
        val (l, r, ci, allNames, dupes) = if (rightSourceNext) {
          val (allNames, dupes) = updatedRecords(rh)
          (l0, r0.tail, rh, allNames, dupes)
        } else { // left source of sorted contributors is next
          val (allNames, dupes) = updatedRecords(lh)
          (l0.tail, r0, lh, allNames, dupes)
        }

        loop(l, r, acc :+ ci, allNames, dupes)
      }

    val (nonAggregated, dupes) = loop(sortedContributors, that.sortedContributors, Vector.empty, Set.empty, Set.empty)
    new SortedByNContributions(aggregateDupes(nonAggregated, dupes))
  }

  private def aggregateDupes(nonAggSortedByNCommits: Vector[ContributorInfo], dupes: Set[String]) = {
    // remove dupes while creating an aggregated-dupe list
    val (noDupes, aggDupes) = nonAggSortedByNCommits.foldLeft(NoAndAggDupesZero) { case ((noDupes0, aggDupes0), ci) =>
      if (dupes.contains(ci.name)) (noDupes0, aggDupes0 |+| Map(ci.tuple))
      else (noDupes0 :+ ci, aggDupes0)
    }

    // insert aggregated dupes into the no-dupes list
    aggDupes.foldLeft(noDupes)(insert)
  }

  private def insert(sortedByNContributions: Vector[ContributorInfo], xtra: (String, Int)) = {
    val (name, nContributions) = xtra
    val ci = ContributorInfo(name, nContributions)
    descInsert(sortedByNContributions, ci)
  }

  override def hashCode: Int = sortedContributors.hashCode

  override def equals(obj: Any): Boolean =
    sortedContributors == obj.asInstanceOf[SortedByNContributions].sortedContributors

  override def toString: String = s"SortedByNContributions(${sortedContributors.mkString(", ")})"
}

object SortedByNContributions {
  private val NoAndAggDupesZero = (Vector.empty[ContributorInfo], Map.empty[String, Int])

  def empty: SortedByNContributions = new SortedByNContributions(Vector.empty)

  implicit def contributorOrdering: Ordering[ContributorInfo] = Ordering.by(_.contributions)
}
