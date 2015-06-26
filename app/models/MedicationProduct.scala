package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._
import com.rockymadden.stringmetric.similarity._

import scala.concurrent.ExecutionContext

case class MedicationProductID(value: Long) extends MappedTo[Long]

case class MedicationProduct(
    id: Option[MedicationProductID],
    name: String)(implicit includes: Includes[MedicationProduct])
  extends Entity[MedicationProduct, MedicationProductID]
{
  val genericTypes = many(MedicationProduct.genericTypes)
}

object MedicationProduct extends EntityCompanion[MedicationProducts, MedicationProduct, MedicationProductID] {
  val genericTypes = toManyThrough[GenericTypes, GenericTypesMedicationProducts, GenericType]

  def hasName(name: String): Query[MedicationProducts, MedicationProduct, Seq] =
    all.filter(_.name.toLowerCase === name.toLowerCase)

  def findAlternatives(userInput: String, similarityThreshold: Double, maxNum: Int)
                      (implicit ec: ExecutionContext)
  : DBIO[Seq[MedicationProduct]] =
    all.result.map { products =>
      products.map(p => (JaroWinklerMetric.compare(userInput.toLowerCase, p.name.toLowerCase), p))
        .filter(_._1.get > similarityThreshold)
        .sortBy(_._1)(Ordering[Option[Double]].reverse)
        .take(maxNum)
        .map(_._2)
    }
}
