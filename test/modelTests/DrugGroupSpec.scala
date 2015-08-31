package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class DrugGroupSpec extends FunSpec with ModelSpec with Matchers {
  import driver.api._

  describe("The DrugGroup companion object") {
    it("retrieves a DrugGroup by name (not case-sensitive)") {
      rollback {
        for {
          referenceID <- DrugGroup.insert(DrugGroup(None, "Some Group"))
          retrievedID <- DrugGroup.hasName("some group").map(_.id).result.headOption
        } yield {
          retrievedID.get shouldBe referenceID
        }
      }
    }
  }
}
