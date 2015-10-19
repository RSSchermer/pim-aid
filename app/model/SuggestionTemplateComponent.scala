package model

import entitytled.Entitytled

trait SuggestionTemplateComponent {
  self: Entitytled
    with RuleComponent
  =>

  import driver.api._

  case class SuggestionTemplateID(value: Long) extends MappedTo[Long]

  case class SuggestionTemplate(
      id: Option[SuggestionTemplateID],
      name: String,
      text: String,
      explanatoryNote: Option[String]
  )(implicit includes: Includes[SuggestionTemplate]) extends Entity[SuggestionTemplate, SuggestionTemplateID] {
    val rules = many(SuggestionTemplate.rules)
  }

  object SuggestionTemplate extends EntityCompanion[SuggestionTemplates, SuggestionTemplate, SuggestionTemplateID] {
    val rules = toManyThrough[Rules, RulesSuggestionTemplates, Rule]
  }

  class SuggestionTemplates(tag: Tag)
    extends EntityTable[SuggestionTemplate, SuggestionTemplateID](tag, "SUGGESTION_TEMPLATES")
  {
    def id = column[SuggestionTemplateID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def text = column[String]("text")
    def explanatoryNote = column[Option[String]]("explanatory_note")

    def * = (id.?, name, text, explanatoryNote) <>
      ((SuggestionTemplate.apply _).tupled, SuggestionTemplate.unapply)

    def nameIndex = index("SUGGESTION_TEMPLATES_NAME_INDEX", name, unique = true)
  }
}