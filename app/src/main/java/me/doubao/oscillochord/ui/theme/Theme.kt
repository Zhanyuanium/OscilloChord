package me.doubao.oscillochord.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OscilloColorScheme = darkColorScheme(
    primary = OscilloPrimary,
    secondary = OscilloSecondary,
    tertiary = OscilloTertiary,
    background = OscilloBackground,
    surface = OscilloSurface,
    surfaceVariant = OscilloSurfaceVariant,
    onBackground = OscilloOnBackground,
    onSurface = OscilloOnSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black
)

@Composable
fun OscilloChordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OscilloColorScheme,
        typography = OscilloTypography,
        content = content
    )
}
