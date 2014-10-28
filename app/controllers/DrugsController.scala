package controllers

import play.api.mvc._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._

object DrugsController extends Controller with UserSessionAware {
  case class DrugJson(id: Option[Long], userInput: String, source: Option[String])

  implicit val drugWrites: Writes[DrugJson] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "userInput").write[String] and
      (JsPath \ "source").write[Option[String]]
    )(unlift(DrugJson.unapply))

  implicit val drugReads: Reads[DrugJson] = (
      (JsPath \ "id").read[Option[Long]] and
      (JsPath \ "userInput").read[String] and
      (JsPath \ "source").read[Option[String]]
    )(DrugJson.apply _)

  def list = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    Ok(Json.toJson(UserSessions.drugListFor(token).map{
      case Drug(id, userInput, _, source, _) => DrugJson(id, userInput, source)
    })).withSession("token" -> token)
  }

  def save = DBAction(BodyParsers.parse.json) { implicit rs =>
    val token = currentUserSession(rs).token

    rs.body.validate[DrugJson].fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          .withSession("token" -> token)
      },
      drug => {
        DrugTypes.all.filter(_.name.toLowerCase === drug.userInput.toLowerCase).firstOption match {
          case Some(drugType) =>
            val drugId = Drugs.insert(Drug(id = drug.id, userInput = drug.userInput, userToken = token,
                                           source = drug.source, resolvedDrugTypeId = drugType.id))

            Ok(Json.toJson(DrugJson(Some(drugId), drug.userInput, drug.source))).withSession("token" -> token)
          case _ => BadRequest(Json.obj("status" ->"KO", "message" -> "Could not resolve user input to drug type."))
            .withSession("token" -> token)
        }
      }
    )
  }

  def delete(id: Long) = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    UserSessions.drugsFor(token).filter(_.id === id).delete
    Ok.withSession("token" -> token)
  }
}
