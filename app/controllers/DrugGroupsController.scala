package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current
import com.google.common.base.Charsets
import com.google.common.io._
import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import play.twirl.api.HtmlFormat
import org.htmlcleaner.HtmlCleaner

import views._
import models._

object DrugGroupsController extends Controller {
  val drugGroupForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      )),
      "name" -> nonEmptyText
    )(DrugGroup.apply)(DrugGroup.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugGroups.list(DrugGroups.list))
  }

  def create = Action {
    Ok(html.drugGroups.create(drugGroupForm))
  }

  def save = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.create(formWithErrors)),
      drugGroup => {
        val id = DrugGroups.insert(drugGroup)

        Redirect(routes.DrugGroupGenericTypesController.list(id.value))
          .flashing("success" -> "The drug group was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(id)) match {
      case Some(drugGroup) =>
        Ok(html.drugGroups.edit(DrugGroupID(id), drugGroupForm.fill(drugGroup)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.edit(DrugGroupID(id), formWithErrors)),
      drugGroup => {
        DrugGroups.update(DrugGroupID(id), drugGroup)
        Redirect(routes.DrugGroupsController.list())
          .flashing("success" -> "The drug group was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    DrugGroups.find(DrugGroupID(id)) match {
      case Some(drugGroup) => Ok(html.drugGroups.remove(drugGroup))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    DrugGroups.delete(DrugGroupID(id))
    Redirect(routes.DrugGroupsController.list())
      .flashing("success" -> "The drug group was deleted successfully.")
  }

  def importFtkGroup = DBAction(parse.multipartFormData) { implicit rs =>
    rs.body.file("ftkGroupPage").map { ftkGroupPage =>
      val contents = Files.toString(ftkGroupPage.ref.file, Charsets.UTF_8)
      val cleaner = new HtmlCleaner()
      val root = cleaner.clean(contents)

      try {
        val searchResults = root.findElementByAttValue("class", "searchresults", true, true)
        val groupName = unescapeHtml4(searchResults.findElementByName("h1", true).getText.toString).trim

        val drugGroupId: DrugGroupID = DrugGroups.findByName(groupName) match {
          case Some(drugGroup) => drugGroup.id.get
          case _ => DrugGroups.insert(DrugGroup(None, groupName))
        }

        searchResults.getElementsByName("ul", true).foreach { listNode =>
          val items = listNode.getElementsByName("li", false)
          val m = """([^\(]+).*""".r.findFirstMatchIn(items.head.findElementByName("a", true).getText.toString).get
          val genericTypeName = unescapeHtml4(m.group(1)).trim

          val genericTypeId = GenericTypes.findByName(genericTypeName) match {
            case Some(genericType) => genericType.id.get
            case _ => GenericTypes.insert(GenericType(None, genericTypeName))
          }

          if (!DrugGroupsGenericTypes.exists(drugGroupId, genericTypeId)) {
            DrugGroupsGenericTypes.insert(DrugGroupGenericType(drugGroupId, genericTypeId))
          }

          val genericMedicationProductId = MedicationProducts.findByName(genericTypeName) match {
            case Some(product) => product.id.get
            case _ => MedicationProducts.insert(MedicationProduct(None, genericTypeName))
          }

          if (!GenericTypesMedicationProducts.exists(genericTypeId, genericMedicationProductId)) {
            GenericTypesMedicationProducts
              .insert(GenericTypeMedicationProduct(genericTypeId, genericMedicationProductId))
          }

          items.tail.foreach { medicationProductItem =>
            val medicationProductName = unescapeHtml4(medicationProductItem.getText.toString).trim

            val medicationProductId = MedicationProducts.findByName(medicationProductName) match {
              case Some(product) => product.id.get
              case _ => MedicationProducts.insert(MedicationProduct(None, medicationProductName))
            }

            if (!GenericTypesMedicationProducts.exists(genericTypeId, medicationProductId)) {
              GenericTypesMedicationProducts.insert(GenericTypeMedicationProduct(genericTypeId, medicationProductId))
            }
          }
        }

        Redirect(routes.DrugGroupsController.list())
          .flashing("success" -> "The FTK group was imported successfully.")
      } catch {
        case e: NullPointerException =>
          Redirect(routes.DrugGroupsController.list())
            .flashing("error" -> "Invalid HTML file, does not contain the expected elements.")
      }
    }.getOrElse {
      Redirect(routes.DrugGroupsController.list())
        .flashing("error" -> "Must specify a file for upload.")
    }
  }
}
