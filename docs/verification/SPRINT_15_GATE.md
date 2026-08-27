# Sprint 15 Gate — UX Hardening

Date: 2026-08-14
Status: IN PROGRESS

- Five required primary destinations are now Home, Money, Wealth, Tax & Records, and Vault.
- A separate More action provides destructive data controls.
- Vault import uses SAF and shows encrypted-file metadata.
- App relocks on lifecycle `ON_STOP`; failed PIN attempts use exponential backoff.

Compose launch and primary-destination navigation tests now pass on API 26 and API 36. A manual API 36 smoke at font scale 1.3 and forced rotation preserved `MainActivity` with no fatal exception. Pending: screenshot protection review, TalkBack smoke, loading/error states, visual review, and large-data performance verification.
