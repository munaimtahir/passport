# Vexel Finance Passport — Current Repository Discovery Report

Session type: READ → INSPECT → BUILD → TEST → VERIFY → REPORT (discovery only; no production code
changed). Full evidence log with exact commands/output: `docs/verification/DISCOVERY_2026-09-06.md`.

All claims below were reproduced against HEAD `5c89a827e57aa9f68e7c398b2a146e4541fb2309` in this
session. Where a claim could not be reproduced (device/emulator testing — no hardware
virtualization available in this environment), that is stated explicitly and the most recent real
evidence is cited with its commit distance from HEAD.

---

## 1. Executive Verdict

### `NO-GO — remediation required before further feature development`

The repository **builds** (`assembleDebug`, `bundleRelease` both succeed) and a large fraction of
it was genuinely device-verified as recently as 2026-08-29 — but that verification is **12 commits
behind current HEAD**. The commit at HEAD ("Complete Wave A and B: Financial Spine and Bills
Integration") bumped the Room schema to v15 and changed `FinancialEventEntity`'s constructor shape,
and in doing so:

1. **Broke `./gradlew test`** — the entire JVM unit test module fails to *compile* (not just fails
   assertions), so 0 of 78 discovered unit test methods can currently run.
2. **Broke `./gradlew lint`** — 2 new lint errors (`SuspiciousIndentation`) in the same commit's
   `MoneyScreen` changes cause the lint gate to fail outright.
3. Was **never run against a device/emulator** — the commit's own audit document
   (`docs/verification/WAVE_A_B_FINAL_AUDIT.md`) states "Emulator: SKIPPED (No devices attached to
   `adb`)" while still concluding "Mega Sprint is fully completed and audited."

