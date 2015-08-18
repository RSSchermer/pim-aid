package model

import entitytled.Entitytled

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider

import slick.driver.JdbcProfile

trait Model extends Entitytled
  with ConditionExpressionComponent
  with DrugComponent
  with DrugGroupComponent
  with ExpressionTermComponent
  with GenericTypeComponent
  with MedicationProductComponent
  with RuleComponent
  with SuggestionTemplateComponent
  with UserSessionComponent
{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val driver = dbConfig.driver

  implicit val db = dbConfig.db
}

object Model extends Model
