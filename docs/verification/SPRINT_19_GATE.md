# Sprint 19 — Loans, Receivables & Wealth Segmentation — Gate Evidence

Date: 2026-08-24. Both parts complete: due-dates/liability structural fields, and the Wealth
screen tab/segment split.

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

## Build (part 2)

`WealthScreen` restructured from one flat `LazyColumn` into `SecondaryScrollableTabRow`-driven
segments — Assets / Investments / Liabilities / Receivables / Goals
(`docs/03_INFORMATION_ARCHITECTURE_AND_UX.md:35-41`). Net-wealth summary card stays always visible
above the tabs. `AddWealthDialog` gained an `initialMode` parameter (keyed via
`rememberSaveable(initialMode)` so a fresh dialog open never restores a stale mode from a prior
session in the same Activity) so each tab's "Add" button opens pre-selected to the matching mode.

## Gate

- `DatabaseMigrationTest.migrateV11ToV12AddsLiabilityStructuralFields`: pre-existing liability row
  survives migration with both new columns `NULL`; new columns are settable afterward against that
  same real row.
- Real regressions found and fixed during device verification (each genuinely distinct — see
  commit `2d26dd9`-style precedent of this repo always diagnosing root cause, never papering over):
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
  5. Adding the tab row introduced a second scrollable region on the Wealth screen (horizontal, for
     tabs, alongside the list's vertical scroll) — the shared `hasScrollAction()`-based test helpers
     in `WealthCaptureDeviceTest`/`ManualE2EWalkthroughDeviceTest` assumed exactly one scrollable
     and threw "found 2 nodes". Fixed by matching on `SemanticsProperties.VerticalScrollAxisRange`
     specifically instead of any scroll action.
  6. **The real, non-obvious one:** clicking the off-screen "Goals" tab (5th tab, outside the
     `ScrollableTabRow`'s initial viewport) via a plain semantics-action `performClick()` silently
     invoked `onClick` without the tab row's internal scroll-position state actually settling —
     `selectedTab` visibly never changed, and the resulting dialog opened in the wrong mode. Unlike
     the earlier `horizontalScroll`-chip-row case (Sprint 17/19 part 1), where an off-screen
     semantics click worked fine, `Tab`/`ScrollableTabRow` needs `performScrollTo()` before
     `performClick()` to actually commit the selection. Root-caused via in-process `Log.d` counters
     (external `adb shell uiautomator dump` polling during a live instrumentation run was tried
     first and discarded — it collides with the test's own UiAutomation connection and corrupts the
     run, confirmed by `IllegalStateException: UiAutomationService ... already registered`).
- Full connected suite (`Android_26_Test`/API 26): **62/62 PASS**.
- `./gradlew test lint` — BUILD SUCCESSFUL.
