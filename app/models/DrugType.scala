package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class DrugType(id: Option[Long], name: String, genericTypeId: Option[Long])

class DrugTypes(tag: Tag) extends Table[DrugType](tag, "DRUG_TYPES"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def genericTypeId = column[Long]("generic_type_id", O.Nullable)

  def * = (id.?, name, genericTypeId.?) <> (DrugType.tupled, DrugType.unapply)

  def genericType = foreignKey("DRUG_TYPE_GENERIC_TYPE_FK", genericTypeId, TableQuery[DrugTypes])(_.id)

  def ? = (id.?, name.?, genericTypeId.?) <> (optionApply, optionUnapply)
  def optionApply(t: (Option[Long], Option[String], Option[Long])): Option[DrugType] = {
    t match {
      case (Some(id), Some(name), genericTypeId) => Some(DrugType(Some(id), name, genericTypeId))
      case (None, _, _) => None
    }
  }
  def optionUnapply(oc: Option[DrugType]): Option[(Option[Long], Option[String], Option[Long])] = None
}

object DrugTypes {
  val all = TableQuery[DrugTypes]
  val drugGroupsTypes = TableQuery[DrugGroupsTypes]
  val generic = all.filter(_.genericTypeId.isNull)

  def list(implicit s: Session) = all.list

  def genericTypes(implicit s: Session) = generic.list

  def genericTypes(excludedId: Long)(implicit s: Session) = generic.filter(_.id =!= excludedId).list

  def one(id: Long) = all.filter(_.id === id)

  def find(id: Long)(implicit s: Session): Option[DrugType] = one(id).firstOption

  def findWithGroupIds(id: Long)(implicit s: Session): Option[(DrugType, List[Long])] = {
    find(id) match {
      case Some(drugType) => Some(drugType, groupIdsFor(id).list)
      case _ => None
    }
  }

  def groupIdsFor(id: Long) = drugGroupsTypes.filter(_.drugTypeId === id).map(_.drugGroupId)

  def insert(drugType: DrugType, drugGroupIds: List[Long])(implicit s: Session) = {
    val typeId = all returning all.map(_.id) += drugType

    drugGroupIds.foreach(groupId => drugGroupsTypes.insert(DrugGroupType(groupId, typeId)))
  }

  def update(id: Long, drugType: DrugType, drugGroupIds: List[Long])(implicit s: Session) = {
    one(id).map(x => (x.name, x.genericTypeId.?)).update((drugType.name, drugType.genericTypeId))

    drugGroupsTypes.filter(_.drugTypeId === id).delete
    drugGroupIds.map(x => DrugGroupType(x, id)).foreach(x => drugGroupsTypes.insert(x))
  }

  def delete(id: Long)(implicit s: Session) = {
    drugGroupsTypes.filter(_.drugTypeId === id).delete
    one(id).delete
  }
}
