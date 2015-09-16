package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import model.PIMAidDBContext._

class UserSessionAwareRequest[A](val userSession: UserSession, request: Request[A]) extends WrappedRequest[A](request)

object UserSessionAwareAction
  extends ActionBuilder[UserSessionAwareRequest] with ActionTransformer[Request, UserSessionAwareRequest]
{
  def transform[A](request: Request[A]): Future[UserSessionAwareRequest[A]] = {
    val userSessionFuture = request.session.get("token") match {
      case Some(token) =>
        db.run(UserSession.one(UserToken(token)).include(
          UserSession.medicationProducts.include(
            MedicationProduct.genericTypes.include(
              GenericType.drugGroups
            )
          )
        ).result).flatMap {
          case Some(userSession) => Future.successful(userSession)
          case _ => db.run(UserSession.create())
        }
      case _ => db.run(UserSession.create())
    }

    userSessionFuture.map(new UserSessionAwareRequest(_, request))
  }
}
