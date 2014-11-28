package models

import play.api.db.slick.Config.driver.simple._

case class RuleSuggestion(ruleId: RuleID, suggestionId: SuggestionID)

class RulesSuggestions(tag: Tag) extends Table[RuleSuggestion](tag, "RULES_SUGGESTIONS") {
  def ruleId = column[RuleID]("rule_id")
  def suggestionId = column[SuggestionID]("suggestion_id")

  def * = (ruleId, suggestionId) <> (RuleSuggestion.tupled, RuleSuggestion.unapply)

  def pk = primaryKey("RULES_SUGGESTIONS_PK", (ruleId, suggestionId))
  def rule = foreignKey("RULES_SUGGESTIONS_RULE_FK", ruleId, TableQuery[Rules])(_.id)
  def suggestion = foreignKey("RULES_SUGGESTIONS_SUGGESTION_FK", suggestionId, TableQuery[Suggestions])(_.id)
}
