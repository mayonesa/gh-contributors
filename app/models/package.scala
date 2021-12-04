import scala.annotation.tailrec

package object models {
  // time: O(log n)
  def descInsert[T](sortedV: Vector[T], t: T)(implicit ord: Ordering[T]): Vector[T] = {
    @tailrec
    def loop(s: Int, e: Int): Int =
      if (s >= e)
        if (ord.gt(t, sortedV(s))) s
        else s + 1
      else {
        val m = (s + e) / 2
        val vm = sortedV(m)
        if (ord.equiv(t, vm)) m
        else if (ord.gt(t, vm)) loop(s, m - 1)
        else loop(m + 1, e)
      }

    if (sortedV.isEmpty)
      Vector(t)
    else {
      val insertIdx = loop(0, sortedV.size - 1)
      val (prefix, suffix) = sortedV.splitAt(insertIdx)
      (prefix :+ t) ++ suffix
    }
  }
}
