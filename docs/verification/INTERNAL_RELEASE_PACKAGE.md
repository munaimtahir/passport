# Internal QA Package

Rewritten 2026-08-24 — the previous version of this file was stale (predated Phase 10 device work
entirely: it cited 28 API-26-only connected tests and a debug-only signing key, neither still
true). See `docs/verification/REMEDIATION_MASTER_STATUS.md` for full phase-by-phase evidence.

## Verdict: GO for internal testing

Internal testing (a closed Play Console track, not public release) is not gated on anything left
open in this repository. The remaining open items — production signing-key backup/Play App
Signing enrollment, and the privacy-policy public URL — are production-release gates, tracked
separately in `docs/BLOCKERS.md`, and do not block an internal track.

## Build artifacts

- AAB: `app/build/outputs/bundle/release/app-release.aab` (built via `./gradlew bundleRelease`)
- APK: `app/build/outputs/apk/release/app-release.apk` (built via `./gradlew assembleRelease`)
- Signing: real `vexel-release` key (RSA-4096, valid to 2056-08-15) — see
  `docs/RELEASE_SIGNING.md` for fingerprints and verification evidence. No longer debug-signed.
- Artifact hashes: recorded fresh at the end of each phase in
  `docs/verification/REMEDIATION_MASTER_STATUS.md`; regenerate via `sha256sum` before upload
  since this doc is not re-run automatically on every commit.

## Automated verification

- Host gate: `./gradlew test lint` — green as of every phase in this remediation run, most
  recently after the Phase 10/11 device and feature work.
- Device gate: `./gradlew :app:connectedDebugAndroidTest` — 43-50 tests (count grew across Phase
  10 as device-lifecycle/notification/backup-equivalence/E2E/performance test classes were added),
  passing on:
  - API 26 (`Android_26_Test`)
  - API 36 (`Android_16_Test`)
  - API 35 (`Android_15_Test`) — used for accessibility spot checks and a bonus crash-scan, not a
    full connected-suite pass every time
- Crash-scan smoke (install → force-stop → clear logcat → `monkey` launch → grep `FATAL
  EXCEPTION`): clean on API 26 and API 36.

## Device/emulator evidence basis

Per the user's explicit 2026-08-23 instruction, no physical Android device is available in this
environment and emulator evidence is accepted in its place for internal-release qualification —
see the closed row in `docs/BLOCKERS.md`. Beyond the connected-suite pass, this run additionally
covered (real, executed, not merely implemented):

- Manual E2E walkthrough (onboarding → PIN → account → income/expense → asset/liability → tax
  item → annual draft → report preview → lock/unlock)
- UI-driven backup → clear → restore equivalence proof (real fixture data entered through the
  actual Add-account dialog, not a repository-level fixture)
- Device-lifecycle: inactivity/backgrounding relock, `deleteAllData` → onboarding re-entry without
  a process kill (found and fixed a real bug here — see below), rotation/process-death for the
  Phase 8 `rememberSaveable` dialog fields
- Real notification delivery (a scheduled reminder actually reaches the system tray, not just
  permission-granted)
- Synthetic large-dataset performance check (2,000 events + 500 tax items, list load and annual
  draft generation stay well under generous responsiveness ceilings)
- Accessibility/adaptive spot checks on the one attached emulator with TalkBack available (API
  35): TalkBack confirmed actively engaging via screenshot evidence; 1.3x font scale and landscape
  rotation both reflow the onboarding/PIN screens correctly with no clipping

Deep-link lock enforcement and biometric cancel-does-not-unlock are documented rather than
device-tested — the former because no deep links exist anywhere in this app to test against, the
latter because neither attached emulator has an enrollable biometric (hardware feature flag
present, no enrollment activity resolvable) — both are explained in detail in
`SecurityLifecycleDeviceTest.kt`'s class doc comment and in the ledger.

## Real defects found and fixed during Phase 10 device work

1. **`deleteAllData` left the app unable to show onboarding again without a manual process kill**
   — a bug flagged as an open question since Phase 1, now confirmed and fixed:
   `Context.getSharedPreferences` caches one in-memory instance per file per process, so the old
   code's raw file deletion never updated the already-constructed `AppPreferences`/`PinStore`.
   Fixed via live-instance `.clear()` calls plus switching `OnboardingGate`'s saved flag from
   `rememberSaveable` to plain `remember`.
2. **The "More" dialog's button list was not scrollable** — "Delete all application data" (and
   other later buttons) were unreachable off-screen on realistic device heights. Fixed with a
   bounded, scrollable `Column`.

Neither defect was previously known; both were caught by writing real device tests for
previously-unscoped mega-prompt items, not by a dedicated bug hunt.

## Known, documented gaps (not release-blocking, tracked as scope)

See `docs/verification/REMEDIATION_MASTER_STATUS.md` and `docs/verification/ACCEPTANCE_MATRIX_PHASE9.md`
for the full list of deliberately-deferred functionality (tax-mapping lineage drill-down UI,
persisted preflight tax issues, tax-year lifecycle states, a second ruleset version, guided
onboarding account setup, receivable/tax-deadline calendar reminders, PIN-field clipboard
restriction) — Phase 11 in this same remediation run addresses these; check the ledger for
current status of each before assuming any is still open.
