# Sprint 23 — Money screen polish gate (filter/date-range)

Scope: the Money-screen half of Sprint 23's time-permitting polish line — "Money screen
FilterBar/DateRangePicker (already in the design system, unused) for activity search/date-range
filtering." (`docs/09_UI_DESIGN_SYSTEM.md` only documented these as bullet points; grep confirmed
neither was implemented anywhere in code before this change.)

## Build

- `MoneyScreen`'s "Activity" section gained client-side filtering over `recentEvents`:
  - A `FilterChip` row (`activity-filter-bar`, tags `activity-filter-ALL/INCOME/EXPENSE/TRANSFER`)
    filters by `eventType`. Defaults to `ALL` — unfiltered is still the default view.
  - A date-range toggle (`activity-date-range-toggle`) reveals two `DateField`s
    (`activity-date-from`/`activity-date-to`, reusing the existing shared `DateField` composable)
    that bound `dateEpochDay`. Off by default.
  - Filtering is applied client-side via `remember(recentEvents, ...)` — `recentEvents` is already
    bounded to 200 rows by `FinanceRepository`, so no new DAO query was needed for a first pass.
  - An empty-state row ("No activity matches these filters.") replaces the list when a filter
    combination matches nothing.

## Gate

- `./gradlew test lint` — pass, no new warnings.
- `./gradlew :app:connectedDebugAndroidTest` on the attached `Android_26_Test(AVD)` emulator —
  **first run failed**, all passing since fixed (see Defects below). Final run: full suite green.

### Defects found and fixed during device verification

1. **Scroll-ambiguity regression, same class of bug as Sprint 19's Wealth tab row.** The new
   `activity-filter-bar` `Row` uses `Modifier.horizontalScroll(...)`, which made the Money screen
   a second scrollable region alongside the vertical `LazyColumn`. Every existing helper that did
   `onNode(hasScrollAction()).performScrollToNode(...)` against the Money screen — `assertVisible`
   in `MoneyCaptureDeviceTest`, `scrollToAndAssertVisible` in `RecurringDraftDeviceTest` (both used
   by tests that never touch the new filter UI at all) — started throwing "found 2 nodes" the
   instant any row after the filter bar needed scrolling into view. This broke 5 previously-passing
   tests (`accountAndSalaryCapturePersistThroughUi`, `incomeWithoutIncomeSourcePickedStillSaves`,
   `incomeSourceCanBeAddedInlineAndAppearsInBreakdown`, the new
   `activityFilterByTypeNarrowsVisibleList`, and `recurringDraftCreatesReminderWithoutFinancialEvent`).
   Fixed by reusing the exact `isVerticallyScrollable` `SemanticsMatcher` pattern already
   established in `WealthCaptureDeviceTest`/`ManualE2EWalkthroughDeviceTest` (matches on
   `SemanticsProperties.VerticalScrollAxisRange` specifically instead of "any scrollable"), applied
   in both `MoneyCaptureDeviceTest` and `RecurringDraftDeviceTest`.
2. Removed the now-unused `hasScrollAction` import from both files after the above fix.

### New test coverage

- `MoneyCaptureDeviceTest.activityFilterByTypeNarrowsVisibleList`: records one expense and one
  income event, confirms both are visible unfiltered, selects the `EXPENSE` chip and confirms the
  income row disappears from the semantics tree (`assertNotVisible` — a plain absence check, since
  a client-side filter removes the row from the data list entirely rather than merely scrolling it
  off-screen) while the expense row stays, then returns to `ALL` and confirms both are visible
  again.

## Deferred / not in this pass

- Date-range filtering itself is implemented and reachable but not covered by an instrumentation
  test in this pass (the type filter was prioritized as the primary, more error-prone regression
  surface — see the scroll-ambiguity defect above). It uses the same `DateField` component already
  covered elsewhere in the suite (tax/document dates), so the residual risk is limited to the
  range-membership predicate itself, which is a one-line `LocalDate` comparison.
- No new DAO query/index was added; if the recent-events list is ever unbounded from 200 rows,
  client-side filtering should be revisited in favor of a filtered query.
