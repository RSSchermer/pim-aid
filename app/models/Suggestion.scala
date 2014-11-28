package models

import play.api.db.slick.Config.driver.simple._

case class SuggestionID(value: Long) extends MappedTo[Long]

case class Suggestion(id: Option[SuggestionID], text: String, explanatoryNote: Option[String])

class Suggestions(tag: Tag) extends Table[Suggestion](tag, "SUGGESTIONS"){
  def id = column[SuggestionID]("id", O.PrimaryKey, O.AutoInc)
  def text = column[String]("text", O.NotNull)
  def explanatoryNote = column[String]("explanatory_note", O.Nullable)

  def * = (id.?, text, explanatoryNote.?) <> (Suggestion.tupled, Suggestion.unapply)
}
