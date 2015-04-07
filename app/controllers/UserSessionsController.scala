package controllers

import play.api.mvc._
import play.api.db.slick._

import views._
import models._

object UserSessionsController extends Controller {
  def list = DBAction { implicit rs =>
    Ok(html.userSessions.list(UserSession.list))
  }

  def show(token: String) = DBAction { implicit rs =>
    UserSession.include(
      UserSession.medicationProducts.include(
        MedicationProduct.genericTypes.include(
          GenericType.drugGroups
        )
      )
    ).find(UserToken(token)) match {
      case Some(userSession) =>
        val drugs = userSession.drugs
        val statements = userSession.buildSelectedStatements
        val suggestions = userSession.buildSuggestions

        Ok(html.userSessions.show(userSession, drugs, statements, suggestions))
      case _ => NotFound
    }
  }
}
