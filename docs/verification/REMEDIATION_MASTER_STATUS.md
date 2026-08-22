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
| 1 Design/nav/onboarding | PARTIAL | Phase 1 | NOT IMPLEMENTED (onboarding), IMPLEMENTED — DEVICE VERIFICATION DEFERRED (nav/theme) | Onboarding flow, privacy masking, historical date pickers targeted this phase |
| 2 Local data | PARTIAL | Phase 1/2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Schema v8 migrations verified by JVM test; date-entry UI absent |
| 3 Security | PARTIAL | Phase 8 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | PIN/biometric/Keystore exist; full lifecycle/device evidence deferred |
| 4 Money | PARTIAL | Phase 2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Accounts/transfers work; historical dates, recurring UI gaps targeted |
| 5 Wealth | PARTIAL | Phase 2 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Domain math exists; valuation/disposal/repayment UI mostly dormant |
| 6 Home | **BROKEN** | Phase 3 | NOT IMPLEMENTED (canonical net worth) | "Net recorded movement" mislabeled as net worth; must be replaced |
| 7 Vault/records | PARTIAL | Phase 6 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Crypto/storage solid; dependency-safe delete, search, expiry absent |
| 8 Tax capture | PARTIAL | Phase 4 | NOT IMPLEMENTED (remapping/taxonomy/drill-down) | Source→tax-item link exists; lineage/remap/evidence workflow incomplete |
| 9 Rules engine | PARTIAL | Phase 4 | NOT IMPLEMENTED (JSON schema/parser/validator) | Hardcoded Kotlin map only |
| 10 Annual workspace | PARTIAL | Phase 5 | NOT IMPLEMENTED (drill-down/versioning) | Draft/issue persistence exists; selected-year workspace + lineage absent |
| 11 Reconciliation | PARTIAL | Phase 5 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Formula/history primitive + zero-diff fixture exist; UI drill-down absent |
| 12 Reports | PARTIAL | Phase 7 | NOT IMPLEMENTED (preview/full catalog) | Export-only; no in-app preview; raw `/100` formatting |
| 13 Backup/restore | PARTIAL | Phase 7 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Crypto/staging done; equivalence proof needs device (Phase 10D) |
| 14 Calendar | PARTIAL | Phase 7 | NOT IMPLEMENTED (expiry/due-date wiring) | Generic reminders exist; document/receivable/tax-review linkage absent |
| 15 UX hardening | PARTIAL | Phase 1/8 | NOT IMPLEMENTED (masking, a11y) | Targeted across Phase 1 (masking) and Phase 8 (a11y/adaptive review) |
| 16 Release | PARTIAL | Phase 8/9 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Debug-signed internal QA build exists; device/physical evidence deferred |

## Phase execution log

| Phase | Status | Commit | Notes |
| --- | --- | --- | --- |
| 0 — Baseline freeze | IN PROGRESS | (pending) | This document created; host baseline re-verified green |
| 1 — Onboarding/dates/privacy/UI foundation | NOT STARTED | — | Next |
| 2 — Canonical money/wealth completion | NOT STARTED | — | |
| 3 — Canonical home dashboard | NOT STARTED | — | |
| 4 — Versioned tax capture engine | NOT STARTED | — | |
| 5 — Annual workspace/reconciliation | NOT STARTED | — | |
| 6 — Vault/records/evidence lifecycle | NOT STARTED | — | |
| 7 — Reports/export/backup/calendar | NOT STARTED | — | |
| 8 — UX/accessibility/security/release hardening | NOT STARTED | — | |
| 9 — Implementation freeze/clone-ready handoff | NOT STARTED | — | |
| 10 — Deferred device qualification | NOT STARTED (explicitly deferred) | — | Requires emulator/device environment |

This table is updated at the end of every phase in this remediation run.
