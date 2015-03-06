package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

case class DrugGroupID(value: Long) extends MappedTo[Long]

case class DrugGroup(
    id: Option[DrugGroupID],
    name: String)(implicit includes: Includes[DrugGroup])
  extends Entity[DrugGroup]
{
  type IdType = DrugGroupID

  val genericTypes = many(DrugGroup.genericTypes)
}

object DrugGroup extends EntityCompanion[DrugGroups, DrugGroup] {
  val query = TableQuery[DrugGroups]

  val genericTypes = toManyThrough[GenericTypes, DrugGroupsGenericTypes, GenericType](
    TableQuery[DrugGroupsGenericTypes] leftJoin TableQuery[GenericTypes] on(_.genericTypeId === _.id),
    _.id === _._1.drugGroupId)

  def findByName(name: String)(implicit s: Session): Option[DrugGroup] =
    query.filter(_.name.toLowerCase === name.toLowerCase).firstOption
}
