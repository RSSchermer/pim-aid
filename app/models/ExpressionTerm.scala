package models

import play.api.db.slick.Config.driver.simple._
import schema._
import ORM.model._

case class ExpressionTerm(
    label: String,
    genericTypeId: Option[GenericTypeID],
    drugGroupId: Option[DrugGroupID],
    statementTemplate: Option[String],
    displayCondition: Option[String],
    comparisonOperator: Option[String],
    age: Option[Int],
    genericType: One[ExpressionTerm, GenericType] = OneFetched(GenericTypeTerm.genericType),
    drugGroup: One[ExpressionTerm, DrugGroup] = OneFetched(DrugGroupTerm.drugGroup))
  extends Entity
{
  type IdType = String

  val id = Some(label)
}

object ExpressionTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms]
}

object GenericTypeTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.genericTypeId.isNotNull)

  val genericType = toOne[GenericType, GenericTypes](
    TableQuery[GenericTypes],
    _.genericTypeId === _.id,
    lenser(_.genericType)
  )
}

object DrugGroupTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.drugGroupId.isNotNull)

  val drugGroup = toOne[DrugGroup, DrugGroups](
    TableQuery[DrugGroups],
    _.drugGroupId === _.id,
    lenser(_.drugGroup)
  )
}

object StatementTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.statementTemplate.isNotNull)
}

object AgeTerm extends EntityCompanion[ExpressionTerms, ExpressionTerm] {
  val query = TableQuery[ExpressionTerms].filter(_.age.isNotNull)
}
