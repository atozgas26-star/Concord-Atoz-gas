package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ThemePreset
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Choreographed 3-second animated app-open logo screen for Concord & A to Z Gas.
 * Features ignition ripple rings, spring-bounce metallic emblem medallion,
 * orbital gauge arc with traveling spark, diagonal metallic light shimmer,
 * staggered brand reveal, and smooth exit fade.
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean,
    onSplashFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isExiting by remember { mutableStateOf(false) }

    // Choreographed animation states
    val emblemScale = remember { Animatable(0.42f) }
    val emblemRotation = remember { Animatable(-16f) }
    val emblemAlpha = remember { Animatable(0f) }
    val ripple1Progress = remember { Animatable(0f) }
    val ripple2Progress = remember { Animatable(0f) }
    val orbitalSweep = remember { Animatable(0f) }
    val shimmerOffset = remember { Animatable(-1.4f) }
    val badgeProgress = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val dividerScaleX = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }
    val launchProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_living_motion")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_orbit"
    )

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        scope.launch {
            launch {
                exitScale.animateTo(
                    targetValue = 1.06f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            onSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Stage 1: Emblem spring entrance + ignition ripples
        launch {
            emblemAlpha.animateTo(1f, tween(durationMillis = 360))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            emblemRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            ripple1Progress.animateTo(1f, tween(durationMillis = 1050, easing = FastOutSlowInEasing))
        }
        launch {
            delay(220)
            ripple2Progress.animateTo(1f, tween(durationMillis = 1050, easing = FastOutSlowInEasing))
        }

        // Stage 2: Precision orbital gauge sweep + metallic shimmer
        launch {
            delay(160)
            orbitalSweep.animateTo(360f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
        }
        launch {
            delay(620)
            shimmerOffset.animateTo(1.8f, tween(durationMillis = 820, easing = FastOutSlowInEasing))
            delay(450)
            shimmerOffset.snapTo(-1.4f)
            shimmerOffset.animateTo(1.8f, tween(durationMillis = 820, easing = FastOutSlowInEasing))
        }

        // Stage 3: Staggered typography & credit reveal
        launch {
            delay(320)
            badgeProgress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
        launch {
            delay(460)
            titleProgress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        }
        launch {
            delay(620)
            dividerScaleX.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }
        launch {
            delay(540)
            footerAlpha.animateTo(1f, tween(durationMillis = 450))
        }

        // Overall 3-second launch bar + smooth fade out
        launchProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2720, easing = LinearEasing)
        )
        triggerSmoothExit()
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val goldAccent = Color(0xFFF6C667)
    val emeraldGlow = Color(0xFF1CE09A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF041A12),
                        primary.copy(alpha = 0.92f),
                        Color(0xFF061510),
                        secondary.copy(alpha = 0.78f)
                    )
                )
            )
            .testTag("startup_splash_overlay")
    ) {
        // Subtle atmospheric texture layer
        Image(
            painter = painterResource(id = R.drawable.img_company_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.14f)
        )

        // Atmospheric radial spotlight behind the animated logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.44f)
            val maxRadius = size.minDimension * 0.58f * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        emeraldGlow.copy(alpha = 0.24f),
                        goldAccent.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top theme & sound status pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(footerAlpha.value),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Active Theme",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = themePreset.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Audio Chime Status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (soundEnabled) "Sound On" else "Muted",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Centerpiece Animated Brand Logo + Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(226.dp)
                        .testTag("animated_logo_emblem")
                ) {
                    // Canvas Layer: Ignition Ripples, Orbital Gauge Arc, Counter-Rotating Ticks & Spark Tip
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.34f

                        // Ignition ripple 1 (Emerald-Gold)
                        if (ripple1Progress.value in 0.01f..0.99f) {
                            val r1 = baseRadius + (size.minDimension * 0.20f) * ripple1Progress.value
                            drawCircle(
                                color = emeraldGlow.copy(alpha = (1f - ripple1Progress.value) * 0.65f),
                                radius = r1,
                                center = center,
                                style = Stroke(width = 3.dp.toPx() * (1f - ripple1Progress.value * 0.5f))
                            )
                        }

                        // Ignition ripple 2 (Crimson-Amber)
                        if (ripple2Progress.value in 0.01f..0.99f) {
                            val r2 = baseRadius + (size.minDimension * 0.22f) * ripple2Progress.value
                            drawCircle(
                                color = BrandRedSecondary.copy(alpha = (1f - ripple2Progress.value) * 0.55f),
                                radius = r2,
                                center = center,
                                style = Stroke(width = 2.5.dp.toPx() * (1f - ripple2Progress.value * 0.5f))
                            )
                        }

                        // Subtle living outer halo
                        val haloRadius = size.minDimension * 0.42f * glowPulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    emeraldGlow.copy(alpha = 0.26f * emblemAlpha.value),
                                    BrandRedSecondary.copy(alpha = 0.10f * emblemAlpha.value),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = haloRadius
                            ),
                            radius = haloRadius,
                            center = center
                        )

                        // Counter-rotating inner precision gauge arc
                        val innerArcInset = 24.dp.toPx()
                        val innerArcSize = Size(size.width - innerArcInset * 2f, size.height - innerArcInset * 2f)
                        drawArc(
                            color = goldAccent.copy(alpha = 0.38f * emblemAlpha.value),
                            startAngle = counterOrbitAngle,
                            sweepAngle = 72f,
                            useCenter = false,
                            topLeft = Offset(innerArcInset, innerArcInset),
                            size = innerArcSize,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = BrandRedSecondary.copy(alpha = 0.55f * emblemAlpha.value),
                            startAngle = counterOrbitAngle + 180f,
                            sweepAngle = 54f,
                            useCenter = false,
                            topLeft = Offset(innerArcInset, innerArcInset),
                            size = innerArcSize,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Outer orbital sweep gauge arc (0° -> 360°)
                        val outerInset = 14.dp.toPx()
                        val outerSize = Size(size.width - outerInset * 2f, size.height - outerInset * 2f)
                        val sweep = orbitalSweep.value
                        if (sweep > 1f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        emeraldGlow,
                                        goldAccent,
                                        BrandRedSecondary,
                                        emeraldGlow
                                    ),
                                    center = center
                                ),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(outerInset, outerInset),
                                size = outerSize,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Bright traveling spark dot at the arc head
                            val tipAngleRad = ((-90f + sweep) * PI / 180.0)
                            val arcRadius = outerSize.width / 2f
                            val sparkPos = Offset(
                                x = center.x + (arcRadius * cos(tipAngleRad)).toFloat(),
                                y = center.y + (arcRadius * sin(tipAngleRad)).toFloat()
                            )
                            val sparkAlpha = if (sweep >= 358f) 0.7f else 1f
                            drawCircle(
                                color = goldAccent.copy(alpha = 0.4f * sparkAlpha),
                                radius = 7.dp.toPx(),
                                center = sparkPos
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = sparkAlpha),
                                radius = 3.2.dp.toPx(),
                                center = sparkPos
                            )
                        }
                    }

                    // Circular Metallic Emblem Medallion with Shimmer Sweep
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06251A),
                        border = BorderStroke(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    goldAccent.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.75f),
                                    emeraldGlow.copy(alpha = 0.85f)
                                )
                            )
                        ),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .size(146.dp)
                            .graphicsLayer {
                                alpha = emblemAlpha.value
                                scaleX = emblemScale.value
                                scaleY = emblemScale.value
                                rotationZ = emblemRotation.value
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_concord_logo_v2),
                                contentDescription = "C&A Gas Animated Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.18f
                                        scaleY = 1.18f
                                    }
                            )

                            // Diagonal specular metallic sheen gliding across the emblem
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val shiftX = size.width * shimmerOffset.value
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.34f),
                                            goldAccent.copy(alpha = 0.22f),
                                            Color.Transparent
                                        ),
                                        start = Offset(shiftX - size.width * 0.45f, 0f),
                                        end = Offset(shiftX + size.width * 0.25f, size.height)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Staggered Pill Badge
                Surface(
                    color = BrandRedSecondary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = badgeProgress.value
                        translationY = (1f - badgeProgress.value) * 18.dp.toPx()
                    }
                ) {
                    Text(
                        text = "C&A GAS • INDUSTRIAL & LPG FLEET",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Staggered Headline Reveal
                Text(
                    text = "CONCORD & ATOZ\nGAS TRADING LLC",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier
                        .testTag("splash_company_headline")
                        .graphicsLayer {
                            alpha = titleProgress.value
                            translationY = (1f - titleProgress.value) * 22.dp.toPx()
                        }
                )

                Spacer(Modifier.height(8.dp))

                // Expanding Emerald -> Gold -> Crimson Brand Accent Bar
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(3.dp)
                        .graphicsLayer {
                            scaleX = dividerScaleX.value
                            alpha = dividerScaleX.value
                        }
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(emeraldGlow, goldAccent, BrandRedSecondary)
                            )
                        )
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Store • Vehicles • Purchases • Reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleProgress.value
                        translationY = (1f - titleProgress.value) * 14.dp.toPx()
                    }
                )

                Spacer(Modifier.height(22.dp))

                LinearProgressIndicator(
                    progress = { launchProgress.value },
                    color = goldAccent,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .width(210.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                val secondsLeft = ((1f - launchProgress.value) * 3f).coerceAtLeast(0f)
                Text(
                    text = String.format(Locale.US, "Launching workspace... %.1fs", secondsLeft),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            // Bottom "Built by Ragavan" Credit + Smooth Skip action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(footerAlpha.value)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "Built by Ragavan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
                TextButton(
                    onClick = { triggerSmoothExit() },
                    modifier = Modifier.testTag("splash_skip_button")
                ) {
                    Text(
                        text = "Tap to Continue",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
