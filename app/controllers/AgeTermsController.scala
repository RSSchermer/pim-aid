package controllers

import scala.concurrent.Future

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

object AgeTermsController extends Controller {
  val ageTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "comparisonOperator" -> nonEmptyText.verifying("Not a valid operator",
        List("==", ">", ">=", "<", "<=").contains(_)),
      "age" -> number(min = 0, max = 120)
    )(AgeTerm.apply)(AgeTerm.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(AgeTerm.all.result).map { ageTerms =>
      Ok(html.ageTerms.list(ageTerms))
    }
  }

  def create = Action { implicit rs =>
    Ok(html.ageTerms.create(ageTermForm))
  }

  def save = Action.async { implicit rs =>
    ageTermForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.ageTerms.create(formWithErrors))),
      ageTerm =>
        db.run(AgeTerm.insert(ageTerm)).map { _ =>
          Redirect(routes.AgeTermsController.list())
            .flashing("success" -> "The expression term was created successfully.")
        }
    )
  }

  def edit(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(AgeTerm.one(id).result).map {
      case Some(term) =>
        Ok(html.ageTerms.edit(id, ageTermForm.fill(term)))
      case _ =>
        NotFound
    }
  }

  def update(id: ExpressionTermID) = Action.async { implicit rs =>
    ageTermForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.ageTerms.edit(id, formWithErrors))),
      term =>
        db.run(AgeTerm.update(term)).map { _ =>
          Redirect(routes.AgeTermsController.list())
            .flashing("success" -> "The expression term was updated successfully.")
        }
    )
  }

  def remove(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(AgeTerm.one(id).result).map {
      case Some(term) =>
        Ok(html.ageTerms.remove(term))
      case _ =>
        NotFound
    }
  }

  def delete(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(AgeTerm.delete(id)).map { _ =>
      Redirect(routes.AgeTermsController.list())
        .flashing("success" -> "The expression term was deleted successfully.")
    }
  }
}
