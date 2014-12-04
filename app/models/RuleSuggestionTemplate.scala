package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class RuleSuggestionTemplate(ruleId: RuleID, suggestionTemplateId: SuggestionTemplateID)

class RulesSuggestionTemplates(tag: Tag) extends Table[RuleSuggestionTemplate](tag, "RULES_SUGGESTION_TEMPLATES") {
  def ruleId = column[RuleID]("rule_id")
  def suggestionTemplateId = column[SuggestionTemplateID]("suggestion_id")

  def * = (ruleId, suggestionTemplateId) <> (RuleSuggestionTemplate.tupled, RuleSuggestionTemplate.unapply)

  def pk = primaryKey("RULES_SUGGESTION_TEMPLATES_PK", (ruleId, suggestionTemplateId))
  def rule = foreignKey("RULES_SUGGESTION_TEMPLATES_RULE_FK", ruleId, TableQuery[Rules])(_.id)
  def suggestionTemplate = foreignKey("RULES_SUGGESTION_TEMPLATES_SUGGESTION_TEMPLATE_FK", suggestionTemplateId,
    TableQuery[SuggestionTemplates])(_.id)
}

object RulesSuggestionTemplates {
  val all = TableQuery[RulesSuggestionTemplates]

  def one(ruleId: RuleID, suggestionTemplateId: SuggestionTemplateID) =
    all.filter(x => x.ruleId === ruleId && x.suggestionTemplateId === suggestionTemplateId)

  def insert(ruleSuggestionTemplate: RuleSuggestionTemplate)(implicit s: Session) =
    all.insert(ruleSuggestionTemplate)

  def delete(ruleId: RuleID, suggestionTemplateId: SuggestionTemplateID)(implicit s: Session) =
    one(ruleId, suggestionTemplateId).delete
}
