package models

import org.scalatestplus.play._

class SortedByNContributionsSpec extends PlaySpec {
  private val Pepe = "pepe"
  private val Tito = "tito"
  private val PepeCommits0 = 33
  private val PepeCommits1 = 56
  private val TitoInfo = ContributorInfo(Tito, 20)
  private val ChuloInfo = ContributorInfo("chulo", 62)
  private val Contributors0 = new SortedByNContributions(Vector(ChuloInfo, ContributorInfo(Pepe, PepeCommits0),
    TitoInfo))
  private val Pedro = "pedro"
  private val PedroCommits = 30
  private val PedroInfo = ContributorInfo(Pedro, PedroCommits)
  private val MistinInfo = ContributorInfo("mistin", 23)
  private val PepeInfo1: ContributorInfo = ContributorInfo(Pepe, PepeCommits1)
  private val Contributors1 = new SortedByNContributions(Vector(PepeInfo1, PedroInfo,
    MistinInfo))

  "++" must {
    "both lists empty" in {
      val empty = SortedByNContributions.empty
      (empty ++ empty) mustBe empty
    }
    "right list when left is empty" in {
      (SortedByNContributions.empty ++ Contributors0) mustBe Contributors0
    }
    "left list when right is empty" in {
      (Contributors0 ++ SortedByNContributions.empty) mustBe Contributors0
    }
    "sort and add name collisions" in {
      (Contributors0 ++ Contributors1) mustBe new SortedByNContributions(Vector(
        ContributorInfo(Pepe, PepeCommits0 + PepeCommits1), ChuloInfo, PedroInfo, MistinInfo, TitoInfo))
    }
    "sort and add same contributor w/ same number of contributions" in {
      val contributors0 = new SortedByNContributions(Vector(ChuloInfo, PedroInfo, TitoInfo))
      (contributors0 ++ Contributors1) mustBe new SortedByNContributions(Vector(
        ChuloInfo, ContributorInfo(Pedro, PedroCommits * 2), PepeInfo1, MistinInfo, TitoInfo))
    }
    "sort and add arithmetic-sequence (w/ difference of 1) contributions" in {
      val fiveN = "five"
      val fiveC = 5
      val fourN = "four"
      val fourC = 4
      val threeN = "three"
      val threeC = 3
      val twoN = "two"
      val twoC = 2
      val oneN = "one"
      val oneC = 1
      val contributors = new SortedByNContributions(Vector(ContributorInfo(fiveN, fiveC), ContributorInfo(fourN, fourC),
        ContributorInfo(threeN, threeC), ContributorInfo(twoN, twoC), ContributorInfo(oneN, oneC)))
      (contributors ++ contributors ++ contributors) mustBe new SortedByNContributions(Vector(
        ContributorInfo(fiveN, fiveC * 3), ContributorInfo(fourN, fourC * 3), ContributorInfo(threeN, threeC * 3),
        ContributorInfo(twoN, twoC * 3), ContributorInfo(oneN, oneC * 3)))
    }
  }
}
