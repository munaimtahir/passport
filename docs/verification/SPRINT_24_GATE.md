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
| `assembleRelease` | PASS | `app-release.apk`, SHA-256 `38b80cac0b7032dc88e254d559a12b9c76895d88766572fc31a0b8730099104e` |
| v13->v14 migration execution | BLOCKED | test compiled; no boot-complete device |
| Connected suite | BLOCKED | API 36 AVD could not complete software-emulated boot without `/dev/kvm` |
| ADB/UI scenario | BLOCKED | Android rejected install while device remained in first boot |
| Live backup/restore | BLOCKED | updated test compiled but could not execute |

Overall: **BLOCKED**. Host quality gate is green; mandatory device gates remain open.
