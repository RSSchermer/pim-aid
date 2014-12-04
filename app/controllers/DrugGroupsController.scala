package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current

import views._
import models._

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

  def list = DBAction { implicit rs =>
    Ok(html.drugGroups.list(DrugGroups.list))
  }

  def create = Action {
    Ok(html.drugGroups.create(drugGroupForm))
  }

  def save = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.create(formWithErrors)),
      drugGroup => {
        val id = DrugGroups.insert(drugGroup)

        Redirect(routes.DrugGroupGenericTypesController.list(id.value))
          .flashing("success" -> "The drug group was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(id)) match {
      case Some(drugGroup) =>
        Ok(html.drugGroups.edit(DrugGroupID(id), drugGroupForm.fill(drugGroup)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.edit(DrugGroupID(id), formWithErrors)),
      drugGroup => {
        DrugGroups.update(DrugGroupID(id), drugGroup)
        Redirect(routes.DrugGroupsController.list())
          .flashing("success" -> "The drug group was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(id)) match {
      case Some(drugGroup) => Ok(html.drugGroups.remove(drugGroup))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    DrugGroups.delete(DrugGroupID(id))
    Redirect(routes.DrugGroupsController.list())
      .flashing("success" -> "The drug group was deleted successfully.")
  }
}
