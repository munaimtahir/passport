# Release and Google Play Preparation

## Android target

For a new/update submission around the current project date, configure **targetSdk 36** and compile against API 36 unless Google Play policy changes before release.

## Release checklist

- Signed AAB
- Version code/name
- Target API verified
- minSdk verified
- app icon
- screenshots
- feature graphic
- privacy policy
- Data safety answers reviewed against actual build
- permissions reviewed
- content rating
- financial-feature wording reviewed
- no misleading “official FBR” branding
- no claim of guaranteed tax correctness
- no unused SDKs
- no ads/tracking if product promise says none

## Suggested Play description boundary

Describe the app as:
- personal finance organizer
- offline financial record
- document vault
- tax preparation organizer

Avoid:
- “official tax filing”
- “FBR approved”
- “guaranteed tax return”
unless that status is actually obtained and documented.

## Permissions minimization

Expected:
- biometric-related platform use
- notifications (runtime permission where required)
- user-selected document access via SAF

Avoid:
- contacts
- SMS
- call logs
- broad file/storage access
- location
unless a future feature has a compelling user-facing purpose.

## Privacy policy must explain

- local data storage
- optional exported/backup files
- permissions
- document handling
- no credential collection
- no sale of financial data
- deletion/export
- any future network feature separately

## Pre-release security check

- dependency audit
- secret scan
- production logging review
- backup rules review
- exported components review
- deep-link auth review
- WebView absence or hardening if introduced
- release signing setup
