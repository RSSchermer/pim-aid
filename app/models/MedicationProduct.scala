package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session
import com.rockymadden.stringmetric.similarity._

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
      case (None, _) => None
    }
  }
  def optionUnapply(oc: Option[MedicationProduct]): Option[(Option[MedicationProductID], Option[String])] = None

  def nameIndex = index("MEDICATION_PRODUCTS_NAME_INDEX", name, unique = true)
}

object MedicationProducts {
  val all = TableQuery[MedicationProducts]

  def list(implicit s: Session) = all.list

  def one(id: MedicationProductID) = all.filter(_.id === id)

  def find(id: MedicationProductID)(implicit s: Session): Option[MedicationProduct] = one(id).firstOption

  def findByName(name: String)(implicit s: Session): Option[MedicationProduct] =
    all.filter(_.name.toLowerCase === name.toLowerCase).firstOption

  def genericTypeListFor(id: MedicationProductID)(implicit s: Session): List[GenericType] = {
    (for {
      (_, genericType) <-
        one(id) innerJoin
        TableQuery[GenericTypesMedicationProducts] on (_.id === _.medicationProductId) innerJoin
        TableQuery[GenericTypes] on (_._2.genericTypeId === _.id)
    } yield genericType).list
  }

  def insert(medicationProduct: MedicationProduct)(implicit s: Session): MedicationProductID =
    all returning all.map(_.id) += medicationProduct

  def update(id: MedicationProductID, medicationProduct: MedicationProduct)(implicit s: Session) =
    one(id).map(x => x.name).update(medicationProduct.name)

  def delete(id: MedicationProductID)(implicit s: Session) = {
    TableQuery[GenericTypesMedicationProducts].filter(_.medicationProductId === id).delete
    one(id).delete
  }

  def alternativesForUserInput(userInput: String, similarityThreshold: Double, maxNum: Int)(implicit s: Session)
  : List[MedicationProduct] =
    list.map(x => (JaroWinklerMetric.compare(userInput.toLowerCase, x.name.toLowerCase), x))
      .filter(_._1.get > similarityThreshold)
      .sortBy(_._1)(Ordering[Option[Double]].reverse)
      .take(maxNum)
      .map(_._2)
}
