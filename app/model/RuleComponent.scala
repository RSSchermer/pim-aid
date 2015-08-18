package model

import scala.concurrent.ExecutionContext

import entitytled.Entitytled

trait RuleComponent {
  self: Entitytled
    with ConditionExpressionComponent
    with ExpressionTermComponent
    with SuggestionTemplateComponent
  =>

  import driver.api._

  case class RuleID(value: Long) extends MappedTo[Long]

  case class Rule(
      id: Option[RuleID],
      name: String,
      conditionExpression: ConditionExpression,
      source: Option[String],
      formalizationReference: Option[String],
      note: Option[String])(implicit includes: Includes[Rule])
    extends Entity[Rule, RuleID]
  {
    val suggestionTemplates = many(Rule.suggestionTemplates)
  }

  object Rule extends EntityCompanion[Rules, Rule, RuleID] {
    val suggestionTemplates = toManyThrough[SuggestionTemplates, RulesSuggestionTemplates, SuggestionTemplate]

    override protected def afterSave(ruleId: RuleID, rule: Rule)(implicit ec: ExecutionContext): DBIO[Unit] = {
      val tq = TableQuery[ExpressionTermsRules]
      val deleteOld = tq.filter(_.ruleId === ruleId).delete
      val insertNew = for {
        terms <- ExpressionTerm.all.filter(_.label inSetBind rule.conditionExpression.expressionTermLabels).result
      } yield DBIO.sequence(terms.map(t => tq +=(t.id.get, ruleId)))

      deleteOld >> insertNew >> DBIO.successful(())
    }
  }

  class Rules(tag: Tag) extends EntityTable[Rule, RuleID](tag, "RULES") {
    def id = column[RuleID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def conditionExpression = column[ConditionExpression]("condition_expression")
    def source = column[Option[String]]("source")
    def formalizationReference = column[Option[String]]("formalization_reference")
    def note = column[Option[String]]("note")

    def * = (id.?, name, conditionExpression, source, formalizationReference, note) <>
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
}
