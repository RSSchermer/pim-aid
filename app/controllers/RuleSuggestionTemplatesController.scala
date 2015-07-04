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
import models.meta.Schema._
import models.meta.Profile._
import models.meta.Profile.driver.api._

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

  def list(ruleId: Long) = Action.async { implicit rs =>
    db.run(for {
      ruleOption <- Rule.one(RuleID(ruleId)).include(Rule.suggestionTemplates).result
      suggestionTemplates <- SuggestionTemplate.all.result
    } yield ruleOption match {
      case Some(rule) =>
        Ok(html.ruleSuggestionTemplates.list(rule, suggestionTemplates, ruleSuggestionTemplateForm))
      case _ => NotFound
    })
  }

  def save(ruleId: Long) = Action.async { implicit rs =>
    db.run(Rule.one(RuleID(ruleId)).include(Rule.suggestionTemplates).result).flatMap {
      case Some(rule) =>
        ruleSuggestionTemplateForm.bindFromRequest.fold(
          formWithErrors =>
            db.run(SuggestionTemplate.all.result).map { suggestionTemplates =>
              BadRequest(html.ruleSuggestionTemplates.list(rule, suggestionTemplates, formWithErrors))
            },
          ruleSuggestionTemplate =>
            db.run(TableQuery[RulesSuggestionTemplates] += ruleSuggestionTemplate).map { _ =>
              Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
                .flashing("success" -> "The suggestion was successfully added to the rule.")
            }
        )
      case _ => Future.successful(NotFound)
    }
  }

  def remove(ruleId: Long, id: Long) = Action.async { implicit rs =>
    db.run(Rule.one(RuleID(ruleId)).result).flatMap {
      case Some(rule) =>
        db.run(SuggestionTemplate.one(SuggestionTemplateID(id)).result).map {
          case Some(suggestionTemplate) => Ok(html.ruleSuggestionTemplates.remove(rule, suggestionTemplate))
          case _ => NotFound
        }
      case _ => Future.successful(NotFound)
    }
  }

  def delete(ruleId: Long, id: Long) = Action.async { implicit rs =>
    val action = TableQuery[RulesSuggestionTemplates]
      .filter(x => x.ruleId === RuleID(ruleId) && x.suggestionTemplateId === SuggestionTemplateID(id))
      .delete

    db.run(action).map { _ =>
      Redirect(routes.RuleSuggestionTemplatesController.list(ruleId))
        .flashing("success" -> "The suggestion was successfully removed from the rule.")
    }
  }
}
