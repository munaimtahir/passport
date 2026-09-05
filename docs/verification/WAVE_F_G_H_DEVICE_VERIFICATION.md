# Wave F/G/H Device Verification

## Target

- Dedicated emulator: `passport`
- Serial: `emulator-5562`
- Android 16 / API 36
- 1080x2400, density 420
- No other emulator was modified.

## Running application evidence

The rebuilt APK was installed on the target. A clean launch completed onboarding, and the running application exposed and rendered:

- `Financial Position` / `Net Worth`
- `Financial Calendar`
- `Evidence Vault`

The app process remained alive during navigation. Force-stop/relaunch completed without an app, Room, or SQLite crash in the filtered logcat review.

## Automated device evidence

Final `connectedDebugAndroidTest` on the same target: **100 executed, 100 passed, 0 failed, 0 skipped**.

## Defects found and fixed

- Position screen initially rendered `PKR PKR` because the formatter already included the currency prefix. The display formatter was corrected and the host/connected gates were rerun.
- The pre-F/G/H navigation test asserted that Position/Calendar/Vault must be absent. It was updated to assert the new intentional surfaces; targeted navigation passed 2/2 and the final full suite passed 100/100.

## Manual scope status

The new shell surfaces are verified, including navigation regression coverage. Full F/G/H acceptance scenarios involving creation/editing of every asset, calendar source, reminder action, camera import, and multi-record evidence thread remain incomplete and are not claimed as PASS.
# Latest rerun evidence

The current connected regression was executed against the dedicated `passport` emulator only:

- Serial: `emulator-5562`
- Model: `sdk_gphone64_x86_64`
- Android/API: `16 / 36`
- Display: `1080x2400`, density `420`
- Result: 100/100 tests passed, 0 failed, 0 skipped.
- Post-dismissal-fix full regression: 101/101 tests passed, 0 failed, 0 skipped; targeted reminder suite: 5/5 passed.
- Focused logcat review found no app fatal exception, SQLite/Room failure, or ANR.

This is regression evidence; the individual F/G/H acceptance gaps remain recorded in their gate documents and the invariant matrix.
