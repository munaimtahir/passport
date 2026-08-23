# Remediation Master Status Ledger

This is the live implementation ledger for the post-audit remediation run that
started 2026-08-23. It supersedes the *status* claims (not the detailed
evidence) in `SPRINT_0_16_TRACEABILITY_MATRIX.md`, `TEST_AND_RUNTIME_EVIDENCE.md`
and `DISCOVERY_VERIFICATION_REPORT.md` for any row updated below. Those files
remain as historical audit evidence and are not deleted or rewritten in place.

## Baseline identity

| Item | Value |
| --- | --- |
| Repository (this session) | `/media/munaim/shared1/Documents/github/passport` |
| Branch | `main` (tracks `origin/main`) |
| Starting HEAD | `468ad6343d61e9b165fa10b89ba8834e8c4a516b` ("Add internal release discovery audit") |
| Audit HEAD referenced by discovery report | `29f4bed9bf0e372b5893270c203b14b1e3bbfac3` ("local") — one commit behind current HEAD; the only intervening commit adds the audit docs themselves, no production code changed since the audit |
| Working tree at session start | Clean except cosmetic re-save (0 byte diff) of the three audit docs |
| Host baseline re-verified this session | `./gradlew test lint` → BUILD SUCCESSFUL in 6m17s, 2026-08-23 |

## Status vocabulary

- **VERIFIED — HOST**: implemented and covered by a passing JVM/host-side test or explicit gradle check this session.
- **IMPLEMENTED — DEVICE VERIFICATION DEFERRED**: code path exists and compiles/host-tests pass, but requires an emulator/device to observe (per Phase 0 execution rule, deferred to Phase 10).
- **NOT IMPLEMENTED**: capability described in the canonical docs is absent from source.
- **BLOCKED — EXTERNAL RELEASE**: requires a real-world asset/decision outside engineering (see `docs/BLOCKERS.md`).
- **POST-MVP**: explicitly out of scope per mega-prompt section 13.

## Per-sprint remediation mapping

| Sprint | Audit status (2026-08-22) | Remediation phase | Current status | Notes |
| --- | --- | --- | --- | --- |
| 0 Foundation | PARTIAL | Phase 0 | VERIFIED — HOST | Build/unit/lint green; install/launch/device gate deferred to Phase 10 |
| 1 Design/nav/onboarding | PARTIAL | Phase 1 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Onboarding flow (`Onboarding.kt`), global privacy masking (`AppPreferences`, `LocalPrivacyMode`), and a reusable `DateField` landed this phase; see Phase 1 log row for what remains |
| 2 Local data | PARTIAL | Phase 1/2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Schema v8 migrations verified by JVM test; income/expense/transfer forms now use `DateField` instead of a silent `LocalDate.now()` default; most other historical-date entry points remain for Phase 2 |
| 3 Security | PARTIAL | Phase 8 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | PIN/biometric/Keystore exist; full lifecycle/device evidence deferred |
| 4 Money | PARTIAL | Phase 2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Account institution/notes metadata now captured; canonical liquid-funds calc added (`FinancialPosition`) |
| 5 Wealth | PARTIAL | Phase 2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Valuation/disposal/repayment/partial-receipt dialogs were already UI-wired (audit predates this); investment holdings summary and non-hardcoded account label added this phase |
| 6 Home | ~~BROKEN~~ FIXED | Phase 3 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Home now shows canonical `FinancialPosition.netWorthMinor` with an assets/liabilities/liquid-funds/investments/receivables breakdown, correctly labeled; the old movement figure is relabeled "Income vs. expense this period" and never called net worth |
| 7 Vault/records | PARTIAL | Phase 6 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Fixed a real bug: deleting a document left linked `TaxItemEntity` rows dangling at evidence state ATTACHED; now reverts to REQUESTED. Added dependency-count-aware safe delete, basic metadata search, duplicate-hash-on-import rejection, document/official-record expiry→calendar-reminder wiring, and account-linking (not just tax items) |
| 8 Tax capture | PARTIAL | Phase 4 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED (mapping lineage); NOT IMPLEMENTED (drill-down UI) | `TaxMappingEntity` now persists SYSTEM_GENERATED/USER_OVERRIDE mapping history with supersession; annual-draft/UI drill-down from a draft line back to its mapping history is still Phase 5 |
| 9 Rules engine | PARTIAL | Phase 4 | IMPLEMENTED — HOST VERIFIED | Ruleset is now JSON (`taxrules/pk-structural-1.json`), parsed/validated by `TaxRulesetLoader` with typed `RulesetError`s; taxonomy already covered all 24 mega-prompt event types, confirmed not extended; only one ruleset version exists (multi-version history is architecturally supported, not yet exercised) |
| 10 Annual workspace | PARTIAL | Phase 5 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Draft generation now takes a selected tax year (not just "now"); draft versioning confirmed by test (regeneration increments version, keeps prior version's lines intact); draft-line → source-tax-item drill-down already existed and is unchanged; draft-line → `TaxMappingEntity` lineage walk still not wired (would need a UI drill-down screen, out of this phase's scope) |
| 11 Reconciliation | PARTIAL | Phase 5 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Fixed a real bug: opening wealth was hardcoded to zero and reconciliation summed *all* financial events ever, not just the tax year's. Now requires a persisted `WealthSnapshotEntity` (Phase 5D, new) opening snapshot and scopes income/expenditure to the tax year's date range; UI drill-down into individual contributing records still absent |
| 12 Reports | PARTIAL | Phase 7 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | In-app preview dialog now precedes every PDF export; all report amounts use canonical `FinancialPosition`/grouped PKR formatting instead of raw `/100` division |
| 13 Backup/restore | PARTIAL | Phase 7 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Manifest now records per-document SHA-256 hashes and the active ruleset version (backward-compatible parsing for older manifests lacking them); equivalence proof still needs a device (Phase 10D) |
| 14 Calendar | PARTIAL | Phase 6/7 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED (document/official-record expiry, Phase 6) | Document/official-record expiry reminders wired in Phase 6; receivable-due-date and tax-review-deadline reminders still absent |
| 15 UX hardening | PARTIAL | Phase 1/8 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED (masking); NOT IMPLEMENTED (a11y) | Global privacy masking landed in Phase 1; a11y/adaptive review remains Phase 8 |
| 16 Release | PARTIAL | Phase 8/9 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Debug-signed internal QA build exists; device/physical evidence deferred |

