package modelTests

import org.scalatestplus.play._
import play.api.db.slick.DB
import models._
import models.meta.Profile.driver.simple._
import models.meta.Schema._

class UserSessionSpec extends PlaySpec with OneAppPerSuite {
  "The UserSession companion" must {
    "generate a random alphanumeric token of the specified length" in {
      UserSession.generateToken(12).value must fullyMatch regex """[0-9A-z]{12}"""
    }

    "create a new UserSession with the specified token" in {
      DB.withTransaction { implicit session =>
        UserSession.create(UserToken("123456789"))
        UserSession.find(UserToken("123456789")) must not be None

        session.rollback()
      }
    }

    "create a new UserSession a random alphanumeric token" in {
      DB.withTransaction { implicit session =>
        UserSession.create().token.value must fullyMatch regex """[0-9A-z]+"""
        session.rollback()
      }
    }
  }

  "A UserSession" must {
    "build the right set of independent statements" in {
      DB.withTransaction { implicit session =>
        ExpressionTerm.insert(ExpressionTerm(None, "term_1", None, None, Some("statement1"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "term_2", None, None, Some("statement2"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "term_3", None, None, Some("statement3"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "term_4", None, None, Some("statement4"), Some(ConditionExpression("true")), None, None))

        UserSession.create().buildIndependentStatements.map(_.text) must
          contain theSameElementsAs Seq("statement1", "statement2", "statement3")

        session.rollback()
      }
    }

    "save the independent statement selection" in {
      DB.withTransaction { implicit session =>
        val id1 = ExpressionTerm.insert(ExpressionTerm(None, "term_1", None, None, Some("statement1"), None, None, None))
        val id2 = ExpressionTerm.insert(ExpressionTerm(None, "term_2", None, None, Some("statement2"), None, None, None))
        val id3 = ExpressionTerm.insert(ExpressionTerm(None, "term_3", None, None, Some("statement3"), None, None, None))

        val us = UserSession.create()
        val statements = Seq(
          Statement(id1, "statement1", selected = true),
          Statement(id2, "statement2", selected = false),
          Statement(id3, "statement3", selected = true)
        )

        us.saveIndependentStatementSelection(statements)

        us.buildIndependentStatements.map(x => (x.termID, x.selected)) must
          contain theSameElementsAs Seq((id1, true), (id2, false), (id3, true))

        session.rollback()
      }
    }

    "build the right set of conditional statements" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val betaBlockersId = DrugGroup.insert(DrugGroup(None, "beta_blockers"))
        val abeInhibitorsId = DrugGroup.insert(DrugGroup(None, "ace_inhibitors"))
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val enalaprilId = GenericType.insert(GenericType(None, "enalapril"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, metoprololId))
        TableQuery[DrugGroupsGenericTypes].insert((abeInhibitorsId, enalaprilId))
        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))

        // Set up expression terms for display conditions
        ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "ace_inhibitors", None, Some(abeInhibitorsId), None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "enalapril", Some(enalaprilId), None, None, None, None, None))
        val statement1TermId = ExpressionTerm.insert(ExpressionTerm(None, "statement1", None, None, Some("statement1"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "statement2", None, None, Some("statement2"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))
        ExpressionTerm.insert(ExpressionTerm(None, "80_or_older", None, None, None, None, Some(">="), Some(80)))

        // Set up conditional statement terms
        ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("betaBlockersTerm"), Some(ConditionExpression("[beta_blockers]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "2", None, None, Some("aceInhibitorsTerm"), Some(ConditionExpression("[ace_inhibitors]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "3", None, None, Some("metoprololTerm"), Some(ConditionExpression("[metoprolol]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "4", None, None, Some("enalaprilTerm"), Some(ConditionExpression("[enalapril]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "5", None, None, Some("statement1Term"), Some(ConditionExpression("[statement1]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "6", None, None, Some("statement2Term"), Some(ConditionExpression("[statement2]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "7", None, None, Some("olderThan70Term"), Some(ConditionExpression("[70_or_older]")), None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "8", None, None, Some("olderThan80Term"), Some(ConditionExpression("[80_or_older]")), None, None))

        // Set up user characteristics
        val userSession = UserSession.create().copy(age = Some(70))
        UserSession.update(userSession)
        Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(userSession.token, statement1TermId, "statement1", false))

        userSession.buildConditionalStatements.map(_.text) must
          contain theSameElementsAs Seq("betaBlockersTerm", "metoprololTerm", "statement1Term", "olderThan70Term")

        session.rollback()
      }
    }

    "should replace drug group placeholders with the matching user input product names in statements" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val betaBlockersId = DrugGroup.insert(DrugGroup(None, "beta_blockers"))
        val sotalolId = GenericType.insert(GenericType(None, "sotalol"))
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
        val sotalolProductId = MedicationProduct.insert(MedicationProduct(None, "sotalol"))

        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, metoprololId))
        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, sotalolId))
        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))
        TableQuery[GenericTypesMedicationProducts].insert((sotalolId, sotalolProductId))

        // Set up expression terms for display conditions
        ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))

        // Set up conditional statement terms
        ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{group(beta_blockers)}}."), Some(ConditionExpression("[beta_blockers]")), None, None))

        // Set up user characteristics
        val userSession = UserSession.create()
        UserSession.update(userSession)
        Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))
        Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

        userSession.buildConditionalStatements.map(_.text) must
          contain theSameElementsAs Seq("I use Selokeen.", "I use sotalol.")

        session.rollback()
      }
    }

    "should replace generic type placeholders with the matching user input product names in statements" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))

        // Set up expression terms for display conditions
        ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))

        // Set up conditional statement terms
        ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{type(metoprolol)}}."), Some(ConditionExpression("[metoprolol]")), None, None))

        // Set up user characteristics
        val userSession = UserSession.create()
        UserSession.update(userSession)
        Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))

        userSession.buildConditionalStatements.map(_.text) must
          contain theSameElementsAs Seq("I use Selokeen.")

        session.rollback()
      }
    }

    "save the conditional statement selection" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val betaBlockersId = DrugGroup.insert(DrugGroup(None, "beta_blockers"))
        val sotalolId = GenericType.insert(GenericType(None, "sotalol"))
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
        val sotalolProductId = MedicationProduct.insert(MedicationProduct(None, "sotalol"))

        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, metoprololId))
        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, sotalolId))
        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))
        TableQuery[GenericTypesMedicationProducts].insert((sotalolId, sotalolProductId))

        // Set up expression terms for display conditions
        ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))

        // Set up conditional statement terms
        val id1 = ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{group(beta_blockers)}}."), Some(ConditionExpression("[beta_blockers]")), None, None))
        val id2 = ExpressionTerm.insert(ExpressionTerm(None, "2", None, None, Some("olderThan70Term"), Some(ConditionExpression("[70_or_older]")), None, None))

        // Set up user characteristics
        val userSession = UserSession.create().copy(age = Some(70))
        UserSession.update(userSession)
        Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
        Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

        val statements = Seq(
          Statement(id1, "I use Selokeen.", selected = true),
          Statement(id1, "I use sotalol.", selected = false),
          Statement(id2, "olderThan70Term", selected = true)
        )

        userSession.saveConditionalStatementSelection(statements)

        userSession.buildConditionalStatements.map(x => (x.text, x.selected)) must
          contain theSameElementsAs Seq(("I use Selokeen.", true), ("I use sotalol.", false), ("olderThan70Term", true))

        session.rollback()
      }
    }

    "build the right suggestions" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val betaBlockersId = DrugGroup.insert(DrugGroup(None, "beta_blockers"))
        val abeInhibitorsId = DrugGroup.insert(DrugGroup(None, "ace_inhibitors"))
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val enalaprilId = GenericType.insert(GenericType(None, "enalapril"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, metoprololId))
        TableQuery[DrugGroupsGenericTypes].insert((abeInhibitorsId, enalaprilId))
        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))

        // Set up expression terms for rule conditions
        ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "ace_inhibitors", None, Some(abeInhibitorsId), None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "enalapril", Some(enalaprilId), None, None, None, None, None))
        val statement1TermId = ExpressionTerm.insert(ExpressionTerm(None, "statement1", None, None, Some("statement1"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "statement2", None, None, Some("statement2"), None, None, None))
        ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))
        ExpressionTerm.insert(ExpressionTerm(None, "80_or_older", None, None, None, None, Some(">="), Some(80)))

        // Set up rules
        val r1Id = Rule.insert(Rule(None, "1", ConditionExpression("[beta_blockers]"), None, None, None))
        val r2Id = Rule.insert(Rule(None, "2", ConditionExpression("[ace_inhibitors]"), None, None, None))
        val r3Id = Rule.insert(Rule(None, "3", ConditionExpression("[metoprolol]"), None, None, None))
        val r4Id = Rule.insert(Rule(None, "4", ConditionExpression("[enalapril]"), None, None, None))
        val r5Id = Rule.insert(Rule(None, "5", ConditionExpression("[statement1]"), None, None, None))
        val r6Id = Rule.insert(Rule(None, "6", ConditionExpression("[statement2]"), None, None, None))
        val r7Id = Rule.insert(Rule(None, "7", ConditionExpression("[70_or_older]"), None, None, None))
        val r8Id = Rule.insert(Rule(None, "8", ConditionExpression("[80_or_older]"), None, None, None))

        // Set up suggestion templates
        val s1AId = SuggestionTemplate.insert(SuggestionTemplate(None, "beta_blockers1", "beta_blockers1", None))
        val s1BId = SuggestionTemplate.insert(SuggestionTemplate(None, "beta_blockers2", "beta_blockers2", None))
        val s2Id = SuggestionTemplate.insert(SuggestionTemplate(None, "ace_inhibitors", "ace_inhibitors", None))
        val s3Id = SuggestionTemplate.insert(SuggestionTemplate(None, "metoprolol", "metoprolol", None))
        val s4Id = SuggestionTemplate.insert(SuggestionTemplate(None, "enalapril", "enalapril", None))
        val s5Id = SuggestionTemplate.insert(SuggestionTemplate(None, "statement1", "statement1", None))
        val s6Id = SuggestionTemplate.insert(SuggestionTemplate(None, "statement2", "statement2", None))
        val s7Id = SuggestionTemplate.insert(SuggestionTemplate(None, "70_or_older", "70_or_older", None))
        val s8Id = SuggestionTemplate.insert(SuggestionTemplate(None, "80_or_older", "80_or_older", None))

        TableQuery[RulesSuggestionTemplates].insert((r1Id, s1AId))
        TableQuery[RulesSuggestionTemplates].insert((r1Id, s1BId))
        TableQuery[RulesSuggestionTemplates].insert((r2Id, s2Id))
        TableQuery[RulesSuggestionTemplates].insert((r3Id, s3Id))
        TableQuery[RulesSuggestionTemplates].insert((r4Id, s4Id))
        TableQuery[RulesSuggestionTemplates].insert((r5Id, s5Id))
        TableQuery[RulesSuggestionTemplates].insert((r6Id, s6Id))
        TableQuery[RulesSuggestionTemplates].insert((r7Id, s7Id))
        TableQuery[RulesSuggestionTemplates].insert((r8Id, s8Id))

        // Set up user characteristics
        val userSession = UserSession.create().copy(age = Some(70))
        UserSession.update(userSession)
        Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
        TableQuery[StatementTermsUserSessions]
          .insert(StatementTermUserSession(userSession.token, statement1TermId, "statement1", false))

        userSession.buildSuggestions.map(_.text) must
          contain theSameElementsAs Seq("beta_blockers1", "beta_blockers2", "metoprolol", "statement1", "70_or_older")

        session.rollback()
      }
    }

    "should replace drug group placeholders with the matching user input product names in suggestions" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val betaBlockersId = DrugGroup.insert(DrugGroup(None, "beta_blockers"))
        val sotalolId = GenericType.insert(GenericType(None, "sotalol"))
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
        val sotalolProductId = MedicationProduct.insert(MedicationProduct(None, "sotalol"))

        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, metoprololId))
        TableQuery[DrugGroupsGenericTypes].insert((betaBlockersId, sotalolId))
        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))
        TableQuery[GenericTypesMedicationProducts].insert((sotalolId, sotalolProductId))

        // Set up expression terms for rule conditions
        ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))

        // Set up rules
        val ruleId = Rule.insert(Rule(None, "1", ConditionExpression("[beta_blockers]"), None, None, None))

        // Set up suggestion templates
        val stId = SuggestionTemplate
          .insert(SuggestionTemplate(None, "beta_blockers", "I use {{group(beta_blockers)}}.", Some("I use {{group(beta_blockers)}}.")))

        TableQuery[RulesSuggestionTemplates].insert((ruleId, stId))

        // Set up user characteristics
        val userSession = UserSession.create()
        UserSession.update(userSession)
        Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))
        Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

        userSession.buildSuggestions.map(x => (x.text, x.explanatoryNote)) must
          contain theSameElementsAs Seq(
            ("I use Selokeen.", Some("I use Selokeen.")),
            ("I use sotalol.", Some("I use sotalol."))
          )

        session.rollback()
      }
    }

    "should replace generic type placeholders with the matching user input product names in suggestions" in {
      DB.withTransaction { implicit session =>
        // Setup medication model
        val metoprololId = GenericType.insert(GenericType(None, "metoprolol"))
        val selokeenId = MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

        TableQuery[GenericTypesMedicationProducts].insert((metoprololId, selokeenId))

        // Set up expression terms for rule conditions
        ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))

        // Set up rules
        val ruleId = Rule.insert(Rule(None, "1", ConditionExpression("[metoprolol]"), None, None, None))

        // Set up suggestion templates
        val stId = SuggestionTemplate
          .insert(SuggestionTemplate(None, "metoprolol", "I use {{type(metoprolol)}}.", Some("I use {{type(metoprolol)}}.")))

        TableQuery[RulesSuggestionTemplates].insert((ruleId, stId))

        // Set up user characteristics
        val userSession = UserSession.create()
        UserSession.update(userSession)
        Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))

        userSession.buildSuggestions.map(x => (x.text, x.explanatoryNote)) must
          contain theSameElementsAs Seq(("I use Selokeen.", Some("I use Selokeen.")))

        session.rollback()
      }
    }
  }
}
