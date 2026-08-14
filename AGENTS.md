# Repository Guidelines

## Project Structure & Module Organization

This repository is a documentation-led Android scaffold for Vexel Finance Passport. Android source will live under `app/`; the intended structure also includes `core/` for shared model, database, security, files, tax rules, UI, and testing code, and `feature/` for modules such as `money`, `wealth`, `tax`, `vault`, `reports`, and `backup`. Product, architecture, security, and acceptance documentation is in `docs/`. Keep verification evidence in `docs/verification/` when introduced. `app/src/main/`, `app/src/test/`, and `app/src/androidTest/` are reserved for production, unit, and instrumentation code.

## Build, Test, and Development Commands

No Gradle project is checked in yet, so build commands are not currently runnable. Once the Android project is implemented, use:

```bash
./gradlew clean assembleDebug       # Build the debug APK
./gradlew test                      # Run JVM unit tests
./gradlew connectedCheck            # Run instrumentation/device tests
./gradlew lint                      # Run Android lint
```

For device verification, use `adb devices`, install `app/build/outputs/apk/debug/app-debug.apk`, then launch package `pk.vexel.financepassport`. Follow the sprint gates in `docs/10_DEVELOPMENT_SPRINTS_AND_QUALITY_GATES.md`.

## Coding Style & Naming Conventions

Use Kotlin and Jetpack Compose conventions: four-space indentation, `PascalCase` types/composables, `camelCase` functions and properties, and `UPPER_SNAKE_CASE` constants. Keep feature and core package boundaries explicit. Prefer immutable state, typed money/date values, repositories over direct data access, and versioned tax-rule configuration. Use the project formatter and lint configuration; do not introduce credentials, financial data, or verbose sensitive logging.

## Testing Guidelines

Maintain the test pyramid: unit tests for calculations and tax mapping, database tests for DAOs/migrations, instrumentation tests for Android integrations, Compose tests for workflows, and device tests for end-to-end flows. Name tests for behavior, for example `transferDoesNotChangeIncomeTotal`. Use the synthetic `DemoUserScenario` and preserve expected totals as fixtures. Every sprint must compile, pass its quality gate, and record evidence under `docs/verification/`.

## Commit & Pull Request Guidelines

Existing history uses short, imperative-style subjects such as `Add minimal Android scaffold directory tree`; follow that style and keep each commit focused. Pull requests should explain the user or architecture impact, list verification commands and results, link relevant documentation or issues, and include screenshots or ADB/device evidence for UI changes. Call out schema migrations, security implications, and any unresolved blocker explicitly.

## Security & Configuration

The app is offline-first, local by default, and must not store bank or FBR credentials. Protect sensitive data with Android Keystore-backed encryption, PIN/biometric lock, app-private document storage, and controlled encrypted backups. Never commit secrets or real financial records; use synthetic fixtures and redact logs.
