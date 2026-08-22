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

data class WealthReconciliationInput(val opening: Money, val inflows: Money, val expenditure: Money, val outflows: Money, val adjustments: Money, val recordedClosing: Money)
data class WealthReconciliationResult(val expectedClosing: Money, val unexplainedDifference: Money, val calculation: String)

fun reconcileWealth(input: WealthReconciliationInput): WealthReconciliationResult {
    val expected = input.opening + input.inflows - input.expenditure - input.outflows + input.adjustments
    val difference = input.recordedClosing - expected
    return WealthReconciliationResult(expected, difference, "opening + inflows - expenditure - outflows + adjustments")
}

fun defaultPakistanStructuralRules(): TaxRuleset = TaxRuleset("PK", "UNSPECIFIED", "pk-structural-1", listOf(
    TaxRule(TaxEventType.EMPLOYMENT_INCOME, "INCOME", "EMPLOYMENT_INCOME", listOf("salary slip", "annual salary certificate")),
    TaxRule(TaxEventType.BANK_PROFIT, "INCOME", "BANK_PROFIT", listOf("profit certificate", "withholding certificate")),
    TaxRule(TaxEventType.DIVIDEND, "INCOME", "DIVIDEND", listOf("dividend statement")),
    TaxRule(TaxEventType.INVESTMENT_SALE, "INVESTMENTS", "CAPITAL_GAIN_OR_LOSS", listOf("broker statement"), ambiguous = true),
    TaxRule(TaxEventType.PERSONAL_EXPENDITURE, "EXPENDITURE", "PERSONAL_EXPENDITURE", emptyList()),
))
