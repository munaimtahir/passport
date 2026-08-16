# Requirements Traceability — Hardening Baseline

Date: 2026-08-16  
Branch: `hardening/internal-release-20260816`  
Baseline commit: `6296ca1`

This is the working traceability matrix for the internal-release hardening pass. Existing sprint evidence remains authoritative for behavior already verified; PARTIAL and NOT TESTED items remain active backlog.

| Sprint / canonical area | Status | Evidence / implementation | Remaining work |
| --- | --- | --- | --- |
| 00 repository/build foundation | PASS | `docs/verification/SPRINT_00_GATE.md`; baseline build below | Keep evidence current |
| 01 onboarding/security | PARTIAL | `SecurityGate.kt`, `PinStore.kt`, `SPRINT_01_GATE.md` | Full onboarding walkthrough and accessibility evidence |
| 02 money/accounts | PARTIAL | `FinanceRepository.kt`, account/event device tests | Expand editing, errors, historical UI coverage |
| 03 transfers | PASS for core invariant | `FinancialEventTest.kt`, `AppDatabaseTest.kt`, explicit selectors in `PassportApp.kt` | Device evidence for arbitrary account pairs |
| 04 wealth | PARTIAL | `InvestmentDomainTest.kt`, wealth device tests | Historical valuation/disposal workflows |
| 05 continuous tax capture | PARTIAL | `TaxEngineTest.kt`, `SPRINT_05_GATE.md` | Deeper source navigation and ruleset evidence |
| 06 annual draft/reconciliation | PARTIAL | `ReportsTest.kt`, annual draft/reconciliation UI | Filing/revision lineage and full drill-down |
| 07 vault/evidence | PARTIAL | `DocumentVault.kt`, vault device tests | Dependency warning and broader file failure evidence |
| 08 reports/exports | PARTIAL | `Reports.kt`, `DataExport.kt`, report tests | Full CSV catalog and manual PDF review |
| 09 calendar/reminders | PARTIAL | `ReminderSchedulerTest.kt`, device reminder test | Notification permission and duplicate-work evidence |
| 10 backup/restore | PARTIAL | `BackupPackageTest.kt`, `LiveRestoreServiceTest.kt`, device restore tests | Bounded-memory streaming implementation |
| 11–14 canonical workflows | PARTIAL | Existing sprint gates and acceptance matrix | Complete missing workflows from backlog |
| 15 UX/accessibility | NOT TESTED | Existing smoke tests | TalkBack, 2.0x font, rotation/layout evidence |
| 16 release hardening | PARTIAL | `BUILD_STATUS.md`, `FINAL_VERIFICATION.md` | Final API 26/API 36 and end-to-end release verdict |

## Phase A/B evidence — 2026-08-16

- Repository root, branch, remotes, and current commit inspected; no pre-existing worktree changes were present.
- Real Git directory remained `.git`; root Git-internal copies were confirmed by `file`, `git ls-files`, and comparison with the healthy `.git` state, then removed narrowly.
- Baseline: `./gradlew clean assembleDebug test lint` — PASS (91 tasks, 10m26s; first local Gradle distribution initialization).
- Phase B rerun: `./gradlew clean test lint assembleDebug` — PASS (91 tasks, 2m24s).
- Focused regression: `PkrMoneyInputTest` — PASS.
- Corrections: strict whole-PKR parser/formatter; explicit event/recurring/transfer account selectors; active-account repository validation; selected-date support for events, transfers, and manual tax items; canonical migration array used by normal and restore validation; schema-8 backup manifest; guarded delete-all cleanup.
- Known non-failing warnings: Room processor-option warning, unstrippable `libandroidx.graphics.path.so`, and deprecated Compose `menuAnchor()` overload.
