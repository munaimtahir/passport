# Sprint 02 Gate — Local Data Foundation

Date: 2026-08-14
Status: IN PROGRESS

- Room schema now reaches v5 with canonical profile, account, financial-event, wealth, tax, vault, calendar, reconciliation, and official-record tables.
- Foreign keys and uniqueness constraints prevent duplicate source tax items and duplicate document links.
- Money remains integer minor-unit based in the domain.
- Unit coverage includes exact money arithmetic, transfer invariants, deterministic tax draft generation, ambiguous mapping issues, and wealth reconciliation.

Latest verification: `./gradlew test lint` PASS and `./gradlew connectedDebugAndroidTest` PASS on 2026-08-14 using `Android_16_Test` / API 36.

Instrumentation now covers atomic transfer writes, duplicate-source idempotency, and direct Room migration-helper execution for the full checked-in schema chain v2→6 with schema validation. Explicit migrations cover v1→2, 2→3, 3→4, 4→5, and 5→6; v1 has no exported schema in this repository and remains an external historical baseline rather than a testable artifact.
