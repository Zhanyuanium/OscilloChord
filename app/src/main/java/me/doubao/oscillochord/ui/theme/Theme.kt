package me.doubao.oscillochord.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FallbackDarkColorScheme = darkColorScheme(
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
fun OscilloChordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use Material You dynamic color on Android 12+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> FallbackDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OscilloTypography,
        content = content
    )
}
