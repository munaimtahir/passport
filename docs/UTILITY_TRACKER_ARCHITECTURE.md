# Utility Tracker Architecture

This document describes the offline-first architecture of the Monthly Utility Bill Tracker.

## 1. Architectural Layers
- **UI Layer (Jetpack Compose):** Handles layout, user input, state rendering, and navigation. Uses `MainViewModel` as the single ViewModel.
- **Repository Layer (`FinanceRepository` & `UtilityRepository`):** Coordinates data operations, manages database writes and reads, schedules notifications, and triggers recurrence checking on app startup.
- **Data Layer (Room Database):** Stores local records for all entities. Encryption is handled transparently where needed.

## 2. Startup Synchronization Flow
1. User opens the application.
2. System loads all active utility profiles.
3. System triggers deterministic generation of occurrences for any missing months up to the current month.
4. Active notification tasks are scheduled for expected/pending items.
