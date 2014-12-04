package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class SuggestionTemplateID(value: Long) extends MappedTo[Long]

case class SuggestionTemplate(id: Option[SuggestionTemplateID], name: String, text: String,
                              explanatoryNote: Option[String])

class SuggestionTemplates(tag: Tag) extends Table[SuggestionTemplate](tag, "SUGGESTION_TEMPLATES"){
  def id = column[SuggestionTemplateID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def text = column[String]("text", O.NotNull)
  def explanatoryNote = column[String]("explanatory_note", O.Nullable)

  def * = (id.?, name, text, explanatoryNote.?) <> (SuggestionTemplate.tupled, SuggestionTemplate.unapply)

  def nameIndex = index("SUGGESTION_TEMPLATES_NAME_INDEX", name, unique = true)
}

object SuggestionTemplates {
  val all = TableQuery[SuggestionTemplates]

  def list(implicit s: Session) = all.list

  def one(id: SuggestionTemplateID) = all.filter(_.id === id)

  def find(id: SuggestionTemplateID)(implicit s: Session): Option[SuggestionTemplate] = one(id).firstOption

  def insert(suggestionTemplate: SuggestionTemplate)(implicit s: Session): SuggestionTemplateID =
    all returning all.map(_.id) += suggestionTemplate

  def update(id: SuggestionTemplateID, suggestionTemplate: SuggestionTemplate)(implicit s: Session) =
    one(id)
      .map(x => (x.name, x.text, x.explanatoryNote.?))
      .update((suggestionTemplate.name, suggestionTemplate.text, suggestionTemplate.explanatoryNote))

  def delete(id: SuggestionTemplateID)(implicit s: Session) = {
    one(id).delete
  }
}