package controllers

import play.api.mvc._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._

object DrugsController extends Controller with UserSessionAware {
  val drugs = TableQuery[Drugs]
  val drugTypes = TableQuery[DrugTypes]

  case class DrugWithType(id: Option[Long], userInput: String, source: Option[String], drugType: Option[DrugType])

  implicit val drugTypeWrites: Writes[DrugType] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "name").write[String] and
      (JsPath \ "genericTypeId").write[Option[Long]]
    )(unlift(DrugType.unapply))

  implicit val drugWrites: Writes[DrugWithType] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "userInput").write[String] and
      (JsPath \ "source").write[Option[String]] and
      (JsPath \ "drugType").write[Option[DrugType]]
    )(unlift(DrugWithType.unapply))

  implicit val drugTypeReads: Reads[DrugType] = (
      (JsPath \ "id").read[Option[Long]] and
      (JsPath \ "name").read[String] and
      (JsPath \ "genericTypeId").read[Option[Long]]
    )(DrugType.apply _)

  implicit val drugReads: Reads[DrugWithType] = (
      (JsPath \ "id").read[Option[Long]] and
      (JsPath \ "userInput").read[String] and
      (JsPath \ "source").read[Option[String]] and
      (JsPath \ "drugType").read[Option[DrugType]]
    )(DrugWithType.apply _)

  def list = DBAction { implicit rs =>
    val userDrugs = drugs.filter(_.userToken === currentUserSession(rs.session.get("token")).token)
    val drugDrugTypes = for {
      drug <- userDrugs
      drugType <- drugTypes if drug.resolvedDrugTypeId === drug.id
    } yield (drug, drugType.?)

    Ok(Json.toJson(drugDrugTypes.list.map {
      case (Drug(id, input, _, source, _), drugType) => DrugWithType(id, input, source, drugType)
    }))
  }

  def save = DBAction(BodyParsers.parse.json) { implicit rs =>
    rs.body.validate[DrugWithType].fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
      },
      drug => {
        drugTypes.filter(_.name === drug.userInput).firstOption match {
          case Some(drugType) =>
            val token = currentUserSession(rs.session.get("token")).token

            drugs.insert(Drug(id = drug.id, userInput = drug.userInput, userToken = token, source = drug.source,
              resolvedDrugTypeId = drugType.id))

            Ok(Json.obj(
              "status" -> "OK",
              "message" -> Json.toJson(DrugWithType(drug.id, drug.userInput, drug.source, Some(drugType)))
            ))
          case _ => BadRequest(Json.obj("status" ->"KO", "message" -> "Could not resolve user input to drug type."))
        }
      }
    )
  }
}
