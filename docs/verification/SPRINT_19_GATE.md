# Sprint 19 — Loans, Receivables & Wealth Segmentation — Gate Evidence

Date: 2026-08-24. Part 1 of 2 (due-dates + liability structural fields); Wealth screen
tab/segment split is tracked separately, not yet done.

## Build (part 1)

- **Due dates (zero new schema):** `FinanceRepository.addReceivable` already threaded
  `dueDate: LocalDate?` through to a scheduled reminder (landed in an earlier phase). Extended the
  same pattern to liabilities: `addLiability` now accepts `lender`, `dueDate`, `interestRateBps`,
  `installmentAmountMinor`, and schedules a `LIABILITY_DUE` calendar reminder via the same
  `scheduleOrCancelExpiryReminder` helper used for receivables and official-record expiry.
- **Loan structural fields (additive migration):** `MIGRATION_11_12` (`AppDatabase.DATABASE_VERSION`
  11→12) adds nullable `interestRateBps INTEGER` / `installmentAmountMinor INTEGER` to
  `liabilities`. `AddWealthDialog`'s LIABILITY mode gained a loan-type picker (Credit Card /
  Personal Loan / Car Financing / Home Financing / Informal / Business / Other, replacing the
  hardcoded `"OTHER"` in `MainViewModel.addLiability`), a lender field (column existed, was never
  in the UI), a due-date toggle, interest rate (%), and installment amount. `WealthScreen`'s
  liability card now surfaces lender/due-date/installment/interest-rate when set.
- Backup/export bookkeeping needed no changes — `DATABASE_VERSION` is Sprint 21's shared constant,
  referenced dynamically by both backup-manifest call sites, so the version bump alone keeps them
  in sync (the exact structural fix that constant was built to guarantee).

## Gate

- `DatabaseMigrationTest.migrateV11ToV12AddsLiabilityStructuralFields`: pre-existing liability row
  survives migration with both new columns `NULL`; new columns are settable afterward against that
  same real row.
- Real regressions found and fixed during device verification (not the same bug repeating — each
  distinct):
  1. Two existing tests filled `AddWealthDialog` fields by index (`hasSetTextAction()[0]`/`[1]`) —
     broke once the LIABILITY block added fields between name and amount. Added stable
     `wealth-name`/`wealth-amount` tags (useful for every mode, not just liability) and fixed both
     call sites (`WealthCaptureDeviceTest`, `ManualE2EWalkthroughDeviceTest`) plus the goal test for
     consistency.
  2. Misused `performScrollTo()` on fields that aren't inside any scrollable container (only the
     mode/type/goal-type chip Rows are `horizontalScroll` — the dialog's outer `Column` isn't).
     Removed the unnecessary calls, kept it only on `liability-type-PERSONAL_LOAN` (genuinely
     inside a scrollable chip row).
  3. `AppDatabaseTest`'s two `addLiability(...)` call sites needed the new leading `context` param.
  4. `DatabaseVersionConsistencyTest` had a second test hardcoding `DATABASE_VERSION == 11` — stale
     the moment this sprint bumped it to 12. Removed it; the real regression guard (schemaVersion
     literal vs. shared constant) is a separate test, unaffected.
- Full connected suite (`Android_26_Test`/API 26): **62/62 PASS** after all of the above.
- `./gradlew test lint` — BUILD SUCCESSFUL.

## Deferred to part 2

Wealth screen restructuring from one flat `LazyColumn` into Assets/Investments/Liabilities/
Receivables/Goals segments (`docs/03_INFORMATION_ARCHITECTURE_AND_UX.md:35-41`) — not yet started.
