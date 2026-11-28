package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.ChromeSilverHighlight
import com.example.ui.theme.ChromeSilverMid
import com.example.ui.theme.ChromeSilverShadow
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
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SpaceGlassBorder
import com.example.ui.theme.SpaceGlassElevated
import com.example.ui.theme.SpaceGlassSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Checks Android reduced-motion settings OR low-RAM device class so every 3D futuristic animation
 * degrades cleanly to a crisp lightweight presentation on constrained hardware.
 */
fun isLightweightMotionMode(context: Context): Boolean {
    if (isReducedMotionEnabled(context)) return true
    return runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true
    }.getOrDefault(false)
}

/**
 * Cinematic Midnight Black -> Deep Space Navy atmospheric backdrop with volumetric electric-blue
 * and molten-orange energy lighting, subtle metallic orbital grid arcs, and drifting sparks.
 */
@Composable
fun FuturisticSpaceBackdrop(
    modifier: Modifier = Modifier,
    showEnergyParticles: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lightweight = remember(context) { isLightweightMotionMode(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "space_backdrop")
    val orbitPhase by if (lightweight || !showEnergyParticles) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.25f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbit_phase"
        )
    }

    Box(modifier = modifier.background(CinematicMidnightBlack)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep Space Midnight gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CinematicMidnightBlack,
                        DeepSpaceNavy,
                        Color(0xFF050C1F),
                        CinematicMidnightBlack
                    )
                )
            )

            // Volumetric Electric Blue top-left halo
            val blueCenter = Offset(
                x = w * (0.18f + 0.05f * sin(orbitPhase * 2f * PI).toFloat()),
                y = h * 0.14f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonElectricBlue.copy(alpha = 0.24f),
                        NeonCyanGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = blueCenter,
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = blueCenter
            )

            // Volumetric Molten Metallic Orange bottom-right energy halo
            val orangeCenter = Offset(
                x = w * (0.86f - 0.04f * cos(orbitPhase * 2f * PI).toFloat()),
                y = h * 0.78f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MoltenOrangeGlow.copy(alpha = 0.20f),
                        MoltenOrangeCore.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = orangeCenter,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = orangeCenter
            )

            // Subtle chrome architectural orbital arcs in background
            val arcCenter = Offset(w * 0.5f, h * 0.22f)
            drawCircle(
                color = NeonCyanGlow.copy(alpha = 0.06f),
                radius = w * 0.62f,
                center = arcCenter,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = MoltenOrangeGlow.copy(alpha = 0.05f),
                radius = w * 0.44f,
                center = arcCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Ambient electric-blue & molten-orange floating energy sparks
            if (showEnergyParticles) {
                val sparkCount = if (lightweight) 8 else 18
                for (i in 0 until sparkCount) {
                    val seed = i * 37.3f
                    val px = ((seed * 19.1f) % 100f) / 100f * w
                    val baseY = ((seed * 11.7f) % 100f) / 100f
                    val py = ((baseY - orbitPhase * (0.25f + (i % 3) * 0.1f) + 1f) % 1f) * h
                    val isOrange = i % 3 == 0
                    val sparkColor = if (isOrange) MoltenOrangeCore else NeonCyanGlow
                    val alpha = if (lightweight) 0.25f else (0.18f + 0.35f * sin((orbitPhase * 2f * PI + i).toDouble()).toFloat().coerceAtLeast(0f))
                    val r = (1.4f + (i % 3) * 0.8f).dp.toPx()
                    drawCircle(
                        color = sparkColor.copy(alpha = alpha),
                        radius = r,
                        center = Offset(px, py)
                    )
                }
            }
        }
        content()
    }
}

/**
 * Signature 3D C&A GAS Metallic Emblem.
 * Combines the custom generated 3D metallic C&A GAS emblem (`img_ca_3d_emblem`) with live
 * rotating chrome-silver & electric-blue 3D orbital energy rings, sweeping metallic orange
 * fire trail, subtle 3D perspective float, and volumetric glow.
 */
