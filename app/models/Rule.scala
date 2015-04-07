package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(
    id: Option[RuleID],
    name: String,
    conditionExpression: ConditionExpression,
    source: Option[String],
    formalizationReference: Option[String],
    note: Option[String])(implicit includes: Includes[Rule])
  extends Entity[Rule, RuleID]
{
  val suggestionTemplates = many(Rule.suggestionTemplates)
}

object Rule extends EntityCompanion[Rules, Rule, RuleID] {
  val query = TableQuery[Rules]

  val suggestionTemplates = toManyThrough[SuggestionTemplates, RulesSuggestionTemplates, SuggestionTemplate](
    TableQuery[RulesSuggestionTemplates] leftJoin TableQuery[SuggestionTemplates] on(_.suggestionTemplateId === _.id),
    _.id === _._1.ruleId)

  override protected def afterSave(ruleId: RuleID, rule: Rule)(implicit s: Session): Unit = {
    val tableQuery = TableQuery[ExpressionTermsRules]
    tableQuery.filter(_.ruleId === ruleId).delete
    rule.conditionExpression.expressionTerms.foreach(t => tableQuery.insert((t.id.get, ruleId)))
  }
}
