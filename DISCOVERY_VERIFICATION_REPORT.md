# Discovery Verification Report

Audit date: 2026-08-22
Product: Vexel Finance Passport
Scope: audit-only internal QA discovery. No production fixes or refactors were made.

## Executive summary

This checkout is a functioning single-module Android project, not an empty scaffold. It compiles, the current JVM tests pass, and lint passes with warnings. It contains Room schema version 8, repository-backed Compose screens, PIN/biometric/Keystore primitives, encrypted vault storage, tax/reconciliation primitives, reports/exports, WorkManager reminders, and backup/restore services.

The evidence-backed verdict is:

> NO-GO — internal release not ready.

Material reasons:

- The current environment had no connected Android device and no configured AVD. No current install, launch, Compose, instrumentation, notification, backup/restore, rotation, font-scale, or accessibility evidence was obtained.
- Home displays “Net recorded movement” from income minus expense. It is not canonical net worth because it excludes wealth/cash/investment/receivable scope.
- Forms have no user date field. Repository defaults use LocalDate.now(), so historical capture and reliable tax-year workflow are unavailable through the UI.
- Tax rules are a small hardcoded structural Kotlin map. JSON schema/parser/validator/version-history behavior is absent, and annual draft lines have no clickable source drill-down.
- Wealth maintenance services exist but are mostly dormant from UI. Investment events use a hardcoded manual account and no displayed unrealized valuation.
- Reports are export-oriented, have no in-app preview, omit required scope/report types, and format values with raw division by 100.
- Vault deletion removes links without a dependency warning or unlink/delete choice.
- Backup crypto and staging code exist, but UI restore reads the selected package into memory and no current device round trip proves equivalence.
- Privacy masking, accessibility evidence, and several expiry/due-date calendar workflows are absent.

No direct P0 financial corruption, irreversible data loss, or lock bypass was reproduced. Unresolved P1 blockers and an unpassed backup/runtime evidence gate require NO-GO under the supplied rules.

## Repository identity and audit environment

| Item | Finding |
| --- | --- |
| Root | /home/munaim/srv/apps/passport |
| Branch | hardening/internal-release-20260816 |
| HEAD | 29f4bed9bf0e372b5893270c203b14b1e3bbfac3 (`local`) |
| Starting worktree | Three pre-existing untracked audit drafts; no tracked production changes |
| Remote | origin: git@github.com:munaimtahir/passport |
| Gradle modules | Root finance, one app module |
| Application ID | pk.vexel.financepassport |
| Version | versionCode 1, versionName 0.1.0 |
| SDK | min 26, compile/target 36 |
| Java/Gradle | Temurin 17.0.18; wrapper 8.13 |
| Devices | adb devices: none; emulator -list-avds: none |
| Files changed by this audit | Only the three requested audit documents |

Prior docs in docs/verification and docs/delivery were treated as historical claims and checked against current source/build state.

## Commands run and outcomes

| Command | Outcome |
| --- | --- |
| git identity/status commands | PASS; identity above; clean at start |
| ./gradlew tasks --all | VERIFIED PASS; BUILD SUCCESSFUL in 1m23s |
| ./gradlew clean assembleDebug --no-daemon --max-workers=2 | VERIFIED PASS; BUILD SUCCESSFUL in 3m20s; APK hash recorded in TEST_AND_RUNTIME_EVIDENCE.md |
| ./gradlew test --no-daemon --max-workers=2 | VERIFIED PASS; BUILD SUCCESSFUL; 50 debug JVM and 50 release JVM test cases reported, 0 failures |
| ./gradlew lint --no-daemon --max-workers=2 | VERIFIED PASS; BUILD SUCCESSFUL in 2m39s; warnings only |
| adb devices | NOT TESTED; no device |
| emulator -list-avds | NOT TESTED; no AVD |
| ./gradlew connectedDebugAndroidTest --no-daemon --max-workers=2 | NOT TESTED; test APK assembled then failed with DeviceException: No connected devices |

Lint warnings include dependency freshness, deprecated android:allowBackup without modern extraction rules, KTX suggestions, obsolete SDK check, and Compose/dependency warnings.

