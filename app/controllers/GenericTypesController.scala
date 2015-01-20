package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object GenericTypesController extends Controller {
  val genericTypeForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )),
      "name" -> nonEmptyText
    )({ case (id, name) => GenericType(id, name)})
      ({ case GenericType(id, name, _, _) => Some(id, name) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.genericTypes.list(GenericType.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.genericTypes.create(genericTypeForm))
  }

  def save = DBAction { implicit rs =>
    genericTypeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.genericTypes.create(formWithErrors)),
      genericType => {
        GenericType.insert(genericType)
        Redirect(routes.GenericTypesController.list())
          .flashing("success" -> "The drug type was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    GenericType.find(GenericTypeID(id)) match {
      case Some(genericType) =>
        Ok(html.genericTypes.edit(genericType.id.get, genericTypeForm.fill(genericType)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    genericTypeForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.genericTypes.edit(GenericTypeID(id), formWithErrors)),
      genericType => {
        GenericType.update(genericType)
        Redirect(routes.GenericTypesController.list())
          .flashing("success" -> "The drug type was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    GenericType.find(GenericTypeID(id)) match {
      case Some(genericType) => Ok(html.genericTypes.remove(genericType))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    GenericType.delete(GenericTypeID(id))
    Redirect(routes.GenericTypesController.list())
      .flashing("success" -> "The drug type was deleted successfully.")
  }
}
