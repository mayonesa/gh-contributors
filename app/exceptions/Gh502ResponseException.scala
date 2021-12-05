package exceptions

import play.api.mvc.Results.BadGateway

case class Gh502ResponseException(msg: String) extends GhResponseException(BadGateway(msg))
