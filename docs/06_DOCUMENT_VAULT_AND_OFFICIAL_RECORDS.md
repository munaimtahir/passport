# Document Vault and Official Records

## Objective

Documents are evidence attached to a structured personal financial record, not isolated files.

## Storage

- App-private local storage
- File contents encrypted with AES-GCM
- Unique content-encryption key per file or robust envelope design
- Keys protected/wrapped using Android Keystore
- SHA-256 hash stored for integrity and duplicate detection
- No plaintext temporary copies left behind after import where avoidable

## Supported file types

MVP:
- PDF
- JPEG
- PNG
- WebP

Optional later:
- Office documents after explicit safe-handling design

## Categories

- Identity
- Tax
- Bank
- Salary
- Investment
- Property
- Vehicle
- Insurance
- Loan
- Business
- Receipt
- Other

## Evidence linking

One document can link to multiple records:
- Bank profit certificate → account + tax item + tax year
- Property registry → asset + tax-year acquisition item
- Broker annual statement → investment account + multiple tax items

## Document metadata

- Title
- Category
- Issuer
- Identifier
- Date
- Tax year(s)
- Expiry
- Tags
- Notes
- Linked entities

## Expiry reminders

Applicable to:
- Passport
- Insurance
- Vehicle documents
- licenses/certificates
- contracts where relevant

## Safe deletion

Before deleting:
“This document is evidence for 3 records.”

Actions:
- Cancel
- Unlink and delete
- Replace document

## Extraction

If on-device text/document extraction is implemented:
- extraction produces **suggestions**
- show source snippet/page when possible
- require user confirmation for monetary values
- never silently overwrite financial data
- store extraction status separately from user-confirmed values

## Official Records model

Official records are metadata + linked evidence.

Examples:
- CNIC/NICOP
- Passport
- NTN
- tax registration
- employment record
- property ownership
- vehicle registration
- insurance
- company/SECP certificates

Sensitive identifiers:
- hide by default
- store encrypted
- reveal only after explicit user action
