# Vexel Finance Passport — Autonomous Build Handoff

**Handoff date:** 2026-08-14  
**Repository:** `/home/munaim/Documents/github/passport`  
**Product:** Vexel Finance Passport  
**Application ID:** `pk.vexel.financepassport`  
**Current verdict:** `NO-GO — INTERNAL RELEASE NOT READY`  
**Build paused by request:** yes

## 1. Purpose and governing instructions

This document is the restart brief for the next engineering agent. It records the implementation and verification state at the moment work was paused. It does not replace the product contract.

Before making changes, read the complete master prompt at:

`Master Autonomous Build Prompt — Vexel Finance Passport.md`

Also read `docs/14_MASTER_AI_AGENT_BUILD_PROMPT.md`, `AGENTS.md`, `docs/BUILD_STATUS.md`, `docs/FINAL_VERIFICATION.md`, all product documents `00_README.md` through `20_DEFINITION_OF_READY.md`, `product_manifest.json`, and `tax_event_taxonomy.json`. The master prompt requires autonomous progress through Sprint 0–16, evidence for every gate, autofix/retest after failures, and a final evidence-backed GO/NO-GO verdict. Do not mark a partial gate as passed.

## 2. Exact pause checkpoint

The repository is on branch `main` at commit `3f41a0e` (`Refresh categorized release artifact`) with additional uncommitted Sprint 14/15/16 work in the working tree. No reset, checkout, force-push, or destructive cleanup was performed.

