package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import play.api.db.slick.Session
import schema._

case class GenericTypeID(value: Long) extends MappedTo[Long]

case class GenericType(
    id: Option[GenericTypeID],
    name: String,
    medicationProducts: Many[GenericTypes, MedicationProducts, GenericType, MedicationProduct] =
      ManyFetched(GenericType.medicationProducts),
    drugGroups: Many[GenericTypes, DrugGroups, GenericType, DrugGroup] =
      ManyFetched(GenericType.drugGroups))
  extends Entity { type IdType = GenericTypeID }

object GenericType extends EntityCompanion[GenericType, GenericTypes] {
  val query = TableQuery[GenericTypes]

  val medicationProducts = toManyThrough[MedicationProduct, (GenericTypeID, MedicationProductID), MedicationProducts, GenericTypesMedicationProducts](
    TableQuery[GenericTypesMedicationProducts] leftJoin TableQuery[MedicationProducts] on(_.medicationProductId === _.id),
    _.id === _._1.genericTypeId,
    lenser(_.medicationProducts)
  )

  val drugGroups = toManyThrough[DrugGroup, (DrugGroupID, GenericTypeID), DrugGroups, DrugGroupsGenericTypes](
    TableQuery[DrugGroupsGenericTypes] leftJoin TableQuery[DrugGroups] on(_.drugGroupId === _.id),
    _.id === _._1.genericTypeId,
    lenser(_.drugGroups)
  )

  def findByName(name: String)(implicit s: Session): Option[GenericType] =
    TableQuery[GenericTypes].filter(_.name.toLowerCase === name.toLowerCase).firstOption
}
