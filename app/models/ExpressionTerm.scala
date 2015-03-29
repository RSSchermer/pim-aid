package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

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
  extends Entity[ExpressionTerm]
{
  type IdType = ExpressionTermID

  val genericType = one(GenericTypeTerm.genericType)
  val drugGroup = one(DrugGroupTerm.drugGroup)
  val dependentStatementTerms = many(ExpressionTerm.dependentStatementTerms)
  val dependentRules = many(ExpressionTerm.dependentRules)
}

abstract class ExpressionTermCompanion extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  def findByLabel(label: String)(implicit s: Session): Option[ExpressionTerm] =
    filter(_.label === label).firstOption

  override protected def beforeUpdate(term: ExpressionTerm)(implicit s: Session): ExpressionTerm = {
    updateDependentRuleConditions(term)
    updateDependentStatementTermConditions(term)
    term
  }

  override protected def afterSave(id: ExpressionTermID, term: ExpressionTerm)(implicit s: Session): Unit =
    if (term.displayCondition.nonEmpty) {
      val etst = TableQuery[ExpressionTermsStatementTerms]
      etst.filter(_.statementTermId === id).delete
      term.displayCondition.get.expressionTerms.foreach(t => etst.insert((t.id.get, id)))
    } else ()

  private def updateDependentRuleConditions(term: ExpressionTerm)(implicit s: Session): Unit = {
    val oldLabel = ExpressionTerm.find(term.id.get).get.label

    term.dependentRules.foreach((r: Rule) => {
      val updatedCE = r.conditionExpression.replaceLabel(oldLabel, term.label)

      // Update rule via TableQuery to bypass afterSave hook, because it will look
      // for an expression term with a label which does not yet exist at this time.
      TableQuery[Rules].filter(_.id === r.id).update(r.copy(conditionExpression = updatedCE))
    })
  }

  private def updateDependentStatementTermConditions(term: ExpressionTerm)(implicit s: Session): Unit = {
    val oldLabel = ExpressionTerm.find(term.id.get).get.label

    term.dependentStatementTerms.foreach((st: ExpressionTerm) => {
      val updatedDC = st.displayCondition.get.replaceLabel(oldLabel, term.label)

      // Update statement term via TableQuery to bypass afterSave hook, because it will look
      // for an expression term with a label which does not yet exist at this time.
      TableQuery[ExpressionTerms].filter(_.id === st.id).update(st.copy(displayCondition = Some(updatedDC)))
    })
  }
}

object ExpressionTerm extends ExpressionTermCompanion {
  val query = TableQuery[ExpressionTerms]

  val dependentStatementTerms = toManyThrough[ExpressionTerms, ExpressionTermsStatementTerms, ExpressionTerm](
    TableQuery[ExpressionTermsStatementTerms] innerJoin TableQuery[ExpressionTerms] on(_.statementTermId === _.id),
    _.id === _._1.expressionTermId)

  val dependentRules = toManyThrough[Rules, ExpressionTermsRules, Rule](
    TableQuery[ExpressionTermsRules] innerJoin TableQuery[Rules] on(_.ruleId === _.id),
    _.id === _._1.expressionTermId)
}

case class GenericTypeTerm(
    id: Option[ExpressionTermID],
    label: String,
    genericTypeId: GenericTypeID)

object GenericTypeTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.genericTypeId.isNotNull)

  val genericType = toOne[GenericTypes, GenericType](
    TableQuery[GenericTypes],
    _.genericTypeId === _.id)
}

case class DrugGroupTerm(
    id: Option[ExpressionTermID],
    label: String,
    drugGroupId: DrugGroupID)

object DrugGroupTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.drugGroupId.isNotNull)

  val drugGroup = toOne[DrugGroups, DrugGroup](
    TableQuery[DrugGroups],
    _.drugGroupId === _.id)
}

case class StatementTerm(
    id: Option[ExpressionTermID],
    label: String,
    statementTemplate: String,
    displayCondition: Option[ConditionExpression])

object StatementTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.statementTemplate.isNotNull)
}

case class AgeTerm(
    id: Option[ExpressionTermID],
    label: String,
    comparisonOperator: String,
    age: Int)

object AgeTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.age.isNotNull)
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
