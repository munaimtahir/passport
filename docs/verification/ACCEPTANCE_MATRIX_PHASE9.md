# Acceptance Matrix — Phase 9 (post-remediation, host-side only)

This maps every AT-### in `docs/15_ACCEPTANCE_TEST_CATALOG.md` to its current implementation
state after remediation phases 0-8 (commit `f8dd4ad`). It supplements, and does not overwrite,
the pre-remediation `docs/verification/ACCEPTANCE_MATRIX.md`.

**No device or emulator was used anywhere in phases 0-8.** No row below is marked PASS on the
strength of this remediation run alone unless an automated JVM/host test actually asserts the
behavior; every row that ultimately needs a rendered UI, a real biometric prompt, a real
notification, or a real file picker is marked **DEVICE REQUIRED** regardless of how complete the
underlying code is — that determination is Phase 10's job, not this one's.

Legend: **Impl.** = implementation path. **Auto test** = JVM/androidTest name if one exists
("(compiled, not run)" for androidTest since no device is available this session). **Device
action needed** = what Phase 10 must actually do. **Status** = HOST-VERIFIED / IMPLEMENTED-DEVICE-REQUIRED / NOT IMPLEMENTED.

## Onboarding
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-001 | `OnboardingGate`/`OnboardingFlow` (`ui/Onboarding.kt`), gates `MainActivity` before `SecurityGate` | `AppPreferencesTest` (compiled, not run) | Fresh install, confirm onboarding shown first | IMPLEMENTED-DEVICE-REQUIRED |
| AT-002 | No network calls anywhere in onboarding | — | Airplane mode + fresh install | IMPLEMENTED-DEVICE-REQUIRED |
| AT-003 | Onboarding has no guided-account-setup step to skip — it hands off directly to PIN creation | — | N/A — guided setup was never built (Phase 1 deferred it); nothing to skip because there is no optional step | NOT IMPLEMENTED |
| AT-004 | `SecurityGate` requires PIN after onboarding | `PinVerifierTest` | Complete onboarding, confirm PIN screen blocks entry | IMPLEMENTED-DEVICE-REQUIRED |

## Money
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-010 | `FinanceRepository.addAccount` incl. institution/notes (Phase 2) | `AppDatabaseTest`, `MoneyCaptureDeviceTest` (compiled, not run) | Add account via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-011 | `EditAccountDialog`/`updateAccount` | `AppDatabaseTest` | Edit account via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-012 | Account archive preserves event history (no cascading delete) | `AppDatabaseTest` | Archive, confirm history intact | IMPLEMENTED-DEVICE-REQUIRED |
| AT-013 | `AddEventDialog` now includes `DateField` (Phase 1) | `MoneyCaptureDeviceTest` (compiled, not run) | Add salary via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-014 | Same path as AT-013, `EXPENSE` type | `MoneyCaptureDeviceTest` (compiled, not run) | Add expense via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-015 | `FinanceRepository.transfer` — paired signed rows, transfer-link table | `FinancialEventTest`, `AppDatabaseTest` (transfer-exclusion invariant explicitly tested) | Transfer via UI, confirm both balances change, income/expense totals don't | HOST-VERIFIED (domain logic); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-016 | No date/category/account filter UI on the Money activity list — money activity is currently unfiltered | — | N/A | NOT IMPLEMENTED |

## Wealth
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-020 | `AddWealthDialog` (ASSET mode) | `WealthCaptureDeviceTest` (compiled, not run) | Add asset via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-021 | Asset disposal dialog, keeps the asset row (status change, not delete) | `AppDatabaseTest` | Dispose via UI, confirm history remains | IMPLEMENTED-DEVICE-REQUIRED |
| AT-022 | Liability add + `AmountDialog`-driven repayment | `AppDatabaseTest` | Add + repay via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-023 | Receivable add + partial-receipt dialog | `AppDatabaseTest` | Add + partial receipt via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-024 | Investment BUY event, non-hardcoded account label (Phase 2 fix) | `InvestmentDomainTest`, `AppDatabaseTest` | Buy via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-025 | Investment SELL event, partial-quantity supported | `InvestmentDomainTest` (partial-sale case) | Sell via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-026 | Investment DIVIDEND/PROFIT event types with gross/withheld amounts | `InvestmentDomainTest` | Record dividend via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-027 | `core/model/FinancialPosition.kt` (Phase 2) — single canonical net-worth calculation now used by Home, Wealth reconciliation, and Reports | `FinancialPositionTest` (deterministic fixture) | Confirm Home/Wealth/Reports all agree on-screen | HOST-VERIFIED (calculation); IMPLEMENTED-DEVICE-REQUIRED (cross-screen UI consistency) |

