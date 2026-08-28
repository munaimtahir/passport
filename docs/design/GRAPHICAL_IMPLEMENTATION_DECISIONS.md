# Vexel Finance Passport — Graphical Implementation Decisions

## Product Vision & Visual Strategy
This document records the architectural and UI design decisions made during the autonomous graphical implementation sprint for **Vexel Finance Passport Design Language 2.0 (Quiet Financial Memory + Financial Pulse)**.

## 1. Product Thesis: Quiet Financial Memory + Financial Pulse
The interface is structured to function as a private personal-finance system:
- **What matters now**: Financial Pulse attention queue on Home prioritizing overdue bills, upcoming due dates, and pending obligations.
- **Living Bills**: Recurring bills evolve visually through life-cycle states (Expected -> Received -> Due Soon -> Overdue -> Paid) rather than static list rows.
- **Bill Rhythm**: A non-chart multi-month strip (`Apr  May  Jun  Jul  Aug  Sep` with status indicators and TalkBack descriptions) providing immediate payment consistency feedback.
- **Vexel Capture Tray**: Ergonomic central modal bottom sheet facilitating rapid Expense, Income, Transfer, and Bill entry.
- **Financial Memory Timeline**: History organized chronologically by day/month with muted editorial typography and search filtering.

## 2. Tokenized Theme System (`ui/theme/`)
- `Color.kt`: Muted Editorial palette featuring Deep Forest Emerald (`#1B3B36`), Warm Muted Ochre (`#8C5D3B`), Soft Architectural Background (`#F6F7F5`), Deep Ink Text (`#171D1B`), and semantic status tokens for Paid, Due Soon, Overdue, Pending, and Skipped.
- `Type.kt`: Quiet Editorial scale with Serif headlines, structural sans titles, and tabular monospace digits.
- `Shape.kt`: Documented radii (4dp extraSmall, 8dp small, 14dp medium, 20dp large, 28dp extraLarge).
- Light and Dark modes implemented as first-class, high-contrast themes.

## 3. Navigation Structure
- Primary destinations: **Home | Money | Bills | History**.
- Central **Vexel Capture Tray** floating action button launching quick entry.
- TopAppBar actions: Privacy mode toggle (amount masking `PKR ••••••`) and Settings/More menu.

## 4. Preservation of Invariants
- Minor unit PKR integer persistence (`PkrMoneyInput`).
- Room DB Schema version 8 and database integrity.
- Paired signing transactions for Transfers (`Transfer != Income/Expense`).
- Local AES-GCM document vault security.
- Security gate with PIN/biometric authentication and `FLAG_SECURE`.
