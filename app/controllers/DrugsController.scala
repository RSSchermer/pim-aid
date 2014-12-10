package controllers

import play.api.mvc._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.rockymadden.stringmetric.similarity._

import models._

object DrugsController extends Controller with UserSessionAware {
  case class DrugJson(id: Option[Long], userInput: String, resolvedMedicationProductId: Option[Long],
                      resolvedMedicationProductName: Option[String], unresolvable: Boolean)

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
    val token = currentUserSession(rs).token

    Ok(Json.toJson(UserSessions.drugWithMedicationProductListFor(token).map {
      case (Drug(id, userInput, _, _), Some(MedicationProduct(productId, productName))) =>
        DrugJson(
          id = Some(id.get.value),
          userInput = userInput,
          resolvedMedicationProductId = Some(productId.get.value),
          resolvedMedicationProductName = Some(productName),
          unresolvable = false
        )
      case (Drug(id, userInput, _, _), None) =>
        DrugJson(
          id = Some(id.get.value),
          userInput = userInput,
          resolvedMedicationProductId = None,
          resolvedMedicationProductName = None,
          unresolvable = true
        )
    })).withSession("token" -> token.value)
  }

  def save = DBAction(BodyParsers.parse.json) { implicit rs =>
    val token = currentUserSession(rs).token

    rs.body.validate[DrugJson].fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          .withSession("token" -> token.value)
      },
      {
        case DrugJson(_, userInput, Some(resolvedProductId), resolvedDrugTypeName, _) =>
          val drugId = Drugs.insert(Drug(
            id = None,
            userInput = userInput,
            userToken = token,
            resolvedMedicationProductId = Some(MedicationProductID(resolvedProductId))
          ))

          Ok(Json.toJson(DrugJson(
            id = Some(drugId.value),
            userInput = userInput,
            resolvedMedicationProductId = Some(resolvedProductId),
            resolvedMedicationProductName = resolvedDrugTypeName,
            unresolvable = false
          ))).withSession("token" -> token.value)
        case DrugJson(_, userInput, None, _, true) =>
          val drugId = Drugs.insert(Drug(
            id = None,
            userInput = userInput,
            userToken = token,
            resolvedMedicationProductId = None
          ))

          Ok(Json.toJson(DrugJson(
            id = Some(drugId.value),
            userInput = userInput,
            resolvedMedicationProductId = None,
            resolvedMedicationProductName = None,
            unresolvable = true
          ))).withSession("token" -> token.value)
        case drugJson =>
          val normalizedInput = drugJson.userInput.trim().replaceAll("""\s+""", " ")

          MedicationProducts.all.filter(_.name.toLowerCase === normalizedInput).firstOption match {
            case Some(medicationProduct) =>
              val drugId = Drugs.insert(Drug(
                id = None,
                userInput = drugJson.userInput,
                userToken = token,
                resolvedMedicationProductId = medicationProduct.id
              ))

              Ok(Json.toJson(DrugJson(
                id = Some(drugId.value),
                userInput = drugJson.userInput,
                resolvedMedicationProductId = Some(medicationProduct.id.get.value),
                resolvedMedicationProductName = Some(medicationProduct.name),
                unresolvable = false
              ))).withSession("token" -> token.value)
            case _ =>
              val alternatives: List[DrugJson] = MedicationProducts.list
                .map(x => (JaroWinklerMetric.compare(drugJson.userInput, x.name), x))
                .filter(_._1.get > 0.3)
                .sortBy(_._1)(Ordering[Option[Double]].reverse)
                .map {
                  case (_, MedicationProduct(id, name)) =>
                    DrugJson(None, drugJson.userInput, Some(id.get.value), Some(name), unresolvable = false)
                }
                .take(5)

              BadRequest(Json.obj("alternatives" -> Json.toJson(alternatives)))
                .withSession("token" -> token.value)
          }
      }
    )
  }

  def delete(id: Long) = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    UserSessions.deleteDrug(token, DrugID(id))
    Ok.withSession("token" -> token.value)
  }
}
