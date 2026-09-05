# Wave C–D–E Forensic Discovery

## 1. Domain / Database Baseline
- **AccountEntity**: Defines user accounts. Has `openingBalanceMinor` and `openingBalanceDateEpochDay`. Will be KEPT.
- **FinancialEventEntity**: Core of the financial spine. Has `eventType` (INCOME, EXPENSE, TRANSFER, ADJUSTMENT). Uses strings for `category` and `contextId`.
- **FinancialContextEntity**: Existing domains (PERSONAL, PROFESSIONAL). KEPT.
- **Category**: Currently represented as a simple `String?` field in `FinancialEventEntity` and `BudgetEntity`. No central taxonomy exists. **MIGRATE / MODERNIZE** to a unified CategoryEntity (Wave C).
- **RecurringItemEntity**: Currently a legacy structure capturing amount, frequency, next due date, etc. **MODERNIZE** to separate `RecurringTemplate` and `ExpectedOccurrence` to match the exact semantics described in Wave C.
- **LiabilityEntity**: Exists, tracks `originalAmountMinor`, `outstandingAmountMinor`, etc. Needs **RECONNECT** to ensure borrowings are accurately reflected as non-income cash flows and installments split appropriately into principal vs. financing cost (Wave D).
- **ReceivableEntity**: Exists, tracks outstanding amounts. **RECONNECT** / **MODERNIZE** to support distinct semantic types (MONEY_LENT, INCOME_DUE, REIMBURSEMENT) and cash-basis income recognition (Wave D).
- **InvestmentEventEntity**: Tracks detailed gross/fees/tax withheld and quantity. Represents a mix of legacy operations. Needs to be evaluated against Wave E's "Simple Investment" requirements to ensure we aren't drifting into unapproved complex securities trading. **MODERNIZE / REPLACE** with simple position entities or adapt the current one to enforce the boundary.
- **Utility Bills**: Supported via `UtilityBillProfileEntity` and `MonthlyBillOccurrenceEntity`. **KEEP** as-is; ensure new C-E structures don't conflict with these well-defined invariants.

## 2. Classification
- **Account**: KEEP
- **FinancialEvent**: KEEP & RECONNECT (extend classification/linkages for financing/investments).
- **FinancialContext**: KEEP.
- **Category (String)**: MIGRATE to real `CategoryEntity`.
- **RecurringItemEntity**: REPLACE / MODERNIZE to template/occurrence engine.
- **LiabilityEntity**: MODERNIZE / RECONNECT (fix financing vs. expense accounting).
- **ReceivableEntity**: MODERNIZE / RECONNECT (fix income recognition vs. settlement).
- **InvestmentEventEntity**: REPLACE / MODERNIZE (enforce simple positions).

## 3. Explicit Identifications
* **Duplicate Ledgers**: The existing `LiabilityEntity` and `ReceivableEntity` might currently operate as duplicate sources of truth if cash movements are logged separately without linkage.
* **Obsolete Models**: `RecurringItemEntity` is a single entity trying to be a template and an expectation at once.
* **Useful Reusable Models**: `FinancialEventEntity` handles standard cash operations perfectly.
* **Migration Hazards**: Changing `category` from String to ID mapping across `FinancialEventEntity`, `BudgetEntity`, and `RecurringItemEntity`. Also, recalculating `nextDueDate` or translating legacy `RecurringItemEntity` instances into explicit `Template` + `Occurrence`.
* **Backup-Format Impact**: New entities will need to be explicitly serialized/deserialized in the encrypted backup formats.
* **History Integration Impact**: We must extend `FinancialEventEntity` logic to present groups (e.g. loan installments split) properly without creating duplicate top-level events.
* **Tax Linkage Impact**: Only recognized income and expenses should hit `TaxItemEntity`. Investments and Settlements explicitly fall outside ordinary tax linkage unless profit is taken or financing costs are incurred.
* **Event Classification Limitations**: `FinancialEventType` has only INCOME, EXPENSE, TRANSFER, ADJUSTMENT. We may need to add FINANCING or SETTLEMENT or rely on `TRANSFER` / `ADJUSTMENT` combined with specialized foreign keys, or introduce a new type altogether. 
* **Room Schema Impact**: Will require explicit migrations for `CategoryEntity`, `RecurringTemplateEntity`, `ExpectedOccurrenceEntity`, and additional fields on `FinancialEventEntity` or a new `FinancialComponentEntity` for grouping. 

## 4. Unresolved Ambiguity
None. Product semantics strictly dictate how things should behave, and we will extend the architecture to respect these rules.

## 5. Build/Schema Baseline
- Initial build/lint tasks succeed (modulo a local file lock failure on `clean`).
- Initial Room Version was **15**; current Room Version is **16** after additive migration.

## 6. Host-side implementation checkpoint

- A/B device verification evidence is recorded in `docs/verification/WAVE_A_B_FINAL_DEVICE_VERIFICATION.md`.
- Dedicated emulator `passport` (`emulator-5562`, API 36) was used for the current verification; `Android_15_Test` was excluded.
- Wave C–E schema work has begun additively at Room version **16**: normalized categories, recurring templates/expected occurrences, settlement events, and simple investment positions are registered without dropping legacy tables.
- Wave C host-side domain coverage now includes fixed/variable amount validation, interval generation, end-date handling, and no-invented-amount behavior.
- Wave D/E host-side primitives now validate liability installment reconciliation, receivable settlement recognition, and investment redemption reconciliation.
- Required follow-up before any wave can be accepted: repository integration, backup/export coverage, migration instrumentation, UI/device workflows, and cross-wave regression.
- Final connected instrumentation on `passport` executed 100 tests with 100 passed, 0 failed, and 0 skipped. C/D/E are still not accepted because the mandatory recurring, liability/receivable, and investment UI flows are not exposed by the current app shell; see `docs/verification/WAVE_C_D_E_FINAL_VERIFICATION.md`.
