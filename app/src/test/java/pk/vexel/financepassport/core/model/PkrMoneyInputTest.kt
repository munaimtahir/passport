package pk.vexel.financepassport.core.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows

class PkrMoneyInputTest {
    @Test fun parsesWholeRupeesAndOptionalGrouping() {
        assertEquals(50_000L, PkrMoneyInput.toMinorUnits("500"))
        assertEquals(150_000L, PkrMoneyInput.toMinorUnits("1,500"))
        assertEquals("1,500,000", PkrMoneyInput.groupedInput("1500000"))
    }

    @Test fun rejectsDecimalsMalformedGroupingAndOverflow() {
        listOf("1.5", "1500.50", "1,50", "-1", "abc").forEach {
            assertThrows(IllegalArgumentException::class.java) { PkrMoneyInput.parseRupees(it) }
        }
        assertThrows(ArithmeticException::class.java) { PkrMoneyInput.toMinorUnits(Long.MAX_VALUE.toString()) }
    }

    @Test fun preservesFractionalLegacyMinorUnitsAndSign() {
        assertEquals("PKR 1,500.50", PkrMoneyInput.formatMinorUnits(150050))
        assertEquals("-PKR 1,500.50", PkrMoneyInput.formatMinorUnits(-150050))
    }
}
