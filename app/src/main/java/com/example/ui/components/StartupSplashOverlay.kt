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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffectsManager
import com.example.audio.StartupSoundStyle
import com.example.data.model.ThemePreset
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.ChromeSilverHighlight
import com.example.ui.theme.ChromeSilverMid
import com.example.ui.theme.CinematicMidnightBlack
import com.example.ui.theme.DeepSpaceNavy
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MoltenOrangeCore
import com.example.ui.theme.MoltenOrangeGlow
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonElectricBlue
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.PlatinumSilver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val STARTUP_LAUNCH_DURATION_SECONDS = 4.8f

private data class CinematicSpark(
    val xFactor: Float,
    val yFactor: Float,
    val angleRad: Float,
    val burstDist: Float,
    val radiusDp: Float,
    val isOrange: Boolean
)

/**
 * C&A GAS V4.0 — Next-Generation 4.8-second 3D Cinematic WOW App Opening:
 * 1. Pure midnight black void with electric-blue & molten-orange energy particles igniting.
 * 2. Dynamic molten-orange plasma energy trails sweep across the scene with motion-blur streaks.
 * 3. Rotating chrome-silver & electric-blue metallic 3D orbital rings lock into alignment.
 * 4. Powerful 3D C&A GAS metallic emblem reveal with particle explosion burst & specular flash.
 * 5. Premium 3D C&A GAS delivery truck drives through the scene with bouncing cylinders & light trails.
 * 6. Glowing metallic "C&A GAS" brand headline appears with subtle camera push-in zoom.
 * 7. Smooth fade transition into Login / Workspace.
 * Includes visible Mute/Unmute sound control + Skip Animation button, and automatically adapts
 * to reduced-motion and low-RAM devices.
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean = false,
    startupSoundStyle: StartupSoundStyle = StartupSoundStyle.ROYAL_FANFARE,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val lightweightMode = remember(context) { isLightweightMotionMode(context) }
    val scope = rememberCoroutineScope()
    var isExiting by remember { mutableStateOf(false) }
    var isMuted by remember(soundEnabled) { mutableStateOf(!soundEnabled) }

    val particleIgnition = remember { Animatable(if (lightweightMode) 1f else 0f) }
    val fireSweep = remember { Animatable(if (lightweightMode) 1f else 0f) }
    val orbitalLock = remember { Animatable(if (lightweightMode) 1f else 0f) }
    val emblemScale = remember { Animatable(if (lightweightMode) 1f else 0.36f) }
    val emblemAlpha = remember { Animatable(if (lightweightMode) 1f else 0f) }
    val burstProgress = remember { Animatable(if (lightweightMode) 0f else 0f) }
    val cameraZoom = remember { Animatable(if (lightweightMode) 1f else 0.94f) }
    val titleAlpha = remember { Animatable(if (lightweightMode) 1f else 0f) }
    val launchProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    // 3D Delivery Truck Choreography Animatables
    val truckX = remember { Animatable(if (lightweightMode) 0.50f else -0.42f) }
    val brakePitch = remember { Animatable(0f) }
    val cylinderBounce = remember { Animatable(0f) }
    val lightTrail = remember { Animatable(if (lightweightMode) 0.75f else 0f) }
    val wheelRotation = remember { Animatable(0f) }
    val brakeGlow = remember { Animatable(0f) }

    val sparks = remember {
        List(28) { idx ->
            val angle = (idx / 28f) * (2.0 * PI).toFloat()
            CinematicSpark(
                xFactor = ((idx * 37 + 13) % 100) / 100f,
                yFactor = ((idx * 53 + 19) % 100) / 100f,
                angleRad = angle,
                burstDist = 0.24f + ((idx * 17) % 55) / 100f,
                radiusDp = 1.5f + (idx % 4) * 0.75f,
                isOrange = idx % 2 == 0
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ca_v4_cinematic_ambient")
    val ambientAngle by if (lightweightMode) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_angle"
        )
    }

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        SoundEffectsManager.stopStartupSound()
        scope.launch {
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = if (lightweightMode) 120 else 320, easing = FastOutSlowInEasing)
            )
            onSplashFinished()
        }
    }

    LaunchedEffect(lightweightMode) {
        withFrameNanos { }

        if (lightweightMode) {
            launchProgress.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
            triggerSmoothExit()
            return@LaunchedEffect
        }

        // Play optional subtle startup chime only if unmuted
        if (!isMuted) {
            runCatching { SoundEffectsManager.playStartupChime(enabled = true, style = startupSoundStyle) }
        }

        // Master 4.5s progress timeline (+ 0.3s fade exit = ~4.8s total)
        launch {
            launchProgress.animateTo(1f, tween(durationMillis = 4500, easing = LinearEasing))
        }
        // Subtle cinematic camera push-in across entire opening
        launch {
            cameraZoom.animateTo(1.03f, tween(durationMillis = 4400, easing = FastOutSlowInEasing))
        }

        // Stage 1: Midnight void -> electric-blue & orange energy particles ignite
        launch { particleIgnition.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }

        // Stage 2: Sweeping molten orange energy trails cross the screen
        delay(220)
        launch { fireSweep.animateTo(1f, tween(820, easing = FastOutSlowInEasing)) }

        // Stage 3: Metallic silver & electric-blue orbital rings rotate into position
        delay(280)
        launch { orbitalLock.animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }

        // Stage 4: Powerful 3D C&A GAS logo emerges with particle explosion burst
        delay(280)
        launch { emblemAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch { burstProgress.animateTo(1f, tween(780, easing = FastOutSlowInEasing)) }

        // Stage 5: Metallic glowing C&A GAS brand name reveals
        delay(280)
        launch { titleAlpha.animateTo(1f, tween(460, easing = FastOutSlowInEasing)) }

        // Stage 6: Premium 3D gas delivery truck drives through the scene
        delay(240)
        launch { wheelRotation.animateTo(880f, tween(1050, easing = FastOutSlowInEasing)) }
        launch { lightTrail.animateTo(0.48f, tween(650)) }
        truckX.animateTo(0.50f, tween(1050, easing = FastOutSlowInEasing))

        // Gentle brake + playful cylinder bounce + electric/orange plasma trails peak
        launch { brakeGlow.animateTo(1f, tween(150)) }
        brakePitch.animateTo(2.5f, tween(150, easing = FastOutSlowInEasing))
        brakePitch.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        launch { brakeGlow.animateTo(0f, tween(180)) }
        cylinderBounce.animateTo(1f, tween(620, easing = LinearEasing))
        lightTrail.animateTo(1f, tween(260, easing = FastOutSlowInEasing))

        // Truck accelerates smoothly away with motion streaks
        launch { wheelRotation.animateTo(1760f, tween(760, easing = FastOutSlowInEasing)) }
        truckX.animateTo(1.48f, tween(760, easing = FastOutSlowInEasing))

        triggerSmoothExit()
    }

    val phaseMessage by remember {
        derivedStateOf {
            val p = launchProgress.value
            when {
                p < 0.24f -> "Igniting 3D C&A Energy Core..."
                p < 0.50f -> "C&A GAS • Next-Generation 3D Platform"
                p < 0.78f -> "Dispatching Fleet & Live Cylinder Telemetry..."
                else -> "Welcome • Concord & Atoz Gas Trading LLC"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(CinematicMidnightBlack)
            .testTag("startup_splash_overlay")
    ) {
        // Cinematic 3D lighting, sweeping orange fire trails, rotating metallic orbital rings & particle burst
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = cameraZoom.value
                    scaleY = cameraZoom.value
                }
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.30f)

            // Deep Space Midnight gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CinematicMidnightBlack,
                        DeepSpaceNavy.copy(alpha = 0.85f * particleIgnition.value.coerceAtLeast(0.25f)),
                        Color(0xFF061028),
                        CinematicMidnightBlack
                    )
                )
            )

            // Volumetric blue + molten orange studio lighting behind emblem
            val lightingAlpha = particleIgnition.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonElectricBlue.copy(alpha = 0.34f * lightingAlpha),
                        NeonCyanGlow.copy(alpha = 0.12f * lightingAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.68f
                ),
                radius = size.minDimension * 0.68f,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MoltenOrangeGlow.copy(alpha = 0.26f * fireSweep.value),
                        MoltenOrangeCore.copy(alpha = 0.08f * fireSweep.value),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.54f, h * 0.58f),
                    radius = size.minDimension * 0.54f
                ),
                radius = size.minDimension * 0.54f,
                center = Offset(w * 0.54f, h * 0.58f)
            )

            // Stage 2: Dynamic sweeping molten orange fire trails & motion blur streaks
            val sweep = fireSweep.value
            if (sweep > 0.01f && !lightweightMode) {
                val trailAlpha = sin(sweep * PI).toFloat().coerceIn(0f, 1f) * 0.85f
                val sweepStartX = -w * 0.2f + w * 1.15f * sweep
                val trailPath = Path().apply {
                    moveTo(sweepStartX - w * 0.55f, h * 0.42f)
                    quadraticTo(sweepStartX - w * 0.15f, h * 0.24f, sweepStartX, h * 0.30f)
                }
                drawPath(
                    path = trailPath,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, MoltenOrangeGlow.copy(alpha = trailAlpha), ChromeSilverHighlight.copy(alpha = trailAlpha))
                    ),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                val blueStreakX = w * 1.2f - w * 1.25f * sweep
                val bluePath = Path().apply {
                    moveTo(blueStreakX + w * 0.48f, h * 0.18f)
                    quadraticTo(blueStreakX + w * 0.12f, h * 0.34f, blueStreakX, h * 0.28f)
                }
                drawPath(
                    path = bluePath,
                    brush = Brush.horizontalGradient(
                        listOf(ChromeSilverHighlight.copy(alpha = trailAlpha), NeonCyanGlow.copy(alpha = trailAlpha), Color.Transparent)
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Stage 3: Rotating silver & electric-blue metallic orbital rings around center
            val ringScale = orbitalLock.value
            if (ringScale > 0.02f) {
                val outerR = size.minDimension * (0.44f - 0.11f * ringScale)
                rotate(degrees = ambientAngle * 1.1f + (1f - ringScale) * 180f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                ChromeSilverHighlight.copy(alpha = 0.72f * ringScale),
                                NeonCyanGlow.copy(alpha = 0.85f * ringScale),
                                NeonElectricBlue.copy(alpha = 0.45f * ringScale),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerR, center.y - outerR),
                        size = Size(outerR * 2f, outerR * 2f),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                val innerR = size.minDimension * (0.36f - 0.09f * ringScale)
                rotate(degrees = -ambientAngle * 1.4f - (1f - ringScale) * 140f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                MoltenOrangeGlow.copy(alpha = 0.88f * ringScale),
                                MoltenOrangeCore.copy(alpha = 0.92f * ringScale),
                                ChromeSilverMid.copy(alpha = 0.55f * ringScale),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 60f,
                        sweepAngle = 165f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerR, center.y - innerR),
                        size = Size(innerR * 2f, innerR * 2f),
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Stage 4: Particle explosion burst & ambient energy sparks
            if (!lightweightMode) {
                val burst = burstProgress.value
                sparks.forEach { sp ->
                    val color = if (sp.isOrange) MoltenOrangeCore else NeonCyanGlow
                    // Ambient drifting sparks
                    val ax = sp.xFactor * w
                    val ay = sp.yFactor * h
                    val sparkAlpha = particleIgnition.value * 0.45f
                    drawCircle(
                        color = color.copy(alpha = sparkAlpha),
                        radius = sp.radiusDp.dp.toPx(),
                        center = Offset(ax, ay)
                    )

                    // Emblem reveal burst particles
                    if (burst > 0.01f && burst < 0.98f) {
                        val bDist = size.minDimension * sp.burstDist * burst
                        val bx = center.x + cos(sp.angleRad) * bDist
                        val by = center.y + sin(sp.angleRad) * bDist
                        val bAlpha = (1f - burst) * 0.92f
                        val br = (sp.radiusDp * (1.25f - 0.6f * burst)).dp.toPx()
                        drawCircle(color = color.copy(alpha = bAlpha * 0.38f), radius = br * 2.6f, center = Offset(bx, by))
                        drawCircle(color = ArcticWhite.copy(alpha = bAlpha), radius = br, center = Offset(bx, by))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Controls: Sound Mute Toggle + Visible Skip Animation button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianMidnight.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, NeonCyanGlow.copy(alpha = 0.42f))
                ) {
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            if (isMuted) {
                                SoundEffectsManager.stopStartupSound()
                            } else {
                                runCatching { SoundEffectsManager.playStartupChime(enabled = true, style = startupSoundStyle) }
                            }
                        },
                        modifier = Modifier.testTag("splash_sound_mute_toggle")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute opening sound" else "Mute opening sound",
                            tint = if (isMuted) PlatinumSilver else MoltenOrangeCore,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { triggerSmoothExit() },
                    border = BorderStroke(
                        1.1.dp,
                        Brush.horizontalGradient(listOf(NeonCyanGlow.copy(alpha = 0.7f), MoltenOrangeGlow.copy(alpha = 0.85f)))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianMidnight.copy(alpha = 0.72f),
                        contentColor = ArcticWhite
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.testTag("splash_skip_button")
                ) {
                    Text(
                        text = "Skip Animation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Animation",
                        tint = MoltenOrangeCore,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Centerpiece: Signature 3D Emblem + Metallic Brand Reveal + 3D Delivery Truck Stage
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .testTag("animated_logo_emblem")
                        .graphicsLayer {
                            alpha = emblemAlpha.value
                            scaleX = emblemScale.value
                            scaleY = emblemScale.value
                        }
                ) {
                    Signature3DEmblem(
                        size = 138.dp,
                        enableOrbitAnimation = !lightweightMode,
                        showOuterHalo = true
                    )
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    color = DeepSpaceNavy.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(ChromeSilverHighlight.copy(alpha = 0.55f), NeonCyanGlow.copy(alpha = 0.75f), MoltenOrangeGlow.copy(alpha = 0.85f))
                        )
                    ),
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                ) {
                    Text(
                        text = "C&A GAS  •  NEXT-GEN 3D EDITION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChromeSilverHighlight,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "C&A GAS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MoltenOrangeCore,
                    letterSpacing = 3.2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                )
                Text(
                    text = "CONCORD & ATOZ GAS TRADING LLC",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArcticWhite,
                    letterSpacing = 1.0.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("splash_company_headline")
                        .graphicsLayer { alpha = titleAlpha.value }
                )

                Spacer(Modifier.height(10.dp))

                // Premium 3D C&A GAS Delivery Truck driving through with plasma trails
                CinematicDeliveryTruckStage(
                    truckXFraction = truckX.value,
                    brakePitchDeg = brakePitch.value,
                    cylinderBouncePhase = cylinderBounce.value,
                    lightTrailAlpha = lightTrail.value,
                    wheelRotationDeg = wheelRotation.value,
                    brakeLightsAlpha = brakeGlow.value,
                    style = TruckAnimationStyle.LUXURY_SPLASH
                )

                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = phaseMessage,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                    label = "splash_phase_text"
                ) { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlatinumSilver.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Glowing Energy Progress Bar
                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(ChromeSilverMid.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(launchProgress.value.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ElectricBlueBright, NeonCyanGlow, MoltenOrangeGlow, ChromeSilverHighlight)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(6.dp))
                SplashCountdownLabel(progressProvider = { launchProgress.value })
            }

            // Bottom Metallic Signature Pill
            Surface(
                color = ObsidianMidnight.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(ChromeSilverMid.copy(alpha = 0.4f), NeonCyanGlow.copy(alpha = 0.45f), MoltenOrangeGlow.copy(alpha = 0.55f))
                    )
                )
            ) {
                Text(
                    text = "Built by Ragavan  •  ${themePreset.title}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArcticWhite,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
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
        color = PlatinumSilver.copy(alpha = 0.80f),
        modifier = Modifier.testTag("splash_countdown_label")
    )
}
