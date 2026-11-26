package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.MusicNote
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
import com.example.audio.SoundEffectsManager
import com.example.audio.StartupSoundStyle
import com.example.data.model.ThemePreset
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val STARTUP_LAUNCH_DURATION_SECONDS = 7f

/**
 * Choreographed 7-second cinematic app-open launch screen for Concord & Atoz Gas Trading LLC.
 * Harmonizes with the Red & Blue C&A cylinder emblem with repeating ignition ripples, dual orbital
 * pressure rings, metallic light shimmer sweeps, animated audio equalizer bars, rotating
 * system-ready phases, and smooth exit fade (with instant skip option).
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean,
    startupSoundStyle: StartupSoundStyle = StartupSoundStyle.ROYAL_FANFARE,
    onSplashFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isExiting by remember { mutableStateOf(false) }

    // Choreographed animation states
    val emblemScale = remember { Animatable(0.38f) }
    val emblemRotation = remember { Animatable(-18f) }
    val emblemAlpha = remember { Animatable(0f) }
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
        initialValue = 0.86f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_orbit"
    )
    val rippleCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ignition_ripple_cycle"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audio_wave_phase"
    )

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        SoundEffectsManager.stopStartupSound()
        scope.launch {
            launch {
                exitScale.animateTo(
                    targetValue = 1.06f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
            onSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Stage 1: Red C&A Emblem spring entrance
        launch {
            emblemAlpha.animateTo(1f, tween(durationMillis = 420))
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

        // Stage 2: Precision orbital gauge sweep + repeating metallic sheen sweeps over 10s
        launch {
            delay(180)
            orbitalSweep.animateTo(360f, tween(durationMillis = 1600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(650)
            repeat(4) {
                shimmerOffset.snapTo(-1.4f)
                shimmerOffset.animateTo(1.8f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
                delay(1450)
            }
        }

        // Stage 3: Staggered typography & credit reveal
        launch {
            delay(360)
            badgeProgress.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
        }
        launch {
            delay(520)
            titleProgress.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
        }
        launch {
            delay(700)
            dividerScaleX.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        }
        launch {
            delay(600)
            footerAlpha.animateTo(1f, tween(durationMillis = 480))
        }

        // Overall 7-second launch progression (6.7s progress + 0.3s smooth exit = 7.0s)
        launchProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6700, easing = LinearEasing)
        )
        triggerSmoothExit()
    }

    val crimsonPrimary = Color(0xFFB31B1A)
    val crimsonBright = Color(0xFFFF4D4D)
    val royalBlue = Color(0xFF1E88E5)
    val goldAccent = Color(0xFFF6C667)
    val warmEmber = Color(0xFFFF8A4C)

    val phaseMessage = when {
        launchProgress.value < 0.25f -> "Igniting C&A Gas Fleet System..."
        launchProgress.value < 0.50f -> "Syncing Store, Vehicles & Duty Punches..."
        launchProgress.value < 0.75f -> "Loading Live Alerts, Chat & Reports..."
        else -> "Ready • Concord & Atoz Gas Trading LLC"
    }

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
                        Color(0xFF061A42),
                        Color(0xFF092C6C),
                        Color(0xFF3E0B14),
                        Color(0xFF150610)
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

        // Atmospheric radial red-and-blue spotlight behind the animated logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.43f)
            val maxRadius = size.minDimension * 0.62f * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        crimsonBright.copy(alpha = 0.28f),
                        royalBlue.copy(alpha = 0.22f),
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
            // Top theme & sound style status pills
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
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.MusicNote else Icons.Default.VolumeOff,
                            contentDescription = "Launch Sound Style",
                            tint = if (soundEnabled) goldAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (soundEnabled) startupSoundStyle.label else "Muted",
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
                        .size(234.dp)
                        .testTag("animated_logo_emblem")
                ) {
                    // Canvas Layer: Repeating Ignition Ripples, Orbital Gauge Arc, Counter-Rotating Arcs & Spark
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension * 0.34f

                        // Continuous ignition ripple 1 (Gold-Crimson)
                        val r1 = baseRadius + (size.minDimension * 0.21f) * rippleCycle
                        drawCircle(
                            color = goldAccent.copy(alpha = (1f - rippleCycle) * 0.58f * emblemAlpha.value),
                            radius = r1,
                            center = center,
                            style = Stroke(width = 3.dp.toPx() * (1f - rippleCycle * 0.45f))
                        )

                        // Offset ignition ripple 2 (Crimson-Ember)
                        val ripple2 = (rippleCycle + 0.5f) % 1f
                        val r2 = baseRadius + (size.minDimension * 0.22f) * ripple2
                        drawCircle(
                            color = crimsonBright.copy(alpha = (1f - ripple2) * 0.50f * emblemAlpha.value),
                            radius = r2,
                            center = center,
                            style = Stroke(width = 2.4.dp.toPx() * (1f - ripple2 * 0.45f))
                        )

                        // Subtle living outer halo
                        val haloRadius = size.minDimension * 0.44f * glowPulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    crimsonBright.copy(alpha = 0.28f * emblemAlpha.value),
                                    goldAccent.copy(alpha = 0.14f * emblemAlpha.value),
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
                            color = goldAccent.copy(alpha = 0.52f * emblemAlpha.value),
                            startAngle = counterOrbitAngle,
                            sweepAngle = 78f,
                            useCenter = false,
                            topLeft = Offset(innerArcInset, innerArcInset),
                            size = innerArcSize,
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = crimsonBright.copy(alpha = 0.65f * emblemAlpha.value),
                            startAngle = counterOrbitAngle + 180f,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(innerArcInset, innerArcInset),
                            size = innerArcSize,
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Outer orbital sweep gauge arc (0° -> 360°)
                        val outerInset = 14.dp.toPx()
                        val outerSize = Size(size.width - outerInset * 2f, size.height - outerInset * 2f)
                        val sweep = orbitalSweep.value
                        if (sweep > 1f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        crimsonBright,
                                        goldAccent,
                                        warmEmber,
                                        crimsonBright
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

                            // Bright traveling spark dot rotating cleanly around the ring
                            val liveSparkAngle = if (sweep >= 359f) -90f + (360f - counterOrbitAngle) else -90f + sweep
                            val tipAngleRad = (liveSparkAngle * PI / 180.0)
                            val arcRadius = outerSize.width / 2f
                            val sparkPos = Offset(
                                x = center.x + (arcRadius * cos(tipAngleRad)).toFloat(),
                                y = center.y + (arcRadius * sin(tipAngleRad)).toFloat()
                            )
                            drawCircle(
                                color = goldAccent.copy(alpha = 0.45f),
                                radius = 7.dp.toPx(),
                                center = sparkPos
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.2.dp.toPx(),
                                center = sparkPos
                            )
                        }
                    }

                    // Circular Red & Blue Emblem Medallion with Shimmer Sweep
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF092C6C),
                        border = BorderStroke(
                            width = 2.8.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    crimsonBright.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.85f),
                                    royalBlue.copy(alpha = 0.95f)
                                )
                            )
                        ),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .size(150.dp)
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
                                painter = painterResource(id = R.drawable.ic_ca_red_logo),
                                contentDescription = "C&A Gas Animated Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.12f
                                        scaleY = 1.12f
                                    }
                            )

                            // Diagonal specular metallic sheen gliding across the emblem
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val shiftX = size.width * shimmerOffset.value
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.36f),
                                            goldAccent.copy(alpha = 0.24f),
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
                    color = crimsonPrimary,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
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

                // Expanding Crimson -> Gold -> Amber Brand Accent Bar
                Box(
                    modifier = Modifier
                        .width(144.dp)
                        .height(3.dp)
                        .graphicsLayer {
                            scaleX = dividerScaleX.value
                            alpha = dividerScaleX.value
                        }
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(crimsonBright, goldAccent, warmEmber)
                            )
                        )
                )

                Spacer(Modifier.height(10.dp))

                // Dynamic 10-second animated phase subtitle
                AnimatedContent(
                    targetState = phaseMessage,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                    label = "splash_phase_text"
                ) { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.90f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Live audio visualizer bars when launch sound is enabled
                if (soundEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(22.dp)
                            .alpha(footerAlpha.value)
                    ) {
                        val barCount = 9
                        for (index in 0 until barCount) {
                            val amplitude = ((sin(wavePhase + index * 0.75f) + 1f) * 0.5f)
                            val barHeight = (5f + amplitude * 15f).dp
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index % 2 == 0) goldAccent else crimsonBright
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                LinearProgressIndicator(
                    progress = { launchProgress.value },
                    color = goldAccent,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .width(230.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                val secondsLeft = ((1f - launchProgress.value) * STARTUP_LAUNCH_DURATION_SECONDS).coerceAtLeast(0f)
                Text(
                    text = String.format(Locale.US, "Launching workspace... %.1fs", secondsLeft),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.testTag("splash_countdown_label")
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
                        text = "Tap to Skip",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
