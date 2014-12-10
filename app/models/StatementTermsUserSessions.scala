package models

import play.api.db.slick.Config.driver.simple._

case class StatementTermUserSession(userSessionToken: UserToken, statementTermLabel: String, textHash: String,
                                    conditional: Boolean)

class StatementTermsUserSessions(tag: Tag)
  extends Table[StatementTermUserSession](tag, "STATEMENT_TERMS_USER_SESSIONS")
{
  def userSessionToken = column[UserToken]("user_session_token")
  def statementTermLabel = column[String]("statement_term_label")
  def textHash = column[String]("text_hash")
  def conditional = column[Boolean]("conditional", O.Default(false))

  def * = (userSessionToken, statementTermLabel, textHash, conditional) <>
    (StatementTermUserSession.tupled, StatementTermUserSession.unapply)

  def pk = primaryKey("STATEMENT_TERMS_USER_SESSIONS_PK", (userSessionToken, statementTermLabel, textHash, conditional))
  def userSession = foreignKey("STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK", userSessionToken,
    TableQuery[UserSessions])(_.token)
  def statementTerm = foreignKey("STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK", statementTermLabel,
    TableQuery[ExpressionTerms])(_.label)
}
