package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object RuleSuggestionTemplatesController extends Controller {
  val ruleSuggestionTemplateForm = Form(
    mapping(
      "ruleId" -> longNumber.transform(
        (id: Long) => RuleID(id),
        (ruleId: RuleID) => ruleId.value
      ),
      "suggestionTemplateId" -> longNumber.transform(
        (id: Long) => SuggestionTemplateID(id),
        (suggestionTemplateId: SuggestionTemplateID) => suggestionTemplateId.value
      )
    )(RuleSuggestionTemplate.apply)(RuleSuggestionTemplate.unapply)
  )

  def list(ruleId: Long) = DBAction { implicit rs =>
    Rules.find(RuleID(ruleId)) match {
      case Some(rule) =>
        Ok(html.ruleSuggestionTemplates.list(
          rule = rule,
          ruleSuggestionTemplates = Rules.suggestionTemplateListFor(RuleID(ruleId)),
          ruleSuggestionTemplateForm = ruleSuggestionTemplateForm
        ))
      case _ => NotFound
    }
  }

  def save(ruleId: Long) = DBAction { implicit rs =>
    Rules.find(RuleID(ruleId)) match {
      case Some(rule) =>
        ruleSuggestionTemplateForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.ruleSuggestionTemplates.list(
              rule = rule,
              ruleSuggestionTemplates = Rules.suggestionTemplateListFor(RuleID(ruleId)),
              ruleSuggestionTemplateForm = formWithErrors
            )),
          ruleSuggestionTemplate => {
            RulesSuggestionTemplates.insert(ruleSuggestionTemplate)
            Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
              .flashing("success" -> "The suggestion was successfully added to the rule.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(ruleId: Long, id: Long) = DBAction { implicit rs =>
    Rules.find(RuleID(ruleId)) match {
      case Some(rule) =>
        SuggestionTemplates.find(SuggestionTemplateID(id)) match {
          case Some(suggestionTemplate) => Ok(html.ruleSuggestionTemplates.remove(rule, suggestionTemplate))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(ruleId: Long, id: Long) = DBAction { implicit rs =>
    RulesSuggestionTemplates.delete(RuleID(ruleId), SuggestionTemplateID(id))
    Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
      .flashing("success" -> "The suggestion was successfully removed from the rule.")
  }
}
