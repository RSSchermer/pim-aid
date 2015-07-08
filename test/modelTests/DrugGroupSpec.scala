package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

import models._
import models.meta.Profile.driver.api._

class DrugGroupSpec extends FunSpec with DBSpec with Matchers {
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
