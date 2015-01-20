package ORM

import scala.slick.lifted.{Query, CanBeQueryCondition, Ordered}
import play.api.db.slick.Config.driver.simple._
import ORM.model._

abstract class EntityCollectionRetriever[E <: Entity[_], T <: EntityTable[E]](implicit ev: BaseColumnType[E#IdType])
{
  val query: Query[T, E, Seq]

  def one(id: E#IdType): EntityInstanceRetriever[E, T] =
    new EntityInstanceBuilder[E, T](query.filter(_.id === id))

  def filter[C <: Column[_]](f: (T) => C)(implicit wt: CanBeQueryCondition[C]) = {
    new EntityCollectionBuilder[E, T](query.filter(f))
  }

  def sortBy[C](f: (T) => C)(implicit arg0: (C) ⇒ Ordered) =
    new EntityCollectionBuilder[E, T](query.sortBy(f))

  def take(num: Int) =
    new EntityCollectionBuilder[E, T](query.take(num))

  def drop(num: Int) =
    new EntityCollectionBuilder[E, T](query.drop(num))

  def list(implicit s: Session): Seq[E] =
    query.list

  def find(key: E#IdType)(implicit s: Session): Option[E] =
    one(key).get
}

class EntityCollectionBuilder[E <: Entity[_], T <: EntityTable[E]](val query: Query[T, E, Seq])
  (implicit ev: BaseColumnType[E#IdType]) extends EntityCollectionRetriever[E, T]
