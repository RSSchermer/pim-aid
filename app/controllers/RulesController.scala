package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

import views._
import models._
import utils._

case class RuleWithSuggestions(id: Option[Long], conditionExpression: String, source: Option[String],
                               note: Option[String], suggestions: Seq[Suggestion])

object RulesController extends Controller {
  val expressionTerms = TableQuery[ExpressionTerms]
  val rules = TableQuery[Rules]
  var suggestions = TableQuery[Suggestions]
  val expressionTermsRules = TableQuery[ExpressionTermsRules]
  val variablePattern = """\[([A-Za-z0-9_\-]+)\]""".r

  def conditionExpressionConstraint(implicit s: Session): Constraint[String] =
    Constraint[String]("constraints.parsableExpression")({ expression =>
      val variableMap: Map[String, Boolean] = expressionTerms.map(t => (t.label, false)).toMap
      val parser = new ConditionExpressionParser(variableMap)

      parser.parse(expression) match {
        case parser.Success(_,_) => Valid
        case parser.NoSuccess(msg,_) => Invalid("Error during parsing: %s.".format(msg))
        case _ => Invalid("Unknown error during parsing, please check for errors.")
      }
    })

  def ruleForm(implicit s: Session) = Form(
    mapping(
      "id" -> optional(longNumber),
      "conditionExpression" -> nonEmptyText.verifying(conditionExpressionConstraint),
      "source" -> optional(text),
      "note" -> optional(text),
      "suggestions" -> seq(mapping(
        "id" -> optional(longNumber),
        "text" -> nonEmptyText,
        "explanatoryNote" -> optional(text),
        "ruleId" -> optional(longNumber)
      )(Suggestion.apply)(Suggestion.unapply))
    )(RuleWithSuggestions.apply)(RuleWithSuggestions.unapply)
  )

  def addExpressionTermsRules(id: Long, conditionExpression: String)(implicit s: Session) = {
    variablePattern.findAllMatchIn(conditionExpression).foreach(m =>
      expressionTermsRules.insert(ExpressionTermRule(m group 1, id)))
  }

  def addSuggestions(ruleId: Long, suggestionList: List[Suggestion])(implicit session: Session) = {
    suggestionList.foreach(s => {
      suggestions.insert(Suggestion(s.id, s.text, s.explanatoryNote, Some(ruleId)))
    })
  }

  def list = DBAction { implicit rs =>
    Ok(html.rules.list(rules.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.rules.create(ruleForm))
  }

  def save = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.create(formWithErrors)),
      rule => {
        val ruleId = rules returning rules.map(_.id) += Rule(rule.id, rule.conditionExpression, rule.source, rule.note)
        addExpressionTermsRules(ruleId, rule.conditionExpression)
        addSuggestions(ruleId, rule.suggestions.toList)

        Redirect(routes.RulesController.list()).flashing("success" -> "The rule was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    rules.filter(_.id === id).firstOption match {
      case Some(r) =>
        val suggestionList = suggestions.filter(_.ruleId === id).list
        val ruleWithSuggestions = RuleWithSuggestions(r.id, r.conditionExpression, r.source, r.note, suggestionList)
        Ok(html.rules.edit(id, ruleForm.fill(ruleWithSuggestions)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.edit(id, formWithErrors)),
      r => {
        rules.filter(_.id === id).map(x => (x.conditionExpression, x.source.?, x.note.?))
          .update((r.conditionExpression, r.source, r.note))
        expressionTermsRules.filter(_.ruleId === id).delete
        addExpressionTermsRules(id, r.conditionExpression)
        suggestions.filter(_.ruleId === id).delete
        addSuggestions(id, r.suggestions.toList)

        Redirect(routes.RulesController.list()).flashing("success" -> "The rule was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    rules.filter(_.id === id).firstOption match {
      case Some(rule) => Ok(html.rules.remove(rule))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    expressionTermsRules.filter(_.ruleId === id).delete
    rules.filter(_.id === id).delete
    Redirect(routes.RulesController.list()).flashing("success" -> "The rule was deleted successfully.")
  }
}
