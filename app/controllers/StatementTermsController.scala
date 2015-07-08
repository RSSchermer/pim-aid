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
import constraints.{MedicationProductTemplateConstraint, ConditionExpressionConstraint}

object StatementTermsController extends Controller {
  def statementTermForm = Form(
    mapping(
      "id" -> optional(longNumber.transform(
        (id: Long) => ExpressionTermID(id),
        (id: ExpressionTermID) => id.value
      )),
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        _.matches("""[A-Za-z0-9\-_]+""")),
      "statementTemplate" -> nonEmptyText.verifying(MedicationProductTemplateConstraint.apply),
      "displayCondition" -> optional(text.verifying(ConditionExpressionConstraint.apply).transform(
        (s: String) => ConditionExpression(s),
        (ce: ConditionExpression) => ce.value
      ))
    )(StatementTerm.apply)(StatementTerm.unapply)
  )

  def list = Action.async { implicit rs =>
    db.run(StatementTerm.all.result).map { terms =>
      Ok(html.statementTerms.list(terms))
    }
  }

  def create = Action.async { implicit rs =>
    db.run(ExpressionTerm.all.result).map { terms =>
      Ok(html.statementTerms.create(terms, statementTermForm))
    }
  }

  def save = Action.async { implicit rs =>
    statementTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(ExpressionTerm.all.result).map { terms =>
          BadRequest(html.statementTerms.create(terms, formWithErrors))
        },
      statementTerm =>
        db.run(StatementTerm.insert(statementTerm)).map { _ =>
          Redirect(routes.StatementTermsController.list())
            .flashing("success" -> "The expression term was created successfully.")
        }
    )
  }

  def edit(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(for {
      termOption <- StatementTerm.one(id).result
      terms <- ExpressionTerm.all.result
    } yield termOption match {
      case Some(term) =>
        Ok(html.statementTerms.edit(id, terms, statementTermForm.fill(term)))
      case _ =>
        NotFound
    })
  }

  def update(id: ExpressionTermID) = Action.async { implicit rs =>
    statementTermForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(ExpressionTerm.all.result).map { terms =>
          BadRequest(html.statementTerms.edit(id, terms, formWithErrors))
        },
      term =>
        db.run(StatementTerm.update(term)).map { _ =>
          Redirect(routes.StatementTermsController.list())
            .flashing("success" -> "The expression term was updated successfully.")
        }
    )
  }

  def remove(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(StatementTerm.one(id).result).map {
      case Some(term) =>
        Ok(html.statementTerms.remove(term))
      case _ =>
        NotFound
    }
  }

  def delete(id: ExpressionTermID) = Action.async { implicit rs =>
    db.run(StatementTerm.delete(id)).map { _ =>
      Redirect(routes.StatementTermsController.list())
        .flashing("success" -> "The expression term was deleted successfully.")
    }
  }
}
