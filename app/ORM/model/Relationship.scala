package ORM.model

import play.api.db.slick.Config.driver.simple._
import monocle._

trait Relationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, Value, Rep <: RelationshipRep[E, Value]]
  extends SideLoadable[From, E]
{
  val propertyLens: Lens[E, Rep]

  def setOn(instance: E, value: Value): E

  def joinQueryFor(id: E#IdType): Query[(From, To), (E, T), Seq]

  def joinQueryFor(query: Query[From, E, Seq]): Query[(From, To), (E, T), Seq]

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

  def include(sideLoad: SideLoadable[To, T]*): Relationship[From, To, E, T, Value, Rep]
}

abstract class DirectRelationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, Value, Rep <: RelationshipRep[E, Value]]
    (implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E, T, Value, Rep]
{
  val fromQuery: Query[From, E, Seq]

  val toQuery: Query[To, T, Seq]

  val joinCondition: (From, To) => Column[Boolean]

  def joinQueryFor(id: E#IdType): Query[(From, To), (E, T), Seq] =
    fromQuery.filter(_.id === id).innerJoin(toQuery).on(joinCondition)

  def joinQueryFor(query: Query[From, E, Seq]): Query[(From, To), (E, T), Seq] =
    query.innerJoin(toQuery).on(joinCondition)
}

abstract class ThroughRelationship[From <: EntityTable[E], Through <: Table[J], To <: Table[T], E <: Entity, J, T, Value, Rep <: RelationshipRep[E, Value]]
    (implicit mapping: BaseColumnType[E#IdType])
  extends Relationship[From, To, E, T, Value, Rep]
{
  val fromQuery: Query[From, E, Seq]

  val toQuery: Query[(Through, To), (J, T), Seq]

  val joinCondition: (From, (Through, To)) => Column[Boolean]

  def joinQueryFor(id: E#IdType): Query[(From, To), (E, T), Seq] =
    fromQuery.filter(_.id === id).leftJoin(toQuery).on(joinCondition).map(x => (x._1, x._2._2))

  def joinQueryFor(query: Query[From, E, Seq]): Query[(From, To), (E, T), Seq] =
    query.leftJoin(toQuery).on(joinCondition).map(x => (x._1, x._2._2))
}

trait ToOneRelationship[E <: Entity, T] {
  self: Relationship[_ <: EntityTable[E], _ <: Table[T], E, T, Option[T], One[E, T]] =>

  def setOn(instance: E, value: Option[T]): E =
    propertyLens.set(OneFetched(this, instance.id, value))(instance)

  def fetchFor(id: E#IdType)(implicit session: Session): Option[T] =
    joinQueryFor(id).map(_._2).firstOption

  def fetchFor(instance: E)(implicit session: Session): Option[T] = instance.id match {
    case Some(id) => fetchFor(id)
    case _ => None
  }
}

trait ToManyRelationship[E <: Entity, T] {
  self: Relationship[_ <: EntityTable[E], _ <: Table[T], E, T, Seq[T], Many[E, T]] =>

  def setOn(instance: E, values: Seq[T]): E =
    propertyLens.set(ManyFetched(this, instance.id, values))(instance)

  def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] =
    joinQueryFor(id).map(_._2).list

  def fetchFor(instance: E)(implicit session: Session): Seq[T] = instance.id match {
    case Some(id) => fetchFor(id)
    case _ => List()
  }
}

class ToOne[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    val propertyLens: Lens[E, One[E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends DirectRelationship[From, To, E, T, Option[T], One[E, T]]
  with ToOneRelationship[E, T]
{
  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Option[T]] =
    joinQueryFor(query).list.groupBy(_._1).map(x => (x._1, x._2.headOption.map(_._2)))

  def include(sideLoad: SideLoadable[To, T]*): OneSideLoading[From, To, E, T] =
    new OneSideLoading(this, sideLoad)
}

class ToMany[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[To, T, Seq],
    val joinCondition: (From, To) => Column[Boolean],
    val propertyLens: Lens[E, Many[E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends DirectRelationship[From, To, E, T, Seq[T], Many[E, T]]
  with ToManyRelationship[E, T]
{
  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Seq[T]] =
    joinQueryFor(query).list
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2)))

  def include(sideLoad: SideLoadable[To, T]*): ManySideLoading[From, To, E, T] =
    new ManySideLoading(this, sideLoad)
}

class ToOneThrough[From <: EntityTable[E], Through <: Table[J], To <: Table[T], E <: Entity, J, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[(Through, To), (J, T), Seq],
    val joinCondition: (From, (Through, To)) => Column[Boolean],
    val propertyLens: Lens[E, One[E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends ThroughRelationship[From, Through, To, E, J, T, Option[T], One[E, T]]
  with ToOneRelationship[E, T]
{
  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Option[T]] =
    joinQueryFor(query).list.groupBy(_._1).map(x => (x._1, x._2.headOption.map(_._2)))

  def include(sideLoad: SideLoadable[To, T]*): OneSideLoading[From, To, E, T] =
    new OneSideLoading(this, sideLoad)
}

class ToManyThrough[From <: EntityTable[E], Through <: Table[J], To <: Table[T], E <: Entity, J, T](
    val fromQuery: Query[From, E, Seq],
    val toQuery: Query[(Through, To), (J, T), Seq],
    val joinCondition: (From, (Through, To)) => Column[Boolean],
    val propertyLens: Lens[E, Many[E, T]])(implicit mapping: BaseColumnType[E#IdType])
  extends ThroughRelationship[From, Through, To, E, J, T, Seq[T], Many[E, T]]
  with ToManyRelationship[E, T]
{
  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Seq[T]] =
    joinQueryFor(query).list
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2)))

  def include(sideLoad: SideLoadable[To, T]*): ManySideLoading[From, To, E, T] =
    new ManySideLoading(this, sideLoad)
}

abstract class WrappingRelationship[From <: EntityTable[E], To <: Table[T], E <: Entity, T, Value, Rep <: RelationshipRep[E, Value]](
    val relationship: Relationship[From, To, E, T, Value, Rep])
  extends Relationship[From, To, E, T, Value, Rep]
{
  val propertyLens: Lens[E, Rep] = relationship.propertyLens

  def setOn(instance: E, value: Value): E =
    relationship.setOn(instance, value)

  def joinQueryFor(id: E#IdType): Query[(From, To), (E, T), Seq] =
    relationship.joinQueryFor(id)

  def joinQueryFor(query: Query[From, E, Seq]): Query[(From, To), (E, T), Seq] =
    relationship.joinQueryFor(query)

  def fetchFor(id: E#IdType)(implicit session: Session): Value =
    relationship.fetchFor(id)

  def fetchFor(instance: E)(implicit session: Session): Value =
    relationship.fetchFor(instance)

  def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Value] =
    relationship.fetchFor(query)
}

class OneSideLoading[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: Relationship[From, To, E, T, Option[T], One[E, T]],
    val sideLoads: Seq[SideLoadable[To, T]])
  extends WrappingRelationship[From, To, E, T, Option[T], One[E, T]](relationship)
{
  override def fetchFor(id: E#IdType)(implicit session: Session): Option[T] =
    relationship.fetchFor(id) match {
      case Some(instance) =>
        val toQuery = relationship.joinQueryFor(id).map(_._2)
        Some(sideLoads.foldLeft(instance)((i, s) => s.sideLoadOn(i, toQuery)))
      case _ => None
    }

  override def fetchFor(instance: E)(implicit session: Session): Option[T] = instance.id match {
    case Some(id) => fetchFor(id)
    case _ => None
  }

  override def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Option[T]] = {
    val toQuery = joinQueryFor(query).map(_._2)
    relationship.fetchFor(query)
      .map({
        case (e, Some(t)) =>
          (e, Some(sideLoads.foldLeft(t)((i, s) => s.sideLoadOn(i, toQuery))))
        case x@_ => x
      })
  }

  def include(sideLoad: SideLoadable[To, T]*): OneSideLoading[From, To, E, T] =
    new OneSideLoading(relationship, sideLoads ++ sideLoad)
}

class ManySideLoading[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: Relationship[From, To, E, T, Seq[T], Many[E, T]],
    val sideLoads: Seq[SideLoadable[To, T]])
  extends WrappingRelationship[From, To, E, T, Seq[T], Many[E, T]](relationship)
{
  override def fetchFor(id: E#IdType)(implicit session: Session): Seq[T] = {
    val toQuery = relationship.joinQueryFor(id).map(_._2)
    sideLoads.foldLeft(relationship.fetchFor(id).toList)((i, s) => s.sideLoadOn(i, toQuery))
  }

  override def fetchFor(instance: E)(implicit session: Session): Seq[T] = instance.id match {
    case Some(id) => fetchFor(id)
    case _ => Seq()
  }

  override def fetchFor(query: Query[From, E, Seq])(implicit session: Session): Map[E, Seq[T]] = {
    val toQuery = joinQueryFor(query).map(_._2)
    relationship.fetchFor(query)
      .map(x => (x._1, sideLoads.foldLeft(x._2)((i, s) => s.sideLoadOn(i.toList, toQuery))))
  }

  def include(sideLoad: SideLoadable[To, T]*): ManySideLoading[From, To, E, T] =
    new ManySideLoading(relationship, sideLoads ++ sideLoad)
}
