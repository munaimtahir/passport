package pk.vexel.financepassport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PassportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) VexelDarkColorScheme else VexelLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VexelTypography,
        shapes = VexelShapes,
        content = content,
    )
}
