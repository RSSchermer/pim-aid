package models

import models.Profile._
import models.Profile.driver.simple._

case class SuggestionTemplateID(value: Long) extends MappedTo[Long]

case class SuggestionTemplate(
    id: Option[SuggestionTemplateID],
    name: String,
    text: String,
    explanatoryNote: Option[String])(implicit includes: Includes[SuggestionTemplate])
  extends Entity[SuggestionTemplate]
{
  type IdType = SuggestionTemplateID

  val rules = many(SuggestionTemplate.rules)
}

object SuggestionTemplate extends EntityCompanion[SuggestionTemplates, SuggestionTemplate] {
  val query = TableQuery[SuggestionTemplates]

  val rules = toManyThrough[Rules, RulesSuggestionTemplates, Rule](
    TableQuery[RulesSuggestionTemplates] leftJoin TableQuery[Rules] on(_.ruleId === _.id),
    _.id === _._1.suggestionTemplateId)
}
