package pk.vexel.financepassport.core.model

import java.util.Currency

@JvmInline
value class MinorUnits(val value: Long)

data class Money(val minorUnits: MinorUnits, val currency: String = "PKR") {
    init { require(currency.length == 3) { "Currency must be an ISO 4217 code" } }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = MinorUnits(minorUnits.value + other.minorUnits.value))
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = MinorUnits(minorUnits.value - other.minorUnits.value))
    }

    companion object {
        fun pkr(rupees: Long) = Money(MinorUnits(rupees * 100), "PKR")
        fun validateCurrency(code: String) { require(runCatching { Currency.getInstance(code) }.isSuccess) }
    }
}
