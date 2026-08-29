package pk.vexel.financepassport.core.model

enum class ContextDomain { PERSONAL, PROFESSIONAL }

data class FinancialContext(
    val id: String,
    val domain: ContextDomain,
    val name: String,
    val isArchived: Boolean = false,
)
