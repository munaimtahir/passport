# Wave F–H Implementation Verification

## Implemented in this pass

- Added immutable position_snapshots storage, Room schema version 17, and additive migration 16 to 17.
- Exposed manual/monthly position snapshot recording and export.
- Added deterministic source calendar projection for bills, recurring items/occurrences, liabilities, receivables, investment maturity, document expiry, and official-record expiry.
- Reconciled source calendar rows on application startup and after restore with stable IDs and cancellation of stale projections.
- Added document unlink and replacement-link operations while preserving many-to-many links and tax evidence state.
- Added optional camera hardware declaration required by lint.
- Added targeted calendar projection, export, and migration tests.

## Verification

Passed:

    ./gradlew clean
    ./gradlew test lint assembleDebug assembleDebugAndroidTest
    ./gradlew connectedDebugAndroidTest

Host gates passed after a clean build. Connected regression passed 102/102 on the required passport (AVD) - 16 emulator at emulator-5562, with zero failures and zero skips.

The required device-specific regression gate is now verified.

## Remaining acceptance work

The full locked F-H scope is not declared VERIFIED / ACCEPTED yet. Dedicated UI/device evidence is still required for complete focused position CRUD and drill-down, all Calendar source actions and privacy deep links, full record-first/Vault-first multi-link UI, replacement lineage presentation, dependency-count deletion UX, plaintext-residue checks, and semantic backup/restore comparison.
