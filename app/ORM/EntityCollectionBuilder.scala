package ORM

import scala.slick.lifted.{Query, CanBeQueryCondition, Ordered}
import play.api.db.slick.Config.driver.simple._
import ORM.model._

abstract class AbstractEntityCollectionBuilder[T <: EntityTable[E], E <: Entity](implicit ev: BaseColumnType[E#IdType])
{
  val query: Query[T, E, Seq]

  val sideLoads: List[SideLoadable[T, E]] = List()

  def one(id: E#IdType) =
    new EntityInstanceBuilder[T, E](query.filter(_.id === id), sideLoads)

  def filter[C <: Column[_]](f: (T) => C)(implicit wt: CanBeQueryCondition[C]) = {
    new EntityCollectionBuilder[T, E](query.filter(f), sideLoads)
  }

  def sortBy[C](f: (T) => C)(implicit arg0: (C) ⇒ Ordered) =
    new EntityCollectionBuilder[T, E](query.sortBy(f), sideLoads)

  def take(num: Int) =
    new EntityCollectionBuilder[T, E](query.take(num), sideLoads)

  def drop(num: Int) =
    new EntityCollectionBuilder[T, E](query.drop(num), sideLoads)

  def list(implicit session: Session): Seq[E] = {
    val instances = query.list
    sideLoads.foldLeft(instances)((i, s) => s.sideLoadOn(i, query))
  }

  def find(key: E#IdType)(implicit s: Session): Option[E] =
    one(key).get

  def include(sideLoad: SideLoadable[T, E]*) =
    new EntityCollectionBuilder[T, E](query, sideLoads ++ sideLoad)
}

class EntityCollectionBuilder[T <: EntityTable[E], E <: Entity](
    val query: Query[T, E, Seq],
    override val sideLoads: List[SideLoadable[T, E]])(implicit ev: BaseColumnType[E#IdType])
  extends AbstractEntityCollectionBuilder[T, E]
