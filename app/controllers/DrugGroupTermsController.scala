package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object DrugGroupTermsController extends Controller {
  val drugGroupTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      )
    )({ case (label, drugGroupId) => ExpressionTerm(label, None, Some(drugGroupId), None, None, None, None) })
      ({ case ExpressionTerm(label, _, Some(drugGroupId), _, _, _, _, _, _) => Some(label,drugGroupId) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.list(DrugGroupTerm.include(DrugGroupTerm.drugGroup).list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.create(drugGroupTermForm))
  }

  def save = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.create(formWithErrors)),
      drugGroupTerm => {
        ExpressionTerm.insert(drugGroupTerm)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    DrugGroupTerm.include(DrugGroupTerm.drugGroup).find(label) match {
      case Some(term) =>
        Ok(html.drugGroupTerms.edit(label, drugGroupTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.edit(label, formWithErrors)),
      term => {
        DrugGroupTerm.update(term)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    DrugGroupTerm.include(DrugGroupTerm.drugGroup).find(label) match {
      case Some(term) => Ok(html.drugGroupTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    DrugGroupTerm.delete(label)
    Redirect(routes.DrugGroupTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
