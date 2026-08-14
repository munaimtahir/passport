package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    @Test fun arithmeticUsesExactMinorUnits() {
        assertEquals(Money.pkr(150), Money.pkr(100) + Money.pkr(50))
        assertEquals(Money.pkr(50), Money.pkr(100) - Money.pkr(50))
    }

    @Test(expected = IllegalArgumentException::class)
    fun arithmeticRejectsCurrencyMismatch() {
        Money(MinorUnits(100), "PKR") + Money(MinorUnits(100), "USD")
    }

    @Test(expected = ArithmeticException::class)
    fun additionRejectsOverflow() {
        Money(MinorUnits(Long.MAX_VALUE)) + Money(MinorUnits(1))
    }

    @Test(expected = ArithmeticException::class)
    fun subtractionRejectsUnderflow() {
        Money(MinorUnits(Long.MIN_VALUE)) - Money(MinorUnits(1))
    }

    @Test fun timesRoundsHalfUpToNearestMinorUnit() {
        assertEquals(MinorUnits(35), (Money(MinorUnits(100)) * BigDecimal("0.345")).minorUnits)
        assertEquals(MinorUnits(34), (Money(MinorUnits(100)) * BigDecimal("0.344")).minorUnits)
    }
}
