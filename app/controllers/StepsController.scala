package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._

import views._
import models._

object StepsController extends Controller with UserSessionAware {
  val generalInformationForm = Form(
    mapping(
      "userToken" -> nonEmptyText.transform(
        (token: String) => UserToken(token),
        (userToken: UserToken) => userToken.value
      ),
      "age" -> number(min = 0, max = 120).transform(
        (num: Int) => { Some(num) } : Option[Int],
        (age: Option[Int]) => age.getOrElse(0)
      )
    )(UserSession.apply)(UserSession.unapply)
  )

  val statementSelectionForm = Form(
    mapping(
      "selectedStatements" -> list(mapping(
        "termLabel" -> nonEmptyText,
        "text" -> nonEmptyText,
        "selected" -> boolean
      )(Statement.apply)(Statement.unapply))
    )(x => x)(x => Some(x))
  )

  def generalInformation = DBAction { implicit rs =>
    currentUserSession(rs) match {
      case session@UserSession(token, Some(age)) =>
        Ok(html.steps.generalInformation(token, generalInformationForm.fill(session)))
      case UserSession(token, None) =>
        Ok(html.steps.generalInformation(token, generalInformationForm))
    }
  }

  def saveGeneralInformation = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    generalInformationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.steps.generalInformation(token, formWithErrors)),
      userSession => {
        UserSessions.update(token, userSession)
        Redirect(routes.StepsController.medicationList())
      }
    )
  }

  def medicationList = Action {
    Ok(html.steps.medicationList())
  }

  def unconditionalStatementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    val statements = UserSessions.unconditionalStatementListFor(token)

    Ok(html.steps.unconditionalStatementSelection(statements))
  }

  def saveUnconditionalStatementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    statementSelectionForm.bindFromRequest.fold(
      formWithErrors => {
        val statementList = UserSessions.unconditionalStatementListFor(token)
        BadRequest(html.steps.unconditionalStatementSelection(statementList))
      },
      statements => {
        UserSessions.updateSelectedUnconditionalStatements(token, statements)
        Redirect(routes.StepsController.conditionalStatementSelection())
      }
    )
  }

  def conditionalStatementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token
    val statements = UserSessions.conditionalStatementListFor(token)
    Ok(html.steps.conditionalStatementSelection(statements))
  }

  def saveConditionalStatementSelection = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    statementSelectionForm.bindFromRequest.fold(
      formWithErrors => {
        val statementList = UserSessions.conditionalStatementListFor(token)
        BadRequest(html.steps.conditionalStatementSelection(statementList))
      },
      statements => {
        UserSessions.updateSelectedConditionalStatements(token, statements)
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
    val selectedStatements = UserSessions.selectedStatementListFor(token)
    val suggestions = UserSessions.suggestionListFor(token)

    Ok(html.steps.print(drugs, selectedStatements, suggestions))
  }
}
