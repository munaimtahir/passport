# Device Qualification Handoff (Phase 10)

This is the Phase 9F deliverable: exact instructions to run Phase 10 (deferred device
qualification) of the remediation mega-prompt. Phases 0-8 are complete and committed
host-side-only (no device was used); see
`docs/IMPLEMENTATION_COMPLETE_DEVICE_VERIFICATION_PENDING.md` for the full implementation report
and `docs/verification/ACCEPTANCE_MATRIX_PHASE9.md` for the acceptance-test mapping this phase
must resolve.

**Update, same session:** real Android emulators turned out to already be present in this
environment (see below), so Phase 10 is executed directly here rather than after a clone to a
separate environment. This document is kept as the reference for exactly what Phase 10 requires,
and doubles as the record of what was actually run.

## Repository / commit

- Repository (this session): `/media/munaim/shared1/Documents/github/passport`
- Remote: `git@github.com:munaimtahir/passport`
- Branch: `main`
- Commit to check out for Phase 10: the HEAD as of the end of Phase 9 (see
  `docs/IMPLEMENTATION_COMPLETE_DEVICE_VERIFICATION_PENDING.md` for the exact SHA) — Phase 10's
  own fixes, if any, land as additional commits after that point, never by rewriting phases 0-9.

## Toolchain

- JDK: Temurin 21.0.12 (`java -version` confirmed this session)
- Gradle: wrapper 8.13 (`./gradlew`, do not use a system Gradle)
- Android SDK: `compileSdk`/`targetSdk` 36, `minSdk` 26 (`app/build.gradle.kts`)
- `ANDROID_HOME` / `ANDROID_SDK_ROOT`: `/home/munaim/Android/Sdk`
- Installed SDK platforms found this session: `android-26`, `android-34`, `android-35`,
  `android-36`, `android-37.0`
- `adb`: `/home/munaim/Android/Sdk/platform-tools/adb` (not on `$PATH` by default — use the full
  path or add `platform-tools` to `PATH`)
- `emulator`: `/home/munaim/Android/Sdk/emulator/emulator` (also not on `$PATH` by default)

## Available AVDs (confirmed present this session via `emulator -list-avds`)

| AVD name | Android release | API level | Use for |
|---|---|---|---|
| `Android_26_Test` | Android 8.0 | 26 | minSdk floor — required per mega-prompt Phase 10A |
| `Android_16_Test` | Android 16 | 36 | targetSdk/compileSdk ceiling — required per mega-prompt Phase 10A |
| `Android_15_Test` | Android 15 | 35 | Optional intermediate level (mega-prompt calls this out as "optional") |

No physical device is attached (`adb devices` returned an empty list this session). Physical-device
smoke (mega-prompt 10I) stays a separate, later requirement — do not claim it from emulator
evidence.

## Build commands

```bash
cd /media/munaim/shared1/Documents/github/passport
./gradlew clean assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2
```

Expected artifacts:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

## Starting an emulator

```bash
export ANDROID_HOME=/home/munaim/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
emulator -avd Android_26_Test -no-snapshot-load -no-boot-anim &
adb wait-for-device
# poll until sys.boot_completed=1, e.g.:
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
```

Repeat with `Android_16_Test` for the API 36 pass. Run both passes; do not treat API 26 alone as
sufficient (minSdk and targetSdk behave differently in several places this remediation touched —
Keystore, WorkManager, notification permission, `dataExtractionRules`).

## Test order (per mega-prompt Phase 10)

1. **Connected automated tests first**, both API levels:
   ```bash
   ./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=2
   ```
   Every androidTest added across phases 1-8 is currently only compile-verified, never run:
   `AppPreferencesTest`, `DocumentLifecycleDeviceTest`, `DatabaseMigrationTest` (now covers
   2→7, 8→9, 9→10), `NavigationSmokeTest`, `MoneyCaptureDeviceTest`, `RecurringDraftDeviceTest`,
   `WealthCaptureDeviceTest`, plus the pre-existing `AppDatabaseTest`, `BackupRestoreDeviceTest`,
   `DocumentPreviewDeviceTest`, `ReminderDeviceTest`. Fix any real failure at its root cause; do
   not weaken an assertion to force green (per the mega-prompt's failure policy).
2. **Manual E2E workflow** (mega-prompt section 10C) — fresh install → onboarding → PIN →
   biometric (if the AVD supports a virtual fingerprint sensor; API 28+ emulators typically do) →
   accounts → historical income/expense/transfer → asset/liability/receivable → investment buy/
   partial-sell/dividend → document import/link → tax capture → selected tax year → tax issue
   handling → duplicate warning → missing evidence → annual draft → source drill-down →
   reconciliation → report preview → PDF/CSV/JSON export → encrypted backup → clear app data →
   restore → verify all counts/totals/links/hashes → background/relock.
   - **No `DemoUserScenario`/synthetic seed data exists** (Phase 1 explicitly deferred it) — this
     workflow needs manually-entered fixture data. Keep the fixture simple and record the exact
     values entered so the backup-equivalence gate (next) has a known-good baseline to compare
     against.
3. **Backup equivalence gate** (mega-prompt 10D, mandatory) — record row counts/balances/net
   worth/tax-item counts/draft totals/document hashes/evidence links/reconciliation result before
   backup; back up; clear app data; restore; prove equivalence. Also test wrong password, a
   tampered/corrupted package, and that a failed restore leaves the prior state intact. Any
   reproducible data-loss/inconsistency bug here is release-blocking per the mega-prompt.
4. **Accessibility/adaptive** (10E) — TalkBack, font scaling, portrait/landscape, tablet/expanded
   width, touch targets, privacy masking, screen-reader order.
5. **Process/lifecycle** (10F) — background/resume, inactivity relock, biometric cancel, wrong
   PIN, process death, activity recreation. This is where Phase 8's `remember`→`rememberSaveable`
   conversions actually get exercised for the first time — specifically check that the
   intentionally-*unconverted* `BackupPasswordDialog` password field is lost (not retained) across
   process death, confirming that Phase 8 decision behaves as intended rather than as a bug.
6. **Performance** (10G) — the mega-prompt's synthetic dataset (10 accounts, 10,000 events, 2,000
   tax items, 1,000 documents, 10 tax years) does not exist as seed data (same gap as step 2); it
   will need to be generated manually or via a throwaway script before this step can run.
7. **Notifications** (10H) — document/official-record expiry reminders (Phase 6), recurring-draft
   reminders, permission behavior, duplicate-notification avoidance (unique work IDs were added in
   Phase 6 — verify no duplicate fires).
8. **Physical-device smoke** (10I) — not available in this environment; remains open.

## Acceptance-test mapping

Every AT row in `docs/verification/ACCEPTANCE_MATRIX_PHASE9.md` marked
`IMPLEMENTED-DEVICE-REQUIRED` is what this phase is expected to resolve to either PASS or a
recorded defect. Rows marked `NOT IMPLEMENTED` cannot pass regardless of device testing — they
need code work first (out of Phase 10's scope; feed them back as a new phase if found necessary).

## On failure

Per the mega-prompt's failure policy: reproduce narrowly, diagnose root cause, fix, rerun the
narrow test, then rerun the full gate on **both** API levels before considering the defect closed.
Do not disable a test, weaken an assertion, or mark a device-only item as passed without having
actually run it.
