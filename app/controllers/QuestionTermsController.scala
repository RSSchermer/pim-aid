package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

import views._
import models._

object QuestionTermsController extends Controller {
  val expressionTerms = TableQuery[ExpressionTerms]
  val questionTerms = expressionTerms.filter(_.question.isNotNull).sortBy(_.label)

  val questionTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        label => label.matches("""[A-Za-z0-9\-_]+""")),
      "question" -> nonEmptyText
    )(QuestionTerm.apply)(QuestionTerm.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.questionTerms.list(questionTerms.list.asInstanceOf[List[QuestionTerm]]))
  }

  def create = DBAction { implicit rs =>
    Ok(html.questionTerms.create(questionTermForm))
  }

  def save = DBAction { implicit rs =>
    questionTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.questionTerms.create(formWithErrors)),
      questionTerm => {
        expressionTerms.insert(questionTerm)
        Redirect(routes.QuestionTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    questionTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.questionTerms.edit(label, questionTermForm.fill(term.asInstanceOf[QuestionTerm])))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    questionTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.questionTerms.edit(label, formWithErrors)),
      term => {
        expressionTerms.filter(_.label === label).map(_.question).update(term.question)
        Redirect(routes.QuestionTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    questionTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.questionTerms.remove(term.asInstanceOf[QuestionTerm]))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    expressionTerms.filter(_.label === label).delete
    Redirect(routes.QuestionTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
