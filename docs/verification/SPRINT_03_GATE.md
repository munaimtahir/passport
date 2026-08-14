# Sprint 03 Gate — Security Foundation

Date: 2026-08-14
Status: PARTIAL

- First-launch PIN creation and verification gate the entire application.
- PIN material is PBKDF2-HMAC-SHA256 derived with a random salt; the PIN itself is never persisted.
- Keystore AES-GCM service uses a provider-generated IV for API compatibility and supports authenticated encryption/decryption; populated API 36 device restore verifies the round trip.

BiometricPrompt integration, lifecycle relock, screenshot protection (`FLAG_SECURE`), and exponential failed-attempt throttling are implemented. Pending: lock-state instrumentation and full biometric hardware/device verification.
