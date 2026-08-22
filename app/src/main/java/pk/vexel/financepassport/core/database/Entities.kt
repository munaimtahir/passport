package pk.vexel.financepassport.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val baseCurrency: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "accounts", indices = [Index("status")])
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String?,
    val accountType: String,
    val maskedIdentifier: String?,
    val encryptedSensitiveIdentifier: ByteArray?,
    val currency: String,
    val openingBalanceMinor: Long,
    val openingBalanceDateEpochDay: Long,
    val status: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "financial_events", indices = [Index("dateEpochDay"), Index("accountId")])
data class FinancialEventEntity(
    @PrimaryKey val id: String,
    val eventType: String,
    val dateEpochDay: Long,
    val amountMinor: Long,
    val currency: String,
    val accountId: String?,
    val category: String?,
    val description: String,
    val notes: String?,
    val taxRelevance: String,
    val deletedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "assets", indices = [Index("status")])
data class AssetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val acquisitionDateEpochDay: Long,
    val acquisitionCostMinor: Long,
    val currentEstimatedValueMinor: Long,
    val currency: String,
    val ownershipPercent: Int,
    val disposalDateEpochDay: Long?,
    val disposalValueMinor: Long?,
    val status: String,
)

@Entity(tableName = "liabilities", indices = [Index("status")])
data class LiabilityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val lender: String?,
    val originalAmountMinor: Long,
    val outstandingAmountMinor: Long,
    val currency: String,
    val startDateEpochDay: Long,
    val dueDateEpochDay: Long?,
    val status: String,
)

@Entity(tableName = "investment_events", indices = [Index("dateEpochDay"), Index("investmentAccountId")])
data class InvestmentEventEntity(
    @PrimaryKey val id: String,
    val investmentAccountId: String,
    val securityName: String,
    val type: String,
    val dateEpochDay: Long,
    val quantityMinor: Long?,
    val grossAmountMinor: Long,
    val feesMinor: Long,
    val taxWithheldMinor: Long,
    val currency: String,
)

@Entity(tableName = "receivables", indices = [Index("status")])
data class ReceivableEntity(
    @PrimaryKey val id: String,
    val title: String,
    val counterparty: String,
    val originalAmountMinor: Long,
    val outstandingAmountMinor: Long,
    val dueDateEpochDay: Long?,
    val status: String,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val goalType: String,
    val targetAmountMinor: Long,
    val targetDateEpochDay: Long?,
    val status: String,
    val currentAmountMinor: Long = 0,
)

@Entity(tableName = "recurring_items", indices = [Index("status"), Index("nextDueDateEpochDay")])
data class RecurringItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val eventType: String,
    val amountMinor: Long,
    val currency: String,
    val accountId: String,
    val category: String?,
    val frequency: String,
    val nextDueDateEpochDay: Long,
    val status: String,
    val autoCreateDraft: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    /** Day-of-month the schedule is anchored to; used to clamp month-end rollover without permanent drift. */
    val anchorDayOfMonth: Int = 1,
)

