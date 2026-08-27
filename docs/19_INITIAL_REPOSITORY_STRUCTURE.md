# Suggested Initial Repository Structure

```text
finance/
├── app/
├── core/
│   ├── model/
│   ├── database/
│   ├── security/
│   ├── files/
│   ├── taxrules/
│   ├── ui/
│   └── testing/
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── money/
│   ├── wealth/
│   ├── tax/
│   ├── records/
│   ├── vault/
│   ├── reports/
│   ├── backup/
│   └── settings/
├── docs/
│   ├── architecture/
│   ├── verification/
│   ├── BUILD_STATUS.md
│   ├── BLOCKERS.md
│   └── FINAL_VERIFICATION.md
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

A smaller initial module count is acceptable if the package boundaries remain clean and build performance is better. Do not spend a sprint creating modules without feature value.