## Phase execution log

| Phase | Status | Commit | Notes |
| --- | --- | --- | --- |
| 0 — Baseline freeze | DONE | `febea67` | This document created; host baseline re-verified green |
| 1 — Onboarding/dates/privacy/UI foundation | DONE (scoped) | `9b8d9ed` | See detail below |
| 2 — Canonical money/wealth completion | DONE (scoped) | `eb6cbd5` | See detail below |
| 3 — Canonical home dashboard | DONE (scoped) | `f793fb4` | See detail below |
| 4 — Versioned tax capture engine | DONE (scoped) | `be260e0` | See detail below |
| 5 — Annual workspace/reconciliation | DONE (scoped) | `c4ce66f` | See detail below |
| 6 — Vault/records/evidence lifecycle | DONE (scoped) | `863eebd` | See detail below |
| 7 — Reports/export/backup/calendar | DONE (scoped) | (pending — see below) | An earlier attempt at this phase was interrupted by a session limit mid-work; this run picked up its verified-compiling partial diff and finished it — see detail below |
| 8 — UX/accessibility/security/release hardening | NOT STARTED | — | |
| 9 — Implementation freeze/clone-ready handoff | NOT STARTED | — | |
| 10 — Deferred device qualification | NOT STARTED (explicitly deferred) | — | Requires emulator/device environment |

This table is updated at the end of every phase in this remediation run.

## Phase 7 detail

**Provenance note:** an earlier attempt at this phase was interrupted mid-work by a session usage
limit, leaving three files (`BackupPackage.kt`, `FinanceRepository.kt`, `Reports.kt`) modified but
uncommitted, with no tests and no ledger update. That partial diff was verified to compile
(`./gradlew compileDebugKotlin` PASS) before this pass built on it — it was not redone. What it had
already landed: `BackupManifest` gained `documentHashes`/`rulesetVersion` fields (serialized and
parsed in `manifestJson()`/`restore()`, backward-compatible with older manifests lacking them);
`FinanceRepository`'s backup-creation path now supplies real document hashes and the active ruleset
version; and `Reports.kt` gained a `canonicalPosition(snapshot)` helper (reusing Phase 2's
`calculateFinancialPosition`) used by `netWorth()`/`investmentSummary()`, plus a `pkr()` formatting
helper replacing every raw `minor / 100` division across the report catalog.

**Landed this pass, in priority order:**

- **7C in-app report preview — the top-priority item, not yet done by the interrupted attempt.**
  `MoreDialog` in `PassportApp.kt` no longer sends every report straight to a SAF PDF picker.
  Each report button now generates the `FinancialReport` first and shows it (title, generated-at
  timestamp, and every line, in a scrollable dialog) with "Export as PDF" / "Close" actions; PDF
  export only happens if the user confirms from the preview. All nine report types (net worth,
  annual summary, and the seven catalog reports) go through this preview step.
