package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

import models._
import models.meta.Profile.driver.api._
import models.meta.Schema._

class ExpressionTermSpec extends FunSpec with DBSpec with Matchers {
  describe("An ExpressionTerm companion object") {
    it("retrieves an ExpressionTerm by label (matching case)") {
      rollback {
        for {
          referenceID <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          retrievedID <- ExpressionTerm.hasLabel("some_term").map(_.id).result.headOption
        } yield {
          retrievedID.get shouldBe referenceID
        }
      }
    }

    it("does not retrieve an ExpressionTerm by label (non-matching case)") {
      rollback {
        for {
          referenceID <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          retrievedID <- ExpressionTerm.hasLabel("Some_term").map(_.id).result.headOption
        } yield {
          retrievedID shouldBe None
        }
      }
    }
  }

  describe("An ExpressionTerm") {
    it("updates dependent rule condition expressions when updated") {
      rollback {
        val term = ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65))

        for {
          termId <- ExpressionTerm.insert(term)
          ruleId <- Rule.insert(Rule(None, "Some rule", ConditionExpression("[some_term] AND true"), None, None, None))

          _ <- ExpressionTerm.update(term.copy(id = Some(termId), label = "changed_term"))

          dependentRule <- Rule.one(ruleId).result
        } yield {
          dependentRule.get.conditionExpression.value shouldBe "[changed_term] AND true"
        }
      }
    }

    it("updates dependent statement term display conditions when updated") {
      rollback {
        val term = ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65))

        for {
          termId <- ExpressionTerm.insert(term)
          stId <- ExpressionTerm.insert(ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term] AND true")), None, None))

          _ <- ExpressionTerm.update(term.copy(id = Some(termId), label = "changed_term"))

          dependentStatementTerm <- StatementTerm.one(stId).result
        } yield {
          dependentStatementTerm.get.displayCondition.get.value shouldBe "[changed_term] AND true"
        }
      }
    }
  }

  describe("A StatementTerm") {
    it("creates links to the expression terms referenced in its display condition") {
      rollback {
        for {
          termId <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          stId <- ExpressionTerm.insert(ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term]")), None, None))
          linkCount <- TableQuery[ExpressionTermsStatementTerms].filter(x => x.statementTermId === stId && x.expressionTermId === termId).length.result
        } yield {
          linkCount shouldBe 1
        }
      }
    }

    it("removes links to expression terms that are no longer referenced in its display condition") {
      rollback {
        val st = ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term]")), None, None)

        for {
          termId <- ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
          stId <- ExpressionTerm.insert(st)

          _ <- ExpressionTerm.update(st.copy(id = Some(stId), displayCondition = Some(ConditionExpression("true"))))

          linkCount <- TableQuery[ExpressionTermsStatementTerms].filter(x => x.statementTermId === stId && x.expressionTermId === termId).length.result
        } yield {
          linkCount shouldBe 0
        }
      }
    }
  }
}
