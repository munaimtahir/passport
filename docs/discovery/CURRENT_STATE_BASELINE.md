# Vexel Finance Passport current-state baseline

Audit date: 2026-08-28. Discovery target: repository root `D:\Documents\github\passport`, branch
`main`, HEAD `e17e294a2fdb88013575d1f1d60420fab731ab59`. This report describes production code and observed
runtime, not historical specifications.

## Repository snapshot

- One Android application module: `:app`; package/namespace/application ID
  `pk.vexel.financepassport`.
- SDK: min 26, target/compile 36. Java/Kotlin bytecode target 17.
- Current staged application metadata: versionCode 4, versionName 1.1.0. This was a pre-existing
  staged change and was not made by discovery.
- Kotlin/plugin 2.3.20; AGP 8.13.2; Compose BOM 2026.06.01. Major libraries are Room 2.8.4,
  WorkManager 2.11.2, Navigation Compose 2.8.3, Lifecycle 2.9.4, Biometric 1.1.0, and Kotlin
  serialization 1.8.1.
- Build types: debug and minified release. Release signing reads ignored `keystore.properties` and
  falls back to debug signing if absent. No flavors and no dependency-injection framework.
- Room database `passport.db`, schema version 13, exported schemas 2 through 13, explicit migrations
  1->2 through 12->13, and no destructive-migration fallback.
- Test sets: `app/src/test` and `app/src/androidTest`. Documentation is under `docs/`, with release
  material in `docs/RELEASE_LEDGER.md`, `docs/RELEASE_SIGNING.md`, `docs/13_RELEASE_AND_PLAY_STORE.md`,
  and `docs/verification/INTERNAL_RELEASE_PACKAGE.md`.
- Initial git state: `main...origin/main [ahead 3]`; only `app/build.gradle.kts` was modified and
  staged (the pre-existing 1.0.3/3 -> 1.1.0/4 bump); no untracked files.

Evidence: `settings.gradle.kts`, root and app `build.gradle.kts`, `AndroidManifest.xml`,
`AppDatabase.kt`, `DatabaseProvider.kt`, and the initial Git capture in
`TEST_AND_RUNTIME_BASELINE.md`.

## Actual architecture

The application is a single-module, single-activity Compose application. It does not use a formal
feature-module or use-case layer.

```text
MainActivity
  -> OnboardingGate -> SecurityGate -> PassportApp
  -> one MainViewModel
  -> one FinanceRepository
  -> Room DAOs / app-private encrypted files / WorkManager / report and backup services
```

`PassportApplication` manually constructs `FinanceRepository(DatabaseProvider.get(this))` and
`AppPreferences`. `PassportApp.kt` contains the reachable utility shell and retained legacy Money,
Tax, Wealth, Vault, Calendar, report, and data-management composables. This is a mixed-responsibility
UI file rather than separated feature boundaries. `FinanceRepository.kt` similarly combines utility,
ledger, wealth, tax, reminders, documents, reports/export snapshots, backup, and deletion behavior.

## Product reality

### Evidence

The normal application exposes only Home, Bills, and History. Onboarding calls the product an
offline-first monthly utility bill tracker. Home, Bills, and History consume utility profiles,
monthly occurrences, payments, and attachments. Settings exposes encrypted backup, restore, and
delete-all. Legacy finance/tax/wealth/vault composables and backend remain compiled but are not
linked from the current shell.

### Interpretation

The repository currently represents a **utility bill and payment-history tracker built on top of a
retained personal-finance/tax backend**. Utilities are the sole normal product workflow. Tax is not
central to current navigation. The most mature reachable area is utility profile/occurrence/history
management. The largest inconsistency is the broad legacy backend and orphaned UI versus the narrow
current product shell.

## Master feature status matrix

`Runtime` uses PASS, PARTIAL, FAIL, or NOT TESTED. WORKING requires connected persistence evidence;
otherwise source-only functionality is classified more conservatively.

