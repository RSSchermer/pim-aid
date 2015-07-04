package controllers

import constraints.ConditionExpressionConstraint
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

object RulesController extends Controller {
  def ruleForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => RuleID(id),
        (ruleId: RuleID) => ruleId.value
      )),
      "name" -> nonEmptyText,
      "conditionExpression" -> nonEmptyText.verifying(ConditionExpressionConstraint.apply).transform(
        (s: String) => ConditionExpression(s),
        (ce: ConditionExpression) => ce.value
      ),
      "source" -> optional(text),
      "formalizationReference" -> optional(text),
      "note" -> optional(text)
    )(Rule.apply)(Rule.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(Rule.all.include(Rule.suggestionTemplates).result).map { rules =>
      Ok(html.rules.list(rules))
    }
  }

  def create = Action.async { implicit rs =>
    db.run(ExpressionTerm.all.result).map { terms =>
      Ok(html.rules.create(terms, ruleForm))
    }
  }

  def save = Action.async { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(ExpressionTerm.all.result).map { terms =>
          BadRequest(html.rules.create(terms, formWithErrors))
        },
      rule =>
        db.run(Rule.insert(rule)).map { id =>
          Redirect(routes.RuleSuggestionTemplatesController.list(id.value))
            .flashing("success" -> "The rule was created successfully.")
        }
    )
  }

  def edit(id: Long) = Action.async { implicit rs =>
    db.run(for {
      ruleOption <- Rule.one(RuleID(id)).result
      terms <- ExpressionTerm.all.result
    } yield ruleOption match {
      case Some(rule) => Ok(html.rules.edit(RuleID(id), terms, ruleForm.fill(rule)))
      case _ => NotFound
    })
  }

  def update(id: Long) = Action.async { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(ExpressionTerm.all.result).map { terms =>
          BadRequest(html.rules.edit(RuleID(id), terms, formWithErrors))
        },
      rule =>
        db.run(Rule.update(rule)).map { _ =>
          Redirect(routes.RulesController.list())
            .flashing("success" -> "The rule was updated successfully.")
        }
    )
  }

  def remove(id: Long) = Action.async { implicit rs =>
    db.run(Rule.one(RuleID(id)).result).map {
      case Some(rule) => Ok(html.rules.remove(rule))
      case _ => NotFound
    }
  }

  def delete(id: Long) = Action.async { implicit rs =>
    db.run(Rule.delete(RuleID(id))).map { _ =>
      Redirect(routes.RulesController.list())
        .flashing("success" -> "The rule was deleted successfully.")
    }
  }
}
