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
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(
        context,
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

        repository.addReceivable(context, "Advance", "Friend", 500_000)
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
    fun addGoalPersistsRealGoalTypeAndTargetDateInsteadOfHardcodedDefaults() = runBlocking {
        val repository = FinanceRepository(database)
        val targetDate = 20000L
        repository.addGoal("New car", 500_000, "PURCHASE", targetDate)
        val goal = database.goalDao().getAll().single()
        assertEquals("PURCHASE", goal.goalType)
        assertEquals(targetDate, goal.targetDateEpochDay)
        // Omitting the new params still falls back to the pre-Sprint-18 defaults, not a crash.
        repository.addGoal("Untyped goal", 10_000)
        val untyped = database.goalDao().getAll().first { it.title == "Untyped goal" }
        assertEquals("CUSTOM", untyped.goalType)
        assertEquals(null, untyped.targetDateEpochDay)
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
        repository.addReceivable(context, "Advance", "Friend", 50_000)

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

    @Test
    fun regeneratingAnnualDraftCreatesNewVersionWithoutDeletingPriorLines() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addManualTaxItem("BANK_PROFIT", 10_000, "First profit certificate")
        val firstDraft = repository.prepareAnnualDraft(2026)
        assertEquals(1, firstDraft.draftVersion)
        val firstLines = repository.getDraftLines(firstDraft.id)
        assertEquals(1, firstLines.size)

        repository.addManualTaxItem("DIVIDEND", 5_000, "Dividend received")
        val secondDraft = repository.prepareAnnualDraft(2026)
        assertEquals("Regeneration must increment the draft version, not overwrite it", 2, secondDraft.draftVersion)
        assertTrue(secondDraft.id != firstDraft.id)

        // The prior draft and its lines must still exist, unmutated, after regeneration.
        val firstLinesAfterRegeneration = repository.getDraftLines(firstDraft.id)
        assertEquals(firstLines, firstLinesAfterRegeneration)
        val secondLines = repository.getDraftLines(secondDraft.id)
        assertEquals(2, secondLines.size)
        assertEquals(2, database.taxDraftDao().observeDrafts().first().count { it.taxYearId == "PK-2026" })
    }

    /**
     * Phase 11: a tax year created under an older ruleset version must keep generating drafts
     * under that same version even after a newer version becomes "current" — the version column
     * on `tax_years` is the source of truth for that year, not whatever `BundledTaxRulesets`
     * currently defaults to. Directly seeds a `tax_years` row on the older version (pre-dating
     * this change would have created it that way) rather than relying on `ensureTaxYearExists`,
     * which always uses the current default.
     */
    @Test
    fun taxYearKeepsUsingItsOwnStoredRulesetVersionAfterANewerVersionBecomesDefault() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES ('PK-2019', 'PK', '2019', 0, 365, 'pk-structural-1', 'OPEN')",
        )
        val repository = FinanceRepository(database)
        // DONATION only has a rule in pk-structural-2 — under the year's own pk-structural-1,
        // this must come back unmapped (NO_RULE), proving v1's rules were actually used.
        repository.addManualTaxItem("DONATION", 5_000, "Zakat receipt", java.time.LocalDate.of(2019, 6, 1))

        val draft = repository.prepareAnnualDraft(2019)
        assertEquals("Draft for a v1 tax year must record v1 as its ruleset version, not today's default", "pk-structural-1", draft.rulesetVersion)
        assertTrue("DONATION has no rule in pk-structural-1, so this must surface as an issue, not a mapped line", draft.issueCount > 0)
        assertTrue(repository.getDraftLines(draft.id).none { it.categoryCode == "CHARITABLE_DONATION" })

        val mapping = database.taxMappingDao().getForTaxItem(database.taxItemDao().getAll().single { it.taxYearId == "PK-2019" }.id).single()
        assertEquals("pk-structural-1", mapping.rulesetVersion)
        assertEquals("UNMAPPED", mapping.sectionCode)

        // A brand-new tax year (no pre-existing row) picks up the current default (v2), where
        // DONATION does map — same source data, different year, genuinely different outcome.
        repository.addManualTaxItem("DONATION", 5_000, "Zakat receipt 2027", java.time.LocalDate.of(2027, 6, 1))
        val newDraft = repository.prepareAnnualDraft(2027)
        assertEquals(pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.CURRENT_VERSION, newDraft.rulesetVersion)
        assertTrue(repository.getDraftLines(newDraft.id).any { it.categoryCode == "CHARITABLE_DONATION" })
    }

    /**
     * Phase 11: tax_years.status previously only ever got set to "OPEN" at creation — no code
     * path ever moved it. Proves the real OPEN -> UNDER_REVIEW -> FILED progression, the reopen
     * escape hatch from either later state back to OPEN, and that an invalid jump (e.g. OPEN
     * straight to FILED) is rejected rather than silently allowed.
     */
    @Test
    fun taxYearStatusFollowsTheOpenReviewFiledLifecycle() = runBlocking {
        val repository = FinanceRepository(database)
        repository.prepareAnnualDraft(2028) // creates the PK-2028 tax_years row, status OPEN
        assertEquals("OPEN", database.taxYearDao().getById("PK-2028")?.status)

        val invalidJump = runCatching { repository.updateTaxYearStatus("PK-2028", "FILED") }.exceptionOrNull()
        assertTrue("OPEN -> FILED directly must be rejected", invalidJump is IllegalArgumentException)
        assertEquals("OPEN", database.taxYearDao().getById("PK-2028")?.status)

        repository.updateTaxYearStatus("PK-2028", "UNDER_REVIEW")
        assertEquals("UNDER_REVIEW", database.taxYearDao().getById("PK-2028")?.status)

        repository.updateTaxYearStatus("PK-2028", "OPEN") // reopen from review
        assertEquals("OPEN", database.taxYearDao().getById("PK-2028")?.status)

        repository.updateTaxYearStatus("PK-2028", "UNDER_REVIEW")
        repository.updateTaxYearStatus("PK-2028", "FILED")
        assertEquals("FILED", database.taxYearDao().getById("PK-2028")?.status)

        repository.updateTaxYearStatus("PK-2028", "OPEN") // reopen from filed
        assertEquals("OPEN", database.taxYearDao().getById("PK-2028")?.status)
    }

    /**
     * Phase 11: receivable due dates never wired to a reminder before — ReceivableEntity's
     * dueDateEpochDay column existed but nothing ever set it. Proves a due date on a new
     * receivable creates a real, correctly-kinded calendar reminder, and that clearing it (a
     * second receivable added with no due date) does not create a stray entry.
     */
    @Test
    fun receivableDueDateSchedulesACalendarReminder() = runBlocking {
        val repository = FinanceRepository(database)
        repository.addReceivable(context, "Advance", "Friend", 500_000, java.time.LocalDate.of(2027, 3, 1))
        val open = database.calendarDao().observeOpen().first()
        val reminder = open.single { it.kind == "RECEIVABLE_DUE" }
        assertEquals("Advance is due", reminder.title)
        val receivableId = database.receivableDao().getAll().single { it.title == "Advance" }.id
        assertEquals(receivableId, reminder.linkedEntityId)
        assertEquals(java.time.LocalDate.of(2027, 3, 1).toEpochDay(), receivableId.let { database.receivableDao().getById(it)?.dueDateEpochDay })

        repository.addReceivable(context, "No due date", "Other friend", 10_000)
        assertEquals(1, database.calendarDao().observeOpen().first().count { it.kind == "RECEIVABLE_DUE" })
    }

    /**
     * Phase 11: filing-deadline reminders are independent of tax-year status transitions — a user
     * can set one at any point, and clearing it (passing null) cancels the calendar entry rather
     * than leaving a stale reminder behind.
     */
    @Test
    fun taxFilingDeadlineReminderCanBeSetAndCleared() = runBlocking {
        val repository = FinanceRepository(database)
        repository.prepareAnnualDraft(2028) // creates the PK-2028 tax_years row
        repository.scheduleTaxFilingDeadlineReminder(context, "PK-2028", java.time.LocalDate.of(2028, 9, 30))
        var open = database.calendarDao().observeOpen().first()
        assertEquals(1, open.count { it.kind == "TAX_FILING_DEADLINE" && it.linkedEntityId == "PK-2028" })

        repository.scheduleTaxFilingDeadlineReminder(context, "PK-2028", null)
        open = database.calendarDao().observeOpen().first()
        assertEquals(0, open.count { it.kind == "TAX_FILING_DEADLINE" })
    }

    @Test
    fun reconciliationRequiresAnOpeningSnapshotBeforeRunning() = runBlocking {
        val repository = FinanceRepository(database)
        repository.prepareAnnualDraft(2027) // creates the PK-2027 tax_years row
        val failure = runCatching { repository.calculateReconciliation("PK-2027") }.exceptionOrNull()
        assertTrue("Reconciling without a recorded opening snapshot must fail loudly, not silently assume zero", failure != null)
    }

    @Test
    fun reconciliationUsesRecordedOpeningSnapshotAndScopesEventsToTheTaxYear() = runBlocking {
        val repository = FinanceRepository(database)
        val account = database.accountDao().getAll().let { existing ->
            repository.addAccount("Main", "SAVINGS", 0)
            database.accountDao().getAll().first { it !in existing }
        }
        repository.prepareAnnualDraft(2026) // ensures the PK-2026 tax_years row (Jan 1 - Dec 31 2026) exists

        // Snapshot opening wealth before any of this year's events are recorded, so it is
        // genuinely the wealth position at the start of the year, not a live-recomputed figure.
        repository.recordWealthSnapshot(2026, "OPENING", java.time.LocalDate.of(2026, 1, 1))
        val opening = database.wealthSnapshotDao().get("PK-2026", "OPENING")!!
        assertEquals(0L, opening.netWealthMinor)

        // An event dated before the tax year must not be counted in this year's inflows/expenditure.
        repository.addEvent(pk.vexel.financepassport.core.model.FinancialEventType.INCOME, 999_999, account.id, "Prior year income", date = java.time.LocalDate.of(2025, 12, 31))
        // Events inside the tax year.
        repository.addEvent(pk.vexel.financepassport.core.model.FinancialEventType.INCOME, 100_000, account.id, "Salary", date = java.time.LocalDate.of(2026, 6, 1))
        repository.addEvent(pk.vexel.financepassport.core.model.FinancialEventType.EXPENSE, 40_000, account.id, "Rent", date = java.time.LocalDate.of(2026, 6, 2))

        val result = repository.calculateReconciliation("PK-2026")
        // expected = opening(0) + this-year income(100000) - this-year expense(40000) = 60000;
        // the 999999 prior-year event must be excluded even though it is in the same account history.
        assertEquals(60_000L, result.expectedClosing.minorUnits.value)

        val stored = database.reconciliationDao().observeAll().first().first { it.taxYearId == "PK-2026" }
        assertEquals(0L, stored.openingWealthMinor)
        assertEquals(100_000L, stored.inflowsMinor)
        assertEquals(40_000L, stored.expenditureMinor)
    }
}
