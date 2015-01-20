package ORM

import ORM.model._
import play.api.db.slick.Config.driver.simple._

trait EntityInstanceRetriever[E <: Entity[_], T <: EntityTable[_]] {
  val query: Query[T, E, Seq]

  def get(implicit s: Session): Option[E] =
    query.firstOption
}

class EntityInstanceBuilder[E <: Entity[_], T <: EntityTable[_]](val query: Query[T, E, Seq])
  extends EntityInstanceRetriever[E, T]
