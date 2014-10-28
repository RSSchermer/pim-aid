package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current

import views._
import models._

object DrugTypesController extends Controller {
  val drugTypeForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "genericTypeId" -> optional(longNumber),
      "drugGroupIds" -> seq(longNumber)
    )(mapForm)(unmapForm)
  )

  def mapForm(id: Option[Long], name: String, genericTypeId: Option[Long], drugGroupIds: Seq[Long]) = {
    (DrugType(id = id, name = name, genericTypeId = genericTypeId), drugGroupIds.toList)
  }

  def unmapForm(t: (DrugType, List[Long])): Option[(Option[Long], String, Option[Long], Seq[Long])] = {
    t match {
      case (DrugType(id, name, genericTypeId), drugGroupIds) => Some(id, name, genericTypeId, drugGroupIds.toSeq)
    }
  }

  def list = DBAction { implicit rs =>
    Ok(html.drugTypes.list(DrugTypes.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugTypes.create(drugTypeForm, DrugTypes.genericTypes, DrugGroups.list))
  }

  def save = DBAction { implicit rs =>
    drugTypeForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.drugTypes.create(formWithErrors, DrugTypes.genericTypes, DrugGroups.list)),
      {
        case (drugType, drugGroupIds) =>
          DrugTypes.insert(drugType, drugGroupIds)
          Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    DrugTypes.findWithGroupIds(id) match {
      case Some((drugType, drugGroupIds)) =>
        Ok(html.drugTypes.edit(id, drugTypeForm.fill((drugType, drugGroupIds)), DrugTypes.genericTypes(id),
          DrugGroups.list))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugTypeForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.drugTypes.edit(id, formWithErrors, DrugTypes.genericTypes(id), DrugGroups.list)),
      {
        case (drugType, drugGroupIds) =>
          DrugTypes.update(id, drugType, drugGroupIds)
          Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    DrugTypes.find(id) match {
      case Some(drugType) => Ok(html.drugTypes.remove(drugType))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    DrugTypes.delete(id)
    Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was deleted successfully.")
  }
}
