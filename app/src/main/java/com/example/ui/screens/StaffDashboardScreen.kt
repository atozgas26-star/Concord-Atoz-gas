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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ProductCategory
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.VehicleStockRow
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.TransactionEntryDialog
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
    isOnlineSynced: Boolean = true,
    onDateSelected: (String) -> Unit,
    onRecordTransaction: (TransactionType, Long, Long?, Int, String) -> Unit,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTxType by remember { mutableStateOf<TransactionType?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        BrandHeaderBanner(
            title = "CONCORD & A TO Z GAS",
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
                    icon = Icons.Default.Input,
                    containerColor = BrandGreenPrimary,
                    onClick = { activeTxType = TransactionType.VEHICLE_FULL_RECEIVED },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_receive_full")
                )
                StaffBigActionButton(
                    label = "Customer\nSale",
                    icon = Icons.Default.Sell,
                    containerColor = BrandRedSecondary,
                    onClick = { activeTxType = TransactionType.VEHICLE_CUSTOMER_SALE },
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
                    onClick = { activeTxType = TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_empty_customer")
                )
                StaffBigActionButton(
                    label = "Return Empty\nto Store",
                    icon = Icons.Default.Output,
                    containerColor = BrandRedSecondary,
                    onClick = { activeTxType = TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staff_action_return_empty")
                )
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Vehicle Stock") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Day Activity (${myVehicleTransactions.size})") }
            )
        }

        if (activeTab == 0) {
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
                    items(filteredRows, key = { it.product.id }) { row ->
                        // Staff CANNOT change opening stock
                        VehicleProductRowCard(row = row, canEditOpening = false)
                    }
                }
            }
        } else {
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

    if (activeTxType != null && assignedVehicle != null) {
        val type = activeTxType!!
        TransactionEntryDialog(
            title = "${assignedVehicle.name} — ${type.label}",
            transactionType = type,
            products = products,
            fixedVehicle = assignedVehicle,
            requireVehicleSelection = false,
            availableStockHint = { prodId, _ ->
                val vRow = vehicleRows.firstOrNull { it.product.id == prodId }
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
                onRecordTransaction(type, prodId, assignedVehicle.id, qty, notes)
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
