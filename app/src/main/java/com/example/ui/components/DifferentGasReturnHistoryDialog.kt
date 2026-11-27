package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Customer-wise and Staff-wise Different Gas Cylinder Return History modal.
 */
@Composable
fun DifferentGasReturnHistoryDialog(
    customerGroups: List<CustomerReturnHistoryGroup>,
    staffGroups: List<StaffReturnHistoryGroup>,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Customer-wise, 1 = Staff-wise
    var query by remember { mutableStateOf("") }

    val filteredCustomerGroups = remember(customerGroups, query) {
        if (query.isBlank()) customerGroups
        else customerGroups.filter { grp ->
            grp.customerName.contains(query, ignoreCase = true) ||
                grp.customerPhone.contains(query, ignoreCase = true) ||
                grp.records.any { e ->
                    e.deliveredSummary.contains(query, ignoreCase = true) ||
                        e.returnedSummary.contains(query, ignoreCase = true) ||
                        e.staffName.contains(query, ignoreCase = true)
                }
        }
    }

    val filteredStaffGroups = remember(staffGroups, query) {
        if (query.isBlank()) staffGroups
        else staffGroups.filter { grp ->
            grp.staffName.contains(query, ignoreCase = true) ||
                grp.records.any { e ->
                    e.customerName.contains(query, ignoreCase = true) ||
                        e.deliveredSummary.contains(query, ignoreCase = true) ||
                        e.returnedSummary.contains(query, ignoreCase = true)
                }
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
                .widthIn(max = 620.dp)
                .heightIn(max = 740.dp)
                .testTag("different_gas_return_history_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ObsidianMidnight, Color(0xFF0E2A66), ObsidianDeep)
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
                                    text = "Different Gas Return History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                                Text(
                                    text = "${customerGroups.size} Customers • ${staffGroups.size} Staff Accounts",
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ArcticWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Customer-wise (${customerGroups.size})",
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = ArcticWhite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Staff-wise (${staffGroups.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search customer, phone, staff, or gas...", color = PlatinumMuted, fontSize = 12.sp) },
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
                            .fillMaxWidth()
                            .testTag("return_history_search_input")
                    )
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
                                ReturnHistoryEmptyState("No customer different-gas return records yet.")
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
                                            ReturnHistoryEntryRow(entry = entry, showCustomer = false, showStaff = true)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (filteredStaffGroups.isEmpty()) {
                            item {
                                ReturnHistoryEmptyState("No staff different-gas return records yet.")
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
                                                    text = "${group.totalTransactions} return transaction(s)",
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
                                            ReturnHistoryEntryRow(entry = entry, showCustomer = true, showStaff = false)
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
}

@Composable
private fun ReturnHistoryEntryRow(
    entry: DifferentGasReturnEntity,
    showCustomer: Boolean,
    showStaff: Boolean
) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(entry.timestamp))
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianElevated.copy(alpha = 0.65f),
        border = BorderStroke(0.8.dp, ObsidianBorder.copy(alpha = 0.6f)),
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
            if (entry.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${entry.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PlatinumMuted
                )
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
