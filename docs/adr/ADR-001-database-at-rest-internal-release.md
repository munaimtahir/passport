# ADR-001: Database at Rest for Internal Release

Date: 2026-08-16  
Status: Accepted for internal release; reassess before public beta

## Decision

Do not introduce SQLCipher in this hardening pass. Continue using Android app-private Room storage, Keystore-backed field/file encryption, PIN/biometric gating, `FLAG_SECURE`, and authenticated portable backups.

## Rationale and prototype result

The current implementation has a tested Room migration chain through schema 8 and a live restore validator. Full database encryption would require a new dependency and a data-preserving migration of existing plaintext Room files, plus API 26 compatibility, Keystore invalidation behavior, restore validation, startup/performance measurements, and rollback testing. No safe prototype evidence exists in this repository that satisfies those gates. Introducing it now would increase the risk of making existing user data inaccessible.

This is a deliberate deferral, not a claim that the database file is independently unreadable to a compromised/rooted device. The threat model and public-beta gate must include a real migration prototype before changing the storage engine.

## Consequences

Internal release retains platform sandbox protection and encryption for sensitive fields/documents/backups. SQLCipher evaluation is a pre-public-beta requirement with explicit interruption, rollback, API 26/API 36, restore, and performance evidence.
