package controllers

import constraints.ConditionExpressionConstraint
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._
import models.meta.Profile.driver.simple.Session

object RulesController extends Controller {
  def ruleForm(implicit s: Session) = Form(
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

  def list = DBAction { implicit rs =>
    Ok(html.rules.list(Rule.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.rules.create(ruleForm))
  }

  def save = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.create(formWithErrors)),
      rule => {
        Rule.insert(rule)
        Redirect(routes.RulesController.list())
          .flashing("success" -> "The rule was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    Rule.find(RuleID(id)) match {
      case Some(rule) => Ok(html.rules.edit(RuleID(id), ruleForm.fill(rule)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.edit(RuleID(id), formWithErrors)),
      rule => {
        Rule.update(rule)
        Redirect(routes.RulesController.list())
          .flashing("success" -> "The rule was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    Rule.find(RuleID(id)) match {
      case Some(rule) => Ok(html.rules.remove(rule))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    Rule.delete(RuleID(id))
    Redirect(routes.RulesController.list())
      .flashing("success" -> "The rule was deleted successfully.")
  }
}
