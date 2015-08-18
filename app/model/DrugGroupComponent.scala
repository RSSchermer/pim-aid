package model

import entitytled.Entitytled

trait DrugGroupComponent {
  self: Entitytled
    with GenericTypeComponent
  =>

  import driver.api._

  case class DrugGroupID(value: Long) extends MappedTo[Long]

  case class DrugGroup(
      id: Option[DrugGroupID],
      name: String)(implicit includes: Includes[DrugGroup])
    extends Entity[DrugGroup, DrugGroupID]
  {
    val genericTypes = many(DrugGroup.genericTypes)
  }

  object DrugGroup extends EntityCompanion[DrugGroups, DrugGroup, DrugGroupID] {
    val genericTypes = toManyThrough[GenericTypes, DrugGroupsGenericTypes, GenericType]

    def hasName(name: String): Query[DrugGroups, DrugGroup, Seq] =
      all.filter(_.name.toLowerCase === name.toLowerCase)
  }

  class DrugGroups(tag: Tag) extends EntityTable[DrugGroup, DrugGroupID](tag, "DRUG_GROUPS") {
    def id = column[DrugGroupID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")

    def * = (id.?, name) <>((DrugGroup.apply _).tupled, DrugGroup.unapply)

    def nameIndex = index("DRUG_GROUPS_NAME_INDEX", name, unique = true)
  }

  class DrugGroupsGenericTypes(tag: Tag)
    extends Table[(DrugGroupID, GenericTypeID)](tag, "DRUG_GROUPS_GENERIC_TYPES")
  {
    def drugGroupId = column[DrugGroupID]("drug_group_id")
    def genericTypeId = column[GenericTypeID]("generic_type_id")

    def * = (drugGroupId, genericTypeId)

    def pk = primaryKey("DRUG_GROUPS_GENERIC_TYPES_PK", (drugGroupId, genericTypeId))

    def drugGroup = foreignKey("DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK", drugGroupId,
      TableQuery[DrugGroups])(_.id, onDelete = ForeignKeyAction.Cascade)
    def genericType = foreignKey("DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK", genericTypeId,
      TableQuery[GenericTypes])(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}