package ORM

import ORM.model._
import play.api.db.slick.Config.driver.simple._

abstract class AbstractEntityInstanceBuilder[T <: EntityTable[E], E <: Entity] {
  val query: Query[T, E, Seq]

  val sideLoads: List[SideLoadable[T, E]] = List()

  def get(implicit session: Session): Option[E] = query.firstOption match {
    case Some(instance) =>
      Some(sideLoads.foldLeft(instance)((i, s) => s.sideLoadOn(i, query)))
    case _ => None
  }

  def include(sideLoad: SideLoadable[T, E]*) =
    new EntityInstanceBuilder[T, E](query, sideLoads ++ sideLoad)
}

class EntityInstanceBuilder[T <: EntityTable[E], E <: Entity](
    val query: Query[T, E, Seq],
    override val sideLoads: List[SideLoadable[T, E]])
  extends AbstractEntityInstanceBuilder[T, E]
