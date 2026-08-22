# ADR-001: Database-at-rest protection for internal QA

Date: 2026-08-17  
Status: Accepted for internal release

## Decision

Keep the Room database in Android app-private storage for the internal release and retain the existing Keystore-backed field/file protections. Do not introduce SQLCipher in this hardening pass.

## Evidence and threat model

The principal risks are extraction from a rooted/unlocked device, backup leakage, accidental plaintext document storage, restore tampering, and loss of access after a Keystore/PIN change. The current implementation protects sensitive identifiers and vault files with Keystore AES-GCM, keeps the database app-private, and uses an authenticated encrypted portable backup. The database snapshot is validated before live replacement and restore has rollback handling.

A full-database encryption migration would add an unverified dependency and require a live migration, interruption/rollback, API 26/API 36, Room schema, Keystore invalidation, and restore compatibility prototype. No safe prototype evidence exists in this repository yet; merging it would increase data-loss risk without improving the current internal gate. The portable backup remains encrypted independently of the database storage decision.

## Consequences

This is a bounded internal-release decision, not a claim that app-private SQLite protects a rooted device. Full database encryption is a pre-public-beta requirement. The follow-up prototype must preserve existing data under interruption, validate on API 26 and API 36, and prove rollback before adoption.
