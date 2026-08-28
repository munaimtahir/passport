# Data-model inventory

## Room configuration

Database: `passport.db`, `AppDatabase`, version 13, 29 entities and 25 DAO accessors. Schema exports
exist for versions 2-13. `DatabaseProvider` registers every migration 1->2 through 12->13. Production
does not call destructive migration fallback. No database views are defined. No production demo/seed
loader was found; tests create synthetic fixtures directly.

## Entity inventory

| Entity/table | Purpose | Usage classification |
|---|---|---|
| `UserProfileEntity` / `user_profiles` | display name/base currency | IMPLEMENTED BUT APPARENTLY UNUSED (no DAO accessor) |
| `AccountEntity` / `accounts` | cash/bank-like account record | IMPLEMENTED, ORPHANED UI |
| `FinancialEventEntity` / `financial_events` | income/expense/transfer/adjustment ledger | IMPLEMENTED, ORPHANED UI |
| `IncomeSourceEntity` / `income_sources` | income-source metadata | IMPLEMENTED, ORPHANED UI |
| `AssetEntity` / `assets` | acquisition/current/disposal values | IMPLEMENTED, ORPHANED UI |
| `LiabilityEntity` / `liabilities` | loan/debt outstanding state | PARTIAL, ORPHANED UI |
| `InvestmentEventEntity` / `investment_events` | security cash/event facts | PARTIAL, ORPHANED UI |
| `ReceivableEntity` / `receivables` | outstanding receivable state | PARTIAL, ORPHANED UI |
| `GoalEntity` / `goals` | target and current progress | IMPLEMENTED, ORPHANED UI |
| `RecurringItemEntity` / `recurring_items` | recurring draft/reminder definition | IMPLEMENTED, ORPHANED UI |
| `BudgetEntity` / `budgets` | monthly category limit | BACKEND ONLY |
| `TransferLinkEntity` / `transfer_links` | paired transfer lineage | BACKEND ONLY |
| `TaxYearEntity` / `tax_years` | year/ruleset/status | BACKEND ONLY |
| `TaxItemEntity` / `tax_items` | captured tax candidate | BACKEND ONLY |
| `TaxMappingEntity` / `tax_mappings` | immutable classification lineage | BACKEND ONLY |
| `TaxAnnualDraftEntity` / `tax_annual_drafts` | versioned annual draft header | BACKEND ONLY |
| `TaxDraftLineEntity` / `tax_draft_lines` | calculated annual line | BACKEND ONLY |
| `TaxIssueEntity` / `tax_issues` | generated review issue | BACKEND ONLY |
| `WealthSnapshotEntity` / `wealth_snapshots` | opening/closing tax-year position | BACKEND ONLY |
| `WealthReconciliationEntity` / `wealth_reconciliations` | reconciliation result | BACKEND ONLY |
| `CalendarItemEntity` / `calendar_items` | reminder state and link | BACKEND ONLY |
| `DocumentEntity` / `documents` | encrypted general-vault metadata | ORPHANED UI |
| `DocumentLinkEntity` / `document_links` | many-to-many evidence links | ORPHANED UI |
| `OfficialRecordEntity` / `official_records` | masked/encrypted identifier | ORPHANED UI |
| `ChangeLogEntity` / `change_log` | intended audit history | IMPLEMENTED BUT UNUSED (no DAO) |
| `UtilityBillProfileEntity` / `utility_bill_profiles` | recurring connection definition | ACTIVE/WORKING |
| `MonthlyBillOccurrenceEntity` / `monthly_bill_occurrences` | one profile-month bill cycle | ACTIVE/WORKING |
| `PaymentRecordEntity` / `payment_records` | one payment per occurrence | ACTIVE/WORKING |
| `BillAttachmentEntity` / `bill_attachments` | utility bill/payment-proof file metadata | ACTIVE/PARTIAL integrity |

## DAOs

`AccountDao`, `IncomeSourceDao`, `WealthDao`, `InvestmentDao`, `ReceivableDao`, `GoalDao`,
`RecurringItemDao`, `BudgetDao`, `ReconciliationDao`, `TaxYearDao`, `WealthSnapshotDao`,
`DocumentDao`, `DocumentLinkDao`, `OfficialRecordDao`, `FinancialEventDao`, `TransferLinkDao`,
`TaxItemDao`, `TaxMappingDao`, `TaxDraftDao`, `TaxIssueDao`, `CalendarDao`, `UtilityBillDao`,
`MonthlyBillOccurrenceDao`, `PaymentRecordDao`, and `BillAttachmentDao`.

No `UserProfileDao` or `ChangeLogDao` exists, so those entities cannot be used through the declared
database API.

## Relationships and integrity

- Transfer links foreign-key both financial-event rows with CASCADE and unique indices.
- Tax items RESTRICT deletion of their tax year and uniquely identify source type/id. Tax mappings
  CASCADE from tax item. Draft lines/issues have indices but no declared foreign keys to drafts.
- Utility occurrences CASCADE from profile and are unique per profile/year/month. Payments CASCADE
  from occurrence and are unique per occurrence.
- `BillAttachmentEntity.linkedId` has no foreign key and no linked-entity type discriminator. It can
  refer to profile, occurrence, or payment only by convention. Database deletion cannot cascade its
  row, and file deletion requires separate code.
- `DocumentLinkEntity` has a uniqueness constraint but no declared foreign key to `DocumentEntity`.
- Financial events have account/income-source indices but no declared foreign keys.

## Important domain types

- `Money`, `MinorUnits`, `FinancialEventType`, `TransferPair`, `FinancialPosition`.
- `RecurringFrequency` and schedule helpers.
- `TaxRelevance`, `EvidenceState`, `ReviewState`, `TaxEventType`, rules/mapping/candidate/draft/issue
  types, duplicate detection, and wealth reconciliation inputs/results.
- Utility statuses and categories are plain strings, not enums. This permits divergent spellings and
  category sets between code paths.

## Migrations

1->2 wealth/investment/receivable/goal; 2->3 drafts/reconciliation; 3->4 calendar; 4->5 official
records; 5->6 tax issues; 6->7 recurring; 7->8 goal progress/recurring anchor/budgets; 8->9 tax
mapping; 9->10 wealth snapshots; 10->11 income sources; 11->12 liability terms; 12->13 utility
profiles/occurrences/payments/attachments.

Evidence: `AppDatabase.kt`, `Entities.kt`, `Daos.kt`, `DatabaseProvider.kt`, and
`app/schemas/pk.vexel.financepassport.core.database.AppDatabase/*.json`.

