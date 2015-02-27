package constraints

import models._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.db.slick._

object ConditionExpressionConstraint {
  def apply(implicit s: Session): Constraint[String] =
    Constraint[String]("constraints.parsableExpression")({ expression =>
      val variableMap: Map[String, Boolean] = ExpressionTerm.list.map(t => (t.label, false)).toMap
      val parser = new ConditionExpressionParser(variableMap)

      parser.parse(expression) match {
        case parser.Success(_,_) => Valid
        case parser.NoSuccess(msg,_) => Invalid("Error during parsing: %s.".format(msg))
        case _ => Invalid("Unknown error during parsing, please check for errors.")
      }
    })
}
