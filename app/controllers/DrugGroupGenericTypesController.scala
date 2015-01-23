package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import schema._

import views._
import models._

object DrugGroupGenericTypesController extends Controller {
  val drugGroupGenericTypeForm = Form(
    tuple(
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      ),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )
  )

  def list(drugGroupId: Long) = DBAction { implicit rs =>
    DrugGroup.include(DrugGroup.genericTypes.include(GenericType.medicationProducts))
      .find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        Ok(html.drugGroupGenericTypes.list(
          drugGroup = drugGroup,
          drugGroupGenericTypeForm = drugGroupGenericTypeForm
        ))
      case _ => NotFound
    }
  }

  def save(drugGroupId: Long) = DBAction { implicit rs =>
    DrugGroup.find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        drugGroupGenericTypeForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.drugGroupGenericTypes.list(drugGroup, formWithErrors)),
          drugGroupGenericType => {
            TableQuery[DrugGroupsGenericTypes].insert(drugGroupGenericType)

            Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
              .flashing("success" -> "The generic type was successfully added to the drug group.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(drugGroupId: Long, id: Long) = DBAction { implicit rs =>
    DrugGroup.find(DrugGroupID(drugGroupId)) match {
      case Some(drugGroup) =>
        GenericType.find(GenericTypeID(id)) match {
          case Some(genericType) => Ok(html.drugGroupGenericTypes.remove(drugGroup, genericType))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(drugGroupId: Long, id: Long) = DBAction { implicit rs =>
    TableQuery[DrugGroupsGenericTypes]
      .filter(x => x.drugGroupId === DrugGroupID(drugGroupId) && x.genericTypeId === GenericTypeID(id))
      .delete

    Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
      .flashing("success" -> "The generic type was succesfully removed from the drug group.")
  }
}
