package pk.vexel.financepassport.core.export

import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.database.TaxAnnualDraftEntity
import pk.vexel.financepassport.core.database.TaxMappingEntity
import pk.vexel.financepassport.core.database.WealthSnapshotEntity
import pk.vexel.financepassport.core.database.PositionSnapshotEntity

class DataExportTest {
    @Test fun jsonAndCsvContainCanonicalEventFields() {
        val snapshot = ExportSnapshot(emptyList(), listOf(FinancialEventEntity("1", "INCOME", 1, 100, "PKR", null, null, null, "Salary, June", null, "RELEVANT", null, 1, 1)), emptyList(), emptyList(), emptyList(), emptyList())
        val service = DataExportService()
        assertTrue(service.json(snapshot).contains("financialEvents"))
        assertTrue(service.csvEvents(snapshot).contains("\"Salary, June\""))
    }

    @Test fun jsonExportIncludesTaxMappingWealthSnapshotAndDraftLineage() {
        val snapshot = ExportSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            taxMappings = listOf(TaxMappingEntity("map1", "item1", "pk-structural-1", "OTHER_INCOME", "S1", "C1", "SYSTEM_GENERATED", null, null, 1)),
            wealthSnapshots = listOf(WealthSnapshotEntity("snap1", "year1", "OPENING", 1, 100, 200, 300, 400, -50, 750, 1)),
            taxDrafts = listOf(TaxAnnualDraftEntity("draft1", "year1", 2, "pk-structural-1", 1, "OPEN", 0)),
        )
        val json = DataExportService().json(snapshot)
        assertTrue(json.contains("\"taxMappings\""))
        assertTrue(json.contains("\"map1\""))
        assertTrue(json.contains("\"wealthSnapshots\""))
        assertTrue(json.contains("\"snap1\""))
        assertTrue(json.contains("\"taxDrafts\""))
        assertTrue(json.contains("\"draftVersion\":2"))
    }

    @Test fun jsonExportIncludesImmutablePositionSnapshots() {
        val snapshot = ExportSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            positionSnapshots = listOf(PositionSnapshotEntity("position-1", "MANUAL", 10, 1, 2, 3, 4, 5, 5, 1)),
        )
        assertTrue(DataExportService().json(snapshot).contains("\"position-1\""))
    }
}
