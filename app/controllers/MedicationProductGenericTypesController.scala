package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import model.Model._
import model.Model.driver.api._

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

  def list(medicationProductId: MedicationProductID) = Action.async { implicit rs =>
    db.run(for {
      productOption <- MedicationProduct.one(medicationProductId).include(MedicationProduct.genericTypes).result
      genericTypes <- GenericType.all.result
    } yield productOption match {
      case Some(medicationProduct) =>
        Ok(html.medicationProductGenericTypes.list(medicationProduct, genericTypes, genericTypeMedicationProductForm))
      case _ =>
        NotFound
    })
  }

  def save(medicationProductId: MedicationProductID) = Action.async { implicit rs =>
    db.run(MedicationProduct.one(medicationProductId).include(MedicationProduct.genericTypes).result).flatMap {
      case Some(medicationProduct) =>
        genericTypeMedicationProductForm.bindFromRequest.fold(
          formWithErrors =>
            db.run(GenericType.all.result).map { genericTypes =>
              BadRequest(html.medicationProductGenericTypes.list(medicationProduct, genericTypes, formWithErrors))
            },
          genericTypeMedicationProduct =>
            db.run(TableQuery[GenericTypesMedicationProducts] += genericTypeMedicationProduct).map { _=>
              Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
                .flashing("success" -> "The generic type was successfully added to the medication product.")
            }
        )
      case _ =>
        Future.successful(NotFound)
    }
  }

  def remove(medicationProductId: MedicationProductID, id: GenericTypeID) = Action.async { implicit rs =>
    db.run(MedicationProduct.one(medicationProductId).result).flatMap {
      case Some(medicationProduct) =>
        db.run(GenericType.one(id).result).map {
          case Some(genericType) =>
            Ok(html.medicationProductGenericTypes.remove(medicationProduct, genericType))
          case _ =>
            NotFound
        }
      case _ => Future.successful(NotFound)
    }
  }

  def delete(medicationProductId: MedicationProductID, id: GenericTypeID) = Action.async { implicit rs =>
    val action = TableQuery[GenericTypesMedicationProducts]
      .filter(x => x.genericTypeId === id && x.medicationProductId === medicationProductId)
      .delete

    db.run(action).map { _ =>
      Redirect(routes.MedicationProductGenericTypesController.list(medicationProductId))
        .flashing("success" -> "The generic type was succesfully removed from the medication product.")
    }
  }
}
