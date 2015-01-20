package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object GenericTypeTermsController extends Controller {
  val genericTypeTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )({ case (label, genericTypeId) => ExpressionTerm(label, Some(genericTypeId), None, None, None, None, None) })
    ({ case ExpressionTerm(label, Some(genericTypeId), _, _, _, _, _, _, _) => Some(label, genericTypeId) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.genericTypeTerms.list(GenericTypeTerm.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.genericTypeTerms.create(genericTypeTermForm))
  }

  def save = DBAction { implicit rs =>
    genericTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.genericTypeTerms.create(formWithErrors)),
      drugTypeTerm => {
        GenericTypeTerm.insert(drugTypeTerm)
        Redirect(routes.GenericTypeTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    GenericTypeTerm.find(label) match {
      case Some(term) => Ok(html.genericTypeTerms.edit(label, genericTypeTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    genericTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.genericTypeTerms.edit(label, formWithErrors)),
      term => {
        GenericTypeTerm.update(term)
        Redirect(routes.GenericTypeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    GenericTypeTerm.find(label) match {
      case Some(term) => Ok(html.genericTypeTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    GenericTypeTerm.delete(label)
    Redirect(routes.GenericTypeTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
