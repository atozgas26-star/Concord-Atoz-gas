package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.BatchProductEntry
import com.example.data.model.CustomerChangeMode
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.ProductCategory
import com.example.data.model.StockCalculator
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFolderGrid
import com.example.ui.components.CategoryFolderStat
import com.example.ui.components.CustomerProductChangeDialog
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.OpenedCategoryFolderHeader
import com.example.ui.components.OpeningStockEditDialog
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.StockPill
import com.example.ui.components.TransactionEntryDialog
import com.example.ui.components.categoryVisualStyle
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StoreModalAction {
    PURCHASE_FULL,
    PURCHASE_EMPTY,
    PURCHASE_COMPLAINT_REPLACE,
    FULL_INWARD,
    EMPTY_INWARD,
    FULL_DISPATCH_VEHICLE,
    FULL_RETURN_VEHICLE,
    EMPTY_RECEIVED_VEHICLE,
    EMPTY_DISPATCH_PLANT,
    ADJUST_FULL_IN,
    ADJUST_FULL_OUT,
    ADJUST_EMPTY_IN,
    ADJUST_EMPTY_OUT
}

@Composable
fun StoreManagementScreen(
    selectedDate: String,
    storeRows: List<StoreStockRow>,
    products: List<ProductEntity>,
    allProducts: List<ProductEntity> = products,
    vehicles: List<VehicleEntity>,
    transactions: List<TransactionDisplayItem>,
    recentPurchaseCompanies: List<String> = emptyList(),
    recentCustomerNames: List<String> = emptyList(),
    isReadOnly: Boolean = false,
    onDateSelected: (String) -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String, String, String) -> Unit,
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
    onUpdateOpeningStock: (productId: Long, full: Int, empty: Int) -> Unit,
    onUpdateTransaction: (txId: Long, productId: Long, qty: Int, purchaseCompany: String, customerName: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteTransaction: (Long) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeModal by remember { mutableStateOf<StoreModalAction?>(null) }
    var showCustomerChangeDialog by remember { mutableStateOf(false) }
    var showAdjustmentPicker by remember { mutableStateOf(false) }
    var editingOpeningRow by remember { mutableStateOf<StoreStockRow?>(null) }
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    val filteredRows = remember(storeRows, selectedCategory, searchQuery) {
        storeRows.filter { row ->
            (selectedCategory == null || row.product.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    row.product.name.contains(searchQuery, ignoreCase = true) ||
                    row.product.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    val dayTransactions = remember(transactions, selectedDate) {
        transactions.filter { it.transaction.businessDate == selectedDate }
    }

    val totalFull = storeRows.sumOf { it.storeFullBalance }
    val totalEmpty = storeRows.sumOf { it.storeEmptyBalance }

    val folderStats = remember(storeRows) {
        ProductCategory.entries.map { cat ->
            val catRows = storeRows.filter { it.product.category == cat }
            CategoryFolderStat(
                category = cat,
                itemCount = catRows.size,
                fullTotal = catRows.sumOf { it.storeFullBalance },
                emptyTotal = catRows.sumOf { it.storeEmptyBalance }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Inside Store (View Only)" else "Inside Store • Main Yard",
            subtitle = "Yard Full: $totalFull  •  Yard Empty: $totalEmpty" + if (isReadOnly) " • Read-Only" else "",
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.InsideStoreGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.InsideStoreAccent
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)

            if (!isReadOnly) {
                // Action Buttons Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoreActionChip("Purchase Full", Icons.Default.AddShoppingCart, BrandGreenPrimary) {
                        activeModal = StoreModalAction.PURCHASE_FULL
                    }
                    StoreActionChip("Purchase Empty", Icons.Default.AddBusiness, BrandGreenPrimary) {
                        activeModal = StoreModalAction.PURCHASE_EMPTY
                    }
                    StoreActionChip("Complaint Replace", Icons.Default.SwapHoriz, BrandRedSecondary) {
                        activeModal = StoreModalAction.PURCHASE_COMPLAINT_REPLACE
                    }
                    StoreActionChip("Dispatch to Vehicle", Icons.Default.LocalShipping, BrandRedSecondary) {
                        activeModal = StoreModalAction.FULL_DISPATCH_VEHICLE
                    }
                    StoreActionChip("Full Return (Veh)", Icons.AutoMirrored.Filled.Input, BrandGreenPrimary) {
                        activeModal = StoreModalAction.FULL_RETURN_VEHICLE
                    }
                    StoreActionChip("Empty Recd (Veh)", Icons.AutoMirrored.Filled.Input, BrandGreenPrimary) {
                        activeModal = StoreModalAction.EMPTY_RECEIVED_VEHICLE
                    }
                    StoreActionChip("Customer Change", Icons.Default.SwapHoriz, Color(0xFF7E22CE)) {
                        showCustomerChangeDialog = true
                    }
                    StoreActionChip("Full Inward", Icons.AutoMirrored.Filled.Input, BrandGreenPrimary) {
                        activeModal = StoreModalAction.FULL_INWARD
                    }
                    StoreActionChip("Empty Inward", Icons.AutoMirrored.Filled.Input, BrandGreenPrimary) {
                        activeModal = StoreModalAction.EMPTY_INWARD
                    }
                    StoreActionChip("Empty Dispatch", Icons.Default.Outbox, BrandRedSecondary) {
                        activeModal = StoreModalAction.EMPTY_DISPATCH_PLANT
                    }
                    StoreActionChip("Stock Adjustment", Icons.Default.Tune, MaterialTheme.colorScheme.tertiary) {
                        showAdjustmentPicker = true
                    }
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Folder Balance (${storeRows.size})") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Day Ledger (${dayTransactions.size})") }
            )
        }

        if (activeTab == 0) {
            val showProducts = selectedCategory != null || searchQuery.isNotBlank()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "store_category_folders") {
                    CategoryFolderGrid(
                        stats = folderStats,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        sectionTitle = "Inside Store Category Folders"
                    )
                }
                item(key = "store_search_field") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search cylinder or category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showProducts) {
                    selectedCategory?.let { openCat ->
                        item(key = "store_opened_folder_header_${openCat.name}") {
                            OpenedCategoryFolderHeader(
                                category = openCat,
                                itemCount = filteredRows.size,
                                subtitle = "Inside Store • ${filteredRows.size} products inside folder",
                                onClose = { selectedCategory = null }
                            )
                        }
                    }
                    if (filteredRows.isEmpty()) {
                        item(key = "store_empty_search_notice") {
                            Text(
                                text = "No matching products found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(filteredRows, key = { it.product.id }) { row ->
                            StoreProductCard(
                                row = row,
                                canEditOpening = !isReadOnly,
                                onEditOpening = { if (!isReadOnly) editingOpeningRow = row }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dayTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "No store or vehicle transactions recorded on $selectedDate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(dayTransactions, key = { it.transaction.id }) { item ->
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.type.label} — ${item.productName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.productCategory.displayName} • Qty: ${tx.quantity} • By ${tx.staffName}" +
                                            (tx.vehicleName?.let { " • $it" } ?: "") +
                                            " • $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.purchaseCompanyName.isNotBlank()) {
                                        Text(
                                            text = "Company: ${tx.purchaseCompanyName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrandGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                                    IconButton(
                                        onClick = { editingTransaction = tx },
                                        modifier = Modifier.testTag("edit_tx_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit transaction",
                                            tint = BrandGreenPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = { deleteCandidateId = tx.id },
                                        modifier = Modifier.testTag("delete_tx_${tx.id}")
                                    ) {
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

    // Adjustment type picker
    if (!isReadOnly && showAdjustmentPicker) {
        AlertDialog(
            onDismissRequest = { showAdjustmentPicker = false },
            title = { Text("Select Stock Adjustment Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showAdjustmentPicker = false
                            activeModal = StoreModalAction.ADJUST_FULL_IN
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Full Cylinders (+)") }
                    OutlinedButton(
                        onClick = {
                            showAdjustmentPicker = false
                            activeModal = StoreModalAction.ADJUST_FULL_OUT
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Deduct Full Cylinders (-)") }
                    OutlinedButton(
                        onClick = {
                            showAdjustmentPicker = false
                            activeModal = StoreModalAction.ADJUST_EMPTY_IN
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Empty Cylinders (+)") }
                    OutlinedButton(
                        onClick = {
                            showAdjustmentPicker = false
                            activeModal = StoreModalAction.ADJUST_EMPTY_OUT
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Deduct Empty Cylinders (-)") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAdjustmentPicker = false }) { Text("Close") }
            }
        )
    }

    // Transaction Modal
    if (!isReadOnly) activeModal?.let { action ->
        val (title, txType, needsVehicle) = when (action) {
            StoreModalAction.PURCHASE_FULL -> Triple("Full Cylinder Purchase", TransactionType.STORE_PURCHASE_FULL, false)
            StoreModalAction.PURCHASE_EMPTY -> Triple("Empty Cylinder Purchase", TransactionType.STORE_PURCHASE_EMPTY, false)
            StoreModalAction.PURCHASE_COMPLAINT_REPLACE -> Triple("Purchase Co. Complaint Replace", TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE, false)
            StoreModalAction.FULL_INWARD -> Triple("Full Cylinder Inward", TransactionType.STORE_FULL_INWARD, false)
            StoreModalAction.EMPTY_INWARD -> Triple("Empty Cylinder Inward", TransactionType.STORE_EMPTY_INWARD, false)
            StoreModalAction.FULL_DISPATCH_VEHICLE -> Triple("Dispatch Full to Vehicle", TransactionType.VEHICLE_FULL_RECEIVED, true)
            StoreModalAction.FULL_RETURN_VEHICLE -> Triple("Full Return from Vehicle to Store", TransactionType.VEHICLE_FULL_RETURN_TO_STORE, true)
            StoreModalAction.EMPTY_RECEIVED_VEHICLE -> Triple("Receive Empty from Vehicle", TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE, true)
            StoreModalAction.EMPTY_DISPATCH_PLANT -> Triple("Empty Dispatch (Refill/Plant)", TransactionType.STORE_EMPTY_DISPATCH, false)
            StoreModalAction.ADJUST_FULL_IN -> Triple("Store Full Adjustment (+)", TransactionType.STORE_ADJUST_FULL_IN, false)
            StoreModalAction.ADJUST_FULL_OUT -> Triple("Store Full Adjustment (-)", TransactionType.STORE_ADJUST_FULL_OUT, false)
            StoreModalAction.ADJUST_EMPTY_IN -> Triple("Store Empty Adjustment (+)", TransactionType.STORE_ADJUST_EMPTY_IN, false)
            StoreModalAction.ADJUST_EMPTY_OUT -> Triple("Store Empty Adjustment (-)", TransactionType.STORE_ADJUST_EMPTY_OUT, false)
        }

        val isPurchaseAction = txType == TransactionType.STORE_PURCHASE_FULL ||
            txType == TransactionType.STORE_PURCHASE_EMPTY ||
            txType == TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE
        val showCustField = false

        TransactionEntryDialog(
            title = title,
            transactionType = txType,
            products = products,
            vehicles = vehicles,
            requireVehicleSelection = needsVehicle,
            showPurchaseCompanyField = isPurchaseAction,
            recentPurchaseCompanies = recentPurchaseCompanies,
            showCustomerNameField = showCustField,
            recentCustomerNames = recentCustomerNames,
            availableStockHint = { productId, _ ->
                val row = storeRows.firstOrNull { it.product.id == productId }
                row?.let { "Store Available -> Full: ${it.storeFullBalance}  |  Empty: ${it.storeEmptyBalance}" }
            },
            liveCalculationPreview = { productId, _, qty, _ ->
                val row = storeRows.firstOrNull { it.product.id == productId }
                if (row == null || qty <= 0) null
                else {
                    val previewTx = StockTransactionEntity(
                        businessDate = selectedDate,
                        timestamp = 0L,
                        type = txType,
                        productId = productId,
                        vehicleId = null,
                        vehicleName = null,
                        staffId = 0L,
                        staffName = "",
                        quantity = qty,
                        notes = ""
                    )
                    val fDelta = StockCalculator.storeFullDelta(previewTx)
                    val eDelta = StockCalculator.storeEmptyDelta(previewTx)
                    val newFull = row.storeFullBalance + fDelta
                    val newEmpty = row.storeEmptyBalance + eDelta
                    buildString {
                        append("Store Calculate: ")
                        if (fDelta != 0) {
                            append("Full ${row.storeFullBalance} -> $newFull")
                        }
                        if (fDelta != 0 && eDelta != 0) append("  |  ")
                        if (eDelta != 0) {
                            append("Empty ${row.storeEmptyBalance} -> $newEmpty")
                        }
                        if (newFull < 0 || newEmpty < 0) {
                            append(" (Insufficient!)")
                        }
                    }
                }
            },
            onDismiss = { activeModal = null },
            onConfirmBatch = { vehicleId, items, notes, purchaseCompany, custName ->
                onRecordBatchTransactions(txType, items, vehicleId, notes, purchaseCompany, custName)
            },
            onConfirm = { productId, vehicleId, qty, notes, purchaseCompany, custName, _ ->
                onRecordTransaction(txType, productId, vehicleId, qty, notes, purchaseCompany, custName)
            }
        )
    }

    if (!isReadOnly && showCustomerChangeDialog) {
        CustomerProductChangeDialog(
            vehicle = null,
            products = products,
            storeRows = storeRows,
            recentCustomerNames = recentCustomerNames,
            onDismiss = { showCustomerChangeDialog = false },
            onConfirmMulti = { mode, retItems, givItems, custName, notes ->
                onRecordMultiCustomerProductChange(mode, retItems, givItems, null, custName, notes)
            },
            onConfirm = { mode, retProdId, givProdId, qty, custName, notes ->
                onRecordCustomerProductChange(mode, retProdId, givProdId, null, qty, custName, notes)
            }
        )
    }

    // Edit Recorded Transaction Dialog
    if (!isReadOnly) editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            products = allProducts,
            onDismiss = { editingTransaction = null },
            onConfirm = { prodId, qty, company, custName, notes ->
                onUpdateTransaction(tx.id, prodId, qty, company, custName, notes)
            }
        )
    }

    // Opening Stock Edit Dialog
    if (!isReadOnly) editingOpeningRow?.let { row ->
        OpeningStockEditDialog(
            title = "Set Store Opening Stock ($selectedDate)",
            productName = "${row.product.category.displayName} • ${row.product.name}",
            initialFull = row.openingFull,
            initialEmpty = row.openingEmpty,
            onDismiss = { editingOpeningRow = null },
            onConfirm = { full, empty ->
                onUpdateOpeningStock(row.product.id, full, empty)
            }
        )
    }

    // Delete Confirmation
    if (!isReadOnly) deleteCandidateId?.let { txId ->
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("Delete Transaction?") },
            text = { Text("This will remove the transaction and recalculate stock balances (negative balances remain blocked).") },
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
private fun StoreActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        },
        modifier = Modifier.testTag("store_action_${label.lowercase().replace(' ', '_')}")
    )
}

@Composable
private fun StoreProductCard(
    row: StoreStockRow,
    canEditOpening: Boolean = true,
    onEditOpening: () -> Unit
) {
    val catStyle = categoryVisualStyle(row.product.category)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, catStyle.primaryColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.product.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = catStyle.primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = row.product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockPill(label = "Full", value = row.storeFullBalance, isFull = true)
                    StockPill(label = "Empty", value = row.storeEmptyBalance, isFull = false)
                    if (canEditOpening) {
                        IconButton(
                            onClick = onEditOpening,
                            modifier = Modifier.testTag("edit_store_opening_${row.product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit opening stock",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Full Breakdown
            Surface(
                color = BrandGreenContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FULL: Op ${row.openingFull} + Pur ${row.fullPurchases} + In ${row.fullReturns} - Disp ${row.fullDispatch} = ${row.storeFullBalance}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = BrandGreenOnContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            // Empty Breakdown
            Surface(
                color = BrandRedContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EMPTY: Op ${row.openingEmpty} + Pur ${row.emptyPurchases} + Ret ${row.emptyReturnsToStore} - Disp ${row.emptyDispatch} = ${row.storeEmptyBalance}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = BrandRedOnContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
