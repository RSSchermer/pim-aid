package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import ORM._
import schema._
import utils._

case class UserToken(value: String) extends MappedTo[String]

case class Statement(termLabel: String, text: String, selected: Boolean)

case class Suggestion(text: String, explanatoryNote: Option[String])

case class UserSession(
    token: UserToken,
    age: Option[Int],
    drugs: Many[UserSessions, Drugs, UserSession, Drug] =
      ManyUnfetched(UserSession.drugs),
    medicationProducts: Many[UserSessions, MedicationProducts, UserSession, MedicationProduct] =
      ManyUnfetched(UserSession.medicationProducts),
    statementTermsUserSessions: Many[UserSessions, StatementTermsUserSessions, UserSession, StatementTermUserSession] =
      ManyUnfetched(UserSession.statementTermsUserSessions))
  extends Entity[UserToken]
{
  type IdType = UserToken

  val id = Some(token)

  def buildIndependentStatements(implicit s: Session): Seq[Statement] = {
    val selectedStatementTermLabels =
      statementTermsUserSessions.getOrFetch.map(_.statementTermLabel)

    StatementTerm.filter(_.displayCondition.isNull).list
      .map(x => Statement(x.label, x.statementTemplate.getOrElse(""), selectedStatementTermLabels.contains(x.label)))
  }


  def saveIndependentStatementSelection(statements: Seq[Statement])(implicit session: Session) = {
    session.withTransaction {
      TableQuery[StatementTermsUserSessions]
        .filter(x => x.userSessionToken === token && x.conditional === false)
        .delete

      statements.foreach { s =>
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(token, s.termLabel, s.text, conditional = false))
      }
    }
  }

  def buildConditionalStatements(implicit s: Session): Seq[Statement] = {
    val parser = buildParser
    val selection = statementTermsUserSessions.getOrFetch.map(x => (x.statementTermLabel, x.textHash))

    StatementTerm.filter(_.displayCondition.isNotNull).list
      .filter { x => parser.parse(x.displayCondition.getOrElse("")) match {
          case parser.Success(true, _) => true
          case _ => false
        }
      }
      .flatMap { x =>
        replacePlaceholders(x.statementTemplate.getOrElse("")).map { text =>
          Statement(x.label, text, selection.contains((x.label, text)))
        }
      }
  }

  def saveConditionalStatementSelection(statements: Seq[Statement])(implicit session: Session) = {
    session.withTransaction {
      TableQuery[StatementTermsUserSessions]
        .filter(x => x.userSessionToken === token && x.conditional === true)
        .delete

      statements.foreach { s =>
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(token, s.termLabel, s.text, conditional = true))
      }
    }
  }

  def buildSuggestions(implicit session: Session): Seq[Suggestion] = {
    val parser = buildParser

    Rule.list.filter(r => parser.parse(r.conditionExpression) match {
        case parser.Success(true, _) => true
        case _ => false
      })
      .flatMap(_.suggestionTemplates.getOrFetch)
      .flatMap({
        case SuggestionTemplate(_, _, text, Some(note), _) =>
          (replacePlaceholders(text) zip replacePlaceholders(note)).map(x => Suggestion(x._1, Some(x._2)))
        case SuggestionTemplate(_, _, text, None, _) =>
          replacePlaceholders(text).map(x => Suggestion(x, None))
      })
  }

  def buildSelectedStatements(implicit session: Session): Seq[Statement] =
    statementTermsUserSessions.getOrFetch
      .map(x => Statement(x.statementTermLabel, x.textHash, x.conditional))

  private def buildParser(implicit s: Session): ConditionExpressionParser = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val genericTypes = medicationProducts.getOrFetch.flatMap(_.genericTypes.getOrFetch)
    val genericTypeIds = genericTypes.map(_.id)
    val drugGroupIds = genericTypes.flatMap(_.drugGroups.getOrFetch.map(_.id))
    val selectedStatementTermLabels =
      statementTermsUserSessions.getOrFetch.map(_.statementTermLabel)

    val variableMap = expressionTerms.map(t => (t.label, t match {
      case ExpressionTerm(_, Some(genericTypeId), _, _, _, _, _, _, _) =>
        genericTypeIds.contains(genericTypeId)
      case ExpressionTerm(_, _, Some(drugGroupId), _, _, _, _, _, _) =>
        drugGroupIds.contains(drugGroupId)
      case ExpressionTerm(label, _, _, Some(statementTemplate), _, _, _, _, _) =>
        selectedStatementTermLabels.contains(label)
      case ExpressionTerm(label, _, _, _, _, Some(comparisonOperator), Some(comparedAge), _, _) =>
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
    val products = medicationProducts.getOrFetch
    val typesProducts = products.flatMap(p => p.genericTypes.getOrFetch.map(t => (t, p)))
    val groupsProducts = products
      .flatMap(p => p.genericTypes.getOrFetch.flatMap(_.drugGroups.getOrFetch).map(g => (g, p)))

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

object UserSession extends EntityCompanion[UserSession, UserSessions] {
  val query = TableQuery[UserSessions]

  val drugs = toMany[Drug, Drugs](TableQuery[Drugs], _.token === _.userToken, lenser(_.drugs))

  val medicationProducts = toManyThrough[MedicationProduct, Drug, MedicationProducts, Drugs](
    TableQuery[Drugs] leftJoin TableQuery[MedicationProducts] on(_.resolvedMedicationProductId === _.id),
    _.token === _._1.userToken,
    lenser(_.medicationProducts)
  )

  val statementTermsUserSessions = toMany[StatementTermUserSession, StatementTermsUserSessions](
    TableQuery[StatementTermsUserSessions],
    _.token === _.userSessionToken,
    lenser(_.statementTermsUserSessions)
  )

  def generateToken(len: Int = 12): UserToken = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new StringBuilder(len)
    val ab = "0123456789abcdefghijklmnopqrstuvwxyz"

    for (i <- 0 until len) {
      sb.append(ab(rand.nextInt(ab.length)))
    }

    UserToken(sb.toString())
  }

  def create(token: UserToken = generateToken())(implicit s: Session): UserSession = {
    val newUserSession = UserSession(token, None)
    insert(newUserSession)
    newUserSession
  }
}
