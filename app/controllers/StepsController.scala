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
        "termID" -> longNumber.transform(
          (id: Long) => ExpressionTermID(id),
          (id: ExpressionTermID) => id.value
        ),
        "text" -> nonEmptyText,
        "selected" -> boolean
      )(Statement.apply)(Statement.unapply))
    )(x => x)(x => Some(x))
  )

  def generalInformation = DBAction { implicit rs =>
    currentUserSession(rs) match {
      case session@UserSession(token, Some(age)) =>
        Ok(html.steps.generalInformation(token, generalInformationForm.fill(session)))
          .withSession("token" -> token.value)
      case UserSession(token, None) =>
        Ok(html.steps.generalInformation(token, generalInformationForm))
          .withSession("token" -> token.value)
    }
  }

  def saveGeneralInformation = DBAction { implicit rs =>
    val token = currentUserSession(rs).token

    generalInformationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.steps.generalInformation(token, formWithErrors)),
      userSession => {
        UserSession.update(userSession)
        Redirect(routes.StepsController.medicationList())
      }
    )
  }

  def medicationList = DBAction { implicit rs =>
    Ok(html.steps.medicationList())
      .withSession("token" -> currentUserSession(rs).token.value)
  }

  def independentStatementSelection = DBAction { implicit rs =>
    val statements = currentUserSession(rs).buildIndependentStatements

    Ok(html.steps.independentStatementSelection(statements))
      .withSession("token" -> currentUserSession(rs).token.value)
  }

  def saveIndependentStatementSelection = DBAction { implicit rs =>
    val userSession = currentUserSession(rs)

    statementSelectionForm.bindFromRequest.fold(
      formWithErrors => {
        val statements = userSession.buildIndependentStatements
        BadRequest(html.steps.independentStatementSelection(statements))
      },
      statements => {
        userSession.saveIndependentStatementSelection(statements)
        Redirect(routes.StepsController.conditionalStatementSelection())
      }
    )
  }

  def conditionalStatementSelection = DBAction { implicit rs =>
    val statements = currentUserSession(rs).buildConditionalStatements
    Ok(html.steps.conditionalStatementSelection(statements))
      .withSession("token" -> currentUserSession(rs).token.value)
  }

  def saveConditionalStatementSelection = DBAction { implicit rs =>
    val userSession = currentUserSession(rs)

    statementSelectionForm.bindFromRequest.fold(
      formWithErrors => {
        val statements = userSession.buildConditionalStatements
        BadRequest(html.steps.conditionalStatementSelection(statements))
      },
      statements => {
        userSession.saveConditionalStatementSelection(statements)
        Redirect(routes.StepsController.suggestionList())
      }
    )
  }

  def suggestionList = DBAction { implicit rs =>
    Ok(html.steps.suggestionList(currentUserSession(rs).buildSuggestions))
      .withSession("token" -> currentUserSession(rs).token.value)
  }

  def print = DBAction { implicit rs =>
    val userSession = currentUserSession(rs)
    val drugs = userSession.drugs
    val statements = userSession.buildSelectedStatements
    val suggestions = userSession.buildSuggestions

    Ok(html.steps.print(drugs, statements, suggestions))
      .withSession("token" -> userSession.token.value)
  }
}
