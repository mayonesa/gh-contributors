import play.api.http.Status.{FORBIDDEN, NOT_FOUND, GATEWAY_TIMEOUT, BAD_GATEWAY}

package object exceptions {
  def ghResponseExceptions(httpCode: Int): String => GhResponseException =
    httpCode match {
      case FORBIDDEN => Gh403ResponseException
      case NOT_FOUND => _: String => Gh404ResponseException()
      case GATEWAY_TIMEOUT => Gh504ResponseException
      case BAD_GATEWAY => Gh502ResponseException
      case _ => Gh500ResponseException
    }
}
