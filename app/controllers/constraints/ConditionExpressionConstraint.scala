package controllers.constraints

import models.ExpressionTerms
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.db.slick._
import utils.ConditionExpressionParser

object ConditionExpressionConstraint {
  def apply(implicit s: Session): Constraint[String] =
    Constraint[String]("constraints.parsableExpression")({ expression =>
      val variableMap: Map[String, Boolean] = ExpressionTerms.list.map(t => (t.label, false)).toMap
      val parser = new ConditionExpressionParser(variableMap)

      parser.parse(expression) match {
        case parser.Success(_,_) => Valid
        case parser.NoSuccess(msg,_) => Invalid("Error during parsing: %s.".format(msg))
        case _ => Invalid("Unknown error during parsing, please check for errors.")
      }
    })
}
