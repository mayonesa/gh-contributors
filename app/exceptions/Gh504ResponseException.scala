package exceptions

import play.api.mvc.Results.GatewayTimeout

class Gh504ResponseException(msg: String) extends GhResponseException(GatewayTimeout(msg))