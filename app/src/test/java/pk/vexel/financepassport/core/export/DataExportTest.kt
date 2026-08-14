package pk.vexel.financepassport.core.export

import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.database.FinancialEventEntity

class DataExportTest {
    @Test fun jsonAndCsvContainCanonicalEventFields() {
        val snapshot = ExportSnapshot(emptyList(), listOf(FinancialEventEntity("1", "INCOME", 1, 100, "PKR", null, null, "Salary, June", null, "RELEVANT", null, 1, 1)), emptyList(), emptyList(), emptyList(), emptyList())
        val service = DataExportService()
        assertTrue(service.json(snapshot).contains("financialEvents"))
        assertTrue(service.csvEvents(snapshot).contains("\"Salary, June\""))
    }
}
