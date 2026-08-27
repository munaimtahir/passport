# Sprint 18 Gate — Goals: Progress & Contribution UI

Run in an isolated git worktree (parallel to a separate background device-qualification agent
running in the main working tree) per `docs/verification/pushthis-and-lets-reflective-quasar.md`
Sprint 18. Host machine was under heavy load (~13.5 load average / 8 cores) from that agent's
emulators, so per explicit constraint no new emulator was booted this pass — instrumentation tests
below are written and compile-verified but not yet run on a device.

## Build

- `FinanceRepository.addGoal` (`core/database/FinanceRepository.kt`): gained optional
  `goalType: String = "CUSTOM"` and `targetDateEpochDay: Long? = null` params, replacing the
  previously hardcoded `"CUSTOM"`/`null`. `MainViewModel.addGoal` extended to match (`LocalDate?`
  target date, converted to epoch day); new `MainViewModel.contributeToGoal` wrapper over the
  already-implemented `FinanceRepository.contributeToGoal`; new `MainViewModel.goalProgress`
  StateFlow exposing the already-computed `FinanceRepository.goalProgress`.
- `AddWealthDialog` (`ui/PassportApp.kt`) GOAL mode gained a goal-type chip picker (Emergency
  Fund/Purchase/Debt Payoff/Custom) and an optional target-date field (toggle + `DateField`,
  mirroring the existing official-record optional-expiry-date pattern — `DateField` itself stays
  non-nullable by contract).
- `WealthScreen`'s goal section rewritten from a static `Text`-only list item to a card per goal:
  `LinearProgressIndicator` + percent/target-date/required-monthly-savings text (or "Achieved"),
  and a "Contribute" button (hidden once achieved) wired to the new `contributeToGoal` via the
  existing `AmountDialog` pattern already used for asset/liability/receivable actions. Goals
  section also gained its own "Add" button in its header, matching the Assets/Investments
  sections (previously goals could only be added via the Assets "Add" button).
- Reused the existing, already-unit-tested `calculateGoalProgress` (`core/model/GoalMath.kt`) for
  all progress/status logic — no new calculation logic written.

## Gate

| Check | Result |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL |
| `./gradlew test` (JVM unit tests, debug+release) | BUILD SUCCESSFUL, all existing tests green, no regressions |
| `./gradlew lint` | BUILD SUCCESSFUL, only pre-existing deprecation warnings (unrelated: `menuAnchor()`, `createAndroidComposeRule` v1) |
| `./gradlew assembleDebugAndroidTest` | BUILD SUCCESSFUL — new instrumentation tests compile |

Full command actually run: `./gradlew test lint assembleDebugAndroidTest` — BUILD SUCCESSFUL in 5m39s, 104 tasks.

### Instrumentation tests written, compiled, **not yet run on a device** (per the no-new-emulator constraint)

- `AppDatabaseTest.addGoalPersistsRealGoalTypeAndTargetDateInsteadOfHardcodedDefaults` — asserts
  a goal created with explicit type/date persists them; asserts the old 2-arg call site still
  defaults to `"CUSTOM"`/`null` (backward-compatible regression guard).
- `WealthCaptureDeviceTest.goalContributionUpdatesProgressAndAchievesAtTarget` — creates a
  Purchase-type goal via the UI, contributes twice (60% then 100%), asserts the progress text
  updates and the card shows "Achieved" once the target is reached.
- Existing `AppDatabaseTest.goalContributionAccumulatesAndMarksAchievedWithoutExceedingTarget`
  (pre-existing, unmodified) still compiles and is unaffected — confirms the old 2-arg `addGoal`
  call site remains valid.

**Follow-up required:** run `./gradlew :app:connectedDebugAndroidTest --tests "*Goal*"` (or the
full suite) on a device once an emulator is free, to close this sprint's gate fully.

## Deferred / not touched this sprint

Nothing from the Sprint 18 Build list was deferred — all three build items landed. Out of scope by
design (per the sprint plan): Sprint 19's due-date/loan-structure work, Sprint 21's income-source
schema, and anything tax-engine-related.
