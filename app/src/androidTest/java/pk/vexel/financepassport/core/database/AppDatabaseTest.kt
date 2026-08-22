package pk.vexel.financepassport.core.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun closeDatabase() = database.close()

    @Test fun transferLinkAndBothLedgerRowsCommitTogether() = runBlocking {
        val now = 1L
        val source = FinancialEventEntity("source", "TRANSFER", 1, -1000, "PKR", "a", null, "Move", null, "NOT_RELEVANT", null, now, now)
        val destination = FinancialEventEntity("destination", "TRANSFER", 1, 1000, "PKR", "b", null, "Move", null, "NOT_RELEVANT", null, now, now)
        database.withTransaction {
            database.financialEventDao().insertAll(listOf(source, destination))
            database.transferLinkDao().insert(TransferLinkEntity("link", source.id, destination.id, "group"))
        }
        assertEquals(source, database.financialEventDao().getById("source"))
        assertEquals(destination, database.financialEventDao().getById("destination"))
    }

    @Test
    fun duplicateTaxSourceIsRejectedByUniqueConstraint() {
        runBlocking {
            database.openHelper.writableDatabase.execSQL("INSERT INTO tax_years VALUES ('year','PK','2026',0,100,'rules-1','OPEN')")
            val item = TaxItemEntity("one", "year", "financial_event", "source", "BANK_PROFIT", 1, 100, null, "PKR", "Profit", "CAPTURED", "NONE", null, 1, 1)
            assertEquals(1L, database.taxItemDao().insertIfAbsent(item))
            assertEquals(-1L, database.taxItemDao().insertIfAbsent(item.copy(id = "two")))
            Unit
        }
    }

    @Test
    fun wealthLifecycleSupportsValuationDisposalAndPartialSettlement() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addAsset("Car", "VEHICLE", 2_000_000)
        val asset = database.wealthDao().getAllAssets().single()
        repository.updateAssetValuation(asset.id, 2_200_000)
        assertEquals(2_200_000L, database.wealthDao().getAssetById(asset.id)?.currentEstimatedValueMinor)
        repository.disposeAsset(asset.id, 2_100_000)
        assertEquals("ARCHIVED", database.wealthDao().getAssetById(asset.id)?.status)

        repository.addLiability("Loan", "PERSONAL", 1_000_000)
        val liability = database.wealthDao().getAllLiabilities().single()
        repository.recordLiabilityPayment(liability.id, 250_000)
        assertEquals(750_000L, database.wealthDao().getLiabilityById(liability.id)?.outstandingAmountMinor)

        repository.addReceivable("Advance", "Friend", 500_000)
        val receivable = database.receivableDao().getAll().single()
        repository.recordReceivablePayment(receivable.id, 125_000)
        assertEquals(375_000L, database.receivableDao().getById(receivable.id)?.outstandingAmountMinor)
    }

    @Test
    fun accountDetailsCanBeUpdatedAndArchivedWithoutRemovingLedgerHistory() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addAccount("Main", "CASH", 100_000)
        val account = database.accountDao().getAll().single()
        repository.updateAccount(account.id, "Renamed", 125_000)
        assertEquals("Renamed", database.accountDao().getById(account.id)?.name)
        database.financialEventDao().upsert(FinancialEventEntity("event", "EXPENSE", 1, 500, "PKR", account.id, null, "Lunch", null, "UNKNOWN", null, 1, 1))
        repository.archiveAccount(account.id)
        assertEquals("ARCHIVED", database.accountDao().getById(account.id)?.status)
        assertEquals(1, database.financialEventDao().getAll().size)
    }

    @Test
    fun manualTaxItemIsCapturedWithSourceLinkAndCanBeExcluded() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addManualTaxItem("BANK_PROFIT", 45_000, "Profit certificate")
        val item = database.taxItemDao().getAll().single()
        assertEquals("manual", item.sourceType)
        assertEquals(45_000L, item.grossAmountMinor)
        repository.updateTaxReview(item.id, "EXCLUDED", "Not applicable")
        assertEquals("EXCLUDED", database.taxItemDao().getAll().single().reviewState)
    }

    @Test
    fun taxReviewReclassifiesWithoutChangingSourceFactAndRequiresExclusionReason() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addManualTaxItem("OTHER_INCOME", 45_000, "Broker statement")
        val item = database.taxItemDao().getAll().single()
        repository.reviewTaxItem(item.id, "DIVIDEND", "REVIEWED", null)
        val reviewed = database.taxItemDao().getAll().single()
        assertEquals("DIVIDEND", reviewed.taxEventType)
        assertEquals("REVIEWED", reviewed.reviewState)
        assertEquals("Broker statement", reviewed.description)
        runCatching { repository.reviewTaxItem(item.id, "DIVIDEND", "EXCLUDED", null) }
            .onSuccess { error("An excluded item must require a reason") }
        repository.reviewTaxItem(item.id, "DIVIDEND", "EXCLUDED", "Not applicable")
        assertEquals("Not applicable", database.taxItemDao().getAll().single().exclusionReason)
    }

    @Test
    fun oneDocumentCanLinkToMultipleTaxItemsWithoutDuplicateLinks() = runBlocking {
        val repository = FinanceRepository(database)
        val document = DocumentEntity("doc", "Certificate", "Tax", "certificate.pdf", "application/pdf", 10, "/data/doc.enc", "hash", null, 1)
        database.documentDao().insert(document)
        repository.addManualTaxItem("BANK_PROFIT", 10_000, "Profit")
        repository.addManualTaxItem("TAX_WITHHELD", 2_000, "Withheld")
        val items = database.taxItemDao().getAll()
        repository.linkDocument(document.id, "tax_item", items[0].id)
        repository.linkDocument(document.id, "tax_item", items[1].id)
        repository.linkDocument(document.id, "tax_item", items[0].id)
        assertEquals(2, database.documentLinkDao().getForDocument(document.id).size)
        assertEquals("ATTACHED", database.taxItemDao().getAll().first { it.id == items[0].id }.evidenceState)
    }

    @Test
    fun deletingDocumentRemovesEncryptedFileMetadataAndLinks() = runBlocking {
        val repository = FinanceRepository(database)
        val file = java.io.File.createTempFile("passport-vault", ".enc")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val document = DocumentEntity("delete-doc", "Evidence", "Tax", "evidence.pdf", "application/pdf", 3, file.absolutePath, "hash-delete", null, 1)
        database.documentDao().insert(document)
        repository.addManualTaxItem("BANK_PROFIT", 10_000, "Profit")
        val taxItem = database.taxItemDao().getAll().single()
        repository.linkDocument(document.id, "tax_item", taxItem.id)
        repository.deleteDocument(document.id)
        assertEquals(0, database.documentDao().getAll().size)
        assertEquals(0, database.documentLinkDao().getAll().size)
        assertEquals(false, file.exists())
    }

    @Test
    fun annualDraftLinesRetainSourceIdsAndCalculations() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addManualTaxItem("BANK_PROFIT", 10_000, "Profit certificate")
        val source = database.taxItemDao().getAll().single()
        val draft = repository.prepareAnnualDraft()
        val lines = repository.getDraftLines(draft.id)
        assertEquals(1, lines.size)
        assertTrue(lines.single().sourceIdsJson.contains(source.sourceId))
        assertTrue(lines.single().calculation.isNotBlank())
        assertEquals(1, database.taxIssueDao().getForDraft(draft.id).size)
    }

    @Test
    fun recentEventQueryBoundsLargeHistoryDataset() = runBlocking {
        val events = (0 until 10_000).map { index -> FinancialEventEntity("event-$index", "ADJUSTMENT", index.toLong(), 1, "PKR", null, null, "Synthetic $index", null, "UNKNOWN", null, index.toLong(), index.toLong()) }
        events.chunked(500).forEach { database.financialEventDao().insertAll(it) }
        assertEquals(10_000, database.financialEventDao().getAll().size)
        assertEquals(100, database.financialEventDao().observeRecent(100).first().size)
    }

    @Test
    fun goalContributionAccumulatesAndMarksAchievedWithoutExceedingTarget() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addGoal("Emergency fund", 100_000)
        val goal = database.goalDao().getAll().single()
        repository.contributeToGoal(goal.id, 60_000)
        assertEquals(60_000L, database.goalDao().getById(goal.id)?.currentAmountMinor)
        assertEquals("OPEN", database.goalDao().getById(goal.id)?.status)
        repository.contributeToGoal(goal.id, 60_000)
        val achieved = database.goalDao().getById(goal.id)
        assertEquals(100_000L, achieved?.currentAmountMinor) // clamped at target, contribution overshoot is not stored
        assertEquals("ACHIEVED", achieved?.status)
    }

    @Test
    fun budgetStatusReflectsOnlyCurrentMonthNonDeletedExpensesForItsCategory() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addBudget("Food", 10_000)
        val today = java.time.LocalDate.now().toEpochDay()
        database.financialEventDao().insertAll(listOf(
            FinancialEventEntity("in-month", "EXPENSE", today, 9_000, "PKR", null, "Food", "Groceries", null, "UNKNOWN", null, 1, 1),
            FinancialEventEntity("wrong-category", "EXPENSE", today, 5_000, "PKR", null, "Fuel", "Petrol", null, "UNKNOWN", null, 1, 1),
        ))
        val statuses = repository.currentMonthBudgetStatuses.first()
        val food = statuses.single { it.category == "Food" }
        assertEquals(9_000L, food.spentMinor)
        assertTrue(food.isNearThreshold)
        assertTrue(!food.isOverBudget)
    }

    @Test
    fun accountMetadataPersistsInstitutionAndNotes() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addAccount("Salary account", "CURRENT", 50_000, institution = "Meezan Bank", notes = "Primary salary account")
        val account = database.accountDao().getAll().single()
        assertEquals("Meezan Bank", account.institution)
        assertEquals("Primary salary account", account.notes)
        repository.updateAccount(account.id, "Salary account", 50_000, institution = "HBL", notes = "Switched bank")
        val updated = database.accountDao().getById(account.id)
        assertEquals("HBL", updated?.institution)
        assertEquals("Switched bank", updated?.notes)
    }

    @Test
    fun investmentEventUsesProvidedAccountLabelInsteadOfAHardcodedConstant() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addInvestmentEvent("PSX Fund", "BUY", 100_000, 10, accountLabel = "AKD Securities")
        assertEquals("AKD Securities", database.investmentDao().getAll().single().investmentAccountId)
        repository.addInvestmentEvent("PSX Fund", "BUY", 50_000, 5)
        assertEquals("Manual", database.investmentDao().getAll().first { it.grossAmountMinor == 50_000L }.investmentAccountId)
    }

    @Test
    fun canonicalFinancialPositionCombinesAccountsWealthAndMonthlyActivity() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addAccount("Cash", "CASH", 100_000)
        val account = database.accountDao().getAll().single()
        val today = java.time.LocalDate.now().toEpochDay()
        database.financialEventDao().insertAll(listOf(
            FinancialEventEntity("salary", "INCOME", today, 30_000, "PKR", account.id, null, "Salary", null, "UNKNOWN", null, 1, 1),
            FinancialEventEntity("groceries", "EXPENSE", today, 5_000, "PKR", account.id, null, "Groceries", null, "UNKNOWN", null, 1, 1),
        ))
        repository.addAsset("Car", "VEHICLE", 800_000)
        repository.addLiability("Loan", "PERSONAL", 200_000)
        repository.addReceivable("Advance", "Friend", 50_000)

        val position = repository.financialPosition.first()
        assertEquals(100_000L + 25_000L, position.liquidFundsMinor)
        assertEquals(800_000L, position.assetsValueMinor)
        assertEquals(200_000L, position.liabilitiesValueMinor)
        assertEquals(50_000L, position.receivablesValueMinor)
        assertEquals(30_000L, position.monthlyIncomeMinor)
        assertEquals(5_000L, position.monthlyExpenseMinor)
        assertEquals(position.liquidFundsMinor + position.assetsValueMinor + position.receivablesValueMinor - position.liabilitiesValueMinor, position.netWorthMinor)
    }

    @Test
    fun accountBalanceAndEventCountUseDatabaseAggregates() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addAccount("Main", "CASH", 100_000)
        val account = database.accountDao().getAll().single()
        database.financialEventDao().insertAll(listOf(
            FinancialEventEntity("income", "INCOME", 1, 25_000, "PKR", account.id, null, "Salary", null, "UNKNOWN", null, 1, 1),
            FinancialEventEntity("expense", "EXPENSE", 1, 7_500, "PKR", account.id, null, "Lunch", null, "UNKNOWN", null, 1, 1),
            FinancialEventEntity("transfer-out", "TRANSFER", 1, -10_000, "PKR", account.id, null, "Move", null, "NOT_RELEVANT", null, 1, 1),
        ))
        assertEquals(7_500L, database.financialEventDao().observeAccountMovement(account.id).first())
        assertEquals(3, database.financialEventDao().observeActiveCount().first())
    }

    @Test
    fun manualTaxItemGetsASystemGeneratedMappingAndReclassificationSupersedesRatherThanReplaces() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addManualTaxItem("EMPLOYMENT_INCOME", 100_000, "Freelance salary")
        val item = database.taxItemDao().getAll().single()

        val initialMappings = database.taxMappingDao().getForTaxItem(item.id)
        assertEquals(1, initialMappings.size)
        val initialMapping = initialMappings.single()
        assertEquals("SYSTEM_GENERATED", initialMapping.source)
        assertEquals("EMPLOYMENT_INCOME", initialMapping.taxEventType)
        assertEquals(null, initialMapping.supersededByMappingId)

        repository.reviewTaxItem(item.id, "BANK_PROFIT", "REVIEWED", "Recharacterized as bank profit after reviewing the statement")

        assertEquals("Reclassification must not create a second TaxItemEntity for the same source", 1, database.taxItemDao().getAll().size)

        val mappingsAfterReview = database.taxMappingDao().getForTaxItem(item.id)
        assertEquals("Reclassification must add a mapping row, not overwrite the original", 2, mappingsAfterReview.size)
        val supersededOriginal = mappingsAfterReview.single { it.id == initialMapping.id }
        assertEquals(initialMapping.copy(supersededByMappingId = supersededOriginal.supersededByMappingId), supersededOriginal)
        assertTrue(supersededOriginal.supersededByMappingId != null)

        val activeMapping = database.taxMappingDao().getActiveForTaxItem(item.id)
        assertEquals(mappingsAfterReview.first { it.id == supersededOriginal.supersededByMappingId }, activeMapping)
        assertEquals("USER_OVERRIDE", activeMapping?.source)
        assertEquals("BANK_PROFIT", activeMapping?.taxEventType)
        assertEquals("Recharacterized as bank profit after reviewing the statement", activeMapping?.overrideReason)
    }
}
