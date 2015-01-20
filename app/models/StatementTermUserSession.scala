package models

case class StatementTermUserSession(
    userSessionToken: UserToken,
    statementTermLabel: String,
    textHash: String,
    conditional: Boolean)
