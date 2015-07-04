package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import models._
import models.meta.Profile._
import models.meta.Profile.driver.api._
import models.ExpressionTermConversions._

object DrugGroupTermsController extends Controller {
  val drugGroupTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "drugGroupId" -> longNumber.transform(
        (id: Long) => DrugGroupID(id),
        (drugGroupId: DrugGroupID) => drugGroupId.value
      )
    )(DrugGroupTerm.apply)(DrugGroupTerm.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(DrugGroupTerm.all.include(DrugGroupTerm.drugGroup).result).map { terms =>
      Ok(html.drugGroupTerms.list(terms))
    }
  }

  def create = Action.async { implicit rs =>
    db.run(DrugGroup.all.result).map { drugGroups =>
      Ok(html.drugGroupTerms.create(drugGroups, drugGroupTermForm))
    }
  }

  def save = Action.async { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(DrugGroup.all.result).map { drugGroups =>
          BadRequest(html.drugGroupTerms.create(drugGroups, formWithErrors))
        },
      drugGroupTerm =>
        db.run(DrugGroupTerm.insert(drugGroupTerm)).map { _ =>
          Redirect(routes.DrugGroupTermsController.list())
            .flashing("success" -> "The expression term was created successfully.")
        }
    )
  }

  def edit(id: Long) = Action.async { implicit rs =>
    db.run(for {
      termOption <- DrugGroupTerm.one(ExpressionTermID(id)).include(DrugGroupTerm.drugGroup).result
      drugGroups <- DrugGroup.all.result
    } yield termOption match {
      case Some(term) =>
        Ok(html.drugGroupTerms.edit(ExpressionTermID(id), drugGroups, drugGroupTermForm.fill(term)))
      case _ => NotFound
    })
  }

  def update(id: Long) = Action.async { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(DrugGroup.all.result).map { drugGroups =>
          BadRequest(html.drugGroupTerms.edit(ExpressionTermID(id), drugGroups, formWithErrors))
        },
      term =>
        db.run(DrugGroupTerm.update(term)).map { _ =>
          Redirect(routes.DrugGroupTermsController.list())
            .flashing("success" -> "The expression term was updated successfully.")
        }
    )
  }

  def remove(id: Long) = Action.async { implicit rs =>
    db.run(DrugGroupTerm.one(ExpressionTermID(id)).include(DrugGroupTerm.drugGroup).result).map {
      case Some(term) => Ok(html.drugGroupTerms.remove(term))
      case _ => NotFound
    }
  }

  def delete(id: Long) = Action.async { implicit rs =>
    db.run(DrugGroupTerm.delete(ExpressionTermID(id))).map { _ =>
      Redirect(routes.DrugGroupTermsController.list())
        .flashing("success" -> "The expression term was deleted successfully.")
    }
  }
}
