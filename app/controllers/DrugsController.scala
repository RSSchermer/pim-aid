package controllers

import play.api.mvc._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.rockymadden.stringmetric.similarity._

import models._

object DrugsController extends Controller with UserSessionAware {
  case class DrugJson(id: Option[Long], userInput: String, source: Option[String], resolvedDrugTypeId: Option[Long],
                       resolvedDrugTypeName: Option[String])

  implicit val drugWrites: Writes[DrugJson] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "userInput").write[String] and
      (JsPath \ "source").write[Option[String]] and
      (JsPath \ "resolvedDrugTypeId").write[Option[Long]] and
      (JsPath \ "resolvedDrugTypeName").write[Option[String]]
    )(unlift(DrugJson.unapply))

  implicit val drugReads: Reads[DrugJson] = (
      (JsPath \ "id").read[Option[Long]] and
      (JsPath \ "userInput").read[String] and
      (JsPath \ "source").read[Option[String]] and
      (JsPath \ "resolvedDrugTypeId").read[Option[Long]] and
      (JsPath \ "resolvedDrugTypeName").read[Option[String]]
    )(DrugJson.apply _)

  def list = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    Ok(Json.toJson(UserSessions.drugWithTypeListFor(token).map{
      case (Drug(id, userInput, _, source, _), Some(MedicationProduct(typeId, typeName, _))) =>
        DrugJson(id, userInput, source, typeId, Some(typeName))
      case (Drug(id, userInput, _, source, _), None) => DrugJson(id, userInput, source, None, None)
    })).withSession("token" -> token)
  }

  def save = DBAction(BodyParsers.parse.json) { implicit rs =>
    val token = currentUserSession(rs).token

    rs.body.validate[DrugJson].fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
          .withSession("token" -> token)
      },
      {
        case DrugJson(_, userInput, source, Some(resolvedDrugTypeId), resolvedDrugTypeName) =>
          val drugId = Drugs.insert(Drug(id = None, userInput = userInput, userToken = token,
            source = source, resolvedMedicationProductId = Some(resolvedDrugTypeId)))

          Ok(Json.toJson(DrugJson(Some(drugId), userInput, source, Some(resolvedDrugTypeId), resolvedDrugTypeName)))
            .withSession("token" -> token)
        case drug =>
          val normalizedInput = drug.userInput.trim().replaceAll("""\s+""", " ")

          GenericTypes.all.filter(_.name.toLowerCase === normalizedInput).firstOption match {
            case Some(drugType) =>
              val drugId = Drugs.insert(Drug(id = drug.id, userInput = drug.userInput, userToken = token,
                                             source = drug.source, resolvedMedicationProductId = drugType.id))

              Ok(Json.toJson(DrugJson(Some(drugId), drug.userInput, drug.source, drugType.id, Some(drugType.name))))
                .withSession("token" -> token)
            case _ =>
              val alternatives = GenericTypes.list
                .map(x => (JaroWinklerMetric.compare(drug.userInput, x.name), x))
                .filter(_._1.get > 0.3)
                .sortBy(_._1)(Ordering[Option[Double]].reverse)
                .map{ case (_, MedicationProduct(id, name, _)) => DrugJson(None, drug.userInput, drug.source, id, Some(name)) }
                .take(5)

              BadRequest(Json.obj("alternatives" -> Json.toJson(alternatives)))
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
