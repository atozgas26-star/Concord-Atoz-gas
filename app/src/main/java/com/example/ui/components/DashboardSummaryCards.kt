package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StockTransactionEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.TransactionType
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeSoft
import com.example.ui.theme.MetallicOrangeOnSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumSilver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Executive Command Card 1: Current Inventory Totals across Store Yard + Active Fleet.
 * Styled with C&A GAS Obsidian Midnight Blue, Royal Electric Blue & Signature Metallic Orange accents.
 */
@Composable
fun CurrentInventoryTotalsCard(
    summary: DashboardSummary,
    onOpenInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grandTotal = summary.totalFullCylinders + summary.totalEmptyCylinders
    val targetRatio = if (grandTotal > 0) {
        (summary.totalFullCylinders.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val fullRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 480),
        label = "dashboard_full_ratio"
    )
    val isDark = LocalIsDarkTheme.current

    Surface(
        onClick = onOpenInventory,
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) ObsidianSurface else Color.White,
        shadowElevation = if (isDark) 10.dp else 5.dp,
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    ElectricBlueGlow.copy(alpha = 0.52f),
                    if (isDark) ObsidianBorder else PlatinumSilver,
                    MetallicOrangeBright.copy(alpha = 0.42f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .luxuryTapScale()
            .testTag("dashboard_inventory_totals_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Obsidian-Royal Atmospheric Executive Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .obsidianRoyalAtmosphere()
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlueBright.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ElectricBlueGlow.copy(alpha = 0.45f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Current Inventory Totals",
                                    tint = ArcticWhite,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ENTERPRISE STOCK COMMAND",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlueGlow
                            )
                            Text(
                                text = "Current Inventory Totals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArcticWhite
                            )
                            Text(
                                text = "Store Yard + ${summary.vehicleOverviews.size} Fleet Vehicles",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = PlatinumSilver
                            )
                        }
                    }

                    Surface(
                        color = ObsidianMidnight.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.48f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            AnimatedStockCounter(
                                count = grandTotal,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticWhite
                            )
                            Text(
                                text = "TOTAL CYL",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                color = MetallicOrangeBright
                            )
                        }
                    }
                }
            }

            SignatureAccentHairline(height = 2.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full vs Empty high-precision ratio bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FULL  ${summary.totalFullCylinders}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "EMPTY  ${summary.totalEmptyCylinders}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = MetallicOrangeBright
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) MetallicOrangeBright.copy(alpha = 0.25f)
                                else MetallicOrangeSoft
                            )
                    ) {
                        if (grandTotal > 0 && fullRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fullRatio)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                ElectricBlueGlow
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Store Yard vs Active Fleet Architectural Sub-Panels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (isDark) ObsidianElevated else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "STORE YARD",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${summary.storeFullBalance} Full  •  ${summary.storeEmptyBalance} Emp",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        color = if (isDark) ObsidianElevated else MetallicOrangeSoft.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            MetallicOrange.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ON VEHICLES",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MetallicOrangeBright else MetallicOrangeOnSoft
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${summary.allVehiclesFullBalance} Full  •  ${summary.allVehiclesEmptyBalance} Emp",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary Card 2: Pending Deliveries & Empty Pickups.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingDeliveriesSummaryCard(
    summary: DashboardSummary,
    pendingCustomerCount: Int,
    totalPendingEmptyCount: Int,
    onOpenVehicles: () -> Unit,
    onOpenVehicleDetail: (Long) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRouteVehicles = summary.vehicleOverviews
        .filter { it.totalFullBalance > 0 }
        .sortedByDescending { it.totalFullBalance }
    val hasPendingDeliveries = summary.pendingDeliveryCylinders > 0 || totalPendingEmptyCount > 0
    val isDark = LocalIsDarkTheme.current

    LuxuryGlassCard(
        shape = RoundedCornerShape(20.dp),
        accentGlow = if (hasPendingDeliveries) MetallicOrangeBright else ElectricBlueGlow,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_pending_deliveries_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasPendingDeliveries) MetallicOrange.copy(alpha = 0.16f)
                                else BrandGreenPrimary.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPendingDeliveries) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                            contentDescription = "Pending Deliveries",
                            tint = if (hasPendingDeliveries) MetallicOrangeBright else BrandGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pending Deliveries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (summary.activeDeliveryVehiclesCount > 0) {
                                "${summary.activeDeliveryVehiclesCount} vehicles on route • ${summary.todaySales} sold today"
                            } else {
                                "All vehicle routes clear • ${summary.todaySales} sold today"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onOpenVehicles,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("pending_deliveries_open_vehicles")
                ) {
                    Text("Fleet", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (summary.pendingDeliveryCylinders > 0) {
                        if (isDark) ObsidianElevated else MetallicOrangeSoft
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (summary.pendingDeliveryCylinders > 0) MetallicOrangeBright.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "OUT FOR DELIVERY",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.pendingDeliveryCylinders > 0) MetallicOrangeBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        AnimatedStockCounter(
                            count = summary.pendingDeliveryCylinders,
                            suffix = " Full",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.activeDeliveryVehiclesCount} active vehicles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onOpenReports,
                    color = if (totalPendingEmptyCount > 0) {
                        if (isDark) Color(0xFF2B1220) else BrandRedContainer.copy(alpha = 0.68f)
                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (totalPendingEmptyCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_empty_returns_tile")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Recycling,
                                contentDescription = null,
                                tint = if (totalPendingEmptyCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "EMPTY PICKUPS",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.6.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendingEmptyCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        AnimatedStockCounter(
                            count = totalPendingEmptyCount,
                            suffix = " Emp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (pendingCustomerCount > 0) "$pendingCustomerCount customers owe" else "No overdue empties",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (activeRouteVehicles.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeRouteVehicles.take(5).forEach { overview ->
                        AssistChip(
                            onClick = { onOpenVehicleDetail(overview.vehicle.id) },
                            label = {
                                Text(
                                    text = "${overview.vehicle.name}: ${overview.totalFullBalance}F",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.5f else 0.78f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("pending_vehicle_chip_${overview.vehicle.name}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Summary Card 3: Recent Store Activity (today's store purchases, inward, dispatches & returns).
 */
@Composable
fun RecentStoreActivityCard(
    summary: DashboardSummary,
    selectedDate: String,
    canEdit: Boolean,
    onOpenStore: () -> Unit,
    onEditTransaction: (StockTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    LuxuryGlassCard(
        shape = RoundedCornerShape(20.dp),
        accentGlow = ElectricBlueBright,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_recent_store_activity_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Recent Store Activity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Recent Store Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${summary.todayStoreActivityCount} store updates on $selectedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onOpenStore,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("recent_store_open_store")
                ) {
                    Text("Store", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreMiniStatPill(
                    label = "Purchased",
                    value = "${summary.todayFullPurchases + summary.todayEmptyPurchases}",
                    container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.78f),
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StoreMiniStatPill(
                    label = "Full Disp",
                    value = "${summary.todayFullDispatch}",
                    container = if (isDark) Color(0xFF2B1220) else BrandRedContainer.copy(alpha = 0.72f),
                    content = if (isDark) Color(0xFFFECACA) else BrandRedOnContainer,
                    modifier = Modifier.weight(1f)
                )
                StoreMiniStatPill(
                    label = "Empty Out",
                    value = "${summary.todayEmptyDispatch}",
                    container = if (isDark) Color(0xFF3A1A0B) else MetallicOrangeSoft,
                    content = if (isDark) MetallicOrangeBright else MetallicOrangeOnSoft,
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.recentStoreActivity.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No store activity yet for $selectedDate",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Record a supplier purchase, inward stock, or vehicle dispatch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onOpenStore,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Log")
                        }
                    }
                }
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.recentStoreActivity.take(5).forEachIndexed { index, item ->
                        val tx = item.transaction
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.timestamp))
                        val isOutward = tx.type == TransactionType.VEHICLE_FULL_RECEIVED ||
                            tx.type == TransactionType.STORE_FULL_DISPATCH_DIRECT ||
                            tx.type == TransactionType.STORE_EMPTY_DISPATCH ||
                            tx.type == TransactionType.STORE_ADJUST_FULL_OUT ||
                            tx.type == TransactionType.STORE_ADJUST_EMPTY_OUT

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tx.type.label} • ${item.productName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val detailParts = buildList {
                                    if (tx.purchaseCompanyName.isNotBlank()) add(tx.purchaseCompanyName)
                                    if (!tx.vehicleName.isNullOrBlank()) add("Veh: ${tx.vehicleName}")
                                    if (tx.customerName.isNotBlank()) add("Cust: ${tx.customerName}")
                                    add(timeStr)
                                }
                                Text(
                                    text = detailParts.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isOutward) {
                                        if (isDark) Color(0xFF3A1A0B) else MetallicOrangeSoft
                                    } else {
                                        if (isDark) Color(0xFF13295A) else BrandGreenContainer
                                    },
                                    shape = RoundedCornerShape(9.dp)
                                ) {
                                    Text(
                                        text = "${if (isOutward) "-" else "+"}${tx.quantity}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutward) {
                                            if (isDark) MetallicOrangeBright else MetallicOrangeOnSoft
                                        } else {
                                            if (isDark) ElectricBlueGlow else BrandGreenOnContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                if (canEdit) {
                                    IconButton(
                                        onClick = { onEditTransaction(tx) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit store activity",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < summary.recentStoreActivity.take(5).lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreMiniStatPill(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                color = content
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
