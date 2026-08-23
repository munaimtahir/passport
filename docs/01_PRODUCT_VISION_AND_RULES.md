# Product Vision and Rules

## 1. Product problem

Personal financial information is fragmented across bank apps, brokerage portals, PDFs, paper records, tax files, messaging attachments, spreadsheets and memory. Conventional budgeting apps solve only a narrow transaction problem.

Vexel Finance Passport treats the user's financial life as a **structured personal diary** — built for someone with a regular job and a household keeping their own records, not an accountant or a full-time investor.

It answers:

- What did I spend today, this month?
- What are my recurring bills and utilities?
- Where does my income come from?
- What do I owe, and to whom?
- What's owed to me?
- What am I saving/investing toward?
- What do I own, what changed this year?
- What documents prove it?
- Can I produce a coherent annual financial/tax package without reconstructing the year from scratch, because I already kept good records?

## 2. Product positioning

### Do say

- Personal finance diary
- Offline-first financial organizer
- Private financial passport / personal financial record
- Daily expense and income tracker
- Loans, debts, and receivables tracker
- Savings and investment planner
- Financial document and evidence vault
- Net-worth and financial statement generator
- Keeps your records tax-ready automatically (supporting feature, not the headline)

### Do not position MVP as

- A bank
- A brokerage
- A tax practitioner
- An official FBR filing client
- A robo-adviser
- A money-transfer service

## 3. Core product loops

### Daily / event-driven loop
Expense, income, bill payment, or loan/receivable movement occurs → user records it → optional tax relevance is assigned in the background → evidence attached → dashboard updates.

### Monthly loop
Review accounts → verify recurring bills/utilities were paid → check loan/receivable balances → review missing evidence → confirm tax items.

### Annual loop
Review the year's diary → reconcile wealth → resolve missing/uncertain items → generate a return-ready dataset and tax pack as a byproduct of the records already kept → user/accountant reviews → mark filed → archive filing acknowledgement.

## 4. User personas

### A. Family financial administrator (primary persona)
A regular salaried household: daily expenses, monthly bills/utilities, income, loans/debts, receivables, savings goals, and organized annual records — this is the core, everyday use case the product is built around.

### B. Salaried professional
Needs salary, bank profit, investments, assets, withholding certificates and wealth reconciliation.

### C. Mixed-income professional
Salary + clinic/consultancy/freelance/business income + investments.

### D. Investor
PSX, mutual funds, dividends, capital gains/losses, CDC/broker statements, bank movements.

### E. Small business owner
Personal and business boundaries, receivables, capital introduced, withdrawals, tax evidence.

## 5. Product rules

### Rule 1: Enter once
A dividend captured in Portfolio must not need to be re-entered in Tax.

### Rule 2: Keep original facts immutable in meaning
Tax mapping may change by tax year; the underlying event does not.

### Rule 3: Evidence is optional for entry but visible as incomplete
Do not block capture because a receipt/certificate is not yet available.

### Rule 4: Separate facts from classification
FinancialEvent = fact. TaxTreatment = interpretation under a versioned ruleset.

### Rule 5: Never silently alter user financial records
Derived classifications can be recomputed; source values require user-visible edits and audit history.

### Rule 6: Tax readiness is not tax correctness
Readiness indicates completion/reconciliation of captured information, not professional certification.

### Rule 7: Local operation must remain functional
Network failure must not prevent ordinary recording, reviewing, reporting or tax-workspace use.

### Rule 8: Explain every derived value
A tax/wealth total must support “Show calculation” and source drill-down.

### Rule 9: Preserve history
Changing current values does not rewrite prior year-end snapshots.

### Rule 10: Privacy first
No analytics/tracking SDK should receive financial values, document names or personally identifying financial metadata.
