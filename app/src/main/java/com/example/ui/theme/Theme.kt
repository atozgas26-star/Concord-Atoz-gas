package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.data.model.ThemePreset

private val LightColorScheme = lightColorScheme(
    primary = BrandGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = BrandGreenOnContainer,
    secondary = BrandRedSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandRedContainer,
    onSecondaryContainer = BrandRedOnContainer,
    tertiary = SlateTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = SlateOnContainer,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = BrandRedBright,
    onError = Color.White,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

private val OceanicTealColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealSecondaryOnContainer,
    tertiary = SlateTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = SlateOnContainer,
    background = TealBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = BrandRedBright,
    onError = Color.White,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

private val SunsetAmberColorScheme = lightColorScheme(
    primary = FlamePrimary,
    onPrimary = Color.White,
    primaryContainer = FlameContainer,
    onPrimaryContainer = FlameOnContainer,
    secondary = FlameSecondary,
    onSecondary = Color.White,
    secondaryContainer = FlameSecondaryContainer,
    onSecondaryContainer = FlameSecondaryOnContainer,
    tertiary = BrandRedSecondary,
    onTertiary = Color.White,
    tertiaryContainer = BrandRedContainer,
    onTertiaryContainer = BrandRedOnContainer,
    background = FlameBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = FlameSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = BrandRedBright,
    onError = Color.White,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = DarkGreenOnPrimary,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = DarkGreenOnContainer,
    secondary = DarkRedSecondary,
    onSecondary = DarkRedOnSecondary,
    secondaryContainer = DarkRedContainer,
    onSecondaryContainer = DarkRedOnContainer,
    tertiary = SlateContainer,
    onTertiary = SlateOnContainer,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun ConcordGasTheme(
    themePreset: ThemePreset = ThemePreset.EMERALD_CORPORATE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreset) {
        ThemePreset.MIDNIGHT_DARK -> DarkColorScheme
        ThemePreset.OCEANIC_TEAL -> if (darkTheme) DarkColorScheme else OceanicTealColorScheme
        ThemePreset.SUNSET_AMBER -> if (darkTheme) DarkColorScheme else SunsetAmberColorScheme
        ThemePreset.EMERALD_CORPORATE -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
