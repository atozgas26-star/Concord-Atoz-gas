package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.VehicleEntity
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
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.OpeningStockEditDialog
import com.example.ui.components.PendingCustomerCard
import com.example.ui.components.StockPill
import com.example.ui.components.TransactionEntryDialog
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

@Composable
fun VehicleManagementScreen(
    selectedDate: String,
    vehicleOverviews: List<VehicleOverview>,
    onDateSelected: (String) -> Unit,
    onSelectVehicle: (Long) -> Unit,
    onAddStaffAndVehicle: (staffName: String, vehicleName: String, pin: String) -> Unit = { _, _, _ -> }
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "Vehicle Management",
            subtitle = "${vehicleOverviews.size} Fleet Accounts • Tap a vehicle for full ledger"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
            }

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

            items(vehicleOverviews, key = { it.vehicle.id }) { overview ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectVehicle(overview.vehicle.id) }
                        .testTag("vehicle_card_${overview.vehicle.name}"),
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
                            color = BrandGreenContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FULL: Old ${overview.totalOldFull} + Recd ${overview.totalFullReceived} = Tot ${overview.totalFull} - Sales ${overview.totalCustomerSales} = Bal ${overview.totalFullBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = BrandGreenOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = BrandRedContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "EMPTY: Old ${overview.totalOldEmpty} + Cust ${overview.totalCustomerEmptyReceived} - Ret ${overview.totalEmptyReturnedToStore} = Bal ${overview.totalEmptyBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = BrandRedOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddVehicleDialog) {
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
    pendingCustomers: List<CustomerPendingRow> = emptyList(),
    recentCustomerNames: List<String> = emptyList(),
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String, String, Int) -> Unit,
    onUpdateTransaction: (txId: Long, productId: Long, qty: Int, purchaseCompany: String, customerName: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteTransaction: (Long) -> Unit = {},
    onUpdateVehicleOpening: (vehicleId: Long, productId: Long, oldFull: Int, oldEmpty: Int) -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var activeTxType by remember { mutableStateOf<TransactionType?>(null) }
    var prefillCustomerName by remember { mutableStateOf("") }
    var prefillQuantity by remember { mutableIntStateOf(1) }
    var editingRow by remember { mutableStateOf<VehicleStockRow?>(null) }
    var editingTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    val filteredRows = remember(rows, selectedCategory) {
        rows.filter { selectedCategory == null || it.product.category == selectedCategory }
    }

    val dayTransactions = remember(transactions, vehicle, selectedDate) {
        if (vehicle == null) emptyList()
        else transactions.filter {
            it.transaction.vehicleId == vehicle.id &&
                it.transaction.businessDate == selectedDate
        }
    }

    val pendingActiveCount = pendingCustomers.count { it.pendingEmptyBalance > 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "Vehicle: ${vehicle?.name ?: ""}",
            subtitle = "Full Bal: ${rows.sumOf { it.fullBalance }}  •  Empty Bal: ${rows.sumOf { it.emptyBalance }}",
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
            DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)

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
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, tint = BrandGreenPrimary) }
                )
                AssistChip(
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_CUSTOMER_SALE
                    },
                    label = { Text("Customer Sale") },
                    leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null, tint = BrandRedSecondary) }
                )
                AssistChip(
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER
                    },
                    label = { Text("Customer Empty Recd") },
                    leadingIcon = { Icon(Icons.Default.Recycling, contentDescription = null, tint = BrandGreenPrimary) }
                )
                AssistChip(
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.CUSTOMER_EMPTY_PENDING_ADD
                    },
                    label = { Text("+ Add Pending") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = BrandRedSecondary) }
                )
                AssistChip(
                    onClick = {
                        prefillCustomerName = ""
                        prefillQuantity = 1
                        activeTxType = TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE
                    },
                    label = { Text("Return Empty to Store") },
                    leadingIcon = { Icon(Icons.Default.Output, contentDescription = null, tint = BrandRedSecondary) }
                )
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Stock") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Pending ($pendingActiveCount)") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Activity (${dayTransactions.size})") }
            )
        }

        when (activeTab) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRows, key = { it.product.id }) { row ->
                            VehicleProductRowCard(
                                row = row,
                                canEditOpening = true,
                                onEditOpening = { editingRow = row }
                            )
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

    if (activeTxType != null && vehicle != null) {
        val type = activeTxType!!
        val showCust = type == TransactionType.VEHICLE_CUSTOMER_SALE ||
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
                    TransactionType.VEHICLE_CUSTOMER_SALE ->
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
                    TransactionType.VEHICLE_CUSTOMER_SALE -> {
                        val vFull = vRow.fullBalance - qty
                        val vEmp = vRow.emptyBalance + empNow
                        "Veh Full: ${vRow.fullBalance} -> $vFull  |  Veh Empty: ${vRow.emptyBalance} -> $vEmp"
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
            onConfirm = { prodId, _, qty, notes, _, custName, empNow ->
                onRecordTransaction(type, prodId, vehicle.id, qty, notes, custName, empNow)
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

    if (editingRow != null && vehicle != null) {
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

    deleteCandidateId?.let { txId ->
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        color = BrandGreenPrimary,
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
                    StockPill(label = "Full", value = row.fullBalance, isFull = true)
                    StockPill(label = "Empty", value = row.emptyBalance, isFull = false)
                    if (canEditOpening && onEditOpening != null) {
                        IconButton(onClick = onEditOpening) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit old stock",
                                tint = MaterialTheme.colorScheme.primary,
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
                    color = BrandGreenContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "FULL CYLINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenOnContainer
                        )
                        Text(
                            text = "Old: ${row.oldFull}  •  Recd: ${row.fullReceived}\nTot: ${row.totalFull}  •  Sale: ${row.customerSales}\nBal: ${row.fullBalance}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMono,
                            color = BrandGreenOnContainer
                        )
                    }
                }
                Surface(
                    color = BrandRedContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "EMPTY CYLINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandRedOnContainer
                        )
                        Text(
                            text = "Old: ${row.oldEmpty}  •  Cust: ${row.customerEmptyReceived}\nRet Store: ${row.emptyReturnedToStore}\nBal: ${row.emptyBalance}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMono,
                            color = BrandRedOnContainer
                        )
                    }
                }
            }
        }
    }
}
