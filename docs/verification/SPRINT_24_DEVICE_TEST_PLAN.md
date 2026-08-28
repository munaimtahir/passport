# Sprint 24 Device Qualification Plan

Date prepared: 2026-08-28  
Target branch: `main`  
Application ID: `pk.vexel.financepassport`  
Required target: Android API 36 emulator or physical device with working ADB access

## Purpose

This handoff closes the device-only gates left open after Sprint 24 host verification. Clone and
build the repository on a workstation that has JDK 17, the Android SDK and ADB. The Android device
is the test target; cloning and Gradle compilation directly on a phone or tablet is not required or
supported by this plan.

Run the automated suite before the manual scenarios. Do not report Sprint 24 as PASS unless the
connected tests, migration, financial workflow, backup/restore and process-death checks all pass on
an actually booted device.

> **Data warning:** instrumentation uses Android Test Orchestrator with `clearPackageData=true`,
> and the manual plan includes clearing app data. Use a disposable emulator, a dedicated test
> device, or a device on which Vexel Finance Passport contains no valuable data.

## 1. Workstation and device prerequisites

- Git with access to `git@github.com:munaimtahir/passport` (or use the HTTPS clone URL).
- A compatible 64-bit workstation with hardware virtualization for an emulator, or a physical
  Android device connected by USB/Wi-Fi debugging.
- JDK 17. The verified host baseline is Temurin 17.0.18.
- Android SDK Platform 36, current Platform Tools, Build Tools and an accepted SDK license set.
- An Android API 36 target is mandatory for Sprint 24. An additional API 26 run is recommended to
  retain minSdk coverage.
- At least 4 GB of free workstation disk space and enough device storage for the app, test APK and
  exported backup.

For a physical device, enable Developer options and USB debugging, unlock the device, accept the
host's RSA authorization prompt, and keep the screen awake during the suite. No bank credentials,
real financial data, signing secrets or production keystore are needed.

## 2. Clone and verify the revision

```bash
git clone git@github.com:munaimtahir/passport.git
cd passport
git switch main
git pull --ff-only origin main
git status --short --branch
git log -3 --oneline
```

HTTPS fallback:

```bash
git clone https://github.com/munaimtahir/passport.git
```

The branch must be clean and synchronized with `origin/main`. The history must contain Sprint 24's
implementation commit `aa36327` and verification-status commit `700bed1`, followed by this device
plan commit or later commits.

## 3. Confirm Java, Gradle and ADB

Set `JAVA_HOME` to a JDK 17 installation if the shell does not already use it. Then run:

```bash
java -version
./gradlew --version
adb version
adb devices -l
```

Required observations:

- Java and the Gradle launcher JVM report version 17.
- Exactly one intended device is listed as `device`, not `offline` or `unauthorized`.
- API level is 36 for the mandatory pass:

```bash
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.model
adb shell getprop sys.boot_completed
```

`sys.boot_completed` must return `1`. If more than one target is attached, use `adb -s SERIAL` for
every ADB command and set `ANDROID_SERIAL=SERIAL` for Gradle's connected-test command.

## 4. Re-run host gates on the cloned workstation

```bash
./gradlew clean test lint assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2
```

Expected artifacts:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- test results under `app/build/reports/tests/`
- lint report under `app/build/reports/`

All commands must pass before installing. A release keystore is not needed for device qualification.

## 5. Run the connected suite

Keep the target unlocked and run:

```bash
./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=2
```

With multiple attached devices:

```bash
ANDROID_SERIAL=SERIAL ./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=2
```

Archive the HTML/XML output from `app/build/reports/androidTests/connected/` and
`app/build/outputs/androidTest-results/connected/`. The suite must execute—not merely compile—and
finish with zero failed tests. It includes the following Sprint 24 critical coverage:

- `UtilityLedgerIntegrationTest`: atomic creation, idempotency, edit/account move, deletion,
  balance, reports and attachment metadata cleanup.
- `DatabaseMigrationTest`: schema migrations including v13 to v14 with retained utility and
  finance rows.
- `UtilityBackupRestoreDeviceTest` and `UiDrivenBackupRestoreDeviceTest`: utility relationships,
  evidence and UI-entered data through encrypted backup/restore.
