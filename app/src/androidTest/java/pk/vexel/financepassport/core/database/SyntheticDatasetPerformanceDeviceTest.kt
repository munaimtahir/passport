package pk.vexel.financepassport.core.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 10 performance item: no synthetic large dataset existed anywhere in this project (the
 * backup-equivalence and demo-data gaps note the same absence). This inserts a four-figure
 * financial-event and tax-item dataset directly via Room into a fresh on-disk database (matching
 * the real app's storage engine, not an in-memory shortcut) and times list load, annual draft
 * generation and reconciliation against it, on a real device/emulator process.
 */
@RunWith(AndroidJUnit4::class)
class SyntheticDatasetPerformanceDeviceTest {
    @Test
    fun repositoryRemainsResponsiveWithFourFigureDataset() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "perf-${UUID.randomUUID()}.db"
            val database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val repository = FinanceRepository(database)

            val account = AccountEntity(UUID.randomUUID().toString(), "Perf account", null, "CASH", null, null, "PKR", 0, LocalDate.now().toEpochDay(), "ACTIVE", null, 1, 1)
            database.accountDao().upsert(account)

            val eventCount = 2000
            val taxItemCount = 500
            val today = LocalDate.now()

            val insertEventsMs = System.currentTimeMillis().let { start ->
                database.withTransaction {
                    repeat(eventCount) { i ->
                        val date = today.minusDays((i % 700).toLong())
                        database.financialEventDao().upsert(
                            FinancialEventEntity(
                                UUID.randomUUID().toString(),
                                if (i % 3 == 0) "INCOME" else "EXPENSE",
                                date.toEpochDay(),
                                1000L + i,
                                "PKR",
                                account.id,
                                null,
                                "Category ${i % 12}",
                                "Synthetic event $i",
                                null,
                                "NEUTRAL",
                                null,
                                1,
                                1,
                            )
                        )
                    }
                }
                System.currentTimeMillis() - start
            }

            val insertTaxItemsMs = System.currentTimeMillis().let { start ->
                repeat(taxItemCount) { i -> repository.addManualTaxItem("OTHER_INCOME", 500L + i, "Synthetic tax item $i") }
                System.currentTimeMillis() - start
            }

            val listLoadMs = System.currentTimeMillis().let { start ->
                val all = database.financialEventDao().getAll()
                assertTrue("expected $eventCount events, got ${all.size}", all.size >= eventCount)
                System.currentTimeMillis() - start
            }

            val draftMs = System.currentTimeMillis().let { start ->
                repository.prepareAnnualDraft(today.year)
                System.currentTimeMillis() - start
            }

            val readiness = System.currentTimeMillis().let { start ->
                val items = database.taxItemDao().getAll()
                pk.vexel.financepassport.core.model.calculateTaxReadiness(items)
                System.currentTimeMillis() - start
            }

            android.util.Log.i(
                "SyntheticDatasetPerf",
                "events=$eventCount insertEventsMs=$insertEventsMs taxItems=$taxItemCount insertTaxItemsMs=$insertTaxItemsMs " +
                    "listLoadMs=$listLoadMs draftGenerationMs=$draftMs taxReadinessMs=$readiness",
            )

            // Generous ceilings: this asserts "stays responsive" (no runaway/quadratic blowup), not a tight perf budget.
            assertTrue("list load took ${listLoadMs}ms, expected well under 5s", listLoadMs < 5_000)
            assertTrue("draft generation took ${draftMs}ms, expected well under 15s", draftMs < 15_000)

            database.close()
            context.getDatabasePath(name).delete()
            java.io.File(context.getDatabasePath(name).path + "-wal").delete()
            java.io.File(context.getDatabasePath(name).path + "-shm").delete()
        }
    }
}
