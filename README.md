# Vexel Finance Passport

Vexel Finance Passport is a private, offline-first Android financial passport for recording money, wealth, evidence, and tax-workflow facts locally.

## Privacy and scope

Data stays on the device by default. Sensitive records and vault documents use app-private and encrypted storage. The app does not include advertising, analytics, tracking, bank credentials, trading, payment initiation, cloud sync, or automatic FBR submission.

## Features

- PKR whole-rupee capture with exact minor-unit storage
- Accounts, income, expenses, transfers, recurring reminders/drafts, wealth, and goals
- Source-linked tax candidates, annual drafts, evidence tracking, and reconciliation
- Encrypted document vault, reports, structured export, and password-protected backup/restore
- PIN/biometric security and guarded local data deletion

## Build and test

```bash
./gradlew clean assembleDebug
./gradlew test
./gradlew lint
./gradlew connectedDebugAndroidTest
```

The internal QA target uses API 26–36 compatibility and a debug/internal signing configuration. Production signing, Play publication, cloud integrations, and external tax integrations are deferred.

## Structure

Production Android code is under `app/src/main`. Shared models, Room persistence, security, backup, reports, and tax rules are under `core` packages. Product, architecture, acceptance, and verification evidence live under `docs/`.

## Status

This repository is undergoing hardening toward a verified internal QA release. See `docs/BUILD_STATUS.md`, `docs/FINAL_VERIFICATION.md`, and `docs/verification/` for current evidence and limitations.
