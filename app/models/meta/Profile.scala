package models.meta

import entitytled.Entitytled
import play.api.db.slick.Config.{driver => PlayDriver}

trait Profile extends Entitytled {
  val driver = PlayDriver
}

object Profile extends Profile
