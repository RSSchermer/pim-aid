package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import models._
import models.meta.Profile._
import models.meta.Profile.driver.api._

object GenericTypesController extends Controller {
  val genericTypeForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )),
      "name" -> nonEmptyText
    )(GenericType.apply)(GenericType.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(GenericType.all.result).map { genericTypes =>
      Ok(html.genericTypes.list(genericTypes))
    }
  }

  def create = Action {
    Ok(html.genericTypes.create(genericTypeForm))
  }

  def save = Action.async { implicit rs =>
    genericTypeForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.genericTypes.create(formWithErrors))),
      genericType =>
        db.run(GenericType.insert(genericType)).map { _ =>
          Redirect(routes.GenericTypesController.list())
            .flashing("success" -> "The drug type was created successfully.")
        }
    )
  }

  def edit(id: GenericTypeID) = Action.async { implicit rs =>
    db.run(GenericType.one(id).result).map {
      case Some(genericType) =>
        Ok(html.genericTypes.edit(id, genericTypeForm.fill(genericType)))
      case _ =>
        NotFound
    }
  }

  def update(id: GenericTypeID) = Action.async { implicit rs =>
    genericTypeForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.genericTypes.edit(id, formWithErrors))),
      genericType =>
        db.run(GenericType.update(genericType)).map { _ =>
          Redirect(routes.GenericTypesController.list())
            .flashing("success" -> "The drug type was updated successfully.")
        }
    )
  }

  def remove(id: GenericTypeID) = Action.async { implicit rs =>
    db.run(GenericType.one(id).result).map {
      case Some(genericType) =>
        Ok(html.genericTypes.remove(genericType))
      case _ =>
        NotFound
    }
  }

  def delete(id: GenericTypeID) = Action.async { implicit rs =>
    db.run(GenericType.delete(id)).map { _ =>
      Redirect(routes.GenericTypesController.list())
        .flashing("success" -> "The drug type was deleted successfully.")
    }
  }
}
