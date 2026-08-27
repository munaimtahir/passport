# Recurrence and Date Rules

This document specifies how recurring bills generate occurrences and update their status.

## 1. Generation Engine
- Generated occurrences are deterministic and idempotent based on `(profileId, billingYear, billingMonth)`.
- Occurrences are generated starting from the configured `recurrenceStartMonth` up to the current calendar month.
- When generating for a month:
  - Expected issue date is calculated using `issueDayAnchor`. If a month has fewer days than `issueDayAnchor`, the last day of the month is used.
  - Expected due date is calculated using `dueDayAnchor`. If `dueDayAnchor < issueDayAnchor`, the due date belongs to the *following* calendar month.
  - If a month has fewer days than `dueDayAnchor`, the last day of the month is used.

## 2. Status Transitions
Status is calculated based on current date, issue/due dates, payment state, and explicit skip flag:
1. **Paid:** A valid payment record exists.
2. **Skipped:** User marked the occurrence as skipped.
3. **Expected:** Current date is before the expected issue date.
4. **Overdue:** Current date is after the expected due date (and not paid/skipped).
5. **Due soon:** Current date is within the due-soon window (default 5 days) before the expected due date (and not paid/skipped).
6. **Pending:** Current date is on or after the expected issue date, but not paid/skipped, and not due soon or overdue.
