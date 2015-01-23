package models

import play.api.db.slick.Config.driver.simple._
import ORM.model._
import schema._

case class DrugID(value: Long) extends MappedTo[Long]

case class Drug(
    id: Option[DrugID],
    userInput: String,
    userToken: UserToken,
    resolvedMedicationProductId: Option[MedicationProductID],
    userSession: One[Drugs, UserSessions, Drug, UserSession] = OneFetched(Drug.userSession),
    resolvedMedicationProduct: One[Drugs, MedicationProducts, Drug, MedicationProduct] =
      OneFetched(Drug.resolvedMedicationProduct))
  extends Entity { type IdType = DrugID }

object Drug extends EntityCompanion[Drugs, Drug] {
  val query = TableQuery[Drugs]

  val userSession = toOne[UserSession, UserSessions](
    TableQuery[UserSessions],
    _.userToken === _.token,
    lenser(_.userSession))

  val resolvedMedicationProduct = toOne[MedicationProduct, MedicationProducts](
    TableQuery[MedicationProducts],
    _.resolvedMedicationProductId === _.id,
    lenser(_.resolvedMedicationProduct)
  )
}
