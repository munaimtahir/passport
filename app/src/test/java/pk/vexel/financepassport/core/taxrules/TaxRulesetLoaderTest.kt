package pk.vexel.financepassport.core.taxrules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaxRulesetLoaderTest {
    private val validJson = """
        {
          "jurisdiction": "PK",
          "version": "pk-structural-1",
          "taxYear": "UNSPECIFIED",
          "rules": [
            { "eventType": "EMPLOYMENT_INCOME", "sectionCode": "INCOME", "categoryCode": "EMPLOYMENT_INCOME", "evidence": ["salary slip"], "ambiguous": false }
          ]
        }
    """.trimIndent()

    @Test fun validRulesetJsonLoads() {
        val ruleset = TaxRulesetLoader.parse(validJson)
        assertEquals("PK", ruleset.jurisdiction)
        assertEquals("pk-structural-1", ruleset.version)
        assertEquals(1, ruleset.rules.size)
        assertEquals(TaxEventType.EMPLOYMENT_INCOME, ruleset.rules.single().eventType)
        assertEquals(listOf("salary slip"), ruleset.rules.single().evidence)
    }

    @Test fun sameJsonParsedTwiceProducesEqualRulesets() {
        assertEquals(TaxRulesetLoader.parse(validJson), TaxRulesetLoader.parse(validJson))
    }

    @Test fun malformedJsonIsRejected() {
        try {
            TaxRulesetLoader.parse("{ not valid json")
            fail("Expected a RulesetError")
        } catch (expected: RulesetError.MalformedJson) {
            // expected
        }
    }

    @Test fun missingRequiredFieldIsRejected() {
        try {
            TaxRulesetLoader.parse("""{ "version": "v1", "taxYear": "2026", "rules": [] }""")
            fail("Expected a RulesetError for missing jurisdiction")
        } catch (expected: RulesetError.MissingField) {
            assertTrue(expected.message!!.contains("jurisdiction"))
        }
    }

    @Test fun emptyRulesArrayIsRejected() {
        try {
            TaxRulesetLoader.parse("""{ "jurisdiction": "PK", "version": "v1", "taxYear": "2026", "rules": [] }""")
            fail("Expected a RulesetError for an empty ruleset")
        } catch (expected: RulesetError.InvalidRule) {
            // expected
        }
    }

    @Test fun unknownTaxEventTypeIsRejected() {
        val json = """
            {
              "jurisdiction": "PK", "version": "v1", "taxYear": "2026",
              "rules": [ { "eventType": "NOT_A_REAL_TYPE", "sectionCode": "X", "categoryCode": "Y", "evidence": [] } ]
            }
        """.trimIndent()
        try {
            TaxRulesetLoader.parse(json)
            fail("Expected a RulesetError for an unknown tax event type")
        } catch (expected: RulesetError.UnknownTaxEventType) {
            assertTrue(expected.message!!.contains("NOT_A_REAL_TYPE"))
        }
    }

    @Test fun bundledDefaultRulesetLoadsFromClasspathResource() {
        val ruleset = BundledTaxRulesets.loadDefault()
        assertEquals("pk-structural-1", ruleset.version)
        assertTrue(ruleset.rules.isNotEmpty())
        // defaultPakistanStructuralRules() must delegate to the same bundled JSON, not a
        // separate hardcoded copy, so the two never drift.
        assertEquals(ruleset, defaultPakistanStructuralRules())
    }
}