The latest targeted command was:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=pk.vexel.financepassport.ui.MoneyCaptureDeviceTest
```

It completed successfully on the connected `Android_26_Test` emulator (API 26 / Android 8.0). The test had initially failed because the new recurring-drafts section pushed the activity row below the composed viewport. The test was repaired to scroll the `LazyColumn` to the Activity section before asserting the saved salary event, and the targeted run then passed. The full suite has **not** been rerun after this repair. The new recurring-draft device test has also not yet been run to a passing result after the final test changes.

On the subsequent resume attempt, the recurring-draft test was improved in three ways: it now creates its prerequisite account on a clean install, waits for the `Add recurring draft` dialog explicitly, and targets stable test tags for the draft fields. An earlier attempt correctly exposed that the action is disabled when no account exists. A later API 26 run was started with the prerequisite-account fix but was intentionally interrupted by the user before a result was available; it must be rerun from the current worktree. No source changes were lost.

Current connected device:

```text
emulator-5554    device
```

## 3. Working-tree changes that must be preserved

The following changes are intentionally unfinished and must be reviewed, built, tested, and either committed or corrected:

- `app/src/main/java/pk/vexel/financepassport/core/database/AppDatabase.kt`
  - Room database version raised from 6 to 7.
  - `RecurringItemEntity` and `RecurringItemDao` added.
- `app/src/main/java/pk/vexel/financepassport/core/database/Daos.kt`
  - Active recurring-item observation, upsert, pause, and retrieval support.
- `app/src/main/java/pk/vexel/financepassport/core/database/DatabaseProvider.kt`
  - Explicit `MIGRATION_6_7` creates `recurring_items` and its indexes.
- `app/src/main/java/pk/vexel/financepassport/core/database/Entities.kt`
  - Recurring draft entity added.
- `app/src/main/java/pk/vexel/financepassport/core/database/FinanceRepository.kt`
  - Recurring drafts persist a future reminder and calendar item.
  - They must not silently create confirmed financial events.
  - Pause cancels the reminder and marks the item paused/cancelled.
- `app/src/main/java/pk/vexel/financepassport/core/security/LiveRestoreService.kt`
  - Restore migration chain updated through schema 7.
- `app/src/main/java/pk/vexel/financepassport/ui/MainViewModel.kt`
  - Recurring-item state and add/pause actions exposed.
- `app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt`
  - Money screen displays recurring drafts and supports creating/pausing them.
  - Money event fields now have test tags to make device tests deterministic.
- `app/src/androidTest/java/pk/vexel/financepassport/core/database/DatabaseMigrationTest.kt`
  - Migration verification updated from v2→v6 to v2→v7.
- `app/src/androidTest/java/pk/vexel/financepassport/ui/MoneyCaptureDeviceTest.kt`
  - Event fields use explicit tags and the test scrolls to Activity before assertions.
- `app/src/androidTest/java/pk/vexel/financepassport/ui/RecurringDraftDeviceTest.kt`
  - New device test for creating a prerequisite account, creating a recurring draft, and verifying its reminder text.
  - Uses explicit dialog synchronization and field test tags.
- `app/schemas/pk.vexel.financepassport.core.database.AppDatabase/7.json`
  - New exported Room schema; validate it before committing.

Do not discard these changes merely to recover a green pre-v7 baseline. The recurring-draft feature is part of the required product behavior, but it is not complete until its migration, unit/instrumentation behavior, offline behavior, and full regression evidence pass.

## 4. What is implemented and verified

The repository is no longer an empty scaffold. The following foundations exist in the single Android app module:

- Kotlin, Jetpack Compose, Material 3, Room, repository/ViewModel layering, coroutines/Flow, WorkManager, DataStore/security services, and SDK configuration of min 26 / compile 36 / target 36.
- Offline-first onboarding/security gate with PIN creation and verification, PBKDF2 PIN derivation, optional biometric unlock, lifecycle/background relock, `FLAG_SECURE`, and local privacy masking foundations.
- Keystore-backed AES-GCM encryption for sensitive values and Vault document contents; SHA-256 document hashes; app-private file storage; PDF/image first-page preview; document metadata and many-to-many links.
- Exact PKR minor-unit arithmetic and currency-aware domain values; Room persistence; explicit migration tests through schema 6 in the last committed baseline, with schema 7 currently being added.
- Money accounts, opening balances, income, expenses, transfers, adjustments, account archive/edit behavior, optional categories, account movement/balance aggregates, and source-linked tax candidates.
- Transfer invariant coverage: transfers are paired movements and do not inflate income or expense totals.
- Assets, valuation/disposal actions, liabilities and repayments, investment events, receivables and partial receipts, goals, and net-worth surfaces.
- Calendar items, reminder scheduling/rescheduling/cancellation, notification channel setup, and WorkManager notification rendering.
- Continuous tax-capture foundations: source-linked tax items, manual items, tax-year assignment, structural versioned ruleset loading/classification, review/reclassification, evidence state, exclusion reasons, duplicate-candidate warnings, annual draft persistence, draft lines, source links, and reconciliation primitives.
- Wealth reconciliation persistence and deterministic calculation tests, including a balanced fixture expected to produce zero unexplained difference.
- JSON and CSV export services, canonical report generation, PDF report generation, encrypted backup packaging, staged/live restore, hash verification, and delete-all repository behavior.
- Database-side aggregates and a seeded 10,000-event regression check to avoid loading complete history for Home/Money summary calculations.

Historical verification already recorded in the repository includes:

- `./gradlew test lint` passing before the current schema-7 device-test work.
- `./gradlew connectedDebugAndroidTest` passing 22 tests on API 36 and 22 tests on API 26 in the schema-v6 baseline.
- Release R8 build, install, and launch passing on API 36 and API 26 in the previous baseline.
- Backup/restore device evidence for populated data, encrypted document bytes, hashes, links, and Room reopening; API 26 WAL-safe fallback evidence.
- Device coverage for navigation, account/salary capture, wealth capture, Vault image/PDF preview, migrations, tax review/source traceability, backup/restore, reminders, and notification behavior.

The detailed historical evidence is in `docs/verification/SPRINT_00_GATE.md` through `SPRINT_16_GATE.md`, `docs/verification/ACCEPTANCE_MATRIX.md`, and the current `docs/FINAL_VERIFICATION.md`. Treat those reports as historical evidence; update them when schema 7 and recurring drafts are actually verified.

## 5. Sprint-by-sprint status

The repository deliberately records incomplete gates as `PARTIAL` or `IN PROGRESS`. The table below is the restart summary; the linked gate file is the authoritative evidence for each stage.

| Sprint | Current status | Completed work | Still required |
|---|---|---|---|
| 0 — Foundation | PASS | Gradle project, SDK/package setup, Compose shell, Git/docs baseline, exact Money type, initial tests, debug install/launch. | Nothing gate-critical; preserve baseline evidence. |
| 1 — Design/navigation | PARTIAL | Theme, semantic palette, five primary destinations, More action, empty states, value hierarchy, icon descriptions. | Full Compose tests, font scaling, rotation, responsive/layout review. |
| 2 — Local data | PARTIAL | Room canonical model, constraints, repositories/DAOs, money arithmetic, transfer transactions, migration chain through v6. | Validate and document schema 7; complete migration evidence and broader database gate. |
| 3 — Security | PARTIAL | PIN/PBKDF2, Keystore AES-GCM, biometric integration, lifecycle relock, failed-attempt backoff, `FLAG_SECURE`. | Full lock-state instrumentation, biometric hardware/cancel evidence, release log/security review. |
| 4 — Money | PARTIAL | Accounts, income/expense, categories, transfers, derived balances, edit/archive implementation, source tax creation. | Recurring drafts, broader edit/archive UI walkthroughs, full transfer/device coverage. |
| 5 — Wealth | PARTIAL | Assets, liabilities, investments, receivables, goals, valuation/disposal/repayment/partial receipts, deterministic position tests. | Device walkthroughs for all lifecycle actions and goal contribution behavior. |
| 6 — Home | PARTIAL | Repository-backed summaries, masking, recent activity, Room-side aggregates, 10,000-event dataset check. | Year-start comparison, seeded UI performance, responsive/font-scale evidence. |
| 7 — Vault/records | PARTIAL | SAF import path, MIME/size checks, encrypted files, hashes, previews, links, official-record masking/encryption. | Full SAF picker, replacement/version behavior, complete link/delete walkthrough. |
| 8 — Tax capture | IN PROGRESS | Canonical event→single TaxItem transaction, tax inbox, evidence states, review/exclusion/reclassification, draft source links. | Tax-year boundaries, full annual workflow, recurring source behavior, end-to-end device review. |
| 9 — Rules engine | PARTIAL | Bundled structural rules, version identity, deterministic classification, ambiguity issues, immutable facts. | Full JSON schema validation and override UI; never invent unsupported rates/legal mappings. |
| 10 — Annual workspace | PARTIAL | Persisted drafts/issues/lines, ruleset metadata, calculation/source drill-down surfaces. | Complete annual sections, evidence/duplicate review, draft version comparison. |
| 11 — Reconciliation | PARTIAL | Deterministic primitive, zero-difference fixture, persisted calculations. | Component drill-down, reasoned adjustments, complete annual UI. |
| 12 — Reports | IN PROGRESS | Canonical report models, paginated PDFs, report catalog, CSV export, date-range filtering, source IDs. | Device opening/pagination/CSV verification and full report acceptance coverage. |
| 13 — Backup/restore | IN PROGRESS | Authenticated package, hashes, staging/rollback, SQLite/WAL-safe paths, populated restore equivalence tests. | Full UI export/restore/delete-all walkthrough and final schema-7 equivalence. |
| 14 — Calendar | PARTIAL | Calendar records, WorkManager scheduling/cancel/reschedule, notification channel/permission, device firing tests. | Recurring-draft reminders and complete UI lifecycle walkthrough. |
| 15 — UX hardening | IN PROGRESS | Navigation smoke, lifecycle relock, basic font/rotation smoke, privacy controls. | TalkBack, accessibility, loading/errors, visual, large-data and responsive review. |
| 16 — Release | PARTIAL | R8 release build, local debug-signed APK, API 26/API 36 install/launch, prior regression suite. | Fresh schema-7 release build, full regression, security/release audit, acceptance closure, external signing inputs. |

The existing gate documents contain the exact commands and historical defects for each row. A new agent must not convert this table to all-PASS without new evidence.

## 6. What remains incomplete

The following are not release-ready and must not be described as complete:

### Immediate technical verification

1. Run `./gradlew test lint` against the final schema-7 working tree.
2. Run the complete connected suite, not only `MoneyCaptureDeviceTest`, on API 26 and API 36.
3. Run `RecurringDraftDeviceTest` explicitly and verify that creating a draft adds only `RecurringItemEntity` plus a calendar/reminder record, not a confirmed `FinancialEventEntity` or duplicate tax item.
4. Complete and pass the v2→v7 Room migration test, including live restore migration.
5. Rebuild the release artifact after schema 7; the previous APK hash (`89716a2f...`) predates this work and must not be presented as the current release candidate.

### Product/workflow gaps

- Full annual tax workspace review remains shallow: complete section coverage, issue navigation, source drill-down, evidence checklist, override history, and regeneration/versioning need device evidence.
- Wealth reconciliation UI needs complete contributor drill-down and reasoned adjustment workflows.
- Vault needs full SAF picker/import walkthrough, replacement/version handling, multi-object linking coverage, and dependency-safe deletion evidence.
- Edit/archive/disposal/repayment/partial-receipt/investment lifecycle UI flows require broader device coverage.
- Recurring drafts need pause/reschedule behavior, frequency coverage, reminder cancellation, process-restart persistence, and clear confirmation semantics.
- Goals need deterministic contribution/progress behavior if required by the product pack.
- Reports need device opening/PDF pagination/CSV scope verification across the documented catalog.
- Backup/export/delete-all need complete UI-level end-to-end evidence, even where service-level tests already pass.
- Accessibility, TalkBack order, content descriptions, 48dp actions, large-font layout, landscape/expanded-width layout, visual hierarchy, and privacy masking need systematic review.
- Performance evidence for the documented large dataset needs to be recorded on an actual emulator/device, including lazy lists, off-main-thread work, document streaming, and deterministic draft generation.

### External release inputs

These may be parked but must remain documented in `docs/BLOCKERS.md` and `docs/DEFERRED_USER_DECISIONS.md`:

- permanent production signing key;
- final application icon/branding asset;
- public privacy-policy URL and final privacy/data-safety declarations;
- Play Console/developer-account actions;
- any future authorized FBR integration or live-data provider decision.

None of these authorizes inventing credentials, storing bank/FBR credentials, or claiming official FBR submission/certification.

## 7. Non-negotiable invariants for restart

Every subsequent agent must preserve and explicitly test these invariants:

- Financial amounts are never persisted as binary floating point; use minor-unit `Long` or a controlled decimal representation.
- Transfers never change income or expense totals.
- A source financial fact and its tax interpretation remain separate objects.
- Reclassification/ruleset changes never rewrite the original financial fact.
- Recomputing tax candidates does not duplicate source-linked tax items.
- Evidence absence never blocks capture; evidence status remains visible and traceable.
- No financial source fact is silently altered, merged, or deleted by duplicate detection.
- Historical tax rulesets and annual drafts remain immutable/versioned.
- Vault bytes are encrypted with authenticated encryption, use safe nonces/keys, and are not Room BLOBs.
- Restore authenticates, validates, stages, migrates, checks integrity, and commits atomically; a failed restore preserves the existing database.
- No app lock bypass, hidden PIN recovery bypass, sensitive production logging, plaintext backup, bank credential storage, FBR credential storage, advertising SDK, or mandatory cloud dependency may be introduced.

## 8. Exact restart procedure

On the next session:

```bash
cd /home/munaim/Documents/github/passport
git status --short
git log -5 --oneline
adb devices
./gradlew test lint
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

