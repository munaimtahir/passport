# UI Design System

## Design intent

Calm, premium, trustworthy and data-dense without looking like accounting software.

## Visual principles

- Large whitespace
- Strong typographic hierarchy
- Restrained use of color
- Rounded cards only where they help grouping
- Minimal shadows
- Clear numeric alignment
- Monetary values visually prominent
- Charts subordinate to numbers and explanations
- Avoid gamification of wealth

## Semantic color usage

Use theme tokens, not literal colors in feature code.

Semantic roles:
- Primary
- Surface
- Surface variant
- Positive
- Warning
- Critical
- Informational
- Muted
- Divider

Do not use green/red alone to distinguish gain/loss.

## Typography hierarchy

- Display: Net worth / major annual figure
- Headline: Section titles
- Title: Cards
- Body: descriptions
- Label: metadata
- Numeric tabular style where supported for aligned financial values

## Privacy mode

Global eye icon:
- Hide monetary values
- Replace with bullets/placeholders
- Persist preference locally
- Screens requiring exact numbers can temporarily reveal by explicit action

## Components

- NetWorthHeroCard
- MoneySummaryCard
- TaxReadinessCard
- EvidenceStatusChip
- TaxItemRow
- FinancialEventRow
- AccountCard
- AssetCard
- InvestmentPositionRow
- DocumentRow
- ReconciliationEquationCard
- EmptyState
- FilterBar
- DateRangePicker
- CurrencyAmountField
- SecureIdentifierField

## Forms

- One concept per section
- Numeric keyboard for amounts
- Preserve entered values on navigation/configuration changes
- Inline validation
- Save button disabled only for truly required invalid state
- Draft support for complex tax/document entries

## Tax UX

Never dump official tax form structure onto the user during normal capture.

Capture language:
“What happened?”

Examples:
- Salary received
- Bank profit received
- Tax deducted
- Bought an asset
- Sold an investment
- Paid tax

Annual review may then show official/technical mapping where needed.

## Tablet/foldable readiness

MVP must not break on expanded widths.
Recommended:
- list/detail layouts on wide screens
- maximum content width for forms
- adaptive navigation rail where appropriate
