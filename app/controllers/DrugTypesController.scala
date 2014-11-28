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
    (MedicationProduct(id = id, name = name, genericTypeId = genericTypeId), drugGroupIds.toList)
  }

  def unmapForm(t: (MedicationProduct, List[Long])): Option[(Option[Long], String, Option[Long], Seq[Long])] = {
    t match {
      case (MedicationProduct(id, name, genericTypeId), drugGroupIds) => Some(id, name, genericTypeId, drugGroupIds.toSeq)
    }
  }

  def list = DBAction { implicit rs =>
    Ok(html.drugTypes.list(GenericTypes.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugTypes.create(drugTypeForm, GenericTypes.genericTypes, DrugGroups.list))
  }

  def save = DBAction { implicit rs =>
    drugTypeForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.drugTypes.create(formWithErrors, GenericTypes.genericTypes, DrugGroups.list)),
      {
        case (drugType, drugGroupIds) =>
          GenericTypes.insert(drugType, drugGroupIds)
          Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    GenericTypes.findWithGroupIds(id) match {
      case Some((drugType, drugGroupIds)) =>
        Ok(html.drugTypes.edit(id, drugTypeForm.fill((drugType, drugGroupIds)), GenericTypes.genericTypes(id),
          DrugGroups.list))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugTypeForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.drugTypes.edit(id, formWithErrors, GenericTypes.genericTypes(id), DrugGroups.list)),
      {
        case (drugType, drugGroupIds) =>
          GenericTypes.update(id, drugType, drugGroupIds)
          Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    GenericTypes.find(id) match {
      case Some(drugType) => Ok(html.drugTypes.remove(drugType))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    GenericTypes.delete(id)
    Redirect(routes.DrugTypesController.list()).flashing("success" -> "The drug type was deleted successfully.")
  }
}
