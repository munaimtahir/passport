# Vexel Finance Passport — Graphical Final Device Acceptance

Date: 2026-08-29 (Asia/Karachi)

Verdict: **VERIFIED AFTER REMEDIATION**

All Android runs were explicitly bound to SDK emulators. The connected Vivo device (`34081500040008N`) was not used.

## Device establishment

| Emulator | Serial | Android | API | State |
| --- | --- | --- | --- | --- |
| Android_26_Test | `emulator-5554` | 8.0.0 | 26 | Factory-reset with `-wipe-data`; clean baseline |
| Android_15_Test | `emulator-5556` | 15 | 35 | Clean connected run |
| AdForge_API_36 | `emulator-5558` | 16 | 36 | Clean connected run |

## Test inventory and exact commands

Instrumentation discovery found 18 Android test classes and 68 `@Test` methods.

```text
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
ANDROID_SERIAL=emulator-5556 ./gradlew connectedDebugAndroidTest
ANDROID_SERIAL=emulator-5558 ./gradlew connectedDebugAndroidTest
./gradlew test
```

Connected-suite results: **68 total, 68 passed, 0 failed, 0 skipped** on each emulator. Final API 26 took 6m01s; API 35 took 9m33s; API 36 took 11m33s. Host JVM tests passed in 36s (debug and release: 78 methods each, 0 failures/skips/errors).

## Acceptance evidence

| Gate | Method | Device | Result | Evidence/Notes |
| ---- | ------ | ------ | ------ | -------------- |
| Device establishment | ADB properties; emulator startup | API 26/35/36 | PASS | Three emulator serials recorded above; Vivo excluded. |
| Build and test APK | `assembleDebug`; `assembleDebugAndroidTest` | Host | PASS | Debug APK and test APK packaged. |
| Host tests | `./gradlew test` | Host | PASS | 78 debug + 78 release methods; 0 failed/skipped/errors. |
| Connected Android tests | Full `connectedDebugAndroidTest` | API 26 | PASS | 68/68; 0 failed/skipped. |
| Connected Android tests | Full `connectedDebugAndroidTest` | API 35 | PASS | 68/68; 0 failed/skipped. |
| Connected Android tests | Full `connectedDebugAndroidTest` | API 36 | PASS | 68/68; 0 failed/skipped. |
| Fresh install and navigation | Onboarding, PIN, shell navigation, lifecycle tests | API 26/35/36 | PASS | Failed methods were rerun individually before full gates. |
| Financial data and invariants | Ledger and database device tests | API 26/35/36 | PASS | Income/expense/transfer/payment flows and balances green. |
| Financial Pulse / Living Bills | UI and recurrence device tests | API 26/35/36 | PASS | Occurrence lifecycle and payment update paths green. |
| Bill Rhythm provenance | Occurrence/recurrence tests | API 26/35/36 | PASS | Month occurrences derive from stored lifecycle data. |
| Capture tray and transfer icon | UI connected coverage | API 26/35/36 | PASS | Capture actions green; transfer uses swap icon. |
| History and repaired filters | UI connected coverage | API 26/35/36 | PASS | Income/expense/transfer filters and reset green. |
| Privacy and repaired dialogs | UI/security connected coverage | API 26/35/36 | PASS | Monthly occurrence and utility profile detail masking green. |
| Light/dark and responsive UI | Compose UI connected coverage | API 26/35/36 | PASS | No test failure across emulator configurations. |
| Font/accessibility semantics | Compose semantics/device coverage | API 26/35/36 | PASS | Connected semantics checks green. |
| Security regression | PIN, relock, relaunch, delete-all | API 26/35/36 | PASS | Lifecycle paths green; no app fatal exception. |
| Backup / restore | UI and utility backup/restore device tests | API 26/35/36 | PASS | Profiles, occurrences, payment, event, attachment round-trip green. |
| Room migrations | 8 migration methods in `DatabaseMigrationTest` | API 26/35/36 | PASS | Executed; schema version 14; no destructive fallback. |
| Rotation/process recreation | Security lifecycle device tests | API 26/35/36 | PASS | In-flight bill form and relock recreation green. |
| Logcat sweep | Filtered logcat after full runs | API 26/35/36 | PASS for app | No app FATAL/Room/encryption/storage exception. API 36 had unrelated platform SystemUI/phone ANR noise; no app test failed. |

## Remediation completed

- Serialized bill refreshes and fixed bill-save/dialog-dismiss ordering in `MainViewModel`.
- Added persisted utility occurrences to History and restored expected runtime headings.
- Persisted occurrence status as `Paid` atomically with payment creation, fixing API 35 backup/restore.
- Added the missing Android document preview test helper so the test APK packages successfully.

## Final quality gate

**VERIFIED AFTER REMEDIATION** — host tests and complete connected Android suites are green on factory-reset API 26 plus API 35 and API 36 emulators. No unresolved app Critical/High defect was observed in the executed gates.
