package ORM.model

import play.api.db.slick.Config.driver.simple._

trait RelationshipRep[E <: Entity[_], T, V] {
  val ownerId: Option[E#IdType]

  val isFetched: Boolean

  def get: V

  def fetch(implicit session: Session): V

  def getOrFetch(implicit session: Session): V =
    if (isFetched) get else fetch
}

sealed abstract class One[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    val relationship: ToOne[From, To, E, T]) extends RelationshipRep[E, T, Option[T]] {
  def fetch(implicit session: Session): Option[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => None
  }
}

case class OneFetched[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    override val relationship: ToOne[From, To, E, T],
    value: Option[T],
    override val ownerId: Option[E#IdType] = None) extends One[From, To, E, T](relationship) {
  val isFetched: Boolean = true

  def get: Option[T] = value
}

case class OneUnfetched[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    override val relationship: ToOne[From, To, E, T],
    override val ownerId: Option[E#IdType] = None) extends One[From, To, E, T](relationship) {
  val isFetched: Boolean = false

  def get: Option[T] = throw new NoSuchElementException("OneUnfetched.get")
}

sealed abstract class Many[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    val relationship: Relationship[From, To, E#IdType, E, T, Seq[T], Many[From, To, E, T]])
  extends RelationshipRep[E, T, Seq[T]]
{
  def fetch(implicit session: Session): Seq[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => List()
  }
}

case class ManyFetched[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    override val relationship: Relationship[From, To, E#IdType, E, T, Seq[T], Many[From, To, E, T]],
    values: Seq[T],
    override val ownerId: Option[E#IdType] = None) extends Many[From, To, E, T](relationship) {
  val isFetched: Boolean = true

  def get: Seq[T] = values
}

case class ManyUnfetched[From <: EntityTable[E], To <: Table[T], E <: Entity[_], T](
    override val relationship: Relationship[From, To, E#IdType, E, T, Seq[T], Many[From, To, E, T]],
    override val ownerId: Option[E#IdType] = None) extends Many[From, To, E, T](relationship) {
  val isFetched: Boolean = false

  def get: Seq[T] = throw new NoSuchElementException("ManyUnfetched.get")
}
