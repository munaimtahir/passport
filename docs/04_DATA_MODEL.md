# Data Model and Domain Model

## Design rule

Financial facts and tax interpretations must be separate.

A `FinancialEvent` is a historical fact.  
A `TaxMapping` is a ruleset-specific interpretation of that fact.

## Core entities

### UserProfile
- id
- displayName
- baseCurrency
- createdAt
- updatedAt

### Account
- id
- name
- institution
- accountType
- maskedIdentifier
- encryptedSensitiveIdentifier
- currency
- openingBalance
- openingBalanceDate
- ownershipType
- status
- notes
- createdAt
- updatedAt

### FinancialEvent
Canonical event record.
- id
- eventType
- date
- amount
- currency
- accountId nullable
- categoryId nullable
- counterpartyId nullable
- description
- notes
- sourceType
- sourceEntityId nullable
- taxRelevance: UNKNOWN / NOT_RELEVANT / POTENTIALLY_RELEVANT / RELEVANT
- createdAt
- updatedAt
- deletedAt nullable

### TransferLink
- id
- sourceEventId
- destinationEventId
- transferGroupId

### Asset
- id
- type
- title
- acquisitionDate
- acquisitionCost
- currency
- currentEstimatedValue
- valuationDate
- ownershipPercent
- fundingSourceNotes
- disposalDate nullable
- disposalValue nullable
- status

### Liability
- id
- type
- title
- lender
- originalAmount
- outstandingAmount
- currency
- startDate
- dueDate nullable
- installmentAmount nullable
- linkedAssetId nullable
- status

### InvestmentAccount
- id
- name
- institution
- type
- currency

### Security
- id
- symbol
- name
- market
- securityType
- currency

### InvestmentEvent
- id
- investmentAccountId
- securityId nullable
- type: BUY / SELL / DIVIDEND / DISTRIBUTION / PROFIT / FEE / TAX_WITHHELD / ADJUSTMENT
- date
- quantity nullable
- unitPrice nullable
- grossAmount
- fees
- taxWithheld
- netAmount
- linkedFinancialEventId nullable

### Receivable
- id
- counterpartyId
- title
- originalAmount
- outstandingAmount
- createdDate
- dueDate nullable
- status

### Goal
- id
- title
- type
- targetAmount
- targetDate nullable
- linkedAccountIds
- status

### Party
- id
- displayName
- type: PERSON / BUSINESS / INSTITUTION / GOVERNMENT / OTHER
- notes

---

# Tax entities

### TaxYear
- id
- jurisdictionCode
- yearLabel
- startDate
- endDate
- rulesetVersion
- status: OPEN / REVIEW / FILED / REVISED / ARCHIVED
- filingDate nullable
- filingReference nullable

### TaxItem
Represents a tax-relevant item presented to the user.

- id
- taxYearId
- sourceType
- sourceId
- taxEventType
- date
- grossAmount nullable
- taxWithheld nullable
- netAmount nullable
- currency
- description
- reviewState
- evidenceState
- userOverrideCategory nullable
- exclusionReason nullable
- createdAt
- updatedAt

### TaxMapping
Derived mapping from source fact to ruleset.
- id
- taxItemId
- rulesetVersion
- sectionCode
- categoryCode
- treatmentCode
- calculatedAmount
- calculationJson
- confidence/derivationState
- generatedAt
- supersededAt nullable

### TaxAnnualDraft
- id
- taxYearId
- draftVersion
- rulesetVersion
- generatedAt
- status
- summaryJson
- reconciliationId

### TaxDraftLine
- id
- draftId
- sectionCode
- fieldCode
- label
- amount
- calculationJson
- userAdjustedAmount nullable
- adjustmentReason nullable

### WealthSnapshot
- id
- taxYearId
- snapshotDate
- assetsTotal
- liabilitiesTotal
- netWealth
- sourceJson
- status

### WealthReconciliation
- id
- taxYearId
- openingWealth
- recognizedInflows
- personalExpenditure
- recognizedOutflows
- adjustments
- expectedClosingWealth
- recordedClosingWealth
- unexplainedDifference
- status
- calculationJson

### TaxIssue
- id
- taxYearId
- severity
- type
- title
- explanation
- sourceType
- sourceId nullable
- resolvedAt nullable
- resolutionNote nullable

---

# Documents

### Document
- id
- title
- category
- originalFilename
- mimeType
- sizeBytes
- localEncryptedPath
- sha256
- issuer nullable
- documentIdentifierEncrypted nullable
- issueDate nullable
- expiryDate nullable
- notes
- createdAt
- updatedAt

### DocumentLink
Many-to-many link.
- id
- documentId
- entityType
- entityId
- purpose: EVIDENCE / REFERENCE / IDENTITY / TAX / OTHER

### OfficialRecord
- id
- type
- title
- issuer
- identifierEncrypted
- issueDate
- expiryDate nullable
- notes

---

# Audit/versioning

### ChangeLog
Local user-visible audit support for important records.
- id
- entityType
- entityId
- operation
- timestamp
- changedFieldsJson
- source

Do not retain deleted sensitive values unnecessarily. The audit log should retain enough metadata for explainability without defeating deletion.

---

# Derived views

- current account balances
- monthly income/expense
- net worth
- investment positions
- tax readiness
- missing evidence
- unresolved tax items
- tax-year totals
- financial calendar
