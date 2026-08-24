# Sprint 17 — Money Capture Ergonomics — Gate Evidence

Date: 2026-08-24. Implemented directly in the main checkout (not delegated), in parallel with
Sprint 21's backend-only slice running in an isolated worktree.

## Build (per the phased sprint plan, `docs/verification/` sprint plan file)

- Money-tab FAB now opens `AddEventDialog` (income/expense) instead of `AddAccountDialog`.
  "Add account" moved to a secondary affordance in `MoneyScreen`'s Accounts section header
  (tag `add-account`), matching the plan's "move to a secondary affordance" instruction.
- New shared `AmountField` composable wrapping every `PkrMoneyInput`-driven amount field with
  `KeyboardOptions(keyboardType = KeyboardType.Number)`, closing the explicit gap in
  `docs/09_UI_DESIGN_SYSTEM.md:75`. Migrated all 8 occurrences across `AddEventDialog`,
  `AddWealthDialog`, `TransferDialog`, `RecurringItemDialog`, `AmountDialog`, `AddAccountDialog`,
  `EditAccountDialog`, and the manual-tax-item dialog.
- Home "Quick Add" now opens `AddEventDialog`/`TransferDialog`/`AddWealthDialog` directly in place
  for the income/expense, transfer, and asset buttons (falls back to switching to the Money tab
  when there are too few accounts for the dialog to be usable — 0 accounts for income/expense,
  <2 for transfer). Tax item and Document quick-add buttons are unchanged (tab-switch only), per
  the plan's scope — their dialogs need more than a simple hoist (file picker / different screen).
- `AddEventDialog` category entry gained suggestion chips built from distinct categories already
  present in `vm.recentEvents` (client-side, no new DAO query), with the existing free-text field
  still available as a fallback. `description` is now optional, matching the functional spec's
  stated minimum of amount/account/category.

## Gate

- New JVM regression test `AmountFieldUsageTest.everyPkrMoneyInputFieldGoesThroughSharedAmountField`
  — scans `PassportApp.kt` for the raw inline `PkrMoneyInput.groupedInput(it)?.let { x -> y = x } }`
  pattern (which only the shared `AmountField` composable's own definition is exempt from, since
  it uses a structurally different `.let(onValueChange)` form) — a concrete, falsifiable guard
  against the numeric-keyboard fix regressing.
- Full connected suite (`./gradlew :app:connectedDebugAndroidTest`, `Android_26_Test`/API 26):
  first run found 3 real regressions (existing tests still reaching for the FAB to create an
  account: `UiDrivenBackupRestoreDeviceTest`, `RecurringDraftDeviceTest`,
  `SecurityLifecycleDeviceTest` — none of these were touched by the earlier Phase 11 fix pass,
  which only updated the 2 test files it happened to be exercising at the time). Fixed by pointing
  all three at the new `add-account` tag instead of the repurposed FAB. Second run: **60/60 PASS**.
- `./gradlew test lint assembleDebugAndroidTest` — BUILD SUCCESSFUL, no regressions.

## Deferred

Money screen's Accounts/Activity/Income/Expenses/Calendar tab split, activity filter/search, and
Home's net-worth trend indicator were in the audit's lower-priority findings but not in this
sprint's explicit Build list — left for a later polish pass, not silently dropped.
