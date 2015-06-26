package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

case class GenericTypeID(value: Long) extends MappedTo[Long]

case class GenericType(
    id: Option[GenericTypeID],
    name: String)(implicit includes: Includes[GenericType])
  extends Entity[GenericType, GenericTypeID]
{
  val medicationProducts = many(GenericType.medicationProducts)
  val drugGroups = many(GenericType.drugGroups)
}

object GenericType extends EntityCompanion[GenericTypes, GenericType, GenericTypeID] {
  val medicationProducts = toManyThrough[MedicationProducts, GenericTypesMedicationProducts, MedicationProduct]
  val drugGroups = toManyThrough[DrugGroups, DrugGroupsGenericTypes, DrugGroup]

  def hasName(name: String): Query[GenericTypes, GenericType, Seq] =
    all.filter(_.name.toLowerCase === name.toLowerCase)
}
