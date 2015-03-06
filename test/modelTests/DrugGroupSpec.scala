package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._

class DrugGroupSpec extends PlaySpec with OneAppPerSuite {
  "The DrugGroup companion object" must {
    "retrieve a DrugGroup by name (not case-sensitive)" in {
      DB.withTransaction { implicit session =>
        val dgId = DrugGroup.insert(DrugGroup(None, "Some Group"))
        DrugGroup.findByName("some group").get.id.get mustBe dgId

        session.rollback()
      }
    }
  }
}
