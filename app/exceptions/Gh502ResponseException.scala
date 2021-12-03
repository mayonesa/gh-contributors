package exceptions

import play.api.mvc.Results.BadGateway

class Gh502ResponseException(msg: String) extends GhResponseException(BadGateway(msg))