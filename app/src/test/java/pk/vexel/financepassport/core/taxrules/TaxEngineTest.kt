package pk.vexel.financepassport.core.taxrules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.model.Money

class TaxEngineTest {
    private val year = TaxYear("2026", "PK", "2026", 0, 1000, "pk-structural-1")

    @Test fun sameFactsAndRulesetProduceDeterministicDraft() {
        val candidates = listOf(TaxCandidate("financial_event", "salary-1", 10, Money.pkr(1000), "Salary", TaxEventType.EMPLOYMENT_INCOME, TaxRelevance.RELEVANT))
        val rules = defaultPakistanStructuralRules()
        val generator = AnnualDraftGenerator()
        assertEquals(generator.generate(year, rules, candidates), generator.generate(year, rules, candidates))
    }

    @Test fun ambiguousMappingCreatesIssueWithoutChangingSource() {
        val candidate = TaxCandidate("investment_event", "sale-1", 10, Money.pkr(1000), "Sold shares", TaxEventType.INVESTMENT_SALE, TaxRelevance.RELEVANT)
        val draft = AnnualDraftGenerator().generate(year, defaultPakistanStructuralRules(), listOf(candidate))
        assertTrue(draft.issues.any { it.code == "AMBIGUOUS" })
        assertEquals("sale-1", draft.lines.single().sourceIds.single())
    }

    @Test fun balancedWealthReconcilesToZero() {
        val result = reconcileWealth(WealthReconciliationInput(Money.pkr(1000), Money.pkr(500), Money.pkr(200), Money.pkr(100), Money.pkr(0), Money.pkr(1200)))
        assertEquals(Money.pkr(1200), result.expectedClosing)
        assertEquals(Money(Money.pkr(0).minorUnits), result.unexplainedDifference)
    }
}
