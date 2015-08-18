package model

import scala.concurrent.ExecutionContext

import entitytled.Entitytled

trait UserSessionComponent {
  self: Entitytled
    with GenericTypeComponent
    with DrugGroupComponent
    with MedicationProductComponent
    with ExpressionTermComponent
    with ConditionExpressionComponent
    with RuleComponent
    with DrugComponent
  =>

  import driver.api._

  case class UserToken(value: String) extends MappedTo[String]

  case class Statement(termID: ExpressionTermID, text: String, selected: Boolean)

  case class StatementTermUserSession(
    userSessionToken: UserToken,
    statementTermID: ExpressionTermID,
    text: String,
    conditional: Boolean)

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
    val genericTypes = many(UserSession.genericTypes)
    val drugGroups = many(UserSession.drugGroups)

    def buildIndependentStatements()(implicit ec: ExecutionContext): DBIO[Seq[Statement]] = {
      for {
        statementTerms <- StatementTerm.all.filter(_.displayCondition.isEmpty).result
        selectedTerms <- selectedStatementTerms.valueAction
      } yield {
        val selection = selectedTerms.map(_.id).flatten

        statementTerms.map { st =>
          Statement(st.id.get, st.statementTemplate.getOrElse(""), selection.contains(st.id.get))
        }
      }
    }

    def saveIndependentStatementSelection(statements: Seq[Statement])(implicit ex: ExecutionContext): DBIO[Unit] = {
      val tq = TableQuery[StatementTermsUserSessions]
      val deleteOld = tq.filter(x => x.userSessionToken === token && x.conditional === false).delete
      val insertNew = DBIO.sequence(statements.filter(_.selected == true).map { s =>
        tq += StatementTermUserSession(token, s.termID, s.text, conditional = false)
      })

      deleteOld >> insertNew >> DBIO.successful(())
    }

    def buildConditionalStatements()(implicit ex: ExecutionContext): DBIO[Seq[Statement]] =
      for {
        conditionalTerms <- StatementTerm.all.filter(_.displayCondition.isDefined).result
        parser <- buildParser()
        placeholderReplacer <- buildPlaceholderReplacer()
        stUs <- statementTermsUserSessions.valueAction
      } yield {
        val selection = stUs.map(x => (x.statementTermID, x.text))

        val statements = conditionalTerms
          .filter(t => parser.parse(t.displayCondition.getOrElse(ConditionExpression(""))).getOrElse(false))
          .flatMap { term =>
            placeholderReplacer.replacePlaceholders(term.statementTemplate.getOrElse("")).map { text =>
              Statement(term.id.get, text, selection.contains((term.id.get, text)))
            }
          }

        statements.distinct
      }

    def saveConditionalStatementSelection(statements: Seq[Statement])(implicit ec: ExecutionContext): DBIO[Unit] = {
      val tq = TableQuery[StatementTermsUserSessions]
      val deleteOld = tq.filter(x => x.userSessionToken === token && x.conditional === true).delete
      val insertNew = DBIO.sequence(statements.filter(_.selected == true).map { s =>
        tq += StatementTermUserSession(token, s.termID, s.text, conditional = true)
      })

      deleteOld >> insertNew >> DBIO.successful(())
    }

    def buildSuggestions()(implicit ec: ExecutionContext): DBIO[Seq[Suggestion]] =
      for {
        rules <- Rule.all.include(Rule.suggestionTemplates).result
        parser <- buildParser()
        placeholderReplacer <- buildPlaceholderReplacer()
      } yield {
        val suggestions = for {
          rule <- rules
          template <- rule.suggestionTemplates
          suggestion <- template.explanatoryNote match {
            case Some(note) =>
              (placeholderReplacer.replacePlaceholders(template.text) zip placeholderReplacer.replacePlaceholders(note))
                .map(x => Suggestion(x._1, Some(x._2), rule))
            case _ =>
              placeholderReplacer.replacePlaceholders(template.text).map(x => Suggestion(x, None, rule))
          }
          if parser.parse(rule.conditionExpression).getOrElse(false)
        } yield suggestion

        suggestions.distinct
      }

    def buildSelectedStatements()(implicit ec: ExecutionContext): DBIO[Seq[Statement]] =
      statementTermsUserSessions.valueAction
        .map(_.map(x => Statement(x.statementTermID, x.text, x.conditional)))

