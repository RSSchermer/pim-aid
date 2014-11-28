package models

import play.api.db.slick.Config.driver.simple._

case class ExpressionTermRule(expressionTermLabel: ExpressionTermLabel, ruleId: RuleID)

class ExpressionTermsRules(tag: Tag) extends Table[ExpressionTermRule](tag, "EXPRESSION_TERMS_RULES"){
  def expressionTermLabel = column[ExpressionTermLabel]("expression_term_label")
  def ruleId = column[RuleID]("rule_id")

  def * = (expressionTermLabel, ruleId) <> (ExpressionTermRule.tupled, ExpressionTermRule.unapply)

  def pk = primaryKey("EXPRESSION_TERMS_RULES_PK", (expressionTermLabel, ruleId))
  def expressionTerm = foreignKey("EXPRESSION_TERMS_RULES_EXPRESSION_TERM_FK", expressionTermLabel,
    TableQuery[ExpressionTerms])(_.label)
  def rule = foreignKey("EXPRESSION_TERMS_RULES_RULE_FK", ruleId, TableQuery[Rules])(_.id)
}
