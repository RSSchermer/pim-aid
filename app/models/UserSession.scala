package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

import scala.concurrent.ExecutionContext

case class UserToken(value: String) extends MappedTo[String]

case class Statement(termID: ExpressionTermID, text: String, selected: Boolean)

case class Suggestion(text: String, explanatoryNote: Option[String], rule: Rule)

case class UserSession(
    token: UserToken,
    age: Option[Int])(implicit includes: Includes[UserSession])
  extends Entity[UserSession, UserToken]
{
  val id = Some(token)

  val drugs = many(UserSession.drugs)
  val medicationProducts = many(UserSession.medicationProducts)
  val statementTermsUserSessions = many(UserSession.statementTermsUserSessions)
  val selectedStatementTerms = many(UserSession.selectedStatementTerms)

  val genericTypes = medicationProducts.flatMap(_.genericTypes)
  val drugGroups = genericTypes.flatMap(_.drugGroups)

  def buildIndependentStatements(implicit ec: ExecutionContext): DBIO[Seq[Statement]] = {
    val selection = selectedStatementTerms.map(_.id).flatten

    for {
      statementTerms <- StatementTerm.all.filter(_.displayCondition.isNull).result
    } yield statementTerms.map { st =>
      Statement(st.id.get, st.statementTemplate.getOrElse(""), selection.contains(st.id.get))
    }
  }

  def saveIndependentStatementSelection(statements: Seq[Statement])
                                       (implicit ex: ExecutionContext)
  : DBIO[Unit] = {
    val StUs = TableQuery[StatementTermsUserSessions]
    val deleteOld = StUs.filter(x => x.userSessionToken === token && x.conditional === false).delete
    val insertNew = DBIO.sequence(statements.filter(_.selected == true).map { s =>
      StUs += StatementTermUserSession(token, s.termID, s.text, conditional = false)
    })

    deleteOld >> insertNew
  }

  def buildConditionalStatements(implicit ex: ExecutionContext): Seq[Statement] = {
    val parser = buildParser
    val selection = statementTermsUserSessions.map(x => (x.statementTermID, x.text))

    val statements = for {
      term <- StatementTerm.filter(_.displayCondition.isNotNull).list
      text <- replacePlaceholders(term.statementTemplate.getOrElse(""))
      if parser.parse(term.displayCondition.getOrElse(ConditionExpression(""))).getOrElse(false)
    } yield Statement(term.id.get, text, selection.contains((term.id.get, text)))

    statements.distinct
  }

  def saveConditionalStatementSelection(statements: Seq[Statement])(implicit session: Session) = {
    session.withTransaction {
      TableQuery[StatementTermsUserSessions]
        .filter(x => x.userSessionToken === token && x.conditional === true)
        .delete

      statements.filter(_.selected == true).foreach { s =>
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(token, s.termID, s.text, conditional = true))
      }
    }
  }

  def buildSuggestions(implicit session: Session): Seq[Suggestion] = {
    val parser = buildParser

    val suggestions = for {
      rule <- Rule.include(Rule.suggestionTemplates).list
      template <- rule.suggestionTemplates
      suggestion <- template.explanatoryNote match {
        case Some(note) =>
          (replacePlaceholders(template.text) zip replacePlaceholders(note))
            .map(x => Suggestion(x._1, Some(x._2), rule))
        case _ =>
          replacePlaceholders(template.text).map(x => Suggestion(x, None, rule))
      }
      if parser.parse(rule.conditionExpression).getOrElse(false)
    } yield suggestion

    suggestions.distinct
  }

  def buildSelectedStatements(implicit session: Session): Seq[Statement] =
    statementTermsUserSessions.map(x => Statement(x.statementTermID, x.text, x.conditional))

  private def buildParser(implicit ex: ExecutionContext): ConditionExpressionParser = {
    val expressionTerms = ExpressionTerm.all.result
    val genericTypeIds = genericTypes.map(_.id).flatten
    val drugGroupIds = drugGroups.map(_.id).flatten
    val selectedStatementTermLabels = selectedStatementTerms.map(_.label)

    val variableMap = expressionTerms.map(t => (t.label, t match {
      case ExpressionTerm(_, _, Some(genericTypeId), _, _, _, _, _) =>
        genericTypeIds.contains(genericTypeId)
      case ExpressionTerm(_, _, _, Some(drugGroupId), _, _, _, _) =>
        drugGroupIds.contains(drugGroupId)
      case ExpressionTerm(_, label, _, _, Some(statementTemplate), _, _, _) =>
        selectedStatementTermLabels.contains(label)
      case ExpressionTerm(_, _, _, _, _, _, Some(comparisonOperator), Some(comparedAge)) =>
        compareAge(comparisonOperator, comparedAge)
    })).toMap

    new ConditionExpressionParser(variableMap)
  }

  private def compareAge(comparisonOperator: String, comparedAge: Int) = {
    val userAge = age.getOrElse(0)

    comparisonOperator match {
      case "==" => userAge == comparedAge
      case ">" => userAge > comparedAge
      case ">=" => userAge >= comparedAge
      case "<" => userAge < comparedAge
      case "<=" => userAge <= comparedAge
      case _ => false
    }
  }

  private def replacePlaceholders(template: String)(implicit s: Session): Seq[String] = {
    val typesProducts = medicationProducts.flatMap(p => p.genericTypes.map(t => (t, p)))
    val groupsProducts = medicationProducts.flatMap { p =>
      p.genericTypes.flatMap(_.drugGroups).map(g => (g, p))
    }

    """\{\{(type|group)\(([^\)]+)\)\}\}""".r.findFirstMatchIn(template) match {
      case Some(m) => m.group(1).toLowerCase match {
        case "type" =>
          typesProducts
            .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
            .map { x => template.replaceAll(s"""\\{\\{type\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
            .flatMap(replacePlaceholders)
        case "group" =>
          groupsProducts
            .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
            .map { x => template.replaceAll(s"""\\{\\{group\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
            .flatMap(replacePlaceholders)
      }
      case _ => List(template)
    }
  }
}

object UserSession extends EntityCompanion[UserSessions, UserSession, UserToken] {
  val drugs = toMany[Drugs, Drug]
  val medicationProducts = toManyThrough[MedicationProducts, Drugs, MedicationProduct]
  val statementTermsUserSessions = toMany[StatementTermsUserSessions, StatementTermUserSession]
  val selectedStatementTerms = toManyThrough[ExpressionTerms, StatementTermsUserSessions, ExpressionTerm]

  def generateToken(len: Int = 12): UserToken = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new StringBuilder(len)
    val ab = "0123456789abcdefghijklmnopqrstuvwxyz"

    for (i <- 0 until len) {
      sb.append(ab(rand.nextInt(ab.length)))
    }

    UserToken(sb.toString())
  }

  def create(token: UserToken = generateToken()): DBIO[UserSession] = {
    val newUserSession = UserSession(token, None)
    insert(newUserSession).map(_ => newUserSession)
  }
}
