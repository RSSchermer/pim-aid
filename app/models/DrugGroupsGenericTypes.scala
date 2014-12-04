package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class DrugGroupGenericType(drugGroupId: DrugGroupID, genericTypeId: GenericTypeID)

class DrugGroupsGenericTypes(tag: Tag) extends Table[DrugGroupGenericType](tag, "DRUG_GROUPS_GENERIC_TYPES"){
  def drugGroupId = column[DrugGroupID]("drug_group_id")
  def genericTypeId = column[GenericTypeID]("generic_type_id")

  def * = (drugGroupId, genericTypeId) <> (DrugGroupGenericType.tupled, DrugGroupGenericType.unapply)

  def pk = primaryKey("DRUG_GROUPS_GENERIC_TYPES_PK", (drugGroupId, genericTypeId))
  def drugGroup = foreignKey("DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def genericType = foreignKey("DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK", genericTypeId,
                               TableQuery[GenericTypes])(_.id)
}

object DrugGroupsGenericTypes {
  val all = TableQuery[DrugGroupsGenericTypes]

  def one(drugGroupId: DrugGroupID, genericTypeId: GenericTypeID) =
    all.filter(x => x.drugGroupId === drugGroupId && x.genericTypeId === genericTypeId)

  def insert(drugGroupGenericType: DrugGroupGenericType)(implicit s: Session) = all.insert(drugGroupGenericType)

  def delete(drugGroupId: DrugGroupID, genericTypeId: GenericTypeID)(implicit s: Session) =
    one(drugGroupId, genericTypeId).delete
}