- **Formatting fix, verified complete.** Grepped `Reports.kt` for any remaining `/ 100` or raw
  `PKR $x` string interpolation the interrupted attempt might have missed — found none;
  `annualFinancialSummary` and `evidenceChecklist` (which don't format currency directly) needed no
  change, and every other function already used `pkr()`.
- **7G JSON export completeness — a real, confirmed gap, now closed.** `ExportSnapshot` and
  `DataExportService.json()` predated `TaxMappingEntity`/`WealthSnapshotEntity` (Phase 4/5) and the
  `TaxAnnualDraftEntity.draftVersion` field — none of the three were in the structured export at
  all. Added `taxMappings`/`wealthSnapshots`/`taxDrafts` fields to `ExportSnapshot` (defaulted to
  `emptyList()` so existing call sites/tests didn't need updating), added the missing `getAll()`
  queries to `TaxMappingDao`/`WealthSnapshotDao`/`TaxDraftDao` (none existed — only per-item/
  per-year/`Flow`-based queries did), wired `FinanceRepository.exportSnapshot()` to populate them,
  and extended `DataExportService.json()` to serialize each list.
- **7K delete-all — confirmed, not changed.** `deleteAllData` calls `db.clearAllTables()`, which
  Room generates to cover every table in the *current* schema automatically — `tax_mappings` and
  `wealth_snapshots` need no special-casing. `WorkManager.cancelAllWork()` cancels all work
  unconditionally, so Phase 6's `document-expiry-$id`/`official-record-expiry-$id` unique work
  requests are covered without any ID-specific logic. No code change was needed for this item; it
  was a verification-only check.

**Deferred (documented here, not faked):** CSV export for report types beyond financial events
(only `csvEvents`/`csvAccounts`/`csvTaxItems` exist); "streaming vs. full-buffer" backup read
improvements beyond what already existed; a second, independent backup/restore device round-trip
proof (that's explicitly Phase 10D's job, not host-testable). No Room migration was needed for this
phase — schema stays at version 10.

**Tests added:**
- `ReportsTest.netWorthReportMatchesCanonicalFinancialPositionIndependently` (a report's net worth
  equals a directly-computed `calculateFinancialPosition` result for the same fixture, and
  `canonicalPosition()` itself is `equals` to that independent computation) and
  `reportAmountsUseGroupedPkrFormattingNotRawDivision` (asserts `"PKR 1,234,567"` grouped output
  appears and the raw-division form does not).
- `BackupPackageTest.manifestRoundTripsDocumentHashesAndRulesetVersion` and
  `manifestParsingToleratesLegacyPackagesMissingHashOrRulesetFieldsEntirely` (hand-builds a
  package whose `manifest.json` has the pre-Phase-7 shape — the two new keys entirely absent, not
  just empty — to prove `restore()` doesn't require them).
- `DataExportTest.jsonExportIncludesTaxMappingWealthSnapshotAndDraftLineage`.

**Verification:** `./gradlew test lint` PASS (all new tests green); `./gradlew assembleDebug` PASS;
`./gradlew assembleDebugAndroidTest` PASS (no androidTest sources were touched this phase, so this
is unchanged from Phase 6's state — reconfirmed anyway). No device available this session.

## Phase 6 detail

Read the real current state before building (a prior read-only survey this session had already
confirmed the key facts below; re-verified against source directly): `core/files/DocumentVault.kt`
already had import/encrypt/decrypt via Keystore AES-GCM and SHA-256 hashing. `DocumentEntity` had
an `expiryDateEpochDay` column that was always `null` — no UI ever set it, nothing ever read it.
No schema migration was needed for this phase — every column Phase 6 needed already existed.

**Landed, in priority order:**

- **6E Safe deletion — real bug fix.** `FinanceRepository.deleteDocument()` removed link rows and
  the file but never reverted a linked `TaxItemEntity.evidenceState` off `ATTACHED`/
  `VERIFIED_BY_USER`, leaving it dangling once the evidence it pointed at was gone. Fixed: deletion
  now walks the document's links inside the same transaction and reverts any affected tax item's
  evidence state to `REQUESTED`. Added `TaxItemDao.getById` (was missing) to support this. The
  Vault delete dialog now computes and shows the dependency count first (`documentDependencyCount`)
  and offers Cancel / "Unlink and delete" (worded accordingly) instead of an unconditional "Delete".
  Did **not** implement a "Replace document" option (keep the same id/links, swap the bytes) —
  deferred, judged lower value than the correctness fix and the other items below within this
  phase's time budget.
- **6G Expiry wiring.** The document-import dialog now collects title, category, and an optional
  expiry date via Phase 1's `DateField` (previously the dialog had *no* title/category input at
  all — title was hardcoded to `"Imported evidence"`, category to `"Other"`; this was also a real
  6A gap, fixed as a side effect). `OfficialRecordDialog` gained the same optional expiry field.
  Both now call `FinanceRepository.scheduleDocumentExpiryReminder` / the equivalent official-record
  path, which upserts a `CalendarItemEntity` (kind `DOCUMENT_EXPIRY` / `OFFICIAL_RECORD_EXPIRY`) and
  calls the existing `ReminderScheduler`, following the exact pattern already used for manual
  calendar items and recurring drafts (`enqueueUniqueWork(..., ExistingWorkPolicy.REPLACE)` +
  `calendarDao().upsert` keyed by a deterministic id `document-expiry-$id` /
  `official-record-expiry-$id`) — re-saving the same expiry cannot duplicate the reminder because
  both the Room upsert and the WorkManager enqueue are keyed by that same stable id.
- **6D Evidence linking generality.** `DocumentLinkEntity.entityType` was already schema-generic,
  but the link dialog only ever offered tax items. It now also lists accounts
  (`entityType = "account"`), matching the mega-prompt's example of one document (e.g. a bank
  certificate) linking to more than a tax item. Assets/liabilities were not added — deferred,
  accounts were judged the highest-value second target.
- **6F Duplicate detection.** `documents.sha256` already had a unique DB index, but `import()`
  would have thrown a raw SQLite constraint-violation exception on a duplicate with no useful
  message. Added an explicit pre-insert check in `DocumentVault.import()` that throws a clear,
  user-facing error naming the existing document; the Vault import dialog now surfaces that message
  instead of silently swallowing the failure (previously `runCatching { ... }` with no error path
  shown to the user at all).
- **6C Search.** Added a basic title/category substring filter to the Vault document list.

**Deferred (documented here, not faked):** "Replace document" delete option; asset/liability
evidence-link targets (only tax items + accounts); 6A/6H broader metadata/category breadth beyond
what was needed for the above; a dedicated instrumentation test for `DocumentVault.import()`'s new
duplicate-hash guard — it's compile-verified but not independently test-run, because exercising it
needs a real `ContentResolver`-backed `Uri` (MIME lookup fails against a plain `file://` `Uri` in
this test environment); the DAO/dependency-count and evidence-state-revert paths it shares with
deletion *are* covered by `DocumentLifecycleDeviceTest`.

**Tests added** (`app/src/androidTest/java/pk/vexel/financepassport/core/database/DocumentLifecycleDeviceTest.kt`,
compiled only — no device this session):
`deletingDocumentRevertsAttachedEvidenceStateInsteadOfLeavingItDangling` (the core regression test
for the bug fix above), `documentDependencyCountReflectsCurrentLinks`,
`documentExpiryReminderIsPersistedAsAnOpenCalendarItem`,
`officialRecordWithoutExpiryDoesNotScheduleAReminder`.

**Verification:** `./gradlew test lint` PASS; `./gradlew assembleDebug` PASS; `./gradlew
assembleDebugAndroidTest` PASS (compiles only, no device this session). No new Room migration —
schema stays at version 10.

## Phase 1 detail

**Note on provenance:** the bulk of this phase's diff (`AppPreferences.kt`, `Onboarding.kt`,
`DateField.kt`, and the wiring into `MainActivity.kt`/`PassportApplication.kt`/
`MainViewModel.kt`/`PassportApp.kt`) was found already present, uncommitted, in the working
tree at the start of this remediation run — left over from an earlier interrupted session, not
written by this run's Phase 1 pass. This run verified it (host gate green), extended it, fixed a
regression it introduced, and is committing it as Phase 1 of this ledger.

Landed:

- Global privacy masking (1C): `AppPreferences` (SharedPreferences-backed, non-sensitive) persists
  a privacy-mode flag; `MainViewModel.privacyModeEnabled`/`togglePrivacyMode()` exposes it; a
  `LocalPrivacyMode` composition local plus a `MaskedPkr` helper is wired into every amount shown
  on Home, Money, and Wealth (accounts, events, recurring drafts, assets, liabilities, investments,
  receivables, goals); an eye icon in the top app bar toggles it.
- Historical date architecture (1B), partial: a reusable `DateField` composable (Material3
  `DatePickerDialog` backed by `java.time.LocalDate`) now replaces the silent `LocalDate.now()`
  default in the income/expense and transfer entry dialogs (`FinanceRepository.addEvent`/`transfer`
  already accepted an explicit date parameter; only the UI wiring was missing, matching the audit
  finding). Asset/liability/investment/receivable/document/official-record date entry points were
  **not** touched this phase — they remain Phase 2/6 work.
- Onboarding (1A), partial: a real 3-page `OnboardingGate`/`OnboardingFlow` (welcome → privacy/
  offline explanation → PKR/PIN handoff) gates `MainActivity` before `SecurityGate`, persisted via
  `AppPreferences.isOnboardingComplete()`. Guided account setup (seed a bank/cash/investment
  account or start empty) was **not** implemented — onboarding hands off directly to PIN creation.
  Re-showing onboarding after delete-all was not independently verified: `deleteAllData` already
  deletes the whole `shared_prefs` directory (pre-existing behavior, not new this phase), which
  should include `passport_app_prefs`, but whether an already-constructed `AppPreferences`/
  `SharedPreferences` instance picks that up without a process restart is a runtime question left
  for Phase 10 device qualification, not re-verified here.
- Demo synthetic data (1E): **not implemented this phase** — deprioritized below 1A–1C per this
  phase's own scoping, and not picked up given the regression fix below took priority.

Regression found and fixed (not device-observed — found by static reading of test bodies):
inserting `OnboardingGate` in front of `SecurityGate` means a fresh install now shows onboarding
before the "Vexel Finance Passport" title / "Create PIN" / "Unlock" text that four existing
instrumentation tests (`NavigationSmokeTest`, `MoneyCaptureDeviceTest`, `RecurringDraftDeviceTest`,
`WealthCaptureDeviceTest`) assert on immediately. Added a `dismissOnboardingIfPresent()` helper
(loops clicking the `onboarding-next`-tagged button while present) to all four, called before
their existing PIN/unlock handling. Not run on a device this session (Phase 10 will confirm).

Added test coverage: `AppPreferencesTest` (androidTest) — onboarding/privacy default state and
persistence across a fresh `AppPreferences` instance. No JVM-level test was added for
`AppPreferences` because it requires an Android `Context` (SharedPreferences) and the project has
no Robolectric dependency; adding one was judged out of scope for this phase.

Verification: `./gradlew test lint` PASS (BUILD SUCCESSFUL, 2026-08-23); `./gradlew
assembleDebugAndroidTest` PASS (androidTest sources compile, not run). No emulator/device
available this session — all of the above is host-side/static verification only.

## Phase 2 detail

**Correction to the audit before starting:** `WealthScreen` in `PassportApp.kt` already had
UI-wired asset valuation update, asset disposal, liability repayment, and receivable
partial-receipt dialogs before this phase — the audit's "Wealth maintenance services exist but
are mostly dormant from UI" finding no longer held even at the audit's own stated HEAD once
re-checked against actual source. Only the investment side (2H) and account metadata (2A) were
genuinely dormant/missing as described.

Landed, in priority order:

- **2J Canonical financial calculation domain (highest priority).** Added
  `core/model/FinancialPosition.kt`: a pure, tested domain function
  (`calculateFinancialPosition`) producing one `FinancialPosition` (liquid funds, investment cost
  basis, assets, receivables, liabilities, monthly income/expense, `totalAssetsMinor`,
  `netWorthMinor`) from entity lists — no live market-price feed is used or invented; investment
  value is the traceable recorded cost basis. Exposed as `FinanceRepository.financialPosition:
  Flow<FinancialPosition>`, combining active accounts + a new bounded DAO aggregate
  (`observeActiveAccountsMovement`, mirroring the existing per-account movement query but across
  all active accounts) with assets/liabilities/investments/receivables and two new ranged DAO
  queries for current-month income/expense (`observeIncomeMinorInRange`/
  `observeExpenseMinorInRange`). Exposed on `MainViewModel.financialPosition` as a
  `StateFlow<FinancialPosition?>` for Phase 3 (Home) to consume directly — **Home was
  deliberately not touched this phase**; it still shows the mislabeled "Net recorded movement"
  and is Phase 3's job per the mega-prompt's own phase boundary.
  - Also used the same canonical calculation to fix `calculateCurrentReconciliation`'s
    `recordedClosing` figure, which previously used assets-minus-liabilities only and silently
    ignored cash/investments/receivables — a real correctness gap, now sourced from
    `financialPosition.first().netWorthMinor`. Full reconciliation UI/drill-down remains Phase 5.
- **2H Investments.** Fixed the audit-flagged hardcoded `"manual"` `investmentAccountId`:
  `addInvestmentEvent`/`MainViewModel.addInvestmentEvent` now take an optional `accountLabel`
  (defaults to `"Manual"` only when left blank), and the wealth-add dialog exposes an optional
  "Broker / account" field. Added an investment-holdings summary card per security in
  `WealthScreen` using the existing (previously dormant) `calculateInvestmentPosition` — shows
  quantity, cost basis, realized gain/loss, and withholding-net income, with an explicit note that
  no live price is used. There is still no dedicated investment-account entity/table (accounts are
  just a free-text label on each event) — building real multi-account investment ledgering would
  need a new entity and migration, judged out of scope for this pass.
- **2A Accounts.** `AccountEntity` already had `institution`/`notes` columns (added in an earlier
  session, never migrated) that the add/edit UI never exposed — no migration was needed, just
  wiring. Added institution + notes fields to `AddAccountDialog`/`EditAccountDialog`, threaded
  through `FinanceRepository.addAccount`/`updateAccount` (new optional params) and the DAO's
  `updateDetails` query, and surfaced them on `AccountCard`. Account-type breadth (the mega-prompt
  lists cash/current/savings/wallet/foreign-currency/brokerage-cash/other as an enum) was **not**
  addressed — accounts still use a free-text `accountType` string with the add dialog hardcoding
  `"OTHER"`; deferred as a UI-only follow-up, not attempted this pass.
- **2D Categories/tax separation.** Verified, not changed: `FinancialEventEntity.category` (a
  free-text spending category) and `TaxItemEntity.taxEventType`/`reviewState` (the tax
  interpretation) are already separate entities/columns with no coupling — the non-negotiable rule
  already holds. No fix needed.
- **2B/2C/2E/2F/2G/2I** (transaction fields, money-activity filtering, asset/liability/receivable
  field completeness beyond what 2J/2H touched, goals UI) were **not** picked up this phase —
  explicitly deprioritized per this phase's own scope order once 2J/2H/2A took the available time.

Tests added: `FinancialPositionTest` (JVM, deterministic net-worth fixture including a partial
investment sale and a mixed asset/liability/receivable position, plus an empty-portfolio zero
case) and three new `AppDatabaseTest` cases (account institution/notes persistence, investment
account-label non-hardcoding, and the canonical `financialPosition` Flow combining accounts +
wealth + monthly activity end-to-end).

Verification: `./gradlew test lint` PASS; `./gradlew assembleDebugAndroidTest assembleDebug` PASS
(compiles only; no device this session).

## Phase 3 detail

Landed:

- **Primary objective — Home net worth fixed.** The audit's #1 flagged ("BROKEN") defect is closed:
  Home's headline card now shows `MainViewModel.financialPosition.netWorthMinor` (the Phase 2
  canonical `FinancialPosition`), labeled "Net worth", with a breakdown of liquid funds,
  investments, assets, receivables and liabilities as separate lines underneath — not a single
  opaque number. The former income-minus-expense figure is kept as a separate card, relabeled
  "Income vs. expense this period", and is never called net worth anywhere in the UI.
  All new amounts go through the existing `MaskedPkr`/`LocalPrivacyMode` path from Phase 1.
