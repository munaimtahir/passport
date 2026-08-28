# Test, build, and runtime baseline

## Test inventory

- JVM: 21 Kotlin files, 76 `@Test` annotations.
- Android instrumentation: 17 Kotlin files, 64 `@Test` annotations.

JVM coverage includes money parsing/arithmetic, financial position/events, budgets, goals,
investments, recurring schedules, utility recurrence (4 tests), reports, export, backup crypto/live
restore, PIN verification, tax engine, ruleset loading, database-version consistency, reminders, and
a UI-source usage guard.

Instrumentation coverage includes 27 database tests, seven migration tests, utility payment status,
utility attachment vault, utility backup/restore, UI-driven backup/restore, general document lifecycle
and preview, reminders/notification delivery, preferences/security lifecycle, navigation/onboarding,
manual utility E2E, and synthetic performance.

Important gaps: no automated “Reset Utility” test because no such feature exists; limited tests for
utility category consistency, attachment cleanup on profile/payment deletion, utility-to-ledger
integration (absent), report/export reachability, and current legacy-screen reachability.

## Build verification

Attempted from the repository root:

```text
.\gradlew.bat assembleDebug test lint --no-daemon --max-workers=2
```

Result: **FAILED before project tasks executed** after Gradle wrapper download. Exact Gradle failure
message was `25.0.2`. Environment inspection showed OpenJDK/JBR 25.0.2 as both launcher and daemon
JVM, with no alternate installed JDK discovered. Gradle 8.13 itself reported normally. The likely
cause is project/plugin incompatibility with the installed JDK 25 runtime; discovery did not install
software or change Gradle configuration. Therefore this audit does not claim a fresh test/lint/build
pass. A pre-existing release APK dated 2026-08-28 10:54:46 was available and used for observation.

## Android runtime discovery

- Tools: ADB and emulator present in the configured Android SDK.
- Initially attached devices: none.
- Existing AVD: `Pixel_8_API_36`; started without snapshot-save.
- Device: `sdk_gphone64_x86_64`, Android 16/API 36, x86_64, 1080x2400 at density 420.
- APK: pre-existing `app/build/outputs/apk/release/app-release.apk`, 2,835,898 bytes.
- Install: `adb install -r` succeeded. App data was cleared for a fresh observational run.
- Notification permission was granted on the disposable emulator.

### Workflows exercised

- Fresh utility-specific onboarding: PASS.
- PIN skip: PASS.
- Home/Bills/History navigation: PASS.
- Create utility profile `DiscoveryUtility`: PASS through production UI.
- Automatic current-month occurrence creation: PASS; August 2026 was created automatically.
- Status derivation: PASS observationally; expected due 27 Aug and audit date 28 Aug produced Overdue.
- Profile detail and occurrence detail rendering: PASS.
- History visibility: PASS.
- Force-stop/relaunch persistence: PASS for profile and occurrence.
- Reset Utility: NOT TESTED because no such action exists.
- Payment completion, attachments, backup/restore, legacy finance/wealth/tax/vault/reports: NOT TESTED
  in this manual run; legacy areas were not reachable from the shell.

No Vexel application FATAL EXCEPTION was observed during the run. A broad log filter returned
system/uiautomator noise, so it is not treated as an application defect.

### Reset Utility runtime result

**Before Reset:** `DiscoveryUtility`, August 2026 occurrence, amount TBD, status Overdue.

**Action:** No production Reset Utility action was present. The History `Reset` control is filter
state only.

**After Reset:** Not applicable.

**Financial Ledger Effect:** No utility/account linkage exists in source.

**History Preservation:** PASS; occurrence visible in History.

**Persistence After Relaunch:** PASS.

**Code vs Runtime Agreement:** AGREE.

## Evidence files

- `evidence/01-onboarding.png`
- `evidence/02-home-empty.png`
- `evidence/03-bill-created.png`
- `evidence/04-history-after-relaunch.png`

These screenshots support UI observations only; source/DAO traces support persistence and behavior
claims.

