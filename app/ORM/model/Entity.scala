package ORM.model

import ORM.EntityRepository
import monocle._
import monocle.macros.Lenser
import play.api.db.slick.Config.driver.simple._

abstract class Entity {
  type IdType

  val id: Option[IdType]
}

abstract class EntityCompanion[T <: EntityTable[E], E <: Entity](implicit ev: BaseColumnType[E#IdType])
  extends EntityRepository[T, E]
{
  val lenser = Lenser[E]

  protected def toOne[M, To <: Table[M]](
    toQuery: Query[To, M, Seq],
    joinCondition: (T, To) => Column[Boolean],
    propertyLens: Lens[E, One[E, M]]
  ): ToOne[T, To, E, M] = new ToOne(query, toQuery, joinCondition, propertyLens)

  protected def toMany[M, To <: Table[M]](
    toQuery: Query[To, M, Seq],
    joinCondition: (T, To) => Column[Boolean],
    propertyLens: Lens[E, Many[E, M]]
  ): ToMany[T, To, E, M] = new ToMany(query, toQuery, joinCondition, propertyLens)

  protected def toManyThrough[M, J, To <: Table[M], Through <: Table[J]](
    toQuery: Query[(Through, To), (J, M), Seq],
    joinCondition: (T, (Through, To)) => Column[Boolean],
    propertyLens: Lens[E, Many[E, M]]
  ): ToManyThrough[T, Through, To, E, J, M] =
    new ToManyThrough(query, toQuery, joinCondition, propertyLens)
}
