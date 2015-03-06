package models

import models.meta.Profile._
import models.meta.Schema._
import models.meta.Profile.driver.simple._

case class DrugID(value: Long) extends MappedTo[Long]

case class Drug(
    id: Option[DrugID],
    userInput: String,
    userToken: UserToken,
    resolvedMedicationProductId: Option[MedicationProductID])(implicit includes: Includes[Drug])
  extends Entity[Drug]
{
  type IdType = DrugID

  val userSession = one(Drug.userSession)
  val resolvedMedicationProduct = one(Drug.resolvedMedicationProduct)
}

object Drug extends EntityCompanion[Drugs, Drug] {
  val query = TableQuery[Drugs]

  val userSession = toOne[UserSessions, UserSession](
    TableQuery[UserSessions],
    _.userToken === _.token)

  val resolvedMedicationProduct = toOne[MedicationProducts, MedicationProduct](
    TableQuery[MedicationProducts],
    _.resolvedMedicationProductId === _.id)
}
