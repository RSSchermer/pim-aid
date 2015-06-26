package controllers

import models.meta.Profile
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._
import Profile.driver.simple.Session

object DrugsController extends Controller with UserSessionAware {
  case class DrugJson(id: Option[Long], userInput: String, resolvedMedicationProductId: Option[Long],
                      resolvedMedicationProductName: Option[String], unresolvable: Boolean)

  def drugJsonFromDrug(drug: Drug, unresolvable: Boolean = false)(implicit s: Session): DrugJson = {
    drug.resolvedMedicationProduct.getOrFetchValue match {
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
    val drugs = UserSession.drugs.include(Drug.resolvedMedicationProduct).fetchFor(userSession)

    Ok(Json.toJson(drugs.map(drugJsonFromDrug(_))))
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
          // The drug has been resolved (user selected one of the alternative drugs)
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
          val normalizedInput = drugJson.userInput.trim().replaceAll("""\s+""", " ")

          MedicationProduct.findByUserInput(normalizedInput) match {
            case Some(medicationProduct) =>
              // There is a matching medication product
              val drugId = Drug.insert(Drug(None, normalizedInput, token, medicationProduct.id))
              val json = DrugJson(Some(drugId.value), normalizedInput, Some(medicationProduct.id.get.value),
                Some(medicationProduct.name), false)

              Ok(Json.toJson(json)).withSession("token" -> token.value)
            case _ =>
              // There is no matching medication product
              val alternatives = MedicationProduct.findAlternatives(normalizedInput, 0.3, 5)
                .map {
                  case (MedicationProduct(id, name)) =>
                    DrugJson(None, normalizedInput, Some(id.get.value), Some(name), unresolvable = false)
                }

              BadRequest(Json.obj("alternatives" -> Json.toJson(alternatives)))
                .withSession("token" -> token.value)
          }
      }
    )
  }

  def delete(id: Long) = DBAction { implicit rs =>
    Drug.delete(DrugID(id))
    Ok.withSession("token" -> currentUserSession(rs).token.value)
  }
}