## Architecture and implementation inventory

Connected implementation:

- MainActivity is the exported launcher, sets FLAG_SECURE, and requests notification permission.
- SecurityGate fronts PassportApp. PIN uses PBKDF2-HMAC-SHA256 and failure backoff. BiometricPrompt is offered when the device reports capability.
- PassportApp has Home, Money, Wealth, Tax & Records, Vault, plus More controls for exports, backup/restore, and delete-all.
- FinanceRepository exposes Room-backed Flows and write methods.
- AppDatabase is version 8. DatabaseProvider registers migrations 1→2 through 7→8 for production and restore validation.
- Entities exist for accounts, events, wealth records, recurring items, budgets, tax years/items/drafts/issues, reconciliation, calendar, documents/links, official records, and change-log rows.
- PkrMoneyInput enforces whole rupee input with optional valid grouping and stores rupees multiplied by 100.
- Transfers write paired signed rows and a transfer link in one Room transaction.
- Vault files use Keystore AES-GCM with SHA-256 associated data under app-private filesDir/vault.
- PortableBackupCrypto uses PBKDF2 and AES-GCM. BackupPackageService has byte-array and streaming creation paths. LiveRestoreService stages, validates, replaces, and rolls back the database.
- WorkManager schedules reminders and a unique daily recurring processor.

Present but partial or dormant:

- Asset valuation/disposal, liability payment, receivable payment, goal contribution, and budget methods exist, but most have no UI caller.
- calculateInvestmentPosition is tested but not called by a displayed holdings/valuation screen.
- addBudget and budget math exist but no budget UI exists.
- Official-record repository code exists, but full metadata/expiry/link behavior is not exposed.
- Annual draft and reconciliation buttons exist, but current-year/current-state implementations do not provide selected-year snapshots or drill-down.
- Report functions exist, but More writes exports directly and does not show in-app previews.

Absent/disconnected:

- Runtime ruleset JSON parser/schema validator and stored ruleset package repository.
- UI date pickers for financial, tax, wealth, or document dates.
- Global privacy eye/masking state.
- Source navigation from tax draft lines to originating records.
- Dependency-aware document deletion.
- Automatic document-expiry, receivable, tax-review, and monthly-review scheduling.
- Full structured export of every persisted table/history.
- Current Android runtime environment.

## Sprint 0–16 status summary

| Sprint | Status | Summary |
| --- | --- | --- |
| 0 | PARTIAL | Build/unit/lint pass; install/launch, CI/version catalog, and runtime gates are absent/unverified. |
| 1 | PARTIAL | Five destinations/theme exist; masking, adaptive layout, accessibility, and runtime evidence incomplete. |
| 2 | PARTIAL | Room v8/migrations/money tests exist; date UI, canonical fixtures, and initial-schema proof incomplete. |
| 3 | PARTIAL | PIN/backoff/biometric/Keystore/FLAG_SECURE exist; lifecycle/device/recovery evidence incomplete. |
| 4 | PARTIAL | Money/account/transfer paths exist; historical entry, filters, fields, process recreation, and device proof incomplete. |
| 5 | PARTIAL | Wealth entities and math exist; maintenance UI, holdings presentation, unrealized values, and device proof incomplete. |
| 6 | BROKEN | Home movement is not canonical net worth; required summaries and masking are missing. |
| 7 | PARTIAL | Encrypted vault primitives exist; dependency warning, tags/search/versioning, expiry automation, and runtime proof incomplete. |
| 8 | PARTIAL | Source/manual tax capture exists; remapping, taxonomy coverage, dates, and source navigation incomplete. |
| 9 | PARTIAL | Structural classifier exists; JSON schema/parser/validator/history and broad mappings absent. |
| 10 | PARTIAL | Persisted draft/issues/source IDs exist; selected year, full sections, drill-down, and lineage incomplete. |
| 11 | PARTIAL | Formula/history primitive exists; snapshots, full scope, adjustment reasons, missing-asset test, and drill-down incomplete. |
| 12 | PARTIAL | Report/PDF/CSV primitives exist; previews, complete scope, reconciliation report, disclaimers, and device evidence absent. |
| 13 | PARTIAL | Encrypted package/staging/rollback/export/delete code exists; current equivalence and full export coverage unverified/incomplete. |
| 14 | PARTIAL | Generic reminders/recurring worker exist; linked due-date/expiry/review work and runtime proof incomplete. |
| 15 | PARTIAL | Some empty/error/saveable state exists; accessibility, font, process-death, performance, privacy, and layout evidence absent. |
| 16 | PARTIAL | Internal release configuration exists; bundle/device/physical evidence absent. Public signing/Play/policy URL are deferred. |

