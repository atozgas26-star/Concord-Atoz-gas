package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.ChromeSilverHighlight
import com.example.ui.theme.DeepSpaceNavy
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.MoltenOrangeGlow
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalElectricBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class AnalyticsTimeRange(val days: Int, val label: String) {
    DAYS_7(7, "7D"),
    DAYS_14(14, "14D"),
    DAYS_30(30, "30D")
}

data class LpgSizeInventoryMetric(
    val productId: Long,
    val label: String,
    val storeFull: Int,
    val fleetFull: Int,
    val storeEmpty: Int,
    val fleetEmpty: Int,
    val damaged: Int,
    val todaySales: Int
) {
    val totalFull: Int get() = storeFull + fleetFull
    val totalEmpty: Int get() = storeEmpty + fleetEmpty
}

data class SalesTrendPoint(
    val dateIso: String,
    val shortLabel: String,
    val lpgSales: Int,
    val lpgRestocks: Int,
    val allGasSales: Int
)

data class LpgCompanyReturnShare(
    val arabGasReturns: Int,
    val emiratesGasReturns: Int,
    val otherReturns: Int
) {
    val totalReturns: Int get() = arabGasReturns + emiratesGasReturns + otherReturns
}

data class LpgActivityEvent(
    val id: Long,
    val dateIso: String,
    val timestamp: Long,
    val productName: String,
    val typeLabel: String,
    val actorName: String,
    val vehicleOrScope: String,
    val quantity: Int,
    val isSale: Boolean
)

data class LpgAnalyticsSnapshot(
    val lpgSizes: List<LpgSizeInventoryMetric>,
    val trendPoints: List<SalesTrendPoint>,
    val returnShare: LpgCompanyReturnShare,
    val totalLpgFull: Int,
    val totalLpgEmpty: Int,
    val totalLpgDamaged: Int,
    val periodLpgSales: Int,
    val periodAllGasSales: Int,
    val peakDayLabel: String,
    val peakDayQty: Int,
    val recentLpgEvents: List<LpgActivityEvent>
)

/**
 * Pure analytics aggregation engine computing real-time LPG cylinder inventory levels
 * (strictly 11 KG, 22 KG, 44 KG) and multi-day sales trends from Firestore-synced records.
 */
