package ORM.model

import play.api.db.slick.Config.driver.simple._
import monocle._

trait SideLoadable[T <: EntityTable[E], E <: Entity] {
  def sideLoadOn(instances: List[E], query: Query[T, E, Seq])(implicit session: Session): List[E]

  def sideLoadOn(instance: E, query: Query[T, E, Seq])(implicit session: Session): E
}

abstract class Relationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, Value, Rep <: RelationshipRep[E, Value]]
  extends SideLoadable[From, E]
{
  val propertyLens: Lens[E, Rep]

  def setOn(instance: E, value: Value): E

  def fetchFor(id: E#IdType)(implicit session: Session): Value

  def fetchFor(instance: E)(implicit session: Session): Value

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Value]

  def fetchOn(instance: E)(implicit session: Session): E =
    setOn(instance, fetchFor(instance))

  def sideLoadOn(instances: List[E], query: Query[From, E, Seq])(implicit session: Session): List[E] = {
    val m = fetchFor(query)

    instances.map(x => m.get(x) match {
      case Some(value) => setOn(x, value)
      case _ => x
    })
  }

  def sideLoadOn(instance: E, query: Query[From, E, Seq])(implicit session: Session): E =
    fetchFor(query).get(instance) match {
      case Some(value) => setOn(instance, value)
      case _ => instance
    }
}

class ToOne[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    override val propertyLens: Lens[E, One[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E, T, Option[T], One[From, To, E, T]]
{
  def setOn(instance: E, value: Option[T]): E =
    propertyLens.set(OneFetched(this, instance.id, value))(instance)

  def fetchFor(id: E#IdType)(implicit session: Session): Option[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2).firstOption

  def fetchFor(instance: E)(implicit session: Session): Option[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => None
    }

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Option[T]] =
    query.leftJoin(toQuery).on(joinCondition).list
      .groupBy(_._1)
      .map(x => (x._1, x._2.headOption.map(_._2)))
}

class ToMany[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    override val propertyLens: Lens[E, Many[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E, T, Seq[T], Many[From, To, E, T]]
{
  def setOn(instance: E, values: Seq[T]): E =
    propertyLens.set(ManyFetched(this, instance.id, values))(instance)

  def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2).list

  def fetchFor(instance: E)(implicit session: Session): Seq[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => List()
    }

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Seq[T]] =
    query.leftJoin(toQuery).on(joinCondition).list
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2)))
}

class ToManyThrough[From <: EntityTable[E], Through <: Table[J], To <: Table[T], E <: Entity, J, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[(Through, To), (J, T), Seq],
    val joinCondition: (From, (Through, To)) => Column[Boolean],
    override val propertyLens: Lens[E, Many[From, To, E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E, T, Seq[T], Many[From, To, E, T]]
{
  def setOn(instance: E, values: Seq[T]): E =
    propertyLens.set(ManyFetched(this, instance.id, values))(instance)

  def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] =
    fromQuery.innerJoin(toQuery).on(joinCondition).filter(_._1.id === id).map(_._2._2).list

  def fetchFor(instance: E)(implicit session: Session): Seq[T] =
    instance.id match {
      case Some(id) => fetchFor(id)
      case _ => List()
    }

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Seq[T]] =
    query.leftJoin(toQuery).on(joinCondition).map(x => (x._1, x._2._2)).list
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2)))
}

class WrappingRelationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, V, R <: RelationshipRep[E, V]](
    val relationship: Relationship[From, To, E, T, V, R])
  extends Relationship[From, To, E, T, V, R]
{
  val propertyLens: Lens[E, R] = relationship.propertyLens

  def setOn(instance: E, value: V): E =
    relationship.setOn(instance, value)

  def fetchFor(id: E#IdType)(implicit session: Session): V =
    relationship.fetchFor(id)

  def fetchFor(instance: E)(implicit session: Session): V =
    relationship.fetchFor(instance)

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, V] =
    relationship.fetchFor(query)
}

//class SideLoadingRelationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, V, R <: RelationshipRep[E, T, V]](
//    override val relationship: Relationship[From, To, E, T, V, R],
//    val sideLoads: Seq[Relationship[To, _, T, _, _, _]])
//  extends WrappingRelationship[From, To, E, T, V, R](relationship)
//{
//
//}
