package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class MedicationProductSpec extends FunSpec with DBSpec with Matchers {
  describe("The MedicationProduct companion object") {
    it("retrieves a MedicationProduct by name (not case-sensitive)") {
      rollback {
        for {
          referenceID <- MedicationProduct.insert(MedicationProduct(None, "Some Product"))
          retrievedID <- MedicationProduct.hasName("some product").map(_.id).result.headOption
        } yield {
          retrievedID.get shouldBe referenceID
        }
      }
    }

    it("proposes the best matching alternatives for user input") {
      rollback {
        for {
          _ <- MedicationProduct.insert(MedicationProduct(None, "Metoprolol"))
          _ <- MedicationProduct.insert(MedicationProduct(None, "Propanolol"))
          _ <- MedicationProduct.insert(MedicationProduct(None, "Metaclamide"))
          _ <- MedicationProduct.insert(MedicationProduct(None, "Enalapril"))

          alternatives <- MedicationProduct.findAlternatives("metaprolol", 0.3, 3)
        } yield {
          alternatives.map(_.name) should contain inOrderOnly("Metoprolol", "Metaclamide", "Propanolol")
        }
      }
    }
  }
}
