package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object DrugGroupGenericTypesController extends Controller {
  val drugGroupGenericTypeForm = Form(
    mapping(
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      ),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )(DrugGroupGenericType.apply)(DrugGroupGenericType.unapply)
  )

  def list(drugGroupId: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        Ok(html.drugGroupGenericTypes.list(
          drugGroup = drugGroup,
          drugGroupGenericTypes = DrugGroups.genericTypeWithMedicationProductsListFor(DrugGroupID(drugGroupId)),
          drugGroupGenericTypeForm = drugGroupGenericTypeForm
        ))
      case _ => NotFound
    }
  }

  def save(drugGroupId: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        drugGroupGenericTypeForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.drugGroupGenericTypes.list(
              drugGroup = drugGroup,
              drugGroupGenericTypes = DrugGroups.genericTypeWithMedicationProductsListFor(DrugGroupID(drugGroupId)),
              drugGroupGenericTypeForm = formWithErrors
            )),
          drugGroupGenericType => {
            DrugGroupsGenericTypes.insert(drugGroupGenericType)
            Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
              .flashing("success" -> "The generic type was successfully added to the drug group.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(drugGroupId: Long, id: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        GenericTypes.find(GenericTypeID(id)) match {
          case Some(genericType) => Ok(html.drugGroupGenericTypes.remove(drugGroup, genericType))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(drugGroupId: Long, id: Long) = DBAction { implicit rs =>
    DrugGroupsGenericTypes.delete(DrugGroupID(drugGroupId), GenericTypeID(id))
    Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
      .flashing("success" -> "The generic type was succesfully removed from the drug group.")
  }
}
