package controllers

import exceptions.{Gh404ResponseException, OtherThanGh404ErrorException}
import models.ContributorInfo
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import scala.concurrent.Future

class ContributorsControllerSpec extends PlaySpec with Results {
  import scala.concurrent.ExecutionContext.Implicits.global

  "Contributors REST#byNContributions" should {
    val org0 = "org0"

    "be valid" in {
      val name1 = "name1"
      val name2 = "name2"
      val name3 = "name3"
      val nConts1 = 34
      val nConts2 = 23
      val nConts3 = 12
      val expResp = Json.parse(
        s"""
         [
           {
             "name": "$name1",
             "contributions": $nConts1
           },
           {
             "name": "$name2",
             "contributions": $nConts2
           },
           {
             "name": "$name3",
             "contributions": $nConts3
           }
         ]""")
      val contributorsFut: String => ContributorsFuture = { org =>
        org mustBe org0
        Future.successful(Vector(ContributorInfo(name1, nConts1), ContributorInfo(name2, nConts2),
          ContributorInfo(name3, nConts3)))
      }
      val controller = new ContributorsController(contributorsFut, stubControllerComponents())
      val result = controller.byNContributions(org0)(FakeRequest())
      val bodyText = contentAsString(result)
      Json.parse(bodyText) mustBe expResp
    }

    def not2XX(exception: Exception, errMsg: String) = {
      val contributorsFut: String => ContributorsFuture = { _ =>
        Future.failed(exception)
      }
      val controller = new ContributorsController(contributorsFut, stubControllerComponents())
      val result = controller.byNContributions(org0)(FakeRequest())
      val bodyText = contentAsString(result)
      bodyText mustBe errMsg
    }

    "404 when 404" in {
      not2XX(new Gh404ResponseException, "Record does not exist or user agent not authenticated")
    }
    "500 when not 200 or 404" in {
      val errMsg = "err_msg"
      not2XX(new OtherThanGh404ErrorException(errMsg), errMsg)
    }
  }
}
