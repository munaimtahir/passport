# Current UI and navigation map

## Entry sequence

```text
Launcher MainActivity
  -> OnboardingGate (first run only)
     -> Welcome
     -> Privacy explanation
     -> optional PIN create/skip
     -> Start Empty
  -> SecurityGate (PIN/biometric only when PIN exists)
  -> PassportApp utility shell
```

There is no Navigation Compose graph despite the dependency. Navigation is a saved integer in
`PassportApp`; dialogs are local Compose state. No deep links are declared in the manifest.

## Reachable map

```text
Utility Bill Tracker
├─ Home
│  ├─ Add Your First Bill -> switches to Bills
│  └─ attention occurrence -> Monthly Occurrence Details
├─ Bills
│  ├─ search + Active/Archived/All + category filters
│  ├─ FAB -> Add Utility Bill
│  └─ profile card -> Utility Profile Details
│     ├─ billing occurrence -> Monthly Occurrence Details
│     ├─ Edit
│     ├─ Archive / Reactivate
│     ├─ Add Month
│     └─ Delete profile
├─ History
│  ├─ search
│  ├─ status/category/year/payment-mode filters
│  ├─ Reset (filters only)
│  └─ occurrence -> Monthly Occurrence Details
└─ Settings icon -> Settings & Local Data
   ├─ Create Encrypted Backup
   ├─ Restore Encrypted Backup
   └─ Delete All Application Data
```

Monthly Occurrence Details supports actual issue/due dates, bill amount, save, skip/unskip, payment
create/edit/delete, and bill/payment-proof attachments with preview/delete. These are modal flows,
not routes.

## Orphaned screens and dialogs

The following composables exist but no reachable shell action calls them: `MoneyScreen`,
`TaxScreen`, `WealthScreen`, `VaultScreen`, `CalendarItemDialog`, `RescheduleDialog`, account add/edit,
income/expense, transfer, recurring item, manual tax item, filing deadline, official record, general
document import/link, and all associated wealth dialogs. `EmptyModuleScreen` is unreachable because
only indices 0..2 can be selected.

`MoreDialog` constructs report/export activity launchers and preview state, but renders no report,
JSON, CSV, or PDF controls. Those paths are effectively dead UI code.

## Meaningful current pathways

- Add utility: Home CTA -> Bills -> FAB -> save -> generated occurrence -> profile/history.
- Pay utility: Home/Bills/History occurrence -> Mark Paid -> payment fields -> save -> Paid status.
- Preserve proof: occurrence/payment -> attach -> system picker -> encrypted local preview.
- Archive/reactivate: Bills -> profile -> Archive; archived view -> Reactivate with new start month.
- Backup: Settings -> create -> password -> system save picker.
- Restore: Settings -> restore -> system open picker -> password -> staged replacement.

There are no current pathways to accounts, ordinary income/expense, transfers, wealth, tax, general
vault, calendar, or reports.

Evidence: `MainActivity.kt`, `Onboarding.kt`, `SecurityGate.kt`, and `PassportApp.kt` lines defining
`destinations`, the top-level `when`, `MoreDialog`, and utility dialogs.

