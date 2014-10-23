package models

import play.api.db.slick.Config.driver.simple._

case class DrugGroupType(drugGroupId: Long, drugTypeId: Long)

class DrugGroupsTypes(tag: Tag) extends Table[DrugGroupType](tag, "DRUG_GROUPS_TYPES"){
  def drugGroupId = column[Long]("drug_group_id")
  def drugTypeId = column[Long]("drug_type_id")

  def * = (drugGroupId, drugTypeId) <> (DrugGroupType.tupled, DrugGroupType.unapply)

  def pk = primaryKey("DRUG_GROUPS_TYPES_PK", (drugGroupId, drugTypeId))
  def drugGroup = foreignKey("DRUG_GROUPS_TYPES_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def drugType = foreignKey("DRUG_GROUPS_TYPES_TYPE_FK", drugTypeId, TableQuery[DrugTypes])(_.id)
}
