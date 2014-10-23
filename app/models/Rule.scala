package models

import play.api.db.slick.Config.driver.simple._

case class Rule(id: Option[Long], conditionExpression: String, source: Option[String], note: Option[String])

class Rules(tag: Tag) extends Table[Rule](tag, "RULE") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def conditionExpression = column[String]("condition_expression", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def note = column[String]("note", O.Nullable)

  def * = (id.?, conditionExpression, source.?, note.?) <> (Rule.tupled, Rule.unapply)
}
