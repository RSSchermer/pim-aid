package models

import models.Profile._
import models.Profile.driver.simple._

case class ExpressionTerm(
    label: String,
    genericTypeId: Option[GenericTypeID],
    drugGroupId: Option[DrugGroupID],
    statementTemplate: Option[String],
    displayCondition: Option[String],
    comparisonOperator: Option[String],
    age: Option[Int])(implicit includes: Includes[ExpressionTerm])
  extends Entity[ExpressionTerm]
{
  type IdType = String

  val id = Some(label)

  val genericType = one(GenericTypeTerm.genericType)
  val drugGroup = one(DrugGroupTerm.drugGroup)
}

object ExpressionTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms]
}

object GenericTypeTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.genericTypeId.isNotNull)

  val genericType = toOne[GenericTypes, GenericType](
    TableQuery[GenericTypes],
    _.genericTypeId === _.id)
}

object DrugGroupTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.drugGroupId.isNotNull)

  val drugGroup = toOne[DrugGroups, DrugGroup](
    TableQuery[DrugGroups],
    _.drugGroupId === _.id)
}

object StatementTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.statementTemplate.isNotNull)
}

object AgeTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.age.isNotNull)
}
