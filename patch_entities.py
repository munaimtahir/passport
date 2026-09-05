import re

with open('app/src/main/java/pk/vexel/financepassport/core/database/Entities.kt', 'r') as f:
    content = f.read()

# 1. Add CategoryEntity
category_entity = """
@Entity(tableName = "categories", indices = [Index("family"), Index("status")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val family: String,
    val parentId: String?,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

"""
if "CategoryEntity" not in content:
    content = content.replace("data class FinancialContextEntity", category_entity + "data class FinancialContextEntity")

# 2. Update FinancialEventEntity
event_old = """    val updatedAtEpochMillis: Long,
    val incomeSourceId: String? = null,"""
event_new = """    val updatedAtEpochMillis: Long,
    val incomeSourceId: String? = null,
    val categoryId: String? = null,
    val counterparty: String? = null,
    val sourceTemplateId: String? = null,
    val sourceOccurrenceId: String? = null,
    val groupId: String? = null,"""
content = content.replace(event_old, event_new)

# 3. Replace RecurringItemEntity with RecurringTemplateEntity and ExpectedOccurrenceEntity
recurring_old = r"@Entity\(tableName = \"recurring_items\".*?anchorDayOfMonth: Int = 1,\n\)"
recurring_new = """@Entity(tableName = "recurring_templates", indices = [Index("status")])
data class RecurringTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val eventType: String,
    val amountMode: String,
    val expectedAmountMinor: Long?,
    val currency: String,
    val frequency: String,
    val intervalCount: Int,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long?,
    val defaultAccountId: String?,
    val defaultContextId: String?,
    val defaultCategoryId: String?,
    val counterparty: String?,
    val notes: String?,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "expected_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("status"), Index("dueDateEpochDay")]
)
data class ExpectedOccurrenceEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val dueDateEpochDay: Long,
    val expectedAmountMinor: Long?,
    val status: String,
    val confirmedEventId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)"""
content = re.sub(recurring_old, recurring_new, content, flags=re.DOTALL)

# 4. Update LiabilityEntity
liability_old = """    val status: String,
    val interestRateBps: Int? = null,
    val installmentAmountMinor: Long? = null,"""
liability_new = """    val status: String,
    val interestRateBps: Int? = null,
    val installmentAmountMinor: Long? = null,
    val contextId: String? = null,
    val notes: String? = null,
    val linkedAssetId: String? = null,"""
content = content.replace(liability_old, liability_new)

# 5. Update ReceivableEntity
receivable_old = """    val counterparty: String,
    val originalAmountMinor: Long,
    val outstandingAmountMinor: Long,
    val dueDateEpochDay: Long?,
    val status: String,"""
receivable_new = """    val counterparty: String,
    val originalAmountMinor: Long,
    val outstandingAmountMinor: Long,
    val dueDateEpochDay: Long?,
    val status: String,
    val receivableType: String = "OTHER",
    val contextId: String? = null,
    val notes: String? = null,
    val activityDateEpochDay: Long? = null,
    val receivedDateEpochDay: Long? = null,"""
content = content.replace(receivable_old, receivable_new)

# 6. Add SettlementEventEntity
settlement_entity = """
@Entity(tableName = "settlement_events", indices = [Index("entityId")])
data class SettlementEventEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val financialEventId: String,
    val principalAmountMinor: Long,
    val financingCostMinor: Long,
    val dateEpochDay: Long,
    val status: String,
)
"""
if "SettlementEventEntity" not in content:
    content = content.replace("data class ReceivableEntity", settlement_entity + "\n@Entity(tableName = \"receivables\", indices = [Index(\"status\")])\ndata class ReceivableEntity")

# 7. Replace InvestmentEventEntity with SimpleInvestmentEntity
investment_old = r"@Entity\(tableName = \"investment_events\".*?currency: String,\n\)"
investment_new = """@Entity(tableName = "simple_investments", indices = [Index("status")])
data class SimpleInvestmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val institution: String?,
    val contextId: String?,
    val acquisitionDateEpochDay: Long,
    val principalInvestedMinor: Long,
    val currentEstimatedValueMinor: Long,
    val currency: String,
    val maturityDateEpochDay: Long?,
    val notes: String?,
    val status: String,
)"""
content = re.sub(investment_old, investment_new, content, flags=re.DOTALL)

with open('app/src/main/java/pk/vexel/financepassport/core/database/Entities.kt', 'w') as f:
    f.write(content)
