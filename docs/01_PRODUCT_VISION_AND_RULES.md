# Product Vision and Rules

## 1. Product problem

Personal financial information is fragmented across bank apps, brokerage portals, PDFs, paper records, tax files, messaging attachments, spreadsheets and memory. Conventional budgeting apps solve only a narrow transaction problem.

Vexel Finance Passport treats the user’s financial life as a **structured personal record**.

It answers:

- What do I own?
- What do I owe?
- Where is my money?
- What changed this year?
- What documents prove it?
- What tax-relevant events occurred?
- What information is still missing?
- Can I produce a coherent annual financial/tax package without reconstructing the year from scratch?

## 2. Product positioning

### Do say

- Private financial passport
- Personal financial record
- Offline-first financial organizer
- Continuous tax capture
- Financial document and evidence vault
- Annual tax preparation workspace
- Net-worth and financial statement generator

### Do not position MVP as

- A bank
- A brokerage
- A tax practitioner
- An official FBR filing client
- A robo-adviser
- A money-transfer service

## 3. Core product loops

### Daily / event-driven loop
Event occurs → user records it → optional tax relevance is assigned → evidence attached → dashboard updates.

### Monthly loop
Review accounts → verify recurring obligations → review missing evidence → confirm tax items.

### Annual loop
Close tax year → reconcile wealth → resolve missing/uncertain items → generate return-ready dataset and tax pack → user/accountant reviews → mark filed → archive filing acknowledgement.

## 4. User personas

### A. Salaried professional
Needs salary, bank profit, investments, assets, withholding certificates and wealth reconciliation.

### B. Mixed-income professional
Salary + clinic/consultancy/freelance/business income + investments.

### C. Investor
PSX, mutual funds, dividends, capital gains/losses, CDC/broker statements, bank movements.

### D. Small business owner
Personal and business boundaries, receivables, capital introduced, withdrawals, tax evidence.

### E. Family financial administrator
Needs household assets, obligations, documents, goals and annual financial records.

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
