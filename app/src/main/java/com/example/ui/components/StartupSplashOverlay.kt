package com.example.ui.components

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundEffectsManager
import com.example.audio.StartupSoundStyle
import com.example.data.model.ThemePreset
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val STARTUP_LAUNCH_DURATION_SECONDS = 4.2f

private data class SplashEnergyParticle(
    val xFactor: Float,
    val yFactor: Float,
    val zDepth: Float,
    val radiusDp: Float,
    val driftSpeed: Float,
    val phaseRad: Float,
    val isMetallicOrange: Boolean
)

/**
 * 4.2-second Signature Cinematic Opening for C&A GAS.
 * Features an Obsidian Midnight Blue & Royal Electric Blue volumetric field, 3D drifting
 * electric-blue & metallic-orange light particles, counter-revolving elliptical light trails,
 * specular metallic reflections over the existing C&A emblem, and an editorial brand reveal.
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean = false,
    startupSoundStyle: StartupSoundStyle = StartupSoundStyle.ROYAL_FANFARE,
    onSplashFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isExiting by remember { mutableStateOf(false) }

    val emblemScale = remember { Animatable(0.44f) }
    val emblemRotation = remember { Animatable(-12f) }
    val emblemAlpha = remember { Animatable(0f) }
    val darknessVeilAlpha = remember { Animatable(0.85f) }
    val orbitalSweep = remember { Animatable(0f) }
    val shimmerOffset = remember { Animatable(-1.5f) }
    val badgeProgress = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val dividerScaleX = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }
    val launchProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    // Pre-allocated deterministic 3D depth particles (zero allocations per frame)
    val particles = remember {
        List(28) { idx ->
            val t = idx / 28f
            SplashEnergyParticle(
                xFactor = ((idx * 37) % 100) / 100f,
                yFactor = ((idx * 53 + 17) % 100) / 100f,
                zDepth = 0.35f + ((idx * 19) % 65) / 100f,
                radiusDp = 1.4f + ((idx * 7) % 4) * 0.85f,
                driftSpeed = 0.6f + (idx % 5) * 0.22f,
                phaseRad = (t * 2.0 * PI).toFloat(),
                isMetallicOrange = idx % 3 == 0
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ca_signature_motion")
    val orbitAngleState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trail_orbit"
    )
    val energyPhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_phase"
    )
    val glowPulseState = infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        SoundEffectsManager.stopStartupSound()
        scope.launch {
            launch {
                exitScale.animateTo(
                    targetValue = 1.045f,
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
        // Commit Frame 1 immediately to SurfaceFlinger before running choreography
        withFrameNanos { }

        // Stage 1: Darkness lifts & C&A emblem emerges with metallic precision
        launch {
            darknessVeilAlpha.animateTo(0f, tween(durationMillis = 850, easing = FastOutSlowInEasing))
        }
        launch {
            emblemAlpha.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            emblemRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Stage 2: Precision orbital ring & metallic specular reflections
        launch {
            delay(180)
            orbitalSweep.animateTo(360f, tween(durationMillis = 1400, easing = FastOutSlowInEasing))
        }
        launch {
            delay(500)
            repeat(2) {
                shimmerOffset.snapTo(-1.5f)
                shimmerOffset.animateTo(1.9f, tween(durationMillis = 980, easing = FastOutSlowInEasing))
                delay(750)
            }
        }

        // Stage 3: Editorial brand name reveal
        launch {
            delay(380)
            badgeProgress.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }
        launch {
            delay(560)
            titleProgress.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
        launch {
            delay(760)
            dividerScaleX.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        }
        launch {
            delay(680)
            footerAlpha.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }

        // Overall 4.2s luxury launch sequence (3.9s progress + 0.3s exit)
        launchProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3900, easing = LinearEasing)
        )
        triggerSmoothExit()
    }

    val phaseMessage by remember {
        derivedStateOf {
            val progress = launchProgress.value
            when {
                progress < 0.34f -> "Initializing C&A Signature Core..."
                progress < 0.68f -> "Synchronizing Store, Fleet & Telemetry..."
                else -> "Ready • Concord & Atoz Gas Trading LLC"
            }
        }
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
                        ObsidianMidnight,
                        ObsidianDeep,
                        RoyalBlueDeep.copy(alpha = 0.85f),
                        ObsidianMidnight
                    )
                )
            )
            .testTag("startup_splash_overlay")
    ) {
        // Volumetric 3D Energy Field, Electric-Blue & Metallic-Orange Light Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowPulse = glowPulseState.value
            val energyPhase = energyPhaseState.value
            val emblemVis = emblemAlpha.value
            val focalCenter = Offset(size.width * 0.5f, size.height * 0.42f)

            // Deep Royal Electric Blue atmospheric dome
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlueBright.copy(alpha = 0.28f * emblemVis),
                        RoyalElectricBlue.copy(alpha = 0.14f * emblemVis),
                        Color.Transparent
                    ),
                    center = focalCenter,
                    radius = size.minDimension * 0.72f * glowPulse
                ),
                radius = size.minDimension * 0.72f * glowPulse,
                center = focalCenter
            )

            // Warm Metallic Orange ambient depth accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MetallicOrangeBright.copy(alpha = 0.18f * emblemVis),
                        MetallicOrange.copy(alpha = 0.06f * emblemVis),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.54f, size.height * 0.46f),
                    radius = size.minDimension * 0.48f * glowPulse
                ),
                radius = size.minDimension * 0.48f * glowPulse,
                center = Offset(size.width * 0.54f, size.height * 0.46f)
            )

            // 3D Energy Light Particles drifting across depth planes
            particles.forEach { p ->
                val drift = sin(energyPhase * p.driftSpeed + p.phaseRad)
                val rise = cos(energyPhase * p.driftSpeed * 0.75f + p.phaseRad)
                val px = (p.xFactor * size.width) + drift * 22.dp.toPx() * p.zDepth
                val py = (p.yFactor * size.height) + rise * 28.dp.toPx() * p.zDepth
                val particleAlpha = (0.30f + 0.55f * p.zDepth) *
                    (0.65f + 0.35f * sin(energyPhase + p.phaseRad)) *
                    (0.35f + 0.65f * emblemVis)
                val particleColor = if (p.isMetallicOrange) MetallicOrangeBright else ElectricBlueGlow
                val baseRadius = p.radiusDp.dp.toPx() * (0.65f + 0.75f * p.zDepth)

                // Soft outer aura
                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha * 0.32f),
                    radius = baseRadius * 3.1f,
                    center = Offset(px, py)
                )
                // Crisp luminous core
                drawCircle(
                    color = if (p.isMetallicOrange) PlatinumSilver.copy(alpha = particleAlpha) else ArcticWhite.copy(alpha = particleAlpha),
                    radius = baseRadius,
                    center = Offset(px, py)
                )
            }
        }

        // Subtle opening darkness veil fading out as logo illuminates
        if (darknessVeilAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darknessVeilAlpha.value * 0.65f))
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
            // Top Minimal Architectural Status Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = footerAlpha.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianMidnight.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PlatinumSilver.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Active Theme",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = themePreset.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArcticWhite
                        )
                    }
                }

                Surface(
                    color = ObsidianMidnight.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.42f))
                ) {
                    Text(
                        text = "C&A ENTERPRISE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumSilver,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Centerpiece 3D Orbital Trails + Existing C&A Gas Logo + Brand Reveal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(252.dp)
                        .testTag("animated_logo_emblem")
                ) {
                    // Revolving 3D Elliptical Light Trails & Precision Telemetry Gauge
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val orbitAngle = orbitAngleState.value
                        val glowPulse = glowPulseState.value
                        val emblemVis = emblemAlpha.value
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Halo illumination behind emblem
                        val haloRadius = size.minDimension * 0.46f * glowPulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricBlueBright.copy(alpha = 0.34f * emblemVis),
                                    MetallicOrangeBright.copy(alpha = 0.16f * emblemVis),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = haloRadius
                            ),
                            radius = haloRadius,
                            center = center
                        )

                        // Revolving 3D Foreshortened Orbital Trail #1 (Electric Blue)
                        val rx = size.width * 0.44f
                        val ry = size.height * 0.24f
                        rotate(degrees = -24f, pivot = center) {
                            drawOval(
                                color = ElectricBlueGlow.copy(alpha = 0.22f * emblemVis),
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f),
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ElectricBlueBright.copy(alpha = 0.88f * emblemVis),
                                        ArcticWhite.copy(alpha = 0.95f * emblemVis),
                                        Color.Transparent
                                    ),
                                    center = center
                                ),
                                startAngle = orbitAngle,
                                sweepAngle = 115f,
                                useCenter = false,
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f),
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val headRad1 = ((orbitAngle + 115f) * PI / 180.0)
                            val headPos1 = Offset(
                                x = center.x + (rx * cos(headRad1)).toFloat(),
                                y = center.y + (ry * sin(headRad1)).toFloat()
                            )
                            drawCircle(
                                color = ElectricBlueGlow.copy(alpha = 0.55f * emblemVis),
                                radius = 6.5.dp.toPx(),
                                center = headPos1
                            )
                            drawCircle(
                                color = ArcticWhite.copy(alpha = emblemVis),
                                radius = 2.6.dp.toPx(),
                                center = headPos1
                            )
                        }

                        // Revolving 3D Foreshortened Orbital Trail #2 (Signature Metallic Orange)
                        rotate(degrees = 26f, pivot = center) {
                            drawOval(
                                color = MetallicOrangeBright.copy(alpha = 0.20f * emblemVis),
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f),
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MetallicOrange.copy(alpha = 0.85f * emblemVis),
                                        MetallicOrangeGold.copy(alpha = 0.95f * emblemVis),
                                        Color.Transparent
                                    ),
                                    center = center
                                ),
                                startAngle = 360f - orbitAngle + 140f,
                                sweepAngle = 105f,
                                useCenter = false,
                                topLeft = Offset(center.x - rx, center.y - ry),
                                size = Size(rx * 2f, ry * 2f),
                                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val headRad2 = (((360f - orbitAngle + 140f) + 105f) * PI / 180.0)
                            val headPos2 = Offset(
                                x = center.x + (rx * cos(headRad2)).toFloat(),
                                y = center.y + (ry * sin(headRad2)).toFloat()
                            )
                            drawCircle(
                                color = MetallicOrangeBright.copy(alpha = 0.55f * emblemVis),
                                radius = 6.5.dp.toPx(),
                                center = headPos2
                            )
                            drawCircle(
                                color = PlatinumSilver.copy(alpha = emblemVis),
                                radius = 2.5.dp.toPx(),
                                center = headPos2
                            )
                        }

                        // Outer architectural precision ring sweep
                        val outerInset = 18.dp.toPx()
                        val outerSize = Size(size.width - outerInset * 2f, size.height - outerInset * 2f)
                        val sweep = orbitalSweep.value
                        if (sweep > 1f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        ElectricBlueBright,
                                        PlatinumSilver,
                                        MetallicOrangeBright,
                                        ElectricBlueBright
                                    ),
                                    center = center
                                ),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(outerInset, outerInset),
                                size = outerSize,
                                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Existing C&A Gas Medallion Emerging with Metallic Platinum-Orange Rim & Specular Reflection
                    Surface(
                        shape = CircleShape,
                        color = ObsidianMidnight,
                        border = BorderStroke(
                            width = 2.6.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    PlatinumSilver.copy(alpha = 0.92f),
                                    ElectricBlueGlow.copy(alpha = 0.85f),
                                    MetallicOrangeBright.copy(alpha = 0.90f),
                                    PlatinumSilver.copy(alpha = 0.75f)
                                )
                            )
                        ),
                        shadowElevation = 20.dp,
                        modifier = Modifier
                            .size(148.dp)
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
                            val splashContext = LocalContext.current
                            val logoBitmap by produceState<ImageBitmap?>(initialValue = null, splashContext) {
                                value = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                                        BitmapFactory.decodeResource(
                                            splashContext.resources,
                                            R.drawable.ic_ca_red_logo,
                                            opts
                                        )?.asImageBitmap()
                                    }.getOrNull()
                                }
                            }
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap!!,
                                    contentDescription = "C&A Gas Animated Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1.12f
                                            scaleY = 1.12f
                                        }
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "C&A",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ArcticWhite
                                    )
                                }
                            }

                            // Metallic Diagonal Specular Reflection Sweep
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val shiftX = size.width * shimmerOffset.value
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            PlatinumSilver.copy(alpha = 0.42f),
                                            MetallicOrangeBright.copy(alpha = 0.24f),
                                            Color.Transparent
                                        ),
                                        start = Offset(shiftX - size.width * 0.48f, 0f),
                                        end = Offset(shiftX + size.width * 0.24f, size.height)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Luxury Badge Pill
                Surface(
                    color = RoyalElectricBlue.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(ElectricBlueGlow.copy(alpha = 0.65f), MetallicOrangeBright.copy(alpha = 0.65f))
                        )
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = badgeProgress.value
                        translationY = (1f - badgeProgress.value) * 16.dp.toPx()
                    }
                ) {
                    Text(
                        text = "C&A GAS  •  INDUSTRIAL & LPG FLEET",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlatinumSilver,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Editorial Brand Name Reveal
                Text(
                    text = "CONCORD & ATOZ\nGAS TRADING LLC",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier
                        .testTag("splash_company_headline")
                        .graphicsLayer {
                            alpha = titleProgress.value
                            translationY = (1f - titleProgress.value) * 20.dp.toPx()
                        }
                )

                Spacer(Modifier.height(10.dp))

                // Illuminated Electric-Blue -> Platinum -> Metallic-Orange Hairline
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(2.5.dp)
                        .graphicsLayer {
                            scaleX = dividerScaleX.value
                            alpha = dividerScaleX.value
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricBlueBright, PlatinumSilver, MetallicOrangeBright)
                            )
                        )
                )

                Spacer(Modifier.height(12.dp))

                AnimatedContent(
                    targetState = phaseMessage,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(220)) },
                    label = "splash_phase_text"
                ) { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlatinumSilver.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Sleek Precision Linear Progress Track
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(PlatinumSilver.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(launchProgress.value.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ElectricBlueBright, ElectricBlueGlow, MetallicOrangeBright)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(8.dp))
                SplashCountdownLabel(progressProvider = { launchProgress.value })
            }

            // Bottom Platinum Signature Badge + Skip Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { alpha = footerAlpha.value }
            ) {
                Surface(
                    color = ObsidianMidnight.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(PlatinumSilver.copy(alpha = 0.3f), MetallicOrangeBright.copy(alpha = 0.45f))
                        )
                    )
                ) {
                    Text(
                        text = "Built by Ragavan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)
                    )
                }
                TextButton(
                    onClick = { triggerSmoothExit() },
                    modifier = Modifier.testTag("splash_skip_button")
                ) {
                    Text(
                        text = "Tap to Skip",
                        color = PlatinumSilver.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashCountdownLabel(progressProvider: () -> Float) {
    val tenthsRemaining by remember {
        derivedStateOf {
            (((1f - progressProvider()) * STARTUP_LAUNCH_DURATION_SECONDS) * 10f)
                .toInt()
                .coerceAtLeast(0)
        }
    }
    Text(
        text = String.format(Locale.US, "Entering C&A Workspace... %.1fs", tenthsRemaining / 10f),
        style = MaterialTheme.typography.labelSmall,
        fontFamily = JetBrainsMono,
        color = PlatinumSilver.copy(alpha = 0.78f),
        modifier = Modifier.testTag("splash_countdown_label")
    )
}
