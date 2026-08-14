# Internal QA Package

This package is intentionally marked NO-GO for internal release because required MVP workflows remain incomplete.

- APK: `app/build/outputs/apk/release/app-release.apk`
- Build: `./gradlew assembleRelease`
- Device smoke: `Android_16_Test`, API 36; compatibility suite: `Android_26_Test`, API 26
- Automated verification: `./gradlew test lint connectedDebugAndroidTest assembleRelease`; connected suite passes 22 tests on API 26 and API 36
- Latest APK SHA-256: `89716a2f792bb7189be9da2264102e6039298d38948963684f68dc425edffccc`
- Acceptance matrix: `docs/verification/ACCEPTANCE_MATRIX.md`
- Final report: `docs/FINAL_VERIFICATION.md`
- Signing: local debug key only; replace before any production distribution
- Known issues: see `docs/FINAL_VERIFICATION.md` and `docs/BLOCKERS.md`
