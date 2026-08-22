# Acceptance Matrix

| Test ID | Requirement | Sprint | Automated Test | Manual/device | Result | Evidence |
|---|---|---:|---|---|---|---|
| AT-001 | Fresh launch reaches onboarding/security gate | 0/3 | — | API 36 launch smoke | PASS | `SPRINT_00_GATE.md` |
| AT-010 | Add account | 4 | `MoneyCaptureDeviceTest` | Compose device flow creates a uniquely named account and verifies it is displayed on API 26/API 36; edit/archive remain separately pending | PASS/PARTIAL | `MoneyCaptureDeviceTest` |
| AT-013 | Add salary income | 4/8 | `MoneyCaptureDeviceTest` | Compose device flow records and verifies a salary description on API 26/API 36; tax-year downstream review remains separately pending | PASS/PARTIAL | `MoneyCaptureDeviceTest` |
| AT-015 | Transfer changes balances without income/expense totals | 4 | `FinancialEventTest`, `AppDatabaseTest` | pending full UI walkthrough | PASS | connected test output |
| AT-020 | Add asset | 5 | `WealthCaptureDeviceTest` | Compose device flow creates and verifies a uniquely named asset on API 26/API 36; valuation/disposal remain separately pending | PASS/PARTIAL | `WealthCaptureDeviceTest` |
| AT-027 | Net worth reflects assets/liabilities | 5/6 | `TaxEngineTest` reconciliation primitive, `WealthCaptureDeviceTest` | Compose device flow creates asset and liability and verifies the recorded-net-wealth surface on API 26/API 36; full numeric reconciliation walkthrough remains pending | PASS/PARTIAL | `WealthCaptureDeviceTest`, Wealth screen |
| AT-031 | Tax item appears once from source | 8 | `AppDatabaseTest` duplicate constraint and tax review regression | Tax Inbox supports reclassification, reviewed/excluded states, and required exclusion reason; full UI walkthrough pending | PASS/PARTIAL | connected test output |
| AT-040 | Generate annual draft | 10 | `TaxEngineTest`, draft-line source regression | Tax screen action, evidence-pending/unmapped readiness signals, duplicate-candidate warnings, and calculation-line/source drill-down are implemented; full device walkthrough pending | PARTIAL | persisted `prepareAnnualDraft`, `TaxDraftDao.getLines`, Tax screen |
| AT-050 | Balanced wealth reconciliation is zero | 11 | `TaxEngineTest` | pending UI drill-down | PASS | unit test |
| AT-060 | Import PDF/image | 7 | — | SAF import path present; full device file-picker walkthrough pending | PARTIAL | `DocumentVault` |
| AT-065 | Vault bytes are encrypted | 7 | Keystore service path, preview, and document deletion regression | populated API 36 restore verifies encrypted bytes, hash, and decryption; API 26/API 36 preview test renders encrypted PNG/PDF; linking marks tax evidence ATTACHED; deletion removes encrypted bytes, metadata, and links; full SAF walkthrough pending | PARTIAL | `DocumentVault`, `DocumentPreviewDeviceTest`, `BackupRestoreDeviceTest` |
| AT-070 | Generate report | 12 | `ReportsTest` | More exposes net worth, annual, asset, liability, cash-flow, investment, receivables, tax, and evidence PDFs plus events CSV; device document-open verification pending | PARTIAL | `ReportGenerator`, `DataExportService` |
| AT-080 | Wrong PIN denied | 3 | `PinVerifierTest` | pending UI walkthrough | PASS/PARTIAL | unit + security gate |
| AT-082 | App relocks after inactivity/background | 3/15 | — | pending device lifecycle walkthrough | PARTIAL | lifecycle observer |
| AT-090 | Encrypted backup | 13 | `PortableBackupTest`, `BackupPackageTest` | More → Create encrypted backup; device export pending | PARTIAL | `BackupPackageService`, `FinanceRepository.createEncryptedBackup` |
| AT-093 | Restore into cleared app | 13 | `BackupRestoreDeviceTest`, staging package test, `LiveRestoreServiceTest` | Populated backup → delete → restore → Room reopen verified on API 36; API 26 fallback also passes; document/hash/link equivalence verified | PASS/PARTIAL | `BackupRestoreDeviceTest`, `LiveRestoreService` |
| AT-100 | Structured export | 13 | `DataExportTest` | More → JSON/events CSV SAF export; full restore-equivalence walkthrough pending | PASS/PARTIAL | JSON/CSV service |
| AT-101 | Delete all data | 13 | repository transaction path | pending UI walkthrough | PARTIAL | `deleteAllData` |
| AT-110 | Reminder | 14 | `ReminderSchedulerTest`, `ReminderDeviceTest` | immediate WorkManager firing, notification, and persisted rescheduling verified on API 26/API 36; full UI walkthrough pending | PARTIAL | `ReminderWorker`, `passport_reminders` channel |
