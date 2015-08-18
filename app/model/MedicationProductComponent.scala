package model

import entitytled.Entitytled
import com.rockymadden.stringmetric.similarity._

import scala.concurrent.ExecutionContext

trait MedicationProductComponent {
  self: Entitytled
    with GenericTypeComponent
  =>

  import driver.api._

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

  class MedicationProducts(tag: Tag)
    extends EntityTable[MedicationProduct, MedicationProductID](tag, "MEDICATION_PRODUCTS")
  {
    def id = column[MedicationProductID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")

    def * = (id.?, name) <>((MedicationProduct.apply _).tupled, MedicationProduct.unapply)

    def nameIndex = index("MEDICATION_PRODUCTS_NAME_INDEX", name, unique = true)
  }

  class GenericTypesMedicationProducts(tag: Tag)
    extends Table[(GenericTypeID, MedicationProductID)](tag, "GENERIC_TYPES_MEDICATION_PRODUCT")
  {
    def genericTypeId = column[GenericTypeID]("generic_type_id")
    def medicationProductId = column[MedicationProductID]("medication_product_id")

    def * = (genericTypeId, medicationProductId)

    def pk = primaryKey("GENERIC_TYPES_MEDICATION_PRODUCT_PK", (medicationProductId, genericTypeId))

    def genericType = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_GENERIC_TYPE_FK", genericTypeId,
      TableQuery[GenericTypes])(_.id, onDelete = ForeignKeyAction.Cascade)
    def medicationProduct = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK", medicationProductId,
      TableQuery[MedicationProducts])(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}