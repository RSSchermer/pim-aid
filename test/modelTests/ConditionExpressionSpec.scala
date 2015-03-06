package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._

class ConditionExpressionSpec extends PlaySpec with OneAppPerSuite {
  "A ConditionExpression" must {
    "identify the expression terms it uses correctly" in {
      DB.withTransaction { implicit session =>
        val term1Id = ExpressionTerm.insert(ExpressionTerm(None, "term1", None, None, None, None, Some(">="), Some(65)))
        val term2Id = ExpressionTerm.insert(ExpressionTerm(None, "term2", None, None, None, None, Some(">="), Some(65)))
        val term3Id = ExpressionTerm.insert(ExpressionTerm(None, "term3", None, None, None, None, Some(">="), Some(65)))

        val CETermIds = ConditionExpression("[term1] AND [term3]").expressionTerms.map(_.id).flatten
        CETermIds must contain theSameElementsAs Seq(term1Id, term3Id)

        session.rollback()
      }
    }

    "replace a term label correctly" in {
      val CE = ConditionExpression("[term1] AND [term3]")
      val updatedCE = CE.replaceLabel("term3", "term2")

      updatedCE.value mustBe "[term1] AND [term2]"
    }
  }

  "A ConditionExpressionParser" must {
    "parse a malformed ConditionExpression as invalid" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3]")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) mustBe a [parser.Failure]
    }

    "parse a ConditionExpression that reference a non-existent variable as invalid" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) mustBe a [parser.Failure]
    }

    "parse a correctly formed ConditionExpression as valid" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE) mustBe a [parser.Success[Boolean]]
    }

    "parse `[term1] AND ([term2] OR NOT [term3])` as true when: term1 is true, term2 is true and term3 is true" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> true, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get mustBe true
    }

    "parse `[term1] AND ([term2] OR NOT [term3])` as true when: term1 is true, term2 is false and term3 is false" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> false, "term3" -> false)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get mustBe true
    }

    "parse `[term1] AND ([term2] OR NOT [term3])` as false when: term1 is false, term2 is false and term3 is false" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> false, "term2" -> false, "term3" -> false)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get mustBe false
    }

    "parse `[term1] AND ([term2] OR NOT [term3])` as false when: term1 is true, term2 is false and term3 is true" in {
      val CE = ConditionExpression("[term1] AND ([term2] OR NOT [term3])")
      val variableMap = Map[String, Boolean]("term1" -> true, "term2" -> false, "term3" -> true)
      val parser = ConditionExpressionParser(variableMap)

      parser.parse(CE).get mustBe false
    }
  }
}