- **Tax-year readiness.** Extracted the tax-readiness counting logic (evidence-pending,
  unmapped/needs-classification, duplicate-candidate groups) that previously lived inline in
  `TaxScreen` into a shared, testable domain function `calculateTaxReadiness` in a new
  `core/model/TaxReadiness.kt`. Home's readiness card and `TaxScreen`'s existing "Annual review
  readiness" card now both call the same function, so the two surfaces cannot drift. No new tax
  workflow was added — this phase only replaced ad hoc inline Compose math with a shared,
  unit-tested calculation, per the mega-prompt's "workflow completeness, not tax correctness" framing.
- **Quick add.** Home gained a "Quick add" row wired via a new `onNavigate: (Int) -> Unit` callback
  from `PassportApp` (switches the bottom-nav tab). Buttons for Income/expense and Transfer jump to
  Money (tab 1, which already has both dialogs); Asset jumps to Wealth (tab 2); Tax item jumps to
  Tax & Records (tab 3); Document jumps to Vault (tab 4). This is a navigation shortcut, not a
  duplicated dialog — it reuses each destination's existing add flow rather than re-implementing it
  inline on Home.

Deferred, deliberately:

- No new Home-specific summary cards for "monthly income"/"monthly expense" as standalone tiles —
  those numbers are shown inline in the relabeled movement card instead of as separate cards, to
  avoid a cluttered/duplicated dashboard; can be split out later if the product spec wants dedicated
  tiles.
