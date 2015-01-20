package controllers

import constraints.MedicationProductTemplateConstraint
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Session

import views._
import models._

object SuggestionTemplatesController extends Controller {
  def suggestionTemplateForm(implicit s: Session) = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => SuggestionTemplateID(id),
        (suggestionTemplateId: SuggestionTemplateID) => suggestionTemplateId.value
      )),
      "name" -> nonEmptyText,
      "text" -> nonEmptyText.verifying(MedicationProductTemplateConstraint.apply),
      "explanatoryNote" -> optional(text.verifying(MedicationProductTemplateConstraint.apply))
    )({ case (id, name, txt, en) => SuggestionTemplate(id, name, txt, en) })
      ({ case SuggestionTemplate(id, name, txt, en, _) => Some(id, name, txt, en) })
  )

  def list = DBAction { implicit rs =>
    Ok(html.suggestionTemplates.list(SuggestionTemplate.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.suggestionTemplates.create(suggestionTemplateForm))
  }

  def save = DBAction { implicit rs =>
    suggestionTemplateForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.suggestionTemplates.create(formWithErrors)),
      suggestionTemplate => {
        SuggestionTemplate.insert(suggestionTemplate)
        Redirect(routes.SuggestionTemplatesController.list())
          .flashing("success" -> "The suggestion was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    SuggestionTemplate.find(SuggestionTemplateID(id)) match {
      case Some(suggestionTemplate) =>
        Ok(html.suggestionTemplates.edit(suggestionTemplate.id.get, suggestionTemplateForm.fill(suggestionTemplate)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    suggestionTemplateForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(html.suggestionTemplates.edit(SuggestionTemplateID(id), formWithErrors)),
      suggestionTemplate => {
        SuggestionTemplate.update(suggestionTemplate)
        Redirect(routes.SuggestionTemplatesController.list())
          .flashing("success" -> "The suggestion was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    SuggestionTemplate.find(SuggestionTemplateID(id)) match {
      case Some(suggestionTemplate) => Ok(html.suggestionTemplates.remove(suggestionTemplate))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    SuggestionTemplate.delete(SuggestionTemplateID(id))
    Redirect(routes.SuggestionTemplatesController.list())
      .flashing("success" -> "The suggestion was deleted successfully.")
  }
}
