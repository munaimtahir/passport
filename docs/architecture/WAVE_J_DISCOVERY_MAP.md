# Wave J discovery map

| Area | Current implementation | Disposition |
|---|---|---|
| Reports | `core/reports/Reports.kt`, Compose preview/PDF path | MODERNIZE |
| Structured/CSV export | `core/export/DataExport.kt` | MODERNIZE/EXTEND |
| Portable backup | AES-GCM + PBKDF2 package with ZIP payload | KEEP/HARDEN |
| Restore | staged package plus database replacement | HARDEN |
| Evidence vault | Keystore-backed encrypted app-private files | KEEP |
| Delete All | Room, preferences, WorkManager, vault cleanup | HARDEN |
| Room/migrations | exported schemas through 17 and explicit migrations | KEEP |
| Wave I tax workspace | present from prior work | DEFER; untouched by Wave J |
