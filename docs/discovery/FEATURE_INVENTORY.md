# Feature inventory

Statuses describe current reachability, not historical intent.

## Reachable production UI

| Surface | Entry | Data and action path | Status |
|---|---|---|---|
| Onboarding | fresh install | `OnboardingGate` -> `AppPreferences`; optional `PinStore` | WORKING |
| Unlock | app start when PIN exists | `SecurityGate` -> `PinStore`/`BiometricPrompt` | IMPLEMENTED - NEEDS VERIFICATION |
| Home | bottom Home | utility profiles/occurrences; attention and paid totals | WORKING |
| Bills | bottom Bills | profile search/filter/add/detail/edit/archive/reactivate/delete | WORKING |
| History | bottom History | occurrence/payment search and filters; detail modal | WORKING |
| Occurrence detail | Home/Bills/History | amount/dates, payment, skip/unskip, attachments | IMPLEMENTED - NEEDS VERIFICATION |
| Settings & Local Data | top-right Settings | encrypted backup, restore, delete-all | IMPLEMENTED - NEEDS VERIFICATION |
| Privacy masking | top-right eye | persisted `AppPreferences` flag | WORKING |

## Retained but unreachable UI

`PassportApp.kt` still defines `MoneyScreen`, `TaxScreen`, `WealthScreen`, `VaultScreen`, calendar
dialogs, account/event/transfer/recurring dialogs, official-record/tax-review dialogs, report preview,
and document link/import dialogs. The `destinations` list contains only Home, Bills, History and the
top-level `when` invokes only those three. Therefore these surfaces are ORPHANED, not current user
features. There are no deep links or alternate routes.

## Backend/domain capability inventory

| Area | Actual capability | Current classification |
|---|---|---|
| Profiles/accounts | user profile entity; active/archive accounts; opening balance + event movement | BACKEND ONLY/ORPHANED |
| Income | persisted event, optional income source, automatic tax item/mapping | BACKEND ONLY/ORPHANED |
| Expense | persisted event/category/account; no automatic tax item | BACKEND ONLY/ORPHANED |
| Transfer | two signed TRANSFER events and `TransferLinkEntity` in one transaction | BACKEND ONLY/ORPHANED |
| Recurring finance | rule + calendar reminder; worker advances due date only; explicit confirmation creates event | BACKEND ONLY/ORPHANED |
| Utility tracker | profiles, monthly occurrences, payment, secure attachments, reminders, search/history | WORKING |
| Subscription | legacy recurring category value only; no dedicated model | PARTIAL |
| Assets | add, valuation update, dispose | BACKEND ONLY/ORPHANED |
| Liabilities | create, outstanding reduction, due reminder; no repayment event/history entity | PARTIAL/ORPHANED |
| Receivables | create, outstanding reduction, due reminder; no receipt history entity | PARTIAL/ORPHANED |
| Investments | BUY/SELL/DIVIDEND/PROFIT/FEE event capture; cost-basis calculation | PARTIAL/ORPHANED |
| Goals/budgets | target/progress and category budget calculation | BACKEND ONLY/ORPHANED |
| Net worth | active account opening + movements + active assets + investment cost basis + open receivables - active liabilities | BACKEND ONLY/ORPHANED |
| Calendar | Room items, WorkManager notifications, document/liability/receivable/tax/recurring helpers | BACKEND ONLY/ORPHANED |
| General document vault | SAF PDF/image import, AES-GCM file, hash, metadata, links, preview, deletion | BACKEND ONLY/ORPHANED |
| Utility attachments | PDF/JPEG/PNG/WebP, 20 MB limit, SHA-256, Keystore encryption, preview/delete | WORKING |
| Official records | encrypted identifier, masking, expiry reminder | BACKEND ONLY/ORPHANED |
| Tax | years/items/mappings/issues/drafts/lines/snapshots/reconciliation; two JSON rulesets | BACKEND ONLY/ORPHANED |
| Reports | net worth, income/expense, cash flow, assets, liabilities, investments, receivables, annual, tax, evidence PDFs | BACKEND ONLY |
| Export | JSON snapshot and CSV event/account/tax helpers | BACKEND ONLY |
| Backup/restore | password-encrypted DB + vault files, manifest, hashes, staging/rollback | IMPLEMENTED - NEEDS VERIFICATION |

## Important behavioral details

- Account balance query treats EXPENSE as negative and all other stored amounts by sign. Transfer
  source is stored negative and destination positive; income/expense totals explicitly filter types.
- Utility payment is independent from `AccountEntity` and `FinancialEventEntity`; it cannot select a
  payment account and does not affect account balances, tax, net worth, or finance reports.
- Income creates a tax item classified initially as employment income regardless of selected income
  source. Expense does not automatically create a tax item.
- Investments have no price feed, security master, lot matching, realized/unrealized gain engine, or
  valuation history. The report states cost basis.
- Asset valuation overwrites current value; liabilities and receivables overwrite outstanding
  balances. No dedicated valuation/payment history rows exist for these domains.
- General documents support many-to-many links via `DocumentLinkEntity`; utility attachments use a
  separate untyped `linkedId` table and separate vault directory.

## Evidence

- UI: `ui/PassportApp.kt`, `ui/Onboarding.kt`, `ui/SecurityGate.kt`, `ui/MainViewModel.kt`
- Data/actions: `core/database/Entities.kt`, `Daos.kt`, `FinanceRepository.kt`
- Utilities: `UtilityRecurrenceEngine.kt`, `UtilityAttachmentVault.kt`
- Tax: `core/taxrules/*`, bundled `resources/taxrules/*.json`
- Reports/export/backup: `Reports.kt`, `DataExport.kt`, `BackupPackage.kt`, `LiveRestoreService.kt`

