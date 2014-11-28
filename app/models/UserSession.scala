package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session
import utils.ConditionExpressionParser

case class UserToken(value: String) extends MappedTo[String]

case class UserSession(token: UserToken)

class UserSessions(tag: Tag) extends Table[UserSession](tag, "USER_SESSIONS") {
  def token = column[UserToken]("token", O.PrimaryKey)

  def * = token <> (UserSession, UserSession.unapply)
}

object UserSessions {
  val all = TableQuery[UserSessions]

  def one(token: UserToken) = all.filter(_.token === token)

  def find(token: UserToken)(implicit s: Session) = one(token).firstOption

  def insert(userSession: UserSession)(implicit s: Session) = all.insert(userSession)

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
    val newUserSession = UserSession(token)
    insert(newUserSession)
    newUserSession
  }

  def drugsFor(token: UserToken) = TableQuery[Drugs].filter(_.userToken === token)

  def drugListFor(token: UserToken)(implicit s: Session): List[Drug] = drugsFor(token).list

  def drugWithMedicationProductListFor(token: UserToken)(implicit s: Session)
  : List[(Drug, Option[MedicationProduct])] = {
    (for {
      drug <- drugsFor(token)
      product <- TableQuery[MedicationProducts] if drug.resolvedMedicationProductId === product.id
    } yield (drug, product.?)).list
  }

  def genericTypesFor(token: UserToken)(implicit s: Session) = {
    for {
      (((drug, drugType), drugTypeGenericType), genericType) <-
        drugsFor(token) rightJoin
        TableQuery[MedicationProducts] on (_.resolvedMedicationProductId === _.id) rightJoin
        TableQuery[GenericTypesMedicationProducts] on (_._2.id === _.medicationProductId) rightJoin
        TableQuery[GenericTypes] on (_._2.genericTypeId === _.id)
    } yield genericType
  }

  def genericTypeListFor(token: UserToken)(implicit s: Session): List[GenericType] = genericTypesFor(token).list

  def drugGroupListFor(token: UserToken)(implicit s: Session): List[DrugGroup] = {
    (for {
      ((genericType, drugGroupType), drugGroup) <-
        genericTypesFor(token) rightJoin
        TableQuery[DrugGroupsGenericTypes] on (_.id === _.genericTypeId) rightJoin
        TableQuery[DrugGroups] on (_._2.drugGroupId === _.id)
    } yield drugGroup).list
  }

  def selectedStatementTermsFor(token: UserToken)(implicit s: Session): List[StatementTerm] = {
    (for {
      (session, statementTerm) <-
      TableQuery[StatementTermsUserSessions] rightJoin TableQuery[ExpressionTerms] on (_.statementTermLabel === _.label)
      if session.userSessionToken === token
    } yield statementTerm).list.asInstanceOf[List[StatementTerm]]
  }

  def updateSelectedStatementTerms(token: UserToken, statementTermLabels: List[StatementTermLabel])
                                  (implicit s: Session) =
  {
    TableQuery[StatementTermsUserSessions].filter(_.userSessionToken === token).delete
    statementTermLabels.map(StatementTermUserSession(token, _))
      .foreach { x => TableQuery[StatementTermsUserSessions].insert(x) }
  }

  def relevantStatementTermsFor(token: UserToken)(implicit s: Session): List[StatementTerm] = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val drugGroups = drugGroupListFor(token)
    val genericTypes = genericTypeListFor(token)

    val variableMap = expressionTerms.map(t => (t.label.value, t match {
      case term: GenericTypeTerm => term.evaluate(genericTypes)
      case term: DrugGroupTerm => term.evaluate(drugGroups)
      case StatementTerm(_, _) => true
    })).toMap

    val parser = new ConditionExpressionParser(variableMap)
    val ruleWithExpressionTerms: List[(Long, ExpressionTerm)] = (for {
      termRule <- TableQuery[ExpressionTermsRules]
      term <- TableQuery[ExpressionTerms] if termRule.expressionTermLabel === term.label
    } yield (termRule.ruleId, term)).list

    TableQuery[Rules].list.flatMap(r => parser.parse(r.conditionExpression) match {
      case parser.Success(true, _) => ruleWithExpressionTerms.filter(_._1 == r.id.get).map(_._2)
      case _ => List()
    }).collect{ case t: StatementTerm => t }
  }

  def suggestionListFor(token: UserToken)(implicit s: Session): List[Suggestion] = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val drugGroups = drugGroupListFor(token)
    val genericTypes = genericTypeListFor(token)
    val selectedStatements = selectedStatementTermsFor(token)

    val variableMap = expressionTerms.map(t => (t.label.value, t match {
      case term: GenericTypeTerm => term.evaluate(genericTypes)
      case term: DrugGroupTerm => term.evaluate(drugGroups)
      case term: StatementTerm => term.evaluate(selectedStatements)
    })).toMap

    val parser = new ConditionExpressionParser(variableMap)
    val ruleWithSuggestions: List[(Long, Suggestion)] = (for {
      ((rule, ruleSuggestion), suggestion) <-
        TableQuery[Rules] rightJoin
        TableQuery[RulesSuggestions] on (_.id === _.ruleId) rightJoin
        TableQuery[Suggestions] on (_._2.suggestionId === _.id)
    } yield (rule.id, suggestion)).list

    val trueRuleIds = TableQuery[Rules].list.filter(r => parser.parse(r.conditionExpression) match {
      case parser.Success(true, _) => true
      case _ => false
    }).map(_.id.get)

    ruleWithSuggestions.filter(x => trueRuleIds.contains(x._1)).map(_._2)
  }
}