object LpgAnalyticsEngine {
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val shortLabelFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.US)

    private val salesTypes = setOf(
        TransactionType.VEHICLE_CUSTOMER_SALE,
        TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
        TransactionType.STORE_FULL_DISPATCH_DIRECT
    )

    private val restockTypes = setOf(
        TransactionType.STORE_PURCHASE_FULL,
        TransactionType.STORE_FULL_INWARD
    )

    fun computeSnapshot(
        anchorDateIso: String,
        range: AnalyticsTimeRange,
        companyStockRows: List<CompanyStockRow>,
        transactions: List<StockTransactionEntity>,
        lpgExchanges: List<DifferentGasReturnEntity>
    ): LpgAnalyticsSnapshot {
        val anchorDate = runCatching { LocalDate.parse(anchorDateIso, isoFormatter) }
            .getOrElse { LocalDate.now() }

        val lpgRows = companyStockRows
            .filter { it.product.category == ProductCategory.COOKING_GAS && it.product.isActive }
            .sortedBy { it.product.sortOrder }

        val lpgProductIds = lpgRows.map { it.product.id }.toSet()
        val lpgNameById = lpgRows.associate { it.product.id to it.product.name }

        val todayIso = anchorDate.format(isoFormatter)
        val todaySalesByProduct = transactions
            .filter { it.businessDate == todayIso && it.type in salesTypes && it.productId in lpgProductIds }
            .groupBy { it.productId }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        val damagedByProduct = transactions
            .filter { it.businessDate <= todayIso && it.productId in lpgProductIds }
            .groupBy { it.productId }
            .mapValues { (_, list) ->
                var netDamaged = 0
                list.forEach { tx ->
                    when (tx.type) {
                        TransactionType.STORE_DAMAGED_ADD,
                        TransactionType.VEHICLE_DAMAGED_ADD -> netDamaged += tx.quantity
                        TransactionType.STORE_DAMAGED_REMOVE,
                        TransactionType.VEHICLE_DAMAGED_REMOVE -> netDamaged -= tx.quantity
                        else -> Unit
                    }
                }
                netDamaged.coerceAtLeast(0)
            }

        val sizeMetrics = lpgRows.map { row ->
            LpgSizeInventoryMetric(
                productId = row.product.id,
                label = row.product.name,
                storeFull = row.storeFull,
                fleetFull = row.vehiclesFull,
                storeEmpty = row.storeEmpty,
                fleetEmpty = row.vehiclesEmpty,
                damaged = damagedByProduct[row.product.id] ?: 0,
                todaySales = todaySalesByProduct[row.product.id] ?: 0
            )
        }

        val txByDate = transactions.groupBy { it.businessDate }
        val trendPoints = (range.days - 1 downTo 0).map { offset ->
            val d = anchorDate.minusDays(offset.toLong())
            val key = d.format(isoFormatter)
            val dayTxs = txByDate[key].orEmpty()
            val lpgSales = dayTxs.filter { it.type in salesTypes && it.productId in lpgProductIds }.sumOf { it.quantity }
            val lpgRestocks = dayTxs.filter { it.type in restockTypes && it.productId in lpgProductIds }.sumOf { it.quantity }
            val allGasSales = dayTxs.filter { it.type in salesTypes }.sumOf { it.quantity }
            SalesTrendPoint(
                dateIso = key,
                shortLabel = d.format(shortLabelFormatter),
                lpgSales = lpgSales,
                lpgRestocks = lpgRestocks,
                allGasSales = allGasSales
            )
        }

        val startIso = anchorDate.minusDays((range.days - 1).toLong()).format(isoFormatter)
        val windowExchanges = lpgExchanges.filter { it.businessDate >= startIso && it.businessDate <= todayIso }
        var arabCount = 0
        var emiratesCount = 0
        var otherCount = 0
        windowExchanges.forEach { ex ->
            val qty = ex.totalReturnedQty.coerceAtLeast(0)
            when {
                ex.returnedEmptyCompany.equals("Arab Gas", ignoreCase = true) -> arabCount += qty
                ex.returnedEmptyCompany.equals("Emirates Gas", ignoreCase = true) -> emiratesCount += qty
                ex.returnedEmptyCompany.isNotBlank() -> otherCount += qty
            }
        }

        val peakPoint = trendPoints.maxByOrNull { it.lpgSales }
        val recentLpgEvents = transactions
            .filter { it.productId in lpgProductIds }
            .sortedByDescending { it.timestamp }
            .take(8)
            .map { tx ->
                LpgActivityEvent(
                    id = tx.id,
                    dateIso = tx.businessDate,
                    timestamp = tx.timestamp,
                    productName = lpgNameById[tx.productId] ?: "LPG",
                    typeLabel = tx.type.label,
                    actorName = tx.staffName,
                    vehicleOrScope = tx.vehicleName?.takeIf { it.isNotBlank() } ?: "Main Store",
                    quantity = tx.quantity,
                    isSale = tx.type in salesTypes
                )
            }

        return LpgAnalyticsSnapshot(
            lpgSizes = sizeMetrics,
            trendPoints = trendPoints,
            returnShare = LpgCompanyReturnShare(
                arabGasReturns = arabCount,
                emiratesGasReturns = emiratesCount,
                otherReturns = otherCount
            ),
            totalLpgFull = sizeMetrics.sumOf { it.totalFull },
            totalLpgEmpty = sizeMetrics.sumOf { it.totalEmpty },
            totalLpgDamaged = sizeMetrics.sumOf { it.damaged },
            periodLpgSales = trendPoints.sumOf { it.lpgSales },
            periodAllGasSales = trendPoints.sumOf { it.allGasSales },
            peakDayLabel = if ((peakPoint?.lpgSales ?: 0) > 0) peakPoint!!.shortLabel else "—",
            peakDayQty = peakPoint?.lpgSales ?: 0,
            recentLpgEvents = recentLpgEvents
        )
    }
}

