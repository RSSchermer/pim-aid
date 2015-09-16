package controllers

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import model.PIMAidDBContext._
import model.PIMAidDBContext.driver.api._

object DrugsController extends Controller {
  case class DrugJson(id: Option[Long],
                      userInput: String,
                      resolvedMedicationProductId: Option[Long],
                      resolvedMedicationProductName: Option[String],
                      unresolvable: Boolean)

  def drugJsonFromDrug(drug: Drug, unresolvable: Boolean = false): DrugJson =
    Await.result(db.run(drug.resolvedMedicationProduct.valueAction).map {
      case Some(p) =>
        DrugJson(Some(drug.id.get.value), drug.userInput, Some(p.id.get.value), Some(p.name), unresolvable)
      case _ =>
        DrugJson(Some(drug.id.get.value), drug.userInput, None, None, unresolvable)
    }, 10 seconds)

  implicit val drugWrites: Writes[DrugJson] = (
      (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "userInput").write[String] and
      (JsPath \ "resolvedMedicationProductId").writeNullable[Long] and
      (JsPath \ "resolvedMedicationProductName").writeNullable[String] and
      (JsPath \ "unresolvable").write[Boolean]
    )(unlift(DrugJson.unapply))

  implicit val drugReads: Reads[DrugJson] = (
      (JsPath \ "id").readNullable[Long] and
      (JsPath \ "userInput").read[String] and
      (JsPath \ "resolvedMedicationProductId").readNullable[Long] and
      (JsPath \ "resolvedMedicationProductName").readNullable[String] and
      (JsPath \ "unresolvable").read[Boolean]
    )(DrugJson.apply _)

  def list = UserSessionAwareAction.async { implicit rs =>
    db.run(rs.userSession.drugs.valueAction).map { drugs =>
      Ok(Json.toJson(drugs.map(drugJsonFromDrug(_)))).withSession("token" -> rs.userSession.token.value)
    }
  }

  def save = UserSessionAwareAction.async(BodyParsers.parse.json) { implicit rs =>
    val token = rs.userSession.token

    rs.body.validate[DrugJson].fold(
      errors =>
        Future.successful(BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          .withSession("token" -> token.value)),
      {
        case DrugJson(_, userInput, Some(resolvedProductId), resolvedDrugTypeName, _) =>
          // The drug has been resolved (user selected one of the alternative drugs)
          db.run(Drug.insert(Drug(None, userInput, token, Some(MedicationProductID(resolvedProductId))))).map { drugId =>
            val json = DrugJson(Some(drugId.value), userInput, Some(resolvedProductId), resolvedDrugTypeName, false)
            Ok(Json.toJson(json)).withSession("token" -> token.value)
          }
        case DrugJson(_, userInput, None, _, true) =>
          // The user declared the drug unresolvable
          db.run(Drug.insert(Drug(None, userInput, token, None))).map { drugId =>
            Ok(Json.toJson(DrugJson(Some(drugId.value), userInput, None, None, true)))
              .withSession("token" -> token.value)
          }
        case drugJson =>
          // A newly entered drug
          val normalizedInput = drugJson.userInput.trim().replaceAll("""\s+""", " ")

          db.run(MedicationProduct.hasName(normalizedInput).result.headOption).flatMap {
            case Some(medicationProduct) =>
              // There is a matching medication product
              db.run(Drug.insert(Drug(None, normalizedInput, token, medicationProduct.id))).map { drugId =>
                val json = DrugJson(Some(drugId.value), normalizedInput, Some(medicationProduct.id.get.value),
                  Some(medicationProduct.name), false)

                Ok(Json.toJson(json)).withSession("token" -> token.value)
              }
            case _ =>
              // There is no matching medication product
              db.run(MedicationProduct.findAlternatives(normalizedInput, 0.3, 5)).map { alternatives =>
                val alternativesJson = alternatives.map {
                  case (MedicationProduct(id, name)) =>
                    DrugJson(None, normalizedInput, Some(id.get.value), Some(name), unresolvable = false)
                }

                BadRequest(Json.obj("alternatives" -> Json.toJson(alternativesJson)))
                  .withSession("token" -> token.value)
              }
          }
      }
    )
  }

  def delete(id: DrugID) = UserSessionAwareAction.async { implicit rs =>
    db.run(Drug.delete(id)).map { _ =>
      Ok.withSession("token" -> rs.userSession.token.value)
    }
  }
}
