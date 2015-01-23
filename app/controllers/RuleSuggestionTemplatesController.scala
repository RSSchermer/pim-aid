package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import schema._

import views._
import models._

object RuleSuggestionTemplatesController extends Controller {
  val ruleSuggestionTemplateForm = Form(
    tuple(
      "ruleId" -> longNumber.transform(
        (id: Long) => RuleID(id),
        (ruleId: RuleID) => ruleId.value
      ),
      "suggestionTemplateId" -> longNumber.transform(
        (id: Long) => SuggestionTemplateID(id),
        (suggestionTemplateId: SuggestionTemplateID) => suggestionTemplateId.value
      )
    )
  )

  def list(ruleId: Long) = DBAction { implicit rs =>
    Rule.include(Rule.suggestionTemplates).find(RuleID(ruleId)) match {
      case Some(rule) =>
        Ok(html.ruleSuggestionTemplates.list(rule, ruleSuggestionTemplateForm))
      case _ => NotFound
    }
  }

  def save(ruleId: Long) = DBAction { implicit rs =>
    Rule.find(RuleID(ruleId)) match {
      case Some(rule) =>
        ruleSuggestionTemplateForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.ruleSuggestionTemplates.list(rule, formWithErrors)),
          ruleSuggestionTemplate => {
            TableQuery[RulesSuggestionTemplates].insert(ruleSuggestionTemplate)
            Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
              .flashing("success" -> "The suggestion was successfully added to the rule.")
          }
        )
      case _ => NotFound
    }
  }

  def remove(ruleId: Long, id: Long) = DBAction { implicit rs =>
    Rule.find(RuleID(ruleId)) match {
      case Some(rule) =>
        SuggestionTemplate.find(SuggestionTemplateID(id)) match {
          case Some(suggestionTemplate) => Ok(html.ruleSuggestionTemplates.remove(rule, suggestionTemplate))
          case _ => NotFound
        }
      case _ => NotFound
    }
  }

  def delete(ruleId: Long, id: Long) = DBAction { implicit rs =>
    TableQuery[RulesSuggestionTemplates]
      .filter(_.ruleId === RuleID(ruleId)).filter(_.suggestionTemplateId === SuggestionTemplateID(id))
      .delete
    Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
      .flashing("success" -> "The suggestion was successfully removed from the rule.")
  }
}
