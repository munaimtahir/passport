# Utility Tracker — Host Gate

Run 2026-08-28, against `main` @ `c1b3cc1` (post utility-tracker reset, commit `603e198` and
subsequent work through `c1b3cc1`), before any version bump.

## Environment note

A stray `GradleDaemon` (pid 8093) from a prior session was pinned at ~99% CPU and held open file
handles on `app/build/intermediates/lint-cache` on the fuse-mounted project path, which made the
first `./gradlew clean` fail with `Unable to delete directory ... Failed to delete some children`.
Stopped cleanly via `./gradlew --stop` (not `kill -9`), then `rm -rf app/build` succeeded and the
gate below ran clean. No source or git state was touched by this recovery.

## Commands run

| Command | Result |
| --- | --- |
| `./gradlew clean test --no-daemon --max-workers=2` | BUILD SUCCESSFUL in 4m 1s — all `testDebugUnitTest`/`testReleaseUnitTest` JVM tests passed, 0 failures |
| `./gradlew lint --no-daemon --max-workers=2` | BUILD SUCCESSFUL in 3m 13s — `app/build/reports/lint-results-debug.html`, no blocking errors |
| `./gradlew assembleDebug assembleRelease --no-daemon --max-workers=2` | BUILD SUCCESSFUL in 4m 27s — both variants packaged; `validateSigningRelease` ran using the real `vexel-release` key (`keystore.properties` present), not a debug-signing fallback |

## Notes

- Only two Kotlin compiler warnings, both pre-existing and non-blocking: deprecated
  `Modifier.menuAnchor()` overload in `PassportApp.kt:1013` and `:1027`.
- Release APK produced at `app/build/outputs/apk/release/app-release.apk`; debug APK at
  `app/build/outputs/apk/debug/app-debug.apk`.
- Room schema exports present through version 13 in `app/schemas/`; no destructive-migration
  markers found in `AppDatabase`/migration source during this pass (spot-checked, full migration
  test evidence is Phase 2/database-test territory, not re-run in this host-only gate).

## Verdict

Host gate: **PASS**. Proceeding to device gate (`docs/verification/UTILITY_TRACKER_DEVICE_GATE.md`).
