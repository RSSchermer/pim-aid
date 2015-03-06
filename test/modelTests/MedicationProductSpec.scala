package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._

class MedicationProductSpec extends PlaySpec with OneAppPerSuite {
  "The MedicationProduct companion object" must {
    "retrieve a MedicationProduct by name (not case-sensitive)" in {
      DB.withTransaction { implicit session =>
        val mpId = MedicationProduct.insert(MedicationProduct(None, "Some Product"))
        MedicationProduct.findByName("some product").get.id.get mustBe mpId

        session.rollback()
      }
    }

    "retrieve a MedicationProduct by user input with white-space pollution (not case-sensitive)" in {
      DB.withTransaction { implicit session =>
        val mpId = MedicationProduct.insert(MedicationProduct(None, "Some Product"))
        MedicationProduct.findByUserInput("   some    product ").get.id.get mustBe mpId

        session.rollback()
      }
    }

    "propose the best matching alternatives for user input" in {
      DB.withTransaction { implicit session =>
        MedicationProduct.insert(MedicationProduct(None, "Metoprolol"))
        MedicationProduct.insert(MedicationProduct(None, "Propanolol"))
        MedicationProduct.insert(MedicationProduct(None, "Metaclamide"))
        MedicationProduct.insert(MedicationProduct(None, "Enalapril"))

        MedicationProduct.findAlternatives("metaprolol", 0.3, 3) must contain
          inOrderOnly("Metoprolol", "Metaclamide", "Propanolol")

        session.rollback()
      }
    }
  }
}
