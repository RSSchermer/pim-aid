package controllers

import play.api.mvc._
import play.api.db.slick.Session

import models._

trait UserSessionAware extends Controller {
  def currentUserSession(request: Request[AnyRef])(implicit s: Session): UserSession = {
    request.session.get("token") match {
      case Some(token) =>
        UserSession.find(UserToken(token)) match {
          case Some(userSession) => userSession
          case _ => UserSession.create()
        }
      case _ => UserSession.create()
    }
  }
}
