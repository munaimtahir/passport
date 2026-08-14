# Information Architecture and UX Specification

## Bottom navigation

1. **Home**
2. **Money**
3. **Wealth**
4. **Tax & Records**
5. **Vault**

`More` is accessible through avatar/menu/top action to avoid six crowded bottom tabs on smaller devices.

## Home hierarchy

### Above the fold
1. Net Worth
2. Quick Add
3. Tax-year readiness
4. Upcoming obligations

### Below
5. Money summary
6. Wealth summary
7. Goals
8. Recent activity

## Money landing page
Tabs:
- Accounts
- Activity
- Income
- Expenses
- Calendar

## Wealth landing page
Tabs or segmented navigation:
- Assets
- Investments
- Liabilities
- Receivables
- Goals

## Tax & Records landing page
Primary cards:
- Tax Inbox
- Current Tax Year
- Wealth Reconciliation
- Official Records
- Filed Years

## Vault landing page
- Search
- Recent
- Tax evidence
- Bank
- Investments
- Property
- Identity/official
- Other

---

# Key flows

## Flow 1 — Add ordinary expense
Home → + → Expense → amount/account/category → Save

Target: <= 4 meaningful interactions after opening Quick Add.

## Flow 2 — Add tax-relevant salary
Home → + → Income → Salary → amount → tax withheld → mark tax relevant → attach salary slip (optional) → Save

System actions:
- creates financial event
- creates derived tax mapping
- links evidence
- updates income dashboard
- updates tax-year totals

## Flow 3 — Add tax item independently
Tax & Records → Tax Inbox → + → choose event type → enter data → evidence optional → Save

System may offer:
“Also reflect this in Money/Wealth?” when needed.

## Flow 4 — Investment purchase
Wealth → Investments → Holding → Buy → quantity/price/fees/account → Save

System:
- updates holding
- creates ledger movement if linked account selected
- marks potential tax relevance according to ruleset
- preserves original cost data

## Flow 5 — Upload certificate
Vault → Add document → choose file → category → optionally link → Save

If extraction is available:
- show suggestions, never silently commit extracted financial values
- user confirms proposed fields

## Flow 6 — Prepare annual tax draft
Tax & Records → Tax Year → Prepare Annual Tax Draft

Step 1: Preflight
- unresolved items
- missing evidence
- duplicate candidates
- missing opening/closing values

Step 2: Generate
- calculations
- mappings
- wealth reconciliation

Step 3: Review
- section-by-section figures with source drill-down

Step 4: Export
- tax preparation PDF
- structured JSON/CSV where applicable
- accountant/share package chosen explicitly by user

## Flow 7 — Wealth reconciliation
Tax Year → Reconciliation

Display:
Opening net wealth  
+ declared/recognized inflows  
− personal expenditure/outflows  
± transfers/adjustments  
= expected closing wealth  
vs recorded closing wealth  
= unexplained difference

Every line must be drillable.

---

# UX rules

- Default to simple terminology; tax terminology appears only where required.
- Do not expose database concepts.
- Always separate **value** from **evidence status**.
- Use progressive disclosure.
- Avoid red for ordinary expenses; reserve red/critical semantics for warnings/errors.
- Use clear empty states with one primary action.
- Never make destructive actions the primary button.
- All critical figures must have source drill-down.
- Use confirmation only for destructive or externally consequential actions.
- Use undo for reversible local edits where possible.
- Do not overload the home screen with charts.
- Use charts only where they answer a clear question.

---

# Accessibility

- Minimum touch target 48dp where practical
- Support system font scaling
- Do not encode status by color alone
- Content descriptions for meaningful icons
- High contrast for financial figures
- Currency formatting readable at large values
- Screen-reader ordering follows visual hierarchy
- Sensitive values can be hidden globally with an “eye” privacy control