- Quick Add does not deep-link directly into the target dialog (e.g. tapping "Tax item" opens the
  Tax tab, not the "Add tax item" dialog already open) — out of scope for this pass; would need
  passing dialog-open intent across screens, a larger change than a tab switch.
- Tax engine internals, annual draft/reconciliation workspace, and vault/reports/backup were not
  touched, per this phase's explicit boundaries.

Tests added: `TaxReadinessTest` (JVM, deterministic fixture covering evidence-pending, unmapped,
and duplicate-group counting — including a regression the first draft of this test caught: five
items sharing default date/amount values collided into an unintended duplicate group, fixed by
giving each item a distinct date/amount). No new Compose/device-level test was added since
`financialPosition`'s correctness is already covered by `FinancialPositionTest` (Phase 2) and Home
only consumes it, doesn't recompute it.

Verification: `./gradlew test lint` PASS (`TaxReadinessTest` caught and had one real fixture bug,
fixed before this report); `./gradlew assembleDebug assembleDebugAndroidTest` PASS (compiles only;
no device this session).

## Phase 4 detail

**Corrected before building:** `TaxEventType` already covers all 24 mega-prompt taxonomy values
(employment/business/professional/rental income, bank profit, dividend, capital gain/loss, tax
withheld, advance tax, tax payment, asset acquisition/disposal, liability created/repaid, personal
expenditure, donation, zakat, insurance/pension, foreign income/asset, investment purchase/sale,
other income, other tax event) — 4C needed no extension, only confirmation.

Landed:

- **4B/4A — JSON ruleset package.** `defaultPakistanStructuralRules()`'s previously-hardcoded list
  of 5 `TaxRule`s is now data: `app/src/main/resources/taxrules/pk-structural-1.json`, parsed and
  validated by a new `TaxRulesetLoader.parse(json: String)` (`core/taxrules/TaxRulesetJson.kt`).
  Malformed JSON, a missing required field, an empty rule list, or a rule naming a tax event type
  outside `TaxEventType` all throw a typed `RulesetError` subtype — never a silent fallback to the
  wrong mapping. `BundledTaxRulesets.loadDefault()` reads the packaged JSON via a classpath
  resource (`javaClass.classLoader.getResourceAsStream(...)`), deliberately **not** `assets/` +
  `Context.getAssets()` — this keeps the full load-and-parse path host-testable in a plain JVM test
  (no Robolectric/instrumentation needed) and confirmed working in that JVM test this session.
  Standard AGP behavior packages `src/main/resources` content into the APK identically to a jar, so
  this is expected to work on-device unchanged, but that specific expectation — resource actually
  readable from an installed APK, not just the JVM test classpath — is unverified until Phase 10.
  `defaultPakistanStructuralRules()` itself is kept (existing `TaxEngineTest.kt` calls it directly)
  but now just delegates to `BundledTaxRulesets.loadDefault()`, so both paths use identical data.
