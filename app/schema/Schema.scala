package schema

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import models._

class DrugGroups(tag: Tag) extends EntityTable[DrugGroup](tag, "DRUG_GROUPS"){
  def id = column[DrugGroupID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[DrugGroupID], String)): DrugGroup =
    DrugGroup(t._1, t._2,
      ManyUnfetched[DrugGroup, GenericType](DrugGroup.genericTypes, t._1))

  def unmapRecord(d: DrugGroup): Option[(Option[DrugGroupID], String)] =
    Some(d.id, d.name)

  def nameIndex = index("DRUG_GROUPS_NAME_INDEX", name, unique = true)
}

class DrugGroupsGenericTypes(tag: Tag) extends Table[(DrugGroupID, GenericTypeID)](tag, "DRUG_GROUPS_GENERIC_TYPES"){
  def drugGroupId = column[DrugGroupID]("drug_group_id")
  def genericTypeId = column[GenericTypeID]("generic_type_id")

  def * = (drugGroupId, genericTypeId)

  def pk = primaryKey("DRUG_GROUPS_GENERIC_TYPES_PK", (drugGroupId, genericTypeId))
  def drugGroup = foreignKey("DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def genericType = foreignKey("DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK", genericTypeId,
    TableQuery[GenericTypes])(_.id)
}

class Drugs(tag: Tag) extends EntityTable[Drug](tag, "DRUGS") {
  def id = column[DrugID]("id", O.PrimaryKey, O.AutoInc)
  def userInput = column[String]("userInput", O.NotNull)
  def userToken = column[UserToken]("userToken", O.NotNull)
  def resolvedMedicationProductId = column[MedicationProductID]("resolved_medication_product_id", O.Nullable)

  def * = (id.?, userInput, userToken, resolvedMedicationProductId.?) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[DrugID], String, UserToken, Option[MedicationProductID])): Drug =
    Drug(t._1, t._2, t._3, t._4,
      OneUnfetched[Drug, UserSession](Drug.userSession, t._1),
      OneUnfetched[Drug, MedicationProduct](Drug.resolvedMedicationProduct, t._1))

  def unmapRecord(d: Drug): Option[(Option[DrugID], String, UserToken, Option[MedicationProductID])] =
    Some(d.id, d.userInput, d.userToken, d.resolvedMedicationProductId)

  def resolvedMedicationProduct = foreignKey("DRUGS_RESOLVED_MEDICATION_PRODUCT_FK", resolvedMedicationProductId,
    TableQuery[MedicationProducts])(_.id)
}

class ExpressionTerms(tag: Tag) extends EntityTable[ExpressionTerm](tag, "EXPRESSION_TERMS"){
  def label = column[String]("label", O.PrimaryKey)
  def genericTypeId = column[GenericTypeID]("drug_type_id", O.Nullable)
  def drugGroupId = column[DrugGroupID]("drug_group_id", O.Nullable)
  def statementTemplate = column[String]("statement_template", O.Nullable)
  def displayCondition = column[String]("display_condition", O.Nullable)
  def comparisonOperator = column[String]("comparison_operator", O.Nullable)
  def age = column[Int]("age", O.Nullable)

  def id = label

  def * = (label, genericTypeId.?, drugGroupId.?, statementTemplate.?, displayCondition.?, comparisonOperator.?, age.?) <> (mapRecord, unmapRecord)

  def mapRecord(t: (String, Option[GenericTypeID], Option[DrugGroupID], Option[String], Option[String], Option[String], Option[Int])): ExpressionTerm =
    ExpressionTerm(t._1, t._2, t._3, t._4, t._5, t._6, t._7,
      OneUnfetched[ExpressionTerm, GenericType](GenericTypeTerm.genericType, Some(t._1)),
      OneUnfetched[ExpressionTerm, DrugGroup](DrugGroupTerm.drugGroup, Some(t._1)))

  def unmapRecord(e: ExpressionTerm): Option[(String, Option[GenericTypeID], Option[DrugGroupID], Option[String], Option[String], Option[String], Option[Int])] =
    Some(e.label, e.genericTypeId, e.drugGroupId, e.statementTemplate, e.displayCondition, e.comparisonOperator, e.age)

  def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", genericTypeId, TableQuery[GenericTypes])(_.id)
}

class ExpressionTermsRules(tag: Tag) extends Table[(String, RuleID)](tag, "EXPRESSION_TERMS_RULES"){
  def expressionTermLabel = column[String]("expression_term_label")
  def ruleId = column[RuleID]("rule_id")

  def * = (expressionTermLabel, ruleId)

  def pk = primaryKey("EXPRESSION_TERMS_RULES_PK", (expressionTermLabel, ruleId))
  def expressionTerm = foreignKey("EXPRESSION_TERMS_RULES_EXPRESSION_TERM_FK", expressionTermLabel,
    TableQuery[ExpressionTerms])(_.label)
  def rule = foreignKey("EXPRESSION_TERMS_RULES_RULE_FK", ruleId, TableQuery[Rules])(_.id)
}

class GenericTypes(tag: Tag) extends EntityTable[GenericType](tag, "GENERIC_TYPES"){
  def id = column[GenericTypeID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[GenericTypeID], String)): GenericType =
    GenericType(t._1, t._2,
      ManyUnfetched[GenericType, MedicationProduct](GenericType.medicationProducts, t._1),
      ManyUnfetched[GenericType, DrugGroup](GenericType.drugGroups, t._1))

  def unmapRecord(g: GenericType): Option[(Option[GenericTypeID], String)] =
    Some(g.id, g.name)

  def nameIndex = index("GENERIC_TYPES_NAME_INDEX", name, unique = true)
}

