package controllers

import play.api.mvc._
import play.api.db.slick.Session
import play.api.db.slick.Config.driver.simple._

import models._

trait UserSessionAware extends Controller {
  val userSessions = TableQuery[UserSessions]

  def generateToken(len: Int = 6): String = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new StringBuilder(len)
    val ab = "0123456789abcdefghijklmnopqrstuvwxyz"

    for (i <- 0 until len) {
      sb.append(ab(rand.nextInt(ab.length)))
    }

    sb.toString()
  }

  def createUserSession(token: String = generateToken())(implicit s: Session): UserSession = {
    val newUserSession = new UserSession(token)
    userSessions.insert(newUserSession)
    newUserSession
  }

  def currentUserSession(token: Option[String])(implicit s: Session): UserSession = {
    token match {
      case Some(t) => userSessions.filter(_.token === t).firstOption match {
        case Some(userSession) => userSession
        case _ => createUserSession()
      }
      case _ => createUserSession()
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
