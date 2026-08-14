package pk.vexel.financepassport.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PassportColors = lightColorScheme(
    primary = Color(0xFF315C52),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5A2B),
    background = Color(0xFFF7F9F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EFEB),
)

@Composable
fun PassportTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PassportColors,
        typography = Typography(),
        content = content,
    )
}
