# Current gap register

Observed gaps only; this is not an implementation roadmap.

| ID | Category | Observation | Evidence |
|---|---|---|---|
| G-001 | Functional gap | No Reset Utility operation exists; only History filter reset | `PassportApp.kt`, repository/ViewModel search |
| G-002 | Integration gap | Utility payments do not create expenses or affect accounts, tax, net worth, or reports | utility entities/repository vs `addEvent` |
| G-003 | Data-model gap | Utility payment has optional bank text but no payment account/domain ID | `PaymentRecordEntity` |
| G-004 | Possible defect | Profile delete warning promises attachment deletion, but attachment table has no FK and repository does not delete attachment rows/files | profile delete dialog; `BillAttachmentEntity`; `deleteUtilityProfile` |
| G-005 | Data-model gap | `BillAttachmentEntity.linkedId` is untyped and has no referential integrity | `Entities.kt` |
| G-006 | UX inconsistency | Utility category sets differ across Add, Bills filter, History, entity comments, and legacy recurring UI | `PassportApp.kt`, `Entities.kt` |
| G-007 | Navigation gap | Money, accounts, recurring finance, wealth, calendar, tax, vault, official records, and reports are compiled but unreachable | top-level destinations/`when` |
| G-008 | Navigation/UI gap | Report/export activity launchers exist in Settings code, but no rendered controls trigger them | `MoreDialog` |
| G-009 | Integration gap | Income auto-creates an employment tax item; expenses do not auto-create tax items | `FinanceRepository.addEvent` |
| G-010 | Functional gap | Liability/receivable repayments overwrite outstanding balances without payment-history records or ledger events | repository methods/entities |
| G-011 | Functional gap | Investments lack lots, prices, valuation history, and realized/unrealized gain calculations | investment model/reports |
| G-012 | Backend gap | `UserProfileEntity` and `ChangeLogEntity` have no DAO/database accessor and appear unused | `AppDatabase.kt`, `Daos.kt` |
| G-013 | Test gap | Current build/test/lint results unavailable because installed JDK 25.0.2 prevents task execution | audit command output |
| G-014 | Test gap | Manual runtime did not complete payment, attachment, biometric, backup/restore, or legacy subsystem workflows | runtime baseline |
| G-015 | Security concern | No screenshot-protection/`FLAG_SECURE` implementation was found | manifest/activity/source search |
| G-016 | Security concern | PIN digest/salt and lockout counters use private SharedPreferences rather than encrypted preferences; PIN hashing is PBKDF-based | `PinStore.kt`, `PinVerifier.kt` |
| G-017 | Security concern | No explicit finite lockout; exponential delay caps while failure count persists | `PinStore.canAttempt` |
| G-018 | Security gap | No deep links exist, so deep-link lock enforcement is not applicable/testable | manifest |
| G-019 | Functional gap | General document vault supports picker import but no camera capture and current shell does not expose it | `DocumentVault.kt`, navigation |
| G-020 | Documentation drift | Release ledger ends at v1.0.3 while staged build metadata and local artifacts are v1.1.0 | ledger, app Gradle, artifact metadata |
| G-021 | Architecture concern | Single `PassportApp.kt`, one ViewModel, and one repository mix utility and legacy domains | source layout |
| G-022 | Documentation drift | Prior broad-finance verification documents describe UI paths no longer reachable after utility shell reset | current destinations vs `docs/verification` |
| G-023 | Functional gap | Settings onboarding copy says PIN can be set later, but current Settings dialog has no PIN setup/change action | `Onboarding.kt`, `MoreDialog` |
| G-024 | Possible defect | Utility reconciliation includes archived months through the archive month based on `updatedAt`; archive timing semantics are implicit | `UtilityRecurrenceEngine.reconcileProfile` |

## Sprint 24 disposition (2026-08-28)

Historical observations above are retained. Implementation evidence is in
`docs/sprints/SPRINT_24_FINANCE_RECONNECTION.md`.

| Finding | Disposition |
| --- | --- |
| G-002, G-003 | Addressed in code by atomic PaymentRecord -> FinancialEvent -> Account linkage; device qualification blocked |
| G-004, G-005 | Addressed in code by typed attachment metadata and explicit profile/occurrence/payment file+metadata cleanup |
| G-006 | Addressed by canonical taxonomy with legacy mapping |
| G-007 | Partially addressed as intended: Money restored; deferred modules remain hidden |
| G-013 | Addressed: JDK 17 host build/test/lint pass |
| G-014 | Still open for live runtime because API 36 AVD could not boot without KVM |
| G-015 | Addressed with activity-level FLAG_SECURE |
| G-020 | Addressed in release ledger for versionName 1.1.0/versionCode 4 |
| G-023 | Addressed with Settings PIN setup/change/remove and current-PIN verification |

G-001 remains a non-gap by product decision: no Reset Utility feature should exist. All other
findings remain unchanged or are explicitly deferred from Sprint 24.

## Security/privacy classification summary

| Control | Status | Finding |
|---|---|---|
| PIN and failed-attempt delay | IMPLEMENTED | PBKDF record; exponential retry delay |
| Biometric/device credential | IMPLEMENTED - NEEDS VERIFICATION | shown only when available and PIN exists |
| Background relock | IMPLEMENTED | relocks on lifecycle `ON_STOP` when PIN exists |
| Inactivity timer | NOT FOUND | background event only, no elapsed inactivity timer |
| Privacy masking | IMPLEMENTED | persisted toggle masks monetary values using `MaskedPkr` |
| Screenshot protection | NOT FOUND | no `FLAG_SECURE` |
| Keystore crypto | IMPLEMENTED | AES-GCM service for files/identifiers |
| Utility/general document encryption | IMPLEMENTED | app-private encrypted files |
| Clipboard restriction | IMPLEMENTED | custom no-op text toolbar for unlock PIN fields; onboarding PIN fields do not use it |
| Automatic Android backup | IMPLEMENTED | disabled and extraction rules exclude root |
| Exported components | IMPLEMENTED/EXPECTED | launcher activity exported; no other components declared |
| Permissions | MINIMAL | notification only |
| Analytics/ads/network SDKs | NOT FOUND | no dependencies/usages found |
| Sensitive logging | NOT FOUND | no production Log/println calls found |
| Committed secrets | NOT FOUND by source/config scan | signing properties/key are described as ignored; no secret value inspected |
