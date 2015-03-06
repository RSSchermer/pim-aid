package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current
import com.google.common.base.Charsets
import com.google.common.io._
import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import org.htmlcleaner.HtmlCleaner

import views._
import models._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

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
    Ok(html.drugGroups.list(DrugGroup.list))
  }

  def create = Action {
    Ok(html.drugGroups.create(drugGroupForm))
  }

  def save = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.create(formWithErrors)),
      drugGroup => {
        val id = DrugGroup.insert(drugGroup)

        Redirect(routes.DrugGroupGenericTypesController.list(id.value))
          .flashing("success" -> "The drug group was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    DrugGroup.find(DrugGroupID(id)) match {
      case Some(drugGroup) =>
        Ok(html.drugGroups.edit(DrugGroupID(id), drugGroupForm.fill(drugGroup)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    drugGroupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroups.edit(DrugGroupID(id), formWithErrors)),
      drugGroup => {
        DrugGroup.update(drugGroup)
        Redirect(routes.DrugGroupsController.list())
          .flashing("success" -> "The drug group was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    DrugGroup.find(DrugGroupID(id)) match {
      case Some(drugGroup) => Ok(html.drugGroups.remove(drugGroup))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    DrugGroup.delete(DrugGroupID(id))
    Redirect(routes.DrugGroupsController.list())
      .flashing("success" -> "The drug group was deleted successfully.")
  }

  def importFtkGroup = DBAction(parse.multipartFormData) { implicit rs =>
    // TODO: decide what to do with this ugly java scraping code
    rs.body.file("ftkGroupPage").map { ftkGroupPage =>
      val contents = Files.toString(ftkGroupPage.ref.file, Charsets.UTF_8)
      val cleaner = new HtmlCleaner()
      val root = cleaner.clean(contents)

      try {
        val searchResults = root.findElementByAttValue("class", "searchresults", true, true)
        val groupName = unescapeHtml4(searchResults.findElementByName("h1", true).getText.toString).trim

        val drugGroupId: DrugGroupID = DrugGroup.findByName(groupName) match {
          case Some(drugGroup) => drugGroup.id.get
          case _ => DrugGroup.insert(DrugGroup(None, groupName))
        }

        searchResults.getElementsByName("ul", true).foreach { listNode =>
          val items = listNode.getElementsByName("li", false)
          val m = """([^\(]+).*""".r.findFirstMatchIn(items.head.findElementByName("a", true).getText.toString).get
          val genericTypeName = unescapeHtml4(m.group(1)).replaceAll("\\/", " / ").trim

          val genericTypeId = GenericType.findByName(genericTypeName) match {
            case Some(genericType) => genericType.id.get
            case _ => GenericType.insert(GenericType(None, genericTypeName))
          }

          if (!TableQuery[DrugGroupsGenericTypes]
            .filter(x => x.drugGroupId === drugGroupId && x.genericTypeId === genericTypeId).exists.run
          ) {
            TableQuery[DrugGroupsGenericTypes].insert((drugGroupId, genericTypeId))
          }

          val genericMedicationProductId = MedicationProduct.findByName(genericTypeName) match {
            case Some(product) => product.id.get
            case _ => MedicationProduct.insert(MedicationProduct(None, genericTypeName))
          }

          if (!TableQuery[GenericTypesMedicationProducts]
            .filter(x => x.genericTypeId === genericTypeId && x.medicationProductId === genericMedicationProductId)
            .exists.run
          ) {
            TableQuery[GenericTypesMedicationProducts].insert((genericTypeId, genericMedicationProductId))
          }

          items.tail.foreach { medicationProductItem =>
            val medicationProductName = unescapeHtml4(medicationProductItem.getText.toString)
              .replaceAll("\\/", " / ").trim

            val medicationProductId = MedicationProduct.findByName(medicationProductName) match {
              case Some(product) => product.id.get
              case _ => MedicationProduct.insert(MedicationProduct(None, medicationProductName))
            }

            if (!TableQuery[GenericTypesMedicationProducts]
              .filter(x => x.genericTypeId === genericTypeId && x.medicationProductId === medicationProductId)
              .exists.run
            ) {
              TableQuery[GenericTypesMedicationProducts].insert((genericTypeId, medicationProductId))
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
