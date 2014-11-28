package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object DrugTypeTermsController extends Controller {
  val drugTypeTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        label => label.matches("""[A-Za-z0-9\-_]+""")),
      "drugTypeId" -> longNumber
    )(GenericTypeTerm.apply)(GenericTypeTerm.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugTypeTerms.list(GenericTypeTerms.listWithDrugType))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugTypeTerms.create(drugTypeTermForm, GenericTypes.list))
  }

  def save = DBAction { implicit rs =>
    drugTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypeTerms.create(formWithErrors, GenericTypes.list)),
      drugTypeTerm => {
        GenericTypeTerms.insert(drugTypeTerm)
        Redirect(routes.DrugTypeTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    GenericTypeTerms.find(label) match {
      case Some(term) => Ok(html.drugTypeTerms.edit(label, drugTypeTermForm.fill(term), GenericTypes.list))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    drugTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypeTerms.edit(label, formWithErrors, GenericTypes.list)),
      term => {
        GenericTypeTerms.update(label, term)
        Redirect(routes.DrugTypeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    GenericTypeTerms.find(label) match {
      case Some(term) => Ok(html.drugTypeTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    GenericTypeTerms.delete(label)
    Redirect(routes.DrugTypeTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
