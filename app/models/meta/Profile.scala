package models.meta

import entitytled.Entitytled

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider

import slick.driver.JdbcProfile

trait Profile extends Entitytled {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val driver = dbConfig.driver

  implicit val db = dbConfig.db
}

object Profile extends Profile
