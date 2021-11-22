package models

case class ContributorInfo(name: String, contributions: Int) {
  private[models] def tuple = (name, contributions)
}