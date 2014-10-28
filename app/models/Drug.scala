package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._

case class Drug(id: Option[Long], userInput: String, userToken: String, source: Option[String],
                resolvedDrugTypeId: Option[Long])

class Drugs(tag: Tag) extends Table[Drug](tag, "DRUGS") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userInput = column[String]("userInput", O.NotNull)
  def userToken = column[String]("userToken", O.NotNull)
  def source = column[String]("source", O.Nullable)
  def resolvedDrugTypeId = column[Long]("resolved_drug_type_id", O.Nullable)

  def * = (id.?, userInput, userToken, source.?, resolvedDrugTypeId.?) <> (Drug.tupled, Drug.unapply)

  def resolvedDrugType = foreignKey("DRUGS_RESOLVED_TYPE_FK", resolvedDrugTypeId, TableQuery[DrugTypes])(_.id)
}

object Drugs {
  val all = TableQuery[Drugs]

  def insert(drug: Drug)(implicit s: Session) = {
    all returning all.map(_.id) += drug
  }
}