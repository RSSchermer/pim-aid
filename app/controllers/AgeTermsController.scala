package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._
import models.ExpressionTermConversions._

object AgeTermsController extends Controller {
  val ageTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "comparisonOperator" -> nonEmptyText.verifying("Not a valid operator",
        List("==", ">", ">=", "<", "<=").contains(_)),
      "age" -> number(min = 0, max = 120)
    )(AgeTerm.apply)(AgeTerm.unapply)
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

  def edit(id: Long) = DBAction { implicit rs =>
    AgeTerm.find(ExpressionTermID(id)) match {
      case Some(term) => Ok(html.ageTerms.edit(ExpressionTermID(id), ageTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    ageTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.ageTerms.edit(ExpressionTermID(id), formWithErrors)),
      term => {
        AgeTerm.update(term)
        Redirect(routes.AgeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    AgeTerm.find(ExpressionTermID(id)) match {
      case Some(term) => Ok(html.ageTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    AgeTerm.delete(ExpressionTermID(id))
    Redirect(routes.AgeTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
