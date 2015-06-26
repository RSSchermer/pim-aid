package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

case class DrugGroupID(value: Long) extends MappedTo[Long]

case class DrugGroup(
    id: Option[DrugGroupID],
    name: String)(implicit includes: Includes[DrugGroup])
  extends Entity[DrugGroup, DrugGroupID]
{
  val genericTypes = many(DrugGroup.genericTypes)
}

object DrugGroup extends EntityCompanion[DrugGroups, DrugGroup, DrugGroupID] {
  val genericTypes = toManyThrough[GenericTypes, DrugGroupsGenericTypes, GenericType]

  def hasName(name: String): Query[DrugGroups, DrugGroup, Seq] =
    all.filter(_.name.toLowerCase === name.toLowerCase)
}
