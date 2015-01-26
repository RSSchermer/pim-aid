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

sealed abstract class One[E <: Entity, T]
  extends RelationshipRep[E, Option[T]]
{
  val relationship: ToOneRelationship[E, T]

  def fetch(implicit session: Session): Option[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => None
  }
}

case class OneFetched[E <: Entity, T](
    override val relationship: ToOneRelationship[E, T],
    override val ownerId: Option[E#IdType] = None,
    value: Option[T] = None)
  extends One[E, T]
{
  val isFetched: Boolean = true

  def get: Option[T] = value
}

case class OneUnfetched[E <: Entity, T](
    override val relationship: ToOneRelationship[E, T],
    override val ownerId: Option[E#IdType])
  extends One[E, T]
{
  val isFetched: Boolean = false

  def get: Option[T] = throw new NoSuchElementException("OneUnfetched.get")
}

sealed abstract class Many[E <: Entity, T]
  extends RelationshipRep[E, Seq[T]]
{
  val relationship: ToManyRelationship[E, T]

  def fetch(implicit session: Session): Seq[T] = ownerId match {
    case Some(id) => relationship.fetchFor(id)
    case _ => List()
  }
}

case class ManyFetched[E <: Entity, T](
    override val relationship: ToManyRelationship[E, T],
    override val ownerId: Option[E#IdType] = None,
    values: Seq[T] = Seq())
  extends Many[E, T]
{
  val isFetched: Boolean = true

  def get: Seq[T] = values
}

case class ManyUnfetched[E <: Entity, T](
    override val relationship: ToManyRelationship[E, T],
    override val ownerId: Option[E#IdType])
  extends Many[E, T]
{
  val isFetched: Boolean = false

  def get: Seq[T] = throw new NoSuchElementException("ManyUnfetched.get")
}