- `UtilityPaymentStatusDeviceTest`: paid status persistence across reconciliation.
- `UtilityAttachmentVaultTest`: encrypted attachment import/decrypt behavior.
- `NavigationSmokeTest`: reachable Home, Bills, Money and History shell.
- `SecurityLifecycleDeviceTest`, `AppPreferencesTest` and onboarding tests: PIN, relock, saved state
  and privacy preferences.
- The remaining database, reminder, notification, preview, performance and walkthrough tests.

If a test fails, preserve its report and log, reproduce it alone, fix the root cause, rerun the
single class, then rerun the full connected suite. Do not weaken assertions, disable tests or treat
an instrumentation APK build as an executed pass.

Example narrow rerun:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=pk.vexel.financepassport.core.database.UtilityLedgerIntegrationTest \
  --no-daemon --max-workers=2
```

## 6. Install a clean APK for manual testing

The connected suite clears test state. After it passes, create a fresh manual-test install:

```bash
adb uninstall pk.vexel.financepassport || true
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n pk.vexel.financepassport/.MainActivity
```

Uninstalling removes this app's local data. Do it only on the disposable target described above.
Complete onboarding with an empty dataset. Exercise both onboarding choices if time permits: skip
PIN for the finance flow, then configure a PIN from Settings for the security checks.

## 7. Manual unified-ledger scenario

Use the current date unless a field explicitly permits another date. Enter these synthetic records:

| Record | Values |
| --- | --- |
| Account 1 | `HBL Personal`; Bank; Personal / Home; opening balance `100,000` |
| Account 2 | `Test Cash`; Cash; Personal / Home; opening balance `0` |
| Income | `50,000`; HBL Personal; Salary; `Sprint 24 test income` |
| Manual expense | `10,000`; HBL Personal; Household; `Sprint 24 manual expense` |
| Utility | `Home Electricity`; Electricity; synthetic provider/reference |
| Current occurrence | bill amount `20,000`; current billing month |
| Utility payment | `20,000`; Paid From HBL Personal; Bank Transfer; synthetic reference |

Test in this order:

1. Confirm the top-level destinations are exactly Home, Bills, Money and History.
2. Open Money and create both accounts. Confirm account type, context and active state display.
3. Add the income and manual expense. Confirm grouping is displayed as `50,000` and `10,000`.
4. Open Bills, create Home Electricity and use its current monthly occurrence. Set its amount to
   `20,000`, choose **Mark Paid**, choose **HBL Personal** under **Paid From**, and save.
5. Return to Money. Confirm total available balance and HBL Personal balance are both `120,000`.
6. Confirm Activity contains exactly one income, one manual expense and one utility-origin expense.
   The utility expense must show Utilities/Electricity provenance, Home Electricity, the billing
   period and HBL Personal. Reopening the paid bill must not add another event.
7. Confirm Home shows current-month income `50,000` and expenses `30,000`, and that the bill is
   represented as paid. Confirm History shows the paid occurrence.

Expected arithmetic:

```text
HBL opening balance       Rs 100,000
Income                   +Rs  50,000
Manual expense           -Rs  10,000
Utility payment          -Rs  20,000
Expected HBL/total        Rs 120,000
```

## 8. Payment edit, account move and deletion

1. Edit the same utility payment from `20,000` to `18,000`. HBL Personal and total available must
   become `122,000`. Activity must still contain one utility expense with the same logical source.
2. Change **Paid From** to Test Cash. Expected balances are HBL Personal `140,000`, Test Cash
   `-18,000`, and total available `122,000`. No new utility expense may appear.
3. Delete the payment—not the bill. Expected balances are HBL Personal `140,000`, Test Cash `0`,
   and total available `140,000`. The linked utility expense must disappear, while the occurrence
   remains and returns to the appropriate unpaid/due/overdue state.
4. Reopen Bills, History and Money, then force-stop/relaunch. The same state must persist and no
   duplicate expense may be created.

```bash
adb shell am force-stop pk.vexel.financepassport
adb shell am start -n pk.vexel.financepassport/.MainActivity
```

## 9. Transfer check

With the payment still deleted, transfer `1,000` from HBL Personal to Test Cash.

- HBL Personal must become `139,000`.
- Test Cash must become `1,000`.
- Total available must remain `140,000`.
- Activity must show paired transfer-out and transfer-in lineage.
- Current-month income and expense totals must remain `50,000` and `10,000`; the transfer is
  neither income nor expense.

## 10. Backup/restore round trip

Before this test, recreate the `18,000` utility payment from HBL Personal and attach a small
synthetic image or PDF as payment proof if the UI permits. Record all account balances, Activity
rows, occurrence/payment state and attachment filename.

1. Open Settings and choose **Create Encrypted Backup**.
2. Use a synthetic password of at least eight characters and save the backup outside app-private
   storage (for example Downloads). Do not commit or upload the backup.
3. Clear only the app's data:

   ```bash
   adb shell pm clear pk.vexel.financepassport
   adb shell am start -n pk.vexel.financepassport/.MainActivity
   ```

4. Complete onboarding, open Settings, choose **Restore Encrypted Backup**, select the saved file,
   enter the password, then close and relaunch as instructed by the app.
5. Confirm both accounts, income, manual expense, transfer, utility profile, occurrence, payment,
   paid-from account, linked single utility expense and attachment all return with the same values.
6. Confirm the restored totals match the pre-backup totals and that reopening/relaunching does not
   duplicate the utility expense.
7. Separately verify a wrong password is rejected and leaves the current dataset intact.

## 11. Security, lifecycle and UI review

- In Settings, set an app PIN, background/foreground the app and confirm it relocks. Change the PIN
  using the current PIN, then remove it using the current PIN. Confirm biometric behavior remains
  available where the target supports it.
- Attempt a screenshot and inspect the recent-apps preview. Sensitive app content must be blocked
  or obscured by `FLAG_SECURE`. A black/blank screenshot is expected evidence, not a test failure.
- Toggle privacy masking and verify amounts are hidden and restored consistently on Home, Bills,
  Money and History.
- Rotate during an entry dialog, test system back, show/hide the keyboard, scroll long content,
  and inspect a long account/utility name.
- Check light and dark system themes, increased font size, and—where available—TalkBack traversal.
  Confirm controls remain reachable without clipped values or inaccessible actions.
- Verify July/August/September-style historical occurrences remain distinct if a time-controlled
  emulator can safely exercise reconciliation. Do not change a personal physical device's clock
  for this. At minimum, rely on the automated recurrence tests and verify existing History is not
  mutated by relaunch/reconciliation.
- Confirm there is no Reset Utility action.

Because screenshot protection is intentional, use connected-test reports, redacted log output and
a photograph of the test device (with synthetic data only) for UI evidence where necessary.

## 12. Logs and evidence

Start with a clean log buffer before each reproduction:

```bash
adb logcat -c
adb logcat -v threadtime > sprint24-device-logcat.txt
```

Stop log capture after the scenario, review it for crashes/ANRs, and redact any sensitive values
before sharing. Do not commit raw logs, device data, backups or screenshots containing financial
information.

Record the result in `docs/verification/SPRINT_24_DEVICE_RESULTS.md` using this minimum structure:

```markdown
# Sprint 24 Device Results

