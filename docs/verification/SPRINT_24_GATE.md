# Sprint 24 quality gate

Date: 2026-08-28

| Gate | Result | Evidence |
| --- | --- | --- |
| Java runtime | PASS | Temurin OpenJDK 17.0.18 |
| Gradle runtime | PASS | Gradle 8.13 on JVM 17.0.18 |
| `assembleDebug` | PASS | `app-debug.apk`, SHA-256 `85e33379204b5e2c308f7c47664418ec682d7aec67d4600803ee2235f36724a7` |
| JVM tests | PASS | 78 unique tests per variant; 156 executions, 0 failures/errors/skips |
| Lint | PASS | 0 errors; report generated |
| `assembleDebugAndroidTest` | PASS | instrumentation APK compiled |
| `assembleRelease` | PASS | `app-release.apk`, SHA-256 `919dce3f9fcaecbff45ab83c089bea2025aff0f6ac58740559cd879063d26077`; `app-release.aab`, SHA-256 `d3d8c28cbffa7b14acc7838c47ee73e61ce03fc6779a8fac7e1d101ecb5c2054` |
| v13->v14 migration execution | PASS | `DatabaseMigrationTest` executed and passed on API 36 |
| Connected suite | PASS | 68/68 tests executed and passed on API 36 emulator |
| ADB/UI scenario | PASS | `ManualE2EWalkthroughDeviceTest`, `NavigationSmokeTest` passed |
| Live backup/restore | PASS | `BackupRestoreDeviceTest`, `UtilityBackupRestoreDeviceTest`, `UiDrivenBackupRestoreDeviceTest` passed |

Overall: **PASS**. All host and mandatory API 36 device qualification gates passed.
