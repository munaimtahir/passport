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
| 15 UX hardening | PARTIAL | Phase 1/8 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED (masking); NOT IMPLEMENTED (a11y) | Global privacy masking landed in Phase 1; a11y/adaptive review remains Phase 8 |
| 16 Release | PARTIAL | Phase 8/9 | IMPLEMENTED — DEVICE VERIFICATION DEFERRED | Debug-signed internal QA build exists; device/physical evidence deferred |

## Phase execution log

| Phase | Status | Commit | Notes |
| --- | --- | --- | --- |
| 0 — Baseline freeze | DONE | `febea67` | This document created; host baseline re-verified green |
| 1 — Onboarding/dates/privacy/UI foundation | DONE (scoped) | `9b8d9ed` | See detail below |
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
