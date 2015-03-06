package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

case class GenericTypeID(value: Long) extends MappedTo[Long]

case class GenericType(
    id: Option[GenericTypeID],
    name: String)(implicit includes: Includes[GenericType])
  extends Entity[GenericType]
{
  type IdType = GenericTypeID

  val medicationProducts = many(GenericType.medicationProducts)
  val drugGroups = many(GenericType.drugGroups)
}

object GenericType extends EntityCompanion[GenericTypes, GenericType] {
  val query = TableQuery[GenericTypes]

  val medicationProducts = toManyThrough[MedicationProducts, GenericTypesMedicationProducts, MedicationProduct](
    TableQuery[GenericTypesMedicationProducts] leftJoin TableQuery[MedicationProducts] on(_.medicationProductId === _.id),
    _.id === _._1.genericTypeId)

  val drugGroups = toManyThrough[DrugGroups, DrugGroupsGenericTypes, DrugGroup](
    TableQuery[DrugGroupsGenericTypes] leftJoin TableQuery[DrugGroups] on(_.drugGroupId === _.id),
    _.id === _._1.genericTypeId)

  def findByName(name: String)(implicit s: Session): Option[GenericType] =
    TableQuery[GenericTypes].filter(_.name.toLowerCase === name.toLowerCase).firstOption
}
