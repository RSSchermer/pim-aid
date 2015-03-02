package models

import models.Profile._
import models.Profile.driver.simple._

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(
    id: Option[RuleID],
    name: String,
    conditionExpression: ConditionExpression,
    source: Option[String],
    note: Option[String])(implicit includes: Includes[Rule])
  extends Entity[Rule]
{
  type IdType = RuleID

  val suggestionTemplates = many(Rule.suggestionTemplates)
}

object Rule extends EntityCompanion[Rules, Rule] {
  val query = TableQuery[Rules]

  val suggestionTemplates = toManyThrough[SuggestionTemplates, RulesSuggestionTemplates, SuggestionTemplate](
    TableQuery[RulesSuggestionTemplates] leftJoin TableQuery[SuggestionTemplates] on(_.suggestionTemplateId === _.id),
    _.id === _._1.ruleId)

  override protected def afterSave(ruleId: RuleID, rule: Rule)(implicit s: Session): Unit = {
    val etr = TableQuery[ExpressionTermsRules]
    etr.filter(_.ruleId === ruleId).delete
    rule.conditionExpression.expressionTerms.foreach(t => etr.insert((t.id.get, ruleId)))
  }
}
