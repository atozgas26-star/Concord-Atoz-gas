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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PendingStoreTransferEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.LowStockAlertState
import com.example.notifications.StaffLoginNotificationItem
import com.example.ui.components.AdminDailyWorkHoursCard
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.BrandHeaderBanner
import com.example.update.AppUpdateStatus
import com.example.update.AppUpdateUiState
import com.example.data.model.CompanyStockRow
import com.example.data.model.StoreStockRow
import com.example.ui.components.CurrentInventoryTotalsCard
import com.example.ui.components.DashboardInsideOutsideFolderCard
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.HeaderBellIconButton
import com.example.ui.components.HeaderChatIconButton
import com.example.ui.components.HeaderUpdateIconButton
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.LuxurySectionReveal
import com.example.ui.components.PendingDeliveriesSummaryCard
import com.example.ui.components.PendingStoreTransferCard
import com.example.ui.components.RecentStoreActivityCard
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.StockPill
import com.example.ui.components.luxuryTapScale
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueOnSoft
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.RoyalElectricBlue
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
    companyStockRows: List<CompanyStockRow> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    pendingCustomerCount: Int = 0,
    totalPendingEmptyCount: Int = 0,
    lowStockAlert: LowStockAlertState = LowStockAlertState(),
    isReadOnly: Boolean = false,
    staffLoginNotifications: List<StaffLoginNotificationItem> = emptyList(),
    latestStaffLoginBanner: StaffLoginNotificationItem? = null,
    onDismissStaffLoginBanner: () -> Unit = {},
    darkModeEnabled: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onOpenAppUpdate: () -> Unit = {},
    onDismissAppUpdateBanner: () -> Unit = {},
    onDateSelected: (String) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenVehicleDetail: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onOpenWorkHoursReport: () -> Unit = onOpenReports,
    users: List<UserAccountEntity> = emptyList(),
    vehicles: List<VehicleEntity> = emptyList(),
    todayAttendance: List<StaffAttendanceEntity> = emptyList(),
    pendingStoreTransfers: List<PendingStoreTransferEntity> = emptyList(),
    unreadNotificationCount: Int = 0,
    unreadChatCount: Int = 0,
    onOpenNotificationsBell: () -> Unit = {},
    onOpenTeamChat: () -> Unit = {},
    onApproveStoreTransfer: (Long) -> Unit = {},
    onRejectStoreTransfer: (Long) -> Unit = {},
    onUpdateLowStockThreshold: (Int) -> Unit = {},
    onDismissLowStockAlert: () -> Unit = {},
    onUpdateTransaction: (txId: Long, productId: Long, qty: Int, purchaseCompany: String, customerName: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onLogout: () -> Unit
) {
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "Concord & Atoz Gas Trading LLC",
            subtitle = if (isReadOnly) {
                "Owner View (Read-Only) • No Entry or Modification"
            } else {
                "Admin Dashboard • Welcome, $adminName"
            },
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.AdminGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.AdminAccent,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasUpdate = appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE
                    HeaderBellIconButton(
                        unreadCount = unreadNotificationCount,
                        onClick = onOpenNotificationsBell,
                        testTag = "admin_bell_notifications_button"
                    )
                    HeaderChatIconButton(
                        unreadCount = unreadChatCount,
                        onClick = onOpenTeamChat,
                        testTag = "admin_team_chat_button"
                    )
                    HeaderUpdateIconButton(
                        hasUpdate = hasUpdate,
                        isChecking = appUpdateState.isChecking,
                        onClick = onOpenAppUpdate,
                        testTag = "admin_app_update_button"
                    )
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("admin_dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (darkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkModeEnabled) "Switch to Light Mode" else "Switch to Warehouse Dark Mode",
                            tint = Color.White
                        )
                    }
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
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appUpdateState.shouldShowBanner) {
                item {
                    AppUpdateBanner(
                        state = appUpdateState,
                        onOpenUpdateDialog = onOpenAppUpdate,
                        onDismiss = onDismissAppUpdateBanner
                    )
                }
            }
            if (isReadOnly) {
                item {
                    Surface(
                        color = BrandGreenContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_view_readonly_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Owner Read-Only Mode",
                                tint = BrandGreenOnContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OWNER VIEW • READ ONLY",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenOnContainer
                                )
                                Text(
                                    text = "Live monitoring active. Data entry, editing, and settings are locked.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandGreenOnContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            if (latestStaffLoginBanner != null) {
                item {
                    val bannerTime = SimpleDateFormat("hh:mm a", Locale.US)
                        .format(Date(latestStaffLoginBanner.loginTimestampMs))
                    val vehicleSuffix = latestStaffLoginBanner.vehicleName
                        .takeIf { it.isNotBlank() }
                        ?.let { " • Vehicle: $it" }
                        .orEmpty()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_login_alert_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandGreenContainer),
                        border = BorderStroke(1.5.dp, BrandGreenPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Staff App Login Notification",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Staff App Login Notification",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenOnContainer
                                    )
                                    Text(
                                        text = "${latestStaffLoginBanner.staffName}$vehicleSuffix signed in at $bannerTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandGreenOnContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissStaffLoginBanner,
                                modifier = Modifier.testTag("dismiss_staff_login_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss staff login alert",
                                    tint = BrandGreenOnContainer
                                )
                            }
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

            // Quick Navigation Section (Editorial Bento Command Tiles)
            item {
                LuxurySectionReveal(delayMillis = 30) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LuxurySectionHeader(
                            eyebrow = "EXECUTIVE COMMAND",
                            title = "Quick Navigation",
                            accentColor = RoyalElectricBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickNavCard(
                                title = "Inventory",
                                subtitle = summary?.let { "${it.totalFullCylinders} Full • ${it.totalEmptyCylinders} Emp" } ?: "All Cylinders",
                                icon = Icons.Default.Inventory2,
                                accentColor = RoyalElectricBlue,
                                containerColor = ElectricBlueSoft,
                                onContainerColor = ElectricBlueOnSoft,
                                onClick = onOpenInventory,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_nav_inventory")
                            )
                            QuickNavCard(
                                title = "Store",
                                subtitle = summary?.let { "${it.storeFullBalance}F / ${it.storeEmptyBalance}E Yard" } ?: "Yard Stock",
                                icon = Icons.Default.Store,
                                accentColor = BrandGreenPrimary,
                                containerColor = BrandGreenContainer,
                                onContainerColor = BrandGreenOnContainer,
                                onClick = onOpenStore,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_nav_store")
                            )
                            QuickNavCard(
                                title = "Vehicles",
                                subtitle = summary?.let { "${it.vehicleOverviews.size} Active Fleet" } ?: "All Vehicles",
                                icon = Icons.Default.LocalShipping,
                                accentColor = MetallicOrange,
                                containerColor = AmberContainer,
                                onContainerColor = AmberOnContainer,
                                onClick = onOpenVehicles,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_nav_vehicles")
                            )
                        }
                    }
                }
            }

            // Separate Inside (Main Store) & Outside (Fleet/Vehicles) Color-Coded Category Folders
            if (companyStockRows.isNotEmpty()) {
                item(key = "inside_outside_category_folders") {
                    LuxurySectionReveal(delayMillis = 70) {
                        DashboardInsideOutsideFolderCard(
                            companyStockRows = companyStockRows,
                            onOpenStore = onOpenStore,
                            onOpenVehicles = onOpenVehicles
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
                        isReadOnly = isReadOnly,
                        onOpenStore = onOpenStore,
                        onConfigureThreshold = { if (!isReadOnly) showThresholdDialog = true },
                        onDismiss = onDismissLowStockAlert
                    )
                }
            }

            // Pending Store-to-Vehicle Full & Vehicle-to-Store Empty Approvals
            if (pendingStoreTransfers.isNotEmpty()) {
                item {
                    Text(
                        text = "Pending Store Cylinder Approvals (${pendingStoreTransfers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandRedSecondary
                    )
                }
                items(pendingStoreTransfers, key = { "dash_pending_${it.id}" }) { req ->
                    PendingStoreTransferCard(
                        request = req,
                        canApprove = !isReadOnly,
                        onApprove = { onApproveStoreTransfer(req.id) },
                        onReject = { onRejectStoreTransfer(req.id) }
                    )
                }
            }

            // Admin & Owner-Only Daily Work Hours Card
            if (users.isNotEmpty()) {
                item {
                    AdminDailyWorkHoursCard(
                        selectedDate = selectedDate,
                        users = users,
                        vehicles = vehicles,
                        attendanceList = todayAttendance,
                        onOpenWorkHoursReport = onOpenWorkHoursReport
                    )
                }
            }

            // Staff App Login Notifications Feed Card
            if (staffLoginNotifications.isNotEmpty()) {
                item {
                    StaffLoginActivityCard(
                        events = staffLoginNotifications
                    )
                }
            }

            if (summary != null) {
                // Summary Card 1: Current Inventory Totals
                item {
                    CurrentInventoryTotalsCard(
                        summary = summary,
                        onOpenInventory = onOpenInventory
                    )
                }

                // Summary Card 2: Pending Deliveries (Vehicles on Route + Pending Empty Pickups)
                item {
                    PendingDeliveriesSummaryCard(
                        summary = summary,
                        pendingCustomerCount = pendingCustomerCount,
                        totalPendingEmptyCount = totalPendingEmptyCount,
                        onOpenVehicles = onOpenVehicles,
                        onOpenVehicleDetail = onOpenVehicleDetail,
                        onOpenReports = onOpenReports
                    )
                }

                // Summary Card 3: Recent Store Activity
                item {
                    RecentStoreActivityCard(
                        summary = summary,
                        selectedDate = selectedDate,
                        canEdit = !isReadOnly && products.isNotEmpty(),
                        onOpenStore = onOpenStore,
                        onEditTransaction = { tx -> if (!isReadOnly) editingTransaction = tx }
                    )
                }

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

                // Reports + Alert Limit Shortcut Row
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
                        if (!isReadOnly) {
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
                }

                // Vehicle-Wise Stock Section
                item {
                    LuxurySectionHeader(
                        eyebrow = "FLEET OVERVIEW",
                        title = "Vehicle-Wise Stock (${summary.vehicleOverviews.size} Vehicles)",
                        accentColor = MetallicOrangeBright,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(summary.vehicleOverviews, key = { it.vehicle.id }) { overview ->
                    LuxuryGlassCard(
                        onClick = { onOpenVehicleDetail(overview.vehicle.id) },
                        accentGlow = RoyalElectricBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_vehicle_${overview.vehicle.name}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = ElectricBlueSoft,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = overview.vehicle.name,
                                                tint = RoyalElectricBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
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
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
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
                }

                // Staff Activity Section
                item {
                    LuxurySectionHeader(
                        eyebrow = "LIVE LEDGER",
                        title = "Staff Activity ($selectedDate)",
                        accentColor = ElectricBlueBright,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (summary.recentTransactions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
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
                        LuxuryGlassCard(accentGlow = RoyalElectricBlue) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.type.label} • ${item.productName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
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
                                            color = MetallicOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = ElectricBlueSoft,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "×${tx.quantity}",
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlueOnSoft,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    if (!isReadOnly && products.isNotEmpty()) {
                                        IconButton(onClick = { editingTransaction = tx }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit transaction",
                                                tint = RoyalElectricBlue,
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
                        text = "C&A GAS • Built by Ragavan",
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

    if (showThresholdDialog && !isReadOnly) {
        LowStockThresholdDialog(
            currentThreshold = lowStockAlert.threshold,
            onDismiss = { showThresholdDialog = false },
            onSave = { newVal ->
                onUpdateLowStockThreshold(newVal)
                showThresholdDialog = false
            }
        )
    }

    if (!isReadOnly) {
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
}

@Composable
private fun StaffLoginActivityCard(
    events: List<StaffLoginNotificationItem>
) {
    val visibleEvents = events.take(5)
    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_login_activity_card"),
        shape = RoundedCornerShape(18.dp),
        accentGlow = RoyalElectricBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Staff App Logins",
                        tint = BrandGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Staff App Login Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = BrandGreenContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${events.size} Logins",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreenOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            HorizontalDivider()
            visibleEvents.forEach { ev ->
                val timeLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                    .format(Date(ev.loginTimestampMs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ev.staffName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (ev.vehicleName.isNotBlank()) {
                            Text(
                                text = "Vehicle: ${ev.vehicleName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreenPrimary
                            )
                        }
                    }
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertBannerCard(
    alertState: LowStockAlertState,
    isReadOnly: Boolean = false,
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
                    Icon(
                        if (isReadOnly) Icons.Default.Store else Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isReadOnly) "View Store Stock" else "Restock Store")
                }
                if (!isReadOnly) {
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
    LuxuryGlassCard(
        onClick = onClick,
        accentGlow = accentColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .luxuryTapScale()
            .minimumInteractiveComponentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor, MetallicOrangeBright)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
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
