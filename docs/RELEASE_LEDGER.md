# Release ledger

Chronological record of every signed release bundle produced for Google Play, its scope, and the
"What's new" copy used for that release. Append a new entry each time a release bundle is
assembled — do not edit prior entries after they've been uploaded to a Play Console track.

Artifact SHA-256 hashes are recorded here as the tamper-evident record of exactly what was
uploaded; see `docs/RELEASE_SIGNING.md` for the signing key identity itself.

---

## v1.1.0 (versionCode 4) — 2026-08-29 Sprint 24 release

**Track:** Production / Closed testing (ready for release upload)

**Commit:** `aa3632752063a24edafa18f7d98fdfcce4b3c3e0` + device qualification fixes
**Scope:** Money navigation, canonical account/activity UI, utility-payment unified-ledger bridge,
Room v14 migration, attachment lifecycle, PIN management and screenshot protection.

**Artifacts:**

| File | SHA-256 |
| --- | --- |
| `app-release.aab` | `d3d8c28cbffa7b14acc7838c47ee73e61ce03fc6779a8fac7e1d101ecb5c2054` |
| `app-release.apk` | `919dce3f9fcaecbff45ab83c089bea2025aff0f6ac58740559cd879063d26077` |
| `app-debug.apk` | `85e33379204b5e2c308f7c47664418ec682d7aec67d4600803ee2235f36724a7` |

**Verification:** Host `test`, `lint`, `assembleDebug`, `assembleDebugAndroidTest`, `bundleRelease`, and
`assembleRelease` all passed. Full connected/API 36 device qualification (68/68 tests) executed and
passed on hardware-accelerated emulator. Release bundle signed with production release key. See
`docs/verification/SPRINT_24_GATE.md` and `docs/verification/SPRINT_24_DEVICE_RESULTS.md`.

---

## v1.0.3 (versionCode 3) — 2026-08-24

**Track:** Closed testing (pending upload)
**Scope:** Sprints 17–23 — the personal-finance-diary focus shift (see `CLAUDE.md` and
`docs/01_PRODUCT_VISION_AND_RULES.md` for the framing this release completes). First release built
after the product reframing away from tax-capture-first.

**Artifacts:**
| File | SHA-256 |
| --- | --- |
| `app-release.aab` | `282c56e36d205f3e0aec811608db0481db689abe6e7dc094e93153309b3ad05d` |
| `debug-symbols.zip` | `b26d78b14eb873078607c42f108f7aa0a47788606e969c75109c4e40e74e5b72` |

**Verification:** `./gradlew test lint assembleDebugAndroidTest` green; full
`connectedDebugAndroidTest` — 67/67 passed on `Android_26_Test(AVD)`; bundle signature verified via
`jarsigner -verify` (see `docs/RELEASE_SIGNING.md`). Full per-sprint gate evidence in
`docs/verification/SPRINT_17_GATE.md` through `SPRINT_23_GATE.md` / `SPRINT_23_MONEY_GATE.md`.

**Play Store "What's new" (500-char limit, as submitted):**
```
What's new:
• Faster income/expense logging with category suggestions & number pad
• Track income sources with a breakdown view
• Loans & receivables: due dates, interest rate, installment tracking, and reminders
• Bills: mark paid in one tap, organized by category
• Goals: progress bars and one-tap contributions
• Wealth screen reorganized into tabs (Assets, Investments, Liabilities, Receivables, Goals)
• Money screen: filter activity by type and date
• Bug fixes and stability improvements
```

**Full changelog (internal, not for the Play listing):**
- Money-tab FAB opens the income/expense dialog directly instead of the account dialog; numeric
  keyboard on every amount field; category suggestion chips; Home Quick Add opens dialogs in place.
- Goals gain a target date, goal type, progress bar, and one-tap "Contribute" action.
- Liabilities gain due date, lender, loan type, interest rate, and installment amount, each wired
  to the reminder engine; Wealth screen restructured into Assets / Investments / Liabilities /
  Receivables / Goals tabs.
- Recurring bills gain a fixed category taxonomy and a one-tap "Mark paid" action that records the
  event immediately instead of waiting for the periodic worker.
- New income sources feature: `IncomeSourceEntity`, optional link from a financial event, inline
  creation from the income dialog, income-by-source breakdown on the Money screen.
- Home screen reordered to match the documented information-architecture hierarchy (Net Worth →
  Quick add → Tax-year readiness → Upcoming obligations → Money summary → Wealth summary → Goals →
  Recent activity); Money screen gains an activity type filter and optional date-range filter.

---

## v1.0.2 (versionCode 2) — pre-existing, no dedicated release notes recorded

Base build that predates the Sprint 17–23 work above (versionCode/versionName bump was already
present, uncommitted, in the working tree when this session started; committed as `a21235e` without
being authored by this session). No signed bundle or "What's new" copy for this version was
produced or recorded here — first ledger entry is v1.0.3.
