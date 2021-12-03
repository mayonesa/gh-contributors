package exceptions

import play.api.mvc.Results.InternalServerError

class Gh500ResponseException(msg: String) extends GhResponseException(InternalServerError(msg))