- **4F — tax mapping lineage.** New `TaxMappingEntity` (`tax_mappings` table, additive
  `MIGRATION_8_9`, schema version 8→9) persists mapping history per tax item: ruleset version,
  classification, `source` (`SYSTEM_GENERATED` vs `USER_OVERRIDE`), override reason, and a
  `supersededByMappingId` pointer. `FinanceRepository.addEvent`'s income branch and
  `addManualTaxItem` now insert an initial `SYSTEM_GENERATED` mapping (via the real
  `StructuralTaxClassifier` against the bundled ruleset) — but only when `TaxItemDao.insertIfAbsent`
  actually inserted a row, so recomputing against an already-captured source never creates a second
  mapping history. `reviewTaxItem` (reclassification) now inserts a new mapping row
  (`USER_OVERRIDE` + the reason) and marks the previously-active mapping's
  `supersededByMappingId`, instead of only mutating `TaxItemEntity` in place as before — prior
  mapping rows are never edited or deleted. `TaxItemEntity` itself is still updated in place for its
  live review-state fields (reviewState/evidenceState/taxEventType), matching existing UI/read
  patterns; `TaxMappingEntity` is the append-only audit trail sitting alongside it, not a
  replacement for it.
- Fixed two schema-version literals in `FinanceRepository.createEncryptedBackup`/
  `createEncryptedBackupFile` that hardcoded `8` for the backup manifest's `schemaVersion` field —
  now `9`, matching the bumped `AppDatabase` version (would otherwise have silently mislabeled every
  backup made after this phase).

Deferred, deliberately:

- **4I duplicate candidate engine** — not touched. The audit-era duplicate-flagging behavior (if
  any existed before this phase) was not investigated or extended; ran out of priority budget after
  4B/4F.
- **4D/4E/4G/4H** (source-capture uniqueness beyond what already existed, the `TaxRelevance` enum,
  split treatment, the `EvidenceState` enum) — not touched; no concrete broken invariant was found
  in these areas while implementing 4B/4F, and the mega-prompt said to leave working things alone
  absent a concrete defect.
- Only one ruleset version (`pk-structural-1`) exists. The architecture (a `rulesetVersion` string
  threaded through `TaxYearEntity`/`TaxMappingEntity`/`TaxAnnualDraftEntity`) supports adding a
  second version as a second JSON resource with no code change, but that was not exercised — there
  is no test proving two different ruleset versions coexist and are each individually reproducible.
  This is a real gap versus the mega-prompt's "historical ruleset versions remain immutable"
  requirement: immutability is architecturally possible but not yet demonstrated with more than one
  version in existence.
- `FinanceRepository.addEvent`'s income branch still hardcodes `"EMPLOYMENT_INCOME"` as the tax
  event type for every income event regardless of category — this predates this phase (audit didn't
  flag it specifically) and was left alone; it does mean the initial mapping this phase now
  generates for that path is only ever classified as employment income until a user reclassifies it.

Tests added:
- JVM (`TaxRulesetLoaderTest`, 7 tests): valid JSON loads; identical JSON parsed twice is `equals`
  (determinism); malformed JSON, a missing field, an empty rule list, and an unknown taxonomy value
  each throw the correct `RulesetError` subtype; the bundled default ruleset loads via the classpath
  resource and is `equals` to what `defaultPakistanStructuralRules()` returns.
- androidTest (compiled, not run this session): `DatabaseMigrationTest.migrateV8ToV9AddsTaxMappingsTable`
  (pre-existing `tax_items` row survives the migration; `tax_mappings` table exists and accepts an
  insert against that pre-existing row) and
  `AppDatabaseTest.manualTaxItemGetsASystemGeneratedMappingAndReclassificationSupersedesRatherThanReplaces`
  (a manual tax item gets exactly one initial `SYSTEM_GENERATED` mapping; reclassifying via
  `reviewTaxItem` does not create a second `TaxItemEntity`, does add a second mapping row, marks the
  first as superseded by the second while leaving all its other fields unchanged, and the override
  reason is preserved on the new active mapping).

Verification: `./gradlew test lint` PASS (7/7 new JVM tests green, full suite green);
`./gradlew assembleDebug` PASS; `./gradlew assembleDebugAndroidTest` PASS (compiles only, not run —
no device this session). New Room schema version: **9** (`app/schemas/.../9.json` exported).

## Phase 5 detail

