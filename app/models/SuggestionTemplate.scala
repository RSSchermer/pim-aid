package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

case class SuggestionTemplateID(value: Long) extends MappedTo[Long]

case class SuggestionTemplate(
    id: Option[SuggestionTemplateID],
    name: String,
    text: String,
    explanatoryNote: Option[String])(implicit includes: Includes[SuggestionTemplate])
  extends Entity[SuggestionTemplate, SuggestionTemplateID]
{
  val rules = many(SuggestionTemplate.rules)
}

object SuggestionTemplate extends EntityCompanion[SuggestionTemplates, SuggestionTemplate, SuggestionTemplateID] {
  val rules = toManyThrough[Rules, RulesSuggestionTemplates, Rule]
}