| Product area | Backend/Data | UI | Navigation | Persistence | Tests | Runtime | Overall | Evidence/notes |
|---|---|---|---|---|---|---|---|---|
| Onboarding | preferences | complete utility flow | entry gate | yes | unit/device | PASS | WORKING | `Onboarding.kt`; fresh API 36 run |
| Security | PIN, biometric, Keystore, masking | unlock + privacy toggle | entry/top bar | yes | unit/device | PARTIAL | IMPLEMENTED - NEEDS VERIFICATION | PIN-skip runtime; biometric not exercised |
| Home | utility flows | dashboard | bottom nav | yes | UI tests | PASS | WORKING | counts and attention list |
| Accounts | Room/repository | retained screen | orphaned | yes | DAO/E2E legacy tests | NOT TESTED | ORPHANED | `MoneyScreen` not routed |
| Income/expenses | ledger repository | retained dialogs | orphaned | yes | unit/DAO | NOT TESTED | ORPHANED | income creates tax item; expense does not |
| Transfers | paired events/link | retained dialog | orphaned | yes | database tests | NOT TESTED | ORPHANED | transactional paired movements |
| Utilities | full v13 model | complete | bottom nav | yes | unit/device | PASS | WORKING | profile creation and occurrence persistence observed |
| Reset Utility | none | none | none | n/a | none | NOT TESTED | NOT FOUND | History has filter reset only |
| Recurring finance | repository/worker | retained Money UI | orphaned | yes | unit/device | NOT TESTED | ORPHANED | reminders/drafts; confirmation creates event |
| Subscriptions | category string only | legacy recurring category | orphaned | limited | no focused test | NOT TESTED | PARTIAL | no subscription entity/workspace |
| Calendar | entity/WorkManager | retained screen | orphaned | yes | unit/device | NOT TESTED | ORPHANED | utility reminders run in background |
| Assets/liabilities/receivables/goals | entities/repository | retained Wealth UI | orphaned | yes | unit/DAO | NOT TESTED | ORPHANED | substantial backend, no current route |
| Investments/net worth | events/calculator/reports | retained Wealth UI | orphaned | yes | unit/report | NOT TESTED | ORPHANED | cost basis only; no live valuations |
| Vault/official records | encrypted vault/models | retained Vault/Tax UI | orphaned | yes | device tests | NOT TESTED | ORPHANED | utility attachments remain reachable separately |
| Tax capture/rules/drafts/reconciliation | substantial v13 subsystem | retained Tax UI | orphaned | yes | unit/DAO/device | NOT TESTED | ORPHANED | two bundled rulesets; not reachable normally |
| Reports/export | generators/export service | dead controls in MoreDialog | no usable control | service output only | unit tests | NOT TESTED | BACKEND ONLY | launchers exist but no report/export buttons rendered |
| Backup/restore | encrypted package + live restore | Settings | top-bar dialog | yes | unit/device | PARTIAL | IMPLEMENTED - NEEDS VERIFICATION | UI visible; round trip not repeated in this audit |
| Settings/delete-all | preferences/repository | compact dialog | top bar | yes | device tests | PASS | WORKING | dialog observed; destructive action not invoked |

## Actual workflows

- **WF-01 Register utility:** Bills -> Add Bill -> `AddBillDialog` -> `MainViewModel.addUtilityProfile`
  -> `FinanceRepository.addUtilityProfile` -> `UtilityBillDao.upsert`; then reconciliation creates
  monthly occurrences. Source + runtime verified.
- **WF-02 Maintain occurrence:** Profile/history -> occurrence details -> set amount/actual dates,
  skip/unskip, or save -> `MonthlyBillOccurrenceDao.update`. Source verified.
- **WF-03 Pay utility:** occurrence -> Mark Paid -> `PaymentRecordDao.insert` plus occurrence status
  update to Paid and reminder cancellation. Source verified; not completed manually in this audit.
- **WF-04 Attach proof:** occurrence/payment details -> Android picker -> `UtilityAttachmentVault`
  -> Keystore AES-GCM file plus `BillAttachmentEntity`. Source and existing device-test evidence.
- **WF-05 Browse history:** History -> search/filter -> occurrence details. Source + runtime verified.
- **WF-06 Archive/reactivate/delete utility:** profile details -> status update/reactivation or
  cascade delete. Source verified. Archive retains existing cycles; delete is destructive.
- **WF-07 Backup/restore:** Settings -> password -> encrypted package/export or open/restore through
  `LiveRestoreService`. Source and existing automated coverage; not rerun end-to-end here.
- **WF-08 legacy money/wealth/tax/vault/report workflows:** implementations exist but have no normal
  entry point in the utility shell, so they are not current user workflows.
- **WF-Reset Utility:** not supported; no such action exists.

## Reset Utility conclusion

**Status: NOT FOUND as a production feature.** “Reset” describes the repository's product-scope
reset and is also the label on a History filter-clearing button. It does not mutate a utility.
Monthly continuity is implemented by inserting a distinct `(profileId, billingYear, billingMonth)`
occurrence when missing. Prior occurrences, payments, amounts, statuses, and attachments are not
cleared by cycle generation. Utility payments do not create `FinancialEventEntity` rows and do not
affect account balances or legacy reports. See `RESET_UTILITY_DISCOVERY.md`.

## Major observed gaps

- No Reset Utility operation despite terminology in the request; no reset-to-next-cycle workflow.
- Utility profiles/occurrences/payments have no account/domain link and never create ledger expenses.
- Utility attachments use an untyped `linkedId` without a foreign key; profile deletion warns that
  attachments are deleted, but the database cascade cannot delete `bill_attachments` rows/files.
- Normal navigation hides all retained money, wealth, calendar, tax, vault, and report functionality.
- Report/export launchers are defined in `MoreDialog`, but its rendered controls only expose backup,
  restore, and delete-all.
- Utility category constants disagree across entity comments, add UI, filters, history, and legacy
  recurring UI (notably Water, Internet, Mobile/Telephone).
- Build verification is blocked by the installed JDK 25.0.2 before Gradle project tasks execute.

## Verification performed

- Static inspection of all production/test source files, Room configuration/schema exports, build
  configuration, manifest, security/storage, release files, and current Git state.
- Test inventory: 76 JVM tests across 21 files; 64 Android tests across 17 files.
- `assembleDebug test lint` attempted together and failed before tasks with `25.0.2`; no code was
  changed in response.
- API 36 x86_64 Pixel 8 AVD, Android 16, 1080x2400: installed the pre-existing release APK,
  cleared app data, completed onboarding without PIN, created `DiscoveryUtility`, observed its
  automatically generated August 2026 overdue occurrence, verified History display and persistence
  after force-stop/relaunch. No application crash was observed.

