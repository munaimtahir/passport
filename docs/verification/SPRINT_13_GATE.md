# Sprint 13 Gate — Backup / Restore / Export / Delete

Date: 2026-08-14
Status: IN PROGRESS

The portable backup cryptographic container is implemented with a versioned header, random salt and nonce, PBKDF2-HMAC-SHA256 key derivation, AES-GCM authentication, and wrong-password/tamper tests. User-facing encrypted backup export uses SQLite `VACUUM INTO` where available and a WAL-safe API 26 fallback without unsafe WAL-mode toggling, packages encrypted Vault files, and restores through staging, schema validation, rollback, atomic replacement, and vault-file restoration. `BackupRestoreDeviceTest` verifies populated backup → delete → restore → Room reopen on API 36 with encrypted document bytes, hash, decryption, and two links preserved; API 26 verifies the fallback path. Full SAF/export UI walkthrough remains pending.
