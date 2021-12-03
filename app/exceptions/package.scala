import play.api.http.Status.{FORBIDDEN, NOT_FOUND, GATEWAY_TIMEOUT, BAD_GATEWAY}

package object exceptions {
  def ghResponseExceptions(httpCode: Int): String => GhResponseException =
    httpCode match {
      case FORBIDDEN => new Gh403ResponseException(_)
      case NOT_FOUND => _: String => new Gh404ResponseException
      case GATEWAY_TIMEOUT => new Gh504ResponseException(_)
      case BAD_GATEWAY => new Gh502ResponseException(_)
      case _ => new Gh500ResponseException(_)
    }
}
