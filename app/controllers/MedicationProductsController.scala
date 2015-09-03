package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import model.PIMAidDbContext._
import model.PIMAidDbContext.driver.api._

object MedicationProductsController extends Controller {
  val medicationProductForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => MedicationProductID(id),
        (medicationProductId: MedicationProductID) => medicationProductId.value
      )),
      "name" -> nonEmptyText
    )(MedicationProduct.apply)(MedicationProduct.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(MedicationProduct.all.result).map { products =>
      Ok(html.medicationProducts.list(products))
    }
  }

  def create = Action {
    Ok(html.medicationProducts.create(medicationProductForm))
  }

  def save = Action.async { implicit rs =>
    medicationProductForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.medicationProducts.create(formWithErrors))),
      medicationProduct =>
        db.run(MedicationProduct.insert(medicationProduct)).map { id =>
          Redirect(routes.MedicationProductGenericTypesController.list(id))
            .flashing("success" -> "The medication product was created successfully.")
        }
    )
  }

  def edit(id: MedicationProductID) = Action.async { implicit rs =>
    db.run(MedicationProduct.one(id).result).map {
      case Some(medicationProduct) =>
        Ok(html.medicationProducts.edit(id, medicationProductForm.fill(medicationProduct)))
      case _ =>
        NotFound
    }
  }

  def update(id: MedicationProductID) = Action.async { implicit rs =>
    medicationProductForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.medicationProducts.edit(id, formWithErrors))),
      medicationProduct =>
        db.run(MedicationProduct.update(medicationProduct)).map { _ =>
          Redirect(routes.MedicationProductsController.list())
            .flashing("success" -> "The medication product was updated successfully.")
        }
    )
  }

  def remove(id: MedicationProductID) = Action.async { implicit rs =>
    db.run(MedicationProduct.one(id).result).map {
      case Some(medicationProduct) =>
        Ok(html.medicationProducts.remove(medicationProduct))
      case _ =>
        NotFound
    }
  }

  def delete(id: MedicationProductID) = Action.async { implicit rs =>
    db.run(MedicationProduct.delete(id)).map { _ =>
      Redirect(routes.MedicationProductsController.list())
        .flashing("success" -> "The drug type was deleted successfully.")
    }
  }
}
