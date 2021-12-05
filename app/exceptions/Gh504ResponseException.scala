package exceptions

import play.api.mvc.Results.GatewayTimeout

case class Gh504ResponseException(msg: String) extends GhResponseException(GatewayTimeout(msg))