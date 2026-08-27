package pk.vexel.financepassport.core.export

import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.AssetEntity
import pk.vexel.financepassport.core.database.DocumentEntity
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.database.LiabilityEntity
import pk.vexel.financepassport.core.database.TaxItemEntity
import pk.vexel.financepassport.core.database.InvestmentEventEntity
import pk.vexel.financepassport.core.database.ReceivableEntity
import pk.vexel.financepassport.core.database.GoalEntity
import pk.vexel.financepassport.core.database.OfficialRecordEntity
import pk.vexel.financepassport.core.database.BudgetEntity
import pk.vexel.financepassport.core.database.TaxMappingEntity
import pk.vexel.financepassport.core.database.WealthSnapshotEntity
import pk.vexel.financepassport.core.database.TaxAnnualDraftEntity
import pk.vexel.financepassport.core.database.IncomeSourceEntity

data class ExportSnapshot(
    val accounts: List<AccountEntity>, val events: List<FinancialEventEntity>, val assets: List<AssetEntity>,
    val liabilities: List<LiabilityEntity>, val taxItems: List<TaxItemEntity>, val documents: List<DocumentEntity>,
    val investments: List<InvestmentEventEntity> = emptyList(), val receivables: List<ReceivableEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(), val officialRecords: List<OfficialRecordEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val taxMappings: List<TaxMappingEntity> = emptyList(),
    val wealthSnapshots: List<WealthSnapshotEntity> = emptyList(),
    val taxDrafts: List<TaxAnnualDraftEntity> = emptyList(),
    val incomeSources: List<IncomeSourceEntity> = emptyList(),
) {
    fun forDateRange(fromEpochDay: Long, toEpochDay: Long): ExportSnapshot {
        require(fromEpochDay <= toEpochDay) { "Report range is invalid" }
        return copy(
            events = events.filter { it.dateEpochDay in fromEpochDay..toEpochDay },
            taxItems = taxItems.filter { it.dateEpochDay in fromEpochDay..toEpochDay },
            investments = investments.filter { it.dateEpochDay in fromEpochDay..toEpochDay },
        )
    }
}

