# Build Status

Updated: 2026-08-14

## Current stage

Sprint 16 — release hardening and final evidence (in progress)

## Baseline

- Repository contained documentation and an empty Android scaffold; no Gradle project or Git metadata was present.
- Java 21.0.12 is installed.
- Android SDK platforms 26, 35 and 36 and build-tools 36.0.0 are installed.
- Gradle 8.13 is cached locally; the system Gradle 4.4.1 is too old for Android builds.
- ADB is installed. No device was connected at baseline.
- Prepared AVDs exist for API 26, 35 and 36; emulator tooling is under `$ANDROID_HOME/emulator`.

## Sprint gates

| Sprint | Status | Evidence |
| --- | --- | --- |
| 00 | PASS | `docs/verification/SPRINT_00_GATE.md` |
| 01 | PARTIAL | `docs/verification/SPRINT_01_GATE.md` |
| 02 | PARTIAL | `docs/verification/SPRINT_02_GATE.md` |
| 03 | PARTIAL | `docs/verification/SPRINT_03_GATE.md` |
| 04 | PARTIAL | `docs/verification/SPRINT_04_GATE.md` |
| 05 | PARTIAL | `docs/verification/SPRINT_05_GATE.md` |
| 06 | PARTIAL | `docs/verification/SPRINT_06_GATE.md` |
| 07 | PARTIAL | `docs/verification/SPRINT_07_GATE.md` |
| 08 | PARTIAL | `docs/verification/SPRINT_08_GATE.md` |
| 09 | PARTIAL | `docs/verification/SPRINT_09_GATE.md` |
| 10 | PARTIAL | `docs/verification/SPRINT_10_GATE.md` |
| 11 | PARTIAL | `docs/verification/SPRINT_11_GATE.md` |
| 12 | PARTIAL | `docs/verification/SPRINT_12_GATE.md` |
| 13 | PARTIAL | `docs/verification/SPRINT_13_GATE.md` |
| 14 | PARTIAL | `docs/verification/SPRINT_14_GATE.md` |
| 15 | PARTIAL | `docs/verification/SPRINT_15_GATE.md` |
| 16 | PARTIAL | `docs/verification/SPRINT_16_GATE.md` |

## Current build/test status

- Build: `./gradlew test lint connectedDebugAndroidTest assembleRelease` PASS; release R8 build completed.
- Release: locally debug-signed internal QA APK produced (not production-signed), SHA-256 `89716a2f792bb7189be9da2264102e6039298d38948963684f68dc425edffccc`.
- Connected/device: `./gradlew connectedDebugAndroidTest` PASS (22 tests) on API 36 and PASS (22 tests) on API 26; current schema-v6 release APK assembles cleanly with no app crash scan hits.
- Unit tests: `./gradlew test` PASS; backup manifest timestamp parsing has focused regression coverage.
- Lint: `./gradlew lint` PASS.
- Instrumentation: `./gradlew connectedDebugAndroidTest` PASS on `Android_16_Test` / API 36 (22 tests) and `Android_26_Test` / API 26 (22 tests), including account/salary and asset/liability capture through Compose UI, encrypted image/PDF preview rendering, primary navigation/accessibility semantics, full checked-in v2→v6 Room migration validation, persisted tax-issue/source traceability, annual draft-line source traceability, account/wealth lifecycle, tax review/reclassification, database-side balance/count aggregates, encrypted document deletion/link cleanup, populated encrypted restore, notification firing, and reminder rescheduling.
- Device: `Android_16_Test` API 36 and `Android_26_Test` API 26; release APK install/launch PASS on both APIs; notification channel `passport_reminders` exists on API 36; no app crash observed.

## Blockers and next action

- Permanent production signing key, final branding asset, public privacy-policy URL, and Play Console access remain external release decisions.
- Backup export/restore actions now use `VACUUM INTO` where available and an API 26 WAL-safe fallback that avoids unsafe WAL toggling; populated API 36 evidence verifies backup → delete → restore → Room reopen with encrypted document bytes, hashes, and links preserved. Report catalog and expanded JSON/CSV exports are wired with source identifiers.
- Wealth UI now supports valuation updates, asset disposal, liability repayment, partial receivable receipts, and detailed investment event capture. Money capture now persists and displays optional transaction categories. Home/Money use Room-side balance/count aggregates instead of collecting full event history; a 10,000-event regression dataset verifies bounded recent queries. Tax Inbox review supports validated reclassification, reviewed/excluded states, required exclusion reasons, explicit evidence-state review, and evidence-state updates after linking. Tax surfaces evidence-pending/unmapped counts and duplicate candidate groups as review-only warnings, while annual draft and reconciliation histories remain visible. Remaining gaps are recurring drafts, broad accessibility/font-scale/performance evidence, and final production release inputs.
- PDF report exports now support an explicit all-records/current-tax-year range toggle backed by canonical snapshot filtering; range behavior has JVM regression coverage.
- Next action: close remaining UI walkthrough, accessibility, performance, and release-input gaps; then rerun the final release audit.
