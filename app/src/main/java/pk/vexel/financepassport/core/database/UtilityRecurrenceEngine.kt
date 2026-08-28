package pk.vexel.financepassport.core.database

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object UtilityRecurrenceEngine {

    fun calculateDates(year: Int, month: Int, issueDayAnchor: Int, dueDayAnchor: Int): Pair<LocalDate, LocalDate> {
        val issueYearMonth = YearMonth.of(year, month)
        val issueDay = issueDayAnchor.coerceAtMost(issueYearMonth.lengthOfMonth())
        val expectedIssueDate = LocalDate.of(year, month, issueDay)

        val expectedDueDate = if (dueDayAnchor >= issueDayAnchor) {
            val dueDay = dueDayAnchor.coerceAtMost(issueYearMonth.lengthOfMonth())
            LocalDate.of(year, month, dueDay)
        } else {
            // Due date belongs to the following calendar month
            val nextMonthYearMonth = issueYearMonth.plusMonths(1)
            val dueDay = dueDayAnchor.coerceAtMost(nextMonthYearMonth.lengthOfMonth())
            LocalDate.of(nextMonthYearMonth.year, nextMonthYearMonth.monthValue, dueDay)
        }

        return expectedIssueDate to expectedDueDate
    }

    fun deriveStatus(
        expectedIssueDate: LocalDate,
        expectedDueDate: LocalDate,
        isPaid: Boolean,
        isSkipped: Boolean,
        today: LocalDate,
        dueSoonDays: Long = 5
    ): String {
        if (isPaid) return "Paid"
        if (isSkipped) return "Skipped"
        if (today.isBefore(expectedIssueDate)) return "Expected"
        if (today.isAfter(expectedDueDate)) return "Overdue"
        val dueSoonStart = expectedDueDate.minusDays(dueSoonDays)
        if (!today.isBefore(dueSoonStart) && !today.isAfter(expectedDueDate)) return "Due soon"
        return "Pending"
    }

    suspend fun reconcileProfile(
        db: AppDatabase,
        profile: UtilityBillProfileEntity,
        today: LocalDate
    ) {
        val startYearMonth = runCatching { YearMonth.parse(profile.recurrenceStartMonth) }
            .getOrElse { YearMonth.from(today) }
        val endYearMonth = YearMonth.from(today)

        if (profile.status == "ARCHIVED") {
            // Archiving stops future generation. We only reconcile existing occurrences up to the archive date,
            // or just skip future ones. Let's just generate occurrences from startYearMonth up to the archived month
            // or today, whichever is earlier. For simplicity, archiving stops future generation from the time it was archived.
            // But we still preserve all history.
        }

        var current = startYearMonth
        while (!current.isAfter(endYearMonth)) {
            val year = current.year
            val month = current.monthValue

            // Check if profile is active or if current month is not after the profile's updated date if archived
            val isArchived = profile.status == "ARCHIVED"
            val archiveLimit = if (isArchived) {
                // If archived, do not generate occurrences for months after the archive month
                val archiveDate = java.time.Instant.ofEpochMilli(profile.updatedAtEpochMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                YearMonth.from(archiveDate)
            } else null

            if (archiveLimit != null && current.isAfter(archiveLimit)) {
                break
            }

            val existing = db.monthlyBillOccurrenceDao().getForMonth(profile.id, year, month)
            if (existing == null) {
                val (issueDate, dueDate) = calculateDates(year, month, profile.issueDayAnchor, profile.dueDayAnchor)
                val now = System.currentTimeMillis()
                
                // Determine initial status based on today
                val initialStatus = deriveStatus(
                    expectedIssueDate = issueDate,
                    expectedDueDate = dueDate,
                    isPaid = false,
                    isSkipped = false,
                    today = today
                )

                val newOccurrence = MonthlyBillOccurrenceEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = profile.id,
                    billingYear = year,
                    billingMonth = month,
                    expectedIssueDateEpochDay = issueDate.toEpochDay(),
                    expectedDueDateEpochDay = dueDate.toEpochDay(),
                    actualIssueDateEpochDay = null,
                    actualDueDateEpochDay = null,
                    amountMinor = null,
                    status = initialStatus,
                    notes = null,
                    creationSource = "Automatic",
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                )
                db.monthlyBillOccurrenceDao().upsert(newOccurrence)
            }
            current = current.plusMonths(1)
        }

        // Reconcile status of all occurrences for this profile
        val occurrences = db.monthlyBillOccurrenceDao().getByProfile(profile.id)
        for (occ in occurrences) {
            val payment = db.paymentRecordDao().getForOccurrence(occ.id)
            val isPaid = payment != null
            val isSkipped = occ.status == "Skipped"
            
            val expectedIssueDate = LocalDate.ofEpochDay(occ.expectedIssueDateEpochDay)
            val expectedDueDate = LocalDate.ofEpochDay(occ.expectedDueDateEpochDay)

            val correctStatus = deriveStatus(
                expectedIssueDate = expectedIssueDate,
                expectedDueDate = expectedDueDate,
                isPaid = isPaid,
                isSkipped = isSkipped,
                today = today
            )

            if (occ.status != correctStatus) {
                db.monthlyBillOccurrenceDao().update(
                    occ.copy(
                        status = correctStatus,
                        updatedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun reconcileAll(db: AppDatabase, today: LocalDate = LocalDate.now()) {
        val profiles = db.utilityBillDao().getAll()
        for (profile in profiles) {
            reconcileProfile(db, profile, today)
        }
    }
}
