package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class DrugGroup(id: Option[Long], name: String)

class DrugGroups(tag: Tag) extends Table[DrugGroup](tag, "DRUG_GROUPS"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (DrugGroup.tupled, DrugGroup.unapply)
}

object DrugGroups {
  val all = TableQuery[DrugGroups]

  def list(implicit s: Session) = all.list

  def one(id: Long) = all.filter(_.id === id)

  def find(id: Long)(implicit s: Session): Option[DrugGroup] = one(id).firstOption

  def insert(drugGroup: DrugGroup)(implicit s: Session) = all.insert(drugGroup)

  def update(id: Long, drugGroup: DrugGroup)(implicit s: Session) = one(id).map(_.name).update(drugGroup.name)

  def delete(id: Long)(implicit s: Session) = one(id).delete
}
