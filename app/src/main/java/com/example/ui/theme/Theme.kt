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

// Signature Flagship Bright Premium 3D Luxury Scheme:
// Electric Blue Primary • Metallic Orange Secondary • Chrome Silver Accent
// Bright Silver / Light Blue Gradient Background • Glossy White-Blue Cards • Deep Navy Text
private val BrightLuxuryColorScheme = lightColorScheme(
    primary = RoyalElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = ElectricBlueSoft,
    onPrimaryContainer = ElectricBlueOnSoft,
    secondary = MetallicOrange,
    onSecondary = PureWhite,
    secondaryContainer = MetallicOrangeSoft,
    onSecondaryContainer = MetallicOrangeOnSoft,
    tertiary = CategoryVioletBlue,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF3B1D8F),
    background = BrightSilverTop,
    onBackground = DeepNavyText,
    surface = PureWhite,
    onSurface = DeepNavyText,
    surfaceVariant = GlossyCardBlueTint,
    onSurfaceVariant = DarkSlateSecondary,
    outline = ChromeBorderLight,
    outlineVariant = LightOutlineVariant,
    error = ErrorDarkRed,
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
    onBackground = DeepNavyText,
    surface = PureWhite,
    onSurface = DeepNavyText,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = DarkSlateSecondary,
    outline = ChromeBorderLight,
    outlineVariant = LightOutlineVariant,
    error = ErrorDarkRed,
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
    tertiary = CategoryEmeraldGreen,
    onTertiary = PureWhite,
    tertiaryContainer = BrandGreenContainer,
    onTertiaryContainer = BrandGreenOnContainer,
    background = FlameBackground,
    onBackground = DeepNavyText,
    surface = PureWhite,
    onSurface = DeepNavyText,
    surfaceVariant = FlameSurfaceVariant,
    onSurfaceVariant = DarkSlateSecondary,
    outline = ChromeBorderLight,
    outlineVariant = LightOutlineVariant,
    error = ErrorDarkRed,
    onError = PureWhite,
    errorContainer = BrandRedContainer,
    onErrorContainer = BrandRedOnContainer
)

// Explicit Dark Mode Scheme (Pure White #FFFFFF primary, Light Silver #E2E8F0 secondary)
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
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
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
    onBackground = PureWhite,
    surface = Color(0xFF091B24),
    onSurface = PureWhite,
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
    onBackground = PureWhite,
    surface = Color(0xFF151828),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1E243B),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF394263),
    outlineVariant = Color(0xFF262D45),
    error = DarkRedSecondary,
    onError = DarkRedOnSecondary,
    errorContainer = DarkRedContainer,
    onErrorContainer = DarkRedOnContainer
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ConcordGasTheme(
    themePreset: ThemePreset = ThemePreset.EMERALD_CORPORATE,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDark = darkTheme || themePreset == ThemePreset.MIDNIGHT_DARK
    val colorScheme = when (themePreset) {
        ThemePreset.MIDNIGHT_DARK -> EmeraldDarkColorScheme
        ThemePreset.OCEANIC_TEAL -> if (effectiveDark) OceanicTealDarkColorScheme else OceanicTealColorScheme
        ThemePreset.SUNSET_AMBER -> if (effectiveDark) SunsetAmberDarkColorScheme else SunsetAmberColorScheme
        ThemePreset.EMERALD_CORPORATE -> if (effectiveDark) EmeraldDarkColorScheme else BrightLuxuryColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(view, effectiveDark) {
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
