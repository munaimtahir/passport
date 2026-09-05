# Wave F Gate

## Result: PASS for implemented scope; overall F/G/H acceptance remains pending

- Canonical `FinancialPosition` is derived from active account balances, receivables, investments, included assets, and liabilities.
- Simple investment current values are now included alongside legacy investment history without adding income or expense.
- Excluded assets contribute zero; ownership percentage is applied to included active assets.
- Position, source breakdown, and masking are exposed in the new Position screen.
- JVM financial-position tests pass, including simple-investment valuation coverage.
- Connected Android regression: 100/100 passed on `passport`.

Remaining F acceptance work is broader manual creation/editing coverage for every asset/liability/receivable/investment scenario and explicit source drill-down actions.
