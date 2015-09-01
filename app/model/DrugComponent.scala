package model

import entitytled.Entitytled

trait DrugComponent {
  self: Entitytled
    with MedicationProductComponent
    with UserSessionComponent
  =>

  import driver.api._

  case class DrugID(value: Long) extends MappedTo[Long]

  case class Drug(
      id: Option[DrugID],
      userInput: String,
      userToken: UserToken,
      resolvedMedicationProductId: Option[MedicationProductID])(implicit includes: Includes[Drug])
    extends Entity[Drug, DrugID]
  {
    val userSession = one(Drug.userSession)
    val resolvedMedicationProduct = one(Drug.resolvedMedicationProduct)
  }

  object Drug extends EntityCompanion[Drugs, Drug, DrugID] {
    val userSession = toOne[self.UserSessions, self.UserSession]
    val resolvedMedicationProduct = toOne[self.MedicationProducts, self.MedicationProduct]
  }

  class Drugs(tag: Tag) extends EntityTable[Drug, DrugID](tag, "DRUGS") {
    def id = column[DrugID]("id", O.PrimaryKey, O.AutoInc)
    def userInput = column[String]("userInput")
    def userToken = column[UserToken]("userToken")
    def resolvedMedicationProductId = column[Option[MedicationProductID]]("resolved_medication_product_id")

    def * = (id.?, userInput, userToken, resolvedMedicationProductId) <>((Drug.apply _).tupled, Drug.unapply)

    def resolvedMedicationProduct =
      foreignKey("DRUGS_RESOLVED_MEDICATION_PRODUCT_FK", resolvedMedicationProductId, TableQuery[MedicationProducts])(_.id.?)
    def userSession = foreignKey("DRUGS_USER_SESSION_FK", userToken, TableQuery[UserSessions])(_.id)
  }
}