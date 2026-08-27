# Android Technical Architecture

## Platform baseline

- Kotlin
- Jetpack Compose
- Single-activity architecture
- Material 3
- minSdk 26
- compileSdk 36
- targetSdk 36
- Java/Kotlin toolchain selected for current stable Android Gradle Plugin compatibility

Use current stable mutually compatible dependency versions at build time; record them in the repository.

## Recommended libraries/components

Android/Jetpack:
- Compose UI
- Navigation Compose
- ViewModel
- Lifecycle
- Room
- DataStore
- WorkManager
- Biometric
- Android Keystore/JCA
- Storage Access Framework
- PdfDocument / suitable first-party-compatible PDF generation path where practical

Architecture:
- Coroutines + Flow
- Repository pattern
- Use-case/domain layer for non-trivial business logic
- Dependency injection (Hilt acceptable)
- KSP where required by current compatible stack

Testing:
- JUnit
- kotlinx-coroutines-test
- Room in-memory tests
- Compose UI tests
- Android instrumentation tests

## Module structure

Suggested:

`:app`
`:core:model`
`:core:database`
`:core:security`
`:core:files`
`:core:ui`
`:core:testing`
`:feature:onboarding`
`:feature:home`
`:feature:money`
`:feature:wealth`
`:feature:tax`
`:feature:records`
`:feature:vault`
`:feature:reports`
`:feature:settings`
`:feature:backup`

If build complexity becomes excessive for MVP, retain clear package boundaries inside a smaller module count. Architecture cleanliness is more important than module count.

## Layering

UI
↓
ViewModel / UI state
↓
Use cases
↓
Repositories
↓
Room / Files / Rules engine / Crypto

UI must not perform direct database queries or tax calculations.

## State model

Use immutable UI state:
- Loading
- Content
- Empty
- Error

One-off UI events should not be embedded as repeatedly consumed state.

## Money handling

Never use binary floating point for persisted money.

Recommended:
- store minor units as `Long` when currency scale is known, OR
- use a decimal representation with explicit scale in domain logic.

PKR can use minor-unit modeling consistently even if UI often displays whole rupees.

Every amount includes:
- value
- currency

## Dates

- Store instants for event timestamps where time matters
- Store LocalDate semantics for tax/document dates
- Do not derive tax year from device locale assumptions alone
- Ruleset defines tax-year boundaries

## Room migrations

- Explicit schema versions
- Migration tests
- Export Room schemas to repository
- No destructive migration in production

## Rules engine

Package:
`core/taxrules`

Interfaces:
- TaxRulesetRepository
- TaxClassifier
- TaxDraftGenerator
- WealthReconciler
- TaxValidator

Deterministic engine:
same inputs + same ruleset version = same output.

## Document storage

Database stores metadata.
Encrypted file service stores bytes.

Never store large PDFs/images as Room BLOBs.

## Search

MVP:
- metadata search
- tags
- categories
- dates
- amounts where relevant

Future:
- local full-text extraction/index

## Notifications

WorkManager/alarm strategy according to notification precision needs.

Use for:
- due-date reminders
- document expiry
- periodic monthly review reminder
- tax review reminders

Do not require background continuous service.

## Deep links

Protect all deep-linked sensitive destinations behind app-lock state.

## Error model

Domain error classes:
- ValidationError
- NotFound
- EncryptionError
- StorageError
- BackupIntegrityError
- RulesetError
- MigrationError

UI messages must be actionable and avoid exposing internals.

## Feature flags

Local build-time or settings-based flags for:
- document extraction
- experimental import
- future connectivity

Do not ship unfinished navigation to production users.
