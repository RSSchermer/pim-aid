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

    term.dependentRules.getOrFetch.foreach((r: Rule) => {
      val updatedCE = r.conditionExpression.replaceLabel(oldLabel, term.label)

      // Update rule via TableQuery to bypass afterSave hook, because it will look
      // for an expression term with a label which does not yet exist at this time.
      TableQuery[Rules].filter(_.id === r.id).update(r.copy(conditionExpression = updatedCE))
    })
  }

  private def updateDependentStatementTermConditions(term: ExpressionTerm)(implicit s: Session): Unit = {
    val oldLabel = ExpressionTerm.find(term.id.get).get.label

    term.dependentStatementTerms.getOrFetch.foreach((st: ExpressionTerm) => {
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

object GenericTypeTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.genericTypeId.isNotNull)

  val genericType = toOne[GenericTypes, GenericType](
    TableQuery[GenericTypes],
    _.genericTypeId === _.id)
}

object DrugGroupTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.drugGroupId.isNotNull)

  val drugGroup = toOne[DrugGroups, DrugGroup](
    TableQuery[DrugGroups],
    _.drugGroupId === _.id)
}

object StatementTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.statementTemplate.isNotNull)
}

object AgeTerm extends ExpressionTermCompanion {
  override val query = TableQuery[ExpressionTerms].filter(_.age.isNotNull)
}
