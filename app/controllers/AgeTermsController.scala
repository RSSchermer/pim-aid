package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object AgeTermsController extends Controller {
  val ageTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "comparisonOperator" -> nonEmptyText.verifying("Not a valid operator",
        List("==", ">", ">=", "<", "<=").contains(_)),
      "age" -> number(min = 0, max = 120)
    )({ case (label, comparisonOperator, age) =>
          ExpressionTerm(label, None, None, None, None, Some(comparisonOperator), Some(age)) })
    ({ case ExpressionTerm(label, _, _, _, _, Some(comparisonOperator), Some(age)) =>
         Some(label, comparisonOperator, age) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.ageTerms.list(AgeTerm.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.ageTerms.create(ageTermForm))
  }

  def save = DBAction { implicit rs =>
    ageTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.ageTerms.create(formWithErrors)),
      ageTerm => {
        ExpressionTerm.insert(ageTerm)
        Redirect(routes.AgeTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    AgeTerm.find(label) match {
      case Some(term) => Ok(html.ageTerms.edit(label, ageTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    ageTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.ageTerms.edit(label, formWithErrors)),
      term => {
        AgeTerm.update(term)
        Redirect(routes.AgeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    AgeTerm.find(label) match {
      case Some(term) => Ok(html.ageTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    AgeTerm.delete(label)
    Redirect(routes.AgeTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
