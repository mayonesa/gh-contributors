package exceptions

import play.api.mvc.Result

abstract class GhResponseException(val result: Result) extends Throwable