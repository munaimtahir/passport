# Test and Runtime Evidence

Audit date: 2026-08-22  
Repository: `/home/munaim/srv/apps/passport`  
Branch: `hardening/internal-release-20260816`  
HEAD: `29f4bed9bf0e372b5893270c203b14b1e3bbfac3` (`local`, fast-forwarded from `origin/main`)

## Environment

| Item | Result |
| --- | --- |
| OS/kernel | Linux 6.17.0-1022-gcp x86_64 |
| Java | OpenJDK/Temurin 17.0.18 |
| Gradle wrapper | Gradle 8.13 distribution; `./gradlew tasks --all` passed in 1m23s |
| Android SDK | ADB 36.0.2; compile/target SDK 36 configured |
| Connected devices | None: `adb devices` returned only the header |
| Configured AVDs | None listed by `emulator -list-avds` |
| Physical device | Not available; no device actions performed |

## Commands run

| Command | Result | Evidence |
| --- | --- | --- |
| `git status --short` | PASS; only the three requested untracked audit drafts were present | Terminal output; no tracked production changes |
| `git rev-parse --show-toplevel` | PASS | `/home/munaim/srv/apps/passport` |
| `git branch --show-current` | PASS | `hardening/internal-release-20260816` |
| `git rev-parse HEAD` | PASS | `29f4bed9bf0e372b5893270c203b14b1e3bbfac3` |
| `git remote -v` | PASS | `origin` points to `git@github.com:munaimtahir/passport` |
| `./gradlew tasks --all` | VERIFIED PASS | `BUILD SUCCESSFUL`; root project `finance`, `:app` tasks present |
| `./gradlew clean assembleDebug --no-daemon --max-workers=2` | VERIFIED PASS | `BUILD SUCCESSFUL in 3m 20s`; debug APK produced at `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `e3e55eb3c4fbfb1343c1eeabc1ce989741941cc3bbfbb87c6c17bfd54684b6a` |
| `./gradlew test --no-daemon --max-workers=2` | VERIFIED PASS | `BUILD SUCCESSFUL`; 50 debug and 50 release JVM test cases reported, 0 failures/errors/skips |
| `./gradlew lint --no-daemon --max-workers=2` | VERIFIED PASS with warnings | `BUILD SUCCESSFUL in 2m 39s`; lint report at `app/build/reports/lint-results-debug.html`; warnings include deprecated backup attribute, dependency freshness, KTX suggestions, and Compose/dependency warnings |
| `adb devices` | NOT TESTED | No connected device |
| `emulator -list-avds` | NOT TESTED | No AVD configured/listed |
| `./gradlew connectedDebugAndroidTest --no-daemon --max-workers=2` | NOT TESTED | Build of test APK completed, then failed with `DeviceException: No connected devices!` |
| API 26 runtime scenario | NOT TESTED | No emulator/device |
| API 36 runtime scenario | NOT TESTED | No emulator/device |
| physical-device smoke | NOT TESTED | No physical device; none used |

## Automated test inventory

### JVM tests run

The 50 debug JVM tests passed. Test classes cover:

- `PkrMoneyInputTest`, `MoneyTest`, `FinancialEventTest`;
- `InvestmentDomainTest`, `BudgetMathTest`, `GoalMathTest`, `RecurringScheduleTest`;
- `TaxEngineTest`, `ReportsTest`, `DataExportTest`;
- `PinVerifierTest`, `PortableBackupTest`, `BackupPackageTest`, `LiveRestoreServiceTest`;
- `ReminderSchedulerTest`.

The tests meaningfully assert money arithmetic and parsing, transfer exclusion from income/expense totals, average-cost investment behavior, budget/goal math, deterministic structural tax generation, reconciliation arithmetic, encrypted backup round trip/wrong password/tamper rejection, report-domain construction, CSV/JSON fields, PIN derivation, and recurring date math. They do not prove the Android UI, Room behavior on a device, notification delivery, or process death.

### Instrumentation/Compose tests present but not run in this audit

Nine Android test files are present:

- `NavigationSmokeTest`;
- `MoneyCaptureDeviceTest`;
- `WealthCaptureDeviceTest`;
- `RecurringDraftDeviceTest`;
- `DocumentPreviewDeviceTest`;
- `AppDatabaseTest`;
- `DatabaseMigrationTest`;
- `BackupRestoreDeviceTest`;
- `ReminderDeviceTest`.

Their assertions are useful and include migration, database transactions, 10,000-event recent-query bounding, encrypted document preview, backup/restore package behavior, notifications, recurring non-posting behavior, and Compose capture smoke. Their current run status is NOT TESTED because the environment had no device. Existing `docs/verification/` claims that these passed on prior emulator runs are historical evidence, not this audit's runtime evidence; several prior documents also state API 36, accessibility, physical-device, and full workflow gaps remain.

## Runtime scenario status

The requested fresh-install/onboarding → money → wealth → tax → vault → reports → backup/restore → relock/accessibility scenario was not executed. No emulator data was created, cleared, or restored.

| Scenario | Status | Reason/evidence |
| --- | --- | --- |
| Fresh install and launch | NOT TESTED | No device; debug APK only built |
| PIN create/wrong PIN/biometric cancellation | NOT TESTED | JVM PIN test passed; UI/lifecycle not run |
| Whole-PKR examples and decimal rejection | VERIFIED PASS for parser/domain | `PkrMoneyInputTest`; UI/runtime not run |
| Two-account income/expense/transfer flow | IMPLEMENTED — UNVERIFIED | UI/repository paths exist; device test not run |
| Historical/backdated entry | NOT IMPLEMENTED in UI | Forms call defaults using `LocalDate.now()` and expose no date field |
| Wealth capture/maintenance | PARTIAL | Capture UI exists; maintenance services exist but most are not UI-wired |
| Tax source capture/draft/drill-down | PARTIAL | Source-linked entities and draft lines exist; full ruleset/issue/drill-down workflow absent |
| Vault import/preview/link/delete | PARTIAL | Encrypted storage and preview code exist; picker and dependency behavior not runtime verified |
| Required reports | PARTIAL | Export buttons and report domain exist; in-app previews, reconciliation report, and full CSV catalog are missing |
| Backup/restore equivalence | NOT TESTED | Package/restore code and JVM tests exist; no current device round trip |
| Notifications/recurring work | NOT TESTED | WorkManager code and JVM/device tests exist; no current device |
| Rotation/font scale/TalkBack/adaptive layout | NOT TESTED | No device or accessibility test run |

## Evidence paths

- Build outputs: `app/build/outputs/apk/debug/app-debug.apk`.
- JVM XML: `app/build/test-results/testDebugUnitTest/`.
- Lint: `app/build/reports/lint-results-debug.html` and `.txt`.
- Current source tests: `app/src/test/` and `app/src/androidTest/`.
- Prior, non-current evidence: `docs/verification/`, especially `HARDENING_2026-08-17.md`, `FINAL_VERIFICATION.md`, and `ACCEPTANCE_MATRIX.md`.

## Not tested / unavailable

No runtime claim is made for API 26, API 36, physical hardware, notifications, biometric hardware, Android file picker behavior, process death, rotation, TalkBack, font scaling, large-data startup, storage-full handling, interrupted restore, or actual UI backup/restore equivalence. The absence of a configured AVD is an environment limitation, not a product pass or fail.
