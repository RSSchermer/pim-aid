package ORM.model

import play.api.db.slick.Config.driver.simple._

abstract class EntityTable[E <: Entity[_]](
    tag: Tag,
    schemaName: Option[String],
    tableName: String)(implicit val colType: BaseColumnType[E#IdType])
  extends Table[E](tag, schemaName, tableName)
{
  def this(tag: Tag, tableName: String)(implicit mapping: BaseColumnType[E#IdType]) =
    this(tag, None, tableName)

  def id: Column[E#IdType]
}
