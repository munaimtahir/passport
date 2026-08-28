package pk.vexel.financepassport.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Palette (Quiet Parchment & Deep Ink)
val EmeraldPrimaryLight = Color(0xFF1B3B36)
val OnEmeraldPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFE3ECE8)
val OnEmeraldContainerLight = Color(0xFF0D2420)

val OchreSecondaryLight = Color(0xFF8C5D3B)
val OnOchreSecondaryLight = Color(0xFFFFFFFF)
val OchreContainerLight = Color(0xFFF7EBE3)
val OnOchreContainerLight = Color(0xFF351F10)

val BackgroundLight = Color(0xFFF6F7F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDEDE9)
val OnSurfaceLight = Color(0xFF171D1B)
val OnSurfaceVariantLight = Color(0xFF525D59)
val OutlineLight = Color(0xFFD5DDD9)

// Dark Palette (Midnight Ink & Soft Slate)
val EmeraldPrimaryDark = Color(0xFF7CA69A)
val OnEmeraldPrimaryDark = Color(0xFF0A201B)
val EmeraldContainerDark = Color(0xFF244740)
val OnEmeraldContainerDark = Color(0xFFD7E5E0)

val OchreSecondaryDark = Color(0xFFDCA884)
val OnOchreSecondaryDark = Color(0xFF3F2514)
val OchreContainerDark = Color(0xFF573B28)
val OnOchreContainerDark = Color(0xFFF7EBE3)

val BackgroundDark = Color(0xFF111413)
val SurfaceDark = Color(0xFF191C1B)
val SurfaceVariantDark = Color(0xFF232725)
val OnSurfaceDark = Color(0xFFE1E3E0)
val OnSurfaceVariantDark = Color(0xFFA3ACA7)
val OutlineDark = Color(0xFF3A413E)

// Semantic Financial Status Tokens (Light & Dark)
object StatusColors {
    // Overdue / Urgent
    val OverdueContainerLight = Color(0xFFFBEBEA)
    val OnOverdueContainerLight = Color(0xFF7D201A)
    val OverdueContainerDark = Color(0xFF3D1917)
    val OnOverdueContainerDark = Color(0xFFE58B84)

    // Due Soon / Warning
    val DueSoonContainerLight = Color(0xFFFFF6E5)
    val OnDueSoonContainerLight = Color(0xFF7A4800)
    val DueSoonContainerDark = Color(0xFF3B2600)
    val OnDueSoonContainerDark = Color(0xFFEDBE6A)

    // Paid / Success
    val PaidContainerLight = Color(0xFFE9F5EF)
    val OnPaidContainerLight = Color(0xFF164D35)
    val PaidContainerDark = Color(0xFF153326)
    val OnPaidContainerDark = Color(0xFF7DCBA4)

    // Pending / Expected
    val PendingContainerLight = Color(0xFFEEF3F7)
    val OnPendingContainerLight = Color(0xFF2B4759)
    val PendingContainerDark = Color(0xFF1E2D38)
    val OnPendingContainerDark = Color(0xFF9CB9CC)

    // Skipped / Archived
    val SkippedContainerLight = Color(0xFFF0F1F0)
    val OnSkippedContainerLight = Color(0xFF4B524E)
    val SkippedContainerDark = Color(0xFF252927)
    val OnSkippedContainerDark = Color(0xFF8A938F)
}

val VexelLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = OchreSecondaryLight,
    onSecondary = OnOchreSecondaryLight,
    secondaryContainer = OchreContainerLight,
    onSecondaryContainer = OnOchreContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

val VexelDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = OchreSecondaryDark,
    onSecondary = OnOchreSecondaryDark,
    secondaryContainer = OchreContainerDark,
    onSecondaryContainer = OnOchreContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)