If the full connected suite fails, capture the exact failing test and follow the master-prompt loop: reproduce narrowly, diagnose root cause, repair, rerun the narrow test, then rerun the full gate on both API 26 and API 36. Do not edit a test only to hide a product defect.

Then verify schema and recurring work:

```bash
./gradlew :app:testDebugUnitTest --tests '*DatabaseMigrationTest*'
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=pk.vexel.financepassport.ui.RecurringDraftDeviceTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=pk.vexel.financepassport.core.database.DatabaseMigrationTest
```

Run the full suite again after those targeted checks. Use the API 36 AVD as well as the currently connected API 26 AVD; inspect available AVDs with `emulator -list-avds` and boot the appropriate API 36 device if needed.

After the current work passes, update `docs/BUILD_STATUS.md`, the relevant Sprint 14–16 gate reports, the acceptance matrix, blockers/deferred-decision files, and `docs/FINAL_VERIFICATION.md`. Build and inspect a fresh release artifact:

```bash
./gradlew clean test lint connectedDebugAndroidTest assembleRelease
sha256sum app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am force-stop pk.vexel.financepassport
adb shell monkey -p pk.vexel.financepassport -c android.intent.category.LAUNCHER 1
```

Only commit after the current sprint gate has genuinely passed. Use a focused imperative subject, for example `Verify recurring draft migration and device flow`. Do not push to an unknown remote.

## 9. Final acceptance and handoff criteria

The objective is not satisfied by compilation. Before declaring `GO — INTERNAL RELEASE READY`, the next agent must provide evidence for the complete acceptance catalog and final regression: fresh onboarding, PIN/biometric/relock, money capture and transfers, wealth/investments/receivables/goals, tax capture and annual draft, source drill-down and reconciliation, encrypted Vault, reports, encrypted backup/transactional restore, exports, delete-all, reminders, offline operation, process death, rotation, large fonts, accessibility smoke, and app-lock/deep-link security.

If any internally solvable critical/high defect or required evidence remains missing, retain:

`# NO-GO — INTERNAL RELEASE NOT READY`

with the exact failed command, defect/root cause, affected requirement, and next action. Use `BLOCKED_EXTERNAL` only for a genuinely external item such as production signing or Play Console access. The next agent should resume from this document and the actual worktree, not recreate the project or assume the prior release APK includes schema-7 changes.
