# Reset Utility / Utility Tracker discovery

## Authoritative answer

> **What Reset Utility currently does in production code:** There is no production operation named
> Reset Utility. It changes no record and has no utility-cycle, payment, amount, date, financial-event,
> or attachment side effect. The visible `Reset` button in History only clears search/filter UI state.

The term “reset” in repository documentation refers to the product being reset to a utility-only
scope. It is not a bill action.

## Actual utility flow

```text
AddBillDialog
 -> MainViewModel.addUtilityProfile
 -> FinanceRepository.addUtilityProfile
 -> UtilityBillDao.upsert (new profile)
 -> FinanceRepository.reconcileAllUtilityBills
 -> UtilityRecurrenceEngine.reconcileAll/reconcileProfile
 -> MonthlyBillOccurrenceDao.getForMonth
 -> insert a new occurrence only when that profile/month is absent
 -> derive status from issue date, due date, payment existence, skip state, and today
 -> schedule due-soon/due-day WorkManager reminders when enabled
```

`MainViewModel` invokes reconciliation after add, update, archive, reactivate, occurrence changes,
and payment changes. A daily `RecurringProcessingWorker` also reconciles all utility bills.

## Cycle behavior

- Record reset: none.
- Fields changed by reset: none.
- New cycle creation: reconciliation inserts a distinct occurrence for each missing month from
  `recurrenceStartMonth` through the current month.
- Previous cycle preservation: yes; existing rows are never replaced by reconciliation.
- History retention: yes unless the user explicitly deletes an occurrence or profile.
- Amount clearing: no. A new occurrence begins with `amountMinor = null`; old amounts are untouched.
- Paid-state clearing: no. Paid is derived from a payment row attached to that occurrence.
- Due-date advancement: no old due date changes. A new month gets newly calculated expected dates.
- New versus old record: new UUID row; old record is not mutated except status can be re-derived as
  time passes or payment/skip state changes.
- Multiple cycles: yes, enforced unique by profile/year/month.
- Duplicate prevention: database unique index plus pre-insert `getForMonth`. A race raises an ABORT
  conflict rather than replacing history.

## Payments and financial ledger effects

`PaymentRecordEntity` contains occurrence, amount, date, mode, optional bank name/reference, and
notes. It has no account ID. `addPayment` inserts it, updates the occurrence status to `Paid`, and
cancels utility reminders. It does **not** call `FinanceRepository.addEvent`, insert a
`FinancialEventEntity`, select/debit an account, create a utility expense, create a tax item, or
affect the canonical financial position.

Consequences:

- Utility payment does not change legacy account balances or ordinary income/expense totals.
- Utility history and legacy financial reports are separate systems.
- Reconciliation/new-cycle generation cannot duplicate a financial expense because it creates no
  financial event at all.
- Historical financial reporting cannot change from utility cycle generation, payment, or any
  nonexistent reset action.

## Evidence/documents

Utility evidence is stored separately as `BillAttachmentEntity` plus an encrypted file under
`filesDir/utility_vault`. Cycle generation does not touch it. Payment deletion does not inherently
delete attachment rows/files. Profile deletion cascades occurrences/payments at the database level,
but utility attachments have no foreign keys; the UI warning says attachments are deleted while the
repository's `deleteUtilityProfile` only deletes the profile. This is a possible orphaned-metadata/
encrypted-file defect, not fixed during discovery.

## Supported utility dimensions

| Capability | Finding |
|---|---|
| Create/edit profile | yes |
| Electricity/gas/telephone/other | Add UI yes |
| Water/internet/mobile | inconsistent: History recognizes Water/Internet; Add UI does not offer them; Mobile not found |
| Custom category | model field exists; Add UI's Other supports a custom name |
| Household/clinic/business | location labels Home/Clinic/Office/Other; no financial-domain model link |
| Recurring monthly cycles | yes, fixed monthly only |
| Issue/due dates | anchors with month-end clamp and cross-month due logic |
| Amount/period/status | per occurrence |
| Payment date/mode | yes |
| Payment account/transaction | no |
| Reminders | optional profile preference; due-soon and due-day work |
| Prior history | yes |
| Archive/reactivate/delete | yes |
| Duplicate prevention | profile warning can be overridden; occurrence uniqueness is strict |
| Trends | counts, paid total, and searchable history; no chart/trend analytics |
| Subscriptions | no dedicated utility capability |

## Runtime result

**Before Reset:** A fresh API 36 install created `DiscoveryUtility`; reconciliation created August
2026 with amount unset and status Overdue.

**Action:** No Reset Utility action was reachable. History's Reset control was identified as filter
reset only and was not treated as a data mutation.

**After Reset:** Not applicable; no reset occurred.

**Financial Ledger Effect:** None by design; utility data has no ledger/account linkage.

**History Preservation:** The created occurrence appeared in History and remained after force-stop/
relaunch.

**Persistence After Relaunch:** PASS for profile and occurrence.

**Code vs Runtime Agreement:** AGREE: runtime exposed monthly occurrence management and no Utility
Reset command.

Evidence: `UtilityRecurrenceEngine.kt`; utility sections of `Entities.kt`, `Daos.kt`,
`FinanceRepository.kt`, `MainViewModel.kt`, and `PassportApp.kt`; `RecurringProcessingWorker`;
`UtilityRecurrenceEngineTest.kt`; `UtilityPaymentStatusDeviceTest.kt`; screenshots in `evidence/`.

