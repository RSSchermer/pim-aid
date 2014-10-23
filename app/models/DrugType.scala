package models

import play.api.db.slick.Config.driver.simple._

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
