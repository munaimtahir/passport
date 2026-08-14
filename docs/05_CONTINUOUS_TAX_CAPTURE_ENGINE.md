# Continuous Tax Capture Engine Specification

## Product goal

Transform tax preparation from an annual reconstruction exercise into continuous capture.

## Core pipeline

**Source financial fact → Tax candidate → Tax item → Ruleset mapping → Review → Annual draft → Reconciliation → Export**

## 1. Capture sources

Tax candidates can originate from:

- Income transaction
- Expense transaction
- Investment event
- Asset acquisition
- Asset disposal
- Liability creation/repayment
- Receivable event
- Manual tax item
- Official document
- Tax certificate
- User-confirmed extraction
- Import

## 2. Tax relevance states

Every source record may be:

- UNKNOWN
- NOT_RELEVANT
- POTENTIALLY_RELEVANT
- RELEVANT

A ruleset may suggest relevance but user can override with reason.

## 3. Tax-event taxonomy

High-level categories:
- EMPLOYMENT_INCOME
- BUSINESS_INCOME
- PROFESSIONAL_INCOME
- RENTAL_INCOME
- BANK_PROFIT
- DIVIDEND
- CAPITAL_GAIN
- CAPITAL_LOSS
- OTHER_INCOME
- TAX_WITHHELD
- ADVANCE_TAX
- TAX_PAYMENT
- ASSET_ACQUISITION
- ASSET_DISPOSAL
- LIABILITY_CREATED
- LIABILITY_REPAID
- PERSONAL_EXPENDITURE
- DONATION
- ZAKAT
- INSURANCE_PENSION
- FOREIGN_INCOME
- FOREIGN_ASSET
- INVESTMENT_PURCHASE
- INVESTMENT_SALE
- OTHER_TAX_EVENT

Tax category names shown to users should be localized and simpler than internal codes.

## 4. Versioned jurisdiction ruleset

Implement interface conceptually as:

`TaxRuleset`
- jurisdiction
- taxYear
- version
- event classification rules
- field mappings
- validation rules
- evidence suggestions
- reconciliation rules
- rate tables if needed
- effective dates
- source references/version notes

### Critical architecture rule
Do not bake tax-year-specific field codes or rates into Room entities or UI navigation.

Tax rules belong in a versioned rules package.

## 5. Suggested ruleset storage

For MVP:
- bundled signed/versioned JSON assets
- immutable historical versions
- local parser and validator
- optional app-update delivery of new rulesets

Future:
- signed remote rules package update, but only after a strong authenticity mechanism is implemented.

## 6. Classification engine

For each source event:
1. Determine tax year by date and jurisdiction rules.
2. Determine candidate tax-event type.
3. Determine likely return section(s).
4. Determine whether evidence is recommended/required.
5. Produce a transparent proposed mapping.
6. If ambiguous, create a `Needs classification` issue.
7. Never guess an irreversible treatment.

## 7. User override

User can:
- change classification
- exclude from annual draft
- edit tax-only amount
- add explanation
- split one financial event into multiple tax treatments

Every override must preserve:
- original source
- original derived mapping
- override reason
- final used value

## 8. Duplicate detection

Duplicate candidates based on:
- same date or close date
- same amount
- same institution/counterparty
- same evidence hash
- same source
- same reference number

Duplicate detection only flags; it does not automatically delete financial records.

## 9. Evidence engine

Evidence statuses:
- NONE
- OPTIONAL
- REQUESTED
- ATTACHED
- VERIFIED_BY_USER
- NOT_AVAILABLE
- NOT_REQUIRED

Evidence suggestions vary by event type.

Example:
BANK_PROFIT:
- profit certificate
- bank statement
- withholding certificate

INVESTMENT_SALE:
- broker statement
- CDC statement where applicable
- transaction note

## 10. Tax readiness

Tax Readiness is a **workflow completeness score**, not a claim of legal compliance.

Suggested calculation dimensions:
- mapped items
- reviewed high-risk items
- evidence coverage
- unresolved duplicate warnings
- opening/closing wealth data
- reconciliation difference
- required annual profile fields

Show dimensions individually rather than only a percentage.

## 11. Annual draft generation

`Prepare Annual Tax Draft` runs deterministically.

Preconditions:
- tax year exists
- ruleset is valid
- database consistency passes

Output:
- draft metadata
- section totals
- field totals
- source links for every amount
- unresolved issues
- wealth reconciliation
- evidence checklist
- generation timestamp
- ruleset version

Regenerating creates a new draft version rather than silently modifying the prior reviewed version.

## 12. Wealth reconciliation

Generic model:

Opening net wealth  
+ recognized income/inflows affecting wealth  
− personal expenditure / consumption  
− recognized outflows not represented in closing assets  
± transfers, financing and allowable adjustments  
= expected closing wealth

Compare with recorded closing wealth.

The exact jurisdictional treatment is ruleset-controlled.

## 13. Annual close

Statuses:
OPEN → REVIEW → FILED

When marked FILED:
- freeze referenced annual draft
- store filing acknowledgement
- store filing date/reference
- store ruleset version
- create closing wealth snapshot

If revised:
FILED → REVISED
- create revision lineage
- do not overwrite original filing snapshot

## 14. One-click principle

“One click” means:
- one click to generate the draft from already captured data;
- not one click to bypass review, legal validation, credentials or official filing safeguards.

## 15. Future FBR adapter

Design an integration boundary:

`TaxSubmissionAdapter`

MVP implementation:
`ManualExportSubmissionAdapter`

Future:
`OfficialFbrSubmissionAdapter` only if an appropriate, authorized and documented FBR interface exists for this use.

The core product must not depend on that adapter.