@Composable
fun Signature3DEmblem(
    size: Dp = 112.dp,
    modifier: Modifier = Modifier,
    enableOrbitAnimation: Boolean = true,
    showOuterHalo: Boolean = true
) {
    val context = LocalContext.current
    val lightweight = remember(context) { isLightweightMotionMode(context) }
    val animate = enableOrbitAnimation && !lightweight

    val infiniteTransition = rememberInfiniteTransition(label = "ca_3d_emblem")
    val ringRotation by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_rotation"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(28f) }
    }

    val reverseRingRotation by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "reverse_ring_rotation"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(210f) }
    }

    val floatPhase by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float_phase"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = floatPhase * 3.5f
                rotationY = floatPhase * 5f
                rotationX = -floatPhase * 3f
                cameraDistance = 16f * density
            }
            .testTag("signature_3d_emblem"),
        contentAlignment = Alignment.Center
    ) {
        // Volumetric halo & rotating metallic orbital energy rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            if (showOuterHalo) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MoltenOrangeGlow.copy(alpha = 0.28f),
                            NeonElectricBlue.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Outer Electric Blue + Chrome Silver orbital ring
            rotate(degrees = ringRotation, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            ChromeSilverHighlight.copy(alpha = 0.9f),
                            NeonCyanGlow,
                            NeonElectricBlue,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.93f, center.y - radius * 0.93f),
                    size = Size(radius * 1.86f, radius * 1.86f),
                    style = Stroke(width = (radius * 0.045f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
                )
            }

            // Counter-rotating Molten Metallic Orange fire trail arc
            rotate(degrees = reverseRingRotation, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            MoltenOrangeGlow.copy(alpha = 0.3f),
                            MoltenOrangeGlow,
                            MoltenOrangeCore,
                            ChromeSilverHighlight,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 40f,
                    sweepAngle = 155f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
                    size = Size(radius * 1.70f, radius * 1.70f),
                    style = Stroke(width = (radius * 0.055f).coerceAtLeast(2.2f), cap = StrokeCap.Round)
                )
            }
        }

        // Core metallic 3D emblem artwork inside circular chrome bezel
        Surface(
            shape = CircleShape,
            color = CinematicMidnightBlack,
            border = BorderStroke(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    listOf(ChromeSilverHighlight, NeonCyanGlow, MoltenOrangeGlow, ChromeSilverMid)
                )
            ),
            shadowElevation = 14.dp,
            modifier = Modifier.size(size * 0.78f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_ca_3d_emblem),
                contentDescription = "C&A GAS 3D Metallic Brand Emblem",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * 3D-rendered metallic gas cylinder graphic for Store & Vehicle inventory cards.
 * Displays realistic brushed cylindrical shading, dome shoulder, pressure collar/valve,
 * and an animated glowing stock level fill indicator.
 */
