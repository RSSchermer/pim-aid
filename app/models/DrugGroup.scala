package models

import play.api.db.slick.Config.driver.simple._

case class DrugGroup(id: Option[Long], name: String)

class DrugGroups(tag: Tag) extends Table[DrugGroup](tag, "DRUG_GROUPS"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (DrugGroup.tupled, DrugGroup.unapply)
}
