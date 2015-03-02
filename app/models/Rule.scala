package models

import models.Profile._
import models.Profile.driver.simple._

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(
    id: Option[RuleID],
    name: String,
    conditionExpression: String,
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
    """\[([A-Za-z0-9_\-]+)\]""".r.findAllMatchIn(rule.conditionExpression)
      .foreach(m => etr.insert((ExpressionTerm.findByLabel(m group 1).get.id.get, ruleId)))
  }
}
