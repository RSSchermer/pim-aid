package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class GenericTypeSpec extends FunSpec with ModelSpec with Matchers {
  import driver.api._

  describe("The GenericType companion object") {
    it("retrieves a GenericType by name (not case-sensitive)") {
      rollback {
        for {
          referenceID <- GenericType.insert(GenericType(None, "Some Type"))
          retrievedID <- GenericType.hasName("some type").map(_.id).result.headOption
        } yield {
          retrievedID.get shouldBe referenceID
        }
      }
    }
  }
}
