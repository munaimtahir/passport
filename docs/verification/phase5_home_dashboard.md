# Phase 5 Verification: Home Dashboard and Pending Obligations

## 1. Home Dashboard Reset
- Replaced the placeholder `HomeScreen` with a complete Monthly Utility Bill Tracker dashboard.
- Displays aggregate status cards:
  - Unpaid obligations count.
  - Overdue bills count (highlighted in red if greater than zero).
  - Paid bills count in the current calendar month.
  - Total paid amount in the current month (with formatting).
- Automatically calculates metrics and paid totals in a lifecycle-aware Compose `LaunchedEffect`.

## 2. Priority Pending Obligations List
- Filters and displays unpaid occurrences whose expected issue date has arrived or passed.
- Implemented deterministic sorting logic:
  1. Overdue bills first.
  2. Due soon bills second.
  3. Pending bills third.
  4. Sorted by expected due date.
- Renders detailed items with the bill name, billing month, expected due date, reference number, status chip, and amount (or TBD).
- Selecting an obligation item opens the profile's detail dialog so the user can easily review the connection and perform payment actions.
- Includes a user-friendly "Add Your First Bill" Call to Action (CTA) pointing to the Bills tab if no bills are registered.

## 3. Host-Side Verification Output
- `./gradlew test` -> BUILD SUCCESSFUL (exited with code 0)
- `./gradlew lint` -> BUILD SUCCESSFUL (exited with code 0)
