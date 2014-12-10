package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session
import utils.{MedicationProductTemplateRenderer, ConditionExpressionParser}

case class UserToken(value: String) extends MappedTo[String]

case class UserSession(token: UserToken, age: Option[Int])

case class Statement(termLabel: String, text: String, selected: Boolean)

case class Suggestion(text: String, explanatoryNote: Option[String])

class UserSessions(tag: Tag) extends Table[UserSession](tag, "USER_SESSIONS") {
  def token = column[UserToken]("token", O.PrimaryKey)
  def age = column[Int]("age", O.Nullable)

  def * = (token, age.?) <> (UserSession.tupled, UserSession.unapply)
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
    val newUserSession = UserSession(token, None)
    insert(newUserSession)
    newUserSession
  }

  def update(token: UserToken, userSession: UserSession)(implicit s: Session) =
    one(token).map(_.age.?).update(userSession.age)

  def drugListFor(token: UserToken)(implicit s: Session): List[Drug] = drugsFor(token).list

  def deleteDrug(token: UserToken, drugId: DrugID)(implicit s: Session) = {
    drugsFor(token).filter(_.id === drugId).delete
  }

  def drugWithMedicationProductListFor(token: UserToken)(implicit s: Session)
  : List[(Drug, Option[MedicationProduct])] = {
    (for {
      (drug, product) <-
        drugsFor(token) leftJoin
        TableQuery[MedicationProducts] on (_.resolvedMedicationProductId === _.id)
    } yield (drug, product.?)).list
  }

  def genericTypeListFor(token: UserToken)(implicit s: Session): List[GenericType] =
    genericTypesFor(token).list

  def drugGroupListFor(token: UserToken)(implicit s: Session): List[DrugGroup] = {
    (for {
      ((genericType, drugGroupType), drugGroup) <-
        genericTypesFor(token) innerJoin
        TableQuery[DrugGroupsGenericTypes] on (_.id === _.genericTypeId) innerJoin
        TableQuery[DrugGroups] on (_._2.drugGroupId === _.id)
    } yield drugGroup).list
  }

  def groupWithProductListFor(token: UserToken)(implicit s: Session): List[(DrugGroup, MedicationProduct)] = {
    (for {
      ((((product, _), _), _), group) <-
        medicationProductsFor(token) innerJoin
        TableQuery[GenericTypesMedicationProducts] on (_.id === _.medicationProductId) innerJoin
        TableQuery[GenericTypes] on (_._2.genericTypeId === _.id) innerJoin
        TableQuery[DrugGroupsGenericTypes] on (_._2.id === _.genericTypeId) innerJoin
        TableQuery[DrugGroups] on (_._2.drugGroupId === _.id)
    } yield (group, product)).list
  }

  def typeWithProductListFor(token: UserToken)(implicit s: Session): List[(GenericType, MedicationProduct)] = {
    (for {
      ((product, _), genericType) <-
      medicationProductsFor(token) innerJoin
      TableQuery[GenericTypesMedicationProducts] on (_.id === _.medicationProductId) innerJoin
      TableQuery[GenericTypes] on (_._2.genericTypeId === _.id)
    } yield (genericType, product)).list
  }

  def unconditionalStatementListFor(token: UserToken)(implicit s: Session): List[Statement] = {
    val selection = selectedTermIdsWithTextHash(token)

    StatementTerms.all.filter(_.displayCondition.isNull).list.asInstanceOf[List[StatementTerm]]
      .map { x =>
        Statement(x.label, x.statementTemplate, selection.contains((x.label, md5(x.statementTemplate))))
      }
  }

  def conditionalStatementListFor(token: UserToken)(implicit s: Session): List[Statement] = {
    val selection = selectedTermIdsWithTextHash(token)
    val parser = new ConditionExpressionParser(buildVariableMap(token))
    val renderer = new MedicationProductTemplateRenderer(groupWithProductListFor(token), typeWithProductListFor(token))

    StatementTerms.all.filter(_.displayCondition.isNotNull).list.asInstanceOf[List[StatementTerm]]
      .filter( x => parser.parse(x.displayCondition.getOrElse("")) match {
        case parser.Success(true, _) => true
        case _ => false
      })
      .flatMap { x =>
        renderer.render(x.statementTemplate).map { text =>
          Statement(x.label, text, selection.contains((x.label, md5(text))))
        }
      }
  }

  def selectedStatementListFor(token: UserToken)(implicit s: Session): List[Statement] = {
    val statements = unconditionalStatementListFor(token) ++ conditionalStatementListFor(token)
    statements.filter(_.selected)
  }

  def updateSelectedUnconditionalStatements(token: UserToken, statements: List[Statement])(implicit s: Session) = {
    TableQuery[StatementTermsUserSessions].filter(x => x.userSessionToken === token && x.conditional === false).delete

    statements.filter(_.selected)
      .map { x => StatementTermUserSession(token, x.termLabel, md5(x.text), conditional = false) }
      .foreach { x => TableQuery[StatementTermsUserSessions].insert(x) }
  }

  def updateSelectedConditionalStatements(token: UserToken, statements: List[Statement])(implicit s: Session) = {
    TableQuery[StatementTermsUserSessions].filter(x => x.userSessionToken === token && x.conditional === true).delete

    statements.filter(_.selected)
      .map { x => StatementTermUserSession(token, x.termLabel, md5(x.text), conditional = true) }
      .foreach { x => TableQuery[StatementTermsUserSessions].insert(x) }
  }

  def suggestionListFor(token: UserToken)(implicit s: Session): List[Suggestion] = {
    val variableMap = buildVariableMap(token)
    val parser = new ConditionExpressionParser(variableMap)
    val renderer = new MedicationProductTemplateRenderer(groupWithProductListFor(token), typeWithProductListFor(token))
    val ruleWithSuggestions: List[(RuleID, SuggestionTemplate)] = (for {
      ((rule, ruleSuggestion), suggestion) <-
        TableQuery[Rules] innerJoin
        TableQuery[RulesSuggestionTemplates] on (_.id === _.ruleId) innerJoin
        TableQuery[SuggestionTemplates] on (_._2.suggestionTemplateId === _.id)
    } yield (rule.id, suggestion)).list

    val trueRuleIds = TableQuery[Rules].list.filter(r => parser.parse(r.conditionExpression) match {
      case parser.Success(true, _) => true
      case _ => false
    }).map(_.id.get)

    ruleWithSuggestions.filter(x => trueRuleIds.contains(x._1)).map(_._2)
      .flatMap {
        case SuggestionTemplate(_, _, text, Some(note)) =>
          (renderer.render(text) zip renderer.render(note)).map(x => Suggestion(x._1, Some(x._2)))
        case SuggestionTemplate(_, _, text, None) =>
          renderer.render(text).map(x => Suggestion(x, None))
      }
  }

  private def drugsFor(token: UserToken) = TableQuery[Drugs].filter(_.userToken === token)

  private def medicationProductsFor(token: UserToken)(implicit s: Session) = {
    for {
      (_, medicationProduct) <-
      drugsFor(token) innerJoin
        TableQuery[MedicationProducts] on (_.resolvedMedicationProductId === _.id)
    } yield medicationProduct
  }

  private def genericTypesFor(token: UserToken)(implicit s: Session) = {
    for {
      (_, genericType) <-
      medicationProductsFor(token) innerJoin
        TableQuery[GenericTypesMedicationProducts] on (_.id === _.medicationProductId) innerJoin
        TableQuery[GenericTypes] on (_._2.genericTypeId === _.id)
    } yield genericType
  }

  private def selectedTermIdsWithTextHash(token: UserToken)(implicit s: Session): List[(String, String)] = {
    (for {
      (session, statementTerm) <-
      TableQuery[StatementTermsUserSessions] innerJoin TableQuery[ExpressionTerms] on (_.statementTermLabel === _.label)
      if session.userSessionToken === token
    } yield (statementTerm.label, session.textHash)).list
  }

  private def selectedStatementTermListFor(token: UserToken)(implicit s: Session): List[StatementTerm] = {
    (for {
      (session, statementTerm) <-
      TableQuery[StatementTermsUserSessions] innerJoin TableQuery[ExpressionTerms] on (_.statementTermLabel === _.label)
      if session.userSessionToken === token
    } yield statementTerm).list.asInstanceOf[List[StatementTerm]]
  }

  private def buildVariableMap(token: UserToken)(implicit s: Session): Map[String, Boolean] = {
    val session = find(token).get
    val expressionTerms = TableQuery[ExpressionTerms].list
    val drugGroups = drugGroupListFor(token)
    val genericTypes = genericTypeListFor(token)
    val selectedStatements = selectedStatementTermListFor(token)

    expressionTerms.map(t => (t.label, t match {
      case term: GenericTypeTerm => term.evaluate(genericTypes)
      case term: DrugGroupTerm => term.evaluate(drugGroups)
      case term: StatementTerm => term.evaluate(selectedStatements)
      case term: AgeTerm => term.evaluate(session.age.getOrElse(0))
    })).toMap
  }

  private def md5(text: String): String = {
    java.security.MessageDigest.getInstance("MD5").digest(text.getBytes).map(0xFF & _)
      .map { "%02x".format(_) }.foldLeft(""){_ + _}
  }
}