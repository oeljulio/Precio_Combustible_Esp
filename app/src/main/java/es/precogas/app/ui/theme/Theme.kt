package es.precogas.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import es.precogas.app.ThemeMode

private val LightColors = lightColorScheme(
    primary = Petrol,
    onPrimary = LightSurface,
    secondary = SavingsGreen,
    onSecondary = LightSurface,
    tertiary = AccentYellow,
    onTertiary = PetrolDark,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFFE5EFF3),
    onSurfaceVariant = Color(0xFF405965)
)

private val DarkColors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = PetrolDark,
    secondary = SavingsGreenBright,
    onSecondary = PetrolDark,
    tertiary = AccentYellow,
    onTertiary = PetrolDark,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8CFDA)
)

private val AmoledColors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = PetrolDark,
    secondary = SavingsGreenBright,
    onSecondary = Color.Black,
    tertiary = AccentYellow,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color.Black,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF101820),
    onSurfaceVariant = Color(0xFFD0DCE2),
    surfaceContainer = Color(0xFF101820),
    surfaceContainerHigh = Color(0xFF171F26)
)


@Composable
fun PreCoGasTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colors = when (themeMode) {
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.AMOLED -> AmoledColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
