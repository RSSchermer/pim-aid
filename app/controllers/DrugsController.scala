package controllers

import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.Config.driver.simple.Session

import models._

object DrugsController extends Controller with UserSessionAware {
  case class DrugJson(id: Option[Long], userInput: String, resolvedMedicationProductId: Option[Long],
                      resolvedMedicationProductName: Option[String], unresolvable: Boolean)

  def drugJsonFromDrug(drug: Drug, unresolvable: Boolean = false)(implicit s: Session): DrugJson = {
    drug.resolvedMedicationProduct.getOrFetch match {
      case Some(p) =>
        DrugJson(Some(drug.id.get.value), drug.userInput, Some(p.id.get.value), Some(p.name), unresolvable)
      case _ =>
        DrugJson(Some(drug.id.get.value), drug.userInput, None, None, unresolvable)
    }
  }

  implicit val drugWrites: Writes[DrugJson] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "userInput").write[String] and
      (JsPath \ "resolvedMedicationProductId").write[Option[Long]] and
      (JsPath \ "resolvedMedicationProductName").write[Option[String]] and
      (JsPath \ "unresolvable").write[Boolean]
    )(unlift(DrugJson.unapply))

  implicit val drugReads: Reads[DrugJson] = (
      (JsPath \ "id").read[Option[Long]] and
      (JsPath \ "userInput").read[String] and
      (JsPath \ "resolvedMedicationProductId").read[Option[Long]] and
      (JsPath \ "resolvedMedicationProductName").read[Option[String]] and
      (JsPath \ "unresolvable").read[Boolean]
    )(DrugJson.apply _)

  def list = DBAction { implicit rs =>
    val userSession = currentUserSession(rs)
    val token = userSession.token

    Ok(Json.toJson(userSession.drugs.getOrFetch.map(drugJsonFromDrug(_))))
      .withSession("token" -> token.value)
  }

  def save = DBAction(BodyParsers.parse.json) { implicit rs =>
    val token = currentUserSession(rs).token

    rs.body.validate[DrugJson].fold(
      errors =>
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          .withSession("token" -> token.value),
      {
        case DrugJson(_, userInput, Some(resolvedProductId), resolvedDrugTypeName, _) =>
          // The drug has been resolved (user selected on the the alternative drugs)
          val drugId = Drug.insert(Drug(None, userInput, token, Some(MedicationProductID(resolvedProductId))))
          val json = DrugJson(Some(drugId.value), userInput, Some(resolvedProductId), resolvedDrugTypeName, false)

          Ok(Json.toJson(json)).withSession("token" -> token.value)
        case DrugJson(_, userInput, None, _, true) =>
          // The user declared the drug unresolvable
          val drugId = Drug.insert(Drug(None, userInput, token, None))

          Ok(Json.toJson(DrugJson(Some(drugId.value), userInput, None, None, true)))
            .withSession("token" -> token.value)
        case drugJson =>
          // A newly entered drug
          MedicationProduct.findByUserInput(drugJson.userInput) match {
            case Some(medicationProduct) =>
              // There is a matching medication product
              val drugId = Drug.insert(Drug(None, drugJson.userInput, token, medicationProduct.id))
              val json = DrugJson(Some(drugId.value),drugJson.userInput, Some(medicationProduct.id.get.value),
                Some(medicationProduct.name), false)

              Ok(Json.toJson(json)).withSession("token" -> token.value)
            case _ =>
              // There is no matching medication product
              val alternatives = MedicationProduct.alternativesForUserInput(drugJson.userInput, 0.3, 5)
                .map {
                  case (MedicationProduct(id, name, _)) =>
                    DrugJson(None, drugJson.userInput, Some(id.get.value), Some(name), unresolvable = false)
                }

              BadRequest(Json.obj("alternatives" -> Json.toJson(alternatives)))
                .withSession("token" -> token.value)
          }
      }
    )
  }

  def delete(id: Long) = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    Drug.delete(DrugID(id))
    Ok.withSession("token" -> token.value)
  }
}
