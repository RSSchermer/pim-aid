package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import model.PIMAidDBContext._
import constraints.MedicationProductTemplateConstraint

object SuggestionTemplatesController extends Controller {
  def suggestionTemplateForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => SuggestionTemplateID(id),
        (suggestionTemplateId: SuggestionTemplateID) => suggestionTemplateId.value
      )),
      "name" -> nonEmptyText,
      "text" -> nonEmptyText.verifying(MedicationProductTemplateConstraint.apply),
      "explanatoryNote" -> optional(text.verifying(MedicationProductTemplateConstraint.apply))
    )(SuggestionTemplate.apply)(SuggestionTemplate.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(SuggestionTemplate.all.include(SuggestionTemplate.rules).result).map { templates =>
      Ok(html.suggestionTemplates.list(templates))
    }
  }

  def create = Action {
    Ok(html.suggestionTemplates.create(suggestionTemplateForm))
  }

  def save = Action.async { implicit rs =>
    suggestionTemplateForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.suggestionTemplates.create(formWithErrors))),
      suggestionTemplate =>
        db.run(SuggestionTemplate.insert(suggestionTemplate)).map { _ =>
          Redirect(routes.SuggestionTemplatesController.list())
            .flashing("success" -> "The suggestion was created successfully.")
        }
    )
  }

  def edit(id: SuggestionTemplateID) = Action.async { implicit rs =>
    db.run(SuggestionTemplate.one(id).result).map {
      case Some(suggestionTemplate) =>
        Ok(html.suggestionTemplates.edit(id, suggestionTemplateForm.fill(suggestionTemplate)))
      case _ =>
        NotFound
    }
  }

  def update(id: SuggestionTemplateID) = Action.async { implicit rs =>
    suggestionTemplateForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.suggestionTemplates.edit(id, formWithErrors))),
      suggestionTemplate =>
        db.run(SuggestionTemplate.update(suggestionTemplate)).map { _ =>
          Redirect(routes.SuggestionTemplatesController.list())
            .flashing("success" -> "The suggestion was updated successfully.")
        }
    )
  }

  def remove(id: SuggestionTemplateID) = Action.async { implicit rs =>
    db.run(SuggestionTemplate.one(id).result).map {
      case Some(suggestionTemplate) =>
        Ok(html.suggestionTemplates.remove(suggestionTemplate))
      case _ =>
        NotFound
    }
  }

  def delete(id: SuggestionTemplateID) = Action.async { implicit rs =>
    db.run(SuggestionTemplate.delete(id)).map { _ =>
      Redirect(routes.SuggestionTemplatesController.list())
        .flashing("success" -> "The suggestion was deleted successfully.")
    }
  }
}
