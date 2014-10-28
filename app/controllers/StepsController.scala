package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object StepsController extends Controller with UserSessionAware {
  val statementSelectionForm = Form(
    mapping(
      "selectedStatementTerms" -> list(nonEmptyText)
    )(x => x)(x => Some(x))
  )

  def medicationList = Action {
    Ok(html.steps.medicationList())
  }

  def statementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    val statements = UserSessions.relevantStatementTermsFor(token)
    val selectedStatements = UserSessions.selectedStatementTermsFor(token)
    Ok(html.steps.statementSelection(statements, selectedStatements))
  }

  def saveStatementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    statementSelectionForm.bindFromRequest.fold(
      formWithErrors => {
        val statements = UserSessions.relevantStatementTermsFor(token)
        val selectedStatements = UserSessions.selectedStatementTermsFor(token)
        BadRequest(html.steps.statementSelection(statements, selectedStatements))
      },
      selectedStatementLabels => {
        UserSessions.updateSelectedStatementTerms(token, selectedStatementLabels)
        Redirect(routes.StepsController.suggestionList())
      }
    )
  }

  def suggestionList = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    Ok(html.steps.suggestionList(UserSessions.suggestionListFor(token)))
  }

  def print = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    val drugs = UserSessions.drugListFor(token)
    val selectedStatements = UserSessions.selectedStatementTermsFor(token)
    val suggestions = UserSessions.suggestionListFor(token)

    Ok(html.steps.print(drugs, selectedStatements, suggestions))
  }
}