See SPRINT_0_16_TRACEABILITY_MATRIX.md for every build item and gate row.

### Matrix status totals

The matrix contains 207 line items. Applicable internal-release rows are 205 after excluding one DEFERRED — PUBLIC RELEASE row and one NOT APPLICABLE row. Counts across all rows are: VERIFIED PASS 34 (including one configuration/inspection pass), IMPLEMENTED — UNVERIFIED 57, PARTIAL 56, BROKEN 4, NOT IMPLEMENTED 18, NOT TESTED 36, DEFERRED — PUBLIC RELEASE 1, and NOT APPLICABLE 1. No row is DEFERRED — POST-MVP in this sprint matrix. On the 205-row applicable denominator, VERIFIED PASS is 34/205 = 16.6% and IMPLEMENTED — UNVERIFIED is 57/205 = 27.8%. These are traceability-row percentages, not a product-quality score.

## Functional module findings

### Onboarding and security

The first screen is the PIN screen. There is no welcome/privacy/offline explanation sequence, display-name setup, initial tax/financial-year context, or guided account setup. Wrong-PIN backoff is implemented in PinStore; UI/device lifecycle, biometric cancellation, and recovery messaging are not current-runtime verified. No deep-link surface exists today.

### Home

HomeScreen shows movement, active account count, event count, reminders, and recent events. It does not calculate canonical net worth, comparison periods, module breakdowns, goals/budgets, or a complete readiness-dimensions model. There is no privacy mask control.

### Money

PkrMoneyInput tests prove 500→50000 minor units, 1,500→150000, grouped input behavior, rejection of 1.5 and 1500.50, and legacy fractional display. Accounts/events/transfers/categories/recurring drafts/edit/archive paths exist. Historical dates and complete account/transaction fields do not.

### Wealth

Asset, liability, investment, receivable, goal, and budget primitives exist. The UI is one compact multi-mode dialog with many defaults. Dates, funding source, ownership editing, lender/due date, partial receipts, valuation history, investment fee/withholding inputs, and goal contribution are not fully exposed. Several repository methods are dormant.

### Vault and official records

PDF/JPEG/PNG/WebP are validated, read, hashed, encrypted, and stored privately. Preview decrypts the whole file and uses a deleted temporary PDF for first-page rendering. Links are many-to-many in Room but current UI only links tax items. Delete removes links without a dependency warning.

### Tax capture, annual draft, reconciliation

Types and persistence represent source IDs, review/evidence states, draft lines, issues, and ruleset strings. The implementation is not a complete configurable tax engine. defaultPakistanStructuralRules contains a small structural list only; no runtime JSON validation, full taxonomy, original/final mapping lineage, selected year, or source navigation exists. Reconciliation hardcodes opening 0 and records closing as assets minus liabilities.

### Reports and exports

ReportGenerator implements named text/PDF reports and chunks long output. More opens SAF export directly. There is no in-app preview; reconciliation is not a report; JSON omits drafts/issues/reconciliations/calendar/recurring data and fields; current-year filtering does not filter assets/liabilities; values use raw division by 100 rather than canonical grouping.

### Backup/restore/delete-all

The internal-release ADR explicitly declines SQLCipher and accepts app-private Room plus Keystore field/file protection, with a pre-public-beta encryption prototype requirement. Portable backup has version header, salt/nonce, PBKDF2, AES-GCM, manifest, database, and encrypted files. Current UI restore uses readBytes and restore materializes ZIP entries; no current device equivalence or interrupted-restore proof exists.

### Calendar and UX

