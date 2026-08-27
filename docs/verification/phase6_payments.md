# Phase 6 Verification: Monthly Occurrence Details and Payments

## 1. Occurrence Details UI
- Implemented `MonthlyOccurrenceDetailsDialog` displaying:
  - Utility profile details (name, category, reference number).
  - Billing period and status.
  - Expected issue/due dates.
  - Interactive inputs to set the actual issue date, actual due date, and expected/actual bill amount.
  - Direct integration with `DateField` and `AmountField` supporting grouping without decimal entry.

## 2. Recording Payments
- Provides a detailed "Record Payment" form when tapping "Mark Paid":
  - Inputs for amount paid (defaulting to the occurrence amount if set).
  - Date of payment (defaulting to today's date).
  - Payment modes: Cash, Bank Transfer, Card, Mobile Wallet, Other.
  - Optional inputs for bank name, transaction reference, and custom notes.
- Validates payment parameters (requires a non-zero, positive amount).
- Inserts a `PaymentRecordEntity` and updates the occurrence status to "Paid".
- Allows deleting a recorded payment, reverting the occurrence status back to its calculated unpaid state.

## 3. Recording Skips
- Allows marking an occurrence as "Skipped" (prompting the user for skip notes/reason).
- Allows reverting a skip (returning the occurrence to its default pending state).

## 4. Host-Side Verification Output
- `./gradlew test` -> BUILD SUCCESSFUL (exited with code 0)
- `./gradlew lint` -> BUILD SUCCESSFUL (exited with code 0)
