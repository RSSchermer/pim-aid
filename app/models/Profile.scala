package models

import entitytled.Entitytled
import play.api.db.slick.Config.{ driver => PlayDriver }

class Profile extends Entitytled {
  val driver = PlayDriver
}

object Profile extends Profile
