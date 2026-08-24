# Sprint 20 — Bills & Recurring Utilities Surface — Gate Evidence

Date: 2026-08-24.

## Build

- `RecurringItemDialog` gains a `BillCategory` taxonomy — Electricity/Gas/Water/Internet/Rent/
  Subscription/Other — shown as chips when `income == false` (expense mode), writing into the
  existing `RecurringItemEntity.category` column. No schema change.
- New `FinanceRepository.confirmRecurringItemNow(context, id)`: the explicit user action
  `processDueRecurringItems` deliberately never performs on its own — records a real
  `FinancialEvent` from the recurring item's stored fields immediately, then advances the schedule
  exactly like a normal due-date rollover, regardless of whether the item is actually due yet.
  `MainViewModel.confirmRecurringItemNow` wrapper added.
- Money screen's recurring section relabeled "Bills & Recurring" (was "Recurring drafts"), each
  card gains a "Mark paid" button alongside the existing "Pause" action, and the card now shows
  category (bill type) inline with the event type/amount/frequency summary.

## Gate

- New instrumentation test `RecurringDraftDeviceTest.markPaidRecordsEventImmediatelyAndAdvancesSchedule`:
  creates an Electricity-tagged recurring expense, taps "Mark paid", confirms via direct DAO
  lookup (not just UI assertion) that exactly one new `FinancialEventEntity` was recorded and the
  item's `nextDueDateEpochDay` advanced — without waiting for the periodic worker. Looks up the
  real recurring-item ID via `application.repository.database.recurringItemDao()` (existing
  precedent: `DocumentPreviewDeviceTest`) rather than assuming UI ordering, since `recurringItems`
  is ordered by due-date-then-title and a second item from an earlier test method in the same
  class could otherwise make node targeting ambiguous.
- New JVM-adjacent instrumentation test (Room requires Android, so this lives in `AppDatabaseTest`
  like every other repository test, not `app/src/test`) —
  `confirmRecurringItemNowIsNotDoubleProcessedBySameDayWorkerRun`: calls `confirmRecurringItemNow`
  then `processDueRecurringItems` the same day, asserts the schedule doesn't advance twice and no
  second event is recorded. This holds by construction (advancing the due date always moves it
  past "today," so the worker's `WHERE nextDueDateEpochDay <= today` query naturally excludes an
  already-confirmed item) rather than needing new guard logic — the test proves that invariant
  actually holds against the real DAO query, not just in theory.
- Updated `RecurringDraftDeviceTest`'s existing assertions for the two label changes ("Recurring
  drafts" → "Bills & Recurring", "Next draft reminder:" → "Next due:").
- Full connected suite (`Android_26_Test`/API 26): **64/64 PASS**.
- `./gradlew test lint assembleDebugAndroidTest` — BUILD SUCCESSFUL.

## Deferred

Vendor/biller free-text field and a dedicated payee address book were considered and explicitly
not built — per the phased sprint plan's own reasoning, a constrained category taxonomy gets most
of the value (recognizable categories, filterable Bills surface) for zero migration risk; a real
payee book (dedup, editing, linking) is a bigger feature better left for later.
