package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class RuleID(value: Long) extends MappedTo[Long]

case class Rule(id: Option[RuleID], name: String, conditionExpression: String, source: Option[String], note: Option[String])

class Rules(tag: Tag) extends Table[Rule](tag, "RULES") {
  def id = column[RuleID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def conditionExpression = column[String]("condition_expression", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def note = column[String]("note", O.Nullable)

  def * = (id.?, name, conditionExpression, source.?, note.?) <> (Rule.tupled, Rule.unapply)

  def nameIndex = index("RULES_NAME_INDEX", name, unique = true)
}

object Rules {
  val all = TableQuery[Rules]
  val variablePattern = """\[([A-Za-z0-9_\-]+)\]""".r

  def list(implicit s: Session) = all.list

  def one(id: RuleID) = all.filter(_.id === id)

  def find(id: RuleID)(implicit s: Session): Option[Rule] = one(id).firstOption

  def rulesSuggestionTemplatesFor(id: RuleID) = {
    for {
      (_, ruleSuggestionTemplate) <-
        one(id) innerJoin
        TableQuery[RulesSuggestionTemplates] on (_.id === _.ruleId)
    } yield ruleSuggestionTemplate
  }

  def suggestionTemplatesFor(id: RuleID) = {
    for {
      (_, suggestion) <-
        rulesSuggestionTemplatesFor(id) innerJoin
        TableQuery[SuggestionTemplates] on (_.suggestionTemplateId === _.id)
    } yield suggestion
  }

  def suggestionTemplateListFor(id: RuleID)(implicit s: Session): List[SuggestionTemplate] =
    suggestionTemplatesFor(id).list

  def expressionTermsRulesFor(id: RuleID) = TableQuery[ExpressionTermsRules].filter(_.ruleId === id)

  def insert(rule: Rule)(implicit s: Session): RuleID = {
    val ruleId = all returning all.map(_.id) += rule
    createExpressionTermsRules(ruleId, rule.conditionExpression)
    ruleId
  }

  def update(id: RuleID, rule: Rule)(implicit s: Session) = {
    all.filter(_.id === id)
      .map(x => (x.name, x.conditionExpression, x.source.?, x.note.?))
      .update((rule.name, rule.conditionExpression, rule.source, rule.note))

    expressionTermsRulesFor(id).delete
    createExpressionTermsRules(id, rule.conditionExpression)
  }

  def delete(id: RuleID)(implicit s: Session) = {
    expressionTermsRulesFor(id).delete
    rulesSuggestionTemplatesFor(id).delete
    one(id).delete
  }

  def createExpressionTermsRules(id: RuleID, conditionExpression: String)(implicit s: Session) =
    variablePattern.findAllMatchIn(conditionExpression).foreach(m =>
      TableQuery[ExpressionTermsRules].insert(ExpressionTermRule(m group 1, id)))
}
