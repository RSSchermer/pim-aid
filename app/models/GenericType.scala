package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class GenericTypeID(value: Long) extends MappedTo[Long]

case class GenericType(id: Option[GenericTypeID], name: String)

class GenericTypes(tag: Tag) extends Table[GenericType](tag, "GENERIC_TYPES"){
  def id = column[GenericTypeID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (GenericType.tupled, GenericType.unapply)

  def ? = (id.?, name.?) <> (optionApply, optionUnapply)
  def optionApply(t: (Option[GenericTypeID], Option[String])): Option[GenericType] = {
    t match {
      case (Some(id), Some(name)) => Some(GenericType(Some(id), name))
      case (None, _) => None
    }
  }
  def optionUnapply(oc: Option[GenericType]): Option[(Option[GenericTypeID], Option[String])] = None

  def nameIndex = index("GENERIC_TYPES_NAME_INDEX", name, unique = true)
}

object GenericTypes {
  val all = TableQuery[GenericTypes]

  def list(implicit s: Session) = all.list

  def one(id: GenericTypeID) = all.filter(_.id === id)

  def find(id: GenericTypeID)(implicit s: Session): Option[GenericType] = one(id).firstOption

  def findByName(name: String)(implicit s: Session): Option[GenericType] =
    all.filter(_.name.toLowerCase === name.toLowerCase).firstOption

  def groupIdListFor(id: GenericTypeID)(implicit s: Session): List[DrugGroupID] =
    TableQuery[DrugGroupsGenericTypes].filter(_.genericTypeId === id).map(_.drugGroupId).list

  def insert(genericType: GenericType)(implicit s: Session): GenericTypeID =
    all returning all.map(_.id) += genericType

  def update(id: GenericTypeID, genericType: GenericType)(implicit s: Session) =
    one(id).map(x => x.name).update(genericType.name)

  def delete(id: GenericTypeID)(implicit s: Session) = {
    TableQuery[DrugGroupsGenericTypes].filter(_.genericTypeId === id).delete
    one(id).delete
  }
}
