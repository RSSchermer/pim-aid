package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class RuleSpec extends FunSpec with DBSpec with Matchers {
  describe("A Rule") {
    it("creates links to the expression terms referenced in its condition") {
      rollback {
        for {
          termId <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          ruleId <- Rule.insert(Rule(None, "Some rule", ConditionExpression("[some_term]"), None, None, None))
          linkCount <- TableQuery[ExpressionTermsRules].filter(x => x.ruleId === ruleId && x.expressionTermId === termId).length.result
        } yield {
          linkCount shouldBe 1
        }
      }
    }

    it("removes links to expression terms that are no longer referenced in its condition") {
      rollback {
        val rule = Rule(None, "Some rule", ConditionExpression("[some_term]"), None, None, None)

        for {
          termId <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          ruleId <- Rule.insert(rule)

          _ <- Rule.update(rule.copy(id = Some(ruleId), conditionExpression = ConditionExpression("true")))

          linkCount <- TableQuery[ExpressionTermsRules].filter(x => x.ruleId === ruleId && x.expressionTermId === termId).length.result
        } yield {
          linkCount shouldBe 0
        }
      }
    }
  }
}
