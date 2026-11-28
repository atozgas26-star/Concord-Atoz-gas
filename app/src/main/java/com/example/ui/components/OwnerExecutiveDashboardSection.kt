package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityAuditLogEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.model.CompanyStockRow
import com.example.data.model.DashboardSummary
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.ChromeSilverHighlight
import com.example.ui.theme.CinematicMidnightBlack
import com.example.ui.theme.DeepSpaceNavy
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.MoltenOrangeCore
import com.example.ui.theme.MoltenOrangeGlow
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBorder
import com.example.ui.theme.SignatureRoyalElevated
import com.example.ui.theme.SignatureRoyalSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Strictly VIEW-ONLY Executive Monitoring Dashboard for C&A GAS Owner accounts.
 * Displays executive KPIs, Arab Gas & Emirates Gas LPG exchange totals, interactive visual charts,
 * and read-only security/device monitoring with ZERO mutation affordances.
 */
@Composable
fun OwnerExecutiveDashboardSection(
    summary: DashboardSummary,
    companyStockRows: List<CompanyStockRow>,
    transactions: List<StockTransactionEntity>,
    lpgExchanges: List<DifferentGasReturnEntity>,
    registeredDevices: List<RegisteredDeviceEntity>,
    securityLogs: List<SecurityAuditLogEntity>,
    onOpenReports: () -> Unit,
    onOpenExchangeHistory: () -> Unit,
    onOpenSecurityMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayPurchases = summary.todayFullPurchases + summary.todayEmptyPurchases
    val todaySales = summary.todaySales

    val arabGasExchangeCount = remember(lpgExchanges) {
        lpgExchanges.count {
            it.returnedEmptyCompany.contains("Arab Gas", ignoreCase = true) ||
                it.deliveredSummary.contains("Arab Gas", ignoreCase = true) ||
                it.returnedSummary.contains("Arab Gas", ignoreCase = true)
        }
    }
    val emiratesGasExchangeCount = remember(lpgExchanges) {
        lpgExchanges.count {
            it.returnedEmptyCompany.contains("Emirates Gas", ignoreCase = true) ||
                it.deliveredSummary.contains("Emirates Gas", ignoreCase = true) ||
                it.returnedSummary.contains("Emirates Gas", ignoreCase = true)
        }
    }
    val totalExchangeDelivered = remember(lpgExchanges) { lpgExchanges.sumOf { it.totalDeliveredQty } }
    val totalExchangeReturned = remember(lpgExchanges) { lpgExchanges.sumOf { it.totalReturnedQty } }

    val activeDeviceCount = remember(registeredDevices) { registeredDevices.count { !it.isRevoked } }
    val alertLogCount = remember(securityLogs) { securityLogs.count { it.isAlert } }

    val categoryChartData = remember(companyStockRows) {
        ProductCategory.activeEntries.map { cat ->
            val storeFull = companyStockRows.filter { it.product.category == cat }.sumOf { it.storeFull }
            val vehicleFull = companyStockRows.filter { it.product.category == cat }.sumOf { it.vehiclesFull }
            Triple(cat.shortCode, storeFull, vehicleFull)
        }
    }

    val trendChartData = remember(transactions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelSdf = SimpleDateFormat("dd/MM", Locale.US)
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dayKey = sdf.format(cal.time)
            val label = labelSdf.format(cal.time)
            val sales = transactions.filter {
                it.businessDate == dayKey && (
                    it.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
                        it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE ||
                        it.type == TransactionType.STORE_FULL_DISPATCH_DIRECT
                    )
            }.sumOf { it.quantity }
            val purchases = transactions.filter {
                it.businessDate == dayKey && (
                    it.type == TransactionType.STORE_PURCHASE_FULL ||
                        it.type == TransactionType.STORE_PURCHASE_EMPTY
                    )
            }.sumOf { it.quantity }
            Triple(label, sales, purchases)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("owner_executive_dashboard_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Futuristic 3D Executive Crown Banner
        val isDarkOwner = com.example.ui.theme.LocalIsDarkTheme.current
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkOwner) DeepSpaceNavy else Color(0xFF1D63ED),
            border = BorderStroke(
                1.4.dp,
                Brush.linearGradient(listOf(ChromeSilverHighlight.copy(alpha = 0.85f), MoltenOrangeGlow, NeonCyanGlow))
            ),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkOwner) {
                            Brush.horizontalGradient(
                                listOf(CinematicMidnightBlack, DeepSpaceNavy, RoyalBlueDeep)
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1D63ED), Color(0xFF2563EB), Color(0xFF3B82F6))
                            )
                        }
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Signature3DEmblem(
                            size = 48.dp,
                            enableOrbitAnimation = true,
                            showOuterHalo = true
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OWNER EXECUTIVE 3D HUB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticWhite,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Strictly View-Only • Live Corporate Analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MoltenOrangeCore
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MetallicOrange.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, MoltenOrangeGlow)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MoltenOrangeCore,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "VIEW ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MoltenOrangeCore
                            )
                        }
                    }
                }
                Text(
                    text = "Real-time 3D telemetry of C&A GAS store stock, fleet vehicles, LPG exchanges (Arab Gas & Emirates Gas), daily closing balances, and security activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PlatinumSilver
                )
            }
        }

        // Executive KPI 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExecutiveKpiCard(
                title = "Total Store Stock",
                primaryValue = "${summary.storeFullBalance} Full",
                secondaryValue = "${summary.storeEmptyBalance} Empty • ${summary.storeDamagedBalance} Damaged",
                icon = Icons.Default.Warehouse,
                accent = ElectricBlueGlow,
                modifier = Modifier.weight(1f)
            )
            ExecutiveKpiCard(
                title = "Total Vehicle Stock",
                primaryValue = "${summary.allVehiclesFullBalance} Full",
                secondaryValue = "${summary.allVehiclesEmptyBalance} Empty • ${summary.allVehiclesDamagedBalance} Dmg",
                icon = Icons.Default.LocalShipping,
                accent = MetallicOrangeBright,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExecutiveKpiCard(
                title = "Purchase Summary",
                primaryValue = "+$todayPurchases Today",
                secondaryValue = "${summary.todayFullPurchases}F / ${summary.todayEmptyPurchases}E • ${summary.todayStoreActivityCount} Store Tx",
                icon = Icons.Default.ShoppingCart,
                accent = BrandGreenLight,
                modifier = Modifier.weight(1f)
            )
            ExecutiveKpiCard(
                title = "Sales & Closing",
                primaryValue = "$todaySales Sold Today",
                secondaryValue = "Closing Full: ${summary.totalFullCylinders}",
                icon = Icons.Default.TrendingUp,
                accent = MetallicOrangeGold,
                modifier = Modifier.weight(1f)
            )
        }

        // LPG Exchange Summary (Arab Gas & Emirates Gas)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SignatureRoyalSurface,
            border = BorderStroke(1.dp, SignatureRoyalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MetallicOrangeBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LPG Exchange Summary (Arab Gas & Emirates Gas)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ArcticWhite
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExecutivePillMetric("Arab Gas", "$arabGasExchangeCount Tx", ElectricBlueGlow, Modifier.weight(1f))
                    ExecutivePillMetric("Emirates Gas", "$emiratesGasExchangeCount Tx", MetallicOrangeBright, Modifier.weight(1f))
                    ExecutivePillMetric("Delivered / Ret", "${totalExchangeDelivered}F / ${totalExchangeReturned}E", BrandGreenLight, Modifier.weight(1.2f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenExchangeHistory,
                        border = BorderStroke(1.dp, ElectricBlueBright),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("owner_open_exchange_history_btn")
                    ) {
                        Text("View Exchanges", color = ElectricBlueGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onOpenReports,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("owner_open_reports_btn")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Executive Reports", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Visual Chart 1: Category Stock Comparison (Store Full vs Vehicle Full)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SignatureRoyalSurface,
            border = BorderStroke(1.dp, SignatureRoyalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Company Stock Distribution (Full Cylinders)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChartLegendDot("Store", ElectricBlueGlow)
                        ChartLegendDot("Vehicles", MetallicOrangeBright)
                    }
                }
                ExecutivePairedBarChart(
                    items = categoryChartData,
                    primaryColor = ElectricBlueBright,
                    secondaryColor = MetallicOrangeBright,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                )
            }
        }

        // Visual Chart 2: Animated Molten-Orange 7-Day Sales & Purchase Energy Graph
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SignatureRoyalSurface,
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(MoltenOrangeGlow.copy(alpha = 0.6f), SignatureRoyalBorder))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-Day Sales & Purchase Energy Graph",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChartLegendDot("Sales", MoltenOrangeGlow)
                        ChartLegendDot("Purchases", BrandGreenLight)
                    }
                }
                FuturisticOrangeTrendGraph(
                    items = trendChartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp)
                )
            }
        }

        // Security, Login & Device Activity Overview (Read-Only)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SignatureRoyalSurface,
            border = BorderStroke(
                1.dp,
                if (alertLogCount > 0) MetallicOrangeBright.copy(alpha = 0.7f) else SignatureRoyalBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (alertLogCount > 0) MetallicOrangeBright else ElectricBlueGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Security, Devices & Login Activity",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ArcticWhite
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenSecurityMonitor,
                        border = BorderStroke(1.dp, MetallicOrangeBright),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("owner_open_security_monitor_btn")
                    ) {
                        Text("Inspect", color = MetallicOrangeBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExecutivePillMetric("Active Devices", "$activeDeviceCount", ElectricBlueGlow, Modifier.weight(1f))
                    ExecutivePillMetric("Security Alerts", "$alertLogCount", if (alertLogCount > 0) Color(0xFFF87171) else BrandGreenLight, Modifier.weight(1f))
                    ExecutivePillMetric("Audit Events", "${securityLogs.size}", PlatinumSilver, Modifier.weight(1f))
                }
                val latestLog = securityLogs.firstOrNull()
                if (latestLog != null) {
                    HorizontalDivider(color = SignatureRoyalBorder.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (latestLog.isAlert) Icons.Default.Warning else Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (latestLog.isAlert) Color(0xFFF87171) else ElectricBlueGlow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${latestLog.eventType}: ${latestLog.details}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlatinumSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SignatureRoyalSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = PlatinumSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                color = ArcticWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.labelSmall,
                color = PlatinumMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExecutivePillMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SignatureRoyalElevated,
        border = BorderStroke(1.dp, SignatureRoyalBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PlatinumMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PlatinumSilver,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ExecutivePairedBarChart(
    items: List<Triple<String, Int, Int>>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val lightweight = remember(context) { isLightweightMotionMode(context) }
    var animateReady by remember { mutableStateOf(lightweight) }
    LaunchedEffect(Unit) { animateReady = true }
    val riseProgress by animateFloatAsState(
        targetValue = if (animateReady) 1f else 0f,
        animationSpec = tween(durationMillis = if (lightweight) 0 else 700, easing = FastOutSlowInEasing),
        label = "bar_3d_rise"
    )
    val maxVal = remember(items) {
        items.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1) ?: 1
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val groupWidth = size.width / items.size.coerceAtLeast(1)
            val barWidth = (groupWidth * 0.25f).coerceIn(10f, 28f)
            val depthDx = (barWidth * 0.26f).coerceIn(3f, 8f)
            val depthDy = (barWidth * 0.18f).coerceIn(2.5f, 6f)
            val gap = (barWidth * 0.32f).coerceAtLeast(5f)
            val chartBottom = size.height - 6f
            val usableHeight = (chartBottom - depthDy - 10f).coerceAtLeast(12f)

            // Subtle holographic horizontal gridlines
            for (step in 0..2) {
                val y = chartBottom - (usableHeight * (step / 2f))
                drawLine(
                    color = SignatureRoyalBorder.copy(alpha = if (step == 0) 0.75f else 0.32f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (step == 0) 1.5f else 1f
                )
            }

            fun draw3DBar(left: Float, height: Float, baseColor: Color, highlightColor: Color) {
                if (height <= 1f) return
                val top = chartBottom - height
                // Front face with vertical neon gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(highlightColor, baseColor, baseColor.copy(alpha = 0.55f))
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Right 3D extruded depth face
                val rightFace = Path().apply {
                    moveTo(left + barWidth, top)
                    lineTo(left + barWidth + depthDx, top - depthDy)
                    lineTo(left + barWidth + depthDx, chartBottom - depthDy)
                    lineTo(left + barWidth, chartBottom)
                    close()
                }
                drawPath(rightFace, color = baseColor.copy(alpha = 0.42f))
                // Top 3D isometric luminous cap
                val topCap = Path().apply {
                    moveTo(left, top)
                    lineTo(left + depthDx, top - depthDy)
                    lineTo(left + barWidth + depthDx, top - depthDy)
                    lineTo(left + barWidth, top)
                    close()
                }
                drawPath(topCap, color = highlightColor.copy(alpha = 0.92f))
            }

            items.forEachIndexed { idx, item ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val h1 = (item.second.toFloat() / maxVal.toFloat()) * usableHeight * riseProgress
                val h2 = (item.third.toFloat() / maxVal.toFloat()) * usableHeight * riseProgress

                draw3DBar(
                    left = centerX - barWidth - gap / 2f,
                    height = h1,
                    baseColor = primaryColor,
                    highlightColor = NeonCyanGlow
                )
                draw3DBar(
                    left = centerX + gap / 2f,
                    height = h2,
                    baseColor = secondaryColor,
                    highlightColor = MoltenOrangeCore
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (label, v1, v2) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PlatinumSilver,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$v1/$v2",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = PlatinumMuted,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FuturisticOrangeTrendGraph(
    items: List<Triple<String, Int, Int>>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val lightweight = remember(context) { isLightweightMotionMode(context) }
    var animateReady by remember { mutableStateOf(lightweight) }
    LaunchedEffect(Unit) { animateReady = true }
    val progress by animateFloatAsState(
        targetValue = if (animateReady) 1f else 0f,
        animationSpec = tween(durationMillis = if (lightweight) 0 else 820, easing = FastOutSlowInEasing),
        label = "orange_trend_reveal"
    )
    val maxVal = remember(items) {
        items.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1) ?: 1
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val count = items.size.coerceAtLeast(1)
            val stepX = size.width / count
            val chartBottom = size.height - 6f
            val chartTop = 10f
            val usableH = (chartBottom - chartTop).coerceAtLeast(14f)

            // Gridlines
            for (g in 0..2) {
                val gy = chartBottom - usableH * (g / 2f)
                drawLine(
                    color = SignatureRoyalBorder.copy(alpha = if (g == 0) 0.75f else 0.28f),
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = if (g == 0) 1.5f else 1f
                )
            }

            // Secondary Purchase bars (translucent emerald glow pillars)
            val purW = (stepX * 0.26f).coerceIn(6f, 18f)
            items.forEachIndexed { idx, (_, _, purchases) ->
                val cx = stepX * idx + stepX / 2f
                val ph = (purchases.toFloat() / maxVal.toFloat()) * usableH * progress
                if (ph > 1f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(BrandGreenLight.copy(alpha = 0.72f), BrandGreenLight.copy(alpha = 0.16f))
                        ),
                        topLeft = Offset(cx - purW / 2f, chartBottom - ph),
                        size = Size(purW, ph),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Molten Orange glowing Sales line + gradient area under curve
            val points = items.mapIndexed { idx, (_, sales, _) ->
                val cx = stepX * idx + stepX / 2f
                val sh = (sales.toFloat() / maxVal.toFloat()) * usableH * progress
                Offset(cx, chartBottom - sh)
            }

            if (points.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(points.first().x, chartBottom)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                    lineTo(points.last().x, chartBottom)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MoltenOrangeGlow.copy(alpha = 0.42f),
                            MoltenOrangeGlow.copy(alpha = 0.04f)
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )
                )

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                // Outer molten halo
                drawPath(
                    path = linePath,
                    color = MoltenOrangeGlow.copy(alpha = 0.35f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Crisp molten core stroke
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        listOf(MetallicOrange, MoltenOrangeGlow, MoltenOrangeCore)
                    ),
                    style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Glowing Sales data nodes
            points.forEach { pt ->
                drawCircle(
                    color = MoltenOrangeGlow.copy(alpha = 0.38f),
                    radius = 7.5f,
                    center = pt
                )
                drawCircle(
                    color = MoltenOrangeCore,
                    radius = 3.8f,
                    center = pt
                )
                drawCircle(
                    color = ArcticWhite,
                    radius = 1.6f,
                    center = pt
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (label, sales, purchases) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PlatinumSilver,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$sales/$purchases",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = MoltenOrangeCore,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