## Tax capture
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-030 | `TaxReviewDialog` relevance field | `TaxEngineTest` | Mark relevance via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-031 | Tax-year assignment from event date; `insertIfAbsent`-style uniqueness on recompute (Phase 4) | `TaxEngineTest`, `AppDatabaseTest` (reclassification-supersedes-not-duplicates case) | Confirm single item across a UI walkthrough | HOST-VERIFIED (uniqueness invariant); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-032 | `ManualTaxItemDialog`/`addManualTaxItem` | `AppDatabaseTest` | Add manual item via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-033 | `linkDocument` sets tax-item evidence state to ATTACHED; Phase 6 also reverts it correctly on document delete | `DocumentLifecycleDeviceTest` (compiled, not run) | Attach evidence via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-034 | `reviewTaxItem` requires a non-blank reason when state is EXCLUDED | `AppDatabaseTest` | Exclude via UI, confirm reason required | HOST-VERIFIED (requirement enforced); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-035 | `reviewTaxItem` reclassification | `AppDatabaseTest` | Reclassify via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-036 | `TaxMappingEntity` (Phase 4): reclassification supersedes the prior mapping (`supersededByMappingId`) instead of only mutating `TaxItemEntity`, and never duplicates the underlying tax item | `AppDatabaseTest.manualTaxItemGetsASystemGeneratedMappingAndReclassificationSupersedesRatherThanReplaces` | Reclassify via UI, inspect no duplicate row created | HOST-VERIFIED |
| AT-037 | Ruleset version is recorded per mapping/draft; only one ruleset version exists so multi-version preservation is architecturally supported but not exercised by a second version | `TaxRulesetLoaderTest` | N/A until a second ruleset version exists | IMPLEMENTED-DEVICE-REQUIRED (partial — single version only) |

## Annual draft
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-040 | `prepareAnnualDraft(year)` (Phase 5, now takes an explicit selected year) | `TaxEngineTest` | Generate via UI for a chosen year | HOST-VERIFIED (generation logic); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-041 | Draft section totals sum from `sourceIdsJson`-linked `TaxItemEntity` rows | `TaxEngineTest` (draft-line source regression) | Cross-check totals in UI | HOST-VERIFIED |
| AT-042 | `TaxDraftLineEntity.sourceIdsJson` → source `TaxItemEntity` lookup exists; no UI drill-down screen built | `TaxDraftDao.getLines` covered by `AppDatabaseTest` | A drill-down screen is Phase 5's explicitly-deferred item — needs to be built before this can be device-tested | NOT IMPLEMENTED (UI) |
| AT-043 | Missing-evidence detection exists in `TaxReadiness.kt` (Phase 3) surfaced on Home/Tax; not yet written as a persisted `TaxIssueEntity` row from a preflight step | `TaxReadinessTest` | Confirm on-screen count via UI | IMPLEMENTED-DEVICE-REQUIRED (as a live count, not a persisted issue row) |
| AT-044 | Duplicate-candidate counting exists in `TaxReadiness.kt`; not yet a persisted `TaxIssueEntity` | `TaxReadinessTest` | Confirm on-screen warning via UI | IMPLEMENTED-DEVICE-REQUIRED (same caveat as AT-043) |
| AT-045 | Draft `draftVersion`/`maxVersion+1` on regeneration (confirmed pre-existing, tested in Phase 5) | `AppDatabaseTest` (regeneration-increments-version case) | Regenerate via UI, confirm version bump and prior version intact | HOST-VERIFIED |
| AT-046 | `TaxMappingEntity.USER_OVERRIDE` + reason field (Phase 4) | `AppDatabaseTest` | View override + reason in UI | HOST-VERIFIED (persistence); IMPLEMENTED-DEVICE-REQUIRED (UI display) |

## Wealth reconciliation
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-050 | `calculateReconciliation(taxYearId)` (Phase 5, fixed from a hardcoded-zero-opening-wealth bug) | `TaxEngineTest`/`AppDatabaseTest` balanced fixture | Confirm zero on-screen for a balanced dataset | HOST-VERIFIED |
| AT-051 | Same method — a deliberately-missing asset/liability produces a nonzero expected-vs-recorded difference | `AppDatabaseTest` (year-scoping regression) | Confirm nonzero on-screen | HOST-VERIFIED |
| AT-052 | No UI drill-down into individual contributing records — reconciliation shows only the aggregate difference | — | Needs a drill-down screen built first (explicitly deferred by Phase 5) | NOT IMPLEMENTED |

