# Wave C–E Final Verification

## Scope and verdict

This document records the independent verification work completed after the Wave A/B implementation. The dedicated `passport` emulator was used; `Android_15_Test` was excluded.

The normalized C/D/E domain primitives and database paths are implemented and covered by host/instrumentation tests. Wave C–E are **not accepted as complete** because the current application shell does not expose the required recurring-template, liability, receivable, or simple-investment UI workflows for mandatory running-app acceptance.

## Baseline

- Branch: `main`
- Starting commit: `5c89a827e57aa9f68e7c398b2a146e4541fb2309`
- Initial Room version: 15
- Current Room version: 16
- Version: 1.1.0 (`versionCode` 4)
- minSdk 26; compileSdk/targetSdk 36

## Device

- Name/profile: `passport` / Pixel 8 profile
- Serial: `emulator-5562`
- Android 16, API 36
- Physical display: 1080x2400, density 420
- The emulator was launched with wiped data, no snapshot, 4 cores, 4 GB RAM, and 8 GB data partition.

## Host gates

The following host gates passed after the C/D/E changes: Kotlin compilation, JVM tests, lint, debug APK assembly, and debug Android-test APK assembly. `git diff --check` passed.

## Instrumentation

Final command:

```text
ANDROID_SERIAL=emulator-5562 ./gradlew --no-daemon connectedDebugAndroidTest
```

Result: **100 executed, 100 passed, 0 failed, 0 skipped** on `passport(AVD) - 16`.

The targeted restore regression also passed after remediation.

## Remediation

The restore path now rebuilds derived utility bill status before returning. This fixed the intermittent full-suite mismatch where a restored fully paid occurrence displayed `Due soon`.

## Automated C/D/E coverage

- Recurring expectation/confirmation idempotency: PASS.
- Liability installment cash/principal/financing-cost split: PASS.
- Income-due versus money-lent settlement classification: PASS.
- Room migration 15→16: PASS in instrumentation coverage.
- Backup serialization includes the new normalized C/D/E records: PASS in source/test coverage.

## Running-app acceptance status

Final APK installation, clean launch, process verification, notification-permission handling, force-stop, and relaunch were exercised on `passport`; no app crash, Room failure, or SQLite failure was observed. The app shell currently exposes Home, Money, Capture, Bills, and History. It does not yet expose all mandatory C/D/E UI journeys, so the following cannot honestly be marked device PASS: recurring-template workflows, liability/receivable settlement workflows, and simple-investment workflows.

## Final verdict

**NOT ACCEPTED for complete Wave C–E acceptance.** Automated and database gates are green, but mandatory UI integration/device scenarios remain incomplete.
