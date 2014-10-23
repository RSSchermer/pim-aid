package models

import play.api.db.slick.Config.driver.simple._

case class UserSession(token: String)

class UserSessions(tag: Tag) extends Table[UserSession](tag, "USER_SESSIONS") {
  def token = column[String]("token", O.PrimaryKey)

  def * = token <> (UserSession, UserSession.unapply)
}
