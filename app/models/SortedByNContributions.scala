package models

import scala.annotation.tailrec

import cats.instances.map._ // for Monoid
import cats.syntax.semigroup._ // for |+|

class SortedByNContributions private[models] (val sortedContributors: Seq[ContributorInfo]) {
  // exploits pre-sorted condition of the data structure (`Map.sortBy` does not). time complexity: O(mn)
  def ++ (that: SortedByNContributions): SortedByNContributions = {
    @tailrec
    def loop(l: Seq[ContributorInfo],
             r: Seq[ContributorInfo],
             acc: Seq[ContributorInfo],
             record: Map[String, Int],
             dupes: Map[String, Int]): (Seq[ContributorInfo], Map[String, Int]) =
      if (l.isEmpty && r.isEmpty) (acc, dupes)
      else {
        lazy val rh = r.head
        lazy val rn = rh.name
        lazy val rc = rh.contributions
        lazy val lh = l.head
        lazy val lc = lh.contributions
        lazy val ln = lh.name

        if (l.isEmpty || (r.nonEmpty && lc > rc))
          if (record.contains(rn)) loop(l, r.tail, acc, record, dupes |+| Map(rn -> rc))
          else loop(l, r.tail, acc :+ rh, record + (rn -> rc), dupes)
        else if (r.isEmpty || lc < rc)
          if (record.contains(ln)) loop(l.tail, r, acc, record, dupes |+| Map(ln -> lc))
          else loop(l.tail, r, acc :+ lh, record + (ln -> lc), dupes)
        else // lc == rc
          if (record.contains(ln) || ln == rn)
            loop(l.tail, r.tail, acc, record, dupes |+| Map(ln -> lc) |+| Map(rn -> rc))
          else loop(l.tail, r.tail, acc :+ lh :+ rh, record + (ln -> lc), dupes)
      }

    val (missingDupes, dupes) = loop(sortedContributors, that.sortedContributors, Seq.empty, Map.empty, Map.empty)
    new SortedByNContributions(merge(missingDupes, dupes))
  }

  private def merge(sortedByNCommits: Seq[ContributorInfo], xtras: Map[String, Int]) =
    xtras.foldLeft(sortedByNCommits)(insert)

  private def insert(xtra: (String, Int), sortedByNContributions: Seq[ContributorInfo]) = {
    val (name, nContributions) = xtra

    // inclusive indices
    @tailrec
    def loop(start: Int, end: Int): Int =
      if (start >= end) end
      else {
        val iContributions = sortedByNContributions(start).contributions
        if (iContributions >= nContributions) start
        else loop()
      }

    if (sortedByNContributions.nonEmpty) {
      val i = loop(0, sortedByNContributions.length - 1)
      val (prefix, suffix) = sortedByNContributions.splitAt(i)
      (prefix :+ ContributorInfo(name, nContributions)) ++ suffix
    } else sortedByNContributions
  }

  override def hashCode: Int = sortedContributors.hashCode

  override def equals(obj: Any): Boolean =
    sortedContributors == obj.asInstanceOf[SortedByNContributions].sortedContributors

  override def toString: String = s"SortedByNContributions(${sortedContributors.mkString(", ")})"
}

object SortedByNContributions {
  def empty: SortedByNContributions = new SortedByNContributions(Seq.empty)
}
