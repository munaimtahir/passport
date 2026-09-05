package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarProjectionTest {
    @Test
    fun projectionIsStableAndSortedWithoutDuplicates() {
        val source = CalendarProjectionSource("BILL", "bill-1", "Electricity", 10)
        val result = calendarProjection(listOf(source, source, CalendarProjectionSource("DOC", "doc-1", "Passport", 5)), 7)
        assertEquals(listOf("doc-1", "bill-1"), result.map { it.sourceId })
        assertEquals("source-BILL-bill-1", source.stableId)
    }

    @Test
    fun overdueRequiresPastDueDateAndUnresolvedSource() {
        assertTrue(CalendarProjectionSource("BILL", "1", "Bill", 9).isOverdue(10))
        assertTrue(!CalendarProjectionSource("BILL", "2", "Bill", 10).isOverdue(10))
        assertTrue(!CalendarProjectionSource("BILL", "3", "Bill", 9, resolved = true).isOverdue(10))
    }
}
