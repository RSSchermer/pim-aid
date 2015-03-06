package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._
import models.ExpressionTermConversions._

object DrugGroupTermsController extends Controller {
  val drugGroupTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      )
    )(DrugGroupTerm.apply)(DrugGroupTerm.unapply)
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

  def edit(id: Long) = DBAction { implicit rs =>
    DrugGroupTerm.include(DrugGroupTerm.drugGroup).find(ExpressionTermID(id)) match {
      case Some(term) =>
        Ok(html.drugGroupTerms.edit(ExpressionTermID(id), drugGroupTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.edit(ExpressionTermID(id), formWithErrors)),
      term => {
        DrugGroupTerm.update(term)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    DrugGroupTerm.include(DrugGroupTerm.drugGroup).find(ExpressionTermID(id)) match {
      case Some(term) => Ok(html.drugGroupTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    DrugGroupTerm.delete(ExpressionTermID(id))
    Redirect(routes.DrugGroupTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
