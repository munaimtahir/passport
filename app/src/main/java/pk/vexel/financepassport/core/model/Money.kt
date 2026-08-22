package pk.vexel.financepassport.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

@JvmInline
value class MinorUnits(val value: Long)

data class Money(val minorUnits: MinorUnits, val currency: String = "PKR") {
    init { require(currency.length == 3) { "Currency must be an ISO 4217 code" } }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = MinorUnits(Math.addExact(minorUnits.value, other.minorUnits.value)))
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = MinorUnits(Math.subtractExact(minorUnits.value, other.minorUnits.value)))
    }

    /** Multiplies by a decimal rate (e.g. a tax or budget-usage percentage), rounding half-up to the nearest minor unit. */
    operator fun times(rate: BigDecimal): Money {
        val result = BigDecimal(minorUnits.value).multiply(rate).setScale(0, RoundingMode.HALF_UP)
        require(result.abs() <= BigDecimal(Long.MAX_VALUE)) { "Money multiplication overflowed" }
        return copy(minorUnits = MinorUnits(result.toLong()))
    }

    companion object {
        fun pkr(rupees: Long) = Money(MinorUnits(Math.multiplyExact(rupees, 100)), "PKR")
        fun validateCurrency(code: String) { require(runCatching { Currency.getInstance(code) }.isSuccess) }
    }
}
