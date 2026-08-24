# Sprint 21 — Income Sources UI Wiring — Gate Evidence

Date: 2026-08-24. UI-wiring half of Sprint 21 (backend already merged to main, commit `c474ef1`:
`IncomeSourceEntity`/`IncomeSourceDao`, `FinanceRepository.addIncomeSource`/`archiveIncomeSource`/
`incomeSources` Flow, `addEvent(..., incomeSourceId: String? = null)`). Built in an isolated
worktree in parallel with Sprint 20 (bills/recurring UI), which was running directly in the main
checkout at the time — no file overlap (different composables/screens), confirmed before starting.

## Build

- `MainViewModel.kt`: `incomeSources` exposed as a `StateFlow` (mirrors `accounts`/`goals`
  pattern), `addIncomeSource(name, sourceType, payerOrEmployer)` wrapper, `addEvent(...)` extended
  with a trailing `incomeSourceId: String? = null` param forwarded to the repository.
- `PassportApp.kt`: new `IncomeSourcePicker` composable, mirroring `AccountPicker`'s
  `ExposedDropdownMenuBox` pattern exactly, but nullable (`selectedId: String?`) with an explicit
  "None" option — income sources are optional, never required. Wired into `AddEventDialog`, shown
  only when `income == true`. An inline "+ New income source" toggle (matching this file's existing
  idiom for other inline add-flows, e.g. the due-date toggle) reveals a name field + "Add source"/
  "Cancel", calling `addIncomeSource`. The confirm button passes `incomeSourceId` through to
  `vm.addEvent(...)` only when `income == true`.
- `MoneyScreen`: new "Income by source" section — client-side grouping of `recentEvents` (filtered
  to `eventType == "INCOME"`) by resolved source name (falling back to "Unassigned" for events with
  no `incomeSourceId`, and for anything recorded before this sprint), summed per source, sorted
  descending. No new DAO query — small in-memory dataset, matches the plan's stated preference for
  client-side grouping here.

## Gate

- **Real, pre-existing build issue found and fixed, unrelated to this sprint's own code:**
  `AccountPicker`'s `ExposedDropdownMenuBox` usage had no `@OptIn(ExperimentalMaterial3Api::class)`
  of its own — it compiled on `main` (single such call site, evidently tolerated by whatever cached
  compiler/dependency state `main`'s working tree currently has) but became a hard compile *error*
  in this fresh worktree the moment a second `ExposedDropdownMenuBox` call site (mine) was added,
  surfacing the gap. Added the same `@OptIn` annotation already used elsewhere in this file
  (`IncomeSourcePicker`, `PassportApp()`) to `AccountPicker` too — correct regardless of why `main`
  tolerated its absence, and resolves the build deterministically rather than relying on cache
  state. Worth flagging to the coordinating session in case `main` hits the same wall on a clean
  checkout.
- `./gradlew test lint` — BUILD SUCCESSFUL (full host gate, all existing JVM tests green, no lint
  regressions).
- `./gradlew :app:compileDebugAndroidTestKotlin` — BUILD SUCCESSFUL, only pre-existing
  `createAndroidComposeRule` deprecation warnings (same ones already present across every other
  test class in this suite, not new).
- Two new instrumentation tests added to `MoneyCaptureDeviceTest.kt`:
  - `incomeSourceCanBeAddedInlineAndAppearsInBreakdown` — adds an account, opens the income dialog,
    adds a new income source inline, selects it (via a retry helper —
    `FinanceRepository.addIncomeSource` is a fire-and-forget write like every other write in this
    repository, and the dropdown's own contents only compose while expanded, so a plain wait isn't
    enough; the helper re-opens the dropdown each retry), records income against it, and asserts
    both the event and the new "Income by source" row appear.
  - `incomeWithoutIncomeSourcePickedStillSaves` — records income without touching the picker at
    all, confirming the field stays genuinely optional (not silently required).
  - **Not run on a device this pass** — host load was ~18-21 (8 cores) from concurrent Sprint 20
    work sharing the one attached emulator; per the load-avoidance constraint, no emulator was
    booted. Both tests are written and compile-verified only. Follow-up:
    `./gradlew :app:connectedDebugAndroidTest --tests "*MoneyCaptureDeviceTest*"` once a device is
    free, plus a full-suite rerun to confirm the two new tests don't regress the existing ones in
    that class (shared Test-Orchestrator process/app-data within the class, same as always).

## Out of scope, confirmed untouched

`RecurringItemDialog`, `MoneyScreen`'s recurring/bills section — Sprint 20's territory, not touched
by this pass.