## Vault
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-060 | SAF `OpenDocument` launcher, PDF preview | `DocumentPreviewDeviceTest` (compiled, not run) | Import PDF via real file picker | IMPLEMENTED-DEVICE-REQUIRED |
| AT-061 | Same launcher, image preview via `BitmapFactory` | `DocumentPreviewDeviceTest` (compiled, not run) | Import image via real file picker | IMPLEMENTED-DEVICE-REQUIRED |
| AT-062 | `linkDocument(documentId, "tax_item", ...)` | `DocumentLifecycleDeviceTest` (compiled, not run) | Link via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-063 | `DocumentLinkEntity` is many-to-many by design; Phase 6 added account as a second linkable entity type alongside tax items | `DocumentLifecycleDeviceTest` (compiled, not run) | Link one document to two different entities via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-064 | Phase 6: delete dialog shows dependency count before Cancel/"Unlink and delete" | `DocumentLifecycleDeviceTest` (compiled, not run) | Delete a linked document via UI, confirm warning | IMPLEMENTED-DEVICE-REQUIRED |
| AT-065 | Keystore AES-GCM encrypted file store, never a Room BLOB | `DocumentVault`-path tests, `BackupRestoreDeviceTest` (compiled, not run) | Inspect on-disk bytes are not plaintext | IMPLEMENTED-DEVICE-REQUIRED |
| AT-066 | SHA-256 duplicate-hash rejection on import (Phase 6) | `DocumentLifecycleDeviceTest` (compiled, not run) | Import the same file twice via real file picker | IMPLEMENTED-DEVICE-REQUIRED |

