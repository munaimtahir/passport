# Hardening verification — 2026-08-17

Branch: `main`  
Baseline: `6296ca1`  
Device: `Android_26_Test(AVD)`, API 26 / Android 8.0.0  
Physical device: not attached (`adb devices -l` exposed only `emulator-5554`)

## Commands and results

| Command | Result |
|---|---|
| `./gradlew clean test lint assembleDebug` | PASS after serial rerun; initial concurrent run was rejected by Kotlin cache locking |
| `./gradlew connectedDebugAndroidTest` | PASS, 28/28 on API 26 |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | PASS |
| `adb shell monkey -p pk.vexel.financepassport -c android.intent.category.LAUNCHER 1` | PASS |
| `adb shell dumpsys activity activities` | `MainActivity` active under `pk.vexel.financepassport` |
| crash scan | No app `FATAL EXCEPTION`; emulator log clear itself reported a permission quirk |

## Corrections verified

- One canonical Room migration registry now includes `7→8` for normal open and restore validation.
- Whole-PKR input rejects decimals/malformed grouping/overflow and formats signed and legacy fractional minor units exactly.
- Income, expense, recurring, and transfer forms use explicit account selectors; transfers prevent identical endpoints.
- Recurring processing advances reminders without creating a confirmed financial event.
- Repository write failures surface a generic actionable UI error without exposing exception text.
- Confirmed tracked Git-internal root copies were removed; `.kotlin/` is ignored as generated build output.

## Limitations

No API 36 or physical-device result is claimed. The final verdict remains NO-GO because broader canonical requirements and the required device matrix are still incomplete; see `FINAL_VERIFICATION.md` and `REQUIREMENTS_TRACEABILITY.md`.
