# Discovery Verification Evidence — 2026-09-06

Raw evidence log supporting `docs/CURRENT_REPOSITORY_DISCOVERY_2026-09-06.md`. This is a
discovery-only session: no production source was modified. All commands below were executed
against HEAD `5c89a827e57aa9f68e7c398b2a146e4541fb2309` on a clean `git status` working tree.

## Git identity

```
$ git rev-parse HEAD
5c89a827e57aa9f68e7c398b2a146e4541fb2309
$ git branch --show-current
main
$ git status --short --branch
## main...origin/main
$ git rev-list --left-right --count origin/main...HEAD
0	0
$ git log --oneline | wc -l
110
$ git tag --sort=-creatordate
safety-phase0-start-20260821
$ git describe --tags --always
safety-phase0-start-20260821-100-g5c89a82
```

Working tree clean, no stashes, branch tracks `origin/main` and is neither ahead nor behind.
No tag points at HEAD; the only tag in the repo is 100 commits behind HEAD.

## Build environment

```
$ java -version
openjdk version "17.0.18" 2026-01-20 (Temurin)
$ ./gradlew --version
Gradle 8.13, Kotlin 2.0.21, Launcher/Daemon JVM 17.0.18
$ adb version
Android Debug Bridge version 1.0.41 (36.0.2)
$ adb devices -l
(empty — no device/emulator attached)
$ ls $ANDROID_HOME/platforms
android-34 android-35 android-36
$ emulator -list-avds
Sprint24_API_36
$ ls /dev/kvm
No such file or directory
$ egrep -c '(vmx|svm)' /proc/cpuinfo
0
$ $ANDROID_HOME/emulator/emulator -accel-check
KVM requires a CPU that supports vmx or svm
$ df -h .
145G total, 26G available
$ free -h
15Gi total, ~8.4Gi available
```

No hardware virtualization is available in this session's host (no `/dev/kvm`, no `vmx`/`svm` CPU
flags). This matches the limitation already recorded in `docs/BLOCKERS.md`. Instrumentation/device
tests could not be executed this session; this is an **environment** limitation, not a source
defect — see `docs/verification/SPRINT_24_DEVICE_RESULTS.md` /
`docs/verification/GRAPHICAL_FINAL_DEVICE_ACCEPTANCE.md` for the most recent device evidence that
does exist (both dated 2026-08-29, both **12 commits behind current HEAD**, both against Room
schema v14, not the current v15).

## Clean build

```
$ ./gradlew clean
BUILD SUCCESSFUL in 1m 20s
```

## assembleDebug

```
$ ./gradlew assembleDebug
BUILD SUCCESSFUL in 5m 59s (41 actionable tasks)
```
Two Kotlin warnings only (unnecessary safe calls, `PassportApp.kt:742,746`). Debug APK produced:
`app/build/outputs/apk/debug/app-debug.apk`, 21,350,469 bytes,
SHA-256 `8a0174b569c74759e408f76c75cd6da6def13d36ed7f960a7c5535bfa0283923`.

## compileDebugAndroidTestKotlin

```
$ ./gradlew compileDebugAndroidTestKotlin
BUILD SUCCESSFUL in 44s
```
Only deprecation warnings (`createAndroidComposeRule` v1 API). The androidTest source set compiles
cleanly against current HEAD.

## test (JVM unit tests) — FAILS

```
$ ./gradlew test
...
e: .../core/export/DataExportTest.kt:12:138 Null cannot be a value of a non-null type 'String'.
e: .../core/export/DataExportTest.kt:12:156 Null cannot be a value of a non-null type 'String'.
e: .../core/export/DataExportTest.kt:12:165 No value passed for parameter 'updatedAtEpochMillis'.
e: .../core/model/BudgetMathTest.kt:18:138 Null cannot be a value of a non-null type 'String'.
e: .../core/model/BudgetMathTest.kt:18:160 Argument type mismatch: actual type is 'Long?', but 'String' was expected.
e: .../core/model/BudgetMathTest.kt:18:172 No value passed for parameter 'updatedAtEpochMillis'.
e: .../core/reports/ReportsTest.kt:22:103 Null cannot be a value of a non-null type 'String'.
e: .../core/reports/ReportsTest.kt:22:120 Null cannot be a value of a non-null type 'String'.
e: .../core/reports/ReportsTest.kt:22:129 No value passed for parameter 'updatedAtEpochMillis'.
e: .../core/reports/ReportsTest.kt:55:98 Null cannot be a value of a non-null type 'String'.
e: .../core/reports/ReportsTest.kt:55:115 Null cannot be a value of a non-null type 'String'.
e: .../core/reports/ReportsTest.kt:55:124 No value passed for parameter 'updatedAtEpochMillis'.
> Task :app:compileDebugUnitTestKotlin FAILED
BUILD FAILED in 1m 37s
```

**Root cause (verified by `git show --stat 5c89a82`):** HEAD's own commit ("Complete Wave A and B")
added a `contextId: String? = null` parameter to `FinancialEventEntity` (7th constructor
position, `app/src/main/java/pk/vexel/financepassport/core/database/Entities.kt`). The same
commit updated `core/model/FinancialEventTest.kt` (a 6-line diff) for the new positional shape but
did **not** update the three other JVM test files that construct `FinancialEventEntity`
positionally (`DataExportTest.kt`, `BudgetMathTest.kt`, `ReportsTest.kt`). Their old argument lists
now shift into the wrong parameters (a nullable `notes`/`category` string lands in the non-null
`description` slot, and the trailing `updatedAtEpochMillis` parameter is left unfilled), so the
whole `debugUnitTest`/`releaseUnitTest` source set fails to compile. **Zero of the 78 discovered
JVM unit test methods can currently execute** — this is a whole-module compile failure, not a
per-test failure.

