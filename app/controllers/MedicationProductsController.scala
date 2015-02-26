package controllers

import play.api.Play.current
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import java.io.File

import com.github.tototoshi.csv._

import views._
import models._

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

  def list = DBAction { implicit rs =>
    Ok(html.medicationProducts.list(MedicationProduct.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.medicationProducts.create(medicationProductForm))
  }

  def save = DBAction { implicit rs =>
    medicationProductForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.medicationProducts.create(formWithErrors)),
      medicationProduct => {
        val id = MedicationProduct.insert(medicationProduct)

        Redirect(routes.MedicationProductGenericTypesController.list(id.value))
          .flashing("success" -> "The medication product was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    MedicationProduct.find(MedicationProductID(id)) match {
      case Some(medicationProduct) =>
        Ok(html.medicationProducts.edit(medicationProduct.id.get, medicationProductForm.fill(medicationProduct)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    medicationProductForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.medicationProducts.edit(MedicationProductID(id), formWithErrors)),
      medicationProduct => {
        MedicationProduct.update(medicationProduct)
        Redirect(routes.MedicationProductsController.list())
          .flashing("success" -> "The medication product was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    MedicationProduct.find(MedicationProductID(id)) match {
      case Some(medicationProduct) => Ok(html.medicationProducts.remove(medicationProduct))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    MedicationProduct.delete(MedicationProductID(id))
    Redirect(routes.MedicationProductsController.list())
      .flashing("success" -> "The drug type was deleted successfully.")
  }

  def uploadCSV = DBAction(parse.multipartFormData) { implicit rs =>
    rs.body.file("medicationProducts").map { medicationProducts =>
      val tmpDir = new File(s"${current.path}/tmp")

      if (!tmpDir.exists()) {
        tmpDir.mkdir()
      }

      val file = new File(s"${current.path}/tmp/medicationProducts.csv")

      medicationProducts.ref.moveTo(file)

      val reader = CSVReader.open(file)

      reader.all().foreach { (x: List[String]) =>
        val productName = x.head

        if (MedicationProduct.findByName(productName).isEmpty) {
          MedicationProduct.insert(MedicationProduct(None, productName))
        }
      }

      Redirect(routes.MedicationProductsController.list())
        .flashing("success" -> "The medication products csv was uploaded successfully.")
    }.getOrElse {
      Redirect(routes.MedicationProductsController.list()).flashing(
        "error" -> "Must specify a file for upload.")
    }
  }
}