- Date/time:
- Tester:
- Commit SHA:
- Device/model:
- Android release/API:
- ADB transport: USB / Wi-Fi / emulator
- Fresh install: PASS/FAIL
- Connected suite: tests run / passed / failed / skipped
- Migration v13->v14: PASS/FAIL
- Unified ledger scenario: PASS/FAIL; observed balances
- Payment edit/account move/delete: PASS/FAIL; observed balances
- Transfer: PASS/FAIL
- Process death/relaunch: PASS/FAIL
- Backup/restore: PASS/FAIL
- PIN/biometric/FLAG_SECURE: PASS/FAIL
- UI/accessibility review: PASS/FAIL
- Defects and reproduction steps:
- Evidence paths:
- Final verdict: PASS/FAIL/BLOCKED
```

If everything passes, update `docs/verification/SPRINT_24_GATE.md`,
`docs/sprints/SPRINT_24_FINANCE_RECONNECTION.md`, `docs/BLOCKERS.md` and
`docs/RELEASE_LEDGER.md` with the device facts and evidence commit. Do not overwrite the historical
record of the original no-KVM blocker; mark it resolved and link the new result.

## Acceptance rule

Sprint 24 device qualification is PASS only when the API 36 connected suite executes with zero
failures and the manual financial scenario, edit/account move/delete flow, transfer, process-death,
encrypted backup/restore, PIN lifecycle and screenshot protection all pass with recorded evidence.
Any unresolved data-loss, duplicate-ledger, wrong-balance, migration or security failure keeps the
sprint verdict at FAIL or BLOCKED.
