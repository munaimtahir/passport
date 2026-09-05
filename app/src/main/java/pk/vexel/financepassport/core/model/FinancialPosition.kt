package pk.vexel.financepassport.core.model

import pk.vexel.financepassport.core.database.AssetEntity
import pk.vexel.financepassport.core.database.InvestmentEventEntity
import pk.vexel.financepassport.core.database.LiabilityEntity
import pk.vexel.financepassport.core.database.ReceivableEntity
import pk.vexel.financepassport.core.database.SimpleInvestmentEntity

/**
 * The single canonical financial position, in PKR minor units. This is the one source of truth
 * for net worth and must be reused by Home, Wealth, Reports, Tax and Reconciliation rather than
 * each screen recomputing its own total.
 */
data class FinancialPosition(
    val liquidFundsMinor: Long,
    val investmentsValueMinor: Long,
    val assetsValueMinor: Long,
    val receivablesValueMinor: Long,
    val liabilitiesValueMinor: Long,
    val monthlyIncomeMinor: Long,
    val monthlyExpenseMinor: Long,
) {
    /** Liquid funds, recorded investment cost basis, non-liquid assets and receivables — before liabilities. */
    val totalAssetsMinor: Long get() = liquidFundsMinor + investmentsValueMinor + assetsValueMinor + receivablesValueMinor

    /** Canonical net worth: everything the user owns, minus everything they owe. */
    val netWorthMinor: Long get() = totalAssetsMinor - liabilitiesValueMinor
}

/**
 * Sum of recorded cost basis across every distinct security still or ever held. No live market
 * price feed exists in this product (see MVP non-goals), so the recorded investment value is the
 * traceable cost basis of open positions, never a fabricated current market value.
 */
fun calculateInvestmentsValueMinor(events: List<InvestmentEventEntity>): Long =
    events.groupBy { it.securityName }.entries.sumOf { (name, group) -> calculateInvestmentPosition(name, group).costBasisMinor }

/** Current position value for Wave E's simple holdings; valuations are not financial events. */
fun calculateSimpleInvestmentsValueMinor(investments: List<SimpleInvestmentEntity>): Long =
    investments.filter { it.status == "ACTIVE" || it.status == "MATURED" }
        .sumOf { it.currentEstimatedValueMinor }

fun calculateFinancialPosition(
    accountsOpeningBalanceMinor: Long,
    accountsMovementMinor: Long,
    assets: List<AssetEntity>,
    liabilities: List<LiabilityEntity>,
    investments: List<InvestmentEventEntity>,
    receivables: List<ReceivableEntity>,
    monthlyIncomeMinor: Long,
    monthlyExpenseMinor: Long,
    simpleInvestments: List<SimpleInvestmentEntity> = emptyList(),
): FinancialPosition = FinancialPosition(
    liquidFundsMinor = accountsOpeningBalanceMinor + accountsMovementMinor,
    // Legacy investment events remain supported for historical data; normalized simple
    // positions are added as a separate source and are never counted as income/expense.
    investmentsValueMinor = calculateInvestmentsValueMinor(investments) + calculateSimpleInvestmentsValueMinor(simpleInvestments),
    assetsValueMinor = assets.filter { it.status == "ACTIVE" && it.includeInNetWorth }
        .sumOf { it.currentEstimatedValueMinor * it.ownershipPercent / 100L },
    receivablesValueMinor = receivables.sumOf { it.outstandingAmountMinor },
    liabilitiesValueMinor = liabilities.sumOf { it.outstandingAmountMinor },
    monthlyIncomeMinor = monthlyIncomeMinor,
    monthlyExpenseMinor = monthlyExpenseMinor,
)