class DataExportService {
    fun json(snapshot: ExportSnapshot): String = buildString {
        append("{\"formatVersion\":1,")
        append("\"accounts\":["); append(snapshot.accounts.joinToString(",") { "{\"id\":\"${it.id}\",\"name\":\"${escape(it.name)}\",\"currency\":\"${it.currency}\",\"openingBalanceMinor\":${it.openingBalanceMinor}}" }); append("],")
        append("\"financialEvents\":["); append(snapshot.events.joinToString(",") { "{\"id\":\"${it.id}\",\"type\":\"${it.eventType}\",\"amountMinor\":${it.amountMinor},\"currency\":\"${it.currency}\",\"description\":\"${escape(it.description)}\"}" }); append("],")
        append("\"assets\":["); append(snapshot.assets.joinToString(",") { "{\"id\":\"${it.id}\",\"title\":\"${escape(it.title)}\",\"valueMinor\":${it.currentEstimatedValueMinor}}" }); append("],")
        append("\"liabilities\":["); append(snapshot.liabilities.joinToString(",") { "{\"id\":\"${it.id}\",\"title\":\"${escape(it.title)}\",\"outstandingMinor\":${it.outstandingAmountMinor}}" }); append("],")
        append("\"taxItems\":["); append(snapshot.taxItems.joinToString(",") { "{\"id\":\"${it.id}\",\"sourceId\":\"${it.sourceId}\",\"type\":\"${it.taxEventType}\",\"amountMinor\":${it.grossAmountMinor ?: 0}}" }); append("],")
        append("\"documents\":["); append(snapshot.documents.joinToString(",") { "{\"id\":\"${it.id}\",\"title\":\"${escape(it.title)}\",\"sha256\":\"${it.sha256}\"}" }); append("],")
        append("\"investments\":["); append(snapshot.investments.joinToString(",") { "{\"id\":\"${it.id}\",\"security\":\"${escape(it.securityName)}\",\"type\":\"${it.type}\",\"grossAmountMinor\":${it.grossAmountMinor}}" }); append("],")
        append("\"receivables\":["); append(snapshot.receivables.joinToString(",") { "{\"id\":\"${it.id}\",\"title\":\"${escape(it.title)}\",\"counterparty\":\"${escape(it.counterparty)}\",\"outstandingAmountMinor\":${it.outstandingAmountMinor}}" }); append("],")
        append("\"goals\":["); append(snapshot.goals.joinToString(",") { "{\"id\":\"${it.id}\",\"title\":\"${escape(it.title)}\",\"targetAmountMinor\":${it.targetAmountMinor}}" }); append("],")
        append("\"officialRecords\":["); append(snapshot.officialRecords.joinToString(",") { "{\"id\":\"${it.id}\",\"recordType\":\"${escape(it.recordType)}\",\"title\":\"${escape(it.title)}\",\"maskedIdentifier\":\"${escape(it.maskedIdentifier ?: "")}\"}" }); append("],")
        append("\"budgets\":["); append(snapshot.budgets.joinToString(",") { "{\"id\":\"${it.id}\",\"category\":\"${escape(it.category)}\",\"monthlyLimitMinor\":${it.monthlyLimitMinor}}" }); append("],")
        append("\"taxMappings\":["); append(snapshot.taxMappings.joinToString(",") { "{\"id\":\"${it.id}\",\"taxItemId\":\"${it.taxItemId}\",\"rulesetVersion\":\"${escape(it.rulesetVersion)}\",\"taxEventType\":\"${it.taxEventType}\",\"source\":\"${it.source}\",\"supersededByMappingId\":${it.supersededByMappingId?.let { id -> "\"$id\"" } ?: "null"}}" }); append("],")
        append("\"wealthSnapshots\":["); append(snapshot.wealthSnapshots.joinToString(",") { "{\"id\":\"${it.id}\",\"taxYearId\":\"${it.taxYearId}\",\"kind\":\"${it.kind}\",\"netWealthMinor\":${it.netWealthMinor}}" }); append("],")
        append("\"taxDrafts\":["); append(snapshot.taxDrafts.joinToString(",") { "{\"id\":\"${it.id}\",\"taxYearId\":\"${it.taxYearId}\",\"draftVersion\":${it.draftVersion},\"rulesetVersion\":\"${escape(it.rulesetVersion)}\",\"status\":\"${it.status}\"}" }); append("],")
        append("\"incomeSources\":["); append(snapshot.incomeSources.joinToString(",") { "{\"id\":\"${it.id}\",\"name\":\"${escape(it.name)}\",\"sourceType\":\"${it.sourceType}\",\"status\":\"${it.status}\"}" }); append("]}")
    }

    fun csvEvents(snapshot: ExportSnapshot): String = buildString {
        appendLine("id,type,dateEpochDay,amountMinor,currency,description")
        snapshot.events.forEach { appendLine(listOf(it.id, it.eventType, it.dateEpochDay, it.amountMinor, it.currency, csv(it.description)).joinToString(",")) }
    }

    fun csvAccounts(snapshot: ExportSnapshot): String = buildString {
        appendLine("id,name,currency,openingBalanceMinor,status")
        snapshot.accounts.forEach { appendLine(listOf(it.id, csv(it.name), it.currency, it.openingBalanceMinor, it.status).joinToString(",")) }
    }

    fun csvTaxItems(snapshot: ExportSnapshot): String = buildString {
        appendLine("id,taxYearId,type,dateEpochDay,grossAmountMinor,currency,reviewState,evidenceState")
        snapshot.taxItems.forEach { appendLine(listOf(it.id, it.taxYearId, it.taxEventType, it.dateEpochDay, it.grossAmountMinor ?: 0, it.currency, it.reviewState, it.evidenceState).joinToString(",")) }
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    private fun csv(value: String) = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
}
