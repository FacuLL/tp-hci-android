package tp3.grupo1.hci.itba.edu.ar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LuminaPrimary,
    onPrimary = Color.White,
    primaryContainer = LuminaCard,
    onPrimaryContainer = LuminaOnDark,
    secondary = LuminaOnDark,
    onSecondary = Color.White,
    secondaryContainer = LuminaSurface1,
    onSecondaryContainer = LuminaContent,
    tertiary = LuminaPrimaryLight,
    onTertiary = LuminaContent,
    background = LuminaBackground,
    onBackground = LuminaContent,
    surface = Color.White,
    onSurface = LuminaContent,
    surfaceVariant = LuminaSurface1,
    onSurfaceVariant = LuminaMuted,
    surfaceContainer = LuminaSurface1,
    surfaceContainerLow = LuminaBackground,
    surfaceContainerHigh = LuminaSurface1,
    surfaceContainerHighest = LuminaSurface2,
    outline = LuminaSurface2,
    outlineVariant = LuminaSurface2,
    error = LuminaDanger,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminaPrimaryLight,
    onPrimary = LuminaContent,
    primaryContainer = LuminaOnDark,
    onPrimaryContainer = LuminaDarkContent,
    secondary = LuminaPrimaryLight,
    onSecondary = LuminaContent,
    secondaryContainer = LuminaDarkSurfaceVariant,
    onSecondaryContainer = LuminaDarkContent,
    tertiary = LuminaCard,
    onTertiary = LuminaContent,
    background = LuminaDarkBackground,
    onBackground = LuminaDarkContent,
    surface = LuminaDarkSurface,
    onSurface = LuminaDarkContent,
    surfaceVariant = LuminaDarkSurfaceVariant,
    onSurfaceVariant = LuminaDarkMuted,
    surfaceContainer = LuminaDarkSurfaceVariant,
    surfaceContainerLow = LuminaDarkSurface,
    surfaceContainerHigh = LuminaDarkSurfaceVariant,
    surfaceContainerHighest = LuminaDarkOutline,
    outline = LuminaDarkOutline,
    outlineVariant = LuminaDarkOutline,
    error = LuminaDarkDanger,
    onError = LuminaContent,
)

@Composable
fun LuminaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
