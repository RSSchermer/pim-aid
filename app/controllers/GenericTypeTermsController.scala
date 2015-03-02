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
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )({ case (id, label, genericTypeId) => ExpressionTerm(id, label, Some(genericTypeId), None, None, None, None, None) })
    ({ case ExpressionTerm(id, label, Some(genericTypeId), _, _, _, _, _) => Some(id, label, genericTypeId) })
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
        ExpressionTerm.insert(drugTypeTerm)
        Redirect(routes.GenericTypeTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    GenericTypeTerm.find(ExpressionTermID(id)) match {
      case Some(term) =>
        Ok(html.genericTypeTerms.edit(ExpressionTermID(id), genericTypeTermForm.fill(term)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    genericTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.genericTypeTerms.edit(ExpressionTermID(id), formWithErrors)),
      term => {
        GenericTypeTerm.update(term)
        Redirect(routes.GenericTypeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    GenericTypeTerm.find(ExpressionTermID(id)) match {
      case Some(term) => Ok(html.genericTypeTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    GenericTypeTerm.delete(ExpressionTermID(id))
    Redirect(routes.GenericTypeTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
