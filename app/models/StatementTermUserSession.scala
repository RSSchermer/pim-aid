package models

case class StatementTermUserSession(
    userSessionToken: UserToken,
    statementTermLabel: String,
    text: String,
    conditional: Boolean)
