package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class GenericTypeMedicationProduct(genericTypeId: GenericTypeID, medicationProductId: MedicationProductID)

class GenericTypesMedicationProducts(tag: Tag)
  extends Table[GenericTypeMedicationProduct](tag, "GENERIC_TYPES_MEDICATION_PRODUCT")
{
  def genericTypeId = column[GenericTypeID]("generic_type_id")
  def medicationProductId = column[MedicationProductID]("medication_product_id")

  def * = (genericTypeId, medicationProductId) <> (GenericTypeMedicationProduct.tupled, GenericTypeMedicationProduct.unapply)

  def pk = primaryKey("GENERIC_TYPES_MEDICATION_PRODUCT_PK", (medicationProductId, genericTypeId))
  def genericType = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_GENERIC_TYPE_FK", genericTypeId,
    TableQuery[GenericTypes])(_.id)
  def medicationProduct = foreignKey("GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK", medicationProductId, 
    TableQuery[MedicationProducts])(_.id)
}

object GenericTypesMedicationProducts {
  val all = TableQuery[GenericTypesMedicationProducts]

  def one(genericTypeId: GenericTypeID, medicationProductId: MedicationProductID) =
    all.filter(x => x.genericTypeId === genericTypeId && x.medicationProductId === medicationProductId)

  def exists(genericTypeId: GenericTypeID, medicationProductId: MedicationProductID)(implicit s: Session): Boolean =
    one(genericTypeId, medicationProductId).exists.run

  def insert(genericTypeMedicationProduct: GenericTypeMedicationProduct)(implicit s: Session) =
    all.insert(genericTypeMedicationProduct)

  def delete(genericTypeId: GenericTypeID, medicationProductId: MedicationProductID)(implicit s: Session) =
    one(genericTypeId, medicationProductId).delete
}