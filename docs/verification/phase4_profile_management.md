# Phase 4 Verification: Utility Profile Management

## 1. UI Forms & Fields
- Added `AddBillDialog` supporting both profile addition and editing:
  - Form validation: Name and reference/consumer number are mandatory. Issue and due anchors must be integers in 1..31.
  - Recurrence start month validation matching the "YYYY-MM" format.
  - Optional inputs: Provider/Company, Custom category name for "Other" category, Location label, custom location text, connection/telephone identifier, and notes.
  - Duplicate Warning: Proactively checks for a matching connection (same category, provider, reference number) and requests confirmation before saving to prevent silent merges.
  - Remembers form states correctly on rotation and process recreation via Compose `rememberSaveable`.

## 2. Profile List UI
- Implemented `BillsScreen` listing utility profiles grouped by their categories:
  - Supports searching by name, reference number, or provider.
  - Supports category filters: All, Electricity, Gas, Telephone, Other.
  - Supports active/archived state filters.
  - Displays Category Icons, name, provider, location label, masked reference number, next due date, and current-month status.

## 3. Profile Details & History
- Implemented `UtilityProfileDetailsDialog`:
  - Shows complete profile details and schedule parameters.
  - Displays aggregated stats: Total bill count, Paid count, Pending count, Overdue count, Total Paid amount, and Latest payment date.
  - Lists historical billing occurrences.
  - Allows actions: Edit Profile, Archive Profile, Reactivate Profile (asking for reactivation month), Add Missing Month, Delete Profile (with confirmation).
- Implemented `AddHistoricalOccurrenceDialog` that asks for Year, Month, and expected amount, checks uniqueness, and manually seeds the occurrence.

## 4. Host-Side Verification Output
- `./gradlew test` -> BUILD SUCCESSFUL (exited with code 0)
- `./gradlew lint` -> BUILD SUCCESSFUL (exited with code 0)
