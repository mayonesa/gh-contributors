package exceptions

import org.scalatestplus.play._
import play.api.http.Status.{BAD_GATEWAY, FORBIDDEN, GATEWAY_TIMEOUT, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NOT_FOUND}

class packageSpec extends PlaySpec {
  "gh response exceptions" must {
    val msg = "msg"
    def test(status: Int, ghEx: GhResponseException) =
      ghResponseExceptions(status)(msg) mustBe ghEx
    "403" in {
      test(FORBIDDEN, Gh403ResponseException(msg))
    }
    "404" in {
      test(NOT_FOUND, Gh404ResponseException())
    }
    "500" in {
      test(INTERNAL_SERVER_ERROR, Gh500ResponseException(msg))
    }
    "502" in {
      test(BAD_GATEWAY, Gh502ResponseException(msg))
    }
    "504" in {
      test(GATEWAY_TIMEOUT, Gh504ResponseException(msg))
    }
    "418 (off-script should be internal server error)" in {
      test(IM_A_TEAPOT, Gh500ResponseException(msg))
    }
  }
}
