package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.LowStockAlertState
import com.example.data.model.StoreStockRow
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StockCalculatorDialog
import com.example.ui.components.StockPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateOnContainer
import com.example.ui.theme.SlateTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    adminName: String,
    selectedDate: String,
    summary: DashboardSummary?,
    storeRows: List<StoreStockRow> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    pendingCustomerCount: Int = 0,
    totalPendingEmptyCount: Int = 0,
    lowStockAlert: LowStockAlertState = LowStockAlertState(),
    onDateSelected: (String) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenVehicleDetail: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onUpdateLowStockThreshold: (Int) -> Unit = {},
    onDismissLowStockAlert: () -> Unit = {},
    onUpdateTransaction: (txId: Long, productId: Long, qty: Int, purchaseCompany: String, customerName: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onLogout: () -> Unit
) {
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var showCalculator by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "CONCORD & A TO Z GAS",
            subtitle = "Admin Dashboard • Welcome, $adminName",
            trailingContent = {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("admin_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }

            // Quick Navigation Section (Material 3 Cards for Inventory, Store, Vehicles)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Navigation",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickNavCard(
                            title = "Inventory",
                            subtitle = summary?.let { "${it.totalFullCylinders} Full • ${it.totalEmptyCylinders} Emp" } ?: "All Cylinders",
                            icon = Icons.Default.Inventory2,
                            accentColor = BrandGreenPrimary,
                            containerColor = BrandGreenContainer,
                            onContainerColor = BrandGreenOnContainer,
                            onClick = onOpenInventory,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_nav_inventory")
                        )
                        QuickNavCard(
                            title = "Store",
                            subtitle = summary?.let { "${it.storeFullBalance}F / ${it.storeEmptyBalance}E Yard" } ?: "Yard Stock",
                            icon = Icons.Default.Store,
                            accentColor = SlateTertiary,
                            containerColor = SlateContainer,
                            onContainerColor = SlateOnContainer,
                            onClick = onOpenStore,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_nav_store")
                        )
                        QuickNavCard(
                            title = "Vehicles",
                            subtitle = summary?.let { "${it.vehicleOverviews.size} Active Fleet" } ?: "All Vehicles",
                            icon = Icons.Default.LocalShipping,
                            accentColor = BrandRedSecondary,
                            containerColor = BrandRedContainer,
                            onContainerColor = BrandRedOnContainer,
                            onClick = onOpenVehicles,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_nav_vehicles")
                        )
                    }
                }
            }

            // Reactive Low Stock Alert Card
            item {
                AnimatedVisibility(
                    visible = lowStockAlert.shouldShowBanner,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LowStockAlertBannerCard(
                        alertState = lowStockAlert,
                        onOpenStore = onOpenStore,
                        onConfigureThreshold = { showThresholdDialog = true },
                        onDismiss = onDismissLowStockAlert
                    )
                }
            }

            if (pendingCustomerCount > 0 || totalPendingEmptyCount > 0) {
                item {
                    Card(
                        onClick = onOpenReports,
                        colors = CardDefaults.cardColors(containerColor = BrandRedContainer),
                        border = BorderStroke(1.dp, BrandRedSecondary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Recycling,
                                    contentDescription = "Customer Pending Empty",
                                    tint = BrandRedOnContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Customer Pending Empty Balance",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrandRedOnContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$pendingCustomerCount customers owe $totalPendingEmptyCount empty cylinders",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandRedOnContainer
                                    )
                                }
                            }
                            Text(
                                text = "View",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandRedSecondary
                            )
                        }
                    }
                }
            }

            if (summary != null) {
                // Hero KPIs: Total Full & Total Empty
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "TOTAL FULL",
                            value = summary.totalFullCylinders.toString(),
                            subtitle = "Store ${summary.storeFullBalance} + Veh ${summary.allVehiclesFullBalance}",
                            icon = Icons.Default.PropaneTank,
                            accentColor = BrandGreenPrimary,
                            containerColor = BrandGreenContainer,
                            onContainerColor = BrandGreenOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TOTAL EMPTY",
                            value = summary.totalEmptyCylinders.toString(),
                            subtitle = "Store ${summary.storeEmptyBalance} + Veh ${summary.allVehiclesEmptyBalance}",
                            icon = Icons.Default.Recycling,
                            accentColor = BrandRedSecondary,
                            containerColor = BrandRedContainer,
                            onContainerColor = BrandRedOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Store Balance & Today's Purchases / Dispatch / Sales
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "STORE BALANCE",
                            value = "${summary.storeFullBalance}/${summary.storeEmptyBalance}",
                            subtitle = "Full / Empty in Yard",
                            icon = Icons.Default.Store,
                            accentColor = SlateTertiary,
                            containerColor = SlateContainer,
                            onContainerColor = SlateOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TODAY'S SALES",
                            value = summary.todaySales.toString(),
                            subtitle = "Cylinders sold today",
                            icon = Icons.Default.Sell,
                            accentColor = BrandGreenPrimary,
                            containerColor = BrandGreenContainer,
                            onContainerColor = BrandGreenOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "TODAY PURCHASES",
                            value = "${summary.todayFullPurchases + summary.todayEmptyPurchases}",
                            subtitle = "Full ${summary.todayFullPurchases} • Emp ${summary.todayEmptyPurchases}",
                            icon = Icons.Default.AddShoppingCart,
                            accentColor = AmberWarning,
                            containerColor = AmberContainer,
                            onContainerColor = AmberOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TODAY DISPATCH",
                            value = "${summary.todayFullDispatch}",
                            subtitle = "Full out • Emp out ${summary.todayEmptyDispatch}",
                            icon = Icons.Default.LocalShipping,
                            accentColor = BrandRedSecondary,
                            containerColor = BrandRedContainer,
                            onContainerColor = BrandRedOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Reports + Calculate + Alert Limit Shortcut Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenReports,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_reports_button")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reports", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = { showCalculator = true },
                            modifier = Modifier.testTag("dashboard_calculate_button")
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Calc", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { showThresholdDialog = true },
                            modifier = Modifier.testTag("dashboard_threshold_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Low Stock Threshold", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (lowStockAlert.threshold == 0) "Min: Off" else "Min: ${lowStockAlert.threshold}",
                                maxLines = 1
                            )
                        }
                    }
                }

                // Vehicle-Wise Stock Section
                item {
                    Text(
                        text = "Vehicle-Wise Stock (${summary.vehicleOverviews.size} Vehicles)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(summary.vehicleOverviews, key = { it.vehicle.id }) { overview ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenVehicleDetail(overview.vehicle.id) }
                            .testTag("dashboard_vehicle_${overview.vehicle.name}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = overview.vehicle.name,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = overview.vehicle.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StockPill(label = "Full", value = overview.totalFullBalance, isFull = true)
                                    StockPill(label = "Empty", value = overview.totalEmptyBalance, isFull = false)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recd: ${overview.totalFullReceived}  •  Sold: ${overview.totalCustomerSales}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMono,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Cust Emp: ${overview.totalCustomerEmptyReceived}  •  Ret: ${overview.totalEmptyReturnedToStore}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMono,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Staff Activity Section
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Staff activity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Staff Activity ($selectedDate)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (summary.recentTransactions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No transactions recorded for $selectedDate yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(summary.recentTransactions, key = { it.transaction.id }) { item ->
                        val tx = item.transaction
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.timestamp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        text = "${tx.type.label} • ${item.productName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.productCategory.displayName} • By ${tx.staffName}" +
                                            (tx.vehicleName?.let { " • Veh: $it" } ?: "") +
                                            " • $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.purchaseCompanyName.isNotBlank()) {
                                        Text(
                                            text = "Company: ${tx.purchaseCompanyName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (tx.customerName.isNotBlank()) {
                                        Text(
                                            text = "Customer: ${tx.customerName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandRedSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = BrandGreenContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "×${tx.quantity}",
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreenOnContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (products.isNotEmpty()) {
                                        IconButton(onClick = { editingTransaction = tx }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit transaction",
                                                tint = BrandGreenPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CONCORD & A TO Z GAS • Built by Ragavan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (showCalculator) {
        StockCalculatorDialog(
            products = products.filter { it.isActive }.ifEmpty { products },
            storeFullProvider = { prodId -> storeRows.firstOrNull { it.product.id == prodId }?.storeFullBalance ?: 0 },
            storeEmptyProvider = { prodId -> storeRows.firstOrNull { it.product.id == prodId }?.storeEmptyBalance ?: 0 },
            onDismiss = { showCalculator = false }
        )
    }

    if (showThresholdDialog) {
        LowStockThresholdDialog(
            currentThreshold = lowStockAlert.threshold,
            onDismiss = { showThresholdDialog = false },
            onSave = { newVal ->
                onUpdateLowStockThreshold(newVal)
                showThresholdDialog = false
            }
        )
    }

    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            products = products,
            onDismiss = { editingTransaction = null },
            onConfirm = { prodId, qty, company, custName, notes ->
                onUpdateTransaction(tx.id, prodId, qty, company, custName, notes)
            }
        )
    }
}

@Composable
private fun LowStockAlertBannerCard(
    alertState: LowStockAlertState,
    onOpenStore: () -> Unit,
    onConfigureThreshold: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleItems = if (expanded) alertState.lowStockItems else alertState.lowStockItems.take(3)
    val hasOutOfStock = alertState.outOfStockCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_low_stock_alert_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AmberContainer),
        border = BorderStroke(1.5.dp, if (hasOutOfStock) BrandRedSecondary else AmberWarning)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (hasOutOfStock) BrandRedSecondary else AmberWarning),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Low Stock Alert",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Low Stock Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberOnContainer
                            )
                            Surface(
                                color = if (hasOutOfStock) BrandRedSecondary else AmberWarning,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${alertState.activeAlertCount} Low",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Store full stock below ${alertState.threshold} cylinders" +
                                if (hasOutOfStock) " • ${alertState.outOfStockCount} out of stock" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberOnContainer
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_low_stock_alert")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss low stock alert",
                        tint = AmberOnContainer
                    )
                }
            }

            HorizontalDivider(color = AmberWarning.copy(alpha = 0.35f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                visibleItems.forEach { item ->
                    val isZero = item.storeFullBalance <= 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.category.shortCode} • ${item.productName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberOnContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Fleet: ${item.vehiclesFullBalance} Full • Total: ${item.totalFullBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = AmberOnContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            color = if (isZero) BrandRedSecondary else AmberWarning,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isZero) "OUT (0)" else "Store: ${item.storeFullBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (alertState.lowStockItems.size > 3) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("low_stock_expand_toggle")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AmberOnContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "Show fewer products" else "Show all ${alertState.lowStockItems.size} low-stock products",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberOnContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenStore,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("low_stock_restock_button")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restock Store")
                }
                OutlinedButton(
                    onClick = onConfigureThreshold,
                    border = BorderStroke(1.dp, AmberOnContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("low_stock_threshold_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AmberOnContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Min: ${alertState.threshold}", color = AmberOnContainer)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LowStockThresholdDialog(
    currentThreshold: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var thresholdText by remember(currentThreshold) { mutableStateOf(currentThreshold.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Low Stock Alert Threshold") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Alert on Dashboard whenever any active product's Store Full balance drops below this number (set 0 to disable alerts).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) {
                            thresholdText = input
                            errorText = null
                        }
                    },
                    label = { Text("Minimum Full Cylinders") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("low_stock_threshold_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Off (0)", 3 to "3", 5 to "5", 10 to "10", 20 to "20").forEach { (preset, label) ->
                        FilterChip(
                            selected = thresholdText == preset.toString(),
                            onClick = {
                                thresholdText = preset.toString()
                                errorText = null
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = thresholdText.toIntOrNull()
                    if (parsed == null || parsed < 0 || parsed > 999) {
                        errorText = "Enter a number between 0 and 999"
                    } else {
                        onSave(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("save_low_stock_threshold_button")
            ) {
                Text("Save Threshold")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = onContainerColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
