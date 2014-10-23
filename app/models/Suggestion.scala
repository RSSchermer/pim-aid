package models

import play.api.db.slick.Config.driver.simple._

case class Suggestion(id: Option[Long], text: String, explanatoryNote: Option[String], ruleId: Option[Long])

class Suggestions(tag: Tag) extends Table[Suggestion](tag, "SUGGESTIONS"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def text = column[String]("text", O.NotNull)
  def explanatoryNote = column[String]("explanatory_note", O.Nullable)
  def ruleId = column[Long]("rule_id", O.NotNull)

  def * = (id.?, text, explanatoryNote.?, ruleId.?) <> (Suggestion.tupled, Suggestion.unapply)

  def rule = foreignKey("SUGGESTIONS_RULE_FK", ruleId, TableQuery[Rules])(_.id)
}
