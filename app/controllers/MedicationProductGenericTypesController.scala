package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._
import models.Profile.driver.simple._

object MedicationProductGenericTypesController extends Controller {
  val genericTypeMedicationProductForm = Form(
    tuple(
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      ),
      "medicationProductId" -> longNumber.transform(
        (id: Long) => MedicationProductID(id),
        (medicationProductId: MedicationProductID) => medicationProductId.value
      )
    )
  )

  def list(medicationProductId: Long) = DBAction { implicit rs =>
    MedicationProduct
      .include(MedicationProduct.genericTypes)
      .find(MedicationProductID(medicationProductId)) match {
        case Some(medicationProduct) =>
          Ok(html.medicationProductGenericTypes.list(medicationProduct, genericTypeMedicationProductForm))
        case _ => NotFound
      }
  }

  def save(medicationProductId: Long) = DBAction { implicit rs =>
    MedicationProduct.find(MedicationProductID(medicationProductId)) match {
      case Some(medicationProduct) =>
        genericTypeMedicationProductForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.medicationProductGenericTypes.list(medicationProduct, formWithErrors)),
          genericTypeMedicationProduct => {
            TableQuery[GenericTypesMedicationProducts].insert(genericTypeMedicationProduct)

            Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
              .flashing("success" -> "The generic type was successfully added to the medication product.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(medicationProductId: Long, id: Long) = DBAction { implicit rs =>
    MedicationProduct.find(MedicationProductID(medicationProductId)) match {
      case Some(medicationProduct) =>
        GenericType.find(GenericTypeID(id)) match {
          case Some(genericType) => Ok(html.medicationProductGenericTypes.remove(medicationProduct, genericType))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(medicationProductId: Long, id: Long) = DBAction { implicit rs =>
    TableQuery[GenericTypesMedicationProducts]
      .filter(_.genericTypeId === GenericTypeID(id))
      .filter(_.medicationProductId === MedicationProductID(medicationProductId))
      .delete
    Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
      .flashing("success" -> "The generic type was succesfully removed from the medication product.")
  }
}
