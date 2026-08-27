# Blockers

## External blockers

| Date | Sprint | Decision/blocker | Temporary assumption | Affects | Required before |
| --- | --- | --- | --- | --- | --- |
| 2026-08-14 | 0 | Original repository history was unavailable | Preserve the working tree and use a new local Git history | Commit provenance only | Not required for local QA |
| ~~2026-08-14~~ 2026-08-23 | 16 | ~~Permanent release signing key is not provided~~ **RESOLVED**: real `vexel-release` keystore generated, wired into `app/build.gradle.kts`, `bundleRelease`/`assembleRelease` verified signed with it | See `docs/RELEASE_SIGNING.md` for fingerprints, verification evidence, and the required offline-backup + Play App Signing enrollment follow-up | Production publishing | N/A — closed |
| ~~2026-08-14~~ 2026-08-23 | 16 | ~~Final branding/icon and public privacy-policy URL are not provided~~ **PARTIALLY RESOLVED**: real Play Store icon/feature graphic provided and wired as the app's adaptive launcher icon; privacy policy drafted (`docs/PRIVACY_POLICY.html`). **Still open**: a public URL for the policy — user is hosting it externally, URL not yet available for the Play listing | Use the drafted policy content until a public URL exists | Store publication | Production release |
| ~~2026-08-17~~ 2026-08-24 | 16 | ~~No physical Android device is exposed through ADB; only API 26 emulator was connected~~ **RESOLVED BY EXPLICIT USER DIRECTION**: no physical device is available in this environment; the user explicitly directed treating the attached emulators (`Android_26_Test`/API 26, `Android_16_Test`/API 36, `Android_15_Test`/API 35) as the qualification device and closing this gate on emulator evidence alone | Full connected-suite (43-50/43-50 depending on pass, across API 26/36), manual E2E walkthrough, UI-driven backup/restore, device-lifecycle (relock/delete-all/rotation), notification delivery, and accessibility/adaptive spot checks (API 35) all recorded in `docs/verification/REMEDIATION_MASTER_STATUS.md` — see the "Phase 10 detail — 2026-08-24 device-qualification completion" section | Final internal QA matrix | N/A — closed on emulator evidence per user direction; a genuine physical-device pass remains open if one becomes available later |

No blocker currently prevents local implementation or internal QA.
