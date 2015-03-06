package models

case class StatementTermUserSession(
    userSessionToken: UserToken,
    statementTermID: ExpressionTermID,
    text: String,
    conditional: Boolean)
