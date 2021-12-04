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
    "random-spacing list" in {
      val v = Vector(39, 23, 23, 19, 18, 1, 0)
      val fifteen = 15
      descInsert(v, fifteen) mustBe Vector(39, 23, 23, 19, 18, fifteen, 1, 0)
    }
    "insert at beginning of list" in {
      val v = Vector(39, 23, 23, 19, 18, 1, 0)
      val hundred = 100
      descInsert(v, hundred) mustBe Vector(hundred, 39, 23, 23, 19, 18, 1, 0)
    }
    "insert right before list" in {
      val v = Vector(39, 23, 23, 19, 18, 1, 0)
      val forty = 40
      descInsert(v, forty) mustBe Vector(forty, 39, 23, 23, 19, 18, 1, 0)
    }
    "insert right after beginning list" in {
      val v = Vector(39, 23, 23, 19, 18, 1, 0)
      val thirty8 = 38
      descInsert(v, thirty8) mustBe Vector(39, thirty8, 23, 23, 19, 18, 1, 0)
    }
    "insert end of list" in {
      val v = Vector(39, 23, 23, 19, 18, 1)
      val zero = 0
      descInsert(v, zero) mustBe Vector(39, 23, 23, 19, 18, 1, 0)
    }
    "insert right before end of list" in {
      val v = Vector(39, 23, 23, 19, 18, 0)
      val one = 1
      descInsert(v, one) mustBe Vector(39, 23, 23, 19, 18, one, 0)
    }
    "insert dupe" in {
      val v = Vector(39, 23, 23, 19, 18, 1, 0)
      val nineteen = 19
      descInsert(v, nineteen) mustBe Vector(39, 23, 23, nineteen, 19, 18, 1, 0)
    }
    "insert dupe w/ adjacents" in {
      val v = Vector(39, 23, 23, 20, 19, 18, 1, 0)
      val nineteen = 19
      descInsert(v, nineteen) mustBe Vector(39, 23, 23, 20, nineteen, 19, 18, 1, 0)
    }
  }
}
