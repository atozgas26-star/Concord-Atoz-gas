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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

const val STARTUP_LAUNCH_DURATION_SECONDS = 3.8f

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
 * 3.8-second C&A GAS V2.3 Luxury Cinematic Delivery Truck Opening Animation:
 * 1. Displays C&A GAS logo on Midnight Royal Blue background.
 * 2. Cute, premium 3D C&A GAS delivery truck drives smoothly across the screen.
 * 3. Truck gently brakes and stops.
 * 4. Gas cylinders on the truck bounce playfully.
 * 5. Electric Blue & Metallic Orange light trails appear.
 * 6. Truck drives away and smoothly reveals Login / Dashboard.
 * Includes a Skip Animation button and respects Android reduced-motion settings.
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean = false,
    startupSoundStyle: StartupSoundStyle = StartupSoundStyle.ROYAL_FANFARE,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val scope = rememberCoroutineScope()
    var isExiting by remember { mutableStateOf(false) }

    val emblemScale = remember { Animatable(if (reducedMotion) 1f else 0.56f) }
    val emblemAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val titleAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val launchProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    // 3D Delivery Truck Choreography Animatables
    val truckX = remember { Animatable(if (reducedMotion) 0.50f else -0.38f) }
    val brakePitch = remember { Animatable(0f) }
    val cylinderBounce = remember { Animatable(0f) }
    val lightTrail = remember { Animatable(if (reducedMotion) 0.75f else 0f) }
    val wheelRotation = remember { Animatable(0f) }
    val brakeGlow = remember { Animatable(0f) }

    val particles = remember {
        List(20) { idx ->
            val t = idx / 20f
            SplashEnergyParticle(
                xFactor = ((idx * 37) % 100) / 100f,
                yFactor = ((idx * 53 + 17) % 100) / 100f,
                zDepth = 0.35f + ((idx * 19) % 65) / 100f,
                radiusDp = 1.4f + ((idx * 7) % 4) * 0.7f,
                driftSpeed = 0.55f + (idx % 4) * 0.2f,
                phaseRad = (t * 2.0 * PI).toFloat(),
                isMetallicOrange = idx % 3 == 0
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ca_v23_ambient")
    val energyPhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_phase"
    )
    val glowPulseState = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    fun triggerSmoothExit() {
        if (isExiting) return
        isExiting = true
        SoundEffectsManager.stopStartupSound()
        scope.launch {
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = if (reducedMotion) 120 else 280, easing = FastOutSlowInEasing)
            )
            onSplashFinished()
        }
    }

    LaunchedEffect(reducedMotion) {
        withFrameNanos { }

        if (reducedMotion) {
            launchProgress.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
            triggerSmoothExit()
            return@LaunchedEffect
        }

        // Progress bar across 3.55s (+ 0.28s fade exit = ~3.8s total)
        launch {
            launchProgress.animateTo(1f, tween(durationMillis = 3550, easing = LinearEasing))
        }

        // Step 1: Display C&A GAS logo & title on Midnight Royal Blue
        launch {
            emblemAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(220)
            titleAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }

        // Step 2: 3D C&A GAS delivery truck drives smoothly onto screen
        delay(320)
        launch { wheelRotation.animateTo(820f, tween(1120, easing = FastOutSlowInEasing)) }
        launch { lightTrail.animateTo(0.42f, tween(700)) }
        truckX.animateTo(0.50f, tween(1120, easing = FastOutSlowInEasing))

        // Step 3: Truck gently brakes and stops
        launch { brakeGlow.animateTo(1f, tween(160)) }
        brakePitch.animateTo(2.6f, tween(160, easing = FastOutSlowInEasing))
        brakePitch.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        launch { brakeGlow.animateTo(0f, tween(200)) }

        // Step 4: Gas cylinders bounce playfully
        cylinderBounce.animateTo(1f, tween(680, easing = LinearEasing))

        // Step 5: Electric Blue & Metallic Orange light trails intensify
        lightTrail.animateTo(1f, tween(280, easing = FastOutSlowInEasing))

        // Step 6: Truck drives smoothly away and transitions into dashboard/login
        launch { wheelRotation.animateTo(1640f, tween(760, easing = FastOutSlowInEasing)) }
        truckX.animateTo(1.45f, tween(760, easing = FastOutSlowInEasing))

        triggerSmoothExit()
    }

    val phaseMessage by remember {
        derivedStateOf {
            val p = launchProgress.value
            when {
                p < 0.30f -> "C&A GAS • Luxury Premium Edition"
                p < 0.62f -> "Preparing Store Yard & Delivery Fleet..."
                else -> "Ready • Concord & Atoz Gas Trading LLC"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(
                Brush.verticalGradient(
                    listOf(
                        ObsidianMidnight,
                        ObsidianDeep,
                        RoyalBlueDeep.copy(alpha = 0.88f),
                        ObsidianMidnight
                    )
                )
            )
            .testTag("startup_splash_overlay")
    ) {
        // Volumetric Midnight Royal Blue & Metallic Orange ambient field
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowPulse = if (reducedMotion) 1f else glowPulseState.value
            val energyPhase = if (reducedMotion) 0f else energyPhaseState.value
            val focalCenter = Offset(size.width * 0.5f, size.height * 0.30f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlueBright.copy(alpha = 0.26f),
                        RoyalElectricBlue.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = focalCenter,
                    radius = size.minDimension * 0.68f * glowPulse
                ),
                radius = size.minDimension * 0.68f * glowPulse,
                center = focalCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MetallicOrangeBright.copy(alpha = 0.16f),
                        MetallicOrange.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.56f, size.height * 0.62f),
                    radius = size.minDimension * 0.50f * glowPulse
                ),
                radius = size.minDimension * 0.50f * glowPulse,
                center = Offset(size.width * 0.56f, size.height * 0.62f)
            )

            if (!reducedMotion) {
                particles.forEach { p ->
                    val drift = sin(energyPhase * p.driftSpeed + p.phaseRad)
                    val rise = cos(energyPhase * p.driftSpeed * 0.7f + p.phaseRad)
                    val px = (p.xFactor * size.width) + drift * 18.dp.toPx() * p.zDepth
                    val py = (p.yFactor * size.height) + rise * 20.dp.toPx() * p.zDepth
                    val alpha = (0.28f + 0.50f * p.zDepth) * (0.65f + 0.35f * sin(energyPhase + p.phaseRad))
                    val color = if (p.isMetallicOrange) MetallicOrangeBright else ElectricBlueGlow
                    val r = p.radiusDp.dp.toPx() * (0.65f + 0.65f * p.zDepth)

                    drawCircle(color = color.copy(alpha = alpha * 0.30f), radius = r * 2.8f, center = Offset(px, py))
                    drawCircle(color = ArcticWhite.copy(alpha = alpha), radius = r, center = Offset(px, py))
                }
            }
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
            // Top Luxury Bar with Theme Pill + Skip Animation button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianMidnight.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, PlatinumSilver.copy(alpha = 0.24f))
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

                OutlinedButton(
                    onClick = { triggerSmoothExit() },
                    border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianMidnight.copy(alpha = 0.65f),
                        contentColor = ArcticWhite
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("splash_skip_button")
                ) {
                    Text(
                        text = "Skip Animation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Animation",
                        tint = MetallicOrangeBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Centerpiece: C&A GAS Medallion + Editorial Headline + 3D Delivery Truck Stage
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianMidnight,
                    border = BorderStroke(
                        width = 2.6.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                PlatinumSilver.copy(alpha = 0.92f),
                                ElectricBlueGlow.copy(alpha = 0.88f),
                                MetallicOrangeBright.copy(alpha = 0.92f)
                            )
                        )
                    ),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .size(116.dp)
                        .testTag("animated_logo_emblem")
                        .graphicsLayer {
                            alpha = emblemAlpha.value
                            scaleX = emblemScale.value
                            scaleY = emblemScale.value
                        }
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
                            contentDescription = "C&A Gas Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .graphicsLayer {
                                    scaleX = 1.12f
                                    scaleY = 1.12f
                                }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "C&A",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticWhite
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    color = RoyalElectricBlue.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(ElectricBlueGlow.copy(alpha = 0.65f), MetallicOrangeBright.copy(alpha = 0.65f))
                        )
                    ),
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                ) {
                    Text(
                        text = "C&A GAS  •  LUXURY PREMIUM V2.3",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlatinumSilver,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "CONCORD & ATOZ\nGAS TRADING LLC",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 29.sp,
                    modifier = Modifier
                        .testTag("splash_company_headline")
                        .graphicsLayer { alpha = titleAlpha.value }
                )

                Spacer(Modifier.height(10.dp))

                // 3D C&A GAS Delivery Truck with braking, bouncing cylinders, and Blue & Orange trails
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlatinumSilver.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(6.dp))
                SplashCountdownLabel(progressProvider = { launchProgress.value })
            }

            // Bottom Signature Badge
            Surface(
                color = ObsidianMidnight.copy(alpha = 0.68f),
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
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
        color = PlatinumSilver.copy(alpha = 0.78f),
        modifier = Modifier.testTag("splash_countdown_label")
    )
}
