# Complete Functional Specification

## A. Onboarding

### Required
- Welcome
- Privacy-first explanation
- Choose currency: PKR default, extensible
- Optional display name
- Create app PIN
- Offer biometric unlock
- Create initial financial year/tax-year context
- Optional guided setup:
  - add bank account
  - add cash account
  - add investment account
  - add major asset
  - start with empty app

### Onboarding principles
- Maximum 5 essential screens
- No mandatory online registration
- No mandatory document upload
- User can skip optional setup
- Use progressive disclosure

---

# B. Home Dashboard

## Primary card
**Net Worth**
- current value
- change from previous month
- change from selected year start
- tap for breakdown

## Summary cards
- Assets
- Liabilities
- Liquid funds
- Investments
- Receivables
- Monthly income
- Monthly expense
- Tax captured this year

## Upcoming
- bills
- loan installments
- tax deadlines entered/configured
- document expiry
- receivable due dates
- investment maturity

## Tax readiness card
- selected tax year
- captured items count
- missing evidence count
- unresolved classifications
- reconciliation status

## Quick Add
- Income
- Expense
- Transfer
- Tax item
- Asset
- Document

---

# C. Money

## Accounts
Types:
- Cash
- Current account
- Savings account
- Wallet/e-money
- Foreign-currency account
- Brokerage cash
- Other

Fields:
- name
- institution
- account nickname
- masked account number
- currency
- opening balance
- current balance
- ownership
- active/archive status
- notes

Sensitive full account numbers should be optional and encrypted.

## Transactions

Types:
- Income
- Expense
- Transfer
- Adjustment

Common fields:
- date/time
- amount
- currency
- account
- category
- counterparty
- notes
- tags
- recurring flag
- tax relevance state
- evidence links

## Transfers
A transfer creates paired ledger movements and must never be counted as income/expense.

## Recurring entries
- salary
- rent
- bills
- subscriptions
- loan installment
- savings contribution
- recurring investment

The system should create reminders or draft entries, not silently invent confirmed transactions unless user explicitly enables that behavior.

---

# D. Wealth

## Assets
Types:
- Property
- Vehicle
- Gold/precious metals
- Business interest
- Cash-equivalent
- Personal high-value asset
- Other

Fields:
- acquisition date
- acquisition cost
- current estimated value
- ownership percentage
- location/description
- funding source
- disposal date/value
- evidence

## Liabilities
Types:
- Credit card
- Personal loan
- Car financing
- Home financing
- Informal borrowing
- Business-related personal liability
- Other

Track:
- original amount
- outstanding amount
- interest/markup metadata
- installment
- due date
- lender
- linked asset
- evidence

## Investments
Types:
- PSX equity
- Mutual fund
- T-bill
- PIB
- Sukuk
- National Savings
- Term deposit
- Gold
- Foreign currency
- Other security

Functions:
- buy
- sell
- dividend/distribution
- profit/markup
- fees
- taxes withheld
- corporate action notes
- current valuation
- realized gain/loss
- unrealized gain/loss
- evidence

MVP may use manual current prices. Live prices are post-MVP.

## Receivables
- person/entity
- amount
- origin
- due date
- partial receipts
- reminder
- notes
- evidence
- tax relevance

---

# E. Goals and Planning

## Goals
- emergency fund
- education
- house
- vehicle
- travel
- retirement
- custom

Track:
- target amount
- target date
- allocated accounts
- progress
- suggested monthly contribution as deterministic calculation

Avoid personalized investment recommendations in MVP.

---

# F. Financial Calendar

Unified calendar for:
- bills
- loan due dates
- tax reminders
- document expiry
- investment maturity
- receivable due dates
- insurance renewal
- annual review

Supports:
- reminder notifications
- snooze
- mark complete
- open linked record

---

# G. Tax & Records

## Tax Inbox
A chronological list of tax-relevant captured items.

Sources:
- direct tax entry
- transaction
- investment event
- asset event
- liability event
- document extraction suggestion
- imported structured record

Each item displays:
- date
- description
- amount
- tax year
- category
- evidence status
- mapping status
- review state

## Tax item states
- Draft
- Captured
- Needs evidence
- Needs classification
- Reviewed
- Included in annual draft
- Excluded with reason

## Annual Tax Workspace
Sections:
- Income
- Tax deducted/withheld
- Investments
- Capital gains/losses
- Assets acquired
- Assets disposed
- Liabilities
- Personal expenditure
- Other tax-relevant data
- Documents
- Reconciliation
- Review issues

## One-click preparation
“Prepare Annual Tax Draft”:
- takes all eligible items
- applies selected versioned ruleset
- computes grouped totals
- links sources
- detects duplicate candidates
- detects missing evidence
- detects unmapped items
- reconciles opening and closing wealth
- produces a draft, never an irreversible submission

## Tax-year close
After user marks return filed:
- store filing date
- store acknowledgement/document
- freeze annual snapshot
- retain ability to reopen as amended/revised with explicit versioning

---

# H. Official Records

Record types:
- CNIC/NICOP
- Passport
- NTN
- Tax registration
- Employment/salary documentation
- SECP/company documents
- Property ownership
- Vehicle registration
- Insurance
- Bank certificates
- Investment certificates
- Loan agreements
- Pension documents
- Other

Fields:
- title
- type
- issuer
- identifier
- issue date
- expiry date
- related person/entity
- linked financial objects
- attachment(s)
- reminder

---

# I. Vault

Features:
- import PDF/image/document
- local storage
- categories/tags
- search by metadata
- link to one or multiple entities/events
- document version/replacement
- expiry date
- evidence status
- safe delete with dependency warning

MVP does not require cloud OCR. Optional on-device extraction may be introduced if implementation remains local and reliable.

---

# J. Reports

Required:
- Net Worth Statement
- Asset Statement
- Liability Statement
- Income & Expense Report
- Cash Flow Summary
- Investment Summary
- Receivables Report
- Annual Financial Summary
- Tax Preparation Summary
- Wealth Reconciliation Report
- Evidence Checklist

Formats:
- in-app preview
- PDF
- CSV for tabular data where applicable

Every financial report:
- date/date range
- currency
- generation timestamp
- source scope
- optional notes
- disclaimer where needed

---

# K. Backup / Restore

## Backup
- user-initiated
- encrypted
- single portable backup package
- includes database + vault documents + metadata + schema version
- integrity manifest/checksum
- user-defined backup password or secure key wrapping design

## Restore
- preview backup metadata
- compatibility check
- integrity verification
- transactional restore
- rollback on failure
- no partial restore state

---

# L. Export / Delete

## Export
- JSON full structured export
- CSV selected data
- PDF reports
- user-accessible document export

## Delete
- delete individual record
- delete tax year
- delete document
- delete all application data
- confirmation for destructive actions
- explicit dependency handling
