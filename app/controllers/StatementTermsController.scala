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
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "statementTemplate" -> nonEmptyText.verifying(MedicationProductTemplateConstraint.apply),
      "displayCondition" -> optional(text.verifying(ConditionExpressionConstraint.apply).transform(
        (s: String) => ConditionExpression(s),
        (ce: ConditionExpression) => ce.value
      ))
    )({ case (id, label, statementTemplate, displayCondition) =>
          ExpressionTerm(id, label, None, None, Some(statementTemplate), displayCondition, None, None) })
      ({ case ExpressionTerm(id, label, _, _, Some(statementTemplate), displayCondition, _, _) =>
         Some(id, label, statementTemplate, displayCondition) })
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

  def edit(id: Long) = DBAction { implicit rs =>
    StatementTerm.find(ExpressionTermID(id)) match {
      case Some(term) =>
        Ok(html.statementTerms.edit(ExpressionTermID(id), statementTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    statementTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.statementTerms.edit(ExpressionTermID(id), formWithErrors)),
      term => {
        StatementTerm.update(term)
        Redirect(routes.StatementTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    StatementTerm.find(ExpressionTermID(id)) match {
      case Some(term) => Ok(html.statementTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    StatementTerm.delete(ExpressionTermID(id))
    Redirect(routes.StatementTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
