package exceptions

// per https://docs.github.com/en/rest/overview/resources-in-the-rest-api#authentication,
// 404 is given even if the record were there if the user agent is not authenticated (safety feature)
class Gh404ResponseException extends Exception("Record does not exist or user agent not authenticated")