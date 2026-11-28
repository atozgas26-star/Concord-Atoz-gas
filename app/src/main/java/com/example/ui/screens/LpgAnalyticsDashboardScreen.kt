package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.model.CompanyStockRow
import com.example.data.remote.WorkspaceConnectionState
import com.example.ui.components.AnalyticsTimeRange
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.FuturisticSpaceBackdrop
import com.example.ui.components.LpgActivityEvent
import com.example.ui.components.LpgAnalyticsEngine
import com.example.ui.components.LpgEmptyReturnDonutChart
import com.example.ui.components.LpgInventoryGroupedBarChart
import com.example.ui.components.LpgSalesTrendAreaChart
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.DarkSlateSecondary
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.DeepSpaceNavy
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.ElectricBlueTextLight
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MoltenOrangeGlow
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SuccessDarkGreen
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningDarkOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-Time LPG Cylinder Inventory & Sales Trends Dashboard Screen.
 * Streams live Firestore-synced stock & transaction updates into interactive native Jetpack Compose
 * charts (Grouped Bar Inventory Levels, Smooth Area/Line Sales Trends, and Arab/Emirates Empty Return Donut).
 */
@Composable
fun LpgAnalyticsDashboardScreen(
    selectedDate: String,
    companyStockRows: List<CompanyStockRow>,
    transactions: List<StockTransactionEntity>,
    lpgExchanges: List<DifferentGasReturnEntity>,
    workspaceState: WorkspaceConnectionState,
    isReadOnly: Boolean,
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedRange by rememberSaveable { mutableStateOf(AnalyticsTimeRange.DAYS_7) }
    val snapshot = remember(selectedDate, selectedRange, companyStockRows, transactions, lpgExchanges) {
        LpgAnalyticsEngine.computeSnapshot(
            anchorDateIso = selectedDate,
            range = selectedRange,
            companyStockRows = companyStockRows,
            transactions = transactions,
            lpgExchanges = lpgExchanges
        )
    }

    val isDark = LocalIsDarkTheme.current
    val isLiveFirestore = workspaceState is WorkspaceConnectionState.Connected
    val pulseTransition = rememberInfiniteTransition(label = "firestore_live_pulse")
    val liveAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    FuturisticSpaceBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lpg_analytics_dashboard_screen"),
        showEnergyParticles = isDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isDark) Modifier.background(MaterialTheme.colorScheme.background) else Modifier)
        ) {
            // Futuristic Header with Live Firestore Telemetry Status
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DeepSpaceNavy, RoyalElectricBlue.copy(alpha = 0.85f), Color(0xFF3E1C09))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("lpg_analytics_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Dashboard",
                                    tint = ArcticWhite
                                )
                            }
                            Column {
                                Text(
                                    text = "Live LPG Analytics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ArcticWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "11 KG • 22 KG • 44 KG Real-Time Inventory & Sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetallicOrangeBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Live Firestore Connection Pill
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isLiveFirestore) BrandGreenLight.copy(alpha = 0.22f) else MetallicOrange.copy(alpha = 0.22f),
                            border = BorderStroke(
                                1.dp,
                                if (isLiveFirestore) BrandGreenLight.copy(alpha = 0.7f) else MetallicOrangeBright.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier.testTag("lpg_analytics_firestore_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(if (isLiveFirestore) liveAlpha else 1f)
                                        .clip(CircleShape)
                                        .background(if (isLiveFirestore) BrandGreenLight else MetallicOrangeBright)
                                )
                                Icon(
                                    imageVector = if (isLiveFirestore) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Firestore Sync Status",
                                    tint = ArcticWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isLiveFirestore) "LIVE FIRESTORE" else "LOCAL CACHE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 720.dp

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isReadOnly) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricBlueBright.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ElectricBlueGlow.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Owner Read-Only Mode",
                                        tint = if (isDark) ElectricBlueGlow else ElectricBlueTextLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Owner Executive View (Read-Only) — Real-Time Firestore Analytics Stream",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) PureWhite else DeepNavyText
                                    )
                                }
                            }
                        }
                    }

                    item {
                        DateSelectorBar(
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected
                        )
                    }

                    // 4-Metric Real-Time LPG KPI Row
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnalyticsKpiTile(
                                    title = "LPG Full Stock",
                                    value = snapshot.totalLpgFull.toString(),
                                    subtitle = "Store + Fleet",
                                    icon = Icons.Default.PropaneTank,
                                    accent = if (isDark) MetallicOrangeBright else WarningDarkOrange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_lpg_total_full")
                                )
                                AnalyticsKpiTile(
                                    title = "LPG Empty Stock",
                                    value = snapshot.totalLpgEmpty.toString(),
                                    subtitle = "Damaged: ${snapshot.totalLpgDamaged}",
                                    icon = Icons.Default.Recycling,
                                    accent = if (isDark) BrandGreenLight else SuccessDarkGreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_lpg_total_empty")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnalyticsKpiTile(
                                    title = "${selectedRange.label} LPG Sold",
                                    value = snapshot.periodLpgSales.toString(),
                                    subtitle = "All Gas: ${snapshot.periodAllGasSales}",
                                    icon = Icons.Default.TrendingUp,
                                    accent = if (isDark) ElectricBlueGlow else ElectricBlueTextLight,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_lpg_period_sales")
                                )
                                AnalyticsKpiTile(
                                    title = "Peak LPG Day",
                                    value = snapshot.peakDayQty.toString(),
                                    subtitle = snapshot.peakDayLabel,
                                    icon = Icons.Default.LocalFireDepartment,
                                    accent = if (isDark) MoltenOrangeGlow else WarningDarkOrange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_lpg_peak_day")
                                )
                            }
                        }
                    }

                    // Adaptive Primary Charts: Side-by-side on Tablets, Stacked on Mobile
                    if (isWide) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                LpgInventoryGroupedBarChart(
                                    metrics = snapshot.lpgSizes,
                                    modifier = Modifier.weight(1f)
                                )
                                LpgSalesTrendAreaChart(
                                    points = snapshot.trendPoints,
                                    selectedRange = selectedRange,
                                    onRangeSelected = { selectedRange = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        item {
                            LpgInventoryGroupedBarChart(metrics = snapshot.lpgSizes)
                        }
                        item {
                            LpgSalesTrendAreaChart(
                                points = snapshot.trendPoints,
                                selectedRange = selectedRange,
                                onRangeSelected = { selectedRange = it }
                            )
                        }
                    }

                    // LPG Empty Return Company Distribution (Arab Gas vs Emirates Gas)
                    item {
                        LpgEmptyReturnDonutChart(returnShare = snapshot.returnShare)
                    }

                    // Live Firestore LPG Activity Feed Header
                    item {
                        Text(
                            text = "Recent LPG Firestore Movements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (snapshot.recentLpgEvents.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) DeepSpaceNavy.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No recent LPG cylinder movements yet. New sales, purchases, and exchanges synced via Firestore will appear here in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(snapshot.recentLpgEvents, key = { "${it.id}_${it.timestamp}" }) { event ->
                            LpgActivityFeedRow(event = event)
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) DeepSpaceNavy else PureWhite,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextSecondaryDark else DarkSlateSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) PureWhite else DeepNavyText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LpgActivityFeedRow(event: LpgActivityEvent) {
    val isDark = LocalIsDarkTheme.current
    val timeText = remember(event.timestamp) {
        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(event.timestamp))
    }
    val badgeColor = if (event.isSale) {
        if (isDark) MetallicOrangeBright else WarningDarkOrange
    } else {
        if (isDark) ElectricBlueGlow else ElectricBlueTextLight
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) ObsidianElevated else PureWhite,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "LPG ${event.productName} • ${event.typeLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PureWhite else DeepNavyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.actorName} (${event.vehicleOrScope}) • $timeText",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) TextSecondaryDark else DarkSlateSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "${if (event.isSale) "-" else "+"}${event.quantity}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
