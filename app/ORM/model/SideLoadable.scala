package ORM.model

import play.api.db.slick.Config.driver.simple._

trait SideLoadable[T <: Table[M], M] {
  def sideLoadOn(instances: List[M], query: Query[T, M, Seq])(implicit session: Session): List[M]

  def sideLoadOn(instance: M, query: Query[T, M, Seq])(implicit session: Session): M
}
