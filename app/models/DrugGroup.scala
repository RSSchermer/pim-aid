package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session
import ORM.model._
import ORM.EntityRepository
import schema._

case class DrugGroupID(value: Long) extends MappedTo[Long]

case class DrugGroup(
    id: Option[DrugGroupID],
    name: String,
    genericTypes: Many[DrugGroups, GenericTypes, DrugGroup, GenericType] =
      ManyFetched(DrugGroup.genericTypes))
  extends Entity { type IdType = DrugGroupID }

object DrugGroup extends EntityCompanion[DrugGroups, DrugGroup] {
  val query = TableQuery[DrugGroups]

  val genericTypes = toManyThrough[GenericType, (DrugGroupID, GenericTypeID), GenericTypes, DrugGroupsGenericTypes](
    TableQuery[DrugGroupsGenericTypes] leftJoin TableQuery[GenericTypes] on(_.genericTypeId === _.id),
    _.id === _._1.drugGroupId,
    lenser(_.genericTypes)
  )

  def findByName(name: String)(implicit s: Session): Option[DrugGroup] =
    TableQuery[DrugGroups].filter(_.name.toLowerCase === name.toLowerCase).firstOption
}
