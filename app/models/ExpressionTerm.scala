package models

import scala.concurrent.ExecutionContext

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

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

  override protected def beforeUpdate(term: ExpressionTerm)
                                     (implicit ec: ExecutionContext)
  : DBIO[ExpressionTerm] =
    DBIO.seq(
      updateDependentRuleConditions(term),
      updateDependentStatementTermConditions(term)
    ).map(_ => term)


  private def updateDependentRuleConditions(term: ExpressionTerm)
                                           (implicit ec: ExecutionContext)
  : DBIO[Unit] =
    for {
      oldLabel <- ExpressionTerm.one(term.id.get).map(_.label).result.headOption
      rules <- term.dependentRules.valueAction
    } yield DBIO.sequence(rules.map { r =>
      val updatedCE = r.conditionExpression.replaceLabel(oldLabel.get, term.label)

      // Update rule via TableQuery to bypass afterSave hook, because it will look
      // for an expression term with a label which does not yet exist at this time.
      TableQuery[Rules].update(r.copy(conditionExpression = updatedCE))
    })

  private def updateDependentStatementTermConditions(term: ExpressionTerm)
                                                    (implicit ec: ExecutionContext)
  : DBIO[Unit] =
    for {
      oldLabel <- ExpressionTerm.one(term.id.get).map(_.label).result.headOption
      statementTerms <- term.dependentStatementTerms.valueAction
    } yield DBIO.sequence(statementTerms.map { statementTerm =>
      val updatedDC = statementTerm.displayCondition.get.replaceLabel(oldLabel.get, term.label)

      // Update statement term via TableQuery to bypass afterSave hook, because it will look
      // for an expression term with a label which does not yet exist at this time.
      TableQuery[ExpressionTerms].update(statementTerm.copy(displayCondition = Some(updatedDC)))
    })
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

  override protected def afterSave(id: ExpressionTermID, term: ExpressionTerm)
                                  (implicit ec: ExecutionContext)
  : DBIO[Unit] =
    term.displayCondition match {
      case Some(condition) =>
        val tq = TableQuery[ExpressionTermsStatementTerms]
        val deleteOld = tq.filter(_.statementTermId === id).delete
        val insertNew = for {
          expressionTerms <- ExpressionTerm.all.filter(_.label inSetBind condition.expressionTermLabels).result
        } yield DBIO.sequence(expressionTerms.map(t => tq += (t.id.get, id)))

        deleteOld >> insertNew >> DBIO.successful(())
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

object ExpressionTermConversions {
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