None of this is environmental: this session independently reproduced both failures from a clean
`./gradlew clean` and identified the exact root cause in the diff (see §19). The fixes are narrow
and well-understood (3 test files' constructor call sites, 2 indentation lines), but per this
repo's own mandatory process ("a sprint is not done when it compiles — it's done when its gate
passes," `CLAUDE.md`), that remediation has not happened yet, and two of the four standard gate
commands currently fail from a clean checkout. Separately (not new to this commit, but material to
the verdict), five of the nine documented product pillars — Wealth, the general Document Vault, the
Tax engine, and Reports/export — have working backend/domain code but **no reachable UI path** at
all (see §5, §16).

Device/instrumentation testing could not be independently re-run this session: this container has
no `/dev/kvm` and no CPU `vmx`/`svm` flags, so the emulator cannot accelerate (confirmed via
`emulator -accel-check`). This matches the limitation already recorded in `docs/BLOCKERS.md` and is
classified as **External/Environment**, not a repository defect.

---

## 2. Repository Identity

| Field | Value |
| --- | --- |
| Root | `/home/munaim/srv/apps/passport` |
| Branch | `main` |
| HEAD | `5c89a827e57aa9f68e7c398b2a146e4541fb2309` |
| Latest commit message | "Complete Wave A and B: Financial Spine and Bills Integration" (2026-08-30 00:03:56 +0500) |
| Remote | `git@github.com:munaimtahir/passport` (fetch/push) |
| Tracking | `main` tracks `origin/main`; 0 ahead / 0 behind |
| Working tree | Clean; no staged/unstaged changes, no untracked files, no stashes |
| Tags | One tag, `safety-phase0-start-20260821`, **100 commits behind HEAD** (not at HEAD) |
| Total commits | 110 |

---

## 3. Application Version

| Field | Value | Source |
| --- | --- | --- |
| `applicationId` / `namespace` | `pk.vexel.financepassport` | `app/build.gradle.kts` |
| `versionCode` | **4** | `app/build.gradle.kts` |
| `versionName` | **1.1.0** | `app/build.gradle.kts` |
| `minSdk` | 26 | `app/build.gradle.kts` |
| `targetSdk` | 36 | `app/build.gradle.kts` |
| `compileSdk` | 36 | `app/build.gradle.kts` |
| Java toolchain | 17 (source/target compatibility) | `app/build.gradle.kts` |
| Kotlin | 2.0.21 (Gradle-reported); Compose compiler plugin via `org.jetbrains.kotlin.plugin.compose` | `./gradlew --version` |
| AGP | Resolved via `com.android.application` plugin (version pinned in the version catalog/plugin block, not printed as a separate string by this build) | `app/build.gradle.kts` |
| Gradle wrapper | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |
| Compose BOM | `2026.06.01` | `app/build.gradle.kts` |
| Room | 2.8.4 | `app/build.gradle.kts` |
| Other key libs | Navigation-Compose 2.8.3, WorkManager not present (no dependency), Biometric 1.1.0, kotlinx-serialization 1.8.1 (force-resolved), Lifecycle 2.9.4 | `app/build.gradle.kts` |

Single-module Gradle project (`rootProject.name = "finance"`, `include(":app")` only — see §4).
No build-variant version divergence: one `defaultConfig` block, debug/release share the same
version numbers, differing only in signing/minification.

**Repository vs. last documented release:** `docs/RELEASE_LEDGER.md`'s newest entry is v1.1.0
(versionCode 4), commit `aa363275...` + "device qualification fixes," device-qualified 2026-08-29.
Current HEAD is **12 commits past** that ledger entry (Wave A/B, schema v14→v15) with no version
bump and no new ledger entry — i.e., **current source is ahead of the last documented/qualified
release** and that gap includes an un-device-tested schema migration.

---

## 4. Current Architecture

### Expected (per `CLAUDE.md` / `docs/19_INITIAL_REPOSITORY_STRUCTURE.md`)
`:app`, `:core:model`, `:core:database`, `:core:security`, `:core:files`, `:core:taxrules`,
`:core:ui`, `:core:testing`, `:feature:{onboarding,home,money,wealth,tax,records,vault,reports,
backup,settings}`.

### Actual
A **single Gradle module**, `:app`, with `core.*` as plain Kotlin packages (not Gradle modules)
under `pk.vexel.financepassport.core`:

```
core/calendar    — ReminderScheduler.kt
core/database    — AppDatabase.kt, Entities.kt, Daos.kt, DatabaseProvider.kt, FinanceRepository.kt,
                   UtilityRecurrenceEngine.kt
core/export      — DataExport.kt
core/files       — DocumentVault.kt, UtilityAttachmentVault.kt
core/model       — Money.kt, FinancialEvent.kt, FinancialContext.kt, FinancialPosition.kt,
                   BudgetMath.kt, GoalMath.kt, InvestmentDomain.kt, TaxReadiness.kt,
                   UtilityCategory.kt, RecurringSchedule.kt, MoneyInput.kt, PkrMoneyInput.kt
core/reports     — Reports.kt
core/security    — AppPreferences.kt, BackupPackage.kt, CryptoService.kt, LiveRestoreService.kt,
                   PinStore.kt, PinVerifier.kt, PortableBackup.kt
core/taxrules    — BundledTaxRulesets.kt, TaxDomain.kt, TaxRulesetJson.kt
ui               — MainActivity.kt (root), PassportApp.kt (1991 lines — nearly the entire UI layer),
                   MainViewModel.kt, Onboarding.kt, SecurityGate.kt, DateField.kt
ui/components    — BillRhythmStrip, FinancialAttentionCard, FinancialTimelineRow, LivingBillCard,
                   VexelCaptureTray, VexelEmptyState, VexelStatusChip
ui/theme         — Color, Shape, Theme, Type (Vexel Design Language 2.0)
```

No `feature:*` modules exist. `PassportApp.kt` is a single 1991-line file containing nearly every
screen, dialog, and composable in the app.

### Classification of deviation
**Evolved implementation, acceptable in principle, with one real cost.** A single-module,
package-organized structure is explicitly sanctioned by `CLAUDE.md` ("a smaller module count is
acceptable if package boundaries stay clean"). The package boundaries under `core/` are clean and
match the documented domain split (model/database/security/files/reports/taxrules/calendar). The
real cost is `PassportApp.kt` itself: nearly 2,000 lines of UI in one file is not a module-count
problem but a file-size/maintainability one, and it is plausibly what caused this session's lint
regression (indentation errors deep in a large `when`/dialog block) and makes positional-constructor
mismatches (§19) harder to catch by eye.

`CLAUDE.md` itself, `AGENTS.md`, and `README.md`'s "Structure" section describing `feature/{money,
wealth,tax,vault,...}` modules are all **stale relative to the actual repository** — see §17.

---

## 5. Current Feature Matrix

Verification column reflects what this session could independently confirm. "Device-verified
pre-HEAD" means real emulator evidence exists but is 12 commits behind current HEAD (schema v14,
not v15) and was not re-run this session (no hardware acceleration available).

| Area | Implementation | Verification | Notes |
| --- | --- | --- | --- |
| Onboarding | Implemented | Device-verified pre-HEAD | `Onboarding.kt`, `OnboardingDeviceTest.kt` |
| Navigation | Implemented, narrow | Confirmed by source inspection | Only 4 reachable destinations: Home, Money, Bills, History (see §16) |
| Design system (Vexel DL 2.0) | Implemented | Device-verified pre-HEAD | `ui/theme/*`, `ui/components/*`, `docs/design/VEXEL_DESIGN_LANGUAGE_2.md` |
| Privacy masking | Implemented | Device-verified pre-HEAD | `LocalPrivacyMode`, `MaskedPkr` |
| Settings | Implemented | Device-verified pre-HEAD | PIN mgmt, backup/restore, delete-all in `MoreDialog` |
| Money: accounts/income/expense/transfers/categories/contexts | Implemented | **Not verified at HEAD** (JVM tests don't compile; androidTest not run this session) | Wave A/B added `contextId` + Unassigned Reconciliation UI |
| Income sources | Implemented, reachable | Device-verified pre-HEAD | Picker + inline create in income dialog |
| Bills/utilities (recurring) | Implemented | Device-verified pre-HEAD (schema v14); Wave B modernization (1:1 payment↔event) added at HEAD, compiles, **not device-run** | `UtilityRecurrenceEngine.kt`, 14 invariant tests (`INV-B01`–`B14`) |
| Loans & debts (Liabilities) | Implemented (backend/domain + unit tests) | **Present but unreachable** — no UI screen | `LiabilityEntity`, zero references in `ui/` |
| Receivables | Implemented (backend/domain + unit tests) | **Present but unreachable** | `ReceivableEntity`, zero references in `ui/` |
| Savings/investment planning (Goals, Investments) | Implemented (backend/domain + `GoalMathTest`, `InvestmentDomainTest`) | **Present but unreachable** | `GoalEntity`, `InvestmentEventEntity`, zero references in `ui/` |
| Net worth at a glance | Partially implemented | Confirmed by source inspection | `FinancialPosition` calculation is canonical and correct, but since assets/liabilities/investments/receivables have no capture UI, the figure always equals account balances only |
| Document vault (general) | Implemented (backend) | **Present but unreachable** | `DocumentVault.kt` imported in `PassportApp.kt` but never instantiated/called |
| Document attachments (bill-scoped) | Implemented, reachable | Device-verified pre-HEAD | `UtilityAttachmentVault.kt`, used from Bills flow |
| Continuous Tax Capture | Implemented (backend: classifier, ruleset, mapping lineage) | **Present but unreachable** | `MainViewModel.taxItems`/`getMappingHistory` exposed but never read in `ui/` |
| Reports (in-app/PDF/CSV) | Implemented (backend: `Reports.kt`, `DataExport.kt`) with JVM test coverage | **Present but unreachable, and currently untestable** | `MoreDialog` declares `requestedReport`/`previewReport`/`pendingReportExport` state but renders no control that uses them; `ReportsTest.kt`/`DataExportTest.kt` are 2 of the 3 files causing the current test-compile failure |
| Backup / Restore | Implemented, reachable | Device-verified pre-HEAD (schema v14) | Wave A/B updated backup record-count logic for the new `financial_contexts` table; **not device-verified against schema v15** |
| Security (PIN/biometric/relock/Keystore/FLAG_SECURE) | Implemented | Device-verified pre-HEAD | No explicit inactivity timer found (relock is on `ON_STOP` only); no deep links exist, so deep-link lock enforcement is N/A |
| Notifications/calendar reminders | Implemented | Device-verified pre-HEAD | `ReminderScheduler.kt`, `ReminderDeviceTest.kt`, `NotificationDeliveryDeviceTest.kt` |

---

## 6. Development / Wave Timeline

| Stage | Git evidence | Documentation evidence | Current status |
| --- | --- | --- | --- |
| Original tax-capture-first MVP (Sprints 0–16) | Early history, `docs/verification/SPRINT_00..16_GATE.md` | `docs/BUILD_STATUS.md` (2026-08-17, stale) | Superseded |
| Remediation phases 0–9 (canonical net worth, JSON tax rules, wealth reconciliation fixes) | `docs/IMPLEMENTATION_COMPLETE_DEVICE_VERIFICATION_PENDING.md` | Explicitly "device verification pending" at the time | Superseded by later resets |
| Product reframe: "personal-finance-diary" (Sprints 17–23) | v1.0.3 ledger entry, commit range ending `2d41b45` area | `docs/RELEASE_LEDGER.md` v1.0.3 | Device-verified (67/67), superseded |
| **Reset to utility-bill-tracker shell** | `5ec5677`, `603e198 refactor: reset visible app shell to utility bill tracking` | `docs/UTILITY_TRACKER_*` docs | Deliberate product-shell reset; Money/Wealth/Tax/Vault hidden |
| Utility tracker build-out (profiles, recurrence, payments, attachments, history) | `f4fcb72`…`e17e294` | `docs/discovery/*`, `docs/verification/phase1–13*.md` | Implemented, device-verified at the time |
| Sprint 24: reconnect utility payments to finance ledger | `aa36327 Reconnect utility payments to the finance ledger` → `e15f9fe Qualify Sprint 24 on API 36 emulator` | `docs/sprints/SPRINT_24_FINANCE_RECONNECTION.md`, `docs/verification/SPRINT_24_GATE.md`, `SPRINT_24_DEVICE_RESULTS.md` | **Device-verified**: 68/68 connected tests, 78/78 host tests, API 26/35/36, 2026-08-29 |
| Graphical redesign: Vexel Design Language 2.0 | `b6dca3b`, `e6d0356` | `docs/design/*`, `docs/verification/GRAPHICAL_FINAL_DEVICE_ACCEPTANCE.md` | Device-verified, same 2026-08-29 window, schema v14 |
| Release packaging | `b109859`…`e25a25e` | `docs/RELEASE_LEDGER.md` v1.1.0 (versionCode 4) | Production-signed AAB built and hashed in that session (not reproducible here — no keystore) |
| **Wave A & B: Financial Spine + Bills Integration (HEAD)** | `5c89a82` | `docs/architecture/WAVE_A_B_DISCOVERY_MAP.md`, `docs/verification/WAVE_A_B_*.md` | Schema v14→v15; **compiles for androidTest, but breaks `test` and `lint`; never device-run** (own audit admits this) |

**Current HEAD is 12 commits ahead of the last commit that has real device evidence**, and those 12
commits include a schema migration that has not been exercised on a device.

---

## 7. Build Results

| Command | Result | Notes |
| --- | --- | --- |
| `./gradlew clean` | PASS | 1m20s |
| `./gradlew assembleDebug` | PASS | 5m59s; 2 harmless Kotlin warnings (`PassportApp.kt:742,746`, unnecessary safe calls) |
| `./gradlew compileDebugAndroidTestKotlin` | PASS | 44s; only deprecation warnings (`createAndroidComposeRule` v1) |
| `./gradlew test` | **FAIL** | 1m37s; `compileDebugUnitTestKotlin` fails — whole JVM test module does not compile (§19) |
| `./gradlew lint` | **FAIL** | 5m49s; 2 `SuspiciousIndentation` errors abort the build (§19) |
| `./gradlew bundleRelease` | PASS | 10m11s; debug-signed fallback (no `keystore.properties` in this environment) |
| `connectedDebugAndroidTest` | **NOT EXECUTED** | No device/emulator available — no `/dev/kvm`, no `vmx`/`svm` CPU flags (environment limitation, matches `docs/BLOCKERS.md`) |

---

## 8. Test Inventory

```
Unit test files (app/src/test):          17 files,  78 @Test methods
Instrumentation test files (androidTest): 19 files,  97 @Test methods
Ignored/disabled (@Ignore/@Disabled):     0
Parameterized/generated:                  0 found
Estimated executable total:               175 methods (78 JVM + 97 instrumentation)
```

Historical count from `docs/verification/GRAPHICAL_FINAL_DEVICE_ACCEPTANCE.md` (12 commits behind
HEAD) was 78 unit + 68 instrumentation (18 classes). The Wave A/B commit at HEAD added
`FinancialSpineInvariantsTest.kt` (14 methods, `INV-A01`–`A14`) and `BillModelModernizationTest.kt`
(14 methods, `INV-B01`–`B14`), consistent with the growth from 68 → 97 instrumentation methods
(19 classes) observed now. The JVM unit test count is unchanged at 78 (one existing file,
`FinancialEventTest.kt`, was edited in place for the new constructor shape; no new JVM test file was
added for the Wave A/B change itself).

---

## 9. Test Execution Results

```
JVM unit tests:
  Executed:  0 / 78     (compileDebugUnitTestKotlin fails — see §19)
  Passed:    0
  Failed:    0 (compile error, not an assertion failure)
  Skipped:   0
  Process deaths: n/a

Instrumentation tests:
  Executed:  0 / 97     (no device/emulator available in this environment)
  Passed / Failed / Skipped: n/a
  Process deaths: n/a

Lint:
  2 errors (SuspiciousIndentation), 24 warnings, 3 hints — build FAILS
```

Most recent **real** execution evidence (12 commits behind HEAD, schema v14, 2026-08-29):
78/78 JVM unit tests passed (debug and release variants each), 68/68 instrumentation tests passed
on API 26, API 35, and API 36 emulators (`docs/verification/GRAPHICAL_FINAL_DEVICE_ACCEPTANCE.md`).
That evidence does not cover HEAD's schema v15 migration or the Wave A/B invariant tests.

---

## 10. ADB / Device Verification

No device or emulator was available in this session (`adb devices -l` returned empty; one AVD
definition, `Sprint24_API_36`, exists but cannot be hardware-accelerated on this host —
`emulator -accel-check` reports "KVM requires a CPU that supports vmx or svm", and `/dev/kvm` does
not exist). No install/launch/logcat smoke test could be performed this session. This is an
**Environment** classification, not a code defect — it matches the identical limitation already
recorded in `docs/BLOCKERS.md` §1.

---

## 11. Release Build Status

| Field | Value |
| --- | --- |
| `bundleRelease` result | PASS (debug-signed fallback; no `keystore.properties` present in this environment) |
| AAB path | `app/build/outputs/bundle/release/app-release.aab` |
| AAB size | 5,693,705 bytes |
| AAB SHA-256 | `5cf69df79d85c3e280d929026dfc1f1cccdd5abea837d89ab7e015d1ed070080` |
| versionCode / versionName | 4 / 1.1.0 (unchanged, as required) |
| Signing status | Debug-signed in this session (release keystore not provisioned here); does **not** match any hash in `docs/RELEASE_LEDGER.md`, which reflects a session where the real release key was present |
| Prior release artifacts | `docs/RELEASE_LEDGER.md` v1.1.0 entry (production-signed, 2026-08-29) is 12 commits behind current HEAD and predates the schema v15 migration — no release artifact exists yet for current HEAD |

No signing secrets, passwords, or key material were read or printed during this session.

---

## 12. Acceptance-Test Status

Mapped against `docs/15_ACCEPTANCE_TEST_CATALOG.md` groups, using the most recent real evidence
available and this session's own findings:

| Group | Status |
| --- | --- |
| Onboarding | Automated + device-verified pre-HEAD; not re-run at HEAD |
| Money | Automated coverage exists; **not run at HEAD** (JVM suite won't compile; androidTest not executed this session) |
| Wealth | Domain-level automated coverage only; **acceptance not reachable** — no UI path exists to exercise assets/liabilities/investments/receivables end-to-end |
| Tax / annual draft / reconciliation | Domain-level automated coverage only; **acceptance not reachable** — no UI path to the tax inbox/draft/reconciliation |
| Vault (general) | Backend implemented; **acceptance not reachable** — no UI path |
| Reports | Backend implemented with JVM tests; **acceptance not reachable**, and its own tests are currently 2 of the 3 files breaking `./gradlew test` |
| Security | Automated + device-verified pre-HEAD; not re-run at HEAD |
| Backup | Automated + device-verified pre-HEAD (schema v14); **not verified against schema v15** |
| Data ownership (delete-all, export) | Automated + device-verified pre-HEAD |
| Notifications | Automated + device-verified pre-HEAD; not re-run at HEAD |

---

## 13. Database / Migration Status

- Current schema version: **15** (`DATABASE_VERSION` constant, `AppDatabase.kt`), 28 entities.
- Migration chain: `MIGRATION_1_2` through `MIGRATION_14_15`, **14 consecutive migrations,
  version 1 → 15, no gaps** (`DatabaseProvider.kt`).
- Exported schema JSON present for versions 2–15 (`app/schemas/.../AppDatabase/`); version 1's
  schema file is not present in the export directory, but this does not break the migration chain
  since `MIGRATION_1_2` is defined and covered by `DatabaseMigrationTest`.
- `fallbackToDestructiveMigration`: **not used anywhere** in the codebase (verified by grep).
- The v14→v15 migration (adding `financial_contexts` and altering `financial_events` /
  `utility_bill_profiles` / `payment_records`) compiles and is exercised by
  `DatabaseMigrationTest` (androidTest, compiles) but **has not been run on a device** in this
  session or, per `docs/verification/WAVE_A_B_FINAL_AUDIT.md`'s own admission, in the session that
  authored it either.
- Financial architecture: `PaymentRecordEntity` links 1:1 to `FinancialEventEntity` via
  `financialEventId` (Wave B enforces this as a strict invariant, `INV-B01`–`B14`); no
  second/competing ledger exists. `FinancialEvent` now carries `contextId` linking to the new
  `FinancialContextEntity` (Personal/Professional slicing).
- Source-to-tax duplication protection: `TaxMappingEntity` records `SYSTEM_GENERATED`/
  `USER_OVERRIDE` lineage with supersession on reclassification (per historical remediation-phase
  documentation; not independently re-verified this session since the Tax UI is unreachable).

---

## 14. Backup / Restore Status

Implemented and reachable via the Settings ("More") dialog: `BackupPackage.kt`, `PortableBackup.kt`,
`LiveRestoreService.kt`, `CryptoService.kt` (AES-GCM via Android Keystore). Password-protected,
with a manifest recording schema version, per-document SHA-256 hashes, and record counts (Wave A/B
added the new `financial_contexts` table's row count to the manifest's `recordCount`). Room backup
uses `VACUUM INTO` where available with an API-26-safe fallback, per prior verification docs.
JVM-level coverage exists (`BackupPackageTest.kt`, `PortableBackupTest.kt`, `LiveRestoreServiceTest.kt`
— all currently blocked from running by the unrelated test-compile failure in §19, since they live
in the same Gradle test source set). Device-level coverage (`BackupRestoreDeviceTest.kt`,
`UtilityBackupRestoreDeviceTest.kt`, `UiDrivenBackupRestoreDeviceTest.kt`) passed against schema v14
on 2026-08-29; **not re-verified against schema v15**.

---

## 15. Security Status

| Control | Status |
| --- | --- |
| PIN (PBKDF-based) + failed-attempt exponential delay | Implemented (`PinStore.kt`, `PinVerifier.kt`) |
| Biometric prompt | Implemented (`SecurityGate.kt`), shown when available and a PIN exists |
| Background relock | Implemented — relocks on `Lifecycle.Event.ON_STOP` when a PIN exists |
| Inactivity timer (elapsed-time-based) | **Not found** — only background-event relock exists, no elapsed-timeout mechanism |
| Deep-link lock enforcement | N/A — no deep links are declared anywhere in the manifest |
| Android Keystore + AES-GCM | Implemented (`CryptoService.kt`) |
| Screenshot protection | Implemented — `FLAG_SECURE` set in `MainActivity.kt` |
| Sensitive/production logging | None found — zero `Log.`/`println` calls in `app/src/main` |
| Exported components | Only the single launcher `MainActivity` is exported; no other components declared |
| Permissions | Minimal — `POST_NOTIFICATIONS` only; no `INTERNET` |
| Analytics/ads/tracking SDKs | None found in `app/build.gradle.kts` |
| Android auto-backup | Disabled (`android:allowBackup="false"`, custom `dataExtractionRules`/`backup_rules` excluding all app data) |
| Hardcoded secrets/credentials | None found in a source/config grep; keystore material is gitignored and not present in this checkout |

---

## 16. Reports / Export Status

Backend implemented (`core/reports/Reports.kt`, `core/export/DataExport.kt`) with canonical
net-worth reuse (`FinancialPosition`) and JVM test coverage (`ReportsTest.kt`, `DataExportTest.kt`).
**No UI control renders or triggers report generation/export/preview.** `MoreDialog` in
`PassportApp.kt` declares the necessary state (`requestedReport`, `previewReport`,
`pendingReportExport`, a `reportSnapshot()` suspend function) but never wires them to a visible
`Button`/control — confirmed by grepping for those identifiers across the file and finding only
their declarations. This is the same condition recorded as gap `G-008` in
`docs/discovery/CURRENT_GAP_REGISTER.md` and it is still true at HEAD. Additionally, `ReportsTest.kt`
and `DataExportTest.kt` are 2 of the 3 files causing the current `./gradlew test` compile failure
(§19), so this feature's only test coverage is presently non-executable.

---

## 17. Documentation Drift

- **`CLAUDE.md` and `AGENTS.md` both describe a repository state that no longer exists.** Both say
  "no Android application code" / "no Gradle project is checked in yet." In reality there are 110
  commits, a full Gradle Android project, 175 discovered test methods, and multiple production
  release artifacts recorded in `docs/RELEASE_LEDGER.md`. This is the single largest documentation
  drift in the repository and should be corrected before any future session trusts these files at
  face value.
- **`README.md`'s "Structure" section** describes `feature/*` and `core/*` modules; only `core/*`
  packages (not modules) exist — see §4.
- **`docs/BUILD_STATUS.md`** is dated 2026-08-17 and describes Sprint 16-era gate status ("PARTIAL"
  across the board) — stale by roughly 90 commits' worth of subsequent work (utility-tracker reset,
  Sprint 24 reconnection, graphical redesign, Wave A/B). It should either be updated or retired in
  favor of `docs/RELEASE_LEDGER.md` / `docs/verification/WAVE_A_B_*.md` as the current source of
  truth.
- **`docs/verification/WAVE_A_B_FINAL_AUDIT.md`** claims "Mega Sprint is fully completed and
  audited" and "All static analysis and Kotlin incremental compilations are clean" while its own
  "Emulator: SKIPPED" line and this session's independent `./gradlew test`/`lint` runs show
  otherwise. Self-reported completion should not be trusted without an independent gate re-run —
  exactly the premise of this discovery session.
- **`docs/RELEASE_LEDGER.md`** is otherwise well-maintained and accurate for what it documents
  (v1.0.2/1.0.3/1.1.0), but has no entry for the Wave A/B work at HEAD — expected, since no new
  release has been cut from it yet, but worth flagging so a future session doesn't assume the
  ledger's newest entry describes current HEAD.
- **`docs/discovery/CURRENT_GAP_REGISTER.md`** (dated around Sprint 24, 2026-08-28) remains mostly
  accurate at HEAD: G-007 (most destinations unreachable), G-008 (reports unreachable), and the
  general shape of "Wealth/Tax/Vault compiled but unreachable" are all still true today, 12 commits
  later. G-002/G-003 (utility payment ↔ financial event linkage) are more thoroughly addressed by
  the Wave A/B work than the register's "Sprint 24 disposition" section describes, though without
  device verification.
- **Features in code but understated in docs:** the `financial_contexts`
  (Personal/Professional) slicing and "Unassigned Reconciliation" UI added in Wave A/B are not yet
  mentioned in any user-facing doc (README, product docs).

---

## 18. Outstanding TODO / FIXME / Disabled Tests

- No `TODO`, `FIXME`, `HACK`, `XXX`, `TODO()`, or `UnsupportedOperationException`-as-placeholder
  markers found in `app/src/main`, `app/src/test`, or `app/src/androidTest`.
- No `@Ignore`/`@Disabled` tests found anywhere.
- No `fallbackToDestructiveMigration` usage found.
- Matches for "fake"/"bypass" were false positives on inspection: a test fixture literal
  (`"fake encrypted payment proof bytes"` in `UtilityBackupRestoreDeviceTest.kt`) and a doc comment
  in `SecurityLifecycleDeviceTest.kt` clarifying that *no* lock-screen bypass exists in the app
  ("Confirmed by inspection, not a test against...").
- No hardcoded credentials, PINs, or API keys found in production source.

---

## 19. Known Defects

### High

1. **`./gradlew test` does not compile at HEAD.** Root cause: HEAD's own commit added a
   `contextId: String? = null` parameter to `FinancialEventEntity`'s constructor (7th position)
   and updated one JVM test file (`FinancialEventTest.kt`) for the new shape but not three others
   (`DataExportTest.kt`, `BudgetMathTest.kt`, `ReportsTest.kt`) that construct the entity
   positionally. Result: the entire `debugUnitTest`/`releaseUnitTest` module fails to compile;
   0 of 78 JVM unit tests can run. **Fix scope:** update the 3 call sites to the new positional
   order (or switch to named arguments), then re-run `./gradlew test` and the full gate.
   File/lines: `app/src/test/java/pk/vexel/financepassport/core/export/DataExportTest.kt:12`,
   `core/model/BudgetMathTest.kt:18`, `core/reports/ReportsTest.kt:22,55`.
2. **`./gradlew lint` fails at HEAD.** 2 `SuspiciousIndentation` errors in
   `ui/PassportApp.kt:1605,1877`, introduced by the same commit's `MoneyScreen` "Unassigned
   Reconciliation"/context-filter additions. Low functional risk but blocks the lint gate as
   configured (errors abort the build; no baseline file is configured to permit known issues).
3. **The Wave A/B schema v15 migration and its Bills-modernization invariants have never been run
   on a device/emulator**, by this session's own inability to do so and by the authoring session's
   own admission (`docs/verification/WAVE_A_B_FINAL_AUDIT.md`: "Emulator: SKIPPED"). A Room
   migration and a new "1 payment : 1 financial event" invariant are exactly the kind of change
   this repo's own rules require device verification for before being considered done.

### Medium

4. **Reports/export feature has no reachable UI** (§16) — implemented, tested (when compiling),
   completely unreachable by a user. Same for Wealth (assets/liabilities/investments/receivables/
   goals), the general Document Vault, and the Tax inbox/draft/reconciliation (§5). Five of the
   app's nine documented core pillars are effectively invisible in the shipped UI.
5. **No inactivity/elapsed-time relock timer** — only lifecycle-`ON_STOP` relock exists. A user who
   leaves the app foregrounded and idle (e.g., screen stays on) is never relocked.

### Low

6. 24 lint warnings / 3 hints: 7 `UseKtx` (SharedPreferences KTX suggestion), 6 `GradleDependency` +
   4 `NewerVersionAvailable` + 1 `AndroidGradlePluginVersion` (dependency version bumps available),
   2 `ObsoleteSdkInt`, 2 `MonochromeLauncherIcon`, 1 `UnusedResources`.
7. 2 harmless Kotlin compiler warnings (unnecessary safe calls, `PassportApp.kt:742,746`).

### Informational

8. `app/schemas/.../AppDatabase/1.json` is absent from the exported-schema directory (versions 2–15
   are present). Does not affect migration correctness (the migration chain starts at
   `MIGRATION_1_2` and is tested), but is worth a look if the schema-export directory is meant to be
   a complete historical record.

---

## 20. External Blockers

- **Production release signing key** is gitignored and not present in this environment
  (`keystore.properties` absent) — expected and by design; `bundleRelease` correctly falls back to
  debug signing rather than failing.
- **Hardware-accelerated emulation is unavailable in this container** (no `/dev/kvm`, no CPU
  `vmx`/`svm`) — pre-existing, documented in `docs/BLOCKERS.md`, blocks all device/instrumentation
  verification in this specific environment. Does not block host-side build/lint/unit-test gates
  (which fail for source reasons, not this environmental one).
- **Play Console access, final branding assets, and public privacy-policy URL** — external
  release-readiness items, unchanged from prior documentation, not evaluated further this session
  since they are out of scope for a code/build discovery.

---

## 21. Phase-2 Readiness

| Phase 2 capability | Existing prerequisites | Missing prerequisites | Readiness |
| --- | --- | --- | --- |
| On-device document extraction | `DocumentVault.kt`, encrypted file storage, Keystore crypto | No reachable Vault UI to attach extraction results to; general Vault is currently unreachable (§5) | **NOT READY** |
| Statement import | Room schema, `FinancialEventEntity` (now with `contextId`), account model | No import pipeline, no validation/duplicate-detection scaffold, and the entity shape just changed underneath any such pipeline | **NOT READY** |
| CSV import templates | `DataExport.kt` already defines canonical CSV field shapes (export direction) | No import-direction code exists at all; its own export-side tests are currently broken (§19) | **NOT READY** |
| Document-to-tax suggestions | `TaxDomain.kt`, `TaxRulesetJson.kt`, bundled ruleset, `TaxMappingEntity` lineage | Tax Inbox itself has no UI (§5) — building suggestions on top of an unreachable feature has no user-facing payoff yet | **BLOCKED** |
| Recurring transaction drafts | `RecurringItemEntity`, `RecurringSchedule.kt`, `UtilityRecurrenceEngine.kt` (proven pattern for bills) | Not extended to general (non-bill) financial events; no UI | **READY WITH MINOR PREREQUISITES** — the bills recurrence engine is a solid, device-proven template to generalize |

**Overall: the repository is not yet ready for Phase 2.** The more fundamental gap is that the MVP
itself is only partially *reachable* — Wealth, Tax, and the general Vault (three of the eight
"Smarter Capture" dependencies) have no UI surface today, and the two host-side quality gates
(`test`, `lint`) currently fail at HEAD. Phase 2 features that build on top of Tax or Wealth have no
foundation to attach to yet in the shipped UI, independent of their backend completeness.

---

## 22. Recommended Next Development Boundary

**Do not start Phase 2.** The next sprint/wave should begin from:

1. **Fix the two broken host gates** (bounded, well-diagnosed, low-risk):
   - Update `DataExportTest.kt:12`, `BudgetMathTest.kt:18`, `ReportsTest.kt:22,55` to the current
     `FinancialEventEntity` positional shape (or convert to named arguments to prevent recurrence).
   - Fix the two `SuspiciousIndentation` sites in `PassportApp.kt:1605,1877`.
   - Re-run `./gradlew clean test lint assembleDebug` and confirm all four pass together.
2. **Get real device evidence for the Wave A/B schema v15 migration and Bills-modernization
   invariants** on a KVM-capable host (this session's environment cannot do this) — run
   `connectedDebugAndroidTest` on at least one emulator/device before treating Wave A/B as done.
3. **Decide, deliberately, what to do about the five unreachable pillars** (Wealth, general Vault,
   Tax, Reports — Money/Bills are reachable). Options are: (a) build the missing navigation/UI to
   expose them, (b) formally mark them post-MVP/deferred in product docs so future sessions stop
   treating them as "implemented," or (c) remove genuinely dead code (e.g., the unused
   `DocumentVault` import, the unused report-preview state in `MoreDialog`) if a decision is made
   not to expose them soon. This should be a product decision, not made silently in a future
   commit.
4. **Refresh `CLAUDE.md` and `AGENTS.md`** to reflect the actual repository state (single `:app`
   module, 110 commits of history, current feature reachability) so future sessions — human or
   AI — don't start from the false premise that "no Android application code" exists.
5. Only after (1)–(2) pass cleanly and (3) is a deliberate decision (not an accident) should a new
   `docs/RELEASE_LEDGER.md` entry and any Phase 2 planning begin.
