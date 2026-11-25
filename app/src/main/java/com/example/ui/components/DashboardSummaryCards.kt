package com.example.ui.components

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.StockTransactionEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.TransactionType
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.DarkHeaderEnd
import com.example.ui.theme.DarkHeaderStart
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Summary Card 1: Current Inventory Totals across Store Yard + Active Fleet.
 */
@Composable
fun CurrentInventoryTotalsCard(
    summary: DashboardSummary,
    onOpenInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grandTotal = summary.totalFullCylinders + summary.totalEmptyCylinders
    val fullRatio = if (grandTotal > 0) {
        (summary.totalFullCylinders.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    ElevatedCard(
        onClick = onOpenInventory,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_inventory_totals_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val isDark = LocalIsDarkTheme.current
            // Branded emerald header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            if (isDark) {
                                listOf(DarkHeaderStart, DarkHeaderEnd)
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                )
                            }
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Current Inventory Totals",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Current Inventory Totals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Store Yard + ${summary.vehicleOverviews.size} Fleet Vehicles",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = grandTotal.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "TOTAL CYL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Full vs Empty ratio bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Full: ${summary.totalFullCylinders}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenPrimary
                        )
                        Text(
                            text = "Empty: ${summary.totalEmptyCylinders}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = BrandRedSecondary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandRedContainer)
                    ) {
                        if (grandTotal > 0 && fullRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fullRatio)
                                    .fillMaxHeight()
                                    .background(BrandGreenPrimary)
                            )
                        }
                    }
                }

                // Store vs Fleet Breakdown Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = BrandGreenContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "STORE YARD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenOnContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${summary.storeFullBalance} Full  •  ${summary.storeEmptyBalance} Emp",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenOnContainer
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ON VEHICLES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
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
 * Summary Card 2: Pending Deliveries (undelivered full cylinders loaded on vehicles +
 * pending customer empty returns awaiting pickup).
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

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_pending_deliveries_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (hasPendingDeliveries) AmberContainer else BrandGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPendingDeliveries) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                            contentDescription = "Pending Deliveries",
                            tint = if (hasPendingDeliveries) AmberOnContainer else BrandGreenOnContainer,
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
                                "${summary.activeDeliveryVehiclesCount} vehicles out on route • ${summary.todaySales} delivered today"
                            } else {
                                "All vehicle routes clear • ${summary.todaySales} delivered today"
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
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Two key pending metrics side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (summary.pendingDeliveryCylinders > 0) AmberContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (summary.pendingDeliveryCylinders > 0) BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "OUT FOR DELIVERY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.pendingDeliveryCylinders > 0) AmberOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.pendingDeliveryCylinders} Full",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.pendingDeliveryCylinders > 0) AmberOnContainer else MaterialTheme.colorScheme.onSurface
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
                    color = if (totalPendingEmptyCount > 0) BrandRedContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (totalPendingEmptyCount > 0) BorderStroke(1.dp, BrandRedSecondary.copy(alpha = 0.45f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_empty_returns_tile")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Recycling,
                                contentDescription = null,
                                tint = if (totalPendingEmptyCount > 0) BrandRedOnContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "EMPTY PICKUPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendingEmptyCount > 0) BrandRedOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$totalPendingEmptyCount Emp",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalPendingEmptyCount > 0) BrandRedOnContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (pendingCustomerCount > 0) "$pendingCustomerCount customers owe" else "No overdue empties",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick tap chips for loaded delivery vehicles
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
                                containerColor = BrandGreenContainer.copy(alpha = 0.65f),
                                labelColor = BrandGreenOnContainer
                            ),
                            border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.35f)),
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
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_recent_store_activity_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Recent Store Activity",
                            tint = BrandGreenOnContainer,
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
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Compact Store Movement Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreMiniStatPill(
                    label = "Purchased",
                    value = "${summary.todayFullPurchases + summary.todayEmptyPurchases}",
                    container = BrandGreenContainer.copy(alpha = 0.6f),
                    content = BrandGreenOnContainer,
                    modifier = Modifier.weight(1f)
                )
                StoreMiniStatPill(
                    label = "Full Disp",
                    value = "${summary.todayFullDispatch}",
                    container = BrandRedContainer.copy(alpha = 0.6f),
                    content = BrandRedOnContainer,
                    modifier = Modifier.weight(1f)
                )
                StoreMiniStatPill(
                    label = "Empty Out",
                    value = "${summary.todayEmptyDispatch}",
                    container = AmberContainer.copy(alpha = 0.7f),
                    content = AmberOnContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.recentStoreActivity.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
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
                                    color = if (isOutward) BrandRedContainer else BrandGreenContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${if (isOutward) "-" else "+"}${tx.quantity}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutward) BrandRedOnContainer else BrandGreenOnContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
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
