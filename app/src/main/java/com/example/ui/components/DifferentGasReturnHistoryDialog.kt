package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.DifferentGasReturnEntity
import com.example.data.model.CustomerReturnHistoryGroup
import com.example.data.model.StaffReturnHistoryGroup
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.SignatureRoyalBorder
import com.example.ui.theme.SignatureRoyalSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ExchangeDateFilter(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_MONTH("This Month")
}

private enum class ExchangeGasFilter(val label: String) {
    ALL("All Gases"),
    ARAB_GAS("Arab Gas"),
    EMIRATES_GAS("Emirates Gas"),
    INDUSTRIAL("Industrial")
}

/**
 * Futuristic Customer-wise & Staff-wise LPG / Different Gas Exchange History Modal.
 * Features Date, Returned Empty Company (Arab Gas / Emirates Gas) & Industrial filters,
 * atomic Edit & Delete with stock reversal (strictly hidden when isReadOnly == true),
 * and one-tap PDF / Excel export of filtered records.
 */
@Composable
fun DifferentGasReturnHistoryDialog(
    customerGroups: List<CustomerReturnHistoryGroup>,
    staffGroups: List<StaffReturnHistoryGroup>,
    isReadOnly: Boolean = false,
    onEditRecord: ((DifferentGasReturnEntity) -> Unit)? = null,
    onDeleteRecord: ((DifferentGasReturnEntity) -> Unit)? = null,
    onExportPdf: ((List<DifferentGasReturnEntity>) -> Unit)? = null,
    onExportExcel: ((List<DifferentGasReturnEntity>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Customer-wise, 1 = Staff-wise
    var query by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf(ExchangeDateFilter.ALL) }
    var gasFilter by remember { mutableStateOf(ExchangeGasFilter.ALL) }
    var selectedVehicleFilter by remember { mutableStateOf<String?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<DifferentGasReturnEntity?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val monthPrefix = remember(todayStr) { todayStr.take(7) }

    val allRecords = remember(customerGroups) {
        customerGroups.flatMap { it.records }.distinctBy { it.id }.sortedByDescending { it.timestamp }
    }

    val vehicleNames = remember(allRecords) {
        allRecords.map { it.vehicleName ?: "Main Store Yard" }.distinct().sorted()
    }

    fun matchesFilters(e: DifferentGasReturnEntity): Boolean {
        val matchesDate = when (dateFilter) {
            ExchangeDateFilter.ALL -> true
            ExchangeDateFilter.TODAY -> e.businessDate == todayStr
            ExchangeDateFilter.THIS_MONTH -> e.businessDate.startsWith(monthPrefix)
        }
        val combinedText = "${e.deliveredSummary} ${e.returnedSummary} ${e.returnedEmptyCompany}"
        val matchesGas = when (gasFilter) {
            ExchangeGasFilter.ALL -> true
            ExchangeGasFilter.ARAB_GAS -> combinedText.contains("Arab Gas", ignoreCase = true)
            ExchangeGasFilter.EMIRATES_GAS -> combinedText.contains("Emirates Gas", ignoreCase = true)
            ExchangeGasFilter.INDUSTRIAL -> listOf("Oxygen", "Acetylene", "Argon", "CO2", "Nitrogen").any {
                combinedText.contains(it, ignoreCase = true)
            }
        }
        val scopeLabel = e.vehicleName ?: "Main Store Yard"
        val matchesVehicle = selectedVehicleFilter == null || scopeLabel == selectedVehicleFilter
        val matchesQuery = query.isBlank() ||
            e.customerName.contains(query, ignoreCase = true) ||
            e.customerPhone.contains(query, ignoreCase = true) ||
            e.staffName.contains(query, ignoreCase = true) ||
            e.deliveredSummary.contains(query, ignoreCase = true) ||
            e.returnedSummary.contains(query, ignoreCase = true) ||
            e.returnedEmptyCompany.contains(query, ignoreCase = true) ||
            scopeLabel.contains(query, ignoreCase = true)
        return matchesDate && matchesGas && matchesVehicle && matchesQuery
    }

    val filteredRecords = remember(allRecords, dateFilter, gasFilter, selectedVehicleFilter, query) {
        allRecords.filter(::matchesFilters)
    }

    val filteredCustomerGroups = remember(customerGroups, dateFilter, gasFilter, selectedVehicleFilter, query) {
        customerGroups.mapNotNull { grp ->
            val matched = grp.records.filter(::matchesFilters)
            if (matched.isEmpty()) null else grp.copy(
                records = matched,
                totalDeliveredFull = matched.sumOf { it.totalDeliveredQty },
                totalReturnedEmpty = matched.sumOf { it.totalReturnedQty },
                totalTransactions = matched.size
            )
        }
    }

    val filteredStaffGroups = remember(staffGroups, dateFilter, gasFilter, selectedVehicleFilter, query) {
        staffGroups.mapNotNull { grp ->
            val matched = grp.records.filter(::matchesFilters)
            if (matched.isEmpty()) null else grp.copy(
                records = matched,
                totalDeliveredFull = matched.sumOf { it.totalDeliveredQty },
                totalReturnedEmpty = matched.sumOf { it.totalReturnedQty },
                totalTransactions = matched.size
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianMidnight,
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(listOf(ElectricBlueGlow.copy(alpha = 0.65f), MetallicOrangeBright.copy(alpha = 0.55f)))
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp)
                .testTag("different_gas_return_history_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ObsidianMidnight, RoyalBlueDeep, ObsidianDeep)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricBlueBright.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, ElectricBlueGlow.copy(alpha = 0.55f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LPG & Gas Exchange History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                                Text(
                                    text = "${filteredRecords.size} Records • ${filteredCustomerGroups.size} Customers • ${filteredStaffGroups.size} Staff",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PlatinumSilver
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MetallicOrange,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("return_history_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Return History", tint = ArcticWhite, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                SignatureAccentHairline(height = 2.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle Tabs: Customer-wise | Staff-wise
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { activeTab = 0 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeTab == 0) ElectricBlueBright else ObsidianElevated,
                            border = BorderStroke(1.dp, if (activeTab == 0) ElectricBlueGlow else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("return_history_tab_customer")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ArcticWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Customer (${filteredCustomerGroups.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                            }
                        }

                        Surface(
                            onClick = { activeTab = 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeTab == 1) MetallicOrange else ObsidianElevated,
                            border = BorderStroke(1.dp, if (activeTab == 1) MetallicOrangeBright else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("return_history_tab_staff")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = ArcticWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Staff (${filteredStaffGroups.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                            }
                        }
                    }

                    // Date + Gas Type Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExchangeDateFilter.entries.forEach { df ->
                            FilterChip(
                                selected = dateFilter == df,
                                onClick = { dateFilter = df },
                                label = { Text(df.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianElevated,
                                    labelColor = PlatinumSilver,
                                    selectedContainerColor = ElectricBlueBright,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        ExchangeGasFilter.entries.forEach { gf ->
                            FilterChip(
                                selected = gasFilter == gf,
                                onClick = { gasFilter = gf },
                                label = { Text(gf.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianElevated,
                                    labelColor = PlatinumSilver,
                                    selectedContainerColor = MetallicOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        if (vehicleNames.size > 1) {
                            vehicleNames.forEach { vName ->
                                val selected = selectedVehicleFilter == vName
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedVehicleFilter = if (selected) null else vName },
                                    label = { Text(vName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ObsidianElevated,
                                        labelColor = PlatinumSilver,
                                        selectedContainerColor = RoyalBlueDeep,
                                        selectedLabelColor = ElectricBlueGlow
                                    )
                                )
                            }
                        }
                    }

                    // Search + PDF/Excel Export Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search customer, Arab Gas, Emirates Gas, staff...", color = PlatinumMuted, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ArcticWhite,
                                unfocusedTextColor = ArcticWhite,
                                focusedContainerColor = ObsidianElevated,
                                unfocusedContainerColor = ObsidianDeep,
                                focusedBorderColor = ElectricBlueGlow,
                                unfocusedBorderColor = ObsidianBorder,
                                cursorColor = MetallicOrangeBright
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("return_history_search_input")
                        )
                    }

                    if (onExportPdf != null || onExportExcel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onExportPdf != null) {
                                OutlinedButton(
                                    enabled = filteredRecords.isNotEmpty(),
                                    onClick = { onExportPdf(filteredRecords) },
                                    border = BorderStroke(1.dp, MetallicOrangeBright),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("return_history_export_pdf_btn")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MetallicOrangeBright, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export PDF", color = MetallicOrangeBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (onExportExcel != null) {
                                OutlinedButton(
                                    enabled = filteredRecords.isNotEmpty(),
                                    onClick = { onExportExcel(filteredRecords) },
                                    border = BorderStroke(1.dp, ElectricBlueGlow),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("return_history_export_excel_btn")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export Excel", color = ElectricBlueGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeTab == 0) {
                        if (filteredCustomerGroups.isEmpty()) {
                            item {
                                ReturnHistoryEmptyState("No matching customer gas exchange records.")
                            }
                        } else {
                            items(filteredCustomerGroups, key = { "cust_${it.customerName}_${it.customerPhone}" }) { group ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianSurface,
                                    border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = group.customerName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ArcticWhite
                                                )
                                                if (group.customerPhone.isNotBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = MetallicOrangeBright, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = group.customerPhone,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = JetBrainsMono,
                                                            color = PlatinumSilver
                                                        )
                                                    }
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ObsidianElevated,
                                                border = BorderStroke(1.dp, ObsidianBorder)
                                            ) {
                                                Text(
                                                    text = "Del ${group.totalDeliveredFull}F • Ret ${group.totalReturnedEmpty}E",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElectricBlueGlow,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.55f))
                                        group.records.forEach { entry ->
                                            ReturnHistoryEntryRow(
                                                entry = entry,
                                                showCustomer = false,
                                                showStaff = true,
                                                canEditDelete = !isReadOnly && (onEditRecord != null || onDeleteRecord != null),
                                                onEdit = onEditRecord?.let { cb -> { cb(entry) } },
                                                onDelete = onDeleteRecord?.let { { pendingDeleteRecord = entry } }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (filteredStaffGroups.isEmpty()) {
                            item {
                                ReturnHistoryEmptyState("No matching staff gas exchange records.")
                            }
                        } else {
                            items(filteredStaffGroups, key = { "staff_${it.staffId}_${it.staffName}" }) { group ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianSurface,
                                    border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = group.staffName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ArcticWhite
                                                )
                                                Text(
                                                    text = "${group.totalTransactions} exchange transaction(s)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PlatinumMuted
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ObsidianElevated,
                                                border = BorderStroke(1.dp, ObsidianBorder)
                                            ) {
                                                Text(
                                                    text = "Del ${group.totalDeliveredFull}F • Ret ${group.totalReturnedEmpty}E",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MetallicOrangeBright,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.55f))
                                        group.records.forEach { entry ->
                                            ReturnHistoryEntryRow(
                                                entry = entry,
                                                showCustomer = true,
                                                showStaff = false,
                                                canEditDelete = !isReadOnly && (onEditRecord != null || onDeleteRecord != null),
                                                onEdit = onEditRecord?.let { cb -> { cb(entry) } },
                                                onDelete = onDeleteRecord?.let { { pendingDeleteRecord = entry } }
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

    val deleteTarget = pendingDeleteRecord
    if (deleteTarget != null && onDeleteRecord != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            containerColor = SignatureRoyalSurface,
            title = { Text("Delete Gas Exchange?", color = ArcticWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Deleting this exchange for ${deleteTarget.customerName} will safely reverse its Full and Empty cylinder stock impact automatically.",
                    color = PlatinumSilver
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteRecord = null
                        onDeleteRecord(deleteTarget)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Delete & Reverse Stock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) {
                    Text("Cancel", color = PlatinumSilver)
                }
            }
        )
    }
}

@Composable
private fun ReturnHistoryEntryRow(
    entry: DifferentGasReturnEntity,
    showCustomer: Boolean,
    showStaff: Boolean,
    canEditDelete: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(entry.timestamp))
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianElevated.copy(alpha = 0.65f),
        border = BorderStroke(0.8.dp, SignatureRoyalBorder.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val metaParts = buildList {
                    if (showCustomer) {
                        add(entry.customerName + if (entry.customerPhone.isNotBlank()) " (${entry.customerPhone})" else "")
                    }
                    if (showStaff) add("By: ${entry.staffName}")
                    add(entry.vehicleName?.let { "Veh: $it" } ?: "Store Yard")
                }
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PlatinumSilver,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${entry.businessDate} • $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = PlatinumMuted
                )
            }
            Text(
                text = "Delivered Full: ${entry.deliveredSummary}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMono,
                color = ElectricBlueGlow
            )
            Text(
                text = "Returned Empty: ${entry.returnedSummary}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMono,
                color = MetallicOrangeBright
            )
            if (entry.returnedEmptyCompany.isNotBlank()) {
                val isEmirates = entry.returnedEmptyCompany.contains("Emirates", ignoreCase = true)
                val badgeColor = if (isEmirates) MetallicOrangeBright else ElectricBlueGlow
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "Empty Company: ${entry.returnedEmptyCompany}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (entry.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${entry.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PlatinumMuted
                )
            }
            if (canEditDelete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("dgr_edit_${entry.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit exchange", tint = ElectricBlueGlow, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = ElectricBlueGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("dgr_delete_${entry.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete exchange", tint = BrandRedBright, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = BrandRedBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnHistoryEmptyState(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = PlatinumMuted
            )
        }
    }
}
