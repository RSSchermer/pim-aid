package model

import entitytled.Entitytled

trait GenericTypeComponent {
  self: Entitytled
    with DrugGroupComponent
    with MedicationProductComponent
  =>

  import driver.api._

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

  class GenericTypes(tag: Tag) extends EntityTable[GenericType, GenericTypeID](tag, "GENERIC_TYPES") {
    def id = column[GenericTypeID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")

    def * = (id.?, name) <>((GenericType.apply _).tupled, GenericType.unapply)

    def nameIndex = index("GENERIC_TYPES_NAME_INDEX", name, unique = true)
  }
}