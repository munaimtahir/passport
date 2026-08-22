# Sprint 0–16 Traceability Matrix

Audit date: 2026-08-22. Statuses use the audit vocabulary from the request. Prior verification documents are historical evidence only; the current audit had no connected Android device.

| Sprint | Requirement/build item or gate | Canonical source | Status | Implementation/evidence | Gap, severity, next action |
| --- | --- | --- | --- | --- | --- |
| 0 | Android project | docs/10, lines 17-30 | VERIFIED PASS | settings.gradle.kts, app/build.gradle.kts | Single app module; feature-module separation absent. P2. |
| 0 | Application ID | docs/10 | VERIFIED PASS | app/build.gradle.kts; pk.vexel.financepassport | None for internal QA. |
| 0 | Compose | docs/10 | VERIFIED PASS | build features and PassportApp.kt | None observed. |
| 0 | min/target/compile SDK | docs/10 | VERIFIED PASS | min 26, target/compile 36 | Runtime matrix untested. P1 evidence gap. |
| 0 | Dependency version catalog | docs/10 | NOT IMPLEMENTED | Versions inline in app/build.gradle.kts | P2 architecture/tooling item. |
| 0 | CI-ready Gradle commands | docs/10 | IMPLEMENTED — UNVERIFIED | Wrapper and documented commands; no CI workflow found | P2; run in CI. |
| 0 | Package/module structure | docs/10 | PARTIAL | Explicit package boundaries; no core/feature Gradle modules | P2. |
| 0 | Lint baseline | docs/10 | VERIFIED PASS | lint passes without baseline | Non-fatal warnings remain. |
| 0 | README | docs/10 | VERIFIED PASS | README.md and docs/00_README.md | Existing claims need audit qualification. |
| 0 | ADRs | docs/10 | VERIFIED PASS | docs/adr/ADR-001 files | SQLCipher explicitly deferred; no prototype. |
| 0 | Clean debug build gate | docs/10 | VERIFIED PASS | ./gradlew clean assembleDebug; 3m20s on merged HEAD 29f4bed9 | Unstrippable graphics warning. |
| 0 | Unit-test gate | docs/10 | VERIFIED PASS | ./gradlew test; 50 debug JVM tests pass | Android tests not in this result. |
| 0 | Lint gate | docs/10 | VERIFIED PASS | ./gradlew lint | Warnings only. |
| 0 | Debug APK installs | docs/10 | NOT TESTED | APK built; no device | P1 evidence gap. |
| 0 | App launches/no placeholder crash | docs/10 | NOT TESTED | MainActivity exists; no runtime target | P1 evidence gap. |
| 1 | Theme | docs/10, lines 41-58 | IMPLEMENTED — UNVERIFIED | ui/theme/Theme.kt | No visual/runtime review. |
| 1 | Typography | docs/10 | IMPLEMENTED — UNVERIFIED | Material 3 theme references | No font-scale evidence. |
| 1 | Components | docs/10 | PARTIAL | Inline Compose dialogs/cards/buttons | No reusable design-system layer. P2. |
| 1 | Bottom navigation | docs/10 | IMPLEMENTED — UNVERIFIED | PassportApp.kt, five destinations | No current Compose run. |
| 1 | Adaptive scaffold | docs/10 | PARTIAL | Scaffold exists; no width-aware/tablet layout | P1 if tablet is internal scope. |
| 1 | Privacy-value masking | docs/10 | NOT IMPLEMENTED | No privacy eye/masking state; dashboard shows amounts | P1 privacy defect. |
| 1 | Empty states | docs/10 | IMPLEMENTED — UNVERIFIED | Empty text in module screens | Loading/error/success coverage incomplete. |
| 1 | Navigation UI tests | docs/10 | NOT TESTED | NavigationSmokeTest present | No device. |
| 1 | Font scaling smoke | docs/10 | NOT TESTED | No current evidence | P1 gate gap. |
| 1 | Portrait/landscape smoke | docs/10 | NOT TESTED | No current evidence | P1 gate gap. |
| 1 | Critical actions accessible | docs/10 | PARTIAL | Some icon content descriptions | No TalkBack audit. P1/P2. |
| 1 | Screenshot review | docs/10 | NOT TESTED | No screenshots created | P2. |
| 2 | Room schema | docs/10, lines 61-79 | VERIFIED PASS | AppDatabase.kt, exported schemas 2–8 | No schema 1 export; migration starts at v2. P2. |
| 2 | Entities/DAOs | docs/10 | VERIFIED PASS for implemented tables | Entities.kt and Daos.kt compile | Some canonical fields/relationships absent. |
| 2 | Repositories | docs/10 | VERIFIED PASS for core paths | FinanceRepository.kt | Many service methods are not UI-exposed. |
| 2 | Migration framework | docs/10 | VERIFIED PASS | DatabaseProvider.ALL_MIGRATIONS, 1→2 through 7→8 | Restore validator uses same list. |
| 2 | Money value type | docs/10 | VERIFIED PASS | Money.kt, PkrMoneyInput.kt, unit tests | Reports do not group output. P2. |
| 2 | Date handling | docs/10 | PARTIAL | Epoch-day fields and LocalDate | No user date entry; draft uses current year. P1. |
| 2 | Seed/test fixtures | docs/10 | PARTIAL | Test-local data; no DemoUserScenario found | P2. |
| 2 | DAO/instrumentation tests | docs/10 | NOT TESTED | AppDatabaseTest exists | No device. |
| 2 | Transaction integrity | docs/10 | VERIFIED PASS for tested paths | Transfer/link DB tests | Failure injection incomplete. P2. |
| 2 | Initial-schema migration | docs/10 | PARTIAL | v2→v7 and v7→v8 tests | No v1 fixture. P2. |
| 2 | No destructive migration | docs/10 | VERIFIED PASS by inspection | No destructive fallback found | Runtime old-schema evidence unavailable. |
| 2 | Money arithmetic tests | docs/10 | VERIFIED PASS | MoneyTest, FinancialEventTest, PkrMoneyInputTest | UI not run. |
| 3 | PIN | docs/10, lines 81-100 | IMPLEMENTED — UNVERIFIED | PinVerifier.kt, PinStore.kt, SecurityGate.kt | Lost-PIN recovery absent; UI not run. P1. |
| 3 | Biometric | docs/10 | IMPLEMENTED — UNVERIFIED | SecurityGate.kt BiometricPrompt | Cancellation/device behavior untested. |
| 3 | Lock state | docs/10 | IMPLEMENTED — UNVERIFIED | SecurityGate relocks on ON_STOP | No current process/lifecycle evidence. |
| 3 | Keystore master key | docs/10 | IMPLEMENTED — UNVERIFIED | KeystoreCryptoService.kt | Database remains app-private plaintext SQLite. |
| 3 | Sensitive-field encryption | docs/10 | IMPLEMENTED — UNVERIFIED | Official identifiers use Keystore AES-GCM | No UI reveal flow found. |
| 3 | Sensitive-screen protection | docs/10 | IMPLEMENTED — UNVERIFIED | MainActivity FLAG_SECURE | No device check. |
| 3 | Secure logging policy | docs/10 | PARTIAL | No verbose logging found; no audit artifact | P2. |
| 3 | Fresh-install lock flow | docs/10 | NOT TESTED | NavigationSmokeTest exists | No device. |
| 3 | Background/resume lock | docs/10 | NOT TESTED | Lifecycle code exists | No runtime test. |
| 3 | Deep-link bypass | docs/10 | NOT APPLICABLE currently | Manifest has launcher only; no deep-link route | Reassess when links are added. |
| 3 | Wrong PIN | docs/10 | VERIFIED PASS for verifier only | PinVerifierTest and backoff code | UI behavior untested. |
| 3 | Biometric cancellation | docs/10 | NOT TESTED | No biometric device | P1 evidence gap. |
| 3 | Encrypted round trip | docs/10 | VERIFIED PASS for portable crypto; UNVERIFIED Keystore | PortableBackupTest | Android Keystore not current-runtime tested. |
| 3 | Release log review | docs/10 | NOT TESTED | No release log capture | P2. |
| 4 | Accounts | docs/10, lines 103-121 | PARTIAL | AccountEntity, repository, Money UI | Type/institution/identifier fields not exposed; type hardcoded OTHER. P1. |
| 4 | Income | docs/10 | IMPLEMENTED — UNVERIFIED | AddEventDialog and repository | No device. |
| 4 | Expense | docs/10 | IMPLEMENTED — UNVERIFIED | AddEventDialog and repository | No device. |
| 4 | Transfer | docs/10 | IMPLEMENTED — UNVERIFIED | AccountPicker and transactional paired rows | Current device unavailable. |
| 4 | Categories | docs/10 | PARTIAL | Optional string category | No managed category CRUD/tax mapping. P2. |
| 4 | Activity list | docs/10 | PARTIAL | Recent list limited to 200 | No search/filter/sort/date range. P1/P2. |
| 4 | Account balances | docs/10 | IMPLEMENTED — UNVERIFIED | Opening balance plus Room movement aggregate | No current device. |
| 4 | Editing/archive | docs/10 | IMPLEMENTED — UNVERIFIED | updateAccount/archiveAccount and UI | No current device. |
| 4 | Balance invariants | docs/10 | VERIFIED PASS for unit/DB tests | FinancialEventTest, AppDatabaseTest | Broader date/currency cases absent. |
| 4 | Transfer excluded from income/expense | docs/10 | VERIFIED PASS | FinancialEventTest and SQL totals | Current UI not run. |
| 4 | CRUD tests | docs/10 | PARTIAL | Account update/archive and event tests | No complete UI CRUD. |
| 4 | Process recreation | docs/10 | NOT TESTED | No device | P1. |
| 4 | Navigation | docs/10 | NOT TESTED | Compose test present | No device. |
| 4 | Device smoke | docs/10 | NOT TESTED | No device | P1. |
| 5 | Assets | docs/10, lines 125-140 | PARTIAL | Entity/repository/dialog | Acquisition/funding/date/type details absent; maintenance UI partial. P1. |
| 5 | Liabilities | docs/10 | PARTIAL | Entity/repository/dialog | Lender/due date absent; payment service not exposed. P1. |
| 5 | Receivables | docs/10 | PARTIAL | Entity/repository/dialog | Due date/payment UI absent. P1. |
| 5 | Investments | docs/10 | PARTIAL | Entity/repository/dialog/domain math | Account hardcoded manual; no holdings summary. P1. |
| 5 | Holdings calculations | docs/10 | IMPLEMENTED — UNVERIFIED | InvestmentDomain.kt and tests | No UI source presentation/unrealized path. |
| 5 | Manual valuations | docs/10 | IMPLEMENTED — UNVERIFIED | updateAssetValuation service | No UI caller. P1 dormant implementation. |
| 5 | Buy/sell tests | docs/10 | VERIFIED PASS for domain | InvestmentDomainTest | No device. |
| 5 | Realized/unrealized tests | docs/10 | PARTIAL | Realized/income tests; no unrealized value calculation | P1. |
| 5 | Asset/liability net-worth tests | docs/10 | PARTIAL | Reconciliation primitive and wealth test | Home is not true net worth. P1. |
| 5 | Archive/disposal | docs/10 | IMPLEMENTED — UNVERIFIED | disposeAsset and DB test | No UI evidence. |
| 5 | Device verification | docs/10 | NOT TESTED | No device | P1. |
| 6 | Net worth | docs/10, lines 144-159 | BROKEN | Home displays Net recorded movement from income/expense only | Excludes assets/liabilities/cash/investments/receivables. P1. |
| 6 | Summaries | docs/10 | PARTIAL | Movement, count, reminders | Required module summaries absent. P1. |
| 6 | Recent activity | docs/10 | IMPLEMENTED — UNVERIFIED | Room recent query and Home list | No current runtime/filter. |
| 6 | Quick add | docs/10 | PARTIAL | FAB adds account only | P2 UX. |
| 6 | Upcoming | docs/10 | IMPLEMENTED — UNVERIFIED | Calendar items on Home | No current runtime. |
| 6 | Tax readiness | docs/10 | PARTIAL | Text card plus Tax counts | Not a complete dimensions model. P1. |
| 6 | Totals reconcile | docs/10 | BROKEN for dashboard net-worth requirement | Dashboard does not consume wealth entities | P1. |
| 6 | Empty-state correctness | docs/10 | IMPLEMENTED — UNVERIFIED | Empty text/cards found | No runtime. |
| 6 | Privacy masking | docs/10 | NOT IMPLEMENTED | No masking state/control | P1. |
| 6 | Seeded performance | docs/10 | NOT TESTED | 10,000-event test source exists | No current Home measurement. P2. |
| 7 | Import | docs/10, lines 162-180 | IMPLEMENTED — UNVERIFIED | DocumentVault.import and SAF launcher | Picker/runtime not tested. |
| 7 | Encrypted storage | docs/10 | IMPLEMENTED — UNVERIFIED | Keystore AES-GCM under filesDir/vault | Import reads full bytes; P2 memory risk. |
| 7 | Metadata | docs/10 | PARTIAL | Title/category/name/mime/size/hash/expiry | Tags/search/version metadata absent. |
| 7 | Tags | docs/10 | NOT IMPLEMENTED | No tag entity or UI | P2. |
| 7 | Links | docs/10 | PARTIAL | Many-to-many links; UI only tax items | Generic record linking absent. P1. |
| 7 | Official records | docs/10 | PARTIAL | Entity/repository and Tax dialog | Full metadata/expiry/link UI incomplete. |
| 7 | Preview | docs/10 | IMPLEMENTED — UNVERIFIED | PDF/image preview and test source | No current device/restart evidence. |
| 7 | Expiry reminder foundation | docs/10 | PARTIAL | Expiry field exists | No automatic expiry work. P1/P2. |
| 7 | Encrypted unreadable gate | docs/10 | IMPLEMENTED — UNVERIFIED | Keystore encryption and prior tests | No current device. |
| 7 | Import/open/delete | docs/10 | PARTIAL | Code exists; delete removes links | Dependency warning missing. P1. |
| 7 | Duplicate hash detection | docs/10 | PARTIAL | Unique SHA-256 index | No friendly duplicate warning at import. |
| 7 | Invalid/corrupt handling | docs/10 | IMPLEMENTED — UNVERIFIED | MIME/size/decrypt/preview checks | No runtime failure injection. |
| 7 | Dependency warning | docs/10 | BROKEN | deleteDocument deletes links; UI only confirms deletion | Must warn and offer unlink/delete choice. P1. |
| 7 | Restart persistence | docs/10 | NOT TESTED | App-private path and DB exist | No device. |
| 8 | Tax years | docs/10, lines 184-200 | IMPLEMENTED — UNVERIFIED | TaxYearEntity and insert-on-capture | UI cannot backdate. |
| 8 | Tax items | docs/10 | IMPLEMENTED — UNVERIFIED | Entity/DAO/Tax screen | No device. |
| 8 | Tax relevance | docs/10 | PARTIAL | Enum/domain and income source path | All states/reasoned override not exposed. P1. |
| 8 | Event taxonomy | docs/10 | PARTIAL | Kotlin enum and JSON doc | Runtime does not parse/validate JSON. P1. |
| 8 | Source linkage | docs/10 | PARTIAL | sourceType/sourceId and draft IDs | No source navigation. P1. |
| 8 | Tax inbox | docs/10 | IMPLEMENTED — UNVERIFIED | TaxScreen observes items | No device. |
| 8 | Review states | docs/10 | IMPLEMENTED — UNVERIFIED | Validated repository methods | Original mapping/reason lineage not separately preserved. |
| 8 | Source event appears once | docs/10 | IMPLEMENTED — UNVERIFIED | Unique source index and DB test | No current UI run. |
| 8 | Independent tax item | docs/10 | IMPLEMENTED — UNVERIFIED | Manual item dialog/repository/test | Date/type validation weak in UI. |
| 8 | Source edit remapping | docs/10 | NOT IMPLEMENTED | No remapping/lineage service | P1. |
| 8 | Exclusion preserves source | docs/10 | VERIFIED PASS for DB path | Review test and separate source event | UI not current-runtime verified. |
| 8 | Tax-year assignment | docs/10 | PARTIAL | Repository supports date; UI always defaults today | Historical acceptance fails. P1. |
| 9 | Ruleset JSON schema | docs/10, lines 204-220 | NOT IMPLEMENTED | No schema found | P1. |
| 9 | Parser | docs/10 | NOT IMPLEMENTED | No parser found | P1. |
| 9 | Validator | docs/10 | NOT IMPLEMENTED | No validator found | P1. |
| 9 | Classifier | docs/10 | IMPLEMENTED — UNVERIFIED | StructuralTaxClassifier | Small hardcoded structural map only. |
| 9 | Mapping engine | docs/10 | PARTIAL | AnnualDraftGenerator | Does not cover taxonomy/split treatments. P1. |
| 9 | Version history | docs/10 | PARTIAL | Version strings in tax year/draft | No stored ruleset package/history. P1. |
| 9 | Invalid ruleset rejected | docs/10 | NOT IMPLEMENTED | No parser/validator path | P1. |
| 9 | Determinism | docs/10 | VERIFIED PASS | TaxEngineTest | Narrow fixture only. |
| 9 | Historic version preserved | docs/10 | PARTIAL | Draft stores version; tax item does not store mapping version | P1. |
| 9 | Ambiguous issue | docs/10 | VERIFIED PASS for structural classifier | TaxEngineTest and ambiguous rule | Narrow coverage. |
| 9 | User override honored | docs/10 | PARTIAL | reviewTaxItem changes type/state | No override reason/lineage field. P1. |
| 10 | Annual sections | docs/10, lines 224-239 | PARTIAL | Draft lines have section/category strings | No complete annual section model. P1. |
| 10 | Issue center | docs/10 | PARTIAL | TaxIssueEntity and Tax screen | No full resolution/source navigation. P1. |
| 10 | Evidence completeness | docs/10 | PARTIAL | Evidence states and pending count | No full dimensions/preconditions. |
| 10 | Duplicate candidates | docs/10 | PARTIAL | UI warning grouping | No canonical close-date detector. |
| 10 | Draft generation | docs/10 | IMPLEMENTED — UNVERIFIED | prepareAnnualDraft, persisted lines/issues | Current year only; no selector. P1. |
| 10 | Source drill-down | docs/10 | PARTIAL | sourceIdsJson and text derivation | No clickable source path. P1. |
| 10 | Totals equal source | docs/10 | IMPLEMENTED — UNVERIFIED | Grouping/source IDs and unit test | No full-year/UI evidence. |
| 10 | Every line traceable | docs/10 | PARTIAL | IDs persisted | No source navigation/display. P1. |
| 10 | Critical issues visible | docs/10 | IMPLEMENTED — UNVERIFIED | Issue rows/counts | No current runtime. |
| 10 | Regeneration versioned | docs/10 | VERIFIED PASS for persistence path | maxVersion + 1 and DB test | No UI comparison/lineage. |
| 10 | No source mutation | docs/10 | IMPLEMENTED — UNVERIFIED | New draft rows only | No device end-to-end. |
| 11 | Opening snapshot | docs/10, lines 243-257 | PARTIAL | Entity fields; calculation hardcodes opening 0 | No user snapshot workflow. P1. |
| 11 | Closing snapshot | docs/10 | PARTIAL | Assets minus liabilities | Cash/investment/receivable scope incomplete. P1. |
| 11 | Inflow/outflow grouping | docs/10 | PARTIAL | Income/expense only | Transfers/outflows incomplete. P1. |
| 11 | Difference | docs/10 | IMPLEMENTED — UNVERIFIED | reconcileWealth and persisted result | No current UI. |
| 11 | Drill-down | docs/10 | NOT IMPLEMENTED | Calculation string only | P1. |
| 11 | Zero scenario | docs/10 | VERIFIED PASS for domain fixture | TaxEngineTest | Not full repository/UI. |
| 11 | Missing asset difference | docs/10 | NOT TESTED | No matching test found | P1. |
| 11 | Adjustment reason | docs/10 | NOT IMPLEMENTED | No adjustment UI/repository | P1. |
| 11 | Reproducible calculation | docs/10 | IMPLEMENTED — UNVERIFIED | Pure reconcileWealth formula | Production input scope incomplete. |
| 12 | Report domain | docs/10, lines 260-276 | IMPLEMENTED — UNVERIFIED | Reports.kt | Raw /100 and incomplete source scope. |
| 12 | In-app previews | docs/10 | NOT IMPLEMENTED | More launches SAF export directly | P1. |
| 12 | PDF | docs/10 | IMPLEMENTED — UNVERIFIED | ReportGenerator.writePdf | No device open test. |
| 12 | CSV | docs/10 | PARTIAL | Events CSV UI; account/tax methods not wired | P2/P1. |
| 12 | Tax preparation report | docs/10 | IMPLEMENTED — UNVERIFIED | taxPreparationSummary/export button | Not a complete pack. P1. |
| 12 | Net-worth report | docs/10 | PARTIAL | Function exists; excludes cash/investments/receivables | P1 incorrect scope. |
| 12 | Numbers match UI/source | docs/10 | BROKEN for canonical net worth | Home and report use different incomplete concepts | P1. |
| 12 | Date range | docs/10 | IMPLEMENTED — UNVERIFIED | ExportSnapshot.forDateRange/current-year toggle | Assets/liabilities not filtered. P1/P2. |
| 12 | Long pagination | docs/10 | IMPLEMENTED — UNVERIFIED | lines.chunked(26) | No runtime clipping evidence. |
| 12 | PDF opens on device | docs/10 | NOT TESTED | No device | P1. |
| 12 | No clipping | docs/10 | NOT TESTED | No screenshot/device | P1/P2. |
| 12 | Privacy disclaimer | docs/10 | NOT IMPLEMENTED | No report disclaimer found | P1/P2. |
| 13 | Encrypted backup | docs/10, lines 280-296 | IMPLEMENTED — UNVERIFIED | PortableBackup, BackupPackage, streaming file path | No current device. |
| 13 | Integrity manifest | docs/10 | PARTIAL | Counts/schema/timestamp; no per-file hashes | P1. |
| 13 | Transactional restore | docs/10 | IMPLEMENTED — UNVERIFIED | Staging/validation/rollback in LiveRestoreService | UI and current device untested. |
| 13 | Full JSON export | docs/10 | PARTIAL | DataExportService.json | Omits drafts/reconciliation/calendar and fields. P1. |
| 13 | CSV export | docs/10 | PARTIAL | Events CSV UI | Other CSV methods not exposed. |
| 13 | Delete all | docs/10 | IMPLEMENTED — UNVERIFIED | clearAllTables, work/vault/cache/prefs cleanup | No clean-onboarding runtime proof. |
| 13 | Backup/clear/restore equivalence | docs/10 | NOT TESTED | Device test exists; no current device | P0/P1 evidence gate. |
| 13 | Wrong password | docs/10 | VERIFIED PASS for crypto | PortableBackupTest | UI not run. |
| 13 | Tampered backup | docs/10 | VERIFIED PASS for crypto | PortableBackupTest | UI not run. |
| 13 | Failed restore preserves state | docs/10 | IMPLEMENTED — UNVERIFIED | Previous DB backup and restore path | Vault partial-file edges not fully proven. P1. |
| 13 | Document hashes | docs/10 | IMPLEMENTED — UNVERIFIED | Device test code compares hash/bytes | Current device unavailable. |
| 13 | Delete-all leaves no records | docs/10 | IMPLEMENTED — UNVERIFIED | clearAllTables and file cleanup | No runtime proof. |
| 14 | Due dates | docs/10, lines 300-315 | PARTIAL | Calendar dueAt entity | Wealth/document/receivable due fields do not schedule automatically. |
| 14 | Reminders | docs/10 | IMPLEMENTED — UNVERIFIED | ReminderScheduler/Worker | No device. |
| 14 | Document expiry | docs/10 | NOT IMPLEMENTED | Expiry field only | P1/P2. |
| 14 | Receivable reminders | docs/10 | NOT IMPLEMENTED | Due date UI absent | P1/P2. |
| 14 | Monthly review | docs/10 | NOT IMPLEMENTED | Generic calendar item only | P2. |
| 14 | Tax review | docs/10 | NOT IMPLEMENTED | No automatic schedule | P2. |
| 14 | Notification permission | docs/10 | IMPLEMENTED — UNVERIFIED | Manifest/request and worker permission check | No API 33+ runtime. |
| 14 | Reminder fires | docs/10 | NOT TESTED | ReminderDeviceTest present | No device. |
| 14 | Edit/cancel schedule | docs/10 | IMPLEMENTED — UNVERIFIED | Repository and unique work | No current runtime. |
| 14 | Duplicate notifications | docs/10 | IMPLEMENTED — UNVERIFIED | Unique work names | No device. |
| 15 | Empty/error/loading | docs/10, lines 318-334 | PARTIAL | Empty and generic write-error UI | Loading/success/recovery incomplete. |
| 15 | Accessibility | docs/10 | NOT TESTED | No TalkBack test | P1. |
| 15 | Rotation | docs/10 | NOT TESTED | rememberSaveable in some screens | No device. |
| 15 | Process recreation | docs/10 | NOT TESTED | No current test | P1. |
| 15 | Large datasets | docs/10 | PARTIAL | 10,000-event test source | No current startup/2,000 tax/1,000 document run. P2. |
| 15 | Keyboard behavior | docs/10 | NOT TESTED | No device | P2. |
| 15 | Privacy masking everywhere | docs/10 | NOT IMPLEMENTED | No global privacy control | P1. |
| 15 | Interaction burden | docs/10 | NOT TESTED | No current UI | P1/P2. |
| 15 | TalkBack | docs/10 | NOT TESTED | No device | P1. |
| 15 | Font scale | docs/10 | NOT TESTED | No device | P1. |
| 15 | Low-memory/process death | docs/10 | NOT TESTED | No device | P1. |
| 15 | Layout overflow | docs/10 | NOT TESTED | No screenshots/device | P1/P2. |
| 16 | Release config | docs/10, lines 338-358 | PARTIAL | Minification enabled; release explicitly debug-signed | Production signing deferred; internal artifact needs runtime proof. |
| 16 | App icon | docs/10 | PARTIAL | ic_passport.xml exists | Final branding deferred public release. |
| 16 | Versioning | docs/10 | VERIFIED PASS | versionCode 1/versionName 0.1.0 | No release lifecycle process. |
| 16 | Privacy policy inputs | docs/10 | PARTIAL | Draft requirements in docs; no public URL | DEFERRED — PUBLIC RELEASE. |
| 16 | Play declarations | docs/10 | DEFERRED — PUBLIC RELEASE | docs/13_RELEASE_AND_PLAY_STORE.md | Not an internal QA blocker per locked decision. |
| 16 | Backup rules | docs/10 | PARTIAL | allowBackup=false; no dataExtractionRules | Lint warning; P1/P2 hardening. |
| 16 | ProGuard/R8 review | docs/10 | IMPLEMENTED — UNVERIFIED | proguard-rules.pro and release config | Release build not run in current audit. |
| 16 | Crash-free smoke | docs/10 | NOT TESTED | No device | P1. |
| 16 | clean bundleRelease | docs/10 | NOT TESTED | Only clean debug run | P1 gate gap. |
| 16 | Lint | docs/10 | VERIFIED PASS | Current lint passed | Warnings remain. |
| 16 | Unit tests | docs/10 | VERIFIED PASS | Current test task passed | JVM only. |
| 16 | Connected tests | docs/10 | NOT TESTED | No connected devices | P1. |
| 16 | Release APK/AAB verification | docs/10 | NOT TESTED | No release build/device | Runtime evidence absent. |
| 16 | No debug flags/secrets | docs/10 | IMPLEMENTED — UNVERIFIED | No secrets/logging found; debug signing remains | Inspect generated release artifact. |
| 16 | Target API compliance | docs/10 | VERIFIED PASS by configuration | target/compile 36 | Device matrix absent. |
| 16 | Physical-device final verification | docs/10 | NOT TESTED | No physical device | P1 gate gap. |

## Interpretation

The matrix separates code presence from current proof. VERIFIED PASS means the relevant current JVM/static evidence passed; it does not imply Android UI or device success. The internal-release verdict is NO-GO because the canonical definition requires runtime/device evidence and material core workflows are incomplete or broken.
