package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

trait ExpressionTerm {
  val label: String
}

case class DrugTypeTerm(override val label: String, drugTypeId: Long) extends ExpressionTerm
case class DrugGroupTerm(override val label: String, drugGroupId: Long) extends ExpressionTerm
case class StatementTerm(override val label: String, statement: String) extends ExpressionTerm

class ExpressionTerms(tag: Tag) extends Table[ExpressionTerm](tag, "EXPRESSION_TERMS"){
  def label = column[String]("label", O.PrimaryKey)
  def drugTypeId = column[Long]("drug_type_id", O.Nullable)
  def drugGroupId = column[Long]("drug_group_id", O.Nullable)
  def statement = column[String]("statement", O.Nullable)

  def * = (label, drugTypeId.?, drugGroupId.?, statement.?) <> (defaultApply, defaultUnapply)
  def defaultApply(tuple: (String, Option[Long], Option[Long], Option[String])): ExpressionTerm = {
    tuple match {
      case (label, Some(drugTypeId), _, _) => DrugTypeTerm(label, drugTypeId)
      case (label, _, Some(drugGroupId), _) => DrugGroupTerm(label, drugGroupId)
      case (label, _, _, Some(statement)) => StatementTerm(label, statement)
    }
  }
  def defaultUnapply(term: ExpressionTerm): Option[(String, Option[Long], Option[Long], Option[String])] = {
    term match {
      case DrugTypeTerm(label, drugTypeId) => Some(label, Some(drugTypeId), None, None)
      case DrugGroupTerm(label, drugGroupId) => Some(label, None, Some(drugGroupId), None)
      case StatementTerm(label, statement) => Some(label, None, None, Some(statement))
    }
  }

  def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", drugTypeId, TableQuery[DrugTypes])(_.id)
}

object ExpressionTerms {
  val all = TableQuery[ExpressionTerms]

  def list(implicit s: Session) = all.list

  def insert(term: ExpressionTerm)(implicit s: Session) = all.insert(term)
}

object DrugGroupTerms {
  val all = ExpressionTerms.all.filter(_.drugGroupId.isNotNull)
  val drugGroups = TableQuery[DrugGroups]

  def listWithDrugGroup(implicit s: Session): List[(DrugGroupTerm, DrugGroup)] = {
    (for {
      term <- all
      group <- drugGroups if term.drugGroupId === group.id
    } yield (term, group)).list.asInstanceOf[List[(DrugGroupTerm, DrugGroup)]]
  }

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[DrugGroupTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[DrugGroupTerm])
    case _ => None
  }

  def insert(term: DrugGroupTerm)(implicit s: Session) = ExpressionTerms.insert(term)

  def update(label: String, term: DrugGroupTerm)(implicit s: Session) =
    one(label).map(x => x.drugGroupId).update(term.drugGroupId)

  def delete(label: String)(implicit s: Session) = one(label).delete
}

object DrugTypeTerms {
  val all = ExpressionTerms.all.filter(_.drugTypeId.isNotNull)
  val drugTypes = TableQuery[DrugTypes]

  def listWithDrugType(implicit s: Session): List[(DrugTypeTerm, DrugType)] = {
    (for {
      term <- all
      dType <- drugTypes if term.drugTypeId === dType.id
    } yield (term, dType)).list.asInstanceOf[List[(DrugTypeTerm, DrugType)]]
  }

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[DrugTypeTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[DrugTypeTerm])
    case _ => None
  }

  def insert(term: DrugTypeTerm)(implicit s: Session) = ExpressionTerms.insert(term)

  def update(label: String, term: DrugTypeTerm)(implicit s: Session) =
    one(label).map(x => x.drugTypeId).update(term.drugTypeId)

  def delete(label: String)(implicit s: Session) = one(label).delete
}

object StatementTerms {
  val all = ExpressionTerms.all.filter(_.statement.isNotNull)

  def list(implicit s: Session): List[StatementTerm] = all.list.asInstanceOf[List[StatementTerm]]

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[StatementTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[StatementTerm])
    case _ => None
  }

  def insert(term: StatementTerm)(implicit s: Session) = ExpressionTerms.insert(term)

  def update(label: String, term: StatementTerm)(implicit s: Session) =
    one(label).map(x => x.statement).update(term.statement)

  def delete(label: String)(implicit s: Session) = one(label).delete
}
