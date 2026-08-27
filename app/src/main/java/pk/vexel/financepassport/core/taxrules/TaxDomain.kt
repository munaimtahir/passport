package pk.vexel.financepassport.core.taxrules

import pk.vexel.financepassport.core.model.Money

enum class TaxRelevance { UNKNOWN, NOT_RELEVANT, POTENTIALLY_RELEVANT, RELEVANT }
enum class EvidenceState { NONE, OPTIONAL, REQUESTED, ATTACHED, VERIFIED_BY_USER, NOT_AVAILABLE, NOT_REQUIRED }
enum class ReviewState { DRAFT, CAPTURED, NEEDS_EVIDENCE, NEEDS_CLASSIFICATION, REVIEWED, INCLUDED, EXCLUDED }

enum class TaxEventType {
    EMPLOYMENT_INCOME, BUSINESS_INCOME, PROFESSIONAL_INCOME, RENTAL_INCOME, BANK_PROFIT,
    DIVIDEND, CAPITAL_GAIN, CAPITAL_LOSS, TAX_WITHHELD, ADVANCE_TAX, TAX_PAYMENT,
    ASSET_ACQUISITION, ASSET_DISPOSAL, LIABILITY_CREATED, LIABILITY_REPAID,
    PERSONAL_EXPENDITURE, DONATION, ZAKAT, INSURANCE_PENSION, FOREIGN_INCOME,
    FOREIGN_ASSET, INVESTMENT_PURCHASE, INVESTMENT_SALE, OTHER_INCOME, OTHER_TAX_EVENT,
}

data class TaxYear(val id: String, val jurisdiction: String, val label: String, val startEpochDay: Long, val endEpochDay: Long, val rulesetVersion: String)

data class TaxCandidate(
    val sourceType: String,
    val sourceId: String,
    val dateEpochDay: Long,
    val amount: Money,
    val description: String,
    val suggestedType: TaxEventType?,
    val relevance: TaxRelevance,
)

data class TaxMapping(
    val eventType: TaxEventType,
    val sectionCode: String,
    val categoryCode: String,
    val amount: Money,
    val evidenceSuggestions: List<String>,
    val derivation: String,
    val isAmbiguous: Boolean = false,
)

data class TaxIssue(val code: String, val title: String, val explanation: String, val sourceId: String?)

data class ClassificationResult(val mapping: TaxMapping?, val issues: List<TaxIssue>)

interface TaxClassifier {
    fun classify(candidate: TaxCandidate, ruleset: TaxRuleset): ClassificationResult
}

data class TaxRule(val eventType: TaxEventType, val sectionCode: String, val categoryCode: String, val evidence: List<String>, val ambiguous: Boolean = false)
data class TaxRuleset(val jurisdiction: String, val taxYear: String, val version: String, val rules: List<TaxRule>)

class StructuralTaxClassifier : TaxClassifier {
    override fun classify(candidate: TaxCandidate, ruleset: TaxRuleset): ClassificationResult {
        val type = candidate.suggestedType ?: return ClassificationResult(null, listOf(TaxIssue("UNMAPPED", "Needs classification", "Choose what happened before including this item.", candidate.sourceId)))
        val rule = ruleset.rules.firstOrNull { it.eventType == type }
            ?: return ClassificationResult(null, listOf(TaxIssue("NO_RULE", "No rule available", "This item is retained but has no mapping in the selected ruleset.", candidate.sourceId)))
        val mapping = TaxMapping(type, rule.sectionCode, rule.categoryCode, candidate.amount, rule.evidence, "${ruleset.jurisdiction}:${ruleset.taxYear}:${ruleset.version}", rule.ambiguous)
        val issues = buildList {
            if (rule.ambiguous) add(TaxIssue("AMBIGUOUS", "Review classification", "Confirm the suggested treatment before preparing an annual draft.", candidate.sourceId))
            if (rule.evidence.isNotEmpty()) add(TaxIssue("EVIDENCE", "Evidence recommended", rule.evidence.joinToString(), candidate.sourceId))
        }
        return ClassificationResult(mapping, issues)
    }
}

