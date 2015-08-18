package constraints

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api.data.validation.{Constraint, Invalid, Valid}

import model.Model._
import model.Model.driver.api._

object ConditionExpressionConstraint {
  def apply(implicit ec: ExecutionContext): Constraint[String] =
    Constraint[String]("constraints.parsableExpression")({ expression =>
      val variableMap: Map[String, Boolean] =
        Await.result(db.run(ExpressionTerm.all.map(t => (t.label, false)).result), 10 seconds).toMap
      val parser = new ConditionExpressionParser(variableMap)

      parser.parse(ConditionExpression(expression)) match {
        case parser.Success(_,_) => Valid
        case parser.NoSuccess(msg,_) => Invalid("Error during parsing: %s.".format(msg))
        case _ => Invalid("Unknown error during parsing, please check for errors.")
      }
    })
}
