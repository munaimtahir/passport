# Blockers

## External blockers

| Date | Sprint | Decision/blocker | Temporary assumption | Affects | Required before |
| --- | --- | --- | --- | --- | --- |
| 2026-08-14 | 0 | Original repository history was unavailable | Preserve the working tree and use a new local Git history | Commit provenance only | Not required for local QA |
| 2026-08-14 | 16 | Permanent release signing key is not provided | Use debug and locally verifiable unsigned/configured release artifacts | Production publishing | Production release |
| 2026-08-14 | 16 | Final branding/icon and public privacy-policy URL are not provided | Use internal-safe `ic_passport` branding and draft policy content | Store publication | Production release |
| 2026-08-17 | 16 | No physical Android device is exposed through ADB; only API 26 emulator was connected | Record emulator evidence and leave physical-device gate open | Final internal QA matrix | Before internal release verdict |

No blocker currently prevents local implementation or internal QA.
