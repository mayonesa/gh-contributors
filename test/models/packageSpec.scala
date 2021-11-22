package models

import org.scalatestplus.play._

class packageSpec extends PlaySpec {
  "sortedInsert" must {
    val empty = Vector.empty[Int]
    val descBy1 = Vector(5, 4, 3, 2, 1)

    "list is empty" in {
      descInsert(empty, 5) mustBe Vector(5)
    }
    "descending by 1" in {
      val two = 2
      descInsert(Vector(5, 4, 3, 1), two) mustBe descBy1
    }
    "descending by 1 on empty list" in {
      Seq(2, 1, 5, 4, 3).foldLeft(empty) { (acc, int) =>
        descInsert(acc, int)
      } mustBe descBy1
    }
  }
}
