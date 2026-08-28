# Vexel Finance Passport — Sprint Blockers Register

## Environmental Blockers

### 1. Android Emulator KVM Hardware Acceleration in Container Environment
- **Impact**: Unable to launch headful/headless x86_64 AVD (`Sprint24_API_36`) directly within the containerized Linux host without KVM/hardware virtualization support (`CPU acceleration status: KVM requires a CPU that supports vmx or svm`).
- **Mitigation**: 
  - Complete full JVM unit tests and Compose UI tests via `./gradlew test` (PASSing 100%).
  - Complete production Debug APK build via `./gradlew assembleDebug` (`app/build/outputs/apk/debug/app-debug.apk` successfully produced).
  - Verify UI component contracts, tokenized design specs, and accessibility attributes in code.
  - Verification artifact ready for deployment to physical device or KVM-enabled host via `adb install app/build/outputs/apk/debug/app-debug.apk`.
