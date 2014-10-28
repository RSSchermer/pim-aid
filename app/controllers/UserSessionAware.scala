package controllers

import play.api.mvc._
import play.api.db.slick.Session

import models._

trait UserSessionAware extends Controller {
  def currentUserSession(request: Request[AnyRef])(implicit s: Session): UserSession = {
    request.session.get("token") match {
      case Some(token) => UserSessions.find(token) match {
        case Some(userSession) => userSession
        case _ => UserSessions.create()
      }
      case _ => UserSessions.create()
    }
  }

// None of the below seem to work with DBAction :(

//  case class RequestWithUserSession[A](userSession: UserSession, request: Request[A]) extends WrappedRequest[A](request)

//  object UserSessionAwareAction extends ActionBuilder[RequestWithUserSession] with ActionTransformer[Request, RequestWithUserSession] {
//    def transform[A](request: Request[A])(implicit s: Session) = Future.successful {
//      val userSession = request.session.get("token") match {
//        case Some(token) => userSessions.filter(_.token === token).firstOption match {
//          case Some(us) => us
//          case _ => createUserSession()
//        }
//        case _ => createUserSession()
//      }
//
//      new RequestWithUserSession(userSession, request)
//    }
//  }

//  case class UserSessionAwareAction[A](action: Action[A]) extends Action[A] {
//
//    def apply(request: Request[A]): Future[Result] = {
//      val userSession = request.session.get("token") match {
//        case Some(token) => userSessions.filter(_.token === token).firstOption match {
//          case Some(us) => us
//          case _ => createUserSession()
//        }
//        case _ => createUserSession()
//      }
//
//      action(RequestWithUserSession(userSession, request))
//    }
//
//    lazy val parser = action.parser
//  }

//  def UserSessionAwareAction(f: RequestWithUserSession => Result) = {
//    DBAction { implicit rs =>
//      val userSession = rs.request.session.get("token") match {
//        case Some(token) => userSessions.filter(_.token === token).firstOption match {
//          case Some(us) => us
//          case _ => createUserSession()
//        }
//        case _ => createUserSession()
//      }
//
//      f(RequestWithUserSession(userSession, rs.request))
//    }
//  }
}