class GenericTypesMedicationProducts(tag: Tag)
  extends Table[(GenericTypeID, MedicationProductID)](tag, "GENERIC_TYPES_MEDICATION_PRODUCT")
{
  def genericTypeId = column[GenericTypeID]("generic_type_id")
  def medicationProductId = column[MedicationProductID]("medication_product_id")

  def * = (genericTypeId, medicationProductId)

  def pk = primaryKey("GENERIC_TYPES_MEDICATION_PRODUCT_PK", (medicationProductId, genericTypeId))
  def genericType = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_GENERIC_TYPE_FK", genericTypeId,
    TableQuery[GenericTypes])(_.id)
  def medicationProduct = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK", medicationProductId,
    TableQuery[MedicationProducts])(_.id)
}

class MedicationProducts(tag: Tag) extends EntityTable[MedicationProduct](tag, "MEDICATION_PRODUCTS"){
  def id = column[MedicationProductID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[MedicationProductID], String)): MedicationProduct =
    MedicationProduct(t._1, t._2,
      ManyUnfetched[MedicationProduct, GenericType](MedicationProduct.genericTypes, t._1))

  def unmapRecord(m: MedicationProduct): Option[(Option[MedicationProductID], String)] =
    Some(m.id, m.name)

  def nameIndex = index("MEDICATION_PRODUCTS_NAME_INDEX", name, unique = true)
}

class Rules(tag: Tag) extends EntityTable[Rule](tag, "RULES") {
  def id = column[RuleID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def conditionExpression = column[String]("condition_expression", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def note = column[String]("note", O.Nullable)

  def * = (id.?, name, conditionExpression, source.?, note.?) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[RuleID], String, String, Option[String], Option[String])): Rule =
    Rule(t._1, t._2, t._3, t._4, t._5,
      ManyUnfetched[Rule, SuggestionTemplate](Rule.suggestionTemplates, t._1))

  def unmapRecord(r: Rule): Option[(Option[RuleID], String, String, Option[String], Option[String])] =
    Some(r.id, r.name, r.conditionExpression, r.source, r.note)

  def nameIndex = index("RULES_NAME_INDEX", name, unique = true)
}

class RulesSuggestionTemplates(tag: Tag) extends Table[(RuleID, SuggestionTemplateID)](tag, "RULES_SUGGESTION_TEMPLATES") {
  def ruleId = column[RuleID]("rule_id")
  def suggestionTemplateId = column[SuggestionTemplateID]("suggestion_id")

  def * = (ruleId, suggestionTemplateId)

  def pk = primaryKey("RULES_SUGGESTION_TEMPLATES_PK", (ruleId, suggestionTemplateId))
  def rule = foreignKey("RULES_SUGGESTION_TEMPLATES_RULE_FK", ruleId, TableQuery[Rules])(_.id)
  def suggestionTemplate = foreignKey("RULES_SUGGESTION_TEMPLATES_SUGGESTION_TEMPLATE_FK", suggestionTemplateId,
    TableQuery[SuggestionTemplates])(_.id)
}

class StatementTermsUserSessions(tag: Tag)
  extends Table[StatementTermUserSession](tag, "STATEMENT_TERMS_USER_SESSIONS")
{
  def userSessionToken = column[UserToken]("user_session_token")
  def statementTermLabel = column[String]("statement_term_label")
  def textHash = column[String]("text_hash")
  def conditional = column[Boolean]("conditional", O.Default(false))

  def * = (userSessionToken, statementTermLabel, textHash, conditional) <>
    (StatementTermUserSession.tupled, StatementTermUserSession.unapply)

  def pk = primaryKey("STATEMENT_TERMS_USER_SESSIONS_PK", (userSessionToken, statementTermLabel, textHash, conditional))
  def userSession = foreignKey("STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK", userSessionToken,
    TableQuery[UserSessions])(_.token)
  def statementTerm = foreignKey("STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK", statementTermLabel,
    TableQuery[ExpressionTerms])(_.label)
}

class SuggestionTemplates(tag: Tag) extends EntityTable[SuggestionTemplate](tag, "SUGGESTION_TEMPLATES"){
  def id = column[SuggestionTemplateID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def text = column[String]("text", O.NotNull)
  def explanatoryNote = column[String]("explanatory_note", O.Nullable)

  def * = (id.?, name, text, explanatoryNote.?) <> (mapRecord, unmapRecord)

  def mapRecord(t: (Option[SuggestionTemplateID], String, String, Option[String])): SuggestionTemplate =
    SuggestionTemplate(t._1, t._2, t._3, t._4,
      ManyUnfetched[SuggestionTemplate, Rule](SuggestionTemplate.rules, t._1))

  def unmapRecord(s: SuggestionTemplate): Option[(Option[SuggestionTemplateID], String, String, Option[String])] =
    Some(s.id, s.name, s.text, s.explanatoryNote)

  def nameIndex = index("SUGGESTION_TEMPLATES_NAME_INDEX", name, unique = true)
}

class UserSessions(tag: Tag) extends EntityTable[UserSession](tag, "USER_SESSIONS") {
  def token = column[UserToken]("token", O.PrimaryKey)
  def age = column[Int]("age", O.Nullable)

  def id = token

  def * = (token, age.?) <> (mapRecord, unmapRecord)

  def mapRecord(t: (UserToken, Option[Int])): UserSession =
    UserSession(t._1, t._2,
      ManyFetched[UserSession, Drug](UserSession.drugs, Some(t._1)),
      ManyFetched[UserSession, MedicationProduct](UserSession.medicationProducts, Some(t._1)),
      ManyFetched[UserSession, StatementTermUserSession](UserSession.statementTermsUserSessions, Some(t._1)))

  def unmapRecord(u: UserSession): Option[(UserToken, Option[Int])] =
    Some(u.token, u.age)
}
