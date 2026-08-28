# Vexel Finance Passport — Design Language 2.0 (Muted Editorial)

## 1. Design Thesis: Private Financial Memory + Financial Pulse
Vexel Finance Passport is designed as an extension of the user's financial memory: calm, mature, private, structured, and evidence-backed. 

It avoids casino fintech tropes (neon gradients, flying coins, confetti, red/green panic walls) in favor of a **Muted Editorial** aesthetic reminiscent of high-end archival stationery, architectural journals, and calm personal ledgers.

## 2. Color Palette Strategy (Light & Dark)

### Light Palette (Quiet Parchment & Deep Ink)
- **Primary / Jewel Accent**: `0xFF1B3B36` (Deep Forest Emerald)
- **On Primary**: `0xFFFFFFFF`
- **Primary Container**: `0xFFE3ECE8`
- **On Primary Container**: `0xFF0D2420`
- **Secondary Accent**: `0xFF8C5D3B` (Warm Muted Ochre)
- **Background**: `0xFFF6F7F5` (Soft Architectural Neutral)
- **Surface**: `0xFFFFFFFF` (Crisp Paper Surface)
- **Surface Variant / Muted**: `0xFFEDEDE9` (Warm Gray Surface)
- **On Surface (Text Primary)**: `0xFF171D1B` (Deep Ink)
- **On Surface Variant (Text Secondary)**: `0xFF525D59` (Slate Gray)
- **Outline / Divider**: `0xFFD5DDD9`

### Dark Palette (Midnight Ink & Soft Slate)
- **Primary / Jewel Accent**: `0xFF7CA69A` (Muted Sage Emerald)
- **On Primary**: `0xFF0A201B`
- **Primary Container**: `0xFF244740`
- **On Primary Container**: `0xFFD7E5E0`
- **Secondary Accent**: `0xFFDCA884` (Warm Soft Amber)
- **Background**: `0xFF111413` (Midnight Dark)
- **Surface**: `0xFF191C1B` (Elevated Midnight Surface)
- **Surface Variant**: `0xFF232725` (Muted Dark Card Surface)
- **On Surface (Text Primary)**: `0xFFE1E3E0` (Soft White Ink)
- **On Surface Variant (Text Secondary)**: `0xFFA3ACA7` (Muted Gray Text)
- **Outline / Divider**: `0xFF3A413E`

### Semantic Status Tokens (Non-Red/Green Dependant)
- **Overdue / Attention Critical**: Light `0xFF7D201A` / Dark `0xFFE58B84` (Terracotta Brick)
- **Due Soon / Attention Moderate**: Light `0xFF854F00` / Dark `0xFFEDBE6A` (Amber Gold)
- **Paid / Completed**: Light `0xFF1E523A` / Dark `0xFF82C8A4` (Quiet Emerald)
- **Pending / Expected**: Light `0xFF3D5565` / Dark `0xFF9CB8CC` (Muted Slate)
- **Skipped / Archived**: Light `0xFF636A66` / Dark `0xFF8A938F` (Pewter Gray)

## 3. Typography & Numeric Hierarchy
- **Headline Large/Medium**: Quiet Editorial Serif/Sans weight (`28sp`/`24sp`, tight tracking, commanding section titles)
- **Title Large/Medium**: Clean structural titles (`20sp`/`16sp`, medium weight)
- **Body Large/Medium**: Generous legibility (`16sp`/`14sp`, 1.4 line-height)
- **Monospace/Numeric Display**: Tabular alignment for PKR amounts, crisp digit grouping, zero jitter on value changes.

## 4. Spacing & Shape Tokens
- **Grid Spacing**: 4dp (micro), 8dp (small), 12dp (compact), 16dp (standard), 24dp (section), 32dp (hero).
- **Surface Radii**:
  - `ExtraSmall`: 4dp (Status chips, tags)
  - `Small`: 8dp (Interactive buttons, inputs)
  - `Medium`: 14dp (Cards, list containers)
  - `Large`: 24dp (Sheets, top/bottom rounded drawers, Capture Tray)

## 5. Signature Motion & Touch Target Rules
- **Touch Target**: Minimum 48dp x 48dp for all interactive elements.
- **Motion Durations**: 200ms-300ms easing (`FastOutSlowInEasing`) for tray expansion, card state transitions, and detail expands.
- **Accessibility**: High contrast text ratios (>= 4.5:1), TalkBack explicit content descriptions on all status chips and icons.
