# Utility Tracker Product Reset Plan

This document outlines the scope of the Monthly Utility Bill Tracker reset.

## 1. Absolute Product Scope Decision
The complete purpose of the application in this version is:
> Register recurring monthly utility bills once, automatically create each monthly bill occurrence, show unpaid obligations, record their payment, retain proof of payment, and provide searchable bill and payment history.

## 2. Navigation Scope
The application navigation has been simplified to a three-destination shell:
1. **Home:** Shows urgent attention items (overdue, due soon, pending) and counts.
2. **Bills:** Manage permanent utility bill profiles (Electricity, Gas, Telephone, Other).
3. **History:** View past payment and occurrence history with advanced search/filters.

All other screens and navigation links from the previous financial/tax version are hidden or removed from the normal user experience.

## 3. Preservation and Safety
We preserve the following:
- Database structures and existing schema versioning (safe Room migrations).
- Signing configuration and release lineage.
- Secure local storage, encryption (CryptoService), and biometric settings.
- Reusable UI design system.