    private def buildParser()(implicit ex: ExecutionContext): DBIO[ConditionExpressionParser] =
      for {
        expressionTerms <- ExpressionTerm.all.result
        genericTypeIds <- genericTypes.valueAction.map(_.map(_.id).flatten)
        drugGroupIds <- drugGroups.valueAction.map(_.map(_.id).flatten)
        selectedTerms <- selectedStatementTerms.valueAction
      } yield {
        val selectedStatementTermLabels = selectedTerms.map(_.label)

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

    private def buildPlaceholderReplacer()(implicit ec: ExecutionContext): DBIO[PlaceholderReplacer] =
      for {
        products <- medicationProducts.valueAction
      } yield {
        val typeProductMap = medicationProducts.flatMap(p => p.genericTypes.map(t => (t, p))).toMap
        val groupProductMap = medicationProducts.flatMap(p => p.genericTypes.flatMap(_.drugGroups).map(g => (g, p))).toMap

        new PlaceholderReplacer(typeProductMap, groupProductMap)
      }
  }

  object UserSession extends EntityCompanion[UserSessions, UserSession, UserToken] {
    val drugs = toMany[Drugs, Drug]
    val medicationProducts = toManyThrough[MedicationProducts, Drugs, MedicationProduct]
    val statementTermsUserSessions = toMany[StatementTermsUserSessions, StatementTermUserSession]
    val selectedStatementTerms = toManyThrough[ExpressionTerms, StatementTermsUserSessions, ExpressionTerm]
    val genericTypes = medicationProducts compose MedicationProduct.genericTypes
    val drugGroups = genericTypes compose GenericType.drugGroups

    def generateToken(len: Int = 12): UserToken = {
      val rand = new scala.util.Random(System.nanoTime)
      val sb = new StringBuilder(len)
      val ab = "0123456789abcdefghijklmnopqrstuvwxyz"

      for (i <- 0 until len) {
        sb.append(ab(rand.nextInt(ab.length)))
      }

      UserToken(sb.toString())
    }

    def create(token: UserToken = generateToken())(implicit ec: ExecutionContext): DBIO[UserSession] = {
      val newUserSession = UserSession(token, None)
      insert(newUserSession).map(_ => newUserSession)
    }
  }

  class PlaceholderReplacer(
    typeProductMap: Map[GenericType, MedicationProduct],
    groupProductMap: Map[DrugGroup, MedicationProduct])
  {
    def replacePlaceholders(template: String): Seq[String] = {
      """\{\{(type|group)\(([^\)]+)\)\}\}""".r.findFirstMatchIn(template) match {
        case Some(m) => m.group(1).toLowerCase match {
          case "type" =>
            typeProductMap
              .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
              .map { x => template.replaceAll( s"""\\{\\{type\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
              .flatMap(replacePlaceholders)
              .toList
          case "group" =>
            groupProductMap
              .filter(x => x._1.name.toLowerCase == m.group(2).toLowerCase)
              .map { x => template.replaceAll( s"""\\{\\{group\\(${m.group(2)}\\)\\}\\}""", x._2.name) }
              .flatMap(replacePlaceholders)
              .toList
        }
        case _ => List(template)
      }
    }
  }

  class StatementTermsUserSessions(tag: Tag)
    extends Table[StatementTermUserSession](tag, "STATEMENT_TERMS_USER_SESSIONS")
  {
    def userSessionToken = column[UserToken]("user_session_token")
    def statementTermId = column[ExpressionTermID]("statement_term_id")
    def text = column[String]("text")
    def conditional = column[Boolean]("conditional", O.Default(false))

    def * = (userSessionToken, statementTermId, text, conditional) <>
      (StatementTermUserSession.tupled, StatementTermUserSession.unapply)

    def pk = primaryKey("STATEMENT_TERMS_USER_SESSIONS_PK",
      (userSessionToken, statementTermId, text, conditional))

    def userSession = foreignKey("STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK", userSessionToken,
      TableQuery[UserSessions])(_.token)
    def statementTerm = foreignKey("STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK", statementTermId,
      TableQuery[ExpressionTerms])(_.id)
  }

  class UserSessions(tag: Tag) extends EntityTable[UserSession, UserToken](tag, "USER_SESSIONS") {
    def token = column[UserToken]("token", O.PrimaryKey)
    def age = column[Option[Int]]("age")

    def id = token

    def * = (token, age) <>((UserSession.apply _).tupled, UserSession.unapply)
  }
}