/**
 * Interactive Recharts-style Grouped Bar Chart for real-time LPG Cylinder Inventory Levels
 * (11 KG, 22 KG, 44 KG) with series toggles and tap-to-inspect floating tooltip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LpgInventoryGroupedBarChart(
    metrics: List<LpgSizeInventoryMetric>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var showStoreFull by remember { mutableStateOf(true) }
    var showFleetFull by remember { mutableStateOf(true) }
    var showStoreEmpty by remember { mutableStateOf(true) }
    var showFleetEmpty by remember { mutableStateOf(false) }
    var selectedIdx by remember(metrics.size) { mutableIntStateOf(if (metrics.isNotEmpty()) 0 else -1) }
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(metrics) { animateTrigger = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "lpg_bar_anim"
    )

    val cardBg = if (isDark) DeepSpaceNavy.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) MetallicOrange.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outlineVariant
    val titleColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lpg_inventory_bar_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MetallicOrange.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "LPG Inventory Levels",
                            tint = MetallicOrangeBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Real-Time LPG Cylinder Inventory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Text(
                            text = "Tap any cylinder size to inspect Store & Fleet balances",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                    }
                }
            }

            // Recharts-style Interactive Series Legend Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartSeriesToggleChip("Store Full", MetallicOrangeBright, showStoreFull, "toggle_series_store_full") {
                    showStoreFull = !showStoreFull
                }
                ChartSeriesToggleChip("Fleet Full", ElectricBlueGlow, showFleetFull, "toggle_series_fleet_full") {
                    showFleetFull = !showFleetFull
                }
                ChartSeriesToggleChip("Store Empty", BrandGreenLight, showStoreEmpty, "toggle_series_store_empty") {
                    showStoreEmpty = !showStoreEmpty
                }
                ChartSeriesToggleChip("Fleet Empty", ChromeSilverHighlight, showFleetEmpty, "toggle_series_fleet_empty") {
                    showFleetEmpty = !showFleetEmpty
                }
            }

            if (metrics.isEmpty()) {
                ChartEmptyPlaceholder(
                    message = "No active LPG cylinder sizes found.",
                    hint = "Add 11 KG, 22 KG, or 44 KG LPG products in Product Master to monitor real-time levels."
                )
            } else {
                val maxVal = remember(metrics, showStoreFull, showFleetFull, showStoreEmpty, showFleetEmpty) {
                    val values = metrics.flatMap { m ->
                        buildList {
                            if (showStoreFull) add(m.storeFull)
                            if (showFleetFull) add(m.fleetFull)
                            if (showStoreEmpty) add(m.storeEmpty)
                            if (showFleetEmpty) add(m.fleetEmpty)
                        }
                    }
                    max(4, values.maxOrNull() ?: 4)
                }

                // Interactive Tooltip Card for selected cylinder size
                val activeMetric = metrics.getOrNull(selectedIdx)
                if (activeMetric != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) ObsidianElevated else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MetallicOrange.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lpg_inventory_tooltip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LPG ${activeMetric.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MetallicOrangeBright
                                )
                                Text(
                                    text = "Today Sold: ${activeMetric.todaySales} • Damaged: ${activeMetric.damaged}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mutedColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TooltipStatPill("Store F", activeMetric.storeFull, MetallicOrangeBright)
                                TooltipStatPill("Fleet F", activeMetric.fleetFull, ElectricBlueGlow)
                                TooltipStatPill("Empty", activeMetric.totalEmpty, BrandGreenLight)
                            }
                        }
                    }
                }

                // Canvas Grouped Bar Chart with Y-Axis Ticks & Dashed Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Y-Axis Scale Labels
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .height(166.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf(maxVal, (maxVal * 0.66f).roundToInt(), (maxVal * 0.33f).roundToInt(), 0).forEach { tick ->
                            Text(
                                text = tick.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                color = mutedColor
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(166.dp)
                                .pointerInput(metrics) {
                                    detectTapGestures { offset ->
                                        if (metrics.isNotEmpty() && size.width > 0) {
                                            val groupW = size.width.toFloat() / metrics.size
                                            selectedIdx = (offset.x / groupW).toInt().coerceIn(0, metrics.lastIndex)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            val gridColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)

                            // Horizontal reference lines
                            for (i in 0..3) {
                                val y = h * (i / 3f)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (i < 3) dashEffect else null
                                )
                            }

                            val groupWidth = w / metrics.size
                            metrics.forEachIndexed { idx, metric ->
                                val groupStart = idx * groupWidth
                                if (idx == selectedIdx) {
                                    drawRoundRect(
                                        color = MetallicOrange.copy(alpha = if (isDark) 0.11f else 0.08f),
                                        topLeft = Offset(groupStart + 4.dp.toPx(), 0f),
                                        size = Size(groupWidth - 8.dp.toPx(), h),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    )
                                }

                                val bars = buildList {
                                    if (showStoreFull) add(metric.storeFull to listOf(MetallicOrangeGold, MetallicOrange))
                                    if (showFleetFull) add(metric.fleetFull to listOf(NeonCyanGlow, RoyalElectricBlue))
                                    if (showStoreEmpty) add(metric.storeEmpty to listOf(BrandGreenLight, BrandGreenPrimary))
                                    if (showFleetEmpty) add(metric.fleetEmpty to listOf(ChromeSilverHighlight, PlatinumSilver))
                                }
                                if (bars.isNotEmpty()) {
                                    val innerPad = 12.dp.toPx()
                                    val barGap = 4.dp.toPx()
                                    val available = (groupWidth - innerPad * 2 - barGap * (bars.size - 1)).coerceAtLeast(8f)
                                    val barW = (available / bars.size).coerceAtMost(24.dp.toPx())
                                    val totalBarsW = barW * bars.size + barGap * (bars.size - 1)
                                    var x = groupStart + (groupWidth - totalBarsW) / 2f

                                    bars.forEach { (value, colors) ->
                                        val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) * animProgress
                                        val barH = (h - 6.dp.toPx()) * ratio
                                        if (barH > 1f) {
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = colors,
                                                    startY = h - barH,
                                                    endY = h
                                                ),
                                                topLeft = Offset(x, h - barH),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                            )
                                        }
                                        x += barW + barGap
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // X-Axis Cylinder Size Labels (clickable too)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            metrics.forEachIndexed { idx, metric ->
                                val isSel = idx == selectedIdx
                                Text(
                                    text = metric.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSel) MetallicOrangeBright else mutedColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedIdx = idx }
                                        .padding(vertical = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Recharts-style Smooth Area & Line Chart for Recent LPG & All-Gas Sales Trends
 * streamed from Firestore, with tap/drag crosshair scrubbing and live tooltip inspection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LpgSalesTrendAreaChart(
    points: List<SalesTrendPoint>,
    selectedRange: AnalyticsTimeRange,
    onRangeSelected: (AnalyticsTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var showLpgSales by remember { mutableStateOf(true) }
    var showLpgRestocks by remember { mutableStateOf(true) }
    var showAllGasSales by remember { mutableStateOf(false) }
    var selectedIdx by remember(points) { mutableIntStateOf((points.lastIndex).coerceAtLeast(0)) }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(points, selectedRange) { animateTrigger = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "lpg_area_anim"
    )

    val cardBg = if (isDark) DeepSpaceNavy.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) ElectricBlueBright.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
    val titleColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lpg_sales_trend_area_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlueBright.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Sales Trend Chart",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Recent Sales Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Text(
                            text = "Tap or slide across chart to inspect daily volume",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnalyticsTimeRange.entries.forEach { range ->
                        val active = range == selectedRange
                        FilterChip(
                            selected = active,
                            onClick = { onRangeSelected(range) },
                            label = {
                                Text(
                                    range.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlueBright,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("analytics_range_${range.label.lowercase()}")
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartSeriesToggleChip("LPG Sales", MoltenOrangeGlow, showLpgSales, "toggle_trend_lpg_sales") {
                    showLpgSales = !showLpgSales
                }
                ChartSeriesToggleChip("LPG Restock", ElectricBlueGlow, showLpgRestocks, "toggle_trend_lpg_restock") {
                    showLpgRestocks = !showLpgRestocks
                }
                ChartSeriesToggleChip("All-Gas Sales", BrandGreenLight, showAllGasSales, "toggle_trend_all_sales") {
                    showAllGasSales = !showAllGasSales
                }
            }

            if (points.isEmpty()) {
                ChartEmptyPlaceholder(
                    message = "No sales history available in this window.",
                    hint = "Record LPG deliveries or purchases to view real-time trend curves."
                )
            } else {
                val inspected = points.getOrNull(selectedIdx)
                if (inspected != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) ObsidianElevated else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lpg_sales_trend_tooltip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = inspected.shortLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricBlueGlow
                                )
                                Text(
                                    text = inspected.dateIso,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                    color = mutedColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TooltipStatPill("LPG Sold", inspected.lpgSales, MoltenOrangeGlow)
                                TooltipStatPill("Restock", inspected.lpgRestocks, ElectricBlueGlow)
                                TooltipStatPill("All Gas", inspected.allGasSales, BrandGreenLight)
                            }
                        }
                    }
                }

                val maxY = remember(points, showLpgSales, showLpgRestocks, showAllGasSales) {
                    val vals = points.flatMap { p ->
                        buildList {
                            if (showLpgSales) add(p.lpgSales)
                            if (showLpgRestocks) add(p.lpgRestocks)
                            if (showAllGasSales) add(p.allGasSales)
                        }
                    }
                    max(4, vals.maxOrNull() ?: 4)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(198.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .height(168.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf(maxY, (maxY * 0.66f).roundToInt(), (maxY * 0.33f).roundToInt(), 0).forEach { tick ->
                            Text(
                                text = tick.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                color = mutedColor
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .pointerInput(points) {
                                    detectTapGestures { offset ->
                                        if (points.size > 1 && size.width > 0) {
                                            val idx = ((offset.x / size.width) * (points.size - 1)).roundToInt()
                                            selectedIdx = idx.coerceIn(0, points.lastIndex)
                                        }
                                    }
                                }
                                .pointerInput(points) {
                                    detectHorizontalDragGestures { change, _ ->
                                        if (points.size > 1 && size.width > 0) {
                                            val idx = ((change.position.x / size.width) * (points.size - 1)).roundToInt()
                                            selectedIdx = idx.coerceIn(0, points.lastIndex)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val topPad = 10.dp.toPx()
                            val usableH = (h - topPad).coerceAtLeast(10f)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            val gridColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)

                            for (i in 0..3) {
                                val y = topPad + usableH * (i / 3f)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (i < 3) dashEffect else null
                                )
                            }

                            val stepX = if (points.size > 1) w / (points.size - 1) else w

                            fun drawSmoothSeries(values: List<Int>, strokeColor: Color, fillArea: Boolean) {
                                if (values.isEmpty()) return
                                val coords = values.mapIndexed { idx, v ->
                                    val x = idx * stepX
                                    val y = h - ((v.toFloat() / maxY.toFloat()).coerceIn(0f, 1f) * usableH * animProgress)
                                    Offset(x, y)
                                }

                                val linePath = Path().apply {
                                    moveTo(coords.first().x, coords.first().y)
                                    for (i in 0 until coords.lastIndex) {
                                        val p0 = coords[i]
                                        val p1 = coords[i + 1]
                                        val midX = (p0.x + p1.x) / 2f
                                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                    }
                                }

                                if (fillArea && coords.size > 1) {
                                    val areaPath = Path().apply {
                                        addPath(linePath)
                                        lineTo(coords.last().x, h)
                                        lineTo(coords.first().x, h)
                                        close()
                                    }
                                    drawPath(
                                        path = areaPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(strokeColor.copy(alpha = 0.34f), strokeColor.copy(alpha = 0.02f)),
                                            startY = topPad,
                                            endY = h
                                        )
                                    )
                                }

                                drawPath(
                                    path = linePath,
                                    color = strokeColor,
                                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )

                                // Draw crisp point markers (for 7D/14D or selected node)
                                coords.forEachIndexed { idx, pt ->
                                    if (points.size <= 14 || idx == selectedIdx) {
                                        val r = if (idx == selectedIdx) 5.dp.toPx() else 3.dp.toPx()
                                        drawCircle(color = strokeColor, radius = r, center = pt)
                                        if (idx == selectedIdx) {
                                            drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = pt)
                                        }
                                    }
                                }
                            }

                            if (showAllGasSales) {
                                drawSmoothSeries(points.map { it.allGasSales }, BrandGreenLight, fillArea = false)
                            }
                            if (showLpgRestocks) {
                                drawSmoothSeries(points.map { it.lpgRestocks }, ElectricBlueGlow, fillArea = true)
                            }
                            if (showLpgSales) {
                                drawSmoothSeries(points.map { it.lpgSales }, MoltenOrangeGlow, fillArea = true)
                            }

                            // Active Vertical Scrub Crosshair
                            if (selectedIdx in points.indices) {
                                val cx = selectedIdx * stepX
                                drawLine(
                                    color = MetallicOrangeBright.copy(alpha = 0.65f),
                                    start = Offset(cx, 0f),
                                    end = Offset(cx, h),
                                    strokeWidth = 1.4.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // Evenly spaced X-axis date ticks
                        val tickIndices = remember(points.size) {
                            when {
                                points.size <= 7 -> points.indices.toList()
                                else -> listOf(0, points.size / 3, (points.size * 2) / 3, points.lastIndex).distinct()
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            tickIndices.forEach { idx ->
                                Text(
                                    text = points[idx].shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                    color = if (idx == selectedIdx) ElectricBlueGlow else mutedColor,
                                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Radial Donut Chart tracking LPG Empty Return Company distribution (Arab Gas vs Emirates Gas).
 * Strictly represents empty-return company identifiers in LPG Exchange, never standalone LPG products.
 */
