package controllers

import constraints.ConditionExpressionConstraint
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Session

import views._
import models._

object RulesController extends Controller {
  def ruleForm(implicit s: Session) = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => RuleID(id),
        (ruleId: RuleID) => ruleId.value
      )),
      "name" -> nonEmptyText,
      "conditionExpression" -> nonEmptyText.verifying(ConditionExpressionConstraint.apply),
      "source" -> optional(text),
      "note" -> optional(text)
    )(Rule.apply)(Rule.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.rules.list(Rules.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.rules.create(ruleForm))
  }

  def save = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.create(formWithErrors)),
      rule => {
        Rules.insert(rule)
        Redirect(routes.RulesController.list())
          .flashing("success" -> "The rule was created successfully.")
      }
    )
  }

  def edit(id: Long) = DBAction { implicit rs =>
    Rules.find(RuleID(id)) match {
      case Some(rule) => Ok(html.rules.edit(RuleID(id), ruleForm.fill(rule)))
      case _ => NotFound
    }
  }

  def update(id: Long) = DBAction { implicit rs =>
    ruleForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.rules.edit(RuleID(id), formWithErrors)),
      rule => {
        Rules.update(RuleID(id), rule)
        Redirect(routes.RulesController.list())
          .flashing("success" -> "The rule was updated successfully.")
      }
    )
  }

  def remove(id: Long) = DBAction { implicit rs =>
    Rules.find(RuleID(id)) match {
      case Some(rule) => Ok(html.rules.remove(rule))
      case _ => NotFound
    }
  }

  def delete(id: Long) = DBAction { implicit rs =>
    Rules.delete(RuleID(id))
    Redirect(routes.RulesController.list())
      .flashing("success" -> "The rule was deleted successfully.")
  }
}
