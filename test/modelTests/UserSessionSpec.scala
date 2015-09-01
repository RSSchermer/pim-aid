package modelTests

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class UserSessionSpec extends FunSpec with ModelSpec with Matchers {
  import driver.api._

  describe("The UserSession companion object") {
    it("generates a random alphanumeric token of the specified length") {
      UserSession.generateToken(12).value should fullyMatch regex """[0-9A-z]{12}"""
    }

    it("creates a new UserSession with the specified token") {
      rollback {
        for {
          _ <- UserSession.create(UserToken("123456789"))
          retrievedSession <- UserSession.one(UserToken("123456789")).result
        } yield {
          retrievedSession should not be None
        }
      }
    }

    it("creates a new UserSession a random alphanumeric token") {
      rollback {
        for {
          userSession <- UserSession.create()
        } yield {
          userSession.token.value should fullyMatch regex """[0-9A-z]+"""
        }
      }
    }
  }

  describe("A UserSession") {
    it("builds the right set of independent statements") {
      rollback {
        for {
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "term_1", None, None, Some("statement1"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "term_2", None, None, Some("statement2"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "term_3", None, None, Some("statement3"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "term_4", None, None, Some("statement4"), Some(ConditionExpression("true")), None, None))

          userSession <- UserSession.create()
          statements <- userSession.buildIndependentStatements()
        } yield {
          statements.map(_.text) should contain theSameElementsAs Seq("statement1", "statement2", "statement3")
        }
      }
    }

    it("persists the independent statement selection") {
      rollback {
        for {
          id1 <- ExpressionTerm.insert(ExpressionTerm(None, "term_1", None, None, Some("statement1"), None, None, None))
          id2 <- ExpressionTerm.insert(ExpressionTerm(None, "term_2", None, None, Some("statement2"), None, None, None))
          id3 <- ExpressionTerm.insert(ExpressionTerm(None, "term_3", None, None, Some("statement3"), None, None, None))

          userSession <- UserSession.create()

          _ <- userSession.saveIndependentStatementSelection(Seq(
            Statement(id1, "statement1", selected = true),
            Statement(id2, "statement2", selected = false),
            Statement(id3, "statement3", selected = true)
          ))

          statements <- userSession.buildIndependentStatements()
        } yield {
          statements.map(x => (x.termID, x.selected)) should contain theSameElementsAs Seq(
            (id1, true),
            (id2, false),
            (id3, true)
          )
        }
      }
    }

    it("builds the right set of conditional statements") {
      rollback {
        for {
          // Setup medication model
          betaBlockersId <- DrugGroup.insert(DrugGroup(None, "beta_blockers"))
          aceInhibitorsId <- DrugGroup.insert(DrugGroup(None, "ace_inhibitors"))
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          enalaprilId <- GenericType.insert(GenericType(None, "enalapril"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, metoprololId)
          _ <- TableQuery[DrugGroupsGenericTypes] += (aceInhibitorsId, enalaprilId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)

          // Set up expression terms for display conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "ace_inhibitors", None, Some(aceInhibitorsId), None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "enalapril", Some(enalaprilId), None, None, None, None, None))
          statement1TermId <- ExpressionTerm.insert(ExpressionTerm(None, "statement1", None, None, Some("statement1"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "statement2", None, None, Some("statement2"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "80_or_older", None, None, None, None, Some(">="), Some(80)))

          // Set up conditional statement terms
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("betaBlockersTerm"), Some(ConditionExpression("[beta_blockers]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "2", None, None, Some("aceInhibitorsTerm"), Some(ConditionExpression("[ace_inhibitors]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "3", None, None, Some("metoprololTerm"), Some(ConditionExpression("[metoprolol]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "4", None, None, Some("enalaprilTerm"), Some(ConditionExpression("[enalapril]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "5", None, None, Some("statement1Term"), Some(ConditionExpression("[statement1]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "6", None, None, Some("statement2Term"), Some(ConditionExpression("[statement2]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "7", None, None, Some("olderThan70Term"), Some(ConditionExpression("[70_or_older]")), None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "8", None, None, Some("olderThan80Term"), Some(ConditionExpression("[80_or_older]")), None, None))

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
          _ <- TableQuery[StatementTermsUserSessions] +=
            StatementTermUserSession(userSession.token, statement1TermId, "statement1", false)

          statements <- userSession.copy(age = Some(70)).buildConditionalStatements()
        } yield {
          statements.map(_.text) should contain theSameElementsAs Seq(
            "betaBlockersTerm",
            "metoprololTerm",
            "statement1Term",
            "olderThan70Term"
          )
        }
      }
    }

    it("replaces drug group placeholders with the matching user input product names in statements") {
      rollback {
        for {
          // Setup medication model
          betaBlockersId <- DrugGroup.insert(DrugGroup(None, "beta_blockers"))
          sotalolId <- GenericType.insert(GenericType(None, "sotalol"))
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
          sotalolProductId <- MedicationProduct.insert(MedicationProduct(None, "sotalol"))

          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, metoprololId)
          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, sotalolId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (sotalolId, sotalolProductId)

          // Set up expression terms for display conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))

          // Set up conditional statement terms
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{group(beta_blockers)}}."), Some(ConditionExpression("[beta_blockers]")), None, None))

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))
          _ <- Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

          statements <- userSession.buildConditionalStatements()
        } yield {
          statements.map(_.text) should contain theSameElementsAs Seq("I use Selokeen.", "I use sotalol.")
        }
      }
    }

    it("replaces generic type placeholders with the matching user input product names in statements") {
      rollback {
        for {
          // Setup medication model
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)

          // Set up expression terms for display conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))

          // Set up conditional statement terms
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{type(metoprolol)}}."), Some(ConditionExpression("[metoprolol]")), None, None))

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))

          statements <- userSession.buildConditionalStatements()
        } yield {
          statements.map(_.text) should contain theSameElementsAs Seq("I use Selokeen.")
        }
      }
    }

    it("persists the conditional statement selection") {
      rollback {
        for {
          // Setup medication model
          betaBlockersId <- DrugGroup.insert(DrugGroup(None, "beta_blockers"))
          sotalolId <- GenericType.insert(GenericType(None, "sotalol"))
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
          sotalolProductId <- MedicationProduct.insert(MedicationProduct(None, "sotalol"))

          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, metoprololId)
          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, sotalolId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (sotalolId, sotalolProductId)

          // Set up expression terms for display conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))

          // Set up conditional statement terms
          id1 <- ExpressionTerm.insert(ExpressionTerm(None, "1", None, None, Some("I use {{group(beta_blockers)}}."), Some(ConditionExpression("[beta_blockers]")), None, None))
          id2 <- ExpressionTerm.insert(ExpressionTerm(None, "2", None, None, Some("olderThan70Term"), Some(ConditionExpression("[70_or_older]")), None, None))

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
          _ <- Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

          _ <- userSession.saveConditionalStatementSelection(Seq(
            Statement(id1, "I use Selokeen.", selected = true),
            Statement(id1, "I use sotalol.", selected = false),
            Statement(id2, "olderThan70Term", selected = true)
          ))

          statements <- userSession.copy(age = Some(70)).buildConditionalStatements()
        } yield {
          statements.map(x => (x.text, x.selected)) should contain theSameElementsAs Seq(
            ("I use Selokeen.", true),
            ("I use sotalol.", false),
            ("olderThan70Term", true)
          )
        }
      }
    }

    it("builds the right suggestions") {
      rollback {
        for {
          // Setup medication model
          betaBlockersId <- DrugGroup.insert(DrugGroup(None, "beta_blockers"))
          abeInhibitorsId <- DrugGroup.insert(DrugGroup(None, "ace_inhibitors"))
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          enalaprilId <- GenericType.insert(GenericType(None, "enalapril"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

          _ <- TableQuery[DrugGroupsGenericTypes] ++= Seq(
            (betaBlockersId, metoprololId),
            (abeInhibitorsId, enalaprilId)
          )
          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)

          // Set up expression terms for rule conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "ace_inhibitors", None, Some(abeInhibitorsId), None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "enalapril", Some(enalaprilId), None, None, None, None, None))
          statement1TermId <- ExpressionTerm.insert(ExpressionTerm(None, "statement1", None, None, Some("statement1"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "statement2", None, None, Some("statement2"), None, None, None))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "70_or_older", None, None, None, None, Some(">="), Some(70)))
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "80_or_older", None, None, None, None, Some(">="), Some(80)))

          // Set up rules
          r1Id <- Rule.insert(Rule(None, "1", ConditionExpression("[beta_blockers]"), None, None, None))
          r2Id <- Rule.insert(Rule(None, "2", ConditionExpression("[ace_inhibitors]"), None, None, None))
          r3Id <- Rule.insert(Rule(None, "3", ConditionExpression("[metoprolol]"), None, None, None))
          r4Id <- Rule.insert(Rule(None, "4", ConditionExpression("[enalapril]"), None, None, None))
          r5Id <- Rule.insert(Rule(None, "5", ConditionExpression("[statement1]"), None, None, None))
          r6Id <- Rule.insert(Rule(None, "6", ConditionExpression("[statement2]"), None, None, None))
          r7Id <- Rule.insert(Rule(None, "7", ConditionExpression("[70_or_older]"), None, None, None))
          r8Id <- Rule.insert(Rule(None, "8", ConditionExpression("[80_or_older]"), None, None, None))

          // Set up suggestion templates
          s1AId <- SuggestionTemplate.insert(SuggestionTemplate(None, "beta_blockers1", "beta_blockers1", None))
          s1BId <- SuggestionTemplate.insert(SuggestionTemplate(None, "beta_blockers2", "beta_blockers2", None))
          s2Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "ace_inhibitors", "ace_inhibitors", None))
          s3Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "metoprolol", "metoprolol", None))
          s4Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "enalapril", "enalapril", None))
          s5Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "statement1", "statement1", None))
          s6Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "statement2", "statement2", None))
          s7Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "70_or_older", "70_or_older", None))
          s8Id <- SuggestionTemplate.insert(SuggestionTemplate(None, "80_or_older", "80_or_older", None))

          _ <- TableQuery[RulesSuggestionTemplates] ++= Seq(
            (r1Id, s1AId),
            (r1Id, s1BId),
            (r2Id, s2Id),
            (r3Id, s3Id),
            (r4Id, s4Id),
            (r5Id, s5Id),
            (r6Id, s6Id),
            (r7Id, s7Id),
            (r8Id, s8Id)
          )

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "selokeen", userSession.token, Some(selokeenId)))
          _ <- TableQuery[StatementTermsUserSessions] +=
            StatementTermUserSession(userSession.token, statement1TermId, "statement1", false)

          suggestions <- userSession.copy(age = Some(70)).buildSuggestions()
        } yield {
          suggestions.map(_.text) should contain theSameElementsAs Seq(
            "beta_blockers1",
            "beta_blockers2",
            "metoprolol",
            "statement1",
            "70_or_older"
          )
        }
      }
    }

    it("replaces drug group placeholders with the matching user input product names in suggestions") {
      rollback {
        for {
          // Setup medication model
          betaBlockersId <- DrugGroup.insert(DrugGroup(None, "beta_blockers"))
          sotalolId <- GenericType.insert(GenericType(None, "sotalol"))
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))
          sotalolProductId <- MedicationProduct.insert(MedicationProduct(None, "sotalol"))

          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, metoprololId)
          _ <- TableQuery[DrugGroupsGenericTypes] += (betaBlockersId, sotalolId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)
          _ <- TableQuery[GenericTypesMedicationProducts] += (sotalolId, sotalolProductId)

          // Set up expression terms for rule conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "beta_blockers", None, Some(betaBlockersId), None, None, None, None))

          // Set up rules
          ruleId <- Rule.insert(Rule(None, "1", ConditionExpression("[beta_blockers]"), None, None, None))

          // Set up suggestion templates
          stId <- SuggestionTemplate.insert(SuggestionTemplate(None, "beta_blockers", "I use {{group(beta_blockers)}}.", Some("I use {{group(beta_blockers)}}.")))

          _ <- TableQuery[RulesSuggestionTemplates] += (ruleId, stId)

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))
          _ <- Drug.insert(Drug(None, "sotalol", userSession.token, Some(sotalolProductId)))

          suggestions <- userSession.buildSuggestions()
        } yield {
          suggestions.map(x => (x.text, x.explanatoryNote)) should contain theSameElementsAs Seq(
            ("I use Selokeen.", Some("I use Selokeen.")),
            ("I use sotalol.", Some("I use sotalol."))
          )
        }
      }
    }

    it("replaces generic type placeholders with the matching user input product names in suggestions") {
      rollback {
        for {
          // Setup medication model
          metoprololId <- GenericType.insert(GenericType(None, "metoprolol"))
          selokeenId <- MedicationProduct.insert(MedicationProduct(None, "Selokeen"))

          _ <- TableQuery[GenericTypesMedicationProducts] += (metoprololId, selokeenId)

          // Set up expression terms for rule conditions
          _ <- ExpressionTerm.insert(ExpressionTerm(None, "metoprolol", Some(metoprololId), None, None, None, None, None))

          // Set up rules
          ruleId <- Rule.insert(Rule(None, "1", ConditionExpression("[metoprolol]"), None, None, None))

          // Set up suggestion templates
          stId <- SuggestionTemplate.insert(SuggestionTemplate(None, "metoprolol", "I use {{type(metoprolol)}}.", Some("I use {{type(metoprolol)}}.")))

          _ <- TableQuery[RulesSuggestionTemplates] += (ruleId, stId)

          // Set up user characteristics
          userSession <- UserSession.create()
          _ <- Drug.insert(Drug(None, "Selokeen", userSession.token, Some(selokeenId)))

          suggestions <- userSession.buildSuggestions()
        } yield {
          suggestions.map(x => (x.text, x.explanatoryNote)) should contain theSameElementsAs Seq(
            ("I use Selokeen.", Some("I use Selokeen."))
          )
        }
      }
    }
  }
}
