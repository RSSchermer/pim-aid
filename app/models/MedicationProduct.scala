package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class MedicationProductID(value: Long) extends MappedTo[Long]

case class MedicationProduct(id: Option[MedicationProductID], name: String)

class MedicationProducts(tag: Tag) extends Table[MedicationProduct](tag, "MEDICATION_PRODUCTS"){
  def id = column[MedicationProductID]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  def * = (id.?, name) <> (MedicationProduct.tupled, MedicationProduct.unapply)

  def ? = (id.?, name.?) <> (optionApply, optionUnapply)
  def optionApply(t: (Option[MedicationProductID], Option[String])): Option[MedicationProduct] = {
    t match {
      case (Some(id), Some(name)) => Some(MedicationProduct(Some(id), name))
      case (None, _, _) => None
    }
  }
  def optionUnapply(oc: Option[MedicationProduct]): Option[(Option[MedicationProductID], Option[String])] = None
}

object MedicationProducts {
  val all = TableQuery[MedicationProducts]

  def list(implicit s: Session) = all.list

  def one(id: MedicationProductID) = all.filter(_.id === id)

  def find(id: MedicationProductID)(implicit s: Session): Option[MedicationProduct] = one(id).firstOption

  def genericTypeIdListFor(id: MedicationProductID): List[GenericTypeID] =
    TableQuery[GenericTypesMedicationProducts].filter(_.medicationProductId === id).map(_.genericTypeId).list

  def insert(drugType: MedicationProduct, genericTypeIds: List[GenericTypeID])(implicit s: Session) = {
    val typeId = all returning all.map(_.id) += drugType

    genericTypeIds.foreach {
      id => TableQuery[GenericTypesMedicationProducts].insert(GenericTypeMedicationProduct(id, typeId))
    }
  }

  def update(id: MedicationProductID, medicationProduct: MedicationProduct, genericTypeIds: List[GenericTypeID])
            (implicit s: Session) =
  {
    one(id).map(x => x.name).update(medicationProduct.name)

    TableQuery[GenericTypesMedicationProducts].filter(_.medicationProductId === id).delete
    genericTypeIds.map(x => GenericTypeMedicationProduct(x, id))
      .foreach { x => TableQuery[GenericTypesMedicationProducts].insert(x) }
  }

  def delete(id: MedicationProductID)(implicit s: Session) = {
    TableQuery[GenericTypesMedicationProducts].filter(_.medicationProductId === id).delete
    one(id).delete
  }
}
