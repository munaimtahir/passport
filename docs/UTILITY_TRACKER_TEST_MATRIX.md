# Utility Tracker Test Matrix

This matrix defines the tests checking correctness of recurrence, data, and UI logic.

## 1. Recurrence Unit Tests
- `testCurrentMonthGeneration`: Verifies deterministic current month occurrence creation.
- `testNextMonthStart`: Verifies occurrence generation starting in future months.
- `testMissedMonthsReconciliation`: Verifies generation when app hasn't opened in months.
- `testLeapYearHandling`: Verifies issue/due dates clamp to Feb 29 in leap years and Feb 28 in non-leap years.
- `testDueDayBeforeIssueDay`: Verifies due date rolls into next calendar month if due day is numerically smaller than issue day.

## 2. Database & Safe Migration Tests
- `testDatabaseMigration`: Verifies migration from version 12 to 13 is non-destructive.
- `testUniquenessConstraint`: Verifies profile + year + month uniqueness constraint.
- `testCascadeDelete`: Verifies deleting a profile cascade-deletes occurrences, payments, and attachments.
- `testPaymentUniqueness`: Verifies only one payment can exist per occurrence.

## 3. UI Flow Compose Tests
- `testOnboardingFlow`: Verifies only utility-tracker welcome and setup screens are shown.
- `testHomeNavigation`: Verifies only Home, Bills, and History are reachable.
- `testPaymentFlow`: Verifies recording cash vs online-bank payments updates dashboard state immediately.
