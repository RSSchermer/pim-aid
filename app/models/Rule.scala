package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(id: Option[RuleID], conditionExpression: String, source: Option[String], note: Option[String])

class Rules(tag: Tag) extends Table[Rule](tag, "RULE") {
  def id = column[RuleID]("id", O.PrimaryKey, O.AutoInc)
  def conditionExpression = column[String]("condition_expression", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def note = column[String]("note", O.Nullable)

  def * = (id.?, conditionExpression, source.?, note.?) <> (Rule.tupled, Rule.unapply)
}

object Rules {
  val all = TableQuery[Rules]
  val variablePattern = """\[([A-Za-z0-9_\-]+)\]""".r

  def list(implicit s: Session) = all.list

  def one(id: RuleID) = all.filter(_.id === id)

  def find(id: RuleID)(implicit s: Session): Option[Rule] = one(id).firstOption

  def findWithSuggestions(id: RuleID)(implicit s: Session): Option[(Rule, List[Suggestion])] = {
    find(id) match {
      case Some(rule) => Some(rule, suggestionsFor(id).list)
      case _ => None
    }
  }

  def suggestionsFor(id: RuleID) = {
    for {
      ((rule, ruleSuggestion), suggestion) <-
        one(id) rightJoin
        TableQuery[RulesSuggestions] on (_.id === _.ruleId) rightJoin
        TableQuery[Suggestions] on (_._2.suggestionId === _.id)
    } yield suggestion
  }

  def expressionTermsRulesFor(id: RuleID) = TableQuery[ExpressionTermsRules].filter(_.ruleId === id)

  def insert(rule: Rule, suggestionIdList: List[SuggestionID])(implicit s: Session) = {
    val ruleId = all returning all.map(_.id) += rule
    createExpressionTermsRules(ruleId, rule.conditionExpression)
    createRulesSuggestions(ruleId, suggestionIdList)
    ruleId
  }

  def update(id: RuleID, rule: Rule, suggestionIdList: List[SuggestionID])(implicit s: Session) = {
    all.filter(_.id === id).map(x => (x.conditionExpression, x.source.?, x.note.?))
      .update((rule.conditionExpression, rule.source, rule.note))

    expressionTermsRulesFor(id).delete
    createExpressionTermsRules(id, rule.conditionExpression)

    suggestionsFor(id).delete
    createRulesSuggestions(id, suggestionIdList)
  }

  def delete(id: RuleID)(implicit s: Session) = {
    expressionTermsRulesFor(id).delete
    suggestionsFor(id).delete
    one(id).delete
  }

  def createExpressionTermsRules(id: RuleID, conditionExpression: String)(implicit s: Session) =
    variablePattern.findAllMatchIn(conditionExpression).foreach(m =>
      TableQuery[ExpressionTermsRules].insert(ExpressionTermRule(ExpressionTermLabel(m group 1), id)))

  def createRulesSuggestions(ruleId: RuleID, suggestionIdList: List[SuggestionID])(implicit session: Session) =
    suggestionIdList.foreach(id => TableQuery[RulesSuggestions].insert(RuleSuggestion(ruleId, id)))
}
