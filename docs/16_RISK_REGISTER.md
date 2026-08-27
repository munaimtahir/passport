# Risk Register

## R1 — Tax law changes
**Risk:** Hard-coded rules become incorrect.  
**Control:** Versioned rulesets, historical immutability, explicit update process.

## R2 — Misleading tax confidence
**Risk:** User interprets readiness as professional/legal certification.  
**Control:** Separate readiness, warnings and review; clear disclaimers; source traceability.

## R3 — No public individual-return submission API
**Risk:** “One-click filing” cannot safely/officially submit.  
**Control:** MVP one-click preparation; adapter boundary for future official integration.

## R4 — Data breach on lost phone
**Control:** PIN/biometric, Keystore, field/file encryption, inactivity lock.

## R5 — Backup loss or corruption
**Control:** authenticated encryption, hashes, restore staging, transactional commit.

## R6 — Encryption dependency abandonment
**Control:** minimize cryptographic dependencies; use platform primitives where suitable; review maintenance state.

## R7 — User duplicates records
**Control:** source linkage and duplicate candidates; do not silently merge.

## R8 — Incorrect investment cost basis
**Control:** transparent calculations, editable events, explicit methodology, tests.

## R9 — Large document storage
**Control:** stream files, metadata DB, storage usage screen, document cleanup.

## R10 — App becomes too complex
**Control:** simple top-level navigation, Quick Add, progressive disclosure, advanced tax details only in annual workspace.

## R11 — Cross-module inconsistency
**Control:** canonical FinancialEvent and linked source architecture.

## R12 — AI/OCR extraction errors
**Control:** suggestions only, user confirmation, source preview, never silent commit.

## R13 — Regulatory/product classification changes
**Control:** avoid money movement/advice; review store/regulatory requirements before adding connected finance features.

## R14 — Device migration
**Control:** portable encrypted backup/restore tested as a core feature.

## R15 — User forgets PIN
**Risk:** Privacy design conflicts with recoverability.  
**Control:** warn clearly; allow encrypted backup recovery path where appropriate; do not create hidden bypass.
