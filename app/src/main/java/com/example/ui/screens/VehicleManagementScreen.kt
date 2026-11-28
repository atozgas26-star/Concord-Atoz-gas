package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.BatchProductEntry
import com.example.data.model.CustomerChangeMode
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.CustomerPendingRow
import com.example.data.model.ProductCategory
import com.example.data.model.StockCalculator
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.VehicleOverview
import com.example.data.model.VehicleStockRow
import com.example.ui.components.AddStaffVehicleDialog
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFolderGrid
import com.example.ui.components.CategoryFolderStat
import com.example.ui.components.CustomerProductChangeDialog
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.Futuristic3DCylinderVisual
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.OpenedCategoryFolderHeader
import com.example.ui.components.OpeningStockEditDialog
import com.example.ui.components.PendingCustomerCard
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.StockPill
import com.example.ui.components.TransactionEntryDialog
import com.example.ui.components.VehicleTransferDialog
import com.example.ui.components.categoryVisualStyle
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehicleManagementScreen(
    selectedDate: String,
    vehicleOverviews: List<VehicleOverview>,
    isReadOnly: Boolean = false,
    onDateSelected: (String) -> Unit,
    onSelectVehicle: (Long) -> Unit,
    onAddStaffAndVehicle: (staffName: String, vehicleName: String, pin: String) -> Unit = { _, _, _ -> }
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianMidnight,
                                ObsidianDeep,
                                SignatureRoyalBackground,
                                ObsidianMidnight
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
    ) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Outside Fleet (View Only)" else "Outside Fleet • Vehicles",
            subtitle = "${vehicleOverviews.size} Fleet Accounts • Tap a vehicle for category folders" + if (isReadOnly) " • Read-Only" else "",
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.OutsideFleetGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.OutsideFleetAccent
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    midnightSurface = isDark
                )
            }

            if (!isReadOnly) {
                item {
                    Button(
                        onClick = { showAddVehicleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_vehicle_staff_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Staff Login & Vehicle (#${vehicleOverviews.size + 1})")
                    }
                }
            }

            items(vehicleOverviews, key = { it.vehicle.id }) { overview ->
                LuxuryGlassCard(
                    onClick = { onSelectVehicle(overview.vehicle.id) },
                    shape = RoundedCornerShape(18.dp),
                    accentGlow = RoyalElectricBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_card_${overview.vehicle.name}")
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
                                    tint = if (isDark) ElectricBlueGlow else BrandGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = overview.vehicle.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StockPill(label = "Full Bal", value = overview.totalFullBalance, isFull = true)
                                StockPill(label = "Empty Bal", value = overview.totalEmptyBalance, isFull = false)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f) else BrandGreenContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isDark) BorderStroke(0.8.dp, ElectricBlueBright.copy(alpha = 0.35f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FULL: Old ${overview.totalOldFull} + Recd ${overview.totalFullReceived} = Tot ${overview.totalFull} - Sales ${overview.totalCustomerSales} = Bal ${overview.totalFullBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = if (isDark) ElectricBlueGlow else BrandGreenOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f) else BrandRedContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isDark) BorderStroke(0.8.dp, MetallicOrangeBright.copy(alpha = 0.35f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "EMPTY: Old ${overview.totalOldEmpty} + Cust ${overview.totalCustomerEmptyReceived} - Ret ${overview.totalEmptyReturnedToStore} = Bal ${overview.totalEmptyBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = if (isDark) MetallicOrangeBright else BrandRedOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (!isReadOnly && showAddVehicleDialog) {
        AddStaffVehicleDialog(
            nextVehicleNumber = vehicleOverviews.size + 1,
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = onAddStaffAndVehicle
        )
    }
}

@Composable
fun VehicleDetailScreen(
    vehicle: VehicleEntity?,
    selectedDate: String,
    rows: List<VehicleStockRow>,
    storeRows: List<StoreStockRow>,
    products: List<ProductEntity>,
    transactions: List<TransactionDisplayItem> = emptyList(),
    allVehicles: List<VehicleEntity> = emptyList(),
    pendingCustomers: List<CustomerPendingRow> = emptyList(),
    recentCustomerNames: List<String> = emptyList(),
    isReadOnly: Boolean = false,
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String, String, Int) -> Unit,
    onRecordBatchTransactions: (
        type: TransactionType,
        items: List<BatchProductEntry>,
        vehicleId: Long?,
        notes: String,
        purchaseCompanyName: String,
        customerName: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onRecordCustomerProductChange: (
        mode: CustomerChangeMode,
        returnedProductId: Long,
        givenProductId: Long?,
        vehicleId: Long?,
        quantity: Int,
        customerName: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onRecordMultiCustomerProductChange: (
        mode: CustomerChangeMode,
        returnedItems: List<CustomerExchangeItem>,
        givenItems: List<CustomerExchangeItem>,
        vehicleId: Long?,
        customerName: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _ -> },
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
    onUpdateTransaction: (txId: Long, productId: Long, qty: Int, purchaseCompany: String, customerName: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteTransaction: (Long) -> Unit = {},
    onUpdateVehicleOpening: (vehicleId: Long, productId: Long, oldFull: Int, oldEmpty: Int) -> Unit,
    onOpenDifferentGasReturn: () -> Unit = {},
    onOpenReturnHistory: () -> Unit = {}
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTxType by remember { mutableStateOf<TransactionType?>(null) }
    var prefillCustomerName by remember { mutableStateOf("") }
    var prefillQuantity by remember { mutableIntStateOf(1) }
    var showVehicleTransferDialog by remember { mutableStateOf(false) }
    var showCustomerChangeDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<VehicleStockRow?>(null) }
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    val filteredRows = remember(rows, selectedCategory, searchQuery) {
        rows.filter { row ->
            (selectedCategory == null || row.product.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    row.product.name.contains(searchQuery, ignoreCase = true) ||
                    row.product.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    val dayTransactions = remember(transactions, vehicle, selectedDate) {
        if (vehicle == null) emptyList()
        else transactions.filter {
            it.transaction.vehicleId == vehicle.id &&
                it.transaction.businessDate == selectedDate
        }
    }

    val pendingActiveCount = pendingCustomers.count { it.pendingEmptyBalance > 0 }
    val vehicleFolderStats = remember(rows) {
        ProductCategory.activeEntries.map { cat ->
            val catRows = rows.filter { it.product.category == cat }
            CategoryFolderStat(
                category = cat,
                itemCount = catRows.size,
                fullTotal = catRows.sumOf { it.fullBalance },
                emptyTotal = catRows.sumOf { it.emptyBalance }
            )
        }
    }

    val isDark = LocalIsDarkTheme.current
    val chipColors = if (isDark) {
        AssistChipDefaults.assistChipColors(
            containerColor = ObsidianElevated,
            labelColor = ArcticWhite
        )
    } else {
        AssistChipDefaults.assistChipColors()
    }
    val chipBorder = if (isDark) {
        AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = ElectricBlueBright.copy(alpha = 0.4f)
        )
    } else {
        AssistChipDefaults.assistChipBorder(enabled = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianMidnight,
                                ObsidianDeep,
                                SignatureRoyalBackground,
                                ObsidianMidnight
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
    ) {
        val totalVehicleDamaged = rows.sumOf { it.damagedBalance }
        BrandHeaderBanner(
            title = "Outside Vehicle: ${vehicle?.name ?: ""}" + if (isReadOnly) " (View Only)" else "",
            subtitle = "Full Bal: ${rows.sumOf { it.fullBalance }}  •  Empty Bal: ${rows.sumOf { it.emptyBalance }}" +
                (if (totalVehicleDamaged > 0) "  •  Damaged: $totalVehicleDamaged" else ""),
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.OutsideFleetGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.OutsideFleetAccent,
            trailingContent = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("vehicle_detail_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to vehicles",
                        tint = Color.White
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateSelectorBar(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                midnightSurface = isDark
            )

            if (!isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_FULL_RECEIVED
                        },
                        label = { Text("Receive Full from Store") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, tint = if (isDark) BrandGreenLight else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_CUSTOMER_SALE
                        },
                        label = { Text("Customer Sale") },
                        leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null, tint = if (isDark) MetallicOrangeBright else BrandRedSecondary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_NEW_CUSTOMER_SALE
                        },
                        label = { Text("New Cust Sale") },
                        leadingIcon = { Icon(Icons.Default.PersonAddAlt1, contentDescription = null, tint = if (isDark) ElectricBlueGlow else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE
                        },
                        label = { Text("Complaint Replace") },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = if (isDark) MetallicOrangeBright else BrandRedSecondary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                        },
                        label = { Text("Customer Empty Recd") },
                        leadingIcon = { Icon(Icons.Default.Recycling, contentDescription = null, tint = if (isDark) ElectricBlueGlow else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.CUSTOMER_EMPTY_PENDING_ADD
                        },
                        label = { Text("+ Add Pending") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = if (isDark) MetallicOrangeBright else BrandRedSecondary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_FULL_RETURN_TO_STORE
                        },
                        label = { Text("Return Full to Store") },
                        leadingIcon = { Icon(Icons.Default.Output, contentDescription = null, tint = if (isDark) BrandGreenLight else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_return_full_chip")
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE
                        },
                        label = { Text("Return Empty to Store") },
                        leadingIcon = { Icon(Icons.Default.Output, contentDescription = null, tint = if (isDark) MetallicOrangeBright else BrandRedSecondary) },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = { showCustomerChangeDialog = true },
                        label = { Text("Customer Change") },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFC084FC)) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_customer_change_chip")
                    )
                    AssistChip(
                        onClick = onOpenDifferentGasReturn,
                        label = { Text("Diff Gas Return") },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MetallicOrangeBright) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_different_gas_return_chip")
                    )
                    AssistChip(
                        onClick = onOpenReturnHistory,
                        label = { Text("Return History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = if (isDark) ElectricBlueGlow else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_return_history_chip")
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_DAMAGED_ADD
                        },
                        label = { Text("Damaged (+)") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = BrandRedBright) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_damaged_add_chip")
                    )
                    AssistChip(
                        onClick = {
                            prefillCustomerName = ""
                            prefillQuantity = 1
                            activeTxType = TransactionType.VEHICLE_DAMAGED_REMOVE
                        },
                        label = { Text("Resolve Dmg (-)") },
                        leadingIcon = { Icon(Icons.Default.Recycling, contentDescription = null, tint = if (isDark) BrandGreenLight else BrandGreenPrimary) },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("vehicle_detail_damaged_remove_chip")
                    )
                    if (vehicle != null && allVehicles.any { it.id != vehicle.id }) {
                        AssistChip(
                            onClick = { showVehicleTransferDialog = true },
                            label = { Text("Vehicle → Vehicle") },
                            leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null, tint = if (isDark) ElectricBlueGlow else BrandGreenPrimary) },
                            colors = chipColors,
                            border = chipBorder,
                            modifier = Modifier.testTag("vehicle_detail_transfer_chip")
                        )
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = activeTab,
            containerColor = if (isDark) ObsidianDeep else MaterialTheme.colorScheme.surface,
            contentColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (activeTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        height = 2.5.dp,
                        color = if (isDark) MetallicOrangeBright else MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                selectedContentColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.primary,
                unselectedContentColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text("Stock") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                selectedContentColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.primary,
                unselectedContentColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text("Pending ($pendingActiveCount)") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                selectedContentColor = if (isDark) ArcticWhite else MaterialTheme.colorScheme.primary,
                unselectedContentColor = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text("Activity (${dayTransactions.size})") }
            )
        }

        when (activeTab) {
            0 -> {
                val showProducts = selectedCategory != null || searchQuery.isNotBlank()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "vehicle_category_folders") {
                        CategoryFolderGrid(
                            stats = vehicleFolderStats,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { selectedCategory = it },
                            sectionTitle = "Outside Vehicle Category Folders",
                            onMidnightBackground = isDark
                        )
                    }
                    item(key = "vehicle_search_field") {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search cylinder in ${vehicle?.name ?: "vehicle"}...",
                                    color = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isDark) ElectricBlueGlow else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = if (isDark) {
                                OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ObsidianSurface,
                                    unfocusedContainerColor = ObsidianSurface,
                                    focusedBorderColor = ElectricBlueBright,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = ArcticWhite,
                                    unfocusedTextColor = ArcticWhite
                                )
                            } else {
                                OutlinedTextFieldDefaults.colors()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (showProducts) {
                        selectedCategory?.let { openCat ->
                            item(key = "vehicle_opened_folder_header_${openCat.name}") {
                                OpenedCategoryFolderHeader(
                                    category = openCat,
                                    itemCount = filteredRows.size,
                                    subtitle = "${vehicle?.name ?: "Vehicle"} • ${filteredRows.size} products inside folder",
                                    onClose = { selectedCategory = null }
                                )
                            }
                        }
                        if (filteredRows.isEmpty()) {
                            item(key = "vehicle_empty_search_notice") {
                                Text(
                                    text = "No matching products found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(filteredRows, key = { it.product.id }) { row ->
                                VehicleProductRowCard(
                                    row = row,
                                    canEditOpening = !isReadOnly,
                                    onEditOpening = if (isReadOnly) null else { { editingRow = row } }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isReadOnly) {
                        item {
                            Button(
                                onClick = {
                                    prefillCustomerName = ""
                                    prefillQuantity = 1
                                    activeTxType = TransactionType.CUSTOMER_EMPTY_PENDING_ADD
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Customer Pending Empty")
                            }
                        }
                    }
                    if (pendingCustomers.isEmpty()) {
                        item {
                            Text(
                                text = "No customer pending empty balances recorded for ${vehicle?.name ?: "this vehicle"} yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(pendingCustomers, key = { it.customerName }) { pendingRow ->
                            PendingCustomerCard(
                                row = pendingRow,
                                showCollectAction = !isReadOnly,
                                onCollectEmpty = {
                                    if (!isReadOnly) {
                                        prefillCustomerName = pendingRow.customerName
                                        prefillQuantity = pendingRow.pendingEmptyBalance.coerceAtLeast(1)
                                        activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                                    }
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
                    if (dayTransactions.isEmpty()) {
                        item {
                            Text(
                                text = "No transactions for ${vehicle?.name ?: "this vehicle"} on $selectedDate.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(dayTransactions, key = { it.transaction.id }) { item ->
                            val tx = item.transaction
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.timestamp))
                            LuxuryGlassCard(
                                shape = RoundedCornerShape(18.dp),
                                accentGlow = ElectricBlueBright
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tx.type.label} • ${item.productName}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${item.productCategory.displayName} • Qty: ${tx.quantity} • By ${tx.staffName} • $timeStr",
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
                                    if (!isReadOnly) {
                                        IconButton(onClick = { editingTransaction = tx }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit transaction",
                                                tint = BrandGreenPrimary
                                            )
                                        }
                                        IconButton(onClick = { deleteCandidateId = tx.id }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete transaction",
                                                tint = BrandRedBright
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
    }

    if (!isReadOnly && activeTxType != null && vehicle != null) {
        val type = activeTxType!!
        val showCust = type == TransactionType.VEHICLE_CUSTOMER_SALE ||
            type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE ||
            type == TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ||
            type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER ||
            type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD
        val requireCust = type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD

        TransactionEntryDialog(
            title = "${vehicle.name} — ${type.label}",
            transactionType = type,
            products = products,
            fixedVehicle = vehicle,
            requireVehicleSelection = false,
            showCustomerNameField = showCust,
            requireCustomerName = requireCust,
            recentCustomerNames = recentCustomerNames,
            initialCustomerName = prefillCustomerName,
            initialQuantity = prefillQuantity,
            showEmptyCollectedNowField = (type == TransactionType.VEHICLE_CUSTOMER_SALE),
            availableStockHint = { prodId, _ ->
                val vRow = rows.firstOrNull { it.product.id == prodId }
                val sRow = storeRows.firstOrNull { it.product.id == prodId }
                when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED ->
                        "Store Full Available: ${sRow?.storeFullBalance ?: 0}  |  Vehicle Full: ${vRow?.fullBalance ?: 0}"
                    TransactionType.VEHICLE_FULL_RETURN_TO_STORE ->
                        "Vehicle Full Available: ${vRow?.fullBalance ?: 0}  |  Store Full: ${sRow?.storeFullBalance ?: 0}"
                    TransactionType.VEHICLE_CUSTOMER_SALE,
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ->
                        "Vehicle Full Available: ${vRow?.fullBalance ?: 0}  |  Empty: ${vRow?.emptyBalance ?: 0}"
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE ->
                        "Vehicle Empty Available: ${vRow?.emptyBalance ?: 0}  |  Store Empty: ${sRow?.storeEmptyBalance ?: 0}"
                    TransactionType.CUSTOMER_EMPTY_PENDING_ADD ->
                        "Logs pending empty cylinder balance for customer (no physical vehicle stock change)"
                    else -> "Vehicle Empty Balance: ${vRow?.emptyBalance ?: 0}"
                }
            },
            liveCalculationPreview = { prodId, _, qty, empNow ->
                val vRow = rows.firstOrNull { it.product.id == prodId }
                val sRow = storeRows.firstOrNull { it.product.id == prodId }
                if (vRow == null || qty <= 0) null
                else when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED -> {
                        val sBal = (sRow?.storeFullBalance ?: 0) - qty
                        val vBal = vRow.fullBalance + qty
                        "Store Full: ${sRow?.storeFullBalance ?: 0} -> $sBal  |  Veh Full: ${vRow.fullBalance} -> $vBal"
                    }
                    TransactionType.VEHICLE_FULL_RETURN_TO_STORE -> {
                        val vBal = vRow.fullBalance - qty
                        val sBal = (sRow?.storeFullBalance ?: 0) + qty
                        "Veh Full: ${vRow.fullBalance} -> $vBal  |  Store Full: ${sRow?.storeFullBalance ?: 0} -> $sBal"
                    }
                    TransactionType.VEHICLE_CUSTOMER_SALE -> {
                        val vFull = vRow.fullBalance - qty
                        val vEmp = vRow.emptyBalance + empNow
                        "Veh Full: ${vRow.fullBalance} -> $vFull  |  Veh Empty: ${vRow.emptyBalance} -> $vEmp"
                    }
                    TransactionType.VEHICLE_NEW_CUSTOMER_SALE -> {
                        val vFull = vRow.fullBalance - qty
                        "New Sale: -$qty Full -> Bal $vFull  |  Pending Empty: 0"
                    }
                    TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE -> {
                        val vFull = vRow.fullBalance - qty
                        val vEmp = vRow.emptyBalance + qty
                        "Complaint Swap: Full ${vRow.fullBalance} -> $vFull  |  Empty ${vRow.emptyBalance} -> $vEmp"
                    }
                    TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER -> {
                        val vEmp = vRow.emptyBalance + qty
                        "Veh Empty: ${vRow.emptyBalance} -> $vEmp  |  Cust Pending: -$qty"
                    }
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE -> {
                        val vEmp = vRow.emptyBalance - qty
                        val sEmp = (sRow?.storeEmptyBalance ?: 0) + qty
                        "Veh Empty: ${vRow.emptyBalance} -> $vEmp  |  Store Empty: ${sRow?.storeEmptyBalance ?: 0} -> $sEmp"
                    }
                    TransactionType.CUSTOMER_EMPTY_PENDING_ADD ->
                        "Customer Empty Pending Balance: +$qty  |  Vehicle Stock Unchanged"
                    else -> null
                }
            },
            onDismiss = { activeTxType = null },
            onConfirmBatch = { _, items, notes, purchaseCompany, custName ->
                onRecordBatchTransactions(type, items, vehicle.id, notes, purchaseCompany, custName)
            },
            onConfirm = { prodId, _, qty, notes, _, custName, empNow ->
                onRecordTransaction(type, prodId, vehicle.id, qty, notes, custName, empNow)
            }
        )
    }

    if (!isReadOnly && showCustomerChangeDialog && vehicle != null) {
        CustomerProductChangeDialog(
            vehicle = vehicle,
            products = products,
            vehicleRows = rows,
            recentCustomerNames = recentCustomerNames,
            onDismiss = { showCustomerChangeDialog = false },
            onConfirmMulti = { mode, retItems, givItems, custName, notes ->
                onRecordMultiCustomerProductChange(mode, retItems, givItems, vehicle.id, custName, notes)
            },
            onConfirm = { mode, retProdId, givProdId, qty, custName, notes ->
                onRecordCustomerProductChange(mode, retProdId, givProdId, vehicle.id, qty, custName, notes)
            }
        )
    }

    if (!isReadOnly && showVehicleTransferDialog && vehicle != null) {
        VehicleTransferDialog(
            sourceVehicle = vehicle,
            allVehicles = allVehicles,
            products = products,
            sourceVehicleRows = rows,
            onDismiss = { showVehicleTransferDialog = false },
            onConfirmTransfer = { targetId, prodId, fullQty, emptyQty, extraFullQty, extraEmptyQty, notes ->
                onTransferBetweenVehicles(vehicle.id, targetId, prodId, fullQty, emptyQty, extraFullQty, extraEmptyQty, notes)
            }
        )
    }

    if (!isReadOnly) editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            products = products,
            onDismiss = { editingTransaction = null },
            onConfirm = { prodId, qty, company, custName, notes ->
                onUpdateTransaction(tx.id, prodId, qty, company, custName, notes)
            }
        )
    }

    if (!isReadOnly && editingRow != null && vehicle != null) {
        val r = editingRow!!
        OpeningStockEditDialog(
            title = "Set ${vehicle.name} Old Stock ($selectedDate)",
            productName = "${r.product.category.displayName} • ${r.product.name}",
            initialFull = r.oldFull,
            initialEmpty = r.oldEmpty,
            onDismiss = { editingRow = null },
            onConfirm = { full, empty ->
                onUpdateVehicleOpening(vehicle.id, r.product.id, full, empty)
            }
        )
    }

    if (!isReadOnly) deleteCandidateId?.let { txId ->
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("Delete Transaction?") },
            text = { Text("This will remove the transaction and recalculate stock balances.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(txId)
                        deleteCandidateId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VehicleProductRowCard(
    row: VehicleStockRow,
    canEditOpening: Boolean,
    onEditOpening: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val catStyle = categoryVisualStyle(row.product.category)
    LuxuryGlassCard(
        shape = RoundedCornerShape(18.dp),
        accentGlow = catStyle.primaryColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Futuristic3DCylinderVisual(
                        fullCount = row.fullBalance,
                        emptyCount = row.emptyBalance,
                        damagedCount = row.damagedBalance,
                        accentColor = catStyle.primaryColor,
                        modifier = Modifier.size(width = 36.dp, height = 52.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.product.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) ElectricBlueGlow else BrandGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = row.product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockPill(label = "Full", value = row.fullBalance, isFull = true)
                    StockPill(label = "Empty", value = row.emptyBalance, isFull = false)
                    if (canEditOpening && onEditOpening != null) {
                        IconButton(onClick = onEditOpening) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit old stock",
                                tint = if (isDark) MetallicOrangeBright else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f) else BrandGreenContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isDark) BorderStroke(0.8.dp, ElectricBlueBright.copy(alpha = 0.35f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "FULL CYLINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ElectricBlueGlow else BrandGreenOnContainer
                        )
                        val replFull = if (row.complaintReplaced > 0) " • Repl: -${row.complaintReplaced}" else ""
                        val retStoreFull = if (row.fullReturnedToStore > 0) " • Ret: -${row.fullReturnedToStore}" else ""
                        val chgFull = if (row.fullChangedOut > 0) " • Chg: -${row.fullChangedOut}" else ""
                        val xferFull = if (row.fullTransferredOut > 0) " • Xfer: -${row.fullTransferredOut}" else ""
                        Text(
                            text = "Old: ${row.oldFull}  •  Recd: ${row.fullReceived}\nTot: ${row.totalFull}  •  Sale: ${row.customerSales}$replFull$retStoreFull$chgFull$xferFull\nBal: ${row.fullBalance}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMono,
                            color = if (isDark) ArcticWhite else BrandGreenOnContainer
                        )
                    }
                }
                Surface(
                    color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f) else BrandRedContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isDark) BorderStroke(0.8.dp, MetallicOrangeBright.copy(alpha = 0.35f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "EMPTY CYLINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MetallicOrangeBright else BrandRedOnContainer
                        )
                        val replEmp = if (row.complaintReplaced > 0) " • Repl: +${row.complaintReplaced}" else ""
                        val chgEmp = if (row.emptyChangedOut > 0) " • Chg: -${row.emptyChangedOut}" else ""
                        val xferEmp = if (row.emptyTransferredOut > 0) " • Xfer: -${row.emptyTransferredOut}" else ""
                        Text(
                            text = "Old: ${row.oldEmpty}  •  Cust: ${row.customerEmptyReceived}$replEmp\nRet Store: ${row.emptyReturnedToStore}$chgEmp$xferEmp\nBal: ${row.emptyBalance}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMono,
                            color = if (isDark) ArcticWhite else BrandRedOnContainer
                        )
                    }
                }
            }
            if (row.damagedBalance > 0 || row.fullBalance <= 3) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (row.damagedBalance > 0) {
                        Surface(
                            color = Color(0xFF45141E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, BrandRedBright.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "DAMAGED: ${row.damagedBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (row.fullBalance <= 3) {
                        Surface(
                            color = if (isDark) Color(0xFF3E2208) else BrandRedContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, MetallicOrangeBright.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (row.fullBalance <= 0) "LOW: 0 FULL" else "LOW STOCK (${row.fullBalance})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MetallicOrangeBright else BrandRedOnContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
