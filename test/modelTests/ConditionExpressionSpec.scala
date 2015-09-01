package modelTests

import org.scalatest.{FunSpec, Matchers}

class ConditionExpressionSpec extends FunSpec with ModelSpec with Matchers {
  describe("A ConditionExpression") {
    it("identifies the expression terms it uses correctly") {
      val labels = ConditionExpression("[term1] AND [term3]").expressionTermLabels
      labels should contain theSameElementsAs Seq("term1", "term3")
    }

    it("replaces a term label correctly") {
      val CE = ConditionExpression("[term1] AND [term3]")
      val updatedCE = CE.replaceLabel("term3", "term2")

      updatedCE.value shouldBe "[term1] AND [term2]"
    }
  }

  describe("A ConditionExpressionParser") {
    it("parses a malformed ConditionExpression as invalid") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3]")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) shouldBe a [parser.Failure]
    }

    it("parses a ConditionExpression that reference a non-existent variable as invalid") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) shouldBe a [parser.Failure]
    }

    it("parses a correctly formed ConditionExpression as valid") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) shouldBe a [parser.Success[Boolean]]
    }

    it("parses `[term1] AND ([term2] OR NOT [term3])` as true when: term1 is true, term2 is true and term3 is true") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get shouldBe true
    }

    it("parses `[term1] AND ([term2] OR NOT [term3])` as true when: term1 is true, term2 is false and term3 is false") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> false, "term3" -> false)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get shouldBe true
    }

    it("parses `[term1] AND ([term2] OR NOT [term3])` as false when: term1 is false, term2 is false and term3 is false") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> false, "term3" -> false)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get shouldBe false
    }

    it("parses `[term1] AND ([term2] OR NOT [term3])` as false when: term1 is true, term2 is false and term3 is true") {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> false, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get shouldBe false
    }
  }
}
