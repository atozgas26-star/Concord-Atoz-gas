package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalElectricBlue
import kotlinx.coroutines.delay

/**
 * C&A GAS Signature Architectural Glass Card.
 * Renders refined platinum hairline edge highlights in Light Mode and obsidian-royal depth in Dark Mode.
 */
@Composable
fun LuxuryGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentGlow: Color? = null,
    forceMidnight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current || forceMidnight
    val surfaceColor = if (isDark) ObsidianSurface.copy(alpha = 0.94f) else PureWhite
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                (accentGlow ?: ElectricBlueGlow).copy(alpha = 0.62f),
                ObsidianBorder.copy(alpha = 0.78f),
                MetallicOrangeBright.copy(alpha = 0.36f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                (accentGlow ?: RoyalElectricBlue).copy(alpha = 0.22f),
                PlatinumSilver,
                PlatinumSilver.copy(alpha = 0.8f)
            )
        )
    }

    val cardModifier = if (onClick != null) {
        modifier.luxuryTapScale()
    } else {
        modifier
    }

    val innerContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (isDark) {
                        // Subtle 2.5D specular top-left ambient light reflection
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    (accentGlow ?: ElectricBlueGlow).copy(alpha = 0.08f),
                                    Color.Transparent,
                                    MetallicOrange.copy(alpha = 0.04f)
                                )
                            )
                        )
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            color = surfaceColor,
            tonalElevation = if (isDark) 3.dp else 1.dp,
            shadowElevation = if (isDark) 12.dp else 3.dp,
            border = BorderStroke(1.1.dp, borderBrush),
            content = innerContent
        )
    } else {
        Surface(
            modifier = cardModifier,
            shape = shape,
            color = surfaceColor,
            tonalElevation = if (isDark) 3.dp else 1.dp,
            shadowElevation = if (isDark) 10.dp else 2.dp,
            border = BorderStroke(1.1.dp, borderBrush),
            content = innerContent
        )
    }
}

/**
 * Subtle tactile spring-scale feedback on press while preserving Material ripple semantics.
 */
fun Modifier.luxuryTapScale(
    pressedScale: Float = 0.982f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "luxury_tap_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Lightweight, non-blocking staggered reveal for luxury editorial section entrances (<260ms).
 */
@Composable
fun LuxurySectionReveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * 14f
        },
        content = content
    )
}

/**
 * Editorial section header with royal electric-blue / metallic-orange precision indicator.
 */
@Composable
fun LuxurySectionHeader(
    eyebrow: String? = null,
    title: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(if (eyebrow != null) 28.dp else 18.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor, MetallicOrangeBright)
                    )
                )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Ambient Obsidian-Royal hero container used for executive summary cards and auth headers.
 */
fun Modifier.obsidianRoyalAtmosphere(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                ObsidianMidnight,
                ObsidianDeep,
                Color(0xFF0F2964)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ElectricBlueBright.copy(alpha = 0.24f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.16f, size.height * 0.12f),
            radius = size.maxDimension * 0.58f
        ),
        radius = size.maxDimension * 0.58f,
        center = Offset(size.width * 0.16f, size.height * 0.12f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                MetallicOrange.copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.92f, size.height * 0.88f),
            radius = size.maxDimension * 0.45f
        ),
        radius = size.maxDimension * 0.45f,
        center = Offset(size.width * 0.92f, size.height * 0.88f)
    )
}

/**
 * Thin horizontal luxury divider with Royal Electric Blue -> Metallic Orange -> Transparent gradient.
 */
@Composable
fun SignatureAccentHairline(
    height: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ElectricBlueBright,
                        ElectricBlueGlow,
                        MetallicOrangeBright,
                        MetallicOrange
                    )
                )
            )
    )
}

/**
 * Animated stock number counter with smooth numeric transitions and subtle update pop feedback.
 * Respects Android reduced-motion settings automatically.
 */
@Composable
fun AnimatedStockCounter(
    count: Int,
    prefix: String = "",
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val animatedCount by androidx.compose.animation.core.animateIntAsState(
        targetValue = count,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 460,
            easing = FastOutSlowInEasing
        ),
        label = "animated_stock_counter"
    )
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (!reducedMotion) {
            pulseScale.snapTo(1.06f)
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    Text(
        text = "$prefix$animatedCount$suffix",
        style = style,
        fontFamily = com.example.ui.theme.JetBrainsMono,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale.value
            scaleY = pulseScale.value
        }
    )
}

/**
 * Lightweight, non-blocking luxury animated Success Checkmark Overlay displayed briefly
 * after saving stock entries, product updates, or different gas returns.
 */
@Composable
fun LuxurySuccessCheckOverlay(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message == null) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val checkProgress = remember(message) { Animatable(if (reducedMotion) 1f else 0f) }
    val cardAlpha = remember(message) { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(message) {
        if (!reducedMotion) {
            cardAlpha.animateTo(1f, tween(180))
            checkProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        }
        delay(1350)
        cardAlpha.animateTo(0f, tween(if (reducedMotion) 80 else 220))
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .graphicsLayer { alpha = cardAlpha.value },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianMidnight.copy(alpha = 0.96f),
            shadowElevation = 14.dp,
            border = BorderStroke(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(ElectricBlueGlow, Color(0xFF10B981), MetallicOrangeBright)
                )
            ),
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .testTag("luxury_success_check_overlay")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(34.dp)) {
                        val p = checkProgress.value
                        val r = size.minDimension / 2f
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.18f),
                            radius = r
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(ElectricBlueGlow, Color(0xFF10B981), MetallicOrangeBright)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * p,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.6.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        if (p > 0.25f) {
                            val checkT = ((p - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            val p1 = Offset(size.width * 0.28f, size.height * 0.52f)
                            val p2 = Offset(size.width * 0.44f, size.height * 0.68f)
                            val p3 = Offset(size.width * 0.74f, size.height * 0.35f)
                            val seg1 = (checkT * 2f).coerceIn(0f, 1f)
                            drawLine(
                                color = Color(0xFF34D399),
                                start = p1,
                                end = Offset(p1.x + (p2.x - p1.x) * seg1, p1.y + (p2.y - p1.y) * seg1),
                                strokeWidth = 2.8.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            if (checkT > 0.5f) {
                                val seg2 = ((checkT - 0.5f) * 2f).coerceIn(0f, 1f)
                                drawLine(
                                    color = Color(0xFF34D399),
                                    start = p2,
                                    end = Offset(p2.x + (p3.x - p2.x) * seg2, p2.y + (p3.y - p2.y) * seg2),
                                    strokeWidth = 2.8.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Saved Successfully",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.ArcticWhite,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

