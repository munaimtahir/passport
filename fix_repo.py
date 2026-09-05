import re

with open('app/src/main/java/pk/vexel/financepassport/core/database/FinanceRepository.kt', 'r') as f:
    content = f.read()

# Fix db.simpleInvestmentDao().insert
old_inv = r"suspend fun addInvestment\([^\{]+\{[^\}]+\}"
new_inv = """suspend fun addInvestment(accountId: String?, title: String, type: String, institution: String?, contextId: String?, principalInvestedMinor: Long, currentEstimatedValueMinor: Long, currency: String, maturityDateEpochDay: Long?, notes: String?) {
        db.simpleInvestmentDao().upsert(SimpleInvestmentEntity(
            id = java.util.UUID.randomUUID().toString(),
            title = title.trim(),
            type = type,
            institution = institution,
            contextId = contextId,
            acquisitionDateEpochDay = java.time.LocalDate.now().toEpochDay(),
            principalInvestedMinor = principalInvestedMinor,
            currentEstimatedValueMinor = currentEstimatedValueMinor,
            currency = currency,
            maturityDateEpochDay = maturityDateEpochDay,
            notes = notes,
            status = "ACTIVE"
        ))
    }"""
content = re.sub(old_inv, new_inv, content)

# Fix addRecurringItem
old_rec = r"suspend fun addRecurringItem\([^\{]+\{[^\}]+\}"
new_rec = """suspend fun addRecurringTemplate(
        title: String,
        eventType: FinancialEventType,
        amountMode: String,
        expectedAmountMinor: Long?,
        frequency: String,
        intervalCount: Int,
        startDateEpochDay: Long,
        endDateEpochDay: Long?,
        defaultAccountId: String?,
        defaultContextId: String?,
        defaultCategoryId: String?,
        counterparty: String?,
        notes: String?
    ) {
        val now = java.time.Instant.now().toEpochMilli()
        val id = java.util.UUID.randomUUID().toString()
        db.recurringTemplateDao().upsert(
            RecurringTemplateEntity(
                id = id,
                title = title.trim(),
                eventType = eventType.name,
                amountMode = amountMode,
                expectedAmountMinor = expectedAmountMinor,
                currency = "PKR",
                frequency = frequency,
                intervalCount = intervalCount,
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                defaultAccountId = defaultAccountId,
                defaultContextId = defaultContextId,
                defaultCategoryId = defaultCategoryId,
                counterparty = counterparty,
                notes = notes,
                status = "ACTIVE",
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        )
    }"""
content = re.sub(old_rec, new_rec, content)

with open('app/src/main/java/pk/vexel/financepassport/core/database/FinanceRepository.kt', 'w') as f:
    f.write(content)
