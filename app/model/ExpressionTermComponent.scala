package model

import scala.concurrent.ExecutionContext

import entitytled.Entitytled

import scala.language.implicitConversions

trait ExpressionTermComponent {
  self: Entitytled
    with GenericTypeComponent
    with DrugGroupComponent
    with MedicationProductComponent
    with RuleComponent
    with ConditionExpressionComponent
  =>

  import driver.api._

  case class ExpressionTermID(value: Long) extends MappedTo[Long]

  case class ExpressionTerm(
      id: Option[ExpressionTermID],
      label: String,
      genericTypeId: Option[GenericTypeID],
      drugGroupId: Option[DrugGroupID],
      statementTemplate: Option[String],
      displayCondition: Option[ConditionExpression],
      comparisonOperator: Option[String],
      age: Option[Int])(implicit includes: Includes[ExpressionTerm])
    extends Entity[ExpressionTerm, ExpressionTermID]
  {
    val genericType = one(GenericTypeTerm.genericType)
    val drugGroup = one(DrugGroupTerm.drugGroup)
    val dependentStatementTerms = many(ExpressionTerm.dependentStatementTerms)
    val dependentRules = many(ExpressionTerm.dependentRules)
  }

  abstract class ExpressionTermCompanion extends EntityCompanion[ExpressionTerms, ExpressionTerm, ExpressionTermID] {
    def hasLabel(label: String): Query[ExpressionTerms, ExpressionTerm, Seq] =
      all.filter(_.label === label)

    override protected def beforeUpdate(instance: ExpressionTerm)
                                       (implicit ec: ExecutionContext)
    : DBIO[ExpressionTerm] = for {
      _ <- updateDependentRuleConditions(instance)
      _ <- updateDependentStatementTermConditions(instance)
    } yield instance

    private def updateDependentRuleConditions(term: ExpressionTerm)
                                             (implicit ec: ExecutionContext)
    : DBIO[Unit] = for {
      oldLabel <- ExpressionTerm.one(term.id.get).map(_.label).result.headOption
      rules <- term.dependentRules.valueAction
      _ <- DBIO.sequence(rules.map { rule =>
        val updatedCE = rule.conditionExpression.replaceLabel(oldLabel.get, term.label)

        // Update rule via TableQuery to bypass afterSave hook, because it will look
        // for an expression term with a label which does not yet exist at this time.
        TableQuery[Rules].filter(_.id === rule.id).update(rule.copy(conditionExpression = updatedCE))
      })
    } yield ()

    private def updateDependentStatementTermConditions(term: ExpressionTerm)
                                                      (implicit ec: ExecutionContext)
    : DBIO[Unit] = for {
      oldLabel <- ExpressionTerm.one(term.id.get).map(_.label).result.headOption
      statementTerms <- term.dependentStatementTerms.valueAction
      _ <- DBIO.sequence(statementTerms.map { statementTerm =>
        val updatedDC = statementTerm.displayCondition.get.replaceLabel(oldLabel.get, term.label)

        // Update statement term via TableQuery to bypass afterSave hook, because it will look
        // for an expression term with a label which does not yet exist at this time.
        TableQuery[ExpressionTerms].filter(_.id === statementTerm.id)
          .update(statementTerm.copy(displayCondition = Some(updatedDC)))
      })
    } yield ()
  }

  object ExpressionTerm extends ExpressionTermCompanion {
    val dependentStatementTerms = toManyThrough[ExpressionTerms, ExpressionTermsStatementTerms, ExpressionTerm](
      toQuery = TableQuery[ExpressionTermsStatementTerms] join TableQuery[ExpressionTerms] on (_.statementTermId === _.id),
      joinCondition = (e: ExpressionTerms, t: (ExpressionTermsStatementTerms, ExpressionTerms)) => e.id === t._1.expressionTermId
    )

    val dependentRules = toManyThrough[Rules, ExpressionTermsRules, Rule]
  }

  case class GenericTypeTerm(
    id: Option[ExpressionTermID],
    label: String,
    genericTypeId: GenericTypeID)

  object GenericTypeTerm extends ExpressionTermCompanion {
    override val all = TableQuery[ExpressionTerms].filter(_.genericTypeId.isDefined)

    val genericType = toOne[GenericTypes, GenericType]
  }