@Entity(tableName = "budgets", indices = [Index("category", unique = true), Index("status")])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val monthlyLimitMinor: Long,
    val currency: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "transfer_links",
    foreignKeys = [
        ForeignKey(entity = FinancialEventEntity::class, parentColumns = ["id"], childColumns = ["sourceEventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FinancialEventEntity::class, parentColumns = ["id"], childColumns = ["destinationEventId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sourceEventId", unique = true), Index("destinationEventId", unique = true)],
)
data class TransferLinkEntity(
    @PrimaryKey val id: String,
    val sourceEventId: String,
    val destinationEventId: String,
    val transferGroupId: String,
)

@Entity(tableName = "tax_years", indices = [Index(value = ["jurisdictionCode", "yearLabel"], unique = true)])
data class TaxYearEntity(
    @PrimaryKey val id: String,
    val jurisdictionCode: String,
    val yearLabel: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val rulesetVersion: String,
    val status: String,
)

@Entity(
    tableName = "tax_items",
    foreignKeys = [ForeignKey(entity = TaxYearEntity::class, parentColumns = ["id"], childColumns = ["taxYearId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("taxYearId"), Index(value = ["sourceType", "sourceId"], unique = true)],
)
data class TaxItemEntity(
    @PrimaryKey val id: String,
    val taxYearId: String,
    val sourceType: String,
    val sourceId: String,
    val taxEventType: String,
    val dateEpochDay: Long,
    val grossAmountMinor: Long?,
    val taxWithheldMinor: Long?,
    val currency: String,
    val description: String,
    val reviewState: String,
    val evidenceState: String,
    val exclusionReason: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/**
 * Persisted tax-mapping lineage (mega-prompt Phase 4F): each row is one classification of a
 * [TaxItemEntity] at a point in time. Reclassifying never mutates or deletes a prior row — it
 * inserts a new one and sets the prior row's [supersededByMappingId], so historical treatment
 * under an earlier ruleset version remains reproducible.
 */
@Entity(
    tableName = "tax_mappings",
    foreignKeys = [ForeignKey(entity = TaxItemEntity::class, parentColumns = ["id"], childColumns = ["taxItemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taxItemId"), Index("supersededByMappingId")],
)
data class TaxMappingEntity(
    @PrimaryKey val id: String,
    val taxItemId: String,
    val rulesetVersion: String,
    val taxEventType: String,
    val sectionCode: String,
    val categoryCode: String,
    /** "SYSTEM_GENERATED" for the ruleset's own classification, "USER_OVERRIDE" for a manual reclassification. */
    val source: String,
    val overrideReason: String?,
    /** Null while this is the active/current mapping for its tax item; set to the superseding row's id once replaced. */
    val supersededByMappingId: String?,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "tax_annual_drafts", indices = [Index("taxYearId")])
data class TaxAnnualDraftEntity(
    @PrimaryKey val id: String,
    val taxYearId: String,
    val draftVersion: Int,
    val rulesetVersion: String,
    val generatedAtEpochMillis: Long,
    val status: String,
    val issueCount: Int,
)

@Entity(tableName = "tax_draft_lines", indices = [Index("draftId")])
data class TaxDraftLineEntity(
    @PrimaryKey val id: String,
    val draftId: String,
    val sectionCode: String,
    val categoryCode: String,
    val amountMinor: Long,
    val currency: String,
    val sourceIdsJson: String,
    val calculation: String,
)

@Entity(tableName = "tax_issues", indices = [Index("draftId"), Index("sourceId")])
data class TaxIssueEntity(
    @PrimaryKey val id: String,
    val draftId: String,
    val code: String,
    val title: String,
    val explanation: String,
    val sourceId: String?,
    val status: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "wealth_reconciliations", indices = [Index("taxYearId")])
data class WealthReconciliationEntity(
    @PrimaryKey val id: String,
    val taxYearId: String,
    val openingWealthMinor: Long,
    val inflowsMinor: Long,
    val expenditureMinor: Long,
    val outflowsMinor: Long,
    val adjustmentsMinor: Long,
    val expectedClosingMinor: Long,
    val recordedClosingMinor: Long,
    val unexplainedDifferenceMinor: Long,
    val calculation: String,
)

@Entity(tableName = "calendar_items", indices = [Index("dueAtEpochMillis"), Index("status")])
data class CalendarItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val dueAtEpochMillis: Long,
    val linkedEntityId: String?,
    val status: String,
    val reminderMinutesBefore: Long,
)

@Entity(tableName = "documents", indices = [Index("sha256", unique = true)])
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val originalFilename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localEncryptedPath: String,
    val sha256: String,
    val expiryDateEpochDay: Long?,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "document_links", indices = [Index(value = ["documentId", "entityType", "entityId"], unique = true)])
data class DocumentLinkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val entityType: String,
    val entityId: String,
    val purpose: String,
)

@Entity(tableName = "official_records", indices = [Index("recordType"), Index("expiryDateEpochDay")])
data class OfficialRecordEntity(
    @PrimaryKey val id: String,
    val recordType: String,
    val title: String,
    val maskedIdentifier: String?,
    val encryptedIdentifier: ByteArray?,
    val issueDateEpochDay: Long?,
    val expiryDateEpochDay: Long?,
    val linkedDocumentId: String?,
    val status: String,
)

@Entity(tableName = "change_log", indices = [Index("entityId"), Index("timestampEpochMillis")])
data class ChangeLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val timestampEpochMillis: Long,
    val changedFieldsJson: String,
    val source: String,
)
