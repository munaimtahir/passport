# Wave F/G/H Discovery Map

| Capability | Existing implementation | Current source | Problem | Decision | Target architecture |
|---|---|---|---|---|---|
| Financial position | `FinancialPosition`, repository Flow | `FinancialPosition.kt`, `FinanceRepository.kt` | Legacy investment aggregation and no complete drill-down UI | MODERNIZE | One derived position from eligible accounts, receivables, simple investments, assets, and liabilities |
| Assets | `AssetEntity`, wealth DAO, basic repository methods | `Entities.kt`, `Daos.kt`, `FinanceRepository.kt` | Basic valuation/disposal exists; include/ownership semantics need complete UI and tests | MODERNIZE | Manually valued assets with immutable historical disposal and inclusion/ownership controls |
| Wealth snapshots | `WealthSnapshotEntity` and tax reconciliation use | `Entities.kt`, `FinanceRepository.kt` | Snapshot path is tax-oriented and uniqueness semantics need verification | RECONNECT | Immutable manual/monthly position snapshots reused by Home and tax reconciliation |
| Calendar | `CalendarItemEntity`, `CalendarDao` | `Entities.kt`, `Daos.kt` | Generic items do not yet cover all source projections | MODERNIZE | Source-linked derived calendar items with domain actions and idempotent reconciliation |
| Reminders | WorkManager scheduler and workers | `ReminderScheduler.kt` | Scheduling exists; source resolution, snooze/dismiss, deep-link/privacy need coverage | RECONNECT | Local source-aware reminders with stable unique work names |
| Documents | `DocumentEntity`, `DocumentVault` | `Entities.kt`, `DocumentVault.kt` | Encryption/hash pipeline exists; import UI and richer metadata/lineage need review | KEEP / MODERNIZE | One encrypted canonical file with metadata and hash-based duplicate warning |
| Evidence links | `DocumentLinkEntity`, repository link/delete methods | `Entities.kt`, `FinanceRepository.kt` | Link target and safe-unlink behavior need tests/UI | MODERNIZE | Many-to-many links without financial mutation or byte duplication |
| Existing official records | `OfficialRecordEntity` | `Entities.kt`, repository | Useful expiry source but not universal evidence target | RECONNECT | Expiry-aware records linked to canonical documents and Calendar |
| Backup/restore | Encrypted DB snapshot plus document files | `FinanceRepository.kt`, security package | New F/G/H semantic equivalence requires explicit fixture verification | RECONNECT | Encrypted backup preserving records, links, files, and rebuildable reminders |
| Tax boundary | Existing tax candidate/draft subsystem | `taxrules`, tax DAOs | Must remain derived and inactive in F/G/H | KEEP | Preserve source links without adding tax friction |
| UI shell | Compose Home/Money/Bills/History | `PassportApp.kt` | No complete Wealth/Calendar/Vault navigation or source workflows | EXTEND | Add focused surfaces within current shell without redesigning global navigation |
