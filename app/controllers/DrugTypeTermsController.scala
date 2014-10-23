package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

import views._
import models._

object DrugTypeTermsController extends Controller {
  val drugTypes = TableQuery[DrugTypes].sortBy(_.name)
  val expressionTerms = TableQuery[ExpressionTerms]
  val drugTypeTerms = expressionTerms.filter(_.drugTypeId.isNotNull).sortBy(_.label)
  val drugTypeTermsDrugTypes = for {
    term <- drugTypeTerms
    drug <- drugTypes if term.drugTypeId === drug.id
  } yield (term.label, drug.name)

  val drugTypeTermForm = Form(
    mapping(
      "label" -> nonEmptyText.verifying("Must alphanumeric characters, dashes and underscores only.",
        label => label.matches("""[A-Za-z0-9\-_]+""")),
      "drugTypeId" -> longNumber
    )(DrugTypeTerm.apply)(DrugTypeTerm.unapply)
  )

  def list = DBAction { implicit rs =>
    Ok(html.drugTypeTerms.list(drugTypeTermsDrugTypes.list))
  }

  def create = DBAction { implicit rs =>
    Ok(html.drugTypeTerms.create(drugTypeTermForm, drugTypes.list))
  }

  def save = DBAction { implicit rs =>
    drugTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypeTerms.create(formWithErrors, drugTypes.list)),
      drugTypeTerm => {
        expressionTerms.insert(drugTypeTerm)
        Redirect(routes.DrugTypeTermsController.list())
          .flashing("success" -> "The expression term was created successfully.")
      }
    )
  }

  def edit(label: String) = DBAction { implicit rs =>
    drugTypeTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.drugTypeTerms.edit(label, drugTypeTermForm.fill(term.asInstanceOf[DrugTypeTerm]),
        drugTypes.list))
      case _ => NotFound
    }
  }

  def update(label: String) = DBAction { implicit rs =>
    drugTypeTermForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.drugTypeTerms.edit(label, formWithErrors, drugTypes.list)),
      term => {
        expressionTerms.filter(_.label === label).map(_.drugTypeId).update(term.drugTypeId)
        Redirect(routes.DrugTypeTermsController.list())
          .flashing("success" -> "The expression term was updated successfully.")
      }
    )
  }

  def remove(label: String) = DBAction { implicit rs =>
    drugTypeTerms.filter(_.label === label).firstOption match {
      case Some(term) => Ok(html.drugTypeTerms.remove(term.asInstanceOf[DrugTypeTerm]))
      case _ => NotFound
    }
  }

  def delete(label: String) = DBAction { implicit rs =>
    expressionTerms.filter(_.label === label).delete
    Redirect(routes.DrugTypeTermsController.list()).flashing("success" -> "The expression term was deleted successfully.")
  }
}
