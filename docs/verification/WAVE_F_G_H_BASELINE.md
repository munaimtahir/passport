# Wave F/G/H Baseline

## Repository

- Branch: `main`
- HEAD at baseline review: `5c89a827e57aa9f68e7c398b2a146e4541fb2309`
- Worktree contains the previously recorded Wave A/B and C–E verification changes; these are preserved.
- Application: `pk.vexel.financepassport`, versionName `1.1.0`, versionCode `4`
- Room schema: version `16`
- Android: minSdk `26`, compileSdk/targetSdk `36`

## Device

- Dedicated target: `passport`, serial `emulator-5562`
- Android 16 / API 36
- 1080x2400, density 420
- `Android_15_Test` is excluded.

## Baseline gates

The pre-F/G/H host baseline completed with debug assembly, JVM tests, lint, and Android-test APK assembly passing. The connected suite on `passport` is recorded as 100/100 passed in the preceding verification report.

## Initial forensic findings

- Financial position, assets, liabilities, receivables, calendar items, reminders, documents, document links, tax records, and encrypted Vault storage already exist in the repository.
- Home already consumes a `FinancialPosition` Flow, but the position calculation still uses legacy investment events and its account aggregation requires review against the Wave F formula.
- Calendar/reminder persistence and scheduling exist, but the calendar is currently a generic manually-added/reminder projection and needs source-aware F/G coverage.
- DocumentVault already hashes and encrypts imported files, and `DocumentLink` supports shared links; deletion and link lifecycle require direct verification.
- The visible app shell now includes Position, Calendar, and Vault surfaces in addition to Home, Money, Bills, and History. Source-specific F/G/H actions remain only partially integrated.

## Gate status

Wave F/G/H implementation and acceptance are not yet complete. This baseline intentionally records gaps rather than treating existing entities as proof of integrated behavior.

## Latest verification rerun

- Host command: `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
- Host result: `BUILD SUCCESSFUL` (30 August 2026).
- Connected command: `ANDROID_SERIAL=emulator-5562 ./gradlew --no-daemon connectedDebugAndroidTest`
- Connected result: `BUILD SUCCESSFUL`; 100 tests executed, 100 passed, 0 failed, 0 skipped on `passport(AVD) - 16`.
- Device: `sdk_gphone64_x86_64`, Android 16/API 36, 1080x2400, density 420.
- Crash-focused logcat review: no `FATAL EXCEPTION`, `SQLiteException`, Room failure, or app ANR found in the reviewed tail.
