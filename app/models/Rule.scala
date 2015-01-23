package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import play.api.db.slick.Session
import schema._

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(
    id: Option[RuleID],
    name: String,
    conditionExpression: String,
    source: Option[String],
    note: Option[String],
    suggestionTemplates: Many[Rules, SuggestionTemplates, Rule, SuggestionTemplate] =
      ManyFetched(Rule.suggestionTemplates))
  extends Entity { type IdType = RuleID }

object Rule extends EntityCompanion[Rules, Rule] {
  val query = TableQuery[Rules]

  val suggestionTemplates = toManyThrough[SuggestionTemplate, (RuleID, SuggestionTemplateID), SuggestionTemplates, RulesSuggestionTemplates](
    TableQuery[RulesSuggestionTemplates] leftJoin TableQuery[SuggestionTemplates] on(_.suggestionTemplateId === _.id),
    _.id === _._1.ruleId,
    lenser(_.suggestionTemplates)
  )

  override protected def afterSave(ruleId: RuleID, rule: Rule)(implicit s: Session): Unit = {
    val etr = TableQuery[ExpressionTermsRules]
    etr.filter(_.ruleId === ruleId).delete
    """\[([A-Za-z0-9_\-]+)\]""".r.findAllMatchIn(rule.conditionExpression)
      .foreach(m => etr.insert((m group 1, ruleId)))
  }

  override protected def beforeDelete(ruleId: RuleID)(implicit s: Session): Unit =
    TableQuery[ExpressionTermsRules].filter(_.ruleId === ruleId).delete
}
