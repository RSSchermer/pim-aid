package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._

import views._
import models._

object DrugGroupsController extends Controller {
  val drugGroups = TableQuery[DrugGroups]

  val drugGroupForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText
    )(DrugGroup.apply)(DrugGroup.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugGroups.list(drugGroups.list))
  }

  def create = Action {
    Ok(html.drugGroups.create(drugGroupForm))
  }

  def save = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.create(formWithErrors)),
      drugGroup => {
        drugGroups.insert(drugGroup)
        Redirect(routes.DrugGroupsController.list()).flashing("success" -> "The drug group was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    drugGroups.filter(_.id === id).firstOption match {
      case Some(drugGroup) => Ok(html.drugGroups.edit(id, drugGroupForm.fill(drugGroup)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.edit(id, formWithErrors)),
      drugGroup => {
        drugGroups.filter(_.id === id).update(drugGroup)
        Redirect(routes.DrugGroupsController.list()).flashing("success" -> "The drug group was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    drugGroups.filter(_.id === id).firstOption match {
      case Some(drugGroup) => Ok(html.drugGroups.remove(drugGroup))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    drugGroups.filter(_.id === id).delete
    Redirect(routes.DrugGroupsController.list()).flashing("success" -> "The drug group was deleted successfully.")
  }
}
