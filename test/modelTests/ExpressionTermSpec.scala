package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._
import models.Profile.driver.simple._

class ExpressionTermSpec extends PlaySpec with OneAppPerSuite {
  "An ExpressionTerm companion object" must {
    "retrieve an ExpressionTerm by label (matching case)" in {
      DB.withTransaction { implicit session =>
        val termId = ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        ExpressionTerm.findByLabel("some_term").get.id.get mustBe termId

        session.rollback()
      }
    }

    "not retrieve an ExpressionTerm by label (non-matching case)" in {
      DB.withTransaction { implicit session =>
        ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        ExpressionTerm.findByLabel("Some_term") mustBe None

        session.rollback()
      }
    }
  }

  "An ExpressionTerm" must {
    "should update dependent rule condition expression" in {
      DB.withTransaction { implicit session =>
        val term = ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65))
        val termId = ExpressionTerm.insert(term)
        val ruleId = Rule.insert(Rule(None, "Some rule", ConditionExpression("[some_term] AND true"), None, None))

        ExpressionTerm.update(term.copy(id = Some(termId), label = "changed_term"))

        Rule.find(ruleId).get.conditionExpression.value mustBe "[changed_term] AND true"

        session.rollback()
      }
    }

    "should update dependent statement term display condition" in {
      DB.withTransaction { implicit session =>
        val term = ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65))
        val termId = ExpressionTerm.insert(term)
        val stId = ExpressionTerm.insert(ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term] AND true")), None, None))

        ExpressionTerm.update(term.copy(id = Some(termId), label = "changed_term"))

        StatementTerm.find(stId).get.displayCondition.get.value mustBe "[changed_term] AND true"

        session.rollback()
      }
    }
  }

  "A StatementTerm" must {
    "create links with the expression terms referenced in its display condition" in {
      DB.withTransaction { implicit session =>
        val termId = ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        val stId = ExpressionTerm.insert(ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term]")), None, None))
        TableQuery[ExpressionTermsStatementTerms]
          .filter(_.statementTermId === stId)
          .filter(_.expressionTermId === termId)
          .length.run mustBe 1

        session.rollback()
      }
    }

    "remove links with expression terms that are no longer referenced in its display condition" in {
      DB.withTransaction { implicit session =>
        val termId = ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        val st = ExpressionTerm(None, "some_other_term", None, None, Some("template"), Some(ConditionExpression("[some_term]")), None, None)
        val stId = ExpressionTerm.insert(st)

        ExpressionTerm.update(st.copy(id = Some(stId), displayCondition = Some(ConditionExpression("true"))))
        TableQuery[ExpressionTermsStatementTerms]
          .filter(_.statementTermId === stId)
          .filter(_.expressionTermId === termId)
          .length.run mustBe 0

        session.rollback()
      }
    }
  }
}
