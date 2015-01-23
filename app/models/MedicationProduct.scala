package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import play.api.db.slick.Session
import schema._
import com.rockymadden.stringmetric.similarity._

case class MedicationProductID(value: Long) extends MappedTo[Long]

case class MedicationProduct(
    id: Option[MedicationProductID],
    name: String,
    genericTypes: Many[MedicationProducts, GenericTypes, MedicationProduct, GenericType] =
      ManyFetched(MedicationProduct.genericTypes))
  extends Entity { type IdType = MedicationProductID }

object MedicationProduct extends EntityCompanion[MedicationProducts, MedicationProduct] {
  val query = TableQuery[MedicationProducts]

  val genericTypes = toManyThrough[GenericType, (GenericTypeID, MedicationProductID), GenericTypes, GenericTypesMedicationProducts](
    TableQuery[GenericTypesMedicationProducts] leftJoin TableQuery[GenericTypes] on(_.genericTypeId === _.id),
    _.id === _._1.medicationProductId,
    lenser(_.genericTypes)
  )

  def findByName(name: String)(implicit s: Session): Option[MedicationProduct] =
    TableQuery[MedicationProducts].filter(_.name.toLowerCase === name.toLowerCase).firstOption

  def findByUserInput(userInput: String)(implicit s: Session): Option[MedicationProduct] = {
    val normalizedInput = userInput.trim().replaceAll("""\s+""", " ")

    query.filter(_.name.toLowerCase === normalizedInput).firstOption
  }

  def alternativesForUserInput(userInput: String, similarityThreshold: Double, maxNum: Int)(implicit s: Session): Seq[MedicationProduct] =
    list.map(x => (JaroWinklerMetric.compare(userInput.toLowerCase, x.name.toLowerCase), x))
      .filter(_._1.get > similarityThreshold)
      .sortBy(_._1)(Ordering[Option[Double]].reverse)
      .take(maxNum)
      .map(_._2)
}
