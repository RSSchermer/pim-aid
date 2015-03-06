package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

case class UserToken(value: String) extends MappedTo[String]

case class Statement(termID: ExpressionTermID, text: String, selected: Boolean)

case class Suggestion(text: String, explanatoryNote: Option[String])

case class UserSession(
    token: UserToken,
    age: Option[Int])(implicit includes: Includes[UserSession])
  extends Entity[UserSession]
{
  type IdType = UserToken

  val id = Some(token)

  val drugs = many(UserSession.drugs)
  val medicationProducts = many(UserSession.medicationProducts)
  val statementTermsUserSessions = many(UserSession.statementTermsUserSessions)
  val selectedStatementTerms = many(UserSession.selectedStatementTerms)

  def buildIndependentStatements(implicit s: Session): Seq[Statement] = {
    val selection = selectedStatementTerms.getOrFetch.map(_.id).flatten

    StatementTerm.filter(_.displayCondition.isNull).list
      .map(x => Statement(x.id.get, x.statementTemplate.getOrElse(""), selection.contains(x.id.get)))
  }

  def saveIndependentStatementSelection(statements: Seq[Statement])(implicit session: Session) = {
    session.withTransaction {
      TableQuery[StatementTermsUserSessions]
        .filter(x => x.userSessionToken === token && x.conditional === false)
        .delete

      statements.filter(_.selected == true).foreach { s =>
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(token, s.termID, s.text, conditional = false))
      }
    }
  }

  def buildConditionalStatements(implicit s: Session): Seq[Statement] = {
    val parser = buildParser
    val selection = statementTermsUserSessions.getOrFetch.map(x => (x.statementTermID, x.text))

    StatementTerm.filter(_.displayCondition.isNotNull).list
      .filter { x => parser.parse(x.displayCondition.getOrElse(ConditionExpression(""))) match {
          case parser.Success(true, _) => true
          case _ => false
        }
      }
      .flatMap { x =>
        replacePlaceholders(x.statementTemplate.getOrElse("")).map { text =>
          Statement(x.id.get, text, selection.contains((x.id.get, text)))
        }
      }
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

    Rule.include(Rule.suggestionTemplates).list
      .filter(r => parser.parse(r.conditionExpression) match {
        case parser.Success(true, _) => true
        case _ => false
      })
      .flatMap(_.suggestionTemplates.getOrFetch)
      .flatMap({
        case SuggestionTemplate(_, _, text, Some(note)) =>
          (replacePlaceholders(text) zip replacePlaceholders(note)).map(x => Suggestion(x._1, Some(x._2)))
        case SuggestionTemplate(_, _, text, None) =>
          replacePlaceholders(text).map(x => Suggestion(x, None))
      })
  }

  def buildSelectedStatements(implicit session: Session): Seq[Statement] =
    statementTermsUserSessions.getOrFetch
      .map(x => Statement(x.statementTermID, x.text, x.conditional))

  private def buildParser(implicit s: Session): ConditionExpressionParser = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val products = UserSession.medicationProducts
      .include(MedicationProduct.genericTypes.include(GenericType.drugGroups)).fetchFor(token)
    val genericTypes = products.flatMap(_.genericTypes.getOrFetch)
    val genericTypeIds = genericTypes.map(_.id).flatten
    val drugGroupIds = genericTypes.flatMap(_.drugGroups.getOrFetch.map(_.id)).flatten
    val selectedStatementTermLabels = selectedStatementTerms.getOrFetch.map(_.label)

    val variableMap = expressionTerms.map(t => (t.label, t match {
      case ExpressionTerm(_, _, Some(genericTypeId), _, _, _, _, _) =>
        genericTypeIds.contains(genericTypeId)
      case ExpressionTerm(_, _, _, Some(drugGroupId), _, _, _, _) =>
        drugGroupIds.contains(drugGroupId)
      case ExpressionTerm(_, label, _, _, Some(statementTemplate), _, _, _) =>
        selectedStatementTermLabels.contains(label)
      case ExpressionTerm(_, label, _, _, _, _, Some(comparisonOperator), Some(comparedAge)) =>
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
    val products = UserSession.medicationProducts
      .include(MedicationProduct.genericTypes.include(GenericType.drugGroups)).fetchFor(token)
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

object UserSession extends EntityCompanion[UserSessions, UserSession] {
  val query = TableQuery[UserSessions]

  val drugs = toMany[Drugs, Drug](
    TableQuery[Drugs],
    _.token === _.userToken)

  val medicationProducts = toManyThrough[MedicationProducts, Drugs, MedicationProduct](
    TableQuery[Drugs] leftJoin TableQuery[MedicationProducts] on(_.resolvedMedicationProductId === _.id),
    _.token === _._1.userToken)

  val statementTermsUserSessions = toMany[StatementTermsUserSessions, StatementTermUserSession](
    TableQuery[StatementTermsUserSessions],
    _.token === _.userSessionToken)

  val selectedStatementTerms = toManyThrough[ExpressionTerms, StatementTermsUserSessions, ExpressionTerm](
    TableQuery[StatementTermsUserSessions] leftJoin TableQuery[ExpressionTerms] on(_.statementTermId === _.id),
    _.token === _._1.userSessionToken)

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