  case class DrugGroupTerm(
    id: Option[ExpressionTermID],
    label: String,
    drugGroupId: DrugGroupID)

  object DrugGroupTerm extends ExpressionTermCompanion {
    override val all = TableQuery[ExpressionTerms].filter(_.drugGroupId.isDefined)

    val drugGroup = toOne[DrugGroups, DrugGroup]
  }

  case class StatementTerm(
    id: Option[ExpressionTermID],
    label: String,
    statementTemplate: String,
    displayCondition: Option[ConditionExpression])

  object StatementTerm extends ExpressionTermCompanion {
    override val all = TableQuery[ExpressionTerms].filter(_.statementTemplate.isDefined)

    override protected def afterSave(id: ExpressionTermID, instance: ExpressionTerm)
                                    (implicit ec: ExecutionContext)
    : DBIO[Unit] =
      instance.displayCondition match {
        case Some(condition) =>
          val tq = TableQuery[ExpressionTermsStatementTerms]

          for {
            _ <- tq.filter(_.statementTermId === id).delete
            expressionTerms <- ExpressionTerm.all.filter(_.label inSetBind condition.expressionTermLabels).result
            _ <- DBIO.sequence(expressionTerms.map(t => tq += (t.id.get, id)))
          } yield ()
        case _ =>
          DBIO.successful(())
      }
  }

  case class AgeTerm(
    id: Option[ExpressionTermID],
    label: String,
    comparisonOperator: String,
    age: Int)

  object AgeTerm extends ExpressionTermCompanion {
    override val all = TableQuery[ExpressionTerms].filter(_.age.isDefined)
  }

  class ExpressionTerms(tag: Tag) extends EntityTable[ExpressionTerm, ExpressionTermID](tag, "EXPRESSION_TERMS") {
    def id = column[ExpressionTermID]("id", O.PrimaryKey, O.AutoInc)
    def label = column[String]("label")
    def genericTypeId = column[Option[GenericTypeID]]("drug_type_id")
    def drugGroupId = column[Option[DrugGroupID]]("drug_group_id")
    def statementTemplate = column[Option[String]]("statement_template")
    def displayCondition = column[Option[ConditionExpression]]("display_condition")
    def comparisonOperator = column[Option[String]]("comparison_operator")
    def age = column[Option[Int]]("age")

    def * = (id.?, label, genericTypeId, drugGroupId, statementTemplate, displayCondition, comparisonOperator, age) <>
      ((ExpressionTerm.apply _).tupled, ExpressionTerm.unapply)

    def labelIndex = index("EXPRESSION_TERMS_LABEL_INDEX", label, unique = true)
    def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id.?)
    def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", genericTypeId, TableQuery[GenericTypes])(_.id.?)
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

  implicit def ExpressionTermToAgeTerm(e: ExpressionTerm): AgeTerm =
    AgeTerm(e.id, e.label, e.comparisonOperator.get, e.age.get)

  implicit def AgeTermToExpressionTerm(a: AgeTerm): ExpressionTerm =
    ExpressionTerm(a.id, a.label, None, None, None, None, Some(a.comparisonOperator), Some(a.age))

  implicit def ExpressionTermToDrugGroupTerm(e: ExpressionTerm): DrugGroupTerm =
    DrugGroupTerm(e.id, e.label, e.drugGroupId.get)

  implicit def DrugGroupTermToExpressionTerm(d: DrugGroupTerm): ExpressionTerm =
    ExpressionTerm(d.id, d.label, None, Some(d.drugGroupId), None, None, None, None)

  implicit def ExpressionTermToGenericTypeTerm(e: ExpressionTerm): GenericTypeTerm =
    GenericTypeTerm(e.id, e.label, e.genericTypeId.get)

  implicit def GenericTypeTermToExpressionTerm(g: GenericTypeTerm): ExpressionTerm =
    ExpressionTerm(g.id, g.label, Some(g.genericTypeId), None, None, None, None, None)

  implicit def ExpressionTermToStatementTerm(e: ExpressionTerm): StatementTerm =
    StatementTerm(e.id, e.label, e.statementTemplate.get, e.displayCondition)

  implicit def StatementTermToExpressionTerm(s: StatementTerm): ExpressionTerm =
    ExpressionTerm(s.id, s.label, None, None, Some(s.statementTemplate), s.displayCondition, None, None)
}
