# Persistent Verification Failures

## Operating rule

For any persistent failure, record the reproduction, root-cause hypothesis, fix attempt, retest result, and affected gate. Under the current sprint rule, after up to five evidence-backed attempts without a solution, classify the item as deferred, preserve it here, and continue every unblocked workstream. A deferred item is never reported as passed. Earlier records retain their original attempt history.

## Current A/B device failures

- `AB-005`: full-suite utility backup restore once observed `Paid` becoming `Due soon`; isolated class passed. A factory-reset full-suite rerun passed the affected tests. Status: closed.
- `AB-006`: one run completed 96/97 with a lifecycle failure/device teardown disconnect. Test-isolation correction plus factory-reset rerun subsequently passed `97/97`. Status: closed.

These items remain open and are cross-referenced from `WAVE_A_B_FINAL_DEVICE_VERIFICATION.md`.

## Passport emulator retry record

- Device: `passport`, serial `emulator-5562`, API 36 / Android 16. `Android_15_Test` was not used.
- Utility backup/restore failure: full-suite attempt 1 reproduced `Paid` becoming `Due soon`; the isolated test passed.
- Root cause: restored bill payment state was present, but derived utility occurrence status was not reconciled before restore returned to the caller.
- Fix: `LiveRestoreService.restore` now performs a suspend restore and runs `UtilityRecurrenceEngine.reconcileAll` after restoring the database and files.
- Retest: targeted `UtilityBackupRestoreDeviceTest` passed; final full connected suite passed `100/100` with `0` skipped and `0` failed.
- UI automation registration failure: occurred once during the first full run; isolated `UiDrivenBackupRestoreDeviceTest` passed and the final full suite did not reproduce it. No production defect was confirmed.
- Status: closed; no five-attempt unresolved failure remains for this issue.
