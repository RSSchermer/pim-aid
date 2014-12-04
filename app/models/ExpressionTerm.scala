package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Session

trait ExpressionTerm {
  val label: String
}

case class GenericTypeTerm(override val label: String, genericTypeId: GenericTypeID) extends ExpressionTerm {
  def evaluate(genericTypes: List[GenericType]): Boolean = genericTypes.exists(x => x.id.getOrElse(-1) == genericTypeId)
}

case class DrugGroupTerm(override val label: String, drugGroupId: DrugGroupID) extends ExpressionTerm {
  def evaluate(drugGroups: List[DrugGroup]): Boolean = drugGroups.exists(x => x.id.getOrElse(-1) == drugGroupId)
}

case class StatementTerm(override val label: String, statement: String) extends ExpressionTerm {
  def evaluate(statementTerms: List[StatementTerm]): Boolean = statementTerms.exists(x => x.label == label)
}

class ExpressionTerms(tag: Tag) extends Table[ExpressionTerm](tag, "EXPRESSION_TERMS"){
  def label = column[String]("label", O.PrimaryKey)
  def genericTypeId = column[GenericTypeID]("drug_type_id", O.Nullable)
  def drugGroupId = column[DrugGroupID]("drug_group_id", O.Nullable)
  def statement = column[String]("statement", O.Nullable)

  def * = (label, genericTypeId.?, drugGroupId.?, statement.?) <> (defaultApply, defaultUnapply)
  def defaultApply(tuple: (String, Option[GenericTypeID], Option[DrugGroupID], Option[String]))
  : ExpressionTerm = {
    tuple match {
      case (label, Some(drugTypeId), _, _) => GenericTypeTerm(label, drugTypeId)
      case (label, _, Some(drugGroupId), _) => DrugGroupTerm(label, drugGroupId)
      case (label, _, _, Some(statement)) => StatementTerm(label, statement)
    }
  }
  def defaultUnapply(term: ExpressionTerm)
  : Option[(String, Option[GenericTypeID], Option[DrugGroupID], Option[String])] = {
    term match {
      case GenericTypeTerm(label, drugTypeId) => Some(label.value, Some(drugTypeId), None, None)
      case DrugGroupTerm(label, drugGroupId) => Some(label.value, None, Some(drugGroupId), None)
      case StatementTerm(label, statement) => Some(label.value, None, None, Some(statement))
    }
  }

  def drugGroup = foreignKey("EXPRESSION_TERMS_DRUG_GROUP_FK", drugGroupId, TableQuery[DrugGroups])(_.id)
  def drugType = foreignKey("EXPRESSION_TERMS_DRUG_TYPE_FK", genericTypeId, TableQuery[GenericTypes])(_.id)
}

object ExpressionTerms {
  val all = TableQuery[ExpressionTerms]

  def list(implicit s: Session) = all.list

  def insert(term: ExpressionTerm)(implicit s: Session) = all.insert(term)
}

object DrugGroupTerms {
  val all = ExpressionTerms.all.filter(_.drugGroupId.isNotNull)

  def listWithDrugGroup(implicit s: Session): List[(DrugGroupTerm, DrugGroup)] = {
    (for {
      term <- all
      group <- TableQuery[DrugGroups] if term.drugGroupId === group.id
    } yield (term, group)).list.asInstanceOf[List[(DrugGroupTerm, DrugGroup)]]
  }

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[DrugGroupTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[DrugGroupTerm])
    case _ => None
  }

  def insert(term: DrugGroupTerm)(implicit s: Session) = ExpressionTerms.all.insert(term)

  def update(label: String, term: DrugGroupTerm)(implicit s: Session) =
    one(label).map(x => x.drugGroupId).update(term.drugGroupId)

  def delete(label: String)(implicit s: Session) = one(label).delete
}

object GenericTypeTerms {
  val all = ExpressionTerms.all.filter(_.genericTypeId.isNotNull)

  def listWithGenericType(implicit s: Session): List[(GenericTypeTerm, GenericType)] = {
    (for {
      term <- all
      dType <- TableQuery[GenericTypes] if term.genericTypeId === dType.id
    } yield (term, dType)).list.asInstanceOf[List[(GenericTypeTerm, GenericType)]]
  }

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[GenericTypeTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[GenericTypeTerm])
    case _ => None
  }

  def insert(term: GenericTypeTerm)(implicit s: Session) = ExpressionTerms.all.insert(term)

  def update(label: String, term: GenericTypeTerm)(implicit s: Session) =
    one(label).map(x => x.genericTypeId).update(term.genericTypeId)

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

  def insert(term: StatementTerm)(implicit s: Session) = ExpressionTerms.all.insert(term)

  def update(label: String, term: StatementTerm)(implicit s: Session) =
    one(label).map(x => x.statement).update(term.statement)

  def delete(label: String)(implicit s: Session) = one(label).delete
}