This directly contradicts:
- `docs/verification/WAVE_A_B_FINAL_AUDIT.md` line 10: "All static analysis and Kotlin incremental
  compilations are clean."
- `docs/verification/WAVE_A_B_EVIDENCE.md`: "Tests: PASS".
- `docs/BUILD_STATUS.md` (stale, dated 2026-08-17): "Unit tests: `./gradlew test` PASS".

## lint — FAILS

```
$ ./gradlew lint
Lint found 2 errors, 24 warnings, 3 hints.
/app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt:1605: Error: Suspicious indentation...
/app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt:1877: Error: Suspicious indentation...
> Task :app:lintDebug FAILED
BUILD FAILED in 5m 49s
```
Both errors are `SuspiciousIndentation` in the Wave A/B "Unassigned Reconciliation"/context-filter
code added to `MoneyScreen` in `PassportApp.kt` (same commit as above). Low functional risk
(formatting only, not a logic defect) but the `lint` task fails outright, which is one of the
project's four standard gate commands (`docs/10_...md`, `README.md`).

Warning/hint breakdown (24 warnings, 3 hints, non-blocking):
```
7 UseKtx (SharedPreferences.edit() KTX suggestion)
6 GradleDependency / 4 NewerVersionAvailable / 1 AndroidGradlePluginVersion (version-bump suggestions)
2 SuspiciousIndentation (the 2 errors above)
2 ObsoleteSdkInt
2 MonochromeLauncherIcon
1 UnusedResources
```

## bundleRelease

```
$ ./gradlew bundleRelease
BUILD SUCCESSFUL in 10m 11s (58 actionable tasks)
```
`keystore.properties` is absent in this environment (gitignored, never committed — expected per
`keystore.properties.example`), so the build fell back to **debug signing** per the conditional
logic in `app/build.gradle.kts`. Produced:
`app/build/outputs/bundle/release/app-release.aab`, 5,693,705 bytes,
SHA-256 `5cf69df79d85c3e280d929026dfc1f1cccdd5abea837d89ab7e015d1ed070080`.

This hash does **not** match any hash recorded in `docs/RELEASE_LEDGER.md` (which records
production-signed artifacts from a session where the real release keystore was present) — expected,
since this is a debug-signed rebuild in an environment without the release key, not a reproduction
of a previously uploaded bundle. R8/minify and resource shrinking ran successfully; no signing
secret was read or printed.

## Repository scans

```
$ grep -rn "fallbackToDestructiveMigration" app/src        → no matches
$ grep -rniE "password=\"|secret=\"|apikey|api_key" app/src/main --include="*.kt"   → no matches
$ grep -rn "@Ignore|@Disabled" app/src/test app/src/androidTest   → no matches
$ grep -rnE "android.util.Log|println\(|Log\.\w\(" app/src/main --include="*.kt" | wc -l   → 0
$ grep -n "INTERNET\|ACCESS_NETWORK" AndroidManifest.xml   → no matches
$ grep -niE "firebase|analytics|admob|crashlytics|facebook" app/build.gradle.kts   → no matches
```
No destructive migrations, no hardcoded secrets, no disabled/ignored tests, no production logging,
no network permission, no analytics/ads SDKs.

## Migration chain

```
$ grep -n "Migration(" DatabaseProvider.kt
MIGRATION_1_2 ... MIGRATION_14_15   (14 consecutive migrations, versions 1→15, no gaps)
```
`app/schemas/pk.vexel.financepassport.core.database.AppDatabase/` contains exported schema JSON for
versions 2–15 (version 1's schema file is absent from the exported-schema directory, but the
migration path is still continuous starting at `MIGRATION_1_2`).

## Navigation reachability (evidence for the Feature Matrix)

```
$ grep -n "val destinations" -A5 PassportApp.kt
Destination("Home", ...), Destination("Money", ...), Destination("Bills", ...), Destination("History", ...)
```
Exactly 4 bottom-nav destinations exist; the `when(selected)` block's `else -> EmptyModuleScreen(...)`
branch is dead code (selected can only be 0–3). No `WealthScreen`, `TaxScreen`, `VaultScreen`, or
`ReportsScreen` composable exists anywhere in `ui/PassportApp.kt`.

```
$ grep -rln "AssetEntity|LiabilityEntity|ReceivableEntity|InvestmentEventEntity|WealthSnapshotEntity" app/src/main/java/.../ui
→ no matches
$ grep -rln "OfficialRecordEntity|GoalEntity|BudgetEntity" app/src/main/java/.../ui
→ no matches
$ grep -n "DocumentVault" PassportApp.kt
→ only the import line; no instantiation/call site
$ grep -n "taxItems|getMappingHistory" PassportApp.kt
→ no matches (MainViewModel exposes both; UI never reads them)
$ grep -n "requestedReport|previewReport|pendingReportExport|reportSnapshot()" PassportApp.kt
→ declared in MoreDialog (lines 275-280); never referenced by any rendered Button/Composable
```
Wealth (assets/liabilities/receivables/investments/goals), the general Document Vault, the Tax
Inbox/annual draft, and the Reports/export feature are all backed by working repository/domain code
and (where JVM-tested) unit tests, but have **zero reachable UI path** from `MainActivity` today.

## Net worth composition

```
$ grep -n "financialPosition" FinanceRepository.kt
```
`financialPosition` genuinely queries `wealthDao().observeAssets()/observeLiabilities()`,
`investmentDao().observeAll()`, `receivableDao().observeAll()` alongside accounts/events — the
calculation itself is canonical and correct. Because no UI path can write to those tables, the
figure shown on Home is mathematically real but always equal to the sum of account balances.
