# Vexel Finance Passport — Graphical Final Device Acceptance

Date: 2026-08-29 (Asia/Karachi)  
Verdict: **PARTIALLY VERIFIED — connected emulator suite has unresolved failures**

The SDK emulator was subsequently started. The physical Vivo device was intentionally excluded from all Gradle runs with `ANDROID_SERIAL=emulator-5554`.

## Device establishment

Commands executed:

```text
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
```

Final compatibility emulator: `emulator-5554`, AVD `Android_26_Test`, Android 8.0.0, API 26. API 36 and API 35 also booted but exhibited System UI non-responsiveness during UI tests.

## Build and test execution

Exact commands executed:

```text
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
./gradlew test
./gradlew connectedDebugAndroidTest
```

Results:

- `assembleDebug`: PASS, 2m 32s.
- `assembleDebugAndroidTest`: initially failed because `DocumentPreviewDeviceTest` referenced missing `renderDocumentPreview`; the test helper was implemented and the command was rerun successfully.
- `test`: PASS. 22 suites, 78 unique JVM test methods, 0 failed, 0 skipped, 0 errors in debug; release also completed with 22 suites / 78 tests / 0 failed / 0 skipped / 0 errors.
- Instrumentation discovery: 18 classes and 68 `@Test` methods.
- `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` on API 36: 61 passed, 7 failed, 0 skipped; System UI became non-responsive.
- The same command on API 35: 60 passed, 8 failed, 0 skipped; System UI also became non-responsive.
- The same command on API 26: 61 passed, 7 failed, 0 skipped. The first isolated failure, `ManualE2EWalkthroughDeviceTest`, reproduced from clean state while waiting for the newly created bill; later UI failures cascaded from that state. API 35 also had `UtilityBackupRestoreDeviceTest` fail with `expected Paid but was Due soon`.

## Acceptance gates

| Gate | Method | Device | Result | Evidence/Notes |
| ---- | ------ | ------ | ------ | -------------- |
| Device establishment | ADB properties and emulator startup | Android_26_Test / API 26 | PASS | `emulator-5554`; Vivo excluded. |
| Connected Android tests | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | Android_26_Test / API 26 | FAIL | 68 executed: 61 passed, 7 failed, 0 skipped. |
| Fresh install and navigation | Install APK and manual walkthrough | None | BLOCKED | No target for install or walkthrough. |
| Controlled financial dataset and balances | Real UI data entry and independent balance comparison | None | BLOCKED | Not executed. |
| Transfer invariant | UI transfer and income/expense totals | None | BLOCKED | Not executed on device. |
| Financial Pulse | Overdue, due-soon, and paid obligations | None | BLOCKED | Not executed. |
| Living Bills / Bill Rhythm provenance | Stored occurrences and payment history | None | BLOCKED | Not executed. |
| Capture tray | Expense, income, transfer, bill flows | None | BLOCKED | Not executed; repaired icon is source/build verified only. |
| History and repaired status filter | UI filtering and chronological history | None | BLOCKED | Not executed; repaired filter is source/build verified only. |
| Privacy sweep and repaired dialogs | Runtime masking across reachable routes | None | BLOCKED | Not executed; dialog repairs are source/build verified only. |
| Light and dark themes | Complete primary navigation | None | BLOCKED | No rendered-device evidence. |
| Font scaling | Default, ~1.5x, ~2x | None | BLOCKED | Not executed. |
| Accessibility semantics | Runtime spot-check | None | BLOCKED | Not executed. |
| Security device regression | PIN, biometric cancel, relock, relaunch, logcat | None | BLOCKED | Not executed; no runtime logcat available. |
| Backup / restore | Populate, encrypted backup, clear, restore, compare counts/balances | None | BLOCKED | Not executed on device. |
| Room migrations | `DatabaseMigrationTest` through connected suite | Android_26_Test / API 26 | PASS | All 8 migration methods passed in the connected run. |
| Rotation / process recreation | Portrait, landscape, rotation, background/process recreation | None | BLOCKED | Not executed. |
| Logcat sweep | Clear logcat, core walkthrough, inspect fatal/runtime errors | None | BLOCKED | No device logcat available. |

## Remediation during this sprint

`DocumentPreviewDeviceTest` had no implementation for its two preview calls, preventing the required Android test APK from compiling. A test-side preview helper now decrypts documents through `DocumentVault`, decodes image previews, and renders the first PDF page with `PdfRenderer`. The Android test APK then built successfully.

## Final quality gate

The required `VERIFIED AFTER REMEDIATION` verdict is not warranted. The corrected verdict is:

**PARTIALLY VERIFIED — emulator access is established, but the connected suite has 7 unresolved API 26 failures, including the isolated E2E bill-save workflow.**