**Read before writing:** confirmed against real source (not the stale audit) that
`TaxAnnualDraftEntity` already had a `draftVersion: Int` column and `TaxDraftDao.maxVersion` +
`prepareAnnualDraft` already incremented it on regeneration — draft versioning existed already and
needed no new migration, only a test proving it actually behaves that way (added; see below).
`TaxDraftLineEntity.sourceIdsJson` already gave line→source-tax-item drill-down. The real, concrete
gaps were: (1) `prepareAnnualDraft`/reconciliation only ever operated on the current device-clock
year, with no way to select a past tax year; (2) `calculateCurrentReconciliation` hardcoded opening
wealth to `Money(MinorUnits(0))` — a direct violation of the mega-prompt's explicit "do not hardcode
opening wealth to zero" rule — and summed *every* financial event ever recorded as this year's
inflows/expenditure, not just events dated within the tax year.

Landed:

- **New `WealthSnapshotEntity`** (`wealth_snapshots` table, additive `MIGRATION_9_10`, schema
  version 9→10; also bumped the two hardcoded backup-manifest `schemaVersion` literals in
  `FinanceRepository` from `9` to `10`, the same class of bug Phase 4 fixed for the 8→9 bump).
  Records an OPENING or CLOSING wealth position for a tax year, captured from the canonical
  `FinancialPosition` at the moment `FinanceRepository.recordWealthSnapshot(year, kind, date)` is
  called. Re-recording the same kind replaces the prior row (`OnConflictStrategy.REPLACE`) — this
  is treated as a correctable working estimate, not an immutable source fact; a `WealthReconciliationEntity`
  already generated from an earlier snapshot keeps its own recorded figures regardless of later
  snapshot edits, so historical reconciliations remain reproducible.
- **`calculateReconciliation(taxYearId)` replaces `calculateCurrentReconciliation()`.** Now
  requires a persisted OPENING snapshot for the year — throws a clear error ("Record an opening
  wealth snapshot for PK-2026 before reconciling") rather than silently defaulting to zero — and
  scopes income/expenditure summation to `FinancialEventEntity` rows whose `dateEpochDay` falls
  inside the tax year's `startDateEpochDay..endDateEpochDay`. Recorded closing wealth prefers a
  persisted CLOSING snapshot; falls back to the live canonical `FinancialPosition` only for a year
  with no closing snapshot yet (i.e. the current, still-open year) — documented as such in code, not
  hidden.
- **`prepareAnnualDraft(year: Int = LocalDate.now().year)`** — added the year parameter (existing
  no-arg call site keeps working via the default); a shared private `ensureTaxYearExists(year)`
  helper (extracted from the pre-existing inline `INSERT OR IGNORE` SQL) is now used by both draft
  generation and snapshot recording, so a snapshot can be recorded for a year before any tax item or
  draft exists for it, without needing a separate "create this tax year" step.
- **UI (Tax screen):** a year-selector stepper (`MainViewModel.selectedTaxYear`/`selectTaxYear`,
  bounded so it cannot select a future year) now drives "Prepare draft" and "Reconcile recorded
  wealth"; two new buttons record an opening/closing snapshot for the selected year. Existing
  draft-history, issue-list, and reconciliation-history cards were left as-is (already functional).

**Deliberately deferred (not faked):**
- **5C tax issue center** — `TaxIssueEntity` rows are still only the ones `AnnualDraftGenerator`'s
  classifier already produces (AMBIGUOUS/EVIDENCE/UNMAPPED/NO_RULE); no additional preflight-only
  issue types (e.g. "missing opening snapshot", "duplicate candidate") were wired into persisted
  `TaxIssueEntity` rows — the missing-opening-snapshot case is instead a thrown error at
  reconciliation time, which is a narrower behavior than a persisted, browsable issue.
- **5F annual close lifecycle (OPEN → REVIEW → FILED)** — not implemented; `TaxYearEntity.status`
  still only ever gets set to `"OPEN"`.
- **Draft-line → `TaxMappingEntity` lineage walk** — a draft line still only stores source tax-item
  ids (`sourceIdsJson`), not mapping ids; there is no UI screen walking from a draft line to the
  specific `TaxMappingEntity` row(s) that produced it. The data needed to build that (mapping table,
  `getForTaxItem`) already exists from Phase 4; only the join/UI was left undone here.
- Multi-tax-year reconciliation UI polish (e.g. showing all recorded snapshots for the selected
  year inline) was not built — snapshot recording has no visible confirmation beyond the existing
  `reconciliationMessage` toast-style text.

**Tests added** (`AppDatabaseTest.kt`, androidTest, compiled only — no device this session):
`regeneratingAnnualDraftCreatesNewVersionWithoutDeletingPriorLines` (draft v1's lines are byte-for-byte
unchanged after generating v2 for the same year); `reconciliationRequiresAnOpeningSnapshotBeforeRunning`
(throws rather than silently using zero); `reconciliationUsesRecordedOpeningSnapshotAndScopesEventsToTheTaxYear`
(a prior-year event is excluded from the current year's expected-closing calculation even though it
is in the same account's history). `DatabaseMigrationTest.migrateV9ToV10AddsWealthSnapshotsTable`
mirrors the existing 8→9 migration test pattern.

Verification: `./gradlew test lint` PASS; `./gradlew assembleDebug` PASS; `./gradlew
assembleDebugAndroidTest` PASS (compiles only, not run — no device this session). New Room schema
version: **10**.
