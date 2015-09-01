package model

import entitytled.Entitytled

import scala.util.parsing.combinator.JavaTokenParsers
import java.util.regex.Pattern

trait ConditionExpressionComponent {
  self: Entitytled =>

  import driver.api.MappedTo

  case class ConditionExpression(value: String) extends MappedTo[String] {
    def expressionTermLabels: Seq[String] =
      """\[([A-Za-z0-9_\-]+)\]""".r.findAllMatchIn(value).map(_.group(1)).toList

    def replaceLabel(oldLabel: String, newLabel: String): ConditionExpression = {
      val newValue = value.replaceAll(Pattern.quote("[" + oldLabel + "]"), "[" + newLabel + "]")
      ConditionExpression(newValue)
    }
  }

  case class ConditionExpressionParser(variableMap: Map[String, Boolean] = Map()) extends JavaTokenParsers {
    private lazy val b_expression: Parser[Boolean] = b_term ~ rep(("or" | "OR" | "||") ~ b_term) ^^ { case f1 ~ fs => (f1 /: fs)(_ || _._2) }

    private lazy val b_term: Parser[Boolean] = (b_not_factor ~ rep(("and" | "AND" | "&&") ~ b_not_factor)) ^^ { case f1 ~ fs => (f1 /: fs)(_ && _._2) }

    private lazy val b_not_factor: Parser[Boolean] = opt("not" | "NOT" | "!") ~ b_factor ^^ { case Some(v) ~ f => !f; case None ~ f => f }

    private lazy val b_factor: Parser[Boolean] = b_literal | b_variable | ("(" ~ b_expression ~ ")" ^^ { case "(" ~ exp ~ ")" => exp })

    private lazy val b_literal: Parser[Boolean] = "true" ^^ (x => true) | "false" ^^ (x => false)

    val variablePattern = """\[([A-Za-z0-9_\-]+)\]""".r

    private lazy val b_variable: Parser[Boolean] =
      variableMap.keysIterator.map(x => Parser("[" + x + "]")).reduceLeft(_ | _) ^^
        (x ⇒ variableMap((for (m <- variablePattern findFirstMatchIn x) yield m group 1).getOrElse("")))

    def parse(expression: ConditionExpression) = this.parseAll(b_expression, expression.value)
  }
}