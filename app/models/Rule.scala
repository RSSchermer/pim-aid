package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class Rule(id: Option[Long], conditionExpression: String, source: Option[String], note: Option[String])

class Rules(tag: Tag) extends Table[Rule](tag, "RULE") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def conditionExpression = column[String]("condition_expression", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def note = column[String]("note", O.Nullable)

  def * = (id.?, conditionExpression, source.?, note.?) <> (Rule.tupled, Rule.unapply)
}

object Rules {
  val all = TableQuery[Rules]
  val suggestions = TableQuery[Suggestions]
  val expressionTermsRules = TableQuery[ExpressionTermsRules]
  val variablePattern = """\[([A-Za-z0-9_\-]+)\]""".r

  def list(implicit s: Session) = all.list

  def one(id: Long) = all.filter(_.id === id)

  def find(id: Long)(implicit s: Session): Option[Rule] = one(id).firstOption

  def findWithSuggestions(id: Long)(implicit s: Session): Option[(Rule, List[Suggestion])] = {
    find(id) match {
      case Some(rule) => Some(rule, suggestionsFor(id).list)
      case _ => None
    }
  }

  def suggestionsFor(id: Long) = suggestions.filter(_.ruleId === id)

  def expressionTermsRulesFor(id: Long) = expressionTermsRules.filter(_.ruleId === id)

  def insert(rule: Rule, suggestionList: List[Suggestion])(implicit s: Session) = {
    val ruleId = all returning all.map(_.id) += rule
    createExpressionTermsRules(ruleId, rule.conditionExpression)
    createSuggestions(ruleId, suggestionList)
    ruleId
  }

  def update(id: Long, rule: Rule, suggestionList: List[Suggestion])(implicit s: Session) = {
    all.filter(_.id === id).map(x => (x.conditionExpression, x.source.?, x.note.?))
      .update((rule.conditionExpression, rule.source, rule.note))

    expressionTermsRulesFor(id).delete
    createExpressionTermsRules(id, rule.conditionExpression)

    suggestionsFor(id).delete
    createSuggestions(id, suggestionList)
  }

  def delete(id: Long)(implicit s: Session) = {
    expressionTermsRulesFor(id).delete
    suggestionsFor(id).delete
    one(id).delete
  }

  def createExpressionTermsRules(id: Long, conditionExpression: String)(implicit s: Session) =
    variablePattern.findAllMatchIn(conditionExpression).foreach(m =>
      expressionTermsRules.insert(ExpressionTermRule(m group 1, id)))

  def createSuggestions(ruleId: Long, suggestionList: List[Suggestion])(implicit session: Session) =
    suggestionList.foreach(s => {
      suggestions.insert(Suggestion(s.id, s.text, s.explanatoryNote, Some(ruleId)))
    })
}
