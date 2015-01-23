package ORM.model

import play.api.db.slick.Config.driver.simple._

trait RelationshipRep[Owner <: Entity, +Value] {
  val ownerId: Option[Owner#IdType]

  val isFetched: Boolean

  def get: Value

  def fetch(implicit session: Session): Value

  def getOrFetch(implicit session: Session): Value =
    if (isFetched) get else fetch
}

sealed abstract class One[From <: EntityTable[E], To <: Table[T], E <: Entity, T]
  extends RelationshipRep[E, Option[T]]
{
  val relationship: ToOne[From, To, E, T]

  def fetch(implicit session: Session): Option[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => None
  }
}

case class OneFetched[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: ToOne[From, To, E, T],
    override val ownerId: Option[E#IdType] = None,
    value: Option[T] = None)
  extends One[From, To, E, T]
{
  val isFetched: Boolean = true

  def get: Option[T] = value
}

case class OneUnfetched[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: ToOne[From, To, E, T],
    override val ownerId: Option[E#IdType])
  extends One[From, To, E, T]
{
  val isFetched: Boolean = false

  def get: Option[T] = throw new NoSuchElementException("OneUnfetched.get")
}

sealed abstract class Many[From <: EntityTable[E], To <: Table[T], E <: Entity, T]
  extends RelationshipRep[E, Seq[T]]
{
  val relationship: Relationship[From, To, E, T, Seq[T], Many[From, To, E, T]]

  def fetch(implicit session: Session): Seq[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => List()
  }
}

case class ManyFetched[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: Relationship[From, To, E, T, Seq[T], Many[From, To, E, T]],
    override val ownerId: Option[E#IdType] = None,
    values: Seq[T] = Seq())
  extends Many[From, To, E, T]
{
  val isFetched: Boolean = true

  def get: Seq[T] = values
}

case class ManyUnfetched[From <: EntityTable[E], To <: Table[T], E <: Entity, T](
    override val relationship: Relationship[From, To, E, T, Seq[T], Many[From, To, E, T]],
    override val ownerId: Option[E#IdType])
  extends Many[From, To, E, T]
{
  val isFetched: Boolean = false

  def get: Seq[T] = throw new NoSuchElementException("ManyUnfetched.get")
}
