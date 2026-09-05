# Wave F–H Invariant Matrix

This matrix records acceptance evidence honestly. A green legacy regression suite does not prove an invariant unless the invariant is covered by a targeted automated or device test.

| Invariant range | Current result | Evidence / gap |
|---|---|---|
| INV-F01–F06 | PARTIAL | Financial-position unit coverage exists, including simple-investment inclusion; full source scenarios and device creation flows are pending. |
| INV-F07 | FAIL | Existing snapshot behavior still requires immutable historical monthly-snapshot verification. |
| INV-F08–F10 | PARTIAL | Ownership and excluded-asset behavior are represented; complete device coverage is pending. |
| INV-F11–F13 | PARTIAL | Position screen exposes totals and assets; complete drill-down and context/allocation coverage is pending. |
| INV-G01–G08 | PARTIAL | Calendar/reminder persistence and dismiss/snooze regression coverage pass; complete source-aware action and overdue semantics coverage is pending. |
| INV-G09–G13 | PARTIAL | Existing regression is green; duplicate projection, deep-link security, restore reconciliation, and follow-up semantics need dedicated acceptance tests. |
| INV-H01, H03, H08, H10–H12 | PARTIAL | Encrypted app-private import, hashing, and existing link/lifecycle tests exist; full mutation-isolation and privacy device evidence is pending. |
| INV-H02, H04–H07, H09, H13 | FAIL | Multi-target link UI, dependency-count delete warning, replacement lineage, expiry integration, backup byte/link comparison, camera convergence, and plaintext-residue acceptance are not complete. |

## Regression evidence

- Connected suite on dedicated `passport` emulator (`emulator-5562`): 100/100 passed, 0 failed, 0 skipped in the latest recorded run.
- This result is a regression result only; it does not upgrade the partial/failed invariant rows above without targeted evidence.

## Gate verdict

`NOT ACCEPTED` until the missing F/G/H implementation and targeted verification are completed.
