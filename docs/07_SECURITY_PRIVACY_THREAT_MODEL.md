# Security, Privacy and Threat Model

## Security objective

Loss or theft of the phone should not trivially expose the user's financial database, documents or identifiers.

## Privacy baseline

- Offline-first
- No mandatory account
- No ad SDK
- No behavior tracking containing financial data
- No bank login credentials
- No FBR credentials in MVP
- No third-party cloud document processing by default

## App lock

Required:
- PIN
- Biometric unlock where device supports it
- inactivity timeout
- lock on process/background transition according to configurable policy
- failed-attempt throttling

Do not implement “forgot PIN” backdoors that weaken local confidentiality.

## Key architecture

Recommended:
- Android Keystore protected master/wrapping key
- AES-GCM for sensitive field/document encryption
- random nonces
- authenticated encryption
- key-version support for rotation

Do not store cryptographic keys in source code, SharedPreferences, database rows or exported backups in plaintext.

## Database

Room stores structured records.

Sensitive values such as:
- full identifiers
- full account numbers
- document identifiers
may use application-layer encryption.

Evaluate whole-database encryption only if dependency maintenance/security is acceptable. Do not depend on an abandoned encryption library.

## Documents

- encrypted app-private storage
- no broad storage permissions
- use Android Storage Access Framework for user-selected imports/exports
- validate MIME/type/size
- prevent path traversal
- sanitize filenames used internally

## Screenshots

Provide a privacy setting:
- “Protect sensitive screens from screenshots”

Recommended default ON for:
- unlock
- document preview
- full identifiers
- backup secret entry

Assess whether to apply `FLAG_SECURE` globally or only to sensitive surfaces based on UX testing.

## Clipboard

- Avoid copying sensitive values automatically
- explicit copy only
- where platform supports, mark clipboard content sensitive
- clear transient sensitive UI values when leaving screen

## Logs

Never log:
- PIN
- encryption keys
- account numbers
- CNIC/NTN
- document text
- financial amounts in production diagnostic logs unless strictly redacted

Release build:
- no verbose debug logs
- no debug endpoints
- no test credentials

## Backup security

Controlled encrypted backup:
- versioned container
- encrypted payload
- integrity authentication
- manifest
- schema version
- document hashes
- restore preflight
- failure rollback

Disable or tightly control Android automatic backup if it can bypass the product's intended backup guarantees.

## Threats and controls

### Stolen unlocked phone
- short inactivity lock
- biometric/PIN
- privacy masking

### Stolen locked phone/file extraction
- app-private storage
- cryptographic protection
- Keystore

### Malicious document
- file validation
- no arbitrary execution
- safe preview
- size limits

### Backup theft
- encrypted portable backup
- password/key derivation design
- authenticated encryption

### Database corruption
- transactions
- integrity checks
- restore rollback
- test migrations

### Rule tampering
- bundled/versioned rules
- hash/signature metadata
- ruleset validation
- immutable historical version identifiers

### Supply-chain dependency compromise
- minimize dependencies
- lock versions
- dependency review
- Gradle verification where practical

## Privacy UX

Trust Center should show:
- Data stays on this device by default
- What the app stores
- What leaves the device
- Backup behavior
- Permissions used
- Export/delete controls

## Security acceptance criteria

- App lock cannot be bypassed through recents/deep links
- Sensitive screens are not exposed before unlock
- Documents are unreadable as plaintext in app storage
- Backup is unreadable without its secret/key material
- Restored data matches original hashes and counts
- Wrong backup password fails safely
- No financial/identifier values appear in normal production logs