Generic reminders and unique recurring processing exist. Document expiry, receivable due-date, monthly review, and tax review automation are not connected. No current TalkBack, touch-target, color-independence, keyboard, rotation, font-scale, landscape, tablet, or process-death evidence exists.

## Security/privacy findings

Positive: PIN digest/backoff, Keystore AES-GCM for identifiers/vault, FLAG_SECURE, disabled automatic backup, authenticated portable backup, and no found analytics/ads/bank credentials/payment/FBR submission.

Risks: database is not independently encrypted; this is explicitly recorded in both ADR-001 files. No PIN-loss recovery workflow exists. Modern dataExtractionRules are absent and lint warns. Full document import and UI restore can buffer large payloads. Device evidence for relock, biometric cancellation, screenshot protection, and sensitive logging is absent.

## Tax-engine and reconciliation findings

TaxEngineTest proves deterministic structural output, ambiguous issue creation, and a pure zero-difference formula. It does not prove the required pipeline. Missing are ruleset validation, full event coverage, historical version association, split treatments, reasoned overrides, source navigation, selected-year generation, and repository scenarios for missing assets/adjustments.

The production reconciliation formula is explainable as opening + inflows - expenditure - outflows + adjustments, but current inputs do not represent the complete financial passport equation.

## Vault and backup/restore findings

JVM tests pass crypto round trip, wrong password, tamper rejection, package staging, and streaming package construction. LiveRestoreService uses shared migrations and a previous database for rollback. Current device tests did not run; per-file manifest hashes, referential-integrity counts, bounded restore memory, vault rollback, and interrupted restore remain unproven.

## UX/accessibility findings

There is no global privacy eye/masking state. Some icon content descriptions exist, but TalkBack, 1.3x/2.0x font, rotation, landscape/tablet, keyboard, touch-target, clipping, and process-death behavior are untested. Dense dialogs and hardcoded defaults create material workflow risk.

## Test inventory and coverage gaps

Current run: 50 JVM tests passed, zero failures/errors/skips. Nine Android test files exist but did not run. Gaps include UI security/onboarding, historical dates, complete wealth maintenance, source edit/remap, tax drill-down, reconciliation failure scenarios, all report opening/figures, backup equivalence, document dependency behavior, WorkManager permission/runtime, TalkBack, font scale, rotation, process death, and full synthetic-data performance.

## Performance/failure-injection findings

An instrumentation source test inserts 10,000 events and checks a bounded recent query. It was not run. No current measurement exists for 2,000 tax items, 1,000 documents, 10 tax years, Home startup, draft/report generation, or bounded restore. Corrupt ruleset, wrong/tampered backup, missing document, interrupted restore, storage full, malformed import, tax-year boundary, currency mismatch, deleted source, duplicate worker, and process death were not runtime tested.

## Contradictions between documentation and implementation

1. Prior verification docs claim API 26/API 36 or API 26 runs, while this environment has no device; those are historical claims only.
2. Prior docs describe Home/report hardening, but current Home is explicitly movement and reports omit required source scope.
3. Prior docs describe bounded-memory backup hardening, but UI restore uses readBytes and ByteArray restore APIs.
4. Specs require dependency-aware document deletion; current delete removes links immediately.
5. Sprint 9 requires JSON schema/parser/validator; current code has Kotlin structural rules only.
6. Requirements need historical dates/tax-year assignment; forms expose no date field and default to now.
7. BUILD_STATUS says no blocker prevents local QA, while BLOCKERS says device evidence remains required before a final internal verdict; this audit follows the final-verdict rules.
8. Two ADR-001 files have different dates but materially consistent decisions.

## Defect register by P0–P3

### P0

No P0 defect was reproduced. Backup/restore equivalence is an unpassed release gate because current runtime evidence is unavailable, not a reproduced data-loss result.

### P1

