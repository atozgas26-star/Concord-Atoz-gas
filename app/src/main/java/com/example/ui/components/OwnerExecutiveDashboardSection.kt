package com.example.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
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
            it.deliveredSummary.contains("Arab Gas", ignoreCase = true) ||
                it.returnedSummary.contains("Arab Gas", ignoreCase = true)
        }
    }
    val emiratesGasExchangeCount = remember(lpgExchanges) {
        lpgExchanges.count {
            it.deliveredSummary.contains("Emirates Gas", ignoreCase = true) ||
                it.returnedSummary.contains("Emirates Gas", ignoreCase = true)
        }
    }
    val totalExchangeDelivered = remember(lpgExchanges) { lpgExchanges.sumOf { it.totalDeliveredQty } }
    val totalExchangeReturned = remember(lpgExchanges) { lpgExchanges.sumOf { it.totalReturnedQty } }

    val activeDeviceCount = remember(registeredDevices) { registeredDevices.count { !it.isRevoked } }
    val alertLogCount = remember(securityLogs) { securityLogs.count { it.isAlert } }

    val categoryChartData = remember(companyStockRows) {
        ProductCategory.entries.map { cat ->
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
        // Executive Crown Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SignatureRoyalSurface,
            border = BorderStroke(
                1.3.dp,
                Brush.linearGradient(listOf(MetallicOrangeGold, ElectricBlueGlow))
            ),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(RoyalBlueDeep, SignatureRoyalSurface, Color(0xFF1A2952))
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MetallicOrange.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MetallicOrangeGold),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MetallicOrangeGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OWNER EXECUTIVE MONITORING",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticWhite,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Strictly View-Only • Live Corporate Analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MetallicOrangeGold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MetallicOrange.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MetallicOrangeBright)
                    ) {
                        Text(
                            text = "VIEW ONLY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MetallicOrangeBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Real-time overview of C&A GAS store stock, fleet vehicles, Arab Gas & Emirates Gas exchanges, daily closing balances, and security activity.",
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

        // Visual Chart 2: 7-Day Sales vs Purchase Activity
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
                        text = "7-Day Sales & Purchase Trend",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChartLegendDot("Sales", MetallicOrangeGold)
                        ChartLegendDot("Purchases", BrandGreenLight)
                    }
                }
                ExecutivePairedBarChart(
                    items = trendChartData,
                    primaryColor = MetallicOrangeGold,
                    secondaryColor = BrandGreenLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
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
            val barWidth = (groupWidth * 0.28f).coerceIn(8f, 28f)
            val gap = (barWidth * 0.22f).coerceAtLeast(3f)
            val chartBottom = size.height - 6f
            val usableHeight = (chartBottom - 8f).coerceAtLeast(12f)

            // Subtle baseline
            drawLine(
                color = SignatureRoyalBorder.copy(alpha = 0.7f),
                start = Offset(0f, chartBottom),
                end = Offset(size.width, chartBottom),
                strokeWidth = 1.5f
            )

            items.forEachIndexed { idx, item ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val h1 = (item.second.toFloat() / maxVal.toFloat()) * usableHeight
                val h2 = (item.third.toFloat() / maxVal.toFloat()) * usableHeight

                if (h1 > 0f) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(centerX - barWidth - gap / 2f, chartBottom - h1),
                        size = Size(barWidth, h1),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                if (h2 > 0f) {
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(centerX + gap / 2f, chartBottom - h2),
                        size = Size(barWidth, h2),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
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
