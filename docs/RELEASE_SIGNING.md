# Release signing

Real release signing key generated 2026-08-23, replacing the debug-signed internal QA config
(`docs/BLOCKERS.md`'s "Permanent release signing key is not provided" row is now resolved).

## Key identity

| Field | Value |
| --- | --- |
| Keystore | `keystore/vexel-release.jks` (gitignored, present only in this working tree) |
| Type | PKCS12 |
| Alias | `vexel-release` |
| Algorithm | RSA 4096, SHA384withRSA |
| Valid | 2026-08-23 → 2056-08-15 (30 years — well past Google Play's Oct 2033 requirement) |
| SHA-1 | `5B:0B:13:B4:C6:4B:D6:78:BA:87:0B:E2:1D:64:5E:BC:A0:04:06:26` |
| SHA-256 | `FE:C4:34:4F:7B:BA:46:C4:1D:D2:ED:F3:F7:5E:2B:12:33:6B:95:45:C9:C7:39:C3:05:AE:66:AD:EF:43:F3:9B` |

The SHA-1/SHA-256 fingerprints above are what you enter into Google Play Console when enrolling
this key (as the upload key, if using Play App Signing — recommended, see below).

## Where the secrets live

- `keystore/vexel-release.jks` — the key material itself.
- `keystore.properties` (repo root) — store/key passwords + alias, read by `app/build.gradle.kts`.
- Both are gitignored (`.gitignore`: `*.jks`, `*.keystore`, `keystore.properties`) and were never
  committed. `keystore.properties.example` (committed) documents the expected shape with
  placeholder values for anyone provisioning a fresh checkout.
- The passwords were shown once, in the chat that generated them, for the user to store in a
  password manager. They are not recorded anywhere else in this repo or its history.

## Build wiring

`app/build.gradle.kts` reads `keystore.properties` if present and wires a real `release`
`signingConfig`; if the file is absent (e.g. a fresh clone before the keystore is provisioned),
the `release` build type falls back to debug signing so the project still builds. No other build
behavior changed.

## Verified this session

- `./gradlew :app:signingReport` — confirms the `release` variant resolves to
  `keystore/vexel-release.jks` / alias `vexel-release` (not the debug key).
- `./gradlew bundleRelease assembleRelease` — BUILD SUCCESSFUL.
- `apksigner verify --print-certs app-release.apk` — verifies (APK Signature Scheme v2), signer
  cert SHA-256 matches the keystore above.
- `app-release.aab` — `apksigner` can't verify a bundle directly (not an APK), so verified via
  `jarsigner -verify -certs`, which confirms the signature and reports the same certificate expiry
  (2056-08-15).

## Backup requirement — read before losing this file

**If `keystore/vexel-release.jks` and its passwords are lost, this specific key can never sign an
update to `pk.vexel.financepassport` again.** Without Play App Signing enrollment, that means the
app could never be updated post-launch under the same package — only a new listing.

Action items (not yet done — external to this repo):
1. Back up `keystore/vexel-release.jks` and the two passwords from `keystore.properties`
   (they're identical, PKCS12 requires matching store/key passwords) to at least one secure,
   offline location (password manager + encrypted archive), independent of this machine/repo.
2. Enroll in **Google Play App Signing** at first Play Console upload — Google then holds the
   app signing key and this key becomes only the "upload key," which Google can help recover/rotate
   if lost. Strongly recommended over relying solely on this local key long-term.

## Still open (tracked in `docs/BLOCKERS.md`)

Branding/icon assets and the public privacy-policy URL — signing is no longer a blocker.
