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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.VehicleEntity
import com.example.data.model.ProductCategory
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionType
import com.example.data.model.VehicleOverview
import com.example.data.model.VehicleStockRow
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.OpeningStockEditDialog
import com.example.ui.components.StockPill
import com.example.ui.components.TransactionEntryDialog
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono

@Composable
fun VehicleManagementScreen(
    selectedDate: String,
    vehicleOverviews: List<VehicleOverview>,
    onDateSelected: (String) -> Unit,
    onSelectVehicle: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "Vehicle Management",
            subtitle = "7 Fleet Accounts • Tap a vehicle for full ledger"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
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
}

@Composable
fun VehicleDetailScreen(
    vehicle: VehicleEntity?,
    selectedDate: String,
    rows: List<VehicleStockRow>,
    storeRows: List<StoreStockRow>,
    products: List<ProductEntity>,
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String) -> Unit,
    onUpdateVehicleOpening: (vehicleId: Long, productId: Long, oldFull: Int, oldEmpty: Int) -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var activeTxType by remember { mutableStateOf<TransactionType?>(null) }
    var editingRow by remember { mutableStateOf<VehicleStockRow?>(null) }

    val filteredRows = remember(rows, selectedCategory) {
        rows.filter { selectedCategory == null || it.product.category == selectedCategory }
    }

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
                    onClick = { activeTxType = TransactionType.VEHICLE_FULL_RECEIVED },
                    label = { Text("Receive Full from Store") },
                    leadingIcon = { Icon(Icons.Default.Input, contentDescription = null, tint = BrandGreenPrimary) }
                )
                AssistChip(
                    onClick = { activeTxType = TransactionType.VEHICLE_CUSTOMER_SALE },
                    label = { Text("Customer Sale") },
                    leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null, tint = BrandRedSecondary) }
                )
                AssistChip(
                    onClick = { activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER },
                    label = { Text("Customer Empty Recd") },
                    leadingIcon = { Icon(Icons.Default.Recycling, contentDescription = null, tint = BrandGreenPrimary) }
                )
                AssistChip(
                    onClick = { activeTxType = TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE },
                    label = { Text("Return Empty to Store") },
                    leadingIcon = { Icon(Icons.Default.Output, contentDescription = null, tint = BrandRedSecondary) }
                )
            }

            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

    if (activeTxType != null && vehicle != null) {
        val type = activeTxType!!
        TransactionEntryDialog(
            title = "${vehicle.name} — ${type.label}",
            transactionType = type,
            products = products,
            fixedVehicle = vehicle,
            requireVehicleSelection = false,
            availableStockHint = { prodId, _ ->
                val vRow = rows.firstOrNull { it.product.id == prodId }
                val sRow = storeRows.firstOrNull { it.product.id == prodId }
                when (type) {
                    TransactionType.VEHICLE_FULL_RECEIVED ->
                        "Store Full Available: ${sRow?.storeFullBalance ?: 0}  |  Vehicle Full: ${vRow?.fullBalance ?: 0}"
                    TransactionType.VEHICLE_CUSTOMER_SALE ->
                        "Vehicle Full Available: ${vRow?.fullBalance ?: 0}"
                    TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE ->
                        "Vehicle Empty Available: ${vRow?.emptyBalance ?: 0}"
                    else -> "Vehicle Empty Balance: ${vRow?.emptyBalance ?: 0}"
                }
            },
            onDismiss = { activeTxType = null },
            onConfirm = { prodId, _, qty, notes ->
                onRecordTransaction(type, prodId, vehicle.id, qty, notes)
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
