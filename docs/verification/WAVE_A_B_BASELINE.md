# WAVE A+B FORENSIC BASELINE

## Repository State
* **Current Branch**: `main`
* **Current Commit**: `e25a25e`
* **Worktree State**: Clean
* **Application Version**: Follows repository baseline, target SDK 36.

## Build and Schema State
* **Room Schema Version**: 14 (as per `AppDatabase.kt`)
* **Gradle/AGP/Kotlin**: Baseline builds successfully with JDK 21. `assembleDebug` completes.
* **Test Modules**: Present in `app/src/test` and `app/src/androidTest`.
* **Important Pre-existing Findings**:
    * Room schema version is exactly 14.
    * Database has models `AccountEntity`, `FinancialEventEntity`, `UtilityBillProfileEntity`, `MonthlyBillOccurrenceEntity`, `PaymentRecordEntity`, `BillAttachmentEntity`.
    * Accounts already have `openingBalanceMinor` and `openingBalanceDateEpochDay`.
    * FinancialEvent has `accountId`, `category`, and `incomeSourceId`.
    * PaymentRecordEntity has an optional `financialEventId` indicating partial connection to financial spine already.

## Device State
* **Connected Devices/Emulators**: None currently connected (`adb devices` list is empty).

## Next Steps
Proceeding to Phase 1: Forensic Financial Model Discovery to map existing models against required domain capabilities.
