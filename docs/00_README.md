# Vexel Finance Passport — AI Development Pack

**Project type:** Android personal finance diary — daily expense/income tracking, bills, debts, receivables, investment planning, document vault, with tax-ready records as a supporting benefit  
**Public working name:** Vexel Finance Passport  
**Suggested repository folder:** `finance`  
**Suggested Android application ID:** `pk.vexel.financepassport`  
**Primary market:** Pakistan-first, architecture designed for future jurisdiction packs  
**Product philosophy:** Offline-first, private, user-controlled, structured, evidence-backed

## Product in one sentence

Vexel Finance Passport is a private personal finance diary for a regular household: the single place to record what you earn, what you spend, what you owe, what's owed to you, and what you're building toward — kept organized enough, all year, to also produce clean financial statements and a tax-return-ready annual dataset without ever re-entering the same fact twice.

## Core feature pillars

Vexel Finance Passport is built for someone with a regular job and a household keeping their own records — not an accountant, not a full-time investor. In priority order:

1. **Daily expense & income tracking** — every transaction, categorized, in one place
2. **Recurring bills & utilities** — rent, electricity, subscriptions, anything monthly, tracked so nothing's forgotten
3. **Income sources** — salary, side income, multiple sources, one ledger
4. **Loans & debts** — what you owe, to whom, repayment progress
5. **Receivables** — what's owed to you, partial-payment tracking
6. **Savings & investment planning** — holdings, contributions, growth over time (manually recorded, not live trading)
7. **Net worth at a glance** — assets, liabilities, liquid funds, one number you trust
8. **Document vault** — receipts, certificates, records tied to all of the above

## Supporting feature: tax-ready records

This is **not primarily a tax app** — but because everything above is tracked in one place all year, the app can also keep the user's records tax-ready automatically, without a separate re-entry step:

> Record a tax-relevant event once, when it happens, as part of ordinary diary-keeping. The app classifies it, links evidence, carries it through the correct tax year, reconciles it with financial records, and uses it to prepare the annual return dataset when filing time arrives.

The user should never be asked at return time to re-enter information the app already knows. This is a real, load-bearing capability (see `docs/05_CONTINUOUS_TAX_CAPTURE_ENGINE.md`) — it just isn't the product's identity.

## Core principles

1. **Offline by default**
2. **No account required**
3. **No bank credentials**
4. **No mandatory cloud**
5. **No advertising SDKs**
6. **Sensitive local data protected by biometric/PIN app lock and cryptographic controls**
7. **One source of truth** — a transaction, asset or document is entered once and reused everywhere
8. **Tax rules are versioned configuration, not hard-coded into financial records**
9. **Evidence is linked to the event/entity it proves**
10. **User remains in control of every generated tax figure**
11. **No automatic tax submission in MVP**
12. **Export and encrypted backup are first-class features**

## Primary navigation

- Home
- Money
- Wealth
- Tax & Records
- Vault
- More

## Major modules

- Financial Dashboard
- Accounts
- Transactions
- Income & Expenses
- Transfers
- Assets
- Liabilities
- Investments
- Receivables & Payables
- Financial Goals
- Financial Calendar
- Continuous Tax Capture
- Annual Tax Workspace
- Wealth Reconciliation
- Official Documents
- Document Vault
- Reports
- Backup / Restore
- Export / Delete
- Security / Privacy

## What is in this pack

1. Product vision and product rules
2. Complete functional specification
3. Information architecture and UX flows
4. Data model and entity relationships
5. Continuous Tax Capture engine specification
6. Document vault and evidence-linking design
7. Security and privacy architecture
8. Android technical architecture
9. UI design system
10. Development sprints and quality gates
11. ADB/device verification plan
12. Backup, export and reporting specification
13. Play Store/release hardening plan
14. Master AI-agent build prompt
15. Acceptance-test catalog
16. Risk register
17. Post-MVP roadmap
18. Current platform/source notes
19. Machine-readable product manifest
20. Machine-readable tax event taxonomy

## MVP success definition

MVP is complete only when a user can:

- create a private local profile;
- add/edit/archive financial accounts;
- record income, expenses and transfers;
- add assets, liabilities, investments and receivables;
- add tax-relevant events directly or from another module;
- attach/link evidence to financial and tax records;
- view a tax-year workspace with captured totals and missing-evidence warnings;
- generate an annual tax preparation pack from stored records;
- generate personal financial/net-worth reports;
- back up and restore all supported data transactionally;
- export/delete their data;
- protect the application with biometric/PIN;
- complete the core workflow entirely offline.

## Explicit MVP non-goals

- Bank credential storage
- Open-banking synchronization
- Payment initiation
- Stock trading
- Brokerage execution
- Automated financial advice
- Automated tax/legal advice
- Scraping or automating FBR login credentials
- Unreviewed automatic filing of an income tax return
- Mandatory AI/cloud processing
- Social/community features
- Advertising

## Development doctrine

Every sprint follows:

**Implement → compile → unit test → instrumentation test → device test → quality gate → fix failures → rerun gate → only then move forward.**

No failed gate may be ignored merely to preserve schedule.