@Composable
fun Futuristic3DCylinderVisual(
    fullCount: Int,
    emptyCount: Int,
    damagedCount: Int = 0,
    accentColor: Color = ElectricBlueBright,
    modifier: Modifier = Modifier.size(width = 44.dp, height = 64.dp)
) {
    val total = (fullCount + emptyCount + damagedCount).coerceAtLeast(1)
    val fullRatio = (fullCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val emptyRatio = (emptyCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyLeft = w * 0.16f
        val bodyRight = w * 0.84f
        val bodyW = bodyRight - bodyLeft
        val shoulderY = h * 0.27f
        val baseY = h * 0.90f
        val bodyH = baseY - shoulderY

        // Elliptical ground shadow for 3D floating depth
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.94f),
                radius = w * 0.45f
            ),
            topLeft = Offset(w * 0.10f, h * 0.87f),
            size = Size(w * 0.80f, h * 0.12f)
        )

        // Brass/Chrome top valve & safety collar
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(ChromeSilverShadow, ChromeSilverHighlight, ChromeSilverShadow)
            ),
            topLeft = Offset(w * 0.40f, h * 0.04f),
            size = Size(w * 0.20f, h * 0.12f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF1E3A8A), NeonCyanGlow, Color(0xFF0F172A))
            ),
            topLeft = Offset(w * 0.31f, h * 0.13f),
            size = Size(w * 0.38f, h * 0.09f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 3D Domed shoulder
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0E1F47),
                    accentColor.copy(alpha = 0.85f),
                    ChromeSilverHighlight.copy(alpha = 0.8f),
                    accentColor.copy(alpha = 0.55f),
                    Color(0xFF071024)
                ),
                startX = bodyLeft,
                endX = bodyRight
            ),
            topLeft = Offset(bodyLeft, h * 0.17f),
            size = Size(bodyW, h * 0.22f)
        )

        // Main cylindrical 3D metallic shell
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0B1633),
                    Color(0xFF1B346E),
                    Color(0xFF335CA8),
                    Color(0xFF12234B),
                    Color(0xFF060D20)
                ),
                startX = bodyLeft,
                endX = bodyRight
            ),
            topLeft = Offset(bodyLeft, shoulderY),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.18f, bodyW * 0.18f)
        )

        // Inner glowing stock fill levels (Full = Electric Blue / Accent, Empty = Molten Orange)
        val gaugePadding = bodyW * 0.12f
        val gaugeW = bodyW - gaugePadding * 2f
        val gaugeTop = shoulderY + bodyH * 0.12f
        val gaugeBottom = baseY - bodyH * 0.10f
        val gaugeH = (gaugeBottom - gaugeTop).coerceAtLeast(4f)

        // Empty level band (bottom)
        if (emptyCount > 0) {
            val eH = (gaugeH * emptyRatio).coerceAtLeast(3f)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        MetallicOrange.copy(alpha = 0.65f),
                        MoltenOrangeCore,
                        MetallicOrange.copy(alpha = 0.45f)
                    )
                ),
                topLeft = Offset(bodyLeft + gaugePadding, gaugeBottom - eH),
                size = Size(gaugeW, eH),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Full level band (stacked above empty)
        if (fullCount > 0) {
            val fH = (gaugeH * fullRatio).coerceAtLeast(4f)
            val eOffset = if (emptyCount > 0) (gaugeH * emptyRatio) else 0f
            val fTop = (gaugeBottom - eOffset - fH).coerceAtLeast(gaugeTop)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.7f),
                        NeonCyanGlow,
                        accentColor.copy(alpha = 0.5f)
                    )
                ),
                topLeft = Offset(bodyLeft + gaugePadding, fTop),
                size = Size(gaugeW, (gaugeBottom - eOffset - fTop).coerceAtLeast(4f)),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Vertical specular glass/chrome highlight along left ridge
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(bodyLeft + bodyW * 0.18f, shoulderY + 2f),
            size = Size(bodyW * 0.14f, bodyH * 0.84f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Metallic rim border around cylinder
        drawRoundRect(
            color = NeonCyanGlow.copy(alpha = 0.42f),
            topLeft = Offset(bodyLeft, shoulderY),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.18f, bodyW * 0.18f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Animated LPG / Cylinder Exchange Flow Visualizer:
 * Displays live animated energy pulses showing Full Cylinder OUT ➔ Customer ➔ Empty Cylinder IN
 * with the selected Returned Empty Company (Arab Gas / Emirates Gas).
 */
@Composable
fun FuturisticStockFlowVisualizer(
    deliveredLabel: String,
    deliveredQty: Int,
    returnedLabel: String,
    returnedCompany: String,
    returnedQty: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lightweight = remember(context) { isLightweightMotionMode(context) }
    val transition = rememberInfiniteTransition(label = "exchange_flow_pulse")
    val pulse by if (lightweight) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "flow_pulse"
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpaceGlassSurface,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonCyanGlow.copy(alpha = 0.65f),
                    SpaceGlassBorder,
                    MoltenOrangeGlow.copy(alpha = 0.72f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("futuristic_exchange_flow_visualizer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Full OUT node
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "FULL OUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyanGlow,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = deliveredLabel.ifBlank { "Select Full" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "-$deliveredQty Full",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlueGlow
                    )
                }

                // Animated energy conduit canvas
                Canvas(
                    modifier = Modifier
                        .width(96.dp)
                        .height(34.dp)
                ) {
                    val midY = size.height / 2f
                    val topY = size.height * 0.28f
                    val botY = size.height * 0.72f

                    // Upper Full OUT stream (Left -> Right, Electric Blue)
                    drawLine(
                        color = NeonCyanGlow.copy(alpha = 0.25f),
                        start = Offset(4f, topY),
                        end = Offset(size.width - 4f, topY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val blueDotX = 6f + (size.width - 12f) * pulse
                    drawCircle(
                        color = NeonCyanGlow,
                        radius = 3.5.dp.toPx(),
                        center = Offset(blueDotX, topY)
                    )

                    // Lower Empty IN stream (Right -> Left, Molten Orange)
                    drawLine(
                        color = MoltenOrangeGlow.copy(alpha = 0.25f),
                        start = Offset(4f, botY),
                        end = Offset(size.width - 4f, botY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val orangeDotX = (size.width - 6f) - (size.width - 12f) * pulse
                    drawCircle(
                        color = MoltenOrangeGlow,
                        radius = 3.5.dp.toPx(),
                        center = Offset(orangeDotX, botY)
                    )

                    // Center diamond node
                    val cx = size.width / 2f
                    val diamond = Path().apply {
                        moveTo(cx, midY - 6.dp.toPx())
                        lineTo(cx + 6.dp.toPx(), midY)
                        lineTo(cx, midY + 6.dp.toPx())
                        lineTo(cx - 6.dp.toPx(), midY)
                        close()
                    }
                    drawPath(diamond, color = SpaceGlassElevated)
                    drawPath(diamond, color = ChromeSilverHighlight.copy(alpha = 0.7f), style = Stroke(1.2.dp.toPx()))
                }

                // Empty IN node
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "EMPTY IN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MoltenOrangeCore,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = buildString {
                            if (returnedCompany.isNotBlank()) append("$returnedCompany • ")
                            append(returnedLabel.ifBlank { "Select Empty" })
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "+$returnedQty Empty",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = MetallicOrangeBright
                    )
                }
            }
            Text(
                text = "Instant Stock Sync • Full deducted from delivered size, Empty added to returned gas",
                style = MaterialTheme.typography.labelSmall,
                color = PlatinumMuted,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Depth-layered holographic KPI pill for futuristic dashboards.
 */
@Composable
fun Futuristic3DKpiTile(
    label: String,
    value: Int,
    subtitle: String? = null,
    accentColor: Color = ElectricBlueBright,
    modifier: Modifier = Modifier
) {
    LuxuryGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        accentGlow = accentColor,
        forceMidnight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PlatinumSilver,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
            }
            AnimatedStockCounter(
                count = value,
                style = MaterialTheme.typography.headlineSmall,
                color = ArcticWhite,
                fontWeight = FontWeight.ExtraBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}
