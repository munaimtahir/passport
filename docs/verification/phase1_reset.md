# Phase 1 Verification: Product Shell and Visible-Scope Reset

## 1. Product Shell & Navigation Verification
- Navigation destinations changed to:
  - **Home** (index 0)
  - **Bills** (index 1)
  - **History** (index 2)
- Scaffold navigation logic completely replaced inside `PassportApp.kt`.
- All legacy workspaces (Money, Wealth, Tax, Vault) are no longer routable or visible in normal operation.
- Privacy toggle and "Settings" options are wired up on the top app bar.
- Onboarding flow simplified to Welcome -> Privacy explanation -> Optional PIN setup -> Start empty.
- Onboarding PIN setup leverages the existing `PinStore` to optionally secure local records immediately. Skip option allows bypassing PIN setup, which automatically unlocks `SecurityGate` on startup.

## 2. Host-Side Verification Output
- `./gradlew test` -> BUILD SUCCESSFUL (exited with code 0)
- `./gradlew lint` -> BUILD SUCCESSFUL (exited with code 0)

All compilation, test suite, and static analysis checks pass.
