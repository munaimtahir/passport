# Sprint 08 Gate — Continuous Tax Capture Core

Date: 2026-08-14
Status: IN PROGRESS

Income capture writes a canonical `FinancialEventEntity` and a single source-linked `TaxItemEntity` in one transaction. The unique `(sourceType, sourceId)` constraint and `INSERT OR IGNORE` behavior prevent recomputation duplicates. The Tax Inbox displays review and evidence states.

Persisted annual draft generation is now available from Tax & Records and stores draft version/ruleset/source calculation lines. Tax Inbox review now supports validated reclassification, reviewed/excluded states with mandatory exclusion reasons, and evidence-state progression after document linking. Full annual workflow and tax-year boundary acceptance tests remain pending.
