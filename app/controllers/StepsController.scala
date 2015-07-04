package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import views._
import models._
import models.meta.Profile._

object StepsController extends Controller {
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

  def generalInformation = UserSessionAwareAction { implicit rs =>
    rs.userSession match {
      case session@UserSession(token, Some(age)) =>
        Ok(html.steps.generalInformation(token, generalInformationForm.fill(session)))
          .withSession("token" -> token.value)
      case UserSession(token, None) =>
        Ok(html.steps.generalInformation(token, generalInformationForm))
          .withSession("token" -> token.value)
    }
  }

  def saveGeneralInformation = UserSessionAwareAction.async { implicit rs =>
    val token = rs.userSession.token

    generalInformationForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(html.steps.generalInformation(token, formWithErrors))),
      userSession =>
        db.run(UserSession.update(userSession)).map { _ =>
          Redirect(routes.StepsController.medicationList())
        }
    )
  }

  def medicationList = UserSessionAwareAction { implicit rs =>
    Ok(html.steps.medicationList()).withSession("token" -> rs.userSession.token.value)
  }

  def independentStatementSelection = UserSessionAwareAction.async { implicit rs =>
    db.run(rs.userSession.buildIndependentStatements()).map { statements =>
      Ok(html.steps.independentStatementSelection(statements))
        .withSession("token" -> rs.userSession.token.value)
    }
  }

  def saveIndependentStatementSelection = UserSessionAwareAction.async { implicit rs =>
    statementSelectionForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(rs.userSession.buildIndependentStatements()).map { statements =>
          BadRequest(html.steps.independentStatementSelection(statements))
        },
      statements =>
        db.run(rs.userSession.saveIndependentStatementSelection(statements)).map { _ =>
          Redirect(routes.StepsController.conditionalStatementSelection())
        }
    )
  }

  def conditionalStatementSelection = UserSessionAwareAction.async { implicit rs =>
    db.run(rs.userSession.buildConditionalStatements()).map { statements =>
      Ok(html.steps.conditionalStatementSelection(statements))
        .withSession("token" -> rs.userSession.token.value)
    }
  }

  def saveConditionalStatementSelection = UserSessionAwareAction.async { implicit rs =>
    statementSelectionForm.bindFromRequest.fold(
      formWithErrors =>
        db.run(rs.userSession.buildConditionalStatements()).map { statements =>
          BadRequest(html.steps.conditionalStatementSelection(statements))
        },
      statements =>
        db.run(rs.userSession.saveConditionalStatementSelection(statements)).map { _ =>
          Redirect(routes.StepsController.suggestionList())
        }
    )
  }

  def suggestionList = UserSessionAwareAction.async { implicit rs =>
    db.run(rs.userSession.buildSuggestions()).map { suggestions =>
      Ok(html.steps.suggestionList(suggestions))
        .withSession("token" -> rs.userSession.token.value)
    }
  }

  def print = UserSessionAwareAction.async { implicit rs =>
    db.run(for{
      drugs <- rs.userSession.drugs.valueAction
      statements <- rs.userSession.buildSelectedStatements()
      suggestions <- rs.userSession.buildSuggestions()
    } yield {
      Ok(html.steps.print(drugs, statements, suggestions))
        .withSession("token" -> rs.userSession.token.value)
    })
  }
}
