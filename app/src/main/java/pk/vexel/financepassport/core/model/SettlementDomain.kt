package pk.vexel.financepassport.core.model

data class LiabilityInstallmentSplit(
    val cashOutflowMinor: Long,
    val principalMinor: Long,
    val financingCostMinor: Long,
) {
    init {
        require(cashOutflowMinor > 0) { "Installment must be positive" }
        require(principalMinor >= 0 && financingCostMinor >= 0) { "Installment components cannot be negative" }
        require(principalMinor + financingCostMinor == cashOutflowMinor) {
            "Installment components must reconcile to cash outflow"
        }
    }
}

data class ReceivableSettlementResult(
    val receivedMinor: Long,
    val remainingMinor: Long,
    val recognizesIncome: Boolean,
) {
    init {
        require(receivedMinor > 0) { "Settlement must be positive" }
        require(remainingMinor >= 0) { "Remaining receivable cannot be negative" }
    }
}

fun settleReceivable(
    outstandingMinor: Long,
    receivedMinor: Long,
    type: String,
): ReceivableSettlementResult {
    require(outstandingMinor > 0) { "Receivable is not outstanding" }
    require(receivedMinor in 1..outstandingMinor) { "Receipt exceeds outstanding receivable" }
    return ReceivableSettlementResult(
        receivedMinor = receivedMinor,
        remainingMinor = outstandingMinor - receivedMinor,
        recognizesIncome = type == "INCOME_DUE",
    )
}

data class InvestmentRedemptionReconciliation(
    val principalReturnedMinor: Long,
    val profitMinor: Long,
    val taxWithheldMinor: Long,
    val netReceivedMinor: Long,
) {
    init {
        require(principalReturnedMinor >= 0 && profitMinor >= 0 && taxWithheldMinor >= 0)
        require(principalReturnedMinor + profitMinor - taxWithheldMinor == netReceivedMinor) {
            "Investment redemption does not reconcile to net cash received"
        }
    }
}
