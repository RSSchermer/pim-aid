package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import model.PIMAidDbContext._
import model.PIMAidDbContext.driver.api._

object DrugGroupGenericTypesController extends Controller {
  val drugGroupGenericTypeForm = Form(
    tuple(
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      ),
      "genericTypeId" -> longNumber.transform(
        (id: Long) => GenericTypeID(id),
        (genericTypeId: GenericTypeID) => genericTypeId.value
      )
    )
  )

  def list(drugGroupId: DrugGroupID) = Action.async { implicit rs =>
    db.run(for {
      drugGroupOption <- DrugGroup.one(drugGroupId).include(
          DrugGroup.genericTypes.include(
            GenericType.medicationProducts
          )
        ).result
      genericTypes <- GenericType.all.result
    } yield drugGroupOption match{
      case Some(drugGroup) =>
        Ok(html.drugGroupGenericTypes.list(drugGroup, genericTypes, drugGroupGenericTypeForm))
      case _ =>
        NotFound
    })

  }

  def save(drugGroupId: DrugGroupID) = Action.async { implicit rs =>
    db.run(DrugGroup.one(drugGroupId).include(
      DrugGroup.genericTypes.include(
        GenericType.medicationProducts
      )
    ).result).flatMap {
      case Some(drugGroup) =>
        drugGroupGenericTypeForm.bindFromRequest.fold(
          formWithErrors => {
            db.run(GenericType.all.result).map { genericTypes =>
              BadRequest(html.drugGroupGenericTypes.list(drugGroup, genericTypes, formWithErrors))
            }
          },
          drugGroupGenericType =>
            db.run(TableQuery[DrugGroupsGenericTypes] += drugGroupGenericType).map { _=>
              Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
                .flashing("success" -> "The generic type was successfully added to the drug group.")
            }
        )
      case _ =>
        Future.successful(NotFound)
    }
  }

  def remove(drugGroupId: DrugGroupID, id: GenericTypeID) = Action.async { implicit rs =>
    db.run(DrugGroup.one(drugGroupId).result).flatMap {
      case Some(drugGroup) =>
        db.run(GenericType.one(id).result).map {
          case Some(genericType) =>
            Ok(html.drugGroupGenericTypes.remove(drugGroup, genericType))
          case _ =>
            NotFound
        }
      case _ => Future.successful(NotFound)
    }
  }

  def delete(drugGroupId: DrugGroupID, id: GenericTypeID) = Action.async { implicit rs =>
    val action = TableQuery[DrugGroupsGenericTypes]
      .filter(x => x.drugGroupId === drugGroupId && x.genericTypeId === id)
      .delete

    db.run(action).map { _ =>
      Redirect(routes.DrugGroupGenericTypesController.list(drugGroupId))
        .flashing("success" -> "The generic type was succesfully removed from the drug group.")
    }
  }
}
