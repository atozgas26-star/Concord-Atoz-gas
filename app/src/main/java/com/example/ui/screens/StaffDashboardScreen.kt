package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PendingStoreTransferEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ApprovalStatus
import com.example.data.model.AutoLogoutMode
import com.example.data.model.CustomerPendingRow
import com.example.data.model.ProductCategory
import com.example.data.model.StoreStockRow
import com.example.data.model.StoreTransferDirection
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.VehicleStockRow
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.BrandHeaderBanner
import com.example.update.AppUpdateStatus
import com.example.update.AppUpdateUiState
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.HeaderBellIconButton
import com.example.ui.components.HeaderChatIconButton
import com.example.ui.components.HeaderUpdateIconButton
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PendingCustomerCard
import com.example.ui.components.StaffDutyAttendanceCard
import com.example.ui.components.StockCalculatorDialog
import com.example.ui.components.TransactionEntryDialog
import com.example.ui.components.VehicleTransferDialog
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StaffDashboardScreen(
    staffUser: UserAccountEntity,
    assignedVehicle: VehicleEntity?,
    selectedDate: String,
    vehicleRows: List<VehicleStockRow>,
    storeRows: List<StoreStockRow>,
    products: List<ProductEntity>,
    allTransactions: List<TransactionDisplayItem>,
    allVehicles: List<VehicleEntity> = emptyList(),
    pendingCustomers: List<CustomerPendingRow> = emptyList(),
    recentCustomerNames: List<String> = emptyList(),
    isOnlineSynced: Boolean = true,
    autoLoginEnabled: Boolean = true,
    autoLogoutMode: AutoLogoutMode = AutoLogoutMode.OFF,
    darkModeEnabled: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onAutoLoginChanged: (Boolean) -> Unit = {},
    onAutoLogoutModeChanged: (AutoLogoutMode) -> Unit = {},
    onDateSelected: (String) -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String, String, Int) -> Unit,
    onSubmitStoreTransferRequest: (StoreTransferDirection, Long, Int, String) -> Unit = { _, _, _, _ -> },
    onTransferBetweenVehicles: (
        sourceVehicleId: Long,
        targetVehicleId: Long,
        productId: Long,
        fullQty: Int,
        emptyQty: Int,
        extraFullQty: Int,
        extraEmptyQty: Int,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    todayAttendance: StaffAttendanceEntity? = null,
    onPunchDutyStage: (String) -> Unit = {},
    myStoreTransferRequests: List<PendingStoreTransferEntity> = emptyList(),
    unreadNotificationCount: Int = 0,
    unreadChatCount: Int = 0,
    onOpenNotificationsBell: () -> Unit = {},
    onOpenTeamChat: () -> Unit = {},
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onOpenAppUpdate: () -> Unit = {},
    onDismissAppUpdateBanner: () -> Unit = {},
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTxType by remember { mutableStateOf<TransactionType?>(null) }
    var prefillCustomerName by remember { mutableStateOf("") }
    var prefillQuantity by remember { mutableIntStateOf(1) }
    var showCalculator by remember { mutableStateOf(false) }
    var showVehicleTransferDialog by remember { mutableStateOf(false) }
    var showAutoSessionDialog by remember { mutableStateOf(false) }
    var staffSalesDailyMode by remember { mutableStateOf(true) }

    val filteredRows = remember(vehicleRows, selectedCategory, searchQuery) {
        vehicleRows.filter { row ->
            (selectedCategory == null || row.product.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    row.product.name.contains(searchQuery, ignoreCase = true) ||
                    row.product.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    // Strictly scoped to ONLY this staff member's assigned vehicle
    val myVehicleTransactions = remember(allTransactions, assignedVehicle, selectedDate) {
        if (assignedVehicle == null) emptyList()
        else allTransactions.filter {
            it.transaction.vehicleId == assignedVehicle.id &&
                it.transaction.businessDate == selectedDate
        }
    }

    val closingFull = vehicleRows.sumOf { it.fullBalance }
    val closingEmpty = vehicleRows.sumOf { it.emptyBalance }
    val totalReceived = vehicleRows.sumOf { it.fullReceived }
    val totalSales = vehicleRows.sumOf { it.customerSales }
    val totalCustEmpty = vehicleRows.sumOf { it.customerEmptyReceived }
    val totalRetEmpty = vehicleRows.sumOf { it.emptyReturnedToStore }
    val activePendingCount = pendingCustomers.count { it.pendingEmptyBalance > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        BrandHeaderBanner(
            title = "Concord & Atoz Gas Trading LLC",
            subtitle = "Staff: ${staffUser.displayName}  •  Vehicle: ${assignedVehicle?.name ?: "N/A"}",
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOnlineSynced) {
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Online Sync Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "ONLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    val hasUpdate = appUpdateState.status == AppUpdateStatus.UPDATE_AVAILABLE
                    HeaderBellIconButton(
                        unreadCount = unreadNotificationCount,
                        onClick = onOpenNotificationsBell,
                        testTag = "staff_bell_notifications_button"
                    )
                    HeaderChatIconButton(
                        unreadCount = unreadChatCount,
                        onClick = onOpenTeamChat,
                        testTag = "staff_team_chat_button"
                    )
                    HeaderUpdateIconButton(
                        hasUpdate = hasUpdate,
                        isChecking = appUpdateState.isChecking,
                        onClick = onOpenAppUpdate,
                        testTag = "staff_app_update_button"
                    )
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("staff_dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (darkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkModeEnabled) "Switch to Light Mode" else "Switch to Warehouse Dark Mode",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showAutoSessionDialog = true },
                        modifier = Modifier.testTag("staff_auto_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "Auto Login & Logout Settings",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("staff_logout_button")
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (appUpdateState.shouldShowBanner) {
                AppUpdateBanner(
                    state = appUpdateState,
                    onOpenUpdateDialog = onOpenAppUpdate,
                    onDismiss = onDismissAppUpdateBanner
                )
            }
            DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)

            // Daily Closing Balance Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "CLOSING FULL BAL",
                    value = closingFull.toString(),
                    subtitle = "Recd $totalReceived • Sold $totalSales",
                    icon = Icons.Default.PropaneTank,
                    accentColor = BrandGreenPrimary,
                    containerColor = BrandGreenContainer,
                    onContainerColor = BrandGreenOnContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "CLOSING EMPTY BAL",
                    value = closingEmpty.toString(),
                    subtitle = "Cust $totalCustEmpty • Ret $totalRetEmpty",
                    icon = Icons.Default.Recycling,
                    accentColor = BrandRedSecondary,
                    containerColor = BrandRedContainer,
                    onContainerColor = BrandRedOnContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4 Big Easy Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StaffBigActionButton(
                    label = "Receive Full\nfrom Store",
                    icon = Icons.AutoMirrored.Filled.Input,
                    containerColor = BrandGreenPrimary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_FULL_RECEIVED
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_receive_full")
                )
                StaffBigActionButton(
                    label = "Customer\nSale",
                    icon = Icons.Default.Sell,
                    containerColor = BrandRedSecondary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_CUSTOMER_SALE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_customer_sale")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StaffBigActionButton(
                    label = "Empty Recd\n(Customer)",
                    icon = Icons.Default.Recycling,
                    containerColor = BrandGreenPrimary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_empty_customer")
                )
                StaffBigActionButton(
                    label = "Return Empty\nto Store",
                    icon = Icons.Default.Output,
                    containerColor = BrandRedSecondary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_return_empty")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StaffBigActionButton(
                    label = "New Cust\nFull Sale",
                    icon = Icons.Default.PersonAddAlt1,
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_NEW_CUSTOMER_SALE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_new_customer_sale")
                )
                StaffBigActionButton(
                    label = "Complaint\nReplace",
                    icon = Icons.Default.SwapHoriz,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_complaint_replace")
                )
                OutlinedButton(
                    onClick = { showCalculator = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(58.dp)
                        .testTag("staff_action_calculate")
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculate")
                }
            }
            if (assignedVehicle != null && allVehicles.any { it.id != assignedVehicle.id }) {
                Button(
                    onClick = { showVehicleTransferDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("staff_action_vehicle_transfer")
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Vehicle → Vehicle Transfer (Full & Empty)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Stock", maxLines = 1) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Sales", maxLines = 1) },
                modifier = Modifier.testTag("staff_tab_sales")
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Pending ($activePendingCount)", maxLines = 1) },
                modifier = Modifier.testTag("staff_tab_pending")
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text("Activity (${myVehicleTransactions.size})", maxLines = 1) }
            )
        }

        when (activeTab) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search gas cylinder...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        CategoryFilterRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "staff_duty_card") {
                            StaffDutyAttendanceCard(
                                attendance = todayAttendance,
                                selectedDate = selectedDate,
                                onPunchStage = onPunchDutyStage
                            )
                        }
                        val recentRequests = myStoreTransferRequests.take(3)
                        if (recentRequests.isNotEmpty()) {
                            item(key = "staff_store_requests") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("staff_store_requests_card"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Store Transfer Admin Approvals",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        recentRequests.forEach { req ->
                                            val isFull = req.direction == StoreTransferDirection.STORE_TO_VEHICLE_FULL.name
                                            val label = if (isFull) "Full from Store" else "Empty to Store"
                                            val badgeColor = when (req.status) {
                                                ApprovalStatus.APPROVED.name -> BrandGreenPrimary
                                                ApprovalStatus.REJECTED.name -> BrandRedSecondary
                                                else -> Color(0xFFD97706)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${req.quantity} × ${req.productName} ($label)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeColor.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = req.status,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        items(filteredRows, key = { it.product.id }) { row ->
                            // Staff CANNOT change opening stock
                            VehicleProductRowCard(row = row, canEditOpening = false)
                        }
                    }
                }
            }

            1 -> {
                val activeYearMonth = selectedDate.take(7)
                val scopedSalesTxs = remember(allTransactions, assignedVehicle, selectedDate, staffSalesDailyMode) {
                    if (assignedVehicle == null) emptyList()
                    else allTransactions.filter { item ->
                        val t = item.transaction
                        val periodMatch = if (staffSalesDailyMode) {
                            t.businessDate == selectedDate
                        } else {
                            t.businessDate.startsWith(activeYearMonth)
                        }
                        t.vehicleId == assignedVehicle.id && periodMatch
                    }
                }
                data class StaffProductSaleSummary(
                    val product: ProductEntity,
                    val fullRecd: Int,
                    val regSales: Int,
                    val newCustSales: Int,
                    val custEmpty: Int,
                    val compRepl: Int
                ) {
                    val totalSold: Int get() = regSales + newCustSales
                }
                val productSummaries = remember(products, scopedSalesTxs) {
                    products.sortedWith(compareBy({ it.category.sortOrder }, { it.sortOrder }, { it.name })).map { p ->
                        val pTxs = scopedSalesTxs.filter { it.transaction.productId == p.id }.map { it.transaction }
                        StaffProductSaleSummary(
                            product = p,
                            fullRecd = pTxs.filter { it.type == TransactionType.VEHICLE_FULL_RECEIVED }.sumOf { it.quantity },
                            regSales = pTxs.filter { it.type == TransactionType.VEHICLE_CUSTOMER_SALE }.sumOf { it.quantity },
                            newCustSales = pTxs.filter { it.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE }.sumOf { it.quantity },
                            custEmpty = pTxs.filter { it.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER }.sumOf { it.quantity },
                            compRepl = pTxs.filter { it.type == TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE }.sumOf { it.quantity }
                        )
                    }.filter { it.totalSold > 0 || it.fullRecd > 0 || it.custEmpty > 0 || it.compRepl > 0 }
                }
                val totalSoldSum = productSummaries.sumOf { it.totalSold }
                val regSoldSum = productSummaries.sumOf { it.regSales }
                val newSoldSum = productSummaries.sumOf { it.newCustSales }
                val emptyCollSum = productSummaries.sumOf { it.custEmpty }
                val compReplSum = productSummaries.sumOf { it.compRepl }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = staffSalesDailyMode,
                                onClick = { staffSalesDailyMode = true },
                                label = { Text("Daily ($selectedDate)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRedSecondary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("staff_sales_daily_chip")
                            )
                            FilterChip(
                                selected = !staffSalesDailyMode,
                                onClick = { staffSalesDailyMode = false },
                                label = { Text("Monthly ($activeYearMonth)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGreenPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("staff_sales_monthly_chip")
                            )
                        }
                    }
                    item {
                        Surface(
                            color = BrandGreenContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (staffSalesDailyMode) {
                                        "Daily Cylinder Sales • ${assignedVehicle?.name ?: "Vehicle"}"
                                    } else {
                                        "Monthly Cylinder Sales ($activeYearMonth) • ${assignedVehicle?.name ?: "Vehicle"}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenOnContainer
                                )
                                Text(
                                    text = "Total Sold: $totalSoldSum (Reg $regSoldSum • New $newSoldSum)  •  Cust Empty: $emptyCollSum  •  Comp.Repl: $compReplSum",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandGreenOnContainer
                                )
                            }
                        }
                    }
                    if (productSummaries.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (staffSalesDailyMode) {
                                        "No cylinder sales recorded for ${assignedVehicle?.name ?: "your vehicle"} on $selectedDate."
                                    } else {
                                        "No cylinder sales recorded for ${assignedVehicle?.name ?: "your vehicle"} in $activeYearMonth."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(productSummaries, key = { it.product.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = item.product.category.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = BrandRedContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Sold: ${item.totalSold}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandRedOnContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Recd: ${item.fullRecd}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Reg: ${item.regSales} • New: ${item.newCustSales}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Empty: ${item.custEmpty} • Repl: ${item.compRepl}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                prefillCustomerName = ""
                                prefillQuantity = 1
                                activeTxType = TransactionType.CUSTOMER_EMPTY_PENDING_ADD
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_add_pending_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Customer Pending Empty")
                        }
                    }
                    if (pendingCustomers.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No customer pending empty cylinders recorded for ${assignedVehicle?.name ?: "your vehicle"}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(pendingCustomers, key = { it.customerName }) { pendingRow ->
                            PendingCustomerCard(
                                row = pendingRow,
                                onCollectEmpty = {
                                    prefillCustomerName = pendingRow.customerName
                                    prefillQuantity = pendingRow.pendingEmptyBalance.coerceAtLeast(1)
                                    activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (myVehicleTransactions.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No entries recorded for ${assignedVehicle?.name ?: "your vehicle"} on $selectedDate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        // Staff CANNOT delete transactions (read-only audit log)
                        items(myVehicleTransactions, key = { it.transaction.id }) { item ->
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
                                            text = "${item.productCategory.displayName} • Recorded by ${tx.staffName} • $timeStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tx.customerName.isNotBlank()) {
                                            Text(
                                                text = "Customer: ${tx.customerName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BrandRedSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                text = "Note: ${tx.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeTxType != null && assignedVehicle != null) {
        val type = activeTxType!!
        val showCust = type == TransactionType.VEHICLE_CUSTOMER_SALE ||
            type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE ||
            type == TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ||
            type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER ||
            type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD
        val requireCust = type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD

        TransactionEntryDialog(
            title = "${assignedVehicle.name} — ${type.label}",
            transactionType = type,
            products = products,
            fixedVehicle = assignedVehicle,
            requireVehicleSelection = false,
            showCustomerNameField = showCust,
            requireCustomerName = requireCust,
            recentCustomerNames = recentCustomerNames,
            initialCustomerName = prefillCustomerName,
            initialQuantity = prefillQuantity,
            showEmptyCollectedNowField = (type == TransactionType.VEHICLE_CUSTOMER_SALE),
            availableStockHint = { prodId, _ ->
                val vRow = vehicleRows.firstOrNull { it.product.id == prodId }
                val sRow = storeRows.firstOrNull { it.product.id == prodId }
                when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED ->
                        "Store Full Available: ${sRow?.storeFullBalance ?: 0}  |  Vehicle Full: ${vRow?.fullBalance ?: 0}"
                    TransactionType.VEHICLE_CUSTOMER_SALE,
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ->
                        "Vehicle Full Available: ${vRow?.fullBalance ?: 0}  |  Empty: ${vRow?.emptyBalance ?: 0}"
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE ->
                        "Vehicle Empty Available: ${vRow?.emptyBalance ?: 0}"
                    TransactionType.CUSTOMER_EMPTY_PENDING_ADD ->
                        "Records opening / missed customer pending empty cylinders"
                    else -> "Vehicle Empty Balance: ${vRow?.emptyBalance ?: 0}"
                }
            },
            liveCalculationPreview = { prodId, _, qty, empNow ->
                val vRow = vehicleRows.firstOrNull { it.product.id == prodId }
                if (vRow == null || qty <= 0) null
                else when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED ->
                        "Vehicle Full: ${vRow.fullBalance} -> ${vRow.fullBalance + qty}"
                    TransactionType.VEHICLE_CUSTOMER_SALE ->
                        "Veh Full: ${vRow.fullBalance} -> ${vRow.fullBalance - qty}  |  Veh Empty: ${vRow.emptyBalance} -> ${vRow.emptyBalance + empNow}"
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE ->
                        "New Sale: -$qty Full -> Bal ${vRow.fullBalance - qty}  |  Pending Empty: 0"
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ->
                        "Complaint Swap: Full ${vRow.fullBalance} -> ${vRow.fullBalance - qty}  |  Empty ${vRow.emptyBalance} -> ${vRow.emptyBalance + qty}"
                    TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER ->
                        "Veh Empty: ${vRow.emptyBalance} -> ${vRow.emptyBalance + qty}  |  Pending -$qty"
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE ->
                        "Veh Empty: ${vRow.emptyBalance} -> ${vRow.emptyBalance - qty}"
                    TransactionType.CUSTOMER_EMPTY_PENDING_ADD ->
                        "Customer Empty Pending: +$qty"
                    else -> null
                }
            },
            onDismiss = { activeTxType = null },
            onConfirm = { prodId, _, qty, notes, _, custName, empNow ->
                when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED ->
                        onSubmitStoreTransferRequest(StoreTransferDirection.STORE_TO_VEHICLE_FULL, prodId, qty, notes)
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE ->
                        onSubmitStoreTransferRequest(StoreTransferDirection.VEHICLE_TO_STORE_EMPTY, prodId, qty, notes)
                    else ->
                        onRecordTransaction(type, prodId, assignedVehicle.id, qty, notes, custName, empNow)
                }
            }
        )
    }

    if (showVehicleTransferDialog && assignedVehicle != null) {
        VehicleTransferDialog(
            sourceVehicle = assignedVehicle,
            allVehicles = allVehicles,
            products = products,
            sourceVehicleRows = vehicleRows,
            onDismiss = { showVehicleTransferDialog = false },
            onConfirmTransfer = { targetId, prodId, fullQty, emptyQty, extraFullQty, extraEmptyQty, notes ->
                onTransferBetweenVehicles(
                    assignedVehicle.id,
                    targetId,
                    prodId,
                    fullQty,
                    emptyQty,
                    extraFullQty,
                    extraEmptyQty,
                    notes
                )
            }
        )
    }

    if (showCalculator) {
        StockCalculatorDialog(
            products = products,
            storeFullProvider = { prodId -> storeRows.firstOrNull { it.product.id == prodId }?.storeFullBalance ?: 0 },
            storeEmptyProvider = { prodId -> storeRows.firstOrNull { it.product.id == prodId }?.storeEmptyBalance ?: 0 },
            vehicleName = assignedVehicle?.name,
            vehicleFullProvider = { prodId -> vehicleRows.firstOrNull { it.product.id == prodId }?.fullBalance ?: 0 },
            vehicleEmptyProvider = { prodId -> vehicleRows.firstOrNull { it.product.id == prodId }?.emptyBalance ?: 0 },
            onDismiss = { showCalculator = false }
        )
    }

    if (showAutoSessionDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSessionDialog = false },
            title = { Text("Staff Auto-Login & Auto-Logout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Login (One-Time PIN)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Remember ${staffUser.displayName} when reopening app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoLoginEnabled,
                            onCheckedChange = onAutoLoginChanged
                        )
                    }
                    Text("Auto-Logout Lock", style = MaterialTheme.typography.labelLarge)
                    AutoLogoutMode.entries.forEach { mode ->
                        FilterChip(
                            selected = autoLogoutMode == mode,
                            onClick = { onAutoLogoutModeChanged(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAutoSessionDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAutoSessionDialog = false
                    onLogout()
                }) { Text("Lock Now") }
            }
        )
    }
}

@Composable
private fun StaffBigActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        modifier = modifier.height(58.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            lineHeight = MaterialTheme.typography.labelMedium.lineHeight
        )
    }
}
