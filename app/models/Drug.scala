package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

case class DrugID(value: Long) extends MappedTo[Long]

case class Drug(id: Option[DrugID], userInput: String, userToken: UserToken,
                resolvedMedicationProductId: Option[MedicationProductID])

class Drugs(tag: Tag) extends Table[Drug](tag, "DRUGS") {
  def id = column[DrugID]("id", O.PrimaryKey, O.AutoInc)
  def userInput = column[String]("userInput", O.NotNull)
  def userToken = column[UserToken]("userToken", O.NotNull)
  def resolvedMedicationProductId = column[MedicationProductID]("resolved_medication_product_id", O.Nullable)

  def * = (id.?, userInput, userToken, resolvedMedicationProductId.?) <> (Drug.tupled, Drug.unapply)

  def resolvedMedicationProduct = foreignKey("DRUGS_RESOLVED_MEDICATION_PRODUCT_FK", resolvedMedicationProductId,
    TableQuery[MedicationProducts])(_.id)
}

object Drugs {
  val all = TableQuery[Drugs]

  def insert(drug: Drug)(implicit s: Session): DrugID = {
    all returning all.map(_.id) += drug
  }
}