package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun arithmeticUsesExactMinorUnits() {
        assertEquals(Money.pkr(150), Money.pkr(100) + Money.pkr(50))
        assertEquals(Money.pkr(50), Money.pkr(100) - Money.pkr(50))
    }

    @Test(expected = IllegalArgumentException::class)
    fun arithmeticRejectsCurrencyMismatch() {
        Money(MinorUnits(100), "PKR") + Money(MinorUnits(100), "USD")
    }
}
