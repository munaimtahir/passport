# Testing and ADB Verification Plan

## Test pyramid

### Unit
- money arithmetic
- account balance derivation
- investment position calculations
- tax-year selection
- tax mapping
- draft generation
- reconciliation
- validation

### Database
- DAOs
- transactions
- cascades/link behavior
- migrations

### Instrumentation
- encrypted storage
- biometric/PIN integration boundaries
- Storage Access Framework
- PDF generation
- backup/restore

### Compose UI
- navigation
- forms
- validation
- list/detail
- tax review
- privacy masking

### End-to-end device
- onboarding to annual tax draft
- backup/restore
- document import
- process death/relaunch
- notification

## Required synthetic test dataset

Create `DemoUserScenario` containing:
- 2 bank accounts
- cash
- salary
- bank profit
- dividend
- stock buy/sell
- car purchase
- property asset
- loan
- receivable
- monthly expenses
- tax withheld
- attached evidence metadata
- one deliberately missing document
- one duplicate candidate
- one ambiguous tax classification

Expected totals must be stored as test fixtures.

## ADB commands

Examples to be used by agent where environment supports them:

```bash
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop pk.vexel.financepassport
adb shell monkey -p pk.vexel.financepassport -c android.intent.category.LAUNCHER 1
adb logcat -c
adb logcat
```

Use actual output paths if project modules differ.

## Device verification checklist

- Fresh install
- First launch
- PIN creation
- Biometric enrollment flow
- Add account
- Add income
- Add expense
- Transfer
- Add investment
- Add asset
- Add tax item from source transaction
- Add manual tax item
- Import document
- Link evidence
- Generate annual draft
- Open source drill-down
- Reconcile wealth
- Generate PDF
- Backup
- Clear app data
- Restore
- Confirm counts/totals/documents
- Background and relock
- Rotation
- dark/light theme if supported
- font scale
- notification

## Performance scenarios

Seed:
- 10 accounts
- 10,000 financial events
- 2,000 tax items
- 1,000 documents metadata records
- 10 tax years

Targets:
- Home should become usable promptly without loading entire history into memory.
- Lists must be paginated/lazy.
- Tax draft calculation should be deterministic and not block the main thread.
- Document files should stream rather than load unnecessarily into memory.

## Failure injection

Test:
- corrupt ruleset
- corrupt backup
- missing document file
- storage full simulation where practical
- interrupted restore
- malformed import
- date edge cases
- currency mismatch
- duplicate transfer
- deleted source linked to tax item

## Evidence record

For each quality gate save:
- command
- date
- result
- failing test if any
- fix commit
- rerun result

Recommended:
`docs/verification/SPRINT_XX_GATE.md`
