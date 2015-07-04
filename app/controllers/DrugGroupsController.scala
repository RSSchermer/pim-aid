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

object DrugGroupsController extends Controller {
  val drugGroupForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      )),
      "name" -> nonEmptyText
    )(DrugGroup.apply)(DrugGroup.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(DrugGroup.all.result).map { drugGroups =>
      Ok(html.drugGroups.list(drugGroups))
    }
  }

  def create = Action {
    Ok(html.drugGroups.create(drugGroupForm))
  }

  def save = Action.async { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.drugGroups.create(formWithErrors))),
      drugGroup =>
        db.run(DrugGroup.insert(drugGroup)).map { id =>
          Redirect(routes.DrugGroupGenericTypesController.list(id.value))
            .flashing("success" -> "The drug group was created successfully.")
        }
    )
  }

  def edit(id: Long) = Action.async { implicit rs =>
    db.run(DrugGroup.one(DrugGroupID(id)).result).map {
      case Some(drugGroup) =>
        Ok(html.drugGroups.edit(DrugGroupID(id), drugGroupForm.fill(drugGroup)))
      case _ => NotFound
    }
  }

  def update(id: Long) = Action.async { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.drugGroups.edit(DrugGroupID(id), formWithErrors))),
      drugGroup =>
        db.run(DrugGroup.update(drugGroup)).map { _ =>
          Redirect(routes.DrugGroupsController.list())
            .flashing("success" -> "The drug group was updated successfully.")
        }
    )
  }

  def remove(id: Long) = Action.async { implicit rs =>
    db.run(DrugGroup.one(DrugGroupID(id)).result).map {
      case Some(drugGroup) => Ok(html.drugGroups.remove(drugGroup))
      case _ => NotFound
    }
  }

  def delete(id: Long) = Action.async { implicit rs =>
    db.run(DrugGroup.delete(DrugGroupID(id))).map { _ =>
      Redirect(routes.DrugGroupsController.list())
        .flashing("success" -> "The drug group was deleted successfully.")
    }
  }
}
