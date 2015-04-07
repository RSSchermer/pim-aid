package models.meta

import models.meta.Profile._
import models.meta.Profile.driver.simple._
import models._

object Schema {
  class DrugGroups(tag: Tag) extends EntityTable[DrugGroup, DrugGroupID](tag, "DRUG_GROUPS") {
    def id = column[DrugGroupID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)

    def * = (id.?, name) <>((DrugGroup.apply _).tupled, DrugGroup.unapply)

    def nameIndex = index("DRUG_GROUPS_NAME_INDEX", name, unique = true)
  }

  class DrugGroupsGenericTypes(tag: Tag)
    extends Table[(DrugGroupID, GenericTypeID)](tag, "DRUG_GROUPS_GENERIC_TYPES")
  {
    def drugGroupId = column[DrugGroupID]("drug_group_id")
    def genericTypeId = column[GenericTypeID]("generic_type_id")

    def * = (drugGroupId, genericTypeId)

    def pk = primaryKey("DRUG_GROUPS_GENERIC_TYPES_PK", (drugGroupId, genericTypeId))

    def drugGroup = foreignKey("DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK", drugGroupId,
      TableQuery[DrugGroups])(_.id, onDelete = ForeignKeyAction.Cascade)
    def genericType = foreignKey("DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK", genericTypeId,
      TableQuery[GenericTypes])(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class Drugs(tag: Tag) extends EntityTable[Drug, DrugID](tag, "DRUGS") {
    def id = column[DrugID]("id", O.PrimaryKey, O.AutoInc)
    def userInput = column[String]("userInput", O.NotNull)
    def userToken = column[UserToken]("userToken", O.NotNull)
    def resolvedMedicationProductId = column[MedicationProductID]("resolved_medication_product_id", O.Nullable)

    def * = (id.?, userInput, userToken, resolvedMedicationProductId.?) <>((Drug.apply _).tupled, Drug.unapply)

    def resolvedMedicationProduct = foreignKey("DRUGS_RESOLVED_MEDICATION_PRODUCT_FK", resolvedMedicationProductId,
      TableQuery[MedicationProducts])(_.id)
  }

  class ExpressionTerms(tag: Tag) extends EntityTable[ExpressionTerm, ExpressionTermID](tag, "EXPRESSION_TERMS") {
    def id = column[ExpressionTermID]("id", O.PrimaryKey, O.AutoInc)
    def label = column[String]("label", O.NotNull)
    def genericTypeId = column[GenericTypeID]("drug_type_id", O.Nullable)
    def drugGroupId = column[DrugGroupID]("drug_group_id", O.Nullable)
    def statementTemplate = column[String]("statement_template", O.Nullable)
    def displayCondition = column[ConditionExpression]("display_condition", O.Nullable)
    def comparisonOperator = column[String]("comparison_operator", O.Nullable)
    def age = column[Int]("age", O.Nullable)

    def * = (id.?, label, genericTypeId.?, drugGroupId.?, statementTemplate.?, displayCondition.?, comparisonOperator.?, age.?) <>
      ((ExpressionTerm.apply _).tupled, ExpressionTerm.unapply)

    def labelIndex = index("EXPRESSION_TERMS_LABEL_INDEX", label, unique = true)
    def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
    def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", genericTypeId, TableQuery[GenericTypes])(_.id)
  }

  class ExpressionTermsRules(tag: Tag) extends Table[(ExpressionTermID, RuleID)](tag, "EXPRESSION_TERMS_RULES") {
    def expressionTermId = column[ExpressionTermID]("expression_term_id")
    def ruleId = column[RuleID]("rule_id")

    def * = (expressionTermId, ruleId)

    def pk = primaryKey("EXPRESSION_TERMS_RULES_PK", (expressionTermId, ruleId))

    def expressionTerm = foreignKey("EXPRESSION_TERMS_RULES_EXPRESSION_TERM_FK", expressionTermId,
      TableQuery[ExpressionTerms])(_.id)
    def rule = foreignKey("EXPRESSION_TERMS_RULES_RULE_FK", ruleId, TableQuery[Rules])(_.id,
      onDelete = ForeignKeyAction.Cascade)
  }

  class ExpressionTermsStatementTerms(tag: Tag)
    extends Table[(ExpressionTermID, ExpressionTermID)](tag, "EXPRESSION_TERMS_STATEMENT_TERMS")
  {
    def expressionTermId = column[ExpressionTermID]("expression_term_id")
    def statementTermId = column[ExpressionTermID]("statement_term_id")

    def * = (expressionTermId, statementTermId)

    def pk = primaryKey("EXPRESSION_TERMS_STATEMENT_TERMS_PK", (expressionTermId, statementTermId))

    def expressionTerm = foreignKey("EXPRESSION_TERMS_STATEMENT_TERMS_EXPRESSION_TERM_FK", expressionTermId,
      TableQuery[ExpressionTerms])(_.id)
    def statementTerm = foreignKey("EXPRESSION_TERMS_STATEMENT_TERMS_STATEMENT_TERM_FK", statementTermId,
      TableQuery[ExpressionTerms])(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class GenericTypes(tag: Tag) extends EntityTable[GenericType, GenericTypeID](tag, "GENERIC_TYPES") {
    def id = column[GenericTypeID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)

    def * = (id.?, name) <>((GenericType.apply _).tupled, GenericType.unapply)

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
      TableQuery[GenericTypes])(_.id, onDelete = ForeignKeyAction.Cascade)
    def medicationProduct = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK", medicationProductId,
      TableQuery[MedicationProducts])(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  class MedicationProducts(tag: Tag)
    extends EntityTable[MedicationProduct, MedicationProductID](tag, "MEDICATION_PRODUCTS")
  {
    def id = column[MedicationProductID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)

    def * = (id.?, name) <>((MedicationProduct.apply _).tupled, MedicationProduct.unapply)

    def nameIndex = index("MEDICATION_PRODUCTS_NAME_INDEX", name, unique = true)
  }

  class Rules(tag: Tag) extends EntityTable[Rule, RuleID](tag, "RULES") {
    def id = column[RuleID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def conditionExpression = column[ConditionExpression]("condition_expression", O.NotNull)
    def source = column[String]("source", O.Nullable)
    def formalizationReference = column[String]("formalization_reference", O.Nullable)
    def note = column[String]("note", O.Nullable)

    def * = (id.?, name, conditionExpression, source.?, formalizationReference.?, note.?) <>
      ((Rule.apply _).tupled, Rule.unapply)

    def nameIndex = index("RULES_NAME_INDEX", name, unique = true)
  }

  class RulesSuggestionTemplates(tag: Tag)
    extends Table[(RuleID, SuggestionTemplateID)](tag, "RULES_SUGGESTION_TEMPLATES")
  {
    def ruleId = column[RuleID]("rule_id")
    def suggestionTemplateId = column[SuggestionTemplateID]("suggestion_id")

    def * = (ruleId, suggestionTemplateId)

    def pk = primaryKey("RULES_SUGGESTION_TEMPLATES_PK", (ruleId, suggestionTemplateId))

    def rule = foreignKey("RULES_SUGGESTION_TEMPLATES_RULE_FK", ruleId, TableQuery[Rules])(_.id,
      onDelete = ForeignKeyAction.Cascade)
    def suggestionTemplate = foreignKey("RULES_SUGGESTION_TEMPLATES_SUGGESTION_TEMPLATE_FK", suggestionTemplateId,
      TableQuery[SuggestionTemplates])(_.id)
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

  class SuggestionTemplates(tag: Tag)
    extends EntityTable[SuggestionTemplate, SuggestionTemplateID](tag, "SUGGESTION_TEMPLATES")
  {
    def id = column[SuggestionTemplateID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def text = column[String]("text", O.NotNull)
    def explanatoryNote = column[String]("explanatory_note", O.Nullable)

    def * = (id.?, name, text, explanatoryNote.?) <>
      ((SuggestionTemplate.apply _).tupled, SuggestionTemplate.unapply)

    def nameIndex = index("SUGGESTION_TEMPLATES_NAME_INDEX", name, unique = true)
  }

  class UserSessions(tag: Tag) extends EntityTable[UserSession, UserToken](tag, "USER_SESSIONS") {
    def token = column[UserToken]("token", O.PrimaryKey)
    def age = column[Int]("age", O.Nullable)

    def id = token

    def * = (token, age.?) <>((UserSession.apply _).tupled, UserSession.unapply)
  }
}
