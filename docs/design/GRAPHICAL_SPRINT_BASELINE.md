# Vexel Finance Passport — Graphical Sprint Baseline (Phase 0)

## Executive Summary
This baseline audit establishes the technical and visual starting point for the **Vexel Finance Passport Design Language 2.0 (Quiet Financial Memory + Financial Pulse)** implementation sprint.

- **Repository**: `/home/munaim/srv/apps/passport`
- **Application ID**: `pk.vexel.financepassport`
- **SDK Targets**: minSdk 26, compileSdk 36, targetSdk 36
- **Build & Test Baseline**: `BUILD SUCCESSFUL`, 50 unit tests passing, clean compile.

## Current Screen & Route Inventory
1. **Home (`HomeScreen`)**: Currently utility-focused metrics dashboard showing unpaid count, overdue count, paid this month, total paid this month, and pending obligations.
2. **Bills (`BillsScreen`)**: List of utility connections categorized by service type (Electricity, Gas, Mobile, etc.) with search and filter chips.
3. **Money (`MoneyScreen`)**: Account list, income/expense/transfer actions, recurring drafts, activity log with category & type filters.
4. **History (`HistoryScreen`)**: Global occurrence list with status, category, year, and payment mode dropdown filters.
5. **Vault / Evidence (`VaultScreen`)**: Document list with encryption metadata, search, preview modal, PDF rendering, link, and delete capabilities.
6. **Tax & Records (`TaxScreen`)**: Tax items, official records, tax year switcher (PK-YYYY), readiness score, annual draft generator, and wealth snapshot.
7. **Wealth & Position (`AddWealthDialog`, `AccountCard`)**: Assets, liabilities, investment holdings, receivables, and financial goals.
8. **Settings & Admin (`MoreDialog`)**: App PIN management, encrypted backup creation/restoration, export tools, and data wiping.

## Visual & Graphical Deficiencies Identified
- **Material 3 Generic Look**: Default green `0xFF315C52` palette with basic `Card` and `OutlinedTextField` styling; lacks editorial visual character.
- **Icon-Only / Ambiguous Controls**: Icon buttons without unambiguous labels or unified styling across different screens.
- **Inconsistent Surface Hierarchy**: Cards within cards, inconsistent rounded corner radii, raw text padding, and lack of visual elevation levels.
- **Navigation Ambiguity**: Standard 4-item bottom bar (`Home`, `Bills`, `Money`, `History`) without a central capture action tray.
- **Amount & Number Presentation**: Basic text rendering for PKR figures without signature typography scaling, tabular alignment, or quiet memory cards.
- **Bills Presentation**: Basic card format instead of dynamic **Living Bill Cards** and multi-month **Bill Rhythm Strips**.

## Safe Functional Invariants to Preserve
- Exact integer minor-unit money handling (`PkrMoneyInput`).
- Database schema version 8 with Room DAOs and migrations.
- Transfer paired signing transaction semantics (`Transfer != Income/Expense`).
- Utility monthly occurrence generation, payments, and attachment linkages.
- Offline-first encrypted document vault (`AES-GCM`) and Keystore security.
- Security gate with PIN/biometric authentication and `FLAG_SECURE`.

## Phase 0 Quality Gate
- [x] Repository status verified clean.
- [x] Baseline unit tests executed (50 passed).
- [x] Architectural and UI inventory completed.
- [x] Baseline visual documentation generated.
