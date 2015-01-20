package ORM

import play.api.db.slick.Config.driver.simple._
import ORM.model._

abstract class EntityRepository[E <: Entity[_], T <: EntityTable[E]](implicit ev: BaseColumnType[E#IdType])
  extends EntityCollectionRetriever[E, T]
{
  def insert(instance: E)(implicit s: Session): E#IdType = {
    s.withTransaction {
      beforeInsert(instance)
      beforeSave(instance)

      val key = query returning query.map(_.id) += instance

      afterInsert(key, instance)
      afterSave(key, instance)

      key
    }
  }

  def update(instance: E)(implicit s: Session): Unit = instance.id match {
    case Some(id) =>
      s.withTransaction {
        beforeUpdate(instance)
        beforeSave(instance)
        query.filter(_.id === id.asInstanceOf[E#IdType]).update(instance)
        afterUpdate(id, instance)
        afterSave(id, instance)
      }
  }

  def delete(key: E#IdType)(implicit s: Session): Unit = {
    s.withTransaction {
      beforeDelete(key)
      query.filter(_.id === key).delete
      afterDelete(key)
    }
  }
  
  protected def beforeInsert(instance: E)(implicit s: Session): Unit = ()
  
  protected def beforeUpdate(instance: E)(implicit s: Session): Unit = ()
  
  protected def beforeSave(instance: E)(implicit s: Session): Unit = ()
  
  protected def beforeDelete(key: E#IdType)(implicit s: Session): Unit = ()
  
  protected def afterInsert(key: E#IdType, instance: E)(implicit s: Session): Unit = ()

  protected def afterUpdate(key: E#IdType, instance: E)(implicit s: Session): Unit = ()

  protected def afterSave(key: E#IdType, instance: E)(implicit s: Session): Unit = ()

  protected def afterDelete(key: E#IdType)(implicit s: Session): Unit = ()
}
