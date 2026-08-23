# Blockers

## External blockers

| Date | Sprint | Decision/blocker | Temporary assumption | Affects | Required before |
| --- | --- | --- | --- | --- | --- |
| 2026-08-14 | 0 | Original repository history was unavailable | Preserve the working tree and use a new local Git history | Commit provenance only | Not required for local QA |
| ~~2026-08-14~~ 2026-08-23 | 16 | ~~Permanent release signing key is not provided~~ **RESOLVED**: real `vexel-release` keystore generated, wired into `app/build.gradle.kts`, `bundleRelease`/`assembleRelease` verified signed with it | See `docs/RELEASE_SIGNING.md` for fingerprints, verification evidence, and the required offline-backup + Play App Signing enrollment follow-up | Production publishing | N/A — closed |
| ~~2026-08-14~~ 2026-08-23 | 16 | ~~Final branding/icon and public privacy-policy URL are not provided~~ **PARTIALLY RESOLVED**: real Play Store icon/feature graphic provided and wired as the app's adaptive launcher icon; privacy policy drafted (`docs/PRIVACY_POLICY.html`). **Still open**: a public URL for the policy — user is hosting it externally, URL not yet available for the Play listing | Use the drafted policy content until a public URL exists | Store publication | Production release |
| 2026-08-17 | 16 | No physical Android device is exposed through ADB; only API 26 emulator was connected | Record emulator evidence and leave physical-device gate open | Final internal QA matrix | Before internal release verdict |

No blocker currently prevents local implementation or internal QA.
