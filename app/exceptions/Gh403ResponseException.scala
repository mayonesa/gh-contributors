package exceptions

import play.api.mvc.Results.Forbidden

class Gh403ResponseException(msg: String) extends GhResponseException(Forbidden(msg))