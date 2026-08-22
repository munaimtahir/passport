# Deferred Owner Decisions

Updated: 2026-08-22

## D-001 — Resolve repository/product identity before implementation

- Decision/input required: confirm the intended workspace is the Health Passport
  repository (`munaimtahir/health`) and provide/attach `Health-Passport-Review.md`,
  or explicitly authorize changing this Finance Passport checkout instead.
- Affected phase/task: all phases; source-of-truth and product identity in Phase 0.
- Safe options and trade-offs:
  - Recommended: provide the correct Health checkout/review and preserve this
    Finance checkout untouched. This avoids destructive product-identity changes
    and keeps finance history safe.
  - Explicitly re-scope to Finance Passport and provide finance-specific repair
    findings. Health-specific features and IDs must not be inferred.
- Recommended option: use the correct Health repository and review attachment.
- Work completed: inspected Git state, remotes, Gradle identity, manifest, local
  canonical docs, source tree, and baseline commands; created safety branch/tag;
  recorded the mismatch and environment limitations.
- Blocks internal release: yes, for the requested Health Passport release. It does
  not block unrelated work on the separate Finance Passport product.
- Six-hour check-in action: provide the Health repository checkout/path and review
  file, or confirm the task should be reissued against Finance Passport.

## D-002 — Environment capacity/device/signing inputs

- Decision/input required: a build environment with enough memory for Gradle tests,
  a compatible Android device/emulator, and (if a signed release is required) the
  authorized internal signing configuration.
- Affected phase/task: release verification and runtime gates.
- Recommended option: rerun on the owner’s established Android build environment;
  do not expose or commit signing material.
- Work completed: no device is connected; serial tests were killed with exit 137;
  no signing secret was requested or read.
- Blocks internal release: yes for runtime/signing evidence; not for source review.
- Six-hour check-in action: provide device/emulator access and authorized signing
  workflow if still required.
