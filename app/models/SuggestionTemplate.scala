package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import schema._

case class SuggestionTemplateID(value: Long) extends MappedTo[Long]

case class SuggestionTemplate(
    id: Option[SuggestionTemplateID],
    name: String,
    text: String,
    explanatoryNote: Option[String],
    rules: Many[SuggestionTemplates, Rules, SuggestionTemplate, Rule] =
      ManyUnfetched(SuggestionTemplate.rules)) extends Entity[SuggestionTemplateID] {
  type IdType = SuggestionTemplateID
}

object SuggestionTemplate extends EntityCompanion[SuggestionTemplate, SuggestionTemplates] {
  val query = TableQuery[SuggestionTemplates]

  val rules = toManyThrough[Rule, (RuleID, SuggestionTemplateID), Rules, RulesSuggestionTemplates](
    TableQuery[RulesSuggestionTemplates] leftJoin TableQuery[Rules] on(_.ruleId === _.id),
    _.id === _._1.suggestionTemplateId,
    lenser(_.rules)
  )
}
