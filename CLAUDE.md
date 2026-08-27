# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

This repository currently contains **no Android application code** — only an AI-development documentation pack (`/docs`) and an empty scaffold (`app/src/{main,test,androidTest}` with `.gitkeep` placeholders, no Gradle files). There is nothing to build, lint, or test yet.

The `/docs` directory is the full specification for **Vexel Finance Passport**, an Android app that has not been implemented. Before writing any code, read `docs/00_README.md` and `docs/14_MASTER_AI_AGENT_BUILD_PROMPT.md` in full — they define the product, the non-negotiable rules, and the exact process to follow. `docs/INDEX.md` lists every doc; read the ones relevant to the module you're touching (e.g. `04_DATA_MODEL.md` before touching entities, `07_SECURITY_PRIVACY_THREAT_MODEL.md` before touching auth/crypto, `05_CONTINUOUS_TAX_CAPTURE_ENGINE.md` before touching the tax engine).

`docs/product_manifest.json` and `docs/tax_event_taxonomy.json` are machine-readable summaries of the product manifest and tax event taxonomy, respectively.

## Build/test commands

None exist yet. Once the Android project is created (Sprint 0, per `docs/10_DEVELOPMENT_SPRINTS_AND_QUALITY_GATES.md`), establish and record standard Gradle commands (`./gradlew clean assembleDebug`, unit test task, lint, `connectedAndroidTest`) and keep this file updated with the actual commands and any single-test invocation syntax.

## Development process (mandatory)

This repo follows a strict sprint-and-gate process defined in `docs/10_DEVELOPMENT_SPRINTS_AND_QUALITY_GATES.md`, driven by the agent instructions in `docs/14_MASTER_AI_AGENT_BUILD_PROMPT.md`. Key points that apply to any coding session here:

- Work proceeds sprint-by-sprint in the order listed in `docs/10_...md` (Sprint 0: repo/build foundation, through Sprint 16: release hardening). Each sprint has a **Build** list and a **Gate** (tests/checks that must pass).
- A sprint is not done when it compiles — it's done when its gate passes. On gate failure: diagnose → fix → rerun the failed check → rerun the full gate → only then continue. Never disable tests, suppress lint without documented justification, or mock out production logic to force a pass.
- No placeholder methods or TODOs as a substitute for required work; no fake/disposable scaffolding.
- Blockers that need user input (signing secrets, developer account actions, branding assets, legal decisions) get recorded in `docs/BLOCKERS.md`; do not let them stop unblocked work.
- Record sprint gate evidence in `docs/verification/SPRINT_XX_GATE.md` (commands run, results, defects found/fixed).
- Never claim "complete" because an APK built — completion requires the Definition of Done at the end of `docs/10_DEVELOPMENT_SPRINTS_AND_QUALITY_GATES.md`.

## Product architecture (once implemented)

**Vexel Finance Passport** — an offline-first, private Android **personal finance diary**: the single place a regular household keeps organized records of their money life — daily expenses, monthly bills/utilities, income sources, loans and debts, receivables, savings/investment planning, and overall net worth. Application ID `pk.vexel.financepassport`, primary jurisdiction Pakistan (PKR base currency), architected for future jurisdiction packs. Built for someone with a regular job and a household keeping their own records, not an accountant or a full-time investor.

Core feature pillars, in priority order: (1) daily expense & income tracking, (2) recurring bills & utilities, (3) income sources, (4) loans & debts, (5) receivables, (6) savings & investment planning, (7) net worth at a glance, (8) document vault, and — as a supporting benefit of keeping good records, not the product's headline — (9) **Continuous Tax Capture**: because a tax-relevant event was already recorded once as part of ordinary diary-keeping, the app can classify it, link evidence, carry it through the correct tax year, reconcile it with financial records, and use it to prepare the annual return dataset without asking the user to re-enter data it already has. Treat this as a real, load-bearing feature (see `docs/05_CONTINUOUS_TAX_CAPTURE_ENGINE.md`) — just not the app's identity.

### Non-negotiable product rules (see `docs/01_PRODUCT_VISION_AND_RULES.md` and `docs/14_MASTER_AI_AGENT_BUILD_PROMPT.md`)

- Offline-first; no login/account required; no bank or FBR credentials stored; no ads/tracking SDKs; no mandatory cloud.
- One source of truth: a transaction/asset/document is entered once and reused everywhere.
- Financial facts and tax treatment are stored separately. Tax rules are versioned configuration, never hard-coded into financial records or UI.
- Every derived tax figure must be traceable back to its source records.
- Annual tax draft generation must never silently mutate source facts; regeneration is versioned.
- No automatic/unauthorized tax filing (no FBR/IRIS scraping or automation) — MVP only prepares a return-ready export.
- No destructive Room migrations, ever.

### Layering

```
UI (Compose) → ViewModel/UI state → Use cases (domain) → Repositories → Room / Files / Rules engine / Crypto
```

UI must never perform direct database queries or tax calculations.

### Planned module structure

`:app`, `:core:model`, `:core:database`, `:core:security`, `:core:files`, `:core:taxrules`, `:core:ui`, `:core:testing`, and `:feature:{onboarding,home,money,wealth,tax,records,vault,reports,backup,settings}`. A smaller module count is acceptable if package boundaries stay clean — architecture cleanliness matters more than module count (see `docs/19_INITIAL_REPOSITORY_STRUCTURE.md`).

### Tax rules engine (`core/taxrules`)

Interfaces: `TaxRulesetRepository`, `TaxClassifier`, `TaxDraftGenerator`, `WealthReconciler`, `TaxValidator`. Must be deterministic: same inputs + same ruleset version = same output. Core entities: `TaxYear`, `TaxItem`, `TaxMapping`, `TaxAnnualDraft`, `TaxDraftLine`, `WealthSnapshot`, `WealthReconciliation`, `TaxIssue`.

### Money and dates

- Never persist money as binary floating point. Use integer minor units (`Long`) or an explicit decimal representation with declared scale. Every amount carries a value + currency.
- Store instants for event timestamps that matter; store `LocalDate` semantics for tax/document dates. Never derive tax year from device locale — the ruleset defines tax-year boundaries.

### Security

PIN + biometric app lock, inactivity relock, deep-link lock enforcement (protect all deep-linked sensitive destinations behind app-lock state), Android Keystore-backed key management, authenticated encryption for sensitive files, no sensitive data in production logs, encrypted portable backup with tamper/integrity checks. No PIN-recovery backdoor.

### Documents

Room stores only metadata/links; document bytes live in an encrypted app-private file store (Storage Access Framework for import) — never store large PDFs/images as Room BLOBs.

### Platform baseline

Kotlin, Jetpack Compose, Material 3, single-activity architecture, `minSdk 26` / `compileSdk 36` / `targetSdk 36`, Coroutines + Flow, Room, DataStore (non-sensitive settings), WorkManager, Hilt (DI) acceptable, KSP where required. Use current mutually-compatible stable versions and record them in the repo once the project exists.

## Explicit MVP non-goals

Bank credential storage, open-banking sync, payment initiation, stock trading/brokerage execution, automated financial/tax/legal advice, FBR login automation/scraping, unreviewed automatic return filing, mandatory AI/cloud processing, social features, advertising.
