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
        l => l.matches("""[A-Za-z0-9\-_]+""")),
      "drugGroupId" -> longNumber
    )(DrugGroupTerm.apply)(DrugGroupTerm.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.list(DrugGroupTerms.listWithDrugGroup))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.create(drugGroupTermForm, DrugGroups.list))
  }

  def save = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.create(formWithErrors, DrugGroups.list)),
      drugGroupTerm => {
        DrugGroupTerms.insert(drugGroupTerm)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    DrugGroupTerms.find(label) match {
      case Some(term) => Ok(html.drugGroupTerms.edit(label, drugGroupTermForm.fill(term), DrugGroups.list))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.edit(label, formWithErrors, DrugGroups.list)),
      term => {
        DrugGroupTerms.update(label, term)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    DrugGroupTerms.find(label) match {
      case Some(term) => Ok(html.drugGroupTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    DrugGroupTerms.delete(label)
    Redirect(routes.DrugGroupTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
