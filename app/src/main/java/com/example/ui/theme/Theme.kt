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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemePreset

val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Signature Flagship Midnight Royal Blue Scheme:
// Midnight Royal Blue • Electric Blue • Metallic Orange • Arctic White • Platinum Grey
private val SignatureRoyalColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF153273),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = MetallicOrangeBright,
    onSecondary = Color(0xFF2B1002),
    secondaryContainer = Color(0xFF472009),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = PlatinumSilver,
    onTertiary = ObsidianMidnight,
    tertiaryContainer = SignatureRoyalElevated,
    onTertiaryContainer = PlatinumSilver,
    background = SignatureRoyalBackground,
    onBackground = TextPrimaryDark,
    surface = SignatureRoyalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SignatureRoyalElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = SignatureRoyalBorder,
    outlineVariant = ObsidianBorder,
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

// Fallback Platinum Light Scheme (unused by default; kept clean)
private val LightColorScheme = lightColorScheme(
    primary = RoyalElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = ElectricBlueSoft,
    onPrimaryContainer = ElectricBlueOnSoft,
    secondary = MetallicOrange,
    onSecondary = PureWhite,
    secondaryContainer = MetallicOrangeSoft,
    onSecondaryContainer = MetallicOrangeOnSoft,
    tertiary = SlateTertiary,
    onTertiary = PureWhite,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = SlateOnContainer,
    background = ArcticWhite,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = ArcticSurfaceSubtle,
    onSurfaceVariant = TextSecondaryLight,
    outline = PlatinumOutline,
    outlineVariant = PlatinumSilver,
    error = BrandRedBright,
    onError = PureWhite,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

private val OceanicTealColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = PureWhite,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = TealSecondary,
    onSecondary = PureWhite,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealSecondaryOnContainer,
    tertiary = MetallicOrange,
    onTertiary = PureWhite,
    tertiaryContainer = MetallicOrangeSoft,
    onTertiaryContainer = MetallicOrangeOnSoft,
    background = TealBackground,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = PlatinumOutline,
    outlineVariant = PlatinumSilver,
    error = BrandRedBright,
    onError = PureWhite,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

private val SunsetAmberColorScheme = lightColorScheme(
    primary = FlamePrimary,
    onPrimary = PureWhite,
    primaryContainer = FlameContainer,
    onPrimaryContainer = FlameOnContainer,
    secondary = FlameSecondary,
    onSecondary = PureWhite,
    secondaryContainer = FlameSecondaryContainer,
    onSecondaryContainer = FlameSecondaryOnContainer,
    tertiary = ObsidianElevated,
    onTertiary = PureWhite,
    tertiaryContainer = PlatinumSilver,
    onTertiaryContainer = ObsidianMidnight,
    background = FlameBackground,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = FlameSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = PlatinumOutline,
    outlineVariant = PlatinumSilver,
    error = BrandRedBright,
    onError = PureWhite,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

// Signature Flagship Dark Scheme: Obsidian Midnight Blue, Royal Electric Blue, Signature Metallic Orange
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = ElectricBlueBright,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF132A60),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = MetallicOrangeBright,
    onSecondary = Color(0xFF2B1002),
    secondaryContainer = Color(0xFF421D08),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = PlatinumSilver,
    onTertiary = ObsidianMidnight,
    tertiaryContainer = ObsidianElevated,
    onTertiaryContainer = PlatinumSilver,
    background = ObsidianMidnight,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
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
    secondary = ElectricBlueGlow,
    onSecondary = ObsidianMidnight,
    secondaryContainer = Color(0xFF132A60),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = MetallicOrangeBright,
    onTertiary = Color(0xFF2B1002),
    background = Color(0xFF041118),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF091B24),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF102835),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF244859),
    outlineVariant = Color(0xFF173240),
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

private val SunsetAmberDarkColorScheme = darkColorScheme(
    primary = MetallicOrangeBright,
    onPrimary = Color(0xFF2B1002),
    primaryContainer = Color(0xFF52230A),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = ElectricBlueGlow,
    onSecondary = ObsidianMidnight,
    secondaryContainer = Color(0xFF162C63),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = PlatinumSilver,
    onTertiary = ObsidianMidnight,
    background = Color(0xFF0D0E18),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF151828),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF1E243B),
    onSurfaceVariant = Color(0xFFBFC8DC),
    outline = Color(0xFF394263),
    outlineVariant = Color(0xFF262D45),
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun ConcordGasTheme(
    themePreset: ThemePreset = ThemePreset.EMERALD_CORPORATE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val effectiveDark = darkTheme || themePreset == ThemePreset.MIDNIGHT_DARK
    val useMidnightTheme = effectiveDark || themePreset == ThemePreset.EMERALD_CORPORATE
    val colorScheme = when (themePreset) {
        ThemePreset.MIDNIGHT_DARK -> EmeraldDarkColorScheme
        ThemePreset.OCEANIC_TEAL -> if (effectiveDark) OceanicTealDarkColorScheme else OceanicTealColorScheme
        ThemePreset.SUNSET_AMBER -> if (effectiveDark) SunsetAmberDarkColorScheme else SunsetAmberColorScheme
        ThemePreset.EMERALD_CORPORATE -> if (effectiveDark) EmeraldDarkColorScheme else SignatureRoyalColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(view, useMidnightTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useMidnightTheme
            controller.isAppearanceLightNavigationBars = !useMidnightTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides useMidnightTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
