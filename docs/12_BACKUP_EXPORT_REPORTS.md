# Backup, Export and Reporting Specification

## Backup container

Suggested logical contents:

```
backup/
  manifest.json
  database.enc
  documents/
    <uuid>.enc
  rulesets/
    referenced_versions.json
```

Manifest:
- product
- backup format version
- app version
- created timestamp
- database schema version
- document count
- record counts
- hashes
- KDF/encryption metadata
- ruleset versions referenced

## Encryption

Use authenticated encryption.

If password-based portable backup is supported:
- use a modern KDF available/maintainable in the chosen Android stack;
- random salt;
- appropriate work factor;
- AES-GCM or similarly strong authenticated encryption;
- never reuse nonces.

Keep crypto implementation small and testable.

## Restore lifecycle

1. Select file
2. Read non-sensitive header
3. Request password if needed
4. Authenticate/decrypt manifest
5. Validate compatibility
6. Validate hashes
7. Import into staging database/storage
8. Run migrations
9. Validate referential integrity
10. Atomically switch/commit
11. Clean staging
12. Show summary

## JSON export

Human/machine readable:
- profile
- accounts
- transactions
- assets
- liabilities
- investments
- tax items
- tax drafts
- document metadata
- official records

Do not include encrypted internal blobs as if they were user-readable data.

## Reports

### Net Worth
- date
- assets by category
- liabilities
- net worth
- source note

### Annual Financial Summary
- opening/closing position
- income
- expenditure
- investments
- major acquisitions/disposals

### Tax Preparation Pack
- tax year
- ruleset version
- income categories
- tax withheld/paid
- investment summaries
- asset changes
- liability changes
- expenditure summary
- wealth reconciliation
- unresolved items
- evidence checklist

### Accountant package
Optional export composition selected by user:
- tax preparation PDF
- CSV summaries
- selected evidence documents

No sharing occurs until explicit Android share/export action.
