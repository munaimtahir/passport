# Sprint 23 Gate — Home visual rebalancing (Home half)

Scope of this section: `HomeScreen` composable in `app/src/main/java/pk/vexel/financepassport/ui/PassportApp.kt`
re-ordered to match the documented hierarchy in `docs/03_INFORMATION_ARCHITECTURE_AND_UX.md`
("Home hierarchy" section). The Money-screen half of Sprint 23 (Activity filter/date-range) is a
separate concurrent workstream and is gated independently.

## Before

Net Worth → Quick add → Tax-year readiness → Income vs. expense this period → Upcoming obligations
→ Recent activity

The "Income vs. expense this period" card sat directly under Net Worth, ahead of Tax-year readiness
and Upcoming obligations, which did not match the documented order.

## After

Matches the documented order exactly:

1. Net Worth
2. Quick add
3. Tax-year readiness
4. Upcoming obligations
5. **Money summary** (re-positioned "Income vs. expense this period" card)
6. **Wealth summary** (new — asset/liability/investment counts and totals, sourced from the
   `assets`, `liabilities`, `investments` `StateFlow`s already exposed by `MainViewModel`)
7. **Goals** (new — shown only when `goalProgress` is non-empty; reuses the `LinearProgressIndicator`
   pattern already used on the Wealth screen's Goals tab)
8. Recent activity

## Build

```
./gradlew :app:compileDebugKotlin
```
Result: BUILD SUCCESSFUL (2 pre-existing `menuAnchor()` deprecation warnings, unrelated to this change).

## Host gate

```
./gradlew test lint
```
Result: BUILD SUCCESSFUL — 71 actionable tasks (52 executed, 19 up-to-date). No lint or unit test
regressions.

## Device verification

Installed to `Android_26_Test(AVD) - 8.0.0` via `./gradlew :app:installDebug`, walked onboarding →
PIN creation → Home on a fresh install, and screenshotted the full scroll of the Home screen.
Confirmed visually:
- Above-the-fold order (Net Worth, Quick add, Tax-year readiness, Upcoming obligations) unchanged.
- Below-the-fold order is now Money summary → Wealth summary → Recent activity, with the Goals
  section correctly absent (no goals exist on a fresh install, and the section is conditional on
  `goalProgress.isNotEmpty()`).

## Connected suite

```
./gradlew :app:connectedDebugAndroidTest
```
Result: PENDING — held back to avoid running instrumentation concurrently with the Sprint 23b
(Money screen) fork's own `connectedDebugAndroidTest` run on the same single emulator
(`emulator-5554`). Will run and record below once the device is free.
