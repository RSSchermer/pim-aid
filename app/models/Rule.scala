package models

import scala.concurrent.ExecutionContext

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

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
  val suggestionTemplates = toManyThrough[SuggestionTemplates, RulesSuggestionTemplates, SuggestionTemplate]

  override protected def afterSave(ruleId: RuleID, rule: Rule)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val tq = TableQuery[ExpressionTermsRules]
    val deleteOld = tq.filter(_.ruleId === ruleId).delete
    val insertNew = for {
      terms <- ExpressionTerm.all.filter(_.label inSetBind rule.conditionExpression.expressionTermLabels).result
    } yield DBIO.sequence(terms.map(t => tq += (t.id.get, ruleId)))

    deleteOld >> insertNew >> DBIO.successful(())
  }
}
