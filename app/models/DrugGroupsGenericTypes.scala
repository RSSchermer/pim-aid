package models

import play.api.db.slick.Config.driver.simple._

case class DrugGroupGenericType(drugGroupId: DrugGroupID, drugTypeId: GenericTypeID)

class DrugGroupsGenericTypes(tag: Tag) extends Table[DrugGroupGenericType](tag, "DRUG_GROUPS_GENERIC_TYPES"){
  def drugGroupId = column[DrugGroupID]("drug_group_id")
  def genericTypeId = column[GenericTypeID]("generic_type_id")

  def * = (drugGroupId, genericTypeId) <> (DrugGroupGenericType.tupled, DrugGroupGenericType.unapply)

  def pk = primaryKey("DRUG_GROUPS_GENERIC_TYPES_PK", (drugGroupId, genericTypeId))
  def drugGroup = foreignKey("DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def genericType = foreignKey("DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK", genericTypeId,
                               TableQuery[GenericTypes])(_.id)
}
