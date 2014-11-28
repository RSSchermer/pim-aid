package models

import play.api.db.slick.Config.driver.simple._

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
