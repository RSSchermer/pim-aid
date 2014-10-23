package models

import play.api.db.slick.Config.driver.simple._

abstract class ExpressionTerm(label: String, drugTypeId: Option[Long], drugGroupId: Option[Long],
                              question: Option[String])

case class DrugTypeTerm(label: String, drugTypeId: Long) extends ExpressionTerm(label, Some(drugTypeId), None, None)
case class DrugGroupTerm(label: String, drugGroupId: Long) extends ExpressionTerm(label, None, Some(drugGroupId), None)
case class QuestionTerm(label: String, question: String) extends ExpressionTerm(label, None, None, Some(question))

class ExpressionTerms(tag: Tag) extends Table[ExpressionTerm](tag, "EXPRESSION_TERMS"){
  def label = column[String]("label", O.PrimaryKey)
  def drugTypeId = column[Long]("drug_type_id", O.Nullable)
  def drugGroupId = column[Long]("drug_group_id", O.Nullable)
  def question = column[String]("question", O.Nullable)

  def * = (label, drugTypeId.?, drugGroupId.?, question.?) <> (defaultApply, defaultUnapply)
  def defaultApply(tuple: (String, Option[Long], Option[Long], Option[String])): ExpressionTerm = {
    tuple match {
      case (label, Some(drugTypeId), _, _) => DrugTypeTerm(label, drugTypeId)
      case (label, _, Some(drugGroupId), _) => DrugGroupTerm(label, drugGroupId)
      case (label, _, _, Some(question)) => QuestionTerm(label, question)
    }
  }
  def defaultUnapply(term: ExpressionTerm): Option[(String, Option[Long], Option[Long], Option[String])] = {
    term match {
      case DrugTypeTerm(label, drugTypeId) => Some(label, Some(drugTypeId), None, None)
      case DrugGroupTerm(label, drugGroupId) => Some(label, None, Some(drugGroupId), None)
      case QuestionTerm(label, question) => Some(label, None, None, Some(question))
    }
  }

  def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", drugTypeId, TableQuery[DrugTypes])(_.id)
}
