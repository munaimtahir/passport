# Development Sprints and Quality Gates

## Global rule

A sprint is complete only when its gate passes. If a gate fails:

1. diagnose;
2. fix;
3. rerun the failed test;
4. rerun the full sprint gate;
5. only then continue.

User-input blockers should be parked in `docs/BLOCKERS.md` and non-blocked work should continue.

---

# Sprint 0 — Repository and Build Foundation

## Build
- create Android project
- application ID
- Compose
- min/target/compile SDK
- dependency version catalog
- CI-ready Gradle commands
- package/module structure
- lint baseline only if justified
- README
- architecture decision records

## Gate
- `./gradlew clean assembleDebug`
- unit-test task passes
- lint passes
- debug APK installs
- app launches
- no placeholder crash

---

# Sprint 1 — Design System and Navigation

## Build
- theme
- typography
- components
- bottom navigation
- adaptive scaffold
- privacy-value masking
- empty states

## Gate
- Compose UI tests for navigation
- font scaling smoke test
- portrait/landscape smoke test
- no inaccessible icon-only critical actions
- screenshot review at common phone dimensions

---

# Sprint 2 — Local Data Foundation

## Build
- Room schema
- entities/DAOs
- repositories
- migrations framework
- money value type
- date handling
- seed/test fixtures

## Gate
- DAO unit/instrumentation tests
- transaction integrity tests
- migration test from initial schema fixture
- no destructive migration
- money arithmetic tests

---

# Sprint 3 — Security Foundation

## Build
- PIN
- biometric
- lock state
- Keystore master key
- sensitive-field encryption service
- sensitive-screen protection
- secure logging policy

## Gate
- fresh install lock flow
- background/resume lock
- deep-link lock bypass test
- wrong PIN behavior
- biometric cancellation
- encrypted round-trip tests
- release log review

---

# Sprint 4 — Money

## Build
- accounts
- income
- expense
- transfer
- categories
- activity list
- account balances
- editing/archive

## Gate
- balance invariants
- transfer not counted as income/expense
- CRUD tests
- process recreation
- navigation
- device smoke test

---

# Sprint 5 — Wealth

## Build
- assets
- liabilities
- receivables
- investments
- holdings calculations
- manual valuations

## Gate
- buy/sell position tests
- realized/unrealized calculation tests
- asset/liability net-worth tests
- archive/disposal states
- device verification

---

# Sprint 6 — Home Dashboard

## Build
- net worth
- summaries
- recent activity
- quick add
- upcoming
- tax readiness placeholder backed by real repository

## Gate
- totals reconcile with source modules
- empty-state correctness
- privacy masking
- performance with seeded dataset

---

# Sprint 7 — Vault and Official Records

## Build
- import
- encrypted storage
- metadata
- tags
- links
- official records
- preview
- expiry reminders foundation

## Gate
- encrypted file unreadable as plaintext
- import/open/delete
- duplicate hash detection
- invalid/corrupt file handling
- dependency warning on delete
- document survives app restart

---

# Sprint 8 — Continuous Tax Capture Core

## Build
- tax years
- tax items
- tax relevance
- event taxonomy
- source linkage
- tax inbox
- review states

## Gate
- event added from Money appears once in Tax
- independent Tax item works
- changing source updates/rebuilds mapping safely
- exclusion preserves source record
- tax-year assignment tests

---

# Sprint 9 — Versioned Tax Rules Engine

## Build
- ruleset JSON schema
- parser
- validator
- classifier
- mapping engine
- ruleset version history
- deterministic calculation framework

## Gate
- invalid ruleset rejected
- same input/version same result
- historic item remains associated with correct version
- ambiguous mappings create issue
- user override honored

---

# Sprint 10 — Annual Tax Workspace

## Build
- annual sections
- issue center
- evidence completeness
- duplicate candidates
- draft generation
- source drill-down

## Gate
- generated totals equal source calculations
- every draft line traceable
- unresolved critical issues visible
- regeneration versioned
- no source mutation during generation

---

# Sprint 11 — Wealth Reconciliation

## Build
- opening snapshot
- closing snapshot
- inflow/outflow grouping
- unexplained difference
- drill-down

## Gate
- known synthetic scenarios reconcile to zero
- deliberately missing asset creates expected difference
- user adjustment requires reason
- calculations reproducible

---

# Sprint 12 — Reports

## Build
- report domain
- in-app previews
- PDF
- CSV where applicable
- tax preparation report
- net-worth report

## Gate
- numbers match UI/source data
- selected date range honored
- long report pagination
- PDF opens on device
- no clipped values
- privacy disclaimer where applicable

---

# Sprint 13 — Backup / Restore / Export / Delete

## Build
- encrypted backup package
- integrity manifest
- transactional restore
- full JSON export
- CSV export
- delete all

## Gate
- backup → uninstall/clear → restore equivalence
- wrong password fails
- tampered backup fails
- failed restore leaves original state intact
- document hashes match
- delete-all leaves no app user records

---

# Sprint 14 — Calendar and Notifications

## Build
- due dates
- reminders
- document expiry
- receivable
- monthly review
- tax review

## Gate
- notification permission behavior
- scheduled reminder fires in test conditions
- edit/cancel updates schedule
- no duplicate notifications

---

# Sprint 15 — UX Hardening

## Build
- all empty/error/loading states
- accessibility
- rotation
- process recreation
- large datasets
- keyboard behavior
- privacy masking everywhere

## Gate
- core workflows <= agreed interaction burden
- TalkBack smoke test
- font scale test
- low-memory/process-death test
- no obvious layout overflow

---

# Sprint 16 — Release Hardening

## Build
- release config
- app icon
- versioning
- privacy policy content inputs
- Play declarations
- backup rules
- ProGuard/R8 review
- crash-free manual smoke

## Gate
- `clean bundleRelease`
- lint
- unit tests
- connected tests
- release APK/AAB verification
- no debug flags/secrets
- target API compliance
- physical-device final verification

---

# Definition of Done

The project is **not complete** because it compiles.

Complete means:
- all required features implemented;
- all global acceptance tests pass;
- device verification evidence exists;
- backup/restore tested;
- security controls tested;
- tax annual draft works from year-long captured data;
- no unresolved critical/high defects.