| ID | Defect | Evidence and impact |
| --- | --- | --- |
| P1-001 | No current Android runtime evidence | Empty adb/AVD; connected task fails with no devices. Core workflows cannot be claimed working. |
| P1-002 | Home net worth is broken/missing | HomeScreen computes income minus expense and labels it movement; wealth scope is excluded. |
| P1-003 | Historical capture/tax-year workflow absent | UI forms have no date and repository defaults use LocalDate.now. |
| P1-004 | Tax JSON configuration/validation absent | TaxDomain has only a small hardcoded structural map. |
| P1-005 | Annual draft source drill-down/lineage absent | Draft source IDs are stored, but no clickable source route or mapping-version lineage exists. |
| P1-006 | Wealth workflows materially incomplete | Maintenance methods lack callers; investment account is hardcoded manual; no displayed holdings/unrealized value. |
| P1-007 | Reports incomplete/inconsistent | No preview/reconciliation report; incomplete JSON; range and source scope are not canonical. |
| P1-008 | Document deletion lacks dependency warning | deleteDocument removes links in the transaction. |
| P1-009 | Backup/restore equivalence unproven | No current device; UI restore buffers full package. |
| P1-010 | Privacy masking/accessibility gates absent | No global masking; no TalkBack/font/rotation evidence. |

### P2

Inline Gradle versions and single-module concentration; whole-file import and restore buffering; no dataExtractionRules resource; missing tags/search/version replacement and activity filters; ungrouped report formatting; missing large-data measurements; generic loading/success recovery.

### P3

Dependency/deprecation warnings; final branding, production signing, Play declarations, and public privacy URL; SQLCipher/database prototype before public beta; post-MVP OCR, statement import, bank/brokerage integrations, FBR submission, household, and advanced intelligence.

## Internal-release readiness verdict

**NO-GO.**

The current build/unit/lint gates pass, but the final rules require all applicable core gates, accurate/reconciled financial/tax figures, safe backup/restore equivalence, security evidence, source drill-down, reports, and sufficient emulator/device evidence. P1 defects and missing evidence remain.

Public-release exclusions are not treated as internal blockers: production signing, store assets, Play declarations, public policy, bank/FBR/payment integrations, advertising, cloud analytics, and trading execution are deferred.

## Exact recommended remediation order

1. Provide disposable API 26 and API 36 AVDs; run connected tests and the redacted end-to-end scenario.
2. Correct Home to show true net worth separately from movement, with calculation drill-down and privacy masking.
3. Add historical date/tax-year selection and boundary tests.
4. Complete tax ruleset schema/parser/validator, taxonomy, mapping lineage, overrides, source navigation, evidence/duplicate issues, and selected-year drafts.
5. Complete wealth maintenance, holdings/unrealized values, snapshots, adjustment reasons, missing-asset test, and reconciliation drill-down.
6. Complete report previews, range/source scope, grouped formatting, reconciliation report, disclaimers, full export, and device checks.
7. Add dependency-aware deletion, duplicate/corrupt/missing-file, and expiry workflows.
8. Prove backup/restore equivalence, wrong/tampered/interrupted cases, vault rollback, and bounded memory on both API levels.
9. Run accessibility, process-death, low-memory, and synthetic-dataset gates.
10. Run release bundle/R8 verification and retain evidence.

## Evidence index

- Canonical docs: docs/00_README.md through docs/18_SOURCE_NOTES.md as applicable, docs/product_manifest.json, docs/tax_event_taxonomy.json, docs/20_DEFINITION_OF_READY.md.
- Implementation: app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt, ui/SecurityGate.kt, ui/MainViewModel.kt, core/database/FinanceRepository.kt, core/database/AppDatabase.kt, core/database/DatabaseProvider.kt, core/database/Entities.kt, core/database/Daos.kt, core/model/PkrMoneyInput.kt, core/taxrules/TaxDomain.kt, core/reports/Reports.kt, core/export/DataExport.kt, core/files/DocumentVault.kt, core/security, and core/calendar.
- Tests: app/src/test and app/src/androidTest.
- Current evidence: TEST_AND_RUNTIME_EVIDENCE.md, app/build/test-results/testDebugUnitTest, app/build/reports/lint-results-debug.html.
- Full line-item matrix: SPRINT_0_16_TRACEABILITY_MATRIX.md.
- Historical claims rechecked: docs/verification, docs/BUILD_STATUS.md, docs/FINAL_VERIFICATION.md, docs/BLOCKERS.md.
