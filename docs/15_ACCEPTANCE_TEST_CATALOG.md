# Acceptance Test Catalog

## Onboarding
- AT-001 Fresh install opens onboarding.
- AT-002 User can finish onboarding without internet.
- AT-003 User can skip optional financial setup.
- AT-004 PIN is required before entering protected app after setup.

## Money
- AT-010 Add account.
- AT-011 Edit account.
- AT-012 Archive account without losing history.
- AT-013 Add salary income.
- AT-014 Add expense.
- AT-015 Transfer between accounts changes both balances but not income/expense totals.
- AT-016 Filter activity by date/category/account.

## Wealth
- AT-020 Add asset.
- AT-021 Dispose asset without deleting history.
- AT-022 Add liability and update outstanding balance.
- AT-023 Add receivable and partial repayment.
- AT-024 Buy investment.
- AT-025 Sell partial investment.
- AT-026 Record dividend and withheld tax.
- AT-027 Net worth equals assets/investments/cash minus liabilities according to configured valuation rules.

## Tax capture
- AT-030 Mark salary as tax relevant.
- AT-031 Tax item appears in correct tax year exactly once.
- AT-032 Add manual tax item.
- AT-033 Attach evidence.
- AT-034 Exclude item with reason.
- AT-035 Reclassify item.
- AT-036 Source edit triggers safe remapping without duplication.
- AT-037 Historic ruleset version preserved.

## Annual draft
- AT-040 Generate annual draft.
- AT-041 Draft section totals match sources.
- AT-042 Every line supports source drill-down.
- AT-043 Missing evidence appears as issue.
- AT-044 Duplicate candidate appears as warning.
- AT-045 Regenerate creates versioned draft.
- AT-046 User override is visible and reason retained.

## Wealth reconciliation
- AT-050 Balanced synthetic dataset reconciles to zero.
- AT-051 Missing acquisition produces non-zero difference.
- AT-052 Drill-down identifies contributing records.

## Vault
- AT-060 Import PDF.
- AT-061 Import image.
- AT-062 Link document to tax item.
- AT-063 Link one document to multiple objects.
- AT-064 Delete warns about links.
- AT-065 Encrypted stored file is not plaintext.
- AT-066 Duplicate hash warning.

## Reports
- AT-070 Generate net-worth report.
- AT-071 Generate tax preparation PDF.
- AT-072 Report figures match UI.
- AT-073 Long document paginates without clipping.
- AT-074 Export CSV.

## Security
- AT-080 Wrong PIN denied.
- AT-081 Biometric cancel does not unlock.
- AT-082 App relocks after configured inactivity.
- AT-083 Deep link cannot bypass lock.
- AT-084 Sensitive identifier hidden by default.
- AT-085 Privacy mode hides dashboard amounts.
- AT-086 Production logs contain no obvious sensitive data.

## Backup
- AT-090 Create encrypted backup.
- AT-091 Wrong password fails safely.
- AT-092 Tampered backup rejected.
- AT-093 Restore into cleared app.
- AT-094 Record counts match.
- AT-095 Financial totals match.
- AT-096 Document hashes match.
- AT-097 Interrupted/failed restore does not leave partial state.

## Data ownership
- AT-100 Export structured data.
- AT-101 Delete all removes user data.
- AT-102 App returns to clean onboarding state after delete all.

## Notifications
- AT-110 Create reminder.
- AT-111 Reminder fires under supported test conditions.
- AT-112 Edit reminder updates schedule.
- AT-113 Delete reminder cancels it.
