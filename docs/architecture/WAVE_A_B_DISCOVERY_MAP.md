# WAVE A+B DISCOVERY MAP

## Domain Discovery

| Domain capability | Current implementation | Source of truth now | Problems | Decision | Target source of truth |
| ----------------- | ---------------------- | ------------------- | -------- | -------- | ---------------------- |
| Transactions | `FinancialEventEntity` | `FinancialEventEntity` | Lacks `contextId`. Missing `FinancialContextEntity`. | KEEP & MODERNIZE | `FinancialEventEntity` (with contextId) |
| Expense/Income | `FinancialEventEntity` type | `FinancialEventEntity` | Good base, but needs robust Unassigned state support. | KEEP | `FinancialEventEntity` |
| Transfers | `TransferLinkEntity` | Linked pairs of events | Solid model, but needs test verification for invariants. | KEEP | Linked `FinancialEventEntity` |
| Contexts | Missing dedicated entity | N/A | `AccountEntity` has `context` string, but they should be orthogonal. | REPLACE | `FinancialContextEntity` |
| Bill Profile | `UtilityBillProfileEntity` | `UtilityBillProfileEntity` | Lacks default account, context, and category. | MODERNIZE | `UtilityBillProfileEntity` |
| Bill Occurrence | `MonthlyBillOccurrenceEntity` | `MonthlyBillOccurrenceEntity` | Represents obligation. Cannot create expense. | KEEP | `MonthlyBillOccurrenceEntity` |
| Bill Payment | `PaymentRecordEntity` | `PaymentRecordEntity` | Has `financialEventId`. Needs atomic tie to FinancialEvent. | MODERNIZE | `PaymentRecordEntity` 1:1 `FinancialEventEntity` |
| Account Balance | Derived from `FinancialEventEntity` | `FinancialEventEntity` & `AccountEntity.openingBalance` | Strong approach, maintain this. | KEEP | Derived calculation |
| Adjustments | `FinancialEventType.ADJUSTMENT` exists | `FinancialEventType` | UI/logic needs to be verified/implemented. | MODERNIZE | `FinancialEventEntity` (ADJUSTMENT) |

## Duplicate Financial Systems Check
Currently, there is no major second/competing ledger. `PaymentRecordEntity` links to `FinancialEventEntity` via `financialEventId`, meaning it respects the central law (Capture once). We will MODERNIZE this link to ensure it's strictly maintained (1:1 per payment).

## Relationships
- `BillOccurrence` (1:N) `PaymentRecordEntity` (1:1) `FinancialEventEntity` (N:1) `AccountEntity` / `FinancialContextEntity`
- `AccountEntity` provides where money moves.
- `FinancialContextEntity` provides organizational slice (Personal/Professional).
