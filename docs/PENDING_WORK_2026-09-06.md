# Vexel Finance Passport — Pending Work (as of commit `b355606`)

**Read this document first, then read `docs/CURRENT_REPOSITORY_DISCOVERY_2026-09-06.md` (and its
addendum) for the full evidence trail behind these items.** This document is the actionable
follow-up list; the discovery report is the audit that produced it. Both were written from a
container **without hardware-accelerated emulation** (no `/dev/kvm`, no CPU `vmx`/`svm`), so every
item below that needs a device/emulator is unverified from this environment by construction, not
because it was skipped carelessly.

## How to use this document

1. Confirm you're on `main` at or after commit `b355606` (`git log -1`).
2. Work items in priority order (P0 → P7). Each item lists its evidence source, what "done" looks
   like, and the files it touches.
3. **Do not mark an item done from source inspection alone if it says "needs device verification"**
   — this repo has a documented history of commits claiming completion that an independent gate
   re-run then disproved (see the discovery report's §1 and §17). Run the actual command and cite
   the result.
4. When you close an item, record evidence the same way the rest of `docs/verification/` does
   (command run, device/emulator identity, pass/fail counts) rather than just narrating success in
   a commit message.
5. Update this file as items close — check them off with the evidence file/line that proves it,
   don't just delete the row.

## Snapshot as of `b355606`

- `pk.vexel.financepassport`, versionCode **5**, versionName **1.1.5**, Room schema **17**.
- Host gates all pass from a clean tree: `./gradlew clean test lint assembleDebug bundleRelease
  compileDebugAndroidTestKotlin`.
- `docs/RELEASE_LEDGER.md`'s newest entry (v1.1.5) presents this build as "ready for release
  upload" to Play — **do not upload it** until P1/P2 below are resolved; the same repository's own
  `docs/verification/WAVE_C_D_E_FINAL_VERIFICATION.md` and `WAVE_F_G_H_DEVICE_VERIFICATION.md`
  explicitly say the underlying features are *not* accepted as complete. That contradiction between
  the ledger and the wave verification docs is itself part of P6.
- No device/instrumentation test has been run against this exact commit from any environment this
  session had access to. The most recent claimed device runs (102/102 connected tests, "Wave J
  ACCEPTED") are against a commit in the same lineage but were performed in a different, KVM-capable
  environment this session could not reproduce or independently confirm.

---

## P0 — Get real device evidence for current HEAD

**Why:** No session in this repo's visible history has run the connected test suite against the
exact commit that includes this session's new migration test
(`migrateV15ToV16AddsCategoriesRecurringAndSettlementTablesWithoutDroppingData`,
`DatabaseMigrationTest.kt`) or confirmed the Wave J OOM fix (`recreate()` removal, see
`docs/verification/WAVE_J_DEVICE_VERIFICATION.md`) still holds at this exact HEAD.

**Do this:**
```
git pull origin main
./gradlew clean test lint assembleDebug assembleDebugAndroidTest
ANDROID_SERIAL=<your emulator serial> ./gradlew connectedDebugAndroidTest
```
**Done when:** full connected suite result recorded (executed/passed/failed/skipped counts, device
identity, API level) in a new `docs/verification/` file, following the format of
`docs/verification/WAVE_J_DEVICE_VERIFICATION.md`. If anything fails, fix it before moving to P1 —
do not proceed on an unverified base.

---

## P1 — Wave C–E UI acceptance (recurring templates, liability/receivable settlement, simple investments)

**Why:** `docs/verification/WAVE_C_D_E_FINAL_VERIFICATION.md` §"Final verdict" explicitly states:
*"**NOT ACCEPTED for complete Wave C–E acceptance.** Automated and database gates are green, but
mandatory UI integration/device scenarios remain incomplete."* Specifically: "recurring-template
workflows, liability/receivable settlement workflows, and simple-investment workflows" have no
device-verified running-app path, and per this session's own inspection, `PositionScreen`
(`app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt:762`) only **lists** assets/
liabilities/investments/receivables and toggles an asset's net-worth inclusion — there is no
creation/edit UI for any of them, and the capture tray (`VexelCaptureAction`,
`ui/components/VexelCaptureTray.kt`) only offers Expense/Income/Transfer/Bill.

**Backend already exists and is tested:** `RecurringDomain.kt`, `SettlementDomain.kt`,
`core/database/Entities.kt` (`RecurringTemplateEntity`, `ExpectedOccurrenceEntity`,
`SettlementEventEntity`, `SimpleInvestmentEntity`), `RecurringDomainTest.kt`,
`SettlementDomainTest.kt`.

**Do this:** design and add the missing capture/edit UI:
- Create/edit a recurring template (non-bill recurring income/expense), confirm/skip its expected
  occurrences.
- Record a liability/receivable settlement event (principal + financing-cost split) from the
  Position screen instead of only viewing outstanding balances.
- Create/edit a simple investment (title, type, principal, current value) instead of only listing
  existing rows.

**Done when:** each workflow is reachable from the running app, has a device-verified acceptance
test (extend `docs/15_ACCEPTANCE_TEST_CATALOG.md`'s crosswalk or add an equivalent), and
`docs/verification/WAVE_C_D_E_FINAL_VERIFICATION.md` (or a new final-acceptance doc) can honestly
say ACCEPTED rather than NOT ACCEPTED.

---

## P2 — Wave F–G–H UI acceptance (asset/calendar/vault workflows)

**Why:** `docs/verification/WAVE_F_G_H_DEVICE_VERIFICATION.md` says: *"Full F/G/H acceptance
scenarios involving creation/editing of every asset, calendar source, reminder action, camera
import, and multi-record evidence thread remain incomplete and are not claimed as PASS."*
`docs/verification/WAVE_F_G_H_BASELINE.md` adds that source-specific F/G/H actions "remain only
partially integrated" and that the Wave F net-worth formula needs review against legacy investment
events.

**Do this:**
- Asset creation/editing (not just the include/exclude-in-net-worth toggle already in
  `PositionScreen`).
- Calendar: verify/extend source-aware linking (a calendar item should trace back to the bill,
  liability, or document that generated it — `CalendarProjection.kt`, `CalendarProjectionTest.kt`
  already model this; confirm the UI actually uses it, `CalendarScreen`,
  `PassportApp.kt:803`).
- Vault: confirm camera import exists (not just Storage-Access-Framework picker import —
  `docs/discovery/CURRENT_GAP_REGISTER.md` G-019 originally flagged this as missing) and that a
  document can carry multiple linked evidence records.
- Re-verify the net-worth formula against the current (not legacy) investment/settlement model per
  the baseline doc's own flagged concern.

**Done when:** each item above is reachable, tested at the device level, and the relevant Wave F/G/H
gate doc is updated from "incomplete" to a dated PASS with evidence.

---

## P3 — Tax (deferred "Wave I") has no reachable UI

**Why:** `docs/architecture/WAVE_J_DISCOVERY_MAP.md` explicitly lists *"Wave I tax workspace |
present from prior work | DEFER; untouched by Wave J."* This session's independent inspection
confirms: `MainViewModel.taxItems` and `getMappingHistory` are exposed but never read anywhere in
`PassportApp.kt`; no `TaxScreen` composable exists; no bottom-nav destination or dialog reaches the
tax classifier, annual draft, or reconciliation. The backend (`core/taxrules/TaxDomain.kt`,
`TaxRulesetJson.kt`, `BundledTaxRulesets.kt`, `TaxEngineTest.kt`, `TaxRulesetLoaderTest.kt`,
`TaxReadinessTest.kt`) is implemented and JVM-tested, but is completely inert from a user's
perspective. This is the single largest gap between the product's documented identity (`CLAUDE.md`:
Continuous Tax Capture is "a real, load-bearing feature… just not the app's identity") and its
actual shipped surface.

**This is a product decision, not a pure bug fix — do not build a full Tax Inbox UI without
confirming scope with the repository owner first.** At minimum, decide and record one of:
(a) build a Tax destination (Tax Inbox → item review → annual draft → reconciliation, per
`docs/02_FUNCTIONAL_SPECIFICATION.md` / `docs/05_CONTINUOUS_TAX_CAPTURE_ENGINE.md`), or
(b) formally mark Tax as post-MVP/deferred in `README.md` and `docs/17_POST_MVP_ROADMAP.md` so
future audits stop flagging it as an unexplained gap, or
(c) remove the now-dead `taxItems`/`getMappingHistory` wiring if a decision is made not to expose it
soon (only if genuinely never going to be used — otherwise leave it, since the backend is real and
tested).

**Done when:** one of the three options above is deliberately chosen and documented (not silently
decided in a commit).

---

## P4 — Security: no elapsed-time inactivity lock

**Why:** `docs/discovery/CURRENT_GAP_REGISTER.md`'s security classification table and this session's
own inspection of `SecurityGate.kt` both confirm: relock only fires on `Lifecycle.Event.ON_STOP`.
A user who backgrounds nothing (app stays foregrounded, screen stays on, device idle) is never
relocked, which is a real gap against `CLAUDE.md`'s "inactivity relock" requirement.

**Do this:** add an elapsed-time-since-last-interaction timer (e.g., a `SystemClock.elapsedRealtime`
timestamp updated on user interaction, checked on a lifecycle event or a short-interval check) that
relocks after a configurable idle threshold, independent of backgrounding.

**Done when:** a device test demonstrates that leaving the app foregrounded and idle past the
threshold triggers the PIN/biometric gate.

---

## P5 — Reconcile the release ledger with the wave-acceptance verdicts

**Why:** `docs/RELEASE_LEDGER.md`'s v1.1.5 entry says "ready for release upload" while
`docs/verification/WAVE_C_D_E_FINAL_VERIFICATION.md` and `WAVE_F_G_H_DEVICE_VERIFICATION.md` (same
lineage, same versionCode-adjacent commits) say the underlying features are **not accepted**. One
of these is wrong, or the ledger entry needs a caveat.

**Do this:** once P1/P2 close (or a deliberate decision is made to ship v1.1.5 without them, e.g. as
an internal build with those surfaces intentionally hidden/behind a flag), update the ledger entry
to state clearly what is and isn't included, or add a new ledger entry for whatever version actually
ships.

**Done when:** the ledger and the wave-verification docs agree on what a given versionCode contains.

---

## P6 — Documentation: `CLAUDE.md` / `AGENTS.md` still describe a nonexistent repo state

**Why:** Both files say "no Android application code" / "no Gradle project checked in yet." The
repository has had a full Gradle Android project, 100+ commits, and multiple production release
candidates for some time. See discovery report §17 for the full drift analysis. This isn't cosmetic
— it actively misleads any future session (human or AI) that reads these files first, as this
session's own task did.

**Do this:** rewrite `CLAUDE.md`'s "Repository state" section and `AGENTS.md`'s "Project Structure &
Build" sections to reflect: single `:app` Gradle module (not the originally planned multi-module
layout), current build/test commands (`./gradlew clean assembleDebug`, `test`, `lint`,
`connectedDebugAndroidTest`), current versionCode/versionName, and the actual reachable/unreachable
feature surface (link to the discovery report rather than re-describing it, so it doesn't go stale
again).

**Done when:** a fresh reader of `CLAUDE.md`/`AGENTS.md` alone would form an accurate picture of the
repo without needing this document.

---

## P7 — Low-priority lint/hygiene cleanup

**Why:** `./gradlew lint` passes but reports 25 warnings / 3 hints (7 `UseKtx`, 6 `GradleDependency`
+ 4 `NewerVersionAvailable` + 1 `AndroidGradlePluginVersion`, 2 `ObsoleteSdkInt`,
2 `MonochromeLauncherIcon`, 1 `UnusedResources`). None block a gate. Low priority — pick up only
after P0–P6.

**Do this:** address opportunistically; re-run `./gradlew lint` after each change to confirm the
warning count actually drops (don't just suppress).

---

## Recommended order

P0 (get real device evidence for current HEAD) must come first — everything else's "done" criteria
depends on being able to run `connectedDebugAndroidTest` at all. After that: P1 and P2 are the
biggest product gaps and are independent of each other (parallelizable across two agents/sessions
if useful). P3 needs an explicit decision before code changes. P4 is small and self-contained. P5
follows naturally once P1/P2 close. P6 can happen any time, ideally once P1–P3 settle so it's
written against a stable target instead of needing another rewrite. P7 is opportunistic.
