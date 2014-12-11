package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class DrugGroupID(value: Long) extends MappedTo[Long]

case class DrugGroup(id: Option[DrugGroupID], name: String)

class DrugGroups(tag: Tag) extends Table[DrugGroup](tag, "DRUG_GROUPS"){
  def id = column[DrugGroupID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (DrugGroup.tupled, DrugGroup.unapply)

  def nameIndex = index("DRUG_GROUPS_NAME_INDEX", name, unique = true)
}

object DrugGroups {
  val all = TableQuery[DrugGroups]

  def list(implicit s: Session) = all.list

  def one(id: DrugGroupID) = all.filter(_.id === id)

  def find(id: DrugGroupID)(implicit s: Session): Option[DrugGroup] = one(id).firstOption

  def findByName(name: String)(implicit s: Session): Option[DrugGroup] =
    all.filter(_.name.toLowerCase === name.toLowerCase).firstOption

  def insert(drugGroup: DrugGroup)(implicit s: Session): DrugGroupID =
    all returning all.map(_.id) += drugGroup

  def update(id: DrugGroupID, drugGroup: DrugGroup)(implicit s: Session) = one(id).map(_.name).update(drugGroup.name)

  def delete(id: DrugGroupID)(implicit s: Session) = {
    TableQuery[DrugGroupsGenericTypes].filter(_.drugGroupId === id).delete
    one(id).delete
  }

  def genericTypesFor(id: DrugGroupID) = {
    for {
      (_, genericType) <-
      one(id) innerJoin
        TableQuery[DrugGroupsGenericTypes] on (_.id === _.drugGroupId) innerJoin
        TableQuery[GenericTypes] on (_._2.genericTypeId === _.id)
    } yield genericType
  }

  def genericTypeListFor(id: DrugGroupID)(implicit s: Session): List[GenericType] = {
    genericTypesFor(id).list
  }

  def genericTypeWithMedicationProductsListFor(id: DrugGroupID)(implicit s: Session)
  : Map[GenericType, List[MedicationProduct]] = {
    (for {
      ((genericType, _), medicationProduct) <-
        genericTypesFor(id) leftJoin
        TableQuery[GenericTypesMedicationProducts] on (_.id === _.genericTypeId) leftJoin
        TableQuery[MedicationProducts] on (_._2.medicationProductId === _.id)
    } yield (genericType, medicationProduct.?)).list.groupBy(_._1).map({ case (k,v) => (k, v.map(_._2).flatten) })
  }
}
