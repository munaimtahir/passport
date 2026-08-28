# Sprint 24 — Finance Reconnection and Unified Ledger

Date: 2026-08-28  
Implementation commit: `aa3632752063a24edafa18f7d98fdfcce4b3c3e0`  
Verdict: **BLOCKED at device qualification; host implementation gates pass**

## Baseline

Discovery described a working Home/Bills/History utility tracker over a compiled but unreachable
finance backend. At sprint start the repository was clean at `c2cc536` on `main`; the discovery
report's staged version bump was no longer staged because current code already contained
versionName 1.1.0/versionCode 4. JDK 17.0.18 was available and replaced the discovery machine's
JDK-25 blocker without application-source workarounds.

## Implemented

- Added canonical utility taxonomy and legacy compatibility mapping for Electricity, Gas, Water,
  Internet, Mobile / Telephone, Subscription / Service and Other.
- Reconnected Money as a primary destination, with account cards, active/archive/reactivate,
  account type and context, income, expense, transfer and unified activity.
- Added Home financial summary and quick Income/Expense/Bill actions while retaining bill status
  and attention views.
- Added mandatory active-account selection to utility payment creation/editing.
- Added atomic, idempotent utility payment/ledger operations. A payment creates exactly one linked
  EXPENSE, edits mutate that event, account changes move the expense, and deletion removes it.
- Preserved provenance in the event description/notes: utility, canonical category, billing period,
  payment ID and occurrence ID. Tax relevance remains UNKNOWN rather than automatically deductible.
- Added attachment type metadata and profile/occurrence/payment cleanup of metadata and encrypted
  files through the existing vault lifecycle.
- Added Settings PIN setup/change/remove with current-PIN verification for changes/removal.
- Enabled activity-level `FLAG_SECURE` screenshot and recent-preview protection.

## Architecture and database

```text
UtilityBillProfile
  -> MonthlyBillOccurrence
  -> PaymentRecord(accountId, financialEventId)
  -> FinancialEvent(EXPENSE, category=Utilities)
  -> Account
```

Room is version 14. `MIGRATION_13_14` adds account context, payment account/event linkage with
indices and a unique financial-event link, and typed utility-attachment linkage. It preserves old
rows with nullable linkage/default UNKNOWN metadata; editing a legacy payment with an active account
creates its missing canonical ledger event. No destructive migration is configured. Schema 14 is
exported.

The account context column is an intentionally lightweight, extensible attribute rather than a new
domain entity. It does not overload account type: for example, context `Clinic / Professional` and
type `CASH` remain independent.

## Host verification

Command:

```text
./gradlew test lint assembleDebug assembleDebugAndroidTest assembleRelease --no-daemon --max-workers=2
```

Result: PASS. JVM suites executed 78 unique tests per build variant (156 total executions), with
zero failures/errors/skips. Debug APK, instrumentation APK and minified release APK assembled.
Lint completed with zero errors (26 pre-existing/non-blocking warnings and five hints).

Focused coverage added for taxonomy compatibility, payment creation/provenance, idempotency,
balance arithmetic (Rs 120,000 scenario), report inclusion, payment amount/date/account edits,
payment deletion, profile/attachment cleanup, v13->v14 migration, and backup linkage assertions.

## Emulator and connected verification

An API 36 Google APIs x86_64 image and `Sprint24_API_36` Pixel 8 AVD were installed/created locally.
The host exposes no `/dev/kvm`, so the emulator had to use TCG software emulation. Three clean-boot
attempts were made with SwiftShader, graphics-off/lavapipe, and `-qemu -cpu max`. The first crashed
after repeated QEMU CPU hangs; later attempts remained in first-boot system dexopt for more than 20
minutes and never set `sys.boot_completed`. Android rejected APK installation with `device is still
booting`. Consequently the connected suite, UI/ADB walkthrough, process-death check and actual
backup/restore round trip could not be executed in this environment.

This is recorded in `docs/BLOCKERS.md`. Compiling the complete instrumentation APK passed, but that
is not represented as an executed connected-test pass.

## Financial scenario status

The device test encodes and asserts:

```text
Opening balance        Rs 100,000
Income                +Rs  50,000
Manual expense        -Rs  10,000
Utility expense       -Rs  20,000
Expected balance       Rs 120,000
```

It further asserts edit to Rs 18,000, account reassignment, stable event identity, and deletion.
The test APK compiled, but the values are expected assertions rather than device-observed results
until the connected blocker is cleared.

## Regression and deferred scope

Monthly reconciliation still inserts distinct profile/year/month occurrence rows and never mutates
prior cycle amounts, payments or evidence. No Reset Utility operation was introduced. Wealth, Tax,
Vault, Calendar and Reports remain outside top-level navigation. Reports reuse canonical financial
events; no utility-specific report calculator was added.

The only Sprint 24 acceptance gap is external device qualification: connected tests, UI/ADB scenario,
visual review, process death and live backup/restore remain unverified. The implementation must not
be called a full PASS until those gates run on an API 36 emulator/device with hardware acceleration.
The clone-to-device-workstation procedure, exact financial fixtures, expected balances and evidence
template are documented in `docs/verification/SPRINT_24_DEVICE_TEST_PLAN.md`.

## Release state

versionName 1.1.0, versionCode 4. Debug and local release APKs exist; no Play Store publication was
performed. Artifact hashes are recorded in `docs/RELEASE_LEDGER.md` as local Sprint 24 evidence.
