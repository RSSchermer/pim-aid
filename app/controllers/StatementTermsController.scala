package controllers

import constraints.{MedicationProductTemplateConstraint, ConditionExpressionConstraint}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Session

import views._
import models._

object StatementTermsController extends Controller {
  def statementTermForm(implicit s: Session) = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "statementTemplate" -> nonEmptyText.verifying(MedicationProductTemplateConstraint.apply),
      "displayCondition" -> optional(text.verifying(ConditionExpressionConstraint.apply))
    )({ case (label, statementTemplate, displayCondition) =>
          ExpressionTerm(label, None, None, Some(statementTemplate), displayCondition, None, None) })
      ({ case ExpressionTerm(label, _, _, Some(statementTemplate), displayCondition, _, _, _, _) =>
         Some(label, statementTemplate, displayCondition) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.statementTerms.list(StatementTerm.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.statementTerms.create(statementTermForm))
  }

  def save = DBAction { implicit rs =>
    statementTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.statementTerms.create(formWithErrors)),
      statementTerm => {
        ExpressionTerm.insert(statementTerm)
        Redirect(routes.StatementTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    StatementTerm.find(label) match {
      case Some(term) => Ok(html.statementTerms.edit(label, statementTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    statementTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.statementTerms.edit(label, formWithErrors)),
      term => {
        StatementTerm.update(term)
        Redirect(routes.StatementTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    StatementTerm.find(label) match {
      case Some(term) => Ok(html.statementTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    StatementTerm.delete(label)
    Redirect(routes.StatementTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
