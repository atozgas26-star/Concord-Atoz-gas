package com.example.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Checks Android system reduced-motion setting (`ANIMATOR_DURATION_SCALE == 0f`).
 */
fun isReducedMotionEnabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)
}

enum class TruckAnimationStyle {
    LUXURY_SPLASH,
    OWNER_EXECUTIVE,
    ADMIN_COMMAND,
    STAFF_DELIVERY
}

/**
 * Custom lightweight 3D-styled C&A GAS delivery truck drawn on Compose Canvas.
 * Supports smooth drive-in, gentle braking pitch, playful bouncing cylinders,
 * glowing Electric Blue & Metallic Orange speed trails, and smooth exit drive-away.
 */
@Composable
fun CinematicDeliveryTruckStage(
    truckXFraction: Float,
    brakePitchDeg: Float,
    cylinderBouncePhase: Float,
    lightTrailAlpha: Float,
    wheelRotationDeg: Float,
    brakeLightsAlpha: Float,
    style: TruckAnimationStyle = TruckAnimationStyle.LUXURY_SPLASH,
    modifier: Modifier = Modifier
) {
    val primaryAccent = when (style) {
        TruckAnimationStyle.OWNER_EXECUTIVE -> Color(0xFFFFD166)
        TruckAnimationStyle.ADMIN_COMMAND -> ElectricBlueBright
        TruckAnimationStyle.STAFF_DELIVERY -> Color(0xFF14B8A6)
        TruckAnimationStyle.LUXURY_SPLASH -> ElectricBlueBright
    }
    val secondaryAccent = when (style) {
        TruckAnimationStyle.OWNER_EXECUTIVE -> MetallicOrangeGold
        TruckAnimationStyle.ADMIN_COMMAND -> MetallicOrangeBright
        TruckAnimationStyle.STAFF_DELIVERY -> MetallicOrangeBright
        TruckAnimationStyle.LUXURY_SPLASH -> MetallicOrangeBright
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .testTag("cinematic_delivery_truck_canvas")
    ) {
        val roadY = size.height * 0.80f
        val truckWidth = (size.width * 0.56f).coerceIn(190.dp.toPx(), 268.dp.toPx())
        val truckHeight = truckWidth * 0.44f
        val centerX = size.width * truckXFraction
        val truckLeft = centerX - truckWidth / 2f
        val truckBottom = roadY - 4.dp.toPx()

        // 1. Sleek illuminated Midnight Highway Horizon & Reflections
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    primaryAccent.copy(alpha = 0.45f),
                    secondaryAccent.copy(alpha = 0.55f),
                    primaryAccent.copy(alpha = 0.45f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, roadY),
            end = Offset(size.width, roadY),
            strokeWidth = 2.dp.toPx()
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    primaryAccent.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                startY = roadY,
                endY = size.height
            ),
            topLeft = Offset(0f, roadY),
            size = Size(size.width, size.height - roadY)
        )

        // 2. Electric Blue & Metallic Orange Light Trails behind truck
        if (lightTrailAlpha > 0.01f) {
            val trailStart = (truckLeft - 140.dp.toPx()).coerceAtLeast(0f)
            val trailEnd = (truckLeft + truckWidth * 0.30f).coerceAtLeast(trailStart + 24f)
            val streakYs = listOf(
                truckBottom - truckHeight * 0.84f,
                truckBottom - truckHeight * 0.56f,
                truckBottom - truckHeight * 0.30f
            )
            streakYs.forEachIndexed { idx, sy ->
                val isOrange = idx % 2 == 1
                val streakColor = if (isOrange) secondaryAccent else primaryAccent
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            streakColor.copy(alpha = 0.45f * lightTrailAlpha),
                            streakColor.copy(alpha = 0.92f * lightTrailAlpha),
                            ArcticWhite.copy(alpha = 0.65f * lightTrailAlpha)
                        ),
                        startX = trailStart,
                        endX = trailEnd
                    ),
                    start = Offset(trailStart, sy),
                    end = Offset(trailEnd, sy),
                    strokeWidth = (if (idx == 1) 4.2.dp else 2.6.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }
            // Ambient glow under truck
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        primaryAccent.copy(alpha = 0.32f * lightTrailAlpha),
                        secondaryAccent.copy(alpha = 0.22f * lightTrailAlpha),
                        Color.Transparent
                    ),
                    center = Offset(centerX, roadY + 2.dp.toPx()),
                    radius = truckWidth * 0.72f
                ),
                topLeft = Offset(centerX - truckWidth * 0.65f, roadY - 12.dp.toPx()),
                size = Size(truckWidth * 1.3f, 24.dp.toPx())
            )
        }

        // Ground shadow beneath the truck
        drawOval(
            color = Color.Black.copy(alpha = 0.46f),
            topLeft = Offset(truckLeft + truckWidth * 0.05f, roadY - 6.dp.toPx()),
            size = Size(truckWidth * 0.90f, 12.dp.toPx())
        )

        // 3. Truck Chassis, Bouncing Cylinders, Branded Cargo Bed & 3D Cab (with subtle braking pitch)
        val wheelRadius = truckHeight * 0.22f
        val rearWheelCenter = Offset(truckLeft + truckWidth * 0.24f, truckBottom - wheelRadius)
        val frontWheelCenter = Offset(truckLeft + truckWidth * 0.78f, truckBottom - wheelRadius)

        withTransform({
            rotate(degrees = brakePitchDeg, pivot = frontWheelCenter)
        }) {
            val deckBottom = truckBottom - wheelRadius * 1.15f
            val cargoWidth = truckWidth * 0.64f
            val cargoHeight = truckHeight * 0.66f
            val cargoTop = deckBottom - cargoHeight
            val cargoLeft = truckLeft + truckWidth * 0.04f

            // Headlight beam cone extending forward
            val headlightPos = Offset(truckLeft + truckWidth * 0.97f, deckBottom - truckHeight * 0.22f)
            val beamPath = Path().apply {
                moveTo(headlightPos.x, headlightPos.y - 4.dp.toPx())
                lineTo(headlightPos.x + 88.dp.toPx(), headlightPos.y - 22.dp.toPx())
                lineTo(headlightPos.x + 88.dp.toPx(), headlightPos.y + 18.dp.toPx())
                lineTo(headlightPos.x, headlightPos.y + 5.dp.toPx())
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFF7D6).copy(alpha = 0.48f),
                        primaryAccent.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startX = headlightPos.x,
                    endX = headlightPos.x + 88.dp.toPx()
                )
            )

            // Playful Bouncing Gas Cylinders seated in the rear open upper rack
            val cylinderColors = listOf(
                Pair(ElectricBlueBright, ElectricBlueGlow),      // Oxygen
                Pair(MetallicOrange, MetallicOrangeGold),        // LPG
                Pair(Color(0xFF0EA5E9), Color(0xFF7DD3FC)),      // Argon
                Pair(Color(0xFF94A3B8), PlatinumSilver)          // Nitrogen
            )
            val cylCount = cylinderColors.size
            val cylSlotWidth = (cargoWidth * 0.84f) / cylCount
            val cylWidth = cylSlotWidth * 0.62f
            val cylBodyHeight = cargoHeight * 0.62f
            val baseCylTop = cargoTop - cylBodyHeight * 0.56f

            cylinderColors.forEachIndexed { idx, (mainCol, shineCol) ->
                val phaseOffset = idx * 0.65f
                val bounceAmt = if (cylinderBouncePhase > 0.001f) {
                    val wave = abs(sin((cylinderBouncePhase * PI.toFloat() * 2.2f) - phaseOffset))
                    val amp = if (style == TruckAnimationStyle.STAFF_DELIVERY) 15.dp.toPx() else 10.dp.toPx()
                    wave * amp * (1f - (cylinderBouncePhase * 0.42f).coerceIn(0f, 0.55f))
                } else {
                    0f
                }
                val cx = cargoLeft + cargoWidth * 0.08f + idx * cylSlotWidth + (cylSlotWidth - cylWidth) / 2f
                val cy = baseCylTop - bounceAmt

                // Cylinder valve neck & cap
                drawRoundRect(
                    color = PlatinumSilver,
                    topLeft = Offset(cx + cylWidth * 0.30f, cy - 6.dp.toPx()),
                    size = Size(cylWidth * 0.40f, 7.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = secondaryAccent,
                    topLeft = Offset(cx + cylWidth * 0.22f, cy - 8.dp.toPx()),
                    size = Size(cylWidth * 0.56f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                // 3D Cylindrical Tank Body with metallic vertical highlight
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(mainCol, shineCol, mainCol.copy(alpha = 0.85f)),
                        startX = cx,
                        endX = cx + cylWidth
                    ),
                    topLeft = Offset(cx, cy),
                    size = Size(cylWidth, cylBodyHeight),
                    cornerRadius = CornerRadius(cylWidth * 0.44f, cylWidth * 0.44f)
                )
                // Subtle metallic collar band
                drawLine(
                    color = ArcticWhite.copy(alpha = 0.55f),
                    start = Offset(cx + 2.dp.toPx(), cy + cylBodyHeight * 0.28f),
                    end = Offset(cx + cylWidth - 2.dp.toPx(), cy + cylBodyHeight * 0.28f),
                    strokeWidth = 1.6.dp.toPx()
                )
            }

            // 3D Branded Cargo Bed Container
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A3A7A),
                        RoyalBlueDeep,
                        ObsidianDeep
                    ),
                    startY = cargoTop,
                    endY = deckBottom
                ),
                topLeft = Offset(cargoLeft, cargoTop),
                size = Size(cargoWidth, cargoHeight),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            // Top metallic rail bevel (3D illusion)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(PlatinumSilver.copy(alpha = 0.65f), primaryAccent, secondaryAccent)
                ),
                topLeft = Offset(cargoLeft + 2.dp.toPx(), cargoTop + 1.dp.toPx()),
                size = Size(cargoWidth - 4.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            // Signature Metallic Orange Diagonal/Horizontal Speed Stripe on Cargo Side
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(MetallicOrange, secondaryAccent, MetallicOrangeGold)
                ),
                topLeft = Offset(cargoLeft + 4.dp.toPx(), cargoTop + cargoHeight * 0.68f),
                size = Size(cargoWidth - 8.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            // Cargo Side Border Highlight
            drawRoundRect(
                color = PlatinumSilver.copy(alpha = 0.38f),
                topLeft = Offset(cargoLeft, cargoTop),
                size = Size(cargoWidth, cargoHeight),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // Company Branding "C&A GAS" drawn cleanly on the Cargo Side Panel
            drawBrandTextOnTruck(
                text = "C&A GAS",
                centerX = cargoLeft + cargoWidth * 0.50f,
                baselineY = cargoTop + cargoHeight * 0.52f,
                textSizePx = (cargoHeight * 0.34f).coerceIn(12.dp.toPx(), 18.dp.toPx()),
                isGold = style == TruckAnimationStyle.OWNER_EXECUTIVE
            )

            // Rear Brake Light Glow
            if (brakeLightsAlpha > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFF3B30).copy(alpha = 0.85f * brakeLightsAlpha),
                            MetallicOrangeBright.copy(alpha = 0.35f * brakeLightsAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cargoLeft, deckBottom - 10.dp.toPx()),
                        radius = 18.dp.toPx()
                    ),
                    radius = 18.dp.toPx(),
                    center = Offset(cargoLeft, deckBottom - 10.dp.toPx())
                )
            }
            drawRoundRect(
                color = if (brakeLightsAlpha > 0.1f) Color(0xFFFF453A) else Color(0xFFB91C1C),
                topLeft = Offset(cargoLeft - 2.dp.toPx(), deckBottom - 15.dp.toPx()),
                size = Size(5.dp.toPx(), 10.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // 3D Rounded Luxury Truck Cab
            val cabLeft = cargoLeft + cargoWidth + 3.dp.toPx()
            val cabWidth = truckWidth * 0.29f
            val cabHeight = truckHeight * 0.60f
            val cabTop = deckBottom - cabHeight

            val cabPath = Path().apply {
                moveTo(cabLeft, cabTop + 6.dp.toPx())
                quadraticTo(cabLeft, cabTop, cabLeft + 8.dp.toPx(), cabTop)
                lineTo(cabLeft + cabWidth * 0.58f, cabTop)
                quadraticTo(cabLeft + cabWidth * 0.78f, cabTop, cabLeft + cabWidth * 0.88f, cabTop + cabHeight * 0.42f)
                lineTo(cabLeft + cabWidth, cabTop + cabHeight * 0.56f)
                lineTo(cabLeft + cabWidth, deckBottom - 4.dp.toPx())
                quadraticTo(cabLeft + cabWidth, deckBottom, cabLeft + cabWidth - 6.dp.toPx(), deckBottom)
                lineTo(cabLeft, deckBottom)
                close()
            }
            drawPath(
                path = cabPath,
                brush = Brush.linearGradient(
                    listOf(
                        RoyalElectricBlue,
                        Color(0xFF1E3A8A),
                        ObsidianDeep
                    ),
                    start = Offset(cabLeft, cabTop),
                    end = Offset(cabLeft + cabWidth, deckBottom)
                )
            )
            drawPath(
                path = cabPath,
                color = PlatinumSilver.copy(alpha = 0.48f),
                style = Stroke(width = 1.4.dp.toPx())
            )

            // Futuristic Glowing Windshield
            val glassPath = Path().apply {
                moveTo(cabLeft + cabWidth * 0.22f, cabTop + 7.dp.toPx())
                lineTo(cabLeft + cabWidth * 0.56f, cabTop + 7.dp.toPx())
                lineTo(cabLeft + cabWidth * 0.78f, cabTop + cabHeight * 0.46f)
                lineTo(cabLeft + cabWidth * 0.22f, cabTop + cabHeight * 0.46f)
                close()
            }
            drawPath(
                path = glassPath,
                brush = Brush.linearGradient(
                    listOf(
                        ElectricBlueGlow.copy(alpha = 0.92f),
                        ArcticWhite.copy(alpha = 0.75f),
                        ElectricBlueBright.copy(alpha = 0.85f)
                    ),
                    start = Offset(cabLeft + cabWidth * 0.2f, cabTop + 6.dp.toPx()),
                    end = Offset(cabLeft + cabWidth * 0.8f, cabTop + cabHeight * 0.46f)
                )
            )

            // Roof Luxury Beacon Light
            drawRoundRect(
                color = secondaryAccent,
                topLeft = Offset(cabLeft + cabWidth * 0.24f, cabTop - 5.dp.toPx()),
                size = Size(cabWidth * 0.24f, 5.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            // Front LED Headlight
            drawRoundRect(
                color = Color(0xFFFFFBEB),
                topLeft = Offset(cabLeft + cabWidth - 5.dp.toPx(), deckBottom - 16.dp.toPx()),
                size = Size(6.dp.toPx(), 7.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Lower Metallic Chassis Frame Connecting Cargo & Cab
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(ObsidianMidnight, Color(0xFF334155), ObsidianMidnight)
                ),
                topLeft = Offset(cargoLeft + 6.dp.toPx(), deckBottom - 4.dp.toPx()),
                size = Size(truckWidth * 0.88f, 9.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        // 4. Spinning 3D Alloy Wheels
        drawTruckWheel(rearWheelCenter, wheelRadius, wheelRotationDeg, primaryAccent)
        drawTruckWheel(frontWheelCenter, wheelRadius, wheelRotationDeg, secondaryAccent)
    }
}

private fun DrawScope.drawTruckWheel(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    rimAccent: Color
) {
    // Tire outer
    drawCircle(
        color = Color(0xFF090D16),
        radius = radius,
        center = center
    )
    drawCircle(
        color = PlatinumSilver.copy(alpha = 0.25f),
        radius = radius - 1.dp.toPx(),
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
    )
    // Metallic alloy rim
    val rimRadius = radius * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(ArcticWhite, PlatinumSilver, Color(0xFF475569)),
            center = center,
            radius = rimRadius
        ),
        radius = rimRadius,
        center = center
    )
    // Rotating alloy spokes
    rotate(degrees = rotationDeg, pivot = center) {
        for (i in 0 until 4) {
            val angle = (i * 90.0) * PI / 180.0
            val dx = (cos(angle) * rimRadius * 0.78f).toFloat()
            val dy = (sin(angle) * rimRadius * 0.78f).toFloat()
            drawLine(
                color = ObsidianMidnight.copy(alpha = 0.75f),
                start = Offset(center.x - dx, center.y - dy),
                end = Offset(center.x + dx, center.y + dy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    // Center hub cap with brand accent
    drawCircle(
        color = rimAccent,
        radius = radius * 0.22f,
        center = center
    )
}

private fun DrawScope.drawBrandTextOnTruck(
    text: String,
    centerX: Float,
    baselineY: Float,
    textSizePx: Float,
    isGold: Boolean
) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = if (isGold) android.graphics.Color.rgb(255, 215, 102) else android.graphics.Color.WHITE
        textSize = textSizePx
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        letterSpacing = 0.08f
    }
    drawContext.canvas.nativeCanvas.drawText(text, centerX, baselineY, paint)
}

/**
 * Three Role-Specific Luxury Welcome Experiences (Owner, Admin, Staff).
 * - OWNER: Exclusive Luxury Welcome Animation + premium branded truck + Owner welcome message.
 * - ADMIN: Elegant Blue & Orange Truck Animation + Admin welcome message.
 * - STAFF: Fun Delivery Truck Animation + friendly bouncing cylinders + Staff welcome message.
 * Respects Android reduced-motion settings and provides a Skip Animation button.
 */
@Composable
fun RoleWelcomeOverlay(
    role: UserRole,
    displayName: String,
    vehicleName: String? = null,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    val truckX = remember { Animatable(if (reducedMotion) 0.5f else -0.35f) }
    val brakePitch = remember { Animatable(0f) }
    val cylinderBounce = remember { Animatable(0f) }
    val lightTrail = remember { Animatable(if (reducedMotion) 0.75f else 0f) }
    val wheelDeg = remember { Animatable(0f) }
    val brakeGlow = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val exitAlpha = remember { Animatable(1f) }

    val truckStyle = when (role) {
        UserRole.OWNER_VIEW -> TruckAnimationStyle.OWNER_EXECUTIVE
        UserRole.ADMIN -> TruckAnimationStyle.ADMIN_COMMAND
        UserRole.STAFF -> TruckAnimationStyle.STAFF_DELIVERY
    }
    val roleBadge = when (role) {
        UserRole.OWNER_VIEW -> "EXECUTIVE OWNER EDITION"
        UserRole.ADMIN -> "ADMIN OPERATIONS COMMAND"
        UserRole.STAFF -> "STAFF DELIVERY CREW"
    }
    val welcomeHeadline = when (role) {
        UserRole.OWNER_VIEW -> "Welcome, Owner"
        UserRole.ADMIN -> "Welcome, Admin"
        UserRole.STAFF -> "Welcome, $displayName!"
    }
    val welcomeSubtitle = when (role) {
        UserRole.OWNER_VIEW -> "Exclusive Business Overview • Live Executive Read-Only Monitoring"
        UserRole.ADMIN -> "Full Store, Fleet, Product Master & Reporting Control Ready"
        UserRole.STAFF -> "Vehicle ${vehicleName ?: "Assigned"} • Ready for Fast Cylinder Deliveries"
    }
    val roleIcon = when (role) {
        UserRole.OWNER_VIEW -> Icons.Default.WorkspacePremium
        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
        UserRole.STAFF -> Icons.Default.LocalShipping
    }
    val badgeAccent = when (role) {
        UserRole.OWNER_VIEW -> Color(0xFFFFD166)
        UserRole.ADMIN -> ElectricBlueGlow
        UserRole.STAFF -> MetallicOrangeBright
    }

    fun finishSmoothly() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            exitAlpha.animateTo(0f, tween(if (reducedMotion) 120 else 260))
            onFinished()
        }
    }

    LaunchedEffect(role, reducedMotion) {
        withFrameNanos { }
        if (reducedMotion) {
            delay(950)
            finishSmoothly()
            return@LaunchedEffect
        }

        launch { textAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { wheelDeg.animateTo(720f, tween(1050, easing = FastOutSlowInEasing)) }
        launch { lightTrail.animateTo(0.85f, tween(650)) }

        // Drive smoothly to center
        truckX.animateTo(0.50f, tween(1020, easing = FastOutSlowInEasing))

        // Gentle brake & stop
        launch { brakeGlow.animateTo(1f, tween(160)) }
        brakePitch.animateTo(2.4f, tween(160, easing = FastOutSlowInEasing))
        brakePitch.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        launch { brakeGlow.animateTo(0f, tween(220)) }

        // Cylinders bounce playfully (extra bounce for Staff fun experience!)
        cylinderBounce.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = if (role == UserRole.STAFF) 760 else 580, easing = LinearEasing)
        )

        // Bright Blue & Orange light trails + accelerate away
        launch { lightTrail.animateTo(1f, tween(220)) }
        launch { wheelDeg.animateTo(1440f, tween(680, easing = FastOutSlowInEasing)) }
        truckX.animateTo(1.42f, tween(680, easing = FastOutSlowInEasing))
        finishSmoothly()
    }

    FuturisticSpaceBackdrop(
        showEnergyParticles = !reducedMotion,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .testTag("role_welcome_overlay_${role.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianMidnight.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = roleBadge,
                            tint = badgeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = roleBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = ArcticWhite
                        )
                    }
                }

                OutlinedButton(
                    onClick = { finishSmoothly() },
                    border = BorderStroke(1.dp, PlatinumSilver.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcticWhite),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("role_welcome_skip_button")
                ) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip Welcome Animation", modifier = Modifier.size(16.dp))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Signature3DEmblem(
                    size = 86.dp,
                    enableOrbitAnimation = !reducedMotion,
                    showOuterHalo = true,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = welcomeHeadline,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { alpha = textAlpha.value }
                        .testTag("role_welcome_headline")
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = welcomeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlatinumSilver.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
                )

                Spacer(Modifier.height(18.dp))

                CinematicDeliveryTruckStage(
                    truckXFraction = truckX.value,
                    brakePitchDeg = brakePitch.value,
                    cylinderBouncePhase = cylinderBounce.value,
                    lightTrailAlpha = lightTrail.value,
                    wheelRotationDeg = wheelDeg.value,
                    brakeLightsAlpha = brakeGlow.value,
                    style = truckStyle
                )
            }

            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(ElectricBlueBright, badgeAccent, MetallicOrangeBright)
                        )
                    )
            )
        }
    }
}
