package pk.vexel.financepassport.core.model

enum class FinancialEventType { INCOME, EXPENSE, TRANSFER, ADJUSTMENT, FINANCING }

data class FinancialEvent(
    val id: String,
    val type: FinancialEventType,
    val amount: Money,
    val accountId: String?,
    val contextId: String?,
    val dateEpochDay: Long,
    val description: String,
)

data class TransferPair(val outgoing: FinancialEvent, val incoming: FinancialEvent) {
    init {
        require(outgoing.type == FinancialEventType.TRANSFER)
        require(incoming.type == FinancialEventType.TRANSFER)
        require(outgoing.amount == incoming.amount) { "Transfer legs must have equal amounts" }
    }
}

fun List<FinancialEvent>.incomeTotal(): Money =
    filter { it.type == FinancialEventType.INCOME }.fold(Money(MinorUnits(0))) { total, event -> total + event.amount }

fun List<FinancialEvent>.expenseTotal(): Money =
    filter { it.type == FinancialEventType.EXPENSE }.fold(Money(MinorUnits(0))) { total, event -> total + event.amount }
