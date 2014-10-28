package models

import play.api.db.slick.Config.driver.simple._

case class StatementTermUserSession(userSessionToken: String, statementTermLabel: String)

class StatementTermsUserSessions(tag: Tag)
  extends Table[StatementTermUserSession](tag, "STATEMENT_TERMS_USER_SESSIONS")
{
  def userSessionToken = column[String]("user_session_token")
  def statementTermLabel = column[String]("statement_term_label")

  def * = (userSessionToken, statementTermLabel) <> (StatementTermUserSession.tupled, StatementTermUserSession.unapply)

  def pk = primaryKey("STATEMENT_TERMS_USER_SESSIONS_PK", (userSessionToken, statementTermLabel))
  def userSession = foreignKey("STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK", userSessionToken,
    TableQuery[UserSessions])(_.token)
  def statementTerm = foreignKey("STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK", statementTermLabel,
    TableQuery[ExpressionTerms])(_.label)
}
