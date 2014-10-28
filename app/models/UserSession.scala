package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session
import utils.ConditionExpressionParser

case class UserSession(token: String)

class UserSessions(tag: Tag) extends Table[UserSession](tag, "USER_SESSIONS") {
  def token = column[String]("token", O.PrimaryKey)

  def * = token <> (UserSession, UserSession.unapply)
}

object UserSessions {
  val all = TableQuery[UserSessions]
  val drugs = TableQuery[Drugs]
  val drugTypes = TableQuery[DrugTypes]
  val statementTermsUserSessions = TableQuery[StatementTermsUserSessions]
  val rules = TableQuery[Rules]

  def one(token: String) = all.filter(_.token === token)

  def find(token: String)(implicit s: Session) = one(token).firstOption

  def insert(userSession: UserSession)(implicit s: Session) = all.insert(userSession)

  def generateToken(len: Int = 12): String = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new StringBuilder(len)
    val ab = "0123456789abcdefghijklmnopqrstuvwxyz"

    for (i <- 0 until len) {
      sb.append(ab(rand.nextInt(ab.length)))
    }

    sb.toString()
  }

  def create(token: String = generateToken())(implicit s: Session): UserSession = {
    val newUserSession = UserSession(token)
    insert(newUserSession)
    newUserSession
  }

  def drugsFor(token: String) = drugs.filter(_.userToken === token)

  def drugListFor(token: String)(implicit s: Session): List[Drug] = drugsFor(token).list

  def drugTypeListFor(token: String)(implicit s: Session): List[DrugType] = {
    (for {
      (d, dt) <- drugs rightJoin drugTypes on (_.resolvedDrugTypeId === _.id)
    } yield (d.userToken, dt)).filter(_._1 === token).map(_._2).list
  }

  /**
   * Checks for any of the selected drug types, if it or its associated generic type, is the type referenced by the
   * drug type term.
   *
   * @param term The drug type term
   * @param selectedDrugTypes The drug types that the user's medication list resolved to
   * @return
   */
  def evaluateDrugTypeTerm(term: DrugTypeTerm, selectedDrugTypes: List[DrugType]): Boolean = {
    selectedDrugTypes.exists(dType =>
      dType.id.getOrElse(-1) == term.drugTypeId || dType.genericTypeId.getOrElse(-1) == term.drugTypeId)
  }

  /**
   * Checks for any of the selected drug types or the associated generic drug type, if it is part of the drug group
   * associated with the drug group term.
   *
   * @param term The drug group term
   * @param selectedDrugTypes The drug types that the user's medication list resolved to
   * @param drugGroupsTypes A list of the all drug type/drug group relations, passed in to save queries
   * @return
   */
  def evaluateDrugGroupTerm(term: DrugGroupTerm, selectedDrugTypes: List[DrugType],
                            drugGroupsTypes: List[DrugGroupType]): Boolean = {
    // If there is going to be performance bottleneck, this'll be it
    selectedDrugTypes.exists(dType => drugGroupsTypes.exists(x =>
      x.drugGroupId == term.drugGroupId &&
        (x.drugTypeId == dType.id.getOrElse(-1) || x.drugTypeId == dType.genericTypeId.getOrElse(-1))))
  }

  def relevantStatementTermsFor(token: String)(implicit s: Session): List[StatementTerm] = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val drugGroupsTypes = TableQuery[DrugGroupsTypes].list
    val drugTypes = drugTypeListFor(token)

    val variableMap = expressionTerms.map(t => (t.label, t match {
      case term: DrugTypeTerm => evaluateDrugTypeTerm(term, drugTypes)
      case term: DrugGroupTerm => evaluateDrugGroupTerm(term, drugTypes, drugGroupsTypes)
      case StatementTerm(_, _) => true
    })).toMap

    val parser = new ConditionExpressionParser(variableMap)
    val ruleWithExpressionTerms: List[(Long, ExpressionTerm)] = (for {
      termRule <- TableQuery[ExpressionTermsRules]
      term <- TableQuery[ExpressionTerms] if termRule.expressionTermLabel === term.label
    } yield (termRule.ruleId, term)).list

    rules.list.flatMap(r => parser.parse(r.conditionExpression) match {
      case parser.Success(true, _) => ruleWithExpressionTerms.filter(_._1 == r.id.get).map(_._2)
      case _ => List()
    }).collect{ case t: StatementTerm => t }
  }

  def selectedStatementTermsFor(token: String)(implicit s: Session): List[StatementTerm] = {
    (for {
      us <- statementTermsUserSessions
      s <- TableQuery[ExpressionTerms] if us.statementTermLabel === s.label
    } yield (us.userSessionToken, s)).filter(_._1 === token).map(_._2).list.asInstanceOf[List[StatementTerm]]
  }

  def updateSelectedStatementTerms(token: String, statementTermLabels: List[String])(implicit s: Session) = {
    statementTermsUserSessions.filter(_.userSessionToken === token).delete
    statementTermLabels.map(StatementTermUserSession(token, _)).foreach { x => statementTermsUserSessions.insert(x) }
  }

  def suggestionListFor(token: String)(implicit s: Session): List[Suggestion] = {
    val expressionTerms = TableQuery[ExpressionTerms].list
    val drugGroupsTypes = TableQuery[DrugGroupsTypes].list
    val drugTypes = drugTypeListFor(token)
    val selectedStatements = selectedStatementTermsFor(token)

    val variableMap = expressionTerms.map(t => (t.label, t match {
      case term: DrugTypeTerm => evaluateDrugTypeTerm(term, drugTypes)
      case term: DrugGroupTerm => evaluateDrugGroupTerm(term, drugTypes, drugGroupsTypes)
      case term: StatementTerm => selectedStatements.contains(term)
    })).toMap

    val parser = new ConditionExpressionParser(variableMap)
    val ruleWithSuggestions: List[(Long, Suggestion)] = (for {
      rule <- rules
      suggestion <- TableQuery[Suggestions] if suggestion.ruleId === rule.id
    } yield (rule.id, suggestion)).list

    val trueRuleIds = rules.list.filter(r => parser.parse(r.conditionExpression) match {
      case parser.Success(true, _) => true
      case _ => false
    }).map(_.id.get)

    ruleWithSuggestions.filter(x => trueRuleIds.contains(x._1)).map(_._2)
  }
}