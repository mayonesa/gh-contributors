package exceptions

import play.api.mvc.Results.InternalServerError

case class Gh500ResponseException(msg: String) extends GhResponseException(InternalServerError(msg))
