package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object MedicationProductGenericTypesController extends Controller {
  val genericTypeMedicationProductForm = Form(
    mapping(
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      ),
      "medicationProductId" -> longNumber.transform(
        (id: Long) => MedicationProductID(id),
        (medicationProductId: MedicationProductID) => medicationProductId.value
      )
    )(GenericTypeMedicationProduct.apply)(GenericTypeMedicationProduct.unapply)
  )

  def list(medicationProductId: Long) = DBAction { implicit rs =>
    MedicationProducts.find(MedicationProductID(medicationProductId)) match {
      case Some(medicationProduct) =>
        Ok(html.medicationProductGenericTypes.list(
          medicationProduct = medicationProduct,
          medicationProductGenericTypes = MedicationProducts.genericTypeListFor(MedicationProductID(medicationProductId)),
          genericTypeMedicationProductForm = genericTypeMedicationProductForm
        ))
      case _ => NotFound
    }
  }

  def save(medicationProductId: Long) = DBAction { implicit rs =>
    MedicationProducts.find(MedicationProductID(medicationProductId)) match {
      case Some(medicationProduct) =>
        genericTypeMedicationProductForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.medicationProductGenericTypes.list(
              medicationProduct = medicationProduct,
              medicationProductGenericTypes =
                MedicationProducts.genericTypeListFor(MedicationProductID(medicationProductId)),
              genericTypeMedicationProductForm = formWithErrors
            )),
          genericTypeMedicationProduct => {
            GenericTypesMedicationProducts.insert(genericTypeMedicationProduct)
            Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
              .flashing("success" -> "The generic type was successfully added to the medication product.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(medicationProductId: Long, id: Long) = DBAction { implicit rs =>
    MedicationProducts.find(MedicationProductID(medicationProductId)) match {
      case Some(medicationProduct) =>
        GenericTypes.find(GenericTypeID(id)) match {
          case Some(genericType) => Ok(html.medicationProductGenericTypes.remove(medicationProduct, genericType))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(medicationProductId: Long, id: Long) = DBAction { implicit rs =>
    GenericTypesMedicationProducts.delete(GenericTypeID(id), MedicationProductID(medicationProductId))
    Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
      .flashing("success" -> "The generic type was succesfully removed from the medication product.")
  }
}
