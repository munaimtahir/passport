# Utility Tracker Data Model

The data model enforces strict separation between recurring connection definition, occurrence, and payment.

## Entities

### 1. UtilityBillProfile
- `id: String` (PK)
- `name: String`
- `category: String` (Electricity, Gas, Telephone, Other)
- `referenceNumber: String`
- `issueDayAnchor: Int`
- `dueDayAnchor: Int`
- `recurrenceStartMonth: String` (YYYY-MM)
- `status: String` (ACTIVE, ARCHIVED)
- `provider: String?`
- `customCategoryName: String?`
- `locationLabel: String?` (Home, Clinic, Office, etc.)
- `connectionIdentifier: String?`
- `notes: String?`
- `reminderPreference: String?`
- `createdAtEpochMillis: Long`
- `updatedAtEpochMillis: Long`

### 2. MonthlyBillOccurrence
- `id: String` (PK)
- `profileId: String` (FK -> UtilityBillProfile.id CASCADE)
- `billingYear: Int`
- `billingMonth: Int`
- `expectedIssueDateEpochDay: Long`
- `expectedDueDateEpochDay: Long`
- `actualIssueDateEpochDay: Long?`
- `actualDueDateEpochDay: Long?`
- `amountMinor: Long?`
- `status: String` (Expected, Pending, Due soon, Overdue, Paid, Skipped)
- `notes: String?`
- `creationSource: String` (Automatic, Manual)
- `createdAtEpochMillis: Long`
- `updatedAtEpochMillis: Long`

*Database Index:* Unique index on `(profileId, billingYear, billingMonth)`.

### 3. PaymentRecord
- `id: String` (PK)
- `occurrenceId: String` (FK -> MonthlyBillOccurrence.id CASCADE)
- `amountPaidMinor: Long`
- `paymentDateEpochDay: Long`
- `paymentMode: String` (Cash, Online banking, Other)
- `bankName: String?`
- `transactionReference: String?`
- `notes: String?`
- `createdAtEpochMillis: Long`
- `updatedAtEpochMillis: Long`

*Database Index:* Unique index on `occurrenceId` to enforce the one-completed-payment-per-occurrence rule.

### 4. BillAttachment
- `id: String` (PK)
- `linkedId: String` (FK -> profile, occurrence, or payment)
- `attachmentType: String` (BILL, PAYMENT)
- `storagePath: String`
- `displayName: String`
- `mimeType: String`
- `sizeBytes: Long`
- `fileHash: String?`
- `createdAtEpochMillis: Long`
