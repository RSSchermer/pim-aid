package ORM.model

import play.api.db.slick.Config.driver.simple._
import monocle._

abstract class Relationship[From <: EntityTable[E], To <: Table[T], I : BaseColumnType, E <: Entity[_], T, V,
                            R <: RelationshipRep[E, T, V]] {
  type OwnerType = E
  type TargetType = T
  type ValueType = V

  val propertyLens: Lens[E, R]

  def fetchFor(id: I)(implicit session: Session): V

  def fetchFor(instance: E)(implicit session: Session): V

  def fetchOn(instance: E)(implicit session: Session): E
}

class ToOne[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    override val propertyLens: Lens[E, One[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E#IdType, E, T, Option[T], One[From, To, E, T]]
{
  def fetchFor(id: E#IdType)(implicit session: Session): Option[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2).firstOption

  def fetchFor(instance: E)(implicit session: Session): Option[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => None
    }

  def fetchOn(instance: E)(implicit session: Session): E =
    propertyLens.set(OneFetched(this, instance.id, fetchFor(instance)))(instance)
}

class ToMany[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    override val propertyLens: Lens[E, Many[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E#IdType, E, T, Seq[T], Many[From, To, E, T]]
{
  def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2).list

  def fetchFor(instance: E)(implicit session: Session): Seq[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => List()
    }

  def fetchOn(instance: E)(implicit session: Session): E =
    propertyLens.set(ManyFetched(this, instance.id, fetchFor(instance)))(instance)
}

class ToManyThrough[From <: EntityTable[E], Through <: Table[J], To <: Table[T], E <: Entity[_], J, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[(Through, To), (J, T), Seq],
    val joinCondition: (From, (Through, To)) => Column[Boolean],
    override val propertyLens: Lens[E, Many[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E#IdType, E, T, Seq[T], Many[From, To, E, T]]
{
  def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2._2).list

  def fetchFor(instance: E)(implicit session: Session): Seq[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => List()
    }

  def fetchOn(instance: E)(implicit session: Session): E =
    propertyLens.set(ManyFetched(this, instance.id, fetchFor(instance)))(instance)
}
