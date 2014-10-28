package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.db.slick._
import play.api.db.slick.Session

import views._
import models._
import utils._

object RulesController extends Controller {
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
    )(mapForm)(unmapForm)
  )

  def conditionExpressionConstraint(implicit s: Session): Constraint[String] =
    Constraint[String]("constraints.parsableExpression")({ expression =>
      val variableMap: Map[String, Boolean] = ExpressionTerms.list.map(t => (t.label, false)).toMap
      val parser = new ConditionExpressionParser(variableMap)

      parser.parse(expression) match {
        case parser.Success(_,_) => Valid
        case parser.NoSuccess(msg,_) => Invalid("Error during parsing: %s.".format(msg))
        case _ => Invalid("Unknown error during parsing, please check for errors.")
      }
    })

  def mapForm(id: Option[Long], ce: String, source: Option[String], note: Option[String], sSeq: Seq[Suggestion]) = {
    (Rule(id = id, conditionExpression = ce, source = source, note = note), sSeq.toList)
  }

  def unmapForm(t: (Rule, List[Suggestion])):
    Option[(Option[Long], String, Option[String], Option[String], Seq[Suggestion])] =
  {
    t match {
      case (Rule(id, ce, source, note), sList) => Some(id, ce, source, note, sList.toSeq)
    }
  }

  def list = DBAction { implicit rs =>
    Ok(html.rules.list(Rules.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.rules.create(ruleForm))
  }

  def save = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.create(formWithErrors)),
      {
        case (rule, suggestionList) =>
          Rules.insert(rule, suggestionList)
          Redirect(routes.RulesController.list()).flashing("success" -> "The rule was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    Rules.findWithSuggestions(id) match {
      case Some((rule, suggestionList)) => Ok(html.rules.edit(id, ruleForm.fill((rule, suggestionList))))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.edit(id, formWithErrors)),
      {
        case (rule, suggestionList) =>
          Rules.update(id, rule, suggestionList)
          Redirect(routes.RulesController.list()).flashing("success" -> "The rule was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    Rules.find(id) match {
      case Some(rule) => Ok(html.rules.remove(rule))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    Rules.delete(id)
    Redirect(routes.RulesController.list()).flashing("success" -> "The rule was deleted successfully.")
  }
}
