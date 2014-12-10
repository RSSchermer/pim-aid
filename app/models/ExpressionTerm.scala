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

case class StatementTerm(override val label: String, statementTemplate: String, displayCondition: Option[String])
  extends ExpressionTerm
{
  def evaluate(statementTerms: List[StatementTerm]): Boolean = statementTerms.exists(x => x.label == label)
}

case class AgeTerm(override val label: String, comparisonOperator: String, age: Int)
  extends ExpressionTerm
{
  def evaluate(userAge: Int): Boolean = comparisonOperator match {
    case "==" => userAge == age
    case ">" => userAge > age
    case ">=" => userAge >= age
    case "<" => userAge < age
    case "<=" => userAge <= age
    case _ => false
  }
}

class ExpressionTerms(tag: Tag) extends Table[ExpressionTerm](tag, "EXPRESSION_TERMS"){
  def label = column[String]("label", O.PrimaryKey)
  def genericTypeId = column[GenericTypeID]("drug_type_id", O.Nullable)
  def drugGroupId = column[DrugGroupID]("drug_group_id", O.Nullable)
  def statementTemplate = column[String]("statement_template", O.Nullable)
  def displayCondition = column[String]("display_condition", O.Nullable)
  def comparisonOperator = column[String]("comparison_operator", O.Nullable)
  def age = column[Int]("age", O.Nullable)

  def * = (label, genericTypeId.?, drugGroupId.?, statementTemplate.?, displayCondition.?, comparisonOperator.?, age.?) <>
    (defaultApply, defaultUnapply)

  def defaultApply(tuple: (String, Option[GenericTypeID], Option[DrugGroupID], Option[String], Option[String], Option[String], Option[Int])) : ExpressionTerm = {
    tuple match {
      case (label, Some(drugTypeId), _, _, _, _, _) => GenericTypeTerm(label, drugTypeId)
      case (label, _, Some(drugGroupId), _, _, _, _) => DrugGroupTerm(label, drugGroupId)
      case (label, _, _, Some(statement), displayCondition, _, _) => StatementTerm(label, statement, displayCondition)
      case (label, _, _, _, _, Some(comparisonOperator), Some(age)) => AgeTerm(label, comparisonOperator, age)
    }
  }

  def defaultUnapply(term: ExpressionTerm): Option[(String, Option[GenericTypeID], Option[DrugGroupID], Option[String], Option[String], Option[String], Option[Int])] = {
    term match {
      case GenericTypeTerm(label, drugTypeId) => Some(label.value, Some(drugTypeId), None, None, None, None, None)
      case DrugGroupTerm(label, drugGroupId) => Some(label.value, None, Some(drugGroupId), None, None, None, None)
      case StatementTerm(label, statement, displayCondition) =>
        Some(label.value, None, None, Some(statement), displayCondition, None, None)
      case AgeTerm(label, comparisonOperator, age) => {
        Some(label.value, None, None, None, None, Some(comparisonOperator), Some(age))
      }
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
  val all = ExpressionTerms.all.filter(_.statementTemplate.isNotNull)

  def list(implicit s: Session): List[StatementTerm] = all.list.asInstanceOf[List[StatementTerm]]

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[StatementTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[StatementTerm])
    case _ => None
  }

  def insert(term: StatementTerm)(implicit s: Session) = ExpressionTerms.all.insert(term)

  def update(label: String, term: StatementTerm)(implicit s: Session) =
    one(label).map(x => (x.statementTemplate, x.displayCondition.?)).update((term.statementTemplate, term.displayCondition))

  def delete(label: String)(implicit s: Session) = one(label).delete
}

object AgeTerms {
  val all = ExpressionTerms.all.filter(x => x.comparisonOperator.isNotNull && x.age.isNotNull)

  def list(implicit s: Session): List[AgeTerm] = all.list.asInstanceOf[List[AgeTerm]]

  def one(label: String) = all.filter(_.label === label)

  def find(label: String)(implicit s: Session): Option[AgeTerm] = one(label).firstOption match {
    case Some(term) => Some(term.asInstanceOf[AgeTerm])
    case _ => None
  }

  def insert(term: AgeTerm)(implicit s: Session) = ExpressionTerms.all.insert(term)

  def update(label: String, term: AgeTerm)(implicit s: Session) =
    one(label).map(x => (x.comparisonOperator, x.age)).update((term.comparisonOperator, term.age))

  def delete(label: String)(implicit s: Session) = one(label).delete
}
