# Sprint 0–16 requirements traceability

Updated: 2026-08-17  
Commit under test: `6296ca1` plus the current hardening worktree

| Area | Status | Evidence / current gap |
|---|---|---|
| Sprint 0 repository/build foundation | PASS | `docs/verification/SPRINT_00_GATE.md`, debug build and unit/lint gate |
| Sprint 1 navigation/design system | PARTIAL | `NavigationSmokeTest.kt`; TalkBack, 2.0x font, and visual review remain pending |
| Sprint 2 Room/data foundation | PASS | schema exports through v8, DAO/device tests, `DatabaseMigrationTest.kt` |
| Sprint 3 security foundation | PARTIAL | PIN/Keystore tests pass; full lifecycle and biometric walkthrough pending |
| Sprint 4 money capture | PARTIAL | account/event/transfer tests; historical dates and complete edit/archive UI remain incomplete |
| Sprint 5 wealth | PARTIAL | `WealthCaptureDeviceTest.kt`; broad historical wealth capture remains incomplete |
| Sprint 6 home dashboard | PARTIAL | aggregate dashboard exists; privacy masking and performance evidence need expansion |
| Sprint 7 vault/official records | PARTIAL | encrypted preview/link/delete tests; full SAF and dependency-warning walkthrough pending |
| Sprint 8 continuous tax capture | PARTIAL | source linkage/review tests; historical/manual tax date selection remains incomplete |
| Sprint 9 versioned tax rules | PASS/PARTIAL | deterministic and invalid-ruleset JVM coverage; jurisdictional completeness is out of MVP scope |
| Sprint 10 annual workspace | PARTIAL | draft/source-line tests and UI exist; complete source drill-down walkthrough pending |
| Sprint 11 wealth reconciliation | PARTIAL | reproducibility tests/UI history exist; deliberate missing-asset device walkthrough pending |
| Sprint 12 reports | PARTIAL | report generator and PDF/CSV tests; device-open and long-report evidence pending |
| Sprint 13 backup/export/restore | PARTIAL | encrypted populated restore and migration validation; bounded-memory streaming is not complete |
| Sprint 14 reminders/notifications | PARTIAL | API 26/API 36 notification tests; recurring processor now does not post confirmed events, periodic worker/UI evidence pending |
| Sprint 15 release hardening | PARTIAL | relock/secure-screen foundations; full accessibility/adaptive walkthrough pending |
| Sprint 16 final verification | NOT TESTED | final gate must rerun after this hardening work; no physical device is attached |

This matrix is intentionally conservative: existing automated evidence is not promoted to PASS where the acceptance behavior still lacks a complete device walkthrough.
