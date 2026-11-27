package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemePreset

val LocalIsDarkTheme = staticCompositionLocalOf { false }

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

// High-Contrast Warehouse Dark Color Schemes
private val EmeraldDarkColorScheme = darkColorScheme(
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
    outlineVariant = DarkOutlineVariant,
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

private val OceanicTealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = SlateContainer,
    onTertiary = SlateOnContainer,
    background = Color(0xFF071316),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF0E1D22),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF162A31),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF35515C),
    outlineVariant = Color(0xFF223740),
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

private val SunsetAmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF172554),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = DarkRedSecondary,
    onTertiary = DarkRedOnSecondary,
    background = Color(0xFF14100B),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF1E1811),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2B2319),
    onSurfaceVariant = Color(0xFFC4B5A5),
    outline = Color(0xFF544637),
    outlineVariant = Color(0xFF3A3025),
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
    val effectiveDark = darkTheme || themePreset == ThemePreset.MIDNIGHT_DARK
    val colorScheme = when (themePreset) {
        ThemePreset.MIDNIGHT_DARK -> EmeraldDarkColorScheme
        ThemePreset.OCEANIC_TEAL -> if (effectiveDark) OceanicTealDarkColorScheme else OceanicTealColorScheme
        ThemePreset.SUNSET_AMBER -> if (effectiveDark) SunsetAmberDarkColorScheme else SunsetAmberColorScheme
        ThemePreset.EMERALD_CORPORATE -> if (effectiveDark) EmeraldDarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.LaunchedEffect(view, effectiveDark) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides effectiveDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
