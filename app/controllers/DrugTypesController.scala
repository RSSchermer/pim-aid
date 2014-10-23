package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._

import views._
import models._

case class DrugTypeWithGroups(id: Option[Long], name: String, genericTypeId: Option[Long], drugGroupIds: Seq[Long])

object DrugTypesController extends Controller {
  val drugTypes = TableQuery[DrugTypes]
  val drugGroups = TableQuery[DrugGroups]
  val genericTypes = drugTypes.filter(_.genericTypeId.isNull)
  val drugGroupsTypes = TableQuery[DrugGroupsTypes]

  val drugTypeForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "genericTypeId" -> optional(longNumber),
      "drugGroupIds" -> seq(longNumber)
    )(DrugTypeWithGroups.apply)(DrugTypeWithGroups.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugTypes.list(drugTypes.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugTypes.create(drugTypeForm, genericTypes.list, drugGroups.list))
  }

  def save = DBAction { implicit rs =>
    drugTypeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypes.create(formWithErrors, genericTypes.list, drugGroups.list)),
      d => {
        val typeId = drugTypes returning drugTypes.map(_.id) += new DrugType(None, d.name, d.genericTypeId)
        d.drugGroupIds.foreach(groupId => drugGroupsTypes.insert(new DrugGroupType(groupId, typeId)))

        Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    drugTypes.filter(_.id === id).firstOption match {
      case Some(DrugType(typeId, name, genericTypeId)) =>
        val drugGroupIds = drugGroupsTypes.filter(_.drugTypeId === id).map(_.drugGroupId).list.toSeq
        val drugTypeWithGroups = new DrugTypeWithGroups(typeId, name, genericTypeId, drugGroupIds)
        val gts = genericTypes.filter(_.id =!= id)

        Ok(html.drugTypes.edit(id, drugTypeForm.fill(drugTypeWithGroups), gts.list, drugGroups.list))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    val gts = genericTypes.filter(_.id =!= id)

    drugTypeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypes.edit(id, formWithErrors, gts.list, drugGroups.list)),
      d => {
        drugTypes.filter(_.id === id).map(x => (x.name, x.genericTypeId.?)).update((d.name, d.genericTypeId))
        drugGroupsTypes.filter(_.drugTypeId === id).delete
        d.drugGroupIds.map(x => new DrugGroupType(x, id)).foreach(x => drugGroupsTypes.insert(x))

        Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    drugTypes.filter(_.id === id).firstOption match {
      case Some(drugType) => Ok(html.drugTypes.remove(drugType))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    drugGroupsTypes.filter(_.drugTypeId === id).delete
    drugTypes.filter(_.id === id).delete
    Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was deleted successfully.")
  }
}
