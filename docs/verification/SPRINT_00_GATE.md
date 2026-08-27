# Sprint 00 Gate — Repository and Build Foundation

Date: 2026-08-14
Status: PASS

## Implemented

- Gradle Android application with package `pk.vexel.financepassport`.
- Kotlin/Compose/Material 3 foundation targeting SDK 36 with minimum SDK 26.
- Single-activity navigation shell for Home, Money, Wealth, Tax & Records and More.
- Typed `Money` value object using exact integer minor units.
- Transfer-pair invariant and unit regression tests.
- Baseline repository status, blockers and deferred-decision records.

## Gate commands

| Command | Result |
| --- | --- |
| `./gradlew --offline clean assembleDebug` | PASS |
| `./gradlew --offline test` | PASS |
| `./gradlew --offline lint` | PASS |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | PASS on `Android_16_Test` / API 36 |
| `adb shell monkey -p pk.vexel.financepassport -c android.intent.category.LAUNCHER 1` | PASS; MainActivity visible |
| Crash log sample | PASS; no `FATAL EXCEPTION` for app package |

## Defects and repairs

- Kotlin 2.3 rejected the legacy `kotlinOptions.jvmTarget` property; migrated to the `compilerOptions` DSL and reran the full gate.
- Emulator install initially reported “device is still booting”; waited for `sys.boot_completed=1` and reran install successfully.
