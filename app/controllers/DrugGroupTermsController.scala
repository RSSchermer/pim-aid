package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

import views._
import models._

object DrugGroupTermsController extends Controller {
  val drugGroups = TableQuery[DrugGroups].sortBy(_.name)
  val expressionTerms = TableQuery[ExpressionTerms]
  val drugGroupTerms = expressionTerms.filter(_.drugGroupId.isNotNull).sortBy(_.label)
  val drugGroupTermsDrugGroups = for {
    term <- drugGroupTerms
    group <- drugGroups if term.drugGroupId === group.id
  } yield (term.label, group.name)

  val drugGroupTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        label => label.matches("""[A-Za-z0-9\-_]+""")),
      "drugGroupId" -> longNumber
    )(DrugGroupTerm.apply)(DrugGroupTerm.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.list(drugGroupTermsDrugGroups.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugGroupTerms.create(drugGroupTermForm, drugGroups.list))
  }

  def save = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.create(formWithErrors, drugGroups.list)),
      drugGroupTerm => {
        expressionTerms.insert(drugGroupTerm)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    drugGroupTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.drugGroupTerms.edit(label, drugGroupTermForm.fill(term.asInstanceOf[DrugGroupTerm]),
        drugGroups.list))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    drugGroupTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugGroupTerms.edit(label, formWithErrors, drugGroups.list)),
      term => {
        expressionTerms.filter(_.label === label).map(_.drugGroupId).update(term.drugGroupId)
        Redirect(routes.DrugGroupTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    drugGroupTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.drugGroupTerms.remove(term.asInstanceOf[DrugGroupTerm]))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    expressionTerms.filter(_.label === label).delete
    Redirect(routes.DrugGroupTermsController.list())
      .flashing("success" -> "The expression term was deleted successfully.")
  }
}
