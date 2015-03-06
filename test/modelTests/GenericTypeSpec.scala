package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._

class GenericTypeSpec extends PlaySpec with OneAppPerSuite {
  "The GenericType companion object" must {
    "retrieve a GenericType by name (not case-sensitive)" in {
      DB.withTransaction { implicit session =>
        val gtId = GenericType.insert(GenericType(None, "Some Type"))
        GenericType.findByName("some type").get.id.get mustBe gtId

        session.rollback()
      }
    }
  }
}
