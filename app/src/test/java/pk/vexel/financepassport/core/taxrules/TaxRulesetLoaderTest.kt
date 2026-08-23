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
        assertEquals(BundledTaxRulesets.CURRENT_VERSION, ruleset.version)
        assertTrue(ruleset.rules.isNotEmpty())
        // defaultPakistanStructuralRules() must delegate to the same bundled JSON, not a
        // separate hardcoded copy, so the two never drift.
        assertEquals(ruleset, defaultPakistanStructuralRules())
    }

    /**
     * Phase 11: a second ruleset version now ships alongside the first. This proves both are
     * independently loadable, parse to different (but each internally consistent) rule sets, and
     * that neither loading nor re-loading a fixed version mutates or drifts from the other —
     * i.e. the "historical ruleset versions remain immutable" requirement is actually exercised
     * with more than one version in existence, not just architecturally possible.
     */
    @Test fun twoRulesetVersionsCoexistAndAreEachIndependentlyReproducible() {
        val v1 = BundledTaxRulesets.loadVersion("pk-structural-1")
        val v2 = BundledTaxRulesets.loadVersion("pk-structural-2")

        assertEquals("pk-structural-1", v1.version)
        assertEquals("pk-structural-2", v2.version)
        assertTrue("v2 must differ from v1 (e.g. an added rule)", v1 != v2)

        // Reproducible: loading the same version twice, even after loading the other version in
        // between, yields an equal result both times — no shared mutable state between loads.
        assertEquals(v1, BundledTaxRulesets.loadVersion("pk-structural-1"))
        assertEquals(v2, BundledTaxRulesets.loadVersion("pk-structural-2"))

        // v2 is a strict superset of v1's rules in this bundled pair (adds a DONATION rule),
        // demonstrated rather than assumed.
        assertTrue(v1.rules.all { rule -> v2.rules.any { it.eventType == rule.eventType } })
        assertTrue(v2.rules.any { it.eventType == TaxEventType.DONATION })
        assertTrue(v1.rules.none { it.eventType == TaxEventType.DONATION })
    }

    @Test fun loadingAnUnregisteredVersionIsRejected() {
        try {
            BundledTaxRulesets.loadVersion("pk-structural-999")
            fail("Expected a RulesetError for an unregistered version")
        } catch (expected: RulesetError.MissingField) {
            assertTrue(expected.message!!.contains("pk-structural-999"))
        }
    }
}
