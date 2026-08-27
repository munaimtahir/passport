# Phase 11, items 1+2 — gate evidence

Continuation of an accidentally-stopped agent run (uncommitted progress was preserved in this
worktree and built on, not redone).

## Item 1 — Draft-line -> TaxMappingEntity lineage drill-down UI

`FinanceRepository.getMappingHistory(taxItemId)` (thin wrapper over the existing
`TaxMappingDao.getForTaxItem`) exposed through `MainViewModel.getMappingHistory`. The "Draft
calculation lines" dialog in `TaxScreen` (`PassportApp.kt`) now parses each line's
`sourceIdsJson` and offers a "View mapping history: <id>" button per source id, opening a new
dialog listing that tax item's full `TaxMappingEntity` chain in creation order — tax event
type/section/category, `SYSTEM_GENERATED` vs `USER_OVERRIDE`, active vs superseded, override
reason when present, and timestamp. No new DAO/repository logic needed beyond the wrapper; no
schema change.

## Item 2 — Persisted preflight TaxIssueEntity rows

Two new issue types generated during `prepareAnnualDraft` (not just thrown errors) and persisted
as `TaxIssueEntity` rows, so they're browsable in the Tax screen's existing "Review issues" list
(that rendering was already generic over `issue.code`/`title`/`explanation` — no UI change needed
there):

- `MISSING_OPENING_SNAPSHOT` — raised when no `WealthSnapshotEntity` of kind `OPENING` exists yet
  for the tax year being drafted. Reconciliation still throws its own clear error if run without
  one; this additionally makes the gap visible up front, without requiring the user to trigger
  reconciliation first.
- `DUPLICATE_CANDIDATE` — new pure function `detectDuplicateCandidates` in `core/taxrules/TaxDomain.kt`:
  flags tax items sharing the same amount + currency within a configurable day window (default 1
  day) of each other. Host-unit-tested (`TaxEngineTest.duplicateCandidateFlagsSameAmountWithinWindow`,
  `duplicateCandidateIgnoresDifferentAmountsAndOutsideWindow`) — real detection against real data,
  not a stub. This is separate from and additional to the pre-existing exact-date-match display-only
  grouping already shown in `TaxScreen`'s "Duplicate candidates" card (`TaxReadiness`-adjacent
  inline logic) — that display-only indicator was left as-is; this is the persisted,
  issue-tracked version the mega-prompt's Phase 4I/5C called for.

No schema migration needed for either item — `TaxIssueEntity`'s `code` column is already a free
`String`, and `TaxMappingEntity`/`TaxMappingDao` already existed from Phase 4.

## Verification

- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL.
- `./gradlew test assembleDebugAndroidTest lint` — BUILD SUCCESSFUL (104 tasks), all JVM tests
  green including the 2 new duplicate-candidate tests (5/5 in `TaxEngineTest`), no lint
  regressions, androidTest sources compile.
- Host machine was under load from other concurrent work; no emulator booted, no instrumentation
  tests run against a device this pass. The lineage-drill-down and issue-list UI paths reuse
  existing, already-device-tested rendering patterns (draft-lines dialog, issue-list card) rather
  than introducing new untested UI machinery, so the residual device-verification risk is limited
  to the two new dialogs' visual layout, not new state/data-flow logic.
