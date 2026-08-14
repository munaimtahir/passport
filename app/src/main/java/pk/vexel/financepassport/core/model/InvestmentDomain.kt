package pk.vexel.financepassport.core.model

import pk.vexel.financepassport.core.database.InvestmentEventEntity

data class InvestmentPosition(
    val securityName: String,
    val quantityMinor: Long,
    val costBasisMinor: Long,
    val realizedGainLossMinor: Long,
    val incomeMinor: Long,
)

/** Deterministic average-cost position calculation; all monetary values are minor units. */
fun calculateInvestmentPosition(securityName: String, events: List<InvestmentEventEntity>): InvestmentPosition {
    var quantity = 0L
    var costBasis = 0L
    var realized = 0L
    var income = 0L
    events.sortedBy { it.dateEpochDay }.forEach { event ->
        require(event.securityName == securityName) { "Investment event security mismatch" }
        when (event.type.uppercase()) {
            "BUY" -> { require(event.quantityMinor != null && event.quantityMinor > 0) { "Buy quantity must be positive" }; quantity += event.quantityMinor; costBasis += event.grossAmountMinor + event.feesMinor }
            "SELL" -> {
                val sold = event.quantityMinor ?: error("Sell quantity is required")
                require(sold > 0 && sold <= quantity) { "Sell quantity exceeds position" }
                val costOut = costBasis * sold / quantity
                quantity -= sold
                costBasis -= costOut
                realized += event.grossAmountMinor - event.feesMinor - costOut
            }
            "DIVIDEND", "DISTRIBUTION", "PROFIT" -> income += event.grossAmountMinor - event.taxWithheldMinor - event.feesMinor
            "FEE" -> costBasis += event.feesMinor
            "TAX_WITHHELD" -> income -= event.taxWithheldMinor
            "ADJUSTMENT" -> costBasis += event.grossAmountMinor
        }
    }
    return InvestmentPosition(securityName, quantity, costBasis, realized, income)
}