## Reports
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-070 | `ReportGenerator.netWorth` using canonical `FinancialPosition` (Phase 7) | `ReportsTest` | Generate via UI | HOST-VERIFIED (figures); IMPLEMENTED-DEVICE-REQUIRED (UI/PDF render) |
| AT-071 | `ReportGenerator.taxPreparationSummary` + `writePdf` | `ReportsTest` | Generate + open PDF via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-072 | In-app preview dialog (Phase 7) shows the same `FinancialReport` object later exported, so figures are guaranteed to match by construction, not just by convention | `ReportsTest` | Visual confirm preview text vs. UI totals | HOST-VERIFIED (construction guarantee); IMPLEMENTED-DEVICE-REQUIRED (visual) |
| AT-073 | `writePdf` pagination logic unchanged this run — no long-document device test exists | — | Generate a report with enough lines to force multiple pages, confirm no clipping | NOT IMPLEMENTED (no automated coverage; behavior itself pre-existed and wasn't touched) |
| AT-074 | `DataExportService` CSV export (events/accounts/tax items) | `DataExportTest` | Export CSV via UI | HOST-VERIFIED (content); IMPLEMENTED-DEVICE-REQUIRED (SAF export) |

## Security
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-080 | PIN verifier with backoff | `PinVerifierTest` | Enter wrong PIN via UI | HOST-VERIFIED (logic); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-081 | `BiometricPrompt` integration in `SecurityGate.kt` — cancel path unchanged this run | — | Cancel biometric prompt, confirm no unlock | IMPLEMENTED-DEVICE-REQUIRED |
| AT-082 | Lifecycle-based relock observer — unchanged this run | — | Background app past inactivity threshold, confirm relock | IMPLEMENTED-DEVICE-REQUIRED |
| AT-083 | No deep links are declared in the manifest beyond the launcher — nothing to bypass currently exists, so there is nothing for this test to catch a regression in yet | — | Confirm no intent-filter exists that could reach protected content directly | IMPLEMENTED-DEVICE-REQUIRED (trivially, by absence) |
| AT-084 | `OfficialRecordEntity` masked/encrypted identifier, revealed only explicitly | — | Confirm masking via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-085 | `LocalPrivacyMode`/`MaskedPkr` (Phase 1), applied across Home/Money/Wealth/Reports | — | Toggle privacy mode via UI | IMPLEMENTED-DEVICE-REQUIRED |
| AT-086 | Phase 8: whole-tree grep for `Log.`/`println`/`print(` found **zero** matches in `app/src/main` — an explicit, checked finding, not an assumption | — | N/A — static check already performed this run | HOST-VERIFIED |

## Backup
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-090 | `BackupPackageService.create`/`createStreaming`, AES-GCM + PBKDF2 | `PortableBackupTest`, `BackupPackageTest` | Create via UI | HOST-VERIFIED (crypto); IMPLEMENTED-DEVICE-REQUIRED (UI/file export) |
| AT-091 | `PortableBackupCrypto.decrypt` fails closed on wrong password | `PortableBackupTest` | Attempt restore with wrong password via UI | HOST-VERIFIED |
| AT-092 | AES-GCM authentication tag rejects tampered ciphertext | `PortableBackupTest` | Corrupt a backup file, attempt restore | HOST-VERIFIED |
| AT-093 | `LiveRestoreService` staged restore with rollback | `LiveRestoreServiceTest`, `BackupRestoreDeviceTest` (compiled, not run) | Full backup→clear→restore on a real device | HOST-VERIFIED (staging/rollback logic); IMPLEMENTED-DEVICE-REQUIRED (full equivalence) |
| AT-094 | Manifest now carries `recordCount`/`documentCount` (Phase 7 extended it with hashes/ruleset version) | `BackupPackageTest` (manifest round-trip) | Compare pre/post counts on a device | HOST-VERIFIED (manifest content); IMPLEMENTED-DEVICE-REQUIRED (real round trip) |
| AT-095 | Restore is validated through Room migrations before commit; financial totals are a function of restored rows, not separately re-derived, so a correct restore implies matching totals | — | Compare pre/post `FinancialPosition` on a device | IMPLEMENTED-DEVICE-REQUIRED |
| AT-096 | Manifest now includes per-document SHA-256 hashes (Phase 7) | `BackupPackageTest` | Compare document hashes pre/post restore on a device | HOST-VERIFIED (manifest content); IMPLEMENTED-DEVICE-REQUIRED (real comparison) |
| AT-097 | `LiveRestoreService` pre-restore snapshot + rollback on failure | `LiveRestoreServiceTest` | Interrupt a restore on a device, confirm original DB intact | HOST-VERIFIED (rollback logic); IMPLEMENTED-DEVICE-REQUIRED (real interruption) |

## Data ownership
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-100 | `DataExportService.json()`, extended in Phase 7 to include `TaxMappingEntity`/`WealthSnapshotEntity`/draft version | `DataExportTest` | Export via UI | HOST-VERIFIED (content); IMPLEMENTED-DEVICE-REQUIRED (SAF export) |
| AT-101 | `FinanceRepository.deleteAllData` — `clearAllTables()`, vault/cache file deletion, WorkManager cancellation, `shared_prefs` deletion | — (no JVM-level fixture test exists for this exact method; it's exercised indirectly by DB-layer tests, not as a populated-then-cleared end-to-end assertion) | Populate data, delete all, confirm empty on device | IMPLEMENTED-DEVICE-REQUIRED (no dedicated automated test — a real gap, noted for Phase 10) |
| AT-102 | `AppPreferences.isOnboardingComplete()` is stored in the same `shared_prefs` directory `deleteAllData` wipes; whether an already-constructed `AppPreferences`/`MainViewModel` instance actually re-reads it without a process restart was flagged as an open question in Phase 1 and never independently re-verified | `AppPreferencesTest` (compiled, not run — tests the preference store in isolation, not the delete-all interaction) | Delete all on a device, confirm onboarding reappears without a manual process kill | NOT IMPLEMENTED (verified) — genuinely open, flag explicitly for Phase 10 |

## Notifications
| AT | Impl. | Auto test | Device action needed | Status |
|---|---|---|---|---|
| AT-110 | `CalendarItemDialog`/`ReminderScheduler`; Phase 6 added document/official-record expiry-driven reminders using the same pattern | `ReminderSchedulerTest`, `ReminderDeviceTest` (compiled, not run) | Create reminder via UI | HOST-VERIFIED (scheduling logic); IMPLEMENTED-DEVICE-REQUIRED (real notification) |
| AT-111 | `ReminderWorker` fires and posts to the `passport_reminders` channel | `ReminderDeviceTest` (compiled, not run) | Confirm real notification fires on a device | IMPLEMENTED-DEVICE-REQUIRED |
| AT-112 | `RescheduleDialog`/`rescheduleCalendarItem` | `ReminderSchedulerTest` | Edit reminder via UI | HOST-VERIFIED (logic); IMPLEMENTED-DEVICE-REQUIRED (UI) |
| AT-113 | Calendar item status update to CANCELLED cancels the underlying unique WorkManager request | `ReminderSchedulerTest` | Delete/cancel reminder via UI | HOST-VERIFIED (logic); IMPLEMENTED-DEVICE-REQUIRED (UI) |

## Summary

- **HOST-VERIFIED** (calculation/domain logic proven by a passing JVM test, independent of any UI): AT-015, AT-027, AT-031, AT-034, AT-036, AT-040, AT-041, AT-045, AT-046, AT-050, AT-051, AT-070, AT-072, AT-074, AT-080, AT-086, AT-090, AT-091, AT-092, AT-093, AT-094, AT-096, AT-097, AT-100, AT-110, AT-112, AT-113.
- **IMPLEMENTED-DEVICE-REQUIRED**: the large majority — real code exists, but the acceptance test as written requires an actual rendered UI, biometric prompt, notification, or file picker to pass, none of which were exercised this run.
- **NOT IMPLEMENTED**: AT-003 (no guided setup step exists to skip), AT-016 (no Money activity filter UI), AT-042/AT-052 (no source/contributor drill-down screens), AT-073 (no automated long-document pagination coverage), AT-101 (no dedicated automated delete-all fixture test), AT-102 (delete-all → onboarding re-entry genuinely unverified, flagged since Phase 1).

No AT-### is marked PASS in this document. Phase 10 is required before any can be.
