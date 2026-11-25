package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PropaneTank
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Advanced 3-second animated launch screen showcasing the company name,
 * selected theme & sound status, and prominent "Built by Ragavan" credit.
 */
@Composable
fun StartupSplashOverlay(
    themePreset: ThemePreset,
    soundEnabled: Boolean,
    onSplashFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.76f) }
    val contentAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550)
        )
    }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2850, easing = LinearEasing)
        )
        delay(150)
        onSplashFinished()
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        primary.copy(alpha = 0.96f),
                        Color(0xFF071E14),
                        secondary.copy(alpha = 0.85f)
                    )
                )
            )
            .testTag("startup_splash_overlay")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_company_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top theme & sound indicator pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
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
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
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

            // Centerpiece Logo & Company Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(logoScale.value)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.45f)),
                    modifier = Modifier.size(104.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PropaneTank,
                            contentDescription = "Concord & A to Z Gas Logo",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Surface(
                    color = BrandRedSecondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "INDUSTRIAL GASES & LPG FLEET",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "CONCORD & A TO Z GAS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Store • Vehicles • Purchases • Reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { progress.value },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .width(210.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                val secondsLeft = ((1f - progress.value) * 3f).coerceAtLeast(0f)
                Text(
                    text = String.format(java.util.Locale.US, "Launching workspace... %.1fs", secondsLeft),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            // Bottom "Built by Ragavan" Credit + Skip action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
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
                    onClick = onSplashFinished,
                    modifier = Modifier.testTag("splash_skip_button")
                ) {
                    Text(
                        text = "Tap to Continue",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
