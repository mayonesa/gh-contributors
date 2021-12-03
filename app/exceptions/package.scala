import play.api.http.Status.{FORBIDDEN, NOT_FOUND, GATEWAY_TIMEOUT, BAD_GATEWAY}

package object exceptions {
  val GhResponseExceptions: Map[Int, String => GhResponseException] = Map(
    (FORBIDDEN, new Gh403ResponseException(_)),
    (NOT_FOUND, _ => new Gh404ResponseException),
    (GATEWAY_TIMEOUT, new Gh504ResponseException(_)),
    (BAD_GATEWAY, new Gh502ResponseException(_)),
  )
}