data class DraftLine(val sectionCode: String, val categoryCode: String, val amount: Money, val sourceIds: List<String>, val calculation: String)
data class AnnualDraft(val taxYear: TaxYear, val rulesetVersion: String, val lines: List<DraftLine>, val issues: List<TaxIssue>)

class AnnualDraftGenerator(private val classifier: TaxClassifier = StructuralTaxClassifier()) {
    fun generate(year: TaxYear, ruleset: TaxRuleset, candidates: List<TaxCandidate>): AnnualDraft {
        require(year.rulesetVersion == ruleset.version) { "Tax year and ruleset versions must match" }
        val classified = candidates.map { it to classifier.classify(it, ruleset) }
        val lines = classified.mapNotNull { (candidate, result) ->
            result.mapping?.let { mapping -> DraftLine(mapping.sectionCode, mapping.categoryCode, mapping.amount, listOf(candidate.sourceId), mapping.derivation) }
        }.groupBy { it.sectionCode to it.categoryCode }.map { (key, grouped) ->
            DraftLine(key.first, key.second, grouped.fold(Money(pk.vexel.financepassport.core.model.MinorUnits(0), grouped.first().amount.currency)) { total, line -> total + line.amount }, grouped.flatMap { it.sourceIds }.distinct().sorted(), grouped.joinToString(" + ") { it.calculation })
        }.sortedWith(compareBy<DraftLine> { it.sectionCode }.thenBy { it.categoryCode })
        val issues = classified.flatMap { it.second.issues }.distinctBy { it.code to it.sourceId }
        return AnnualDraft(year, ruleset.version, lines, issues)
    }
}

/** Minimal, Room-independent shape so duplicate-candidate detection stays a pure, host-unit-testable function. */
data class DuplicateCandidateInput(val id: String, val dateEpochDay: Long, val amountMinor: Long?, val currency: String, val description: String)

/**
 * Flags pairs of tax items that share amount/currency within [windowDays] of each other as
 * possible duplicate captures of the same real-world event (mega-prompt Phase 4I, previously
 * undone). Exact-date grouping already existed as a live UI count (`TaxReadiness`/`TaxScreen`);
 * this widens detection to a small date window and turns it into persistable [TaxIssue] rows
 * instead of a display-only count, without merging or deleting anything automatically.
 */
fun detectDuplicateCandidates(items: List<DuplicateCandidateInput>, windowDays: Long = 1): List<TaxIssue> {
    val sorted = items.filter { it.amountMinor != null }.sortedBy { it.dateEpochDay }
    val issues = mutableListOf<TaxIssue>()
    for (i in sorted.indices) {
        for (j in i + 1 until sorted.size) {
            val a = sorted[i]
            val b = sorted[j]
            if (b.dateEpochDay - a.dateEpochDay > windowDays) break
            if (a.amountMinor == b.amountMinor && a.currency == b.currency) {
                issues += TaxIssue(
                    "DUPLICATE_CANDIDATE",
                    "Possible duplicate entry",
                    "\"${a.description}\" and \"${b.description}\" share the same amount within $windowDays day(s) of each other — review whether the same event was captured twice.",
                    b.id,
                )
            }
        }
    }
    return issues.distinctBy { it.sourceId }
}

data class WealthReconciliationInput(val opening: Money, val inflows: Money, val expenditure: Money, val outflows: Money, val adjustments: Money, val recordedClosing: Money)
data class WealthReconciliationResult(val expectedClosing: Money, val unexplainedDifference: Money, val calculation: String)

fun reconcileWealth(input: WealthReconciliationInput): WealthReconciliationResult {
    val expected = input.opening + input.inflows - input.expenditure - input.outflows + input.adjustments
    val difference = input.recordedClosing - expected
    return WealthReconciliationResult(expected, difference, "opening + inflows - expenditure - outflows + adjustments")
}

/**
 * The bundled structural Pakistan ruleset, now data-driven: the content below used to be a
 * hardcoded Kotlin list; it is loaded from `src/main/resources/taxrules/pk-structural-1.json`
 * via [BundledTaxRulesets] so a future ruleset version can be added as data, not code.
 */
fun defaultPakistanStructuralRules(): TaxRuleset = BundledTaxRulesets.loadDefault()
