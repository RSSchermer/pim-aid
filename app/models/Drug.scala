package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.api._

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
  val userSession = toOne[UserSessions, UserSession]
  val resolvedMedicationProduct = toOne[MedicationProducts, MedicationProduct]
}
