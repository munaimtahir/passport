# Internal QA Package

This package remains intentionally marked NO-GO for internal release because required MVP workflows and the required API/device matrix remain incomplete.

- APK: `app/build/outputs/apk/release/app-release.apk`
- Build: `./gradlew assembleRelease`
- Device smoke: attached `Android_26_Test`, API 26; API 36 and physical device unavailable in this run
- Automated verification: `./gradlew test lint assembleDebug connectedDebugAndroidTest`; unit/lint/debug and 28 connected API 26 tests pass
- Latest APK SHA-256: `ad0a5d427e22d0b9d5cc567e1f1e894289427dfd10c478f5842d4ba21c2d2118`
- Acceptance matrix: `docs/verification/ACCEPTANCE_MATRIX.md`
- Final report: `docs/FINAL_VERIFICATION.md`
- Signing: local debug key only; replace before any production distribution
- Known issues: see `docs/FINAL_VERIFICATION.md` and `docs/BLOCKERS.md`
