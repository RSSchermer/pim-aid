package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._
import models.Profile.driver.simple._

class RuleSpec extends PlaySpec with OneAppPerSuite {
  "A Rule" must {
    "create links with the expression terms referenced in its condition" in {
      DB.withTransaction { implicit session =>
        val termId = ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        val ruleId = Rule.insert(Rule(None, "Some rule", ConditionExpression("[some_term]"), None, None))
        TableQuery[ExpressionTermsRules].filter(_.ruleId === ruleId).filter(_.expressionTermId === termId)
          .length.run mustBe 1

        session.rollback()
      }
    }

    "remove links with expression terms that are no longer referenced in its condition" in {
      DB.withTransaction { implicit session =>
        val termId = ExpressionTerm.insert(ExpressionTerm(None, "some_term", None, None, None, None, Some(">="), Some(65)))
        val rule = Rule(None, "Some rule", ConditionExpression("[some_term]"), None, None)
        val ruleId = Rule.insert(rule)

        Rule.update(rule.copy(id = Some(ruleId), conditionExpression = ConditionExpression("true")))
        TableQuery[ExpressionTermsRules].filter(_.ruleId === ruleId).filter(_.expressionTermId === termId)
          .length.run mustBe 0

        session.rollback()
      }
    }
  }
}
