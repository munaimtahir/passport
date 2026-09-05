# Wave A+B Final Verification

## Repository baseline

- Branch: `main`
- Starting HEAD: `5c89a827e57aa9f68e7c398b2a146e4541fb2309`
- Application: `pk.vexel.financepassport`
- Version: `1.1.0` / versionCode `4`
- Room schema at verification start: `15`; current implementation schema: `16`
- SDK: min `26`, compile/target `36`
- Worktree was clean at verification start; later changes are listed by Git.

## Environment and device

- Selected serial: `emulator-5554`
- Model: `sdk_gphone64_x86_64` (`Android_15_Test`)
- Android/API: Android 15 / API 35
- Size/density: `1080x2340` / `440`
- The AVD was factory-reset with `-wipe-data -no-snapshot` before the current verification run.

## Host gates

| Command | Result |
|---|---|
| `./gradlew clean` | PASS (initial run) |
| `./gradlew compileDebugKotlin` | PASS |
| `./gradlew test` | PASS after fixture remediation |
| `./gradlew lint` | PASS after WorkManager manifest remediation |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleDebugAndroidTest` | PASS |

## Instrumentation execution

The complete connected suite was executed against API 35 after the factory reset: `97/97` completed, `0` failed, `0` skipped. A targeted normalized C–E database run also executed `30/30`, `0` failed, `0` skipped. The final complete connected suite on the dedicated `passport` emulator (API 36, serial `emulator-5562`) executed `100/100`, `0` failed, `0` skipped.

## Defects discovered and remediation attempts

### AB-001 — migration fixture SQL used unquoted string literals

- Severity: Medium
- Reproduction: `DatabaseMigrationTest.migrateV14ToV15AddsContextsAndBillDefaults`
- Root cause: SQLite interpreted fixture identifiers such as `pr1` and `Cash` as names.
- Fix: quoted the fixture string values.
- Retest: direct migration class passed `9/9`.
- Status: fixed.

### AB-002 — fresh process could crash before Compose content

- Severity: High
- Reproduction: cold app/instrumentation launch with no initialized WorkManager.
- Root cause: `PassportApplication.onCreate()` scheduled work before WorkManager initialization.
- Fix: explicit on-demand WorkManager initialization and `Configuration.Provider`.
- Retest: cold launch and subsequent instrumentation progressed without the original crash.
- Status: fixed; lint later required the corresponding manifest initializer removal.

### AB-003 — onboarding instrumentation tests were order-dependent

- Severity: Medium
- Reproduction: `OnboardingDeviceTest` failures when prior tests left onboarding/PIN state.
- Root cause: shared app preferences and PIN state between test methods.
- Fix: per-test live preference/PIN reset and activity recreation.
- Retest: direct onboarding class passed `4/4`.
- Status: fixed.

### AB-004 — UI test state/selector assumptions

- Severity: Medium
- Reproduction: Manual E2E timed out or reported no hierarchy in shared runs; direct rerun also found two matching `HBL Personal` nodes.
- Root cause: test state leakage and a wait helper requiring exactly one matching node.
- Fix attempts: per-test state reset; multiplicity-safe text wait; Security lifecycle onboarding flow corrected so PIN-protected tests actually create a PIN.
- Retest: direct Manual E2E passed `1/1`; Security lifecycle passed its first two workflows before a later emulator UiAutomation registration cascade.
- Status: closed by direct rerun and final connected execution.

### AB-005 — suite-only utility backup status mismatch

- Severity: High
- Reproduction: full suite `UtilityBackupRestoreDeviceTest` expected `Paid`, observed `Due soon`; isolated class passed.
- Root cause: restored derived utility status was not reconciled before the restore call returned.
- Fix: restore now runs utility recurrence reconciliation after database/file restoration.
- Retest: targeted restore test passed, followed by final `100/100` connected suite on `passport`.
- Status: closed by retest.

### AB-006 — final connected run lifecycle failure/device teardown disconnect

- Severity: High
- Reproduction: latest connected run completed 96/97; `SecurityLifecycleDeviceTest.deleteAllDataReturnsToOnboardingWithoutProcessKill` failed, followed by `device 'emulator-5554' not found` during teardown.
- Root cause: suite/device lifecycle instability was isolated by test-state cleanup and a clean emulator run.
- Retest: final `passport` suite completed `100/100` without device loss.
- Status: closed by retest.

## Financial invariant matrix

The A/B automated and connected gates are green. The final APK was installed on `passport`, launched after clean app data, notification permission was handled, and the process survived force-stop/relaunch with no app, Room, or SQLite crash observed. Complete manual A/B financial entry evidence on `passport` is still limited; C–E device workflows are not used to claim final A–E acceptance.

## Final verdict

# NOT ACCEPTED

Wave A+B automated acceptance gates are green. Formal overall acceptance remains **NOT ACCEPTED** until the complete manual device workflow evidence is captured and the C–E UI integration gaps are resolved; see `WAVE_C_D_E_FINAL_VERIFICATION.md`.
