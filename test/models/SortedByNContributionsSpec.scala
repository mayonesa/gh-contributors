package models

import org.scalatestplus.play._

class SortedByNContributionsSpec extends PlaySpec {
  private val Pepe = "pepe"
  private val Tito = "tito"
  private val PepeCommits0 = 33
  private val PepeCommits1 = 56
  private val TitoInfo = ContributorInfo(Tito, 20)
  private val ChuloInfo = ContributorInfo("chulo", 62)
  private val Contributors0 = new SortedByNContributions(Seq(ChuloInfo, ContributorInfo(Pepe, PepeCommits0), TitoInfo))
  private val Pedro = "pedro"
  private val PedroCommits = 30
  private val PedroInfo = ContributorInfo(Pedro, PedroCommits)
  private val MistinInfo = ContributorInfo("mistin", 23)
  private val PepeInfo1: ContributorInfo = ContributorInfo(Pepe, PepeCommits1)
  private val Contributors1 = new SortedByNContributions(Seq(PepeInfo1, PedroInfo,
    MistinInfo))

  "++" must {
    "right list when left is empty" in {
      (SortedByNContributions.empty ++ Contributors0) mustBe Contributors0
    }
    "left list when right is empty" in {
      (Contributors0 ++ SortedByNContributions.empty) mustBe Contributors0
    }
    "sort and add collisions" in {
      (Contributors0 ++ Contributors1) mustBe new SortedByNContributions(Seq(
        ContributorInfo(Pepe, PepeCommits0 + PepeCommits1), ChuloInfo, PedroInfo, MistinInfo, TitoInfo))
    }
    "sort and add same contributor w/ same number of contributions" in {
      val contributors0 = new SortedByNContributions(Seq(ChuloInfo, PedroInfo, TitoInfo))
      (contributors0 ++ Contributors1) mustBe new SortedByNContributions(Seq(
        ChuloInfo, ContributorInfo(Pedro, PedroCommits * 2), PepeInfo1, MistinInfo, TitoInfo))
    }
  }
}