@Composable
fun LpgEmptyReturnDonutChart(
    returnShare: LpgCompanyReturnShare,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) DeepSpaceNavy.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) MetallicOrange.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
    val titleColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lpg_empty_return_donut_card"),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "LPG Empty Returns by Company",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Text(
                text = "Arab Gas & Emirates Gas tracked strictly as LPG Exchange return companies",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                        val total = returnShare.totalReturns
                        if (total <= 0) {
                            drawArc(
                                color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = stroke
                            )
                        } else {
                            var start = -90f
                            val segments = listOf(
                                returnShare.arabGasReturns to MetallicOrangeBright,
                                returnShare.emiratesGasReturns to ElectricBlueGlow,
                                returnShare.otherReturns to ChromeSilverHighlight
                            )
                            segments.forEach { (qty, color) ->
                                if (qty > 0) {
                                    val sweep = (qty.toFloat() / total.toFloat()) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = start,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = stroke
                                    )
                                    start += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = returnShare.totalReturns.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                        Text(
                            text = "EMPTIES",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = mutedColor
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val total = returnShare.totalReturns.coerceAtLeast(1)
                    DonutLegendRow(
                        label = "Arab Gas",
                        count = returnShare.arabGasReturns,
                        percent = if (returnShare.totalReturns > 0) (returnShare.arabGasReturns * 100 / total) else 0,
                        color = MetallicOrangeBright,
                        titleColor = titleColor,
                        mutedColor = mutedColor
                    )
                    DonutLegendRow(
                        label = "Emirates Gas",
                        count = returnShare.emiratesGasReturns,
                        percent = if (returnShare.totalReturns > 0) (returnShare.emiratesGasReturns * 100 / total) else 0,
                        color = ElectricBlueGlow,
                        titleColor = titleColor,
                        mutedColor = mutedColor
                    )
                    if (returnShare.otherReturns > 0) {
                        DonutLegendRow(
                            label = "Other",
                            count = returnShare.otherReturns,
                            percent = returnShare.otherReturns * 100 / total,
                            color = ChromeSilverHighlight,
                            titleColor = titleColor,
                            mutedColor = mutedColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartSeriesToggleChip(
    label: String,
    accent: Color,
    selected: Boolean,
    tag: String,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else accent)
            )
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.85f),
            selectedLabelColor = Color.White
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun TooltipStatPill(label: String, value: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = accent.copy(alpha = 0.85f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
            fontWeight = FontWeight.ExtraBold,
            color = accent
        )
    }
}

@Composable
private fun DonutLegendRow(
    label: String,
    count: Int,
    percent: Int,
    color: Color,
    titleColor: Color,
    mutedColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = titleColor)
        }
        Text(
            text = "$count ($percent%)",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
            fontWeight = FontWeight.Bold,
            color = mutedColor
        )
    }
}

@Composable
private fun ChartEmptyPlaceholder(message: String, hint: String) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) ObsidianElevated else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
