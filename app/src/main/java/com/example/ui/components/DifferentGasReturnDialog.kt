package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProductEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.ProductCategory
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
import java.util.UUID

/**
 * Luxury C&A GAS V2.3 Different Gas Cylinder Return Dialog.
 * Allows BOTH Admin and Staff to deliver Full cylinder(s) of one or more gas types
 * and receive Empty cylinder(s) of DIFFERENT gas type(s) in a single atomic transaction
 * with immediate stock update (no Admin approval required) and duplicate-submission guard.
 */
@Composable
fun DifferentGasReturnDialog(
    products: List<ProductEntity>,
    vehicles: List<VehicleEntity> = emptyList(),
    fixedVehicle: VehicleEntity? = null,
    lockToStore: Boolean = false,
    allowVehicleSwitch: Boolean = false,
    recentCustomerNames: List<String> = emptyList(),
    availableFullLookup: (productId: Long, vehicleId: Long?) -> Int,
    onVehicleSelected: (Long?) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (
        deliveredItems: List<CustomerExchangeItem>,
        returnedItems: List<CustomerExchangeItem>,
        vehicleId: Long?,
        customerName: String,
        customerPhone: String,
        notes: String,
        submissionKey: String
    ) -> Unit
) {
    val submissionKey = remember { "dgr_${UUID.randomUUID()}" }
    var isSubmitting by remember { mutableStateOf(false) }

    var selectedVehicle by remember(fixedVehicle, lockToStore, vehicles) {
        mutableStateOf(
            when {
                lockToStore -> null
                fixedVehicle != null -> fixedVehicle
                else -> null // Default to Main Store on Admin Dashboard unless vehicle selected
            }
        )
    }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var activeSection by remember { mutableStateOf(0) } // 0 = Delivered Full, 1 = Returned Empty
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val deliveredQtyMap = remember { mutableStateMapOf<Long, Int>() }
    val returnedQtyMap = remember { mutableStateMapOf<Long, Int>() }
    var localError by remember { mutableStateOf<String?>(null) }

    // Respect exact admin-saved sortOrder
    val orderedProducts = remember(products) {
        products.sortedWith(compareBy({ it.category.sortOrder }, { it.sortOrder }, { it.id }))
    }

    val visibleProducts = remember(orderedProducts, selectedCategory, searchQuery) {
        orderedProducts.filter { p ->
            (selectedCategory == null || p.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.cylinderSizeKg.contains(searchQuery, ignoreCase = true) ||
                    p.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    val activeVehicleId = if (lockToStore) null else (fixedVehicle?.id ?: selectedVehicle?.id)

    val deliveredItems = orderedProducts.mapNotNull { prod ->
        val qty = deliveredQtyMap[prod.id] ?: 0
        if (qty > 0) CustomerExchangeItem(productId = prod.id, quantity = qty) else null
    }
    val returnedItems = orderedProducts.mapNotNull { prod ->
        val qty = returnedQtyMap[prod.id] ?: 0
        if (qty > 0) CustomerExchangeItem(productId = prod.id, quantity = qty) else null
    }

    val totalDelivered = deliveredItems.sumOf { it.quantity }
    val totalReturned = returnedItems.sumOf { it.quantity }

    val stockShortages = deliveredItems.mapNotNull { item ->
        val prod = orderedProducts.firstOrNull { it.id == item.productId } ?: return@mapNotNull null
        val avail = availableFullLookup(item.productId, activeVehicleId)
        if (item.quantity > avail) "${prod.name} (Need ${item.quantity}, Avail $avail)" else null
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ArcticWhite,
        unfocusedTextColor = ArcticWhite,
        focusedContainerColor = ObsidianElevated.copy(alpha = 0.85f),
        unfocusedContainerColor = ObsidianDeep.copy(alpha = 0.8f),
        focusedBorderColor = ElectricBlueGlow,
        unfocusedBorderColor = ObsidianBorder,
        focusedLabelColor = ElectricBlueGlow,
        unfocusedLabelColor = PlatinumMuted,
        cursorColor = MetallicOrangeBright
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianMidnight,
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(listOf(ElectricBlueGlow.copy(alpha = 0.7f), MetallicOrangeBright.copy(alpha = 0.65f)))
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 600.dp)
                .heightIn(max = 760.dp)
                .testTag("different_gas_return_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Luxury Top Header
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MetallicOrange.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.6f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = MetallicOrangeBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Different Gas Cylinder Return",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                                Text(
                                    text = "Immediate Stock Update • No Admin Approval Required",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399)
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
                                modifier = Modifier.testTag("dgr_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Different Gas Return",
                                    tint = ArcticWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                SignatureAccentHairline(height = 2.dp)

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Scope Selector (Main Store vs Vehicle)
                    if (allowVehicleSwitch && fixedVehicle == null && !lockToStore) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { vehicleMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianElevated,
                                border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dgr_scope_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = ElectricBlueGlow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "STOCK LOCATION",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PlatinumMuted,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = selectedVehicle?.let { "Vehicle: ${it.name}" } ?: "Main Store Yard",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = ArcticWhite
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select location",
                                        tint = ElectricBlueGlow
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = vehicleMenuExpanded,
                                onDismissRequest = { vehicleMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Main Store Yard") },
                                    onClick = {
                                        selectedVehicle = null
                                        onVehicleSelected(null)
                                        vehicleMenuExpanded = false
                                    }
                                )
                                vehicles.forEach { veh ->
                                    DropdownMenuItem(
                                        text = { Text("Vehicle: ${veh.name}") },
                                        onClick = {
                                            selectedVehicle = veh
                                            onVehicleSelected(veh.id)
                                            vehicleMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (lockToStore || fixedVehicle == null) {
                                    "Stock Scope: Main Store Yard"
                                } else {
                                    "Stock Scope: ${fixedVehicle.name}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlueGlow,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Customer Details (Name + Phone)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = {
                                customerName = it
                                localError = null
                            },
                            label = { Text("Customer Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("dgr_customer_name_input")
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = {
                                customerPhone = it.take(20)
                                localError = null
                            },
                            label = { Text("Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MetallicOrangeBright, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dgr_customer_phone_input")
                        )
                    }

                    if (recentCustomerNames.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentCustomerNames.take(6).forEach { name ->
                                FilterChip(
                                    selected = customerName.equals(name, ignoreCase = true),
                                    onClick = {
                                        customerName = name
                                        localError = null
                                    },
                                    label = { Text(name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ObsidianElevated,
                                        labelColor = PlatinumSilver,
                                        selectedContainerColor = ElectricBlueBright,
                                        selectedLabelColor = ArcticWhite
                                    )
                                )
                            }
                        }
                    }

                    // Dual Step Switcher: 1) Delivered Full  |  2) Returned Empty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val delSelected = activeSection == 0
                        Surface(
                            onClick = { activeSection = 0 },
                            shape = RoundedCornerShape(14.dp),
                            color = if (delSelected) ElectricBlueBright else ObsidianElevated,
                            border = BorderStroke(
                                1.2.dp,
                                if (delSelected) ElectricBlueGlow else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dgr_tab_delivered")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "1. DELIVERED FULL",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (delSelected) ArcticWhite else ElectricBlueGlow
                                )
                                Text(
                                    text = "${deliveredItems.size} gases • -$totalDelivered Full",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = if (delSelected) ArcticWhite.copy(alpha = 0.9f) else PlatinumMuted
                                )
                            }
                        }

                        val retSelected = activeSection == 1
                        Surface(
                            onClick = { activeSection = 1 },
                            shape = RoundedCornerShape(14.dp),
                            color = if (retSelected) MetallicOrange else ObsidianElevated,
                            border = BorderStroke(
                                1.2.dp,
                                if (retSelected) MetallicOrangeBright else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dgr_tab_returned")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "2. RETURNED EMPTY",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (retSelected) ArcticWhite else MetallicOrangeBright
                                )
                                Text(
                                    text = "${returnedItems.size} gases • +$totalReturned Empty",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = if (retSelected) ArcticWhite.copy(alpha = 0.9f) else PlatinumMuted
                                )
                            }
                        }
                    }

                    // Category filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Gases", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ObsidianElevated,
                                labelColor = PlatinumSilver,
                                selectedContainerColor = ElectricBlueBright,
                                selectedLabelColor = ArcticWhite
                            )
                        )
                        ProductCategory.entries.forEach { cat ->
                            val isSel = selectedCategory == cat
                            val catStyle = categoryVisualStyle(cat)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = if (isSel) null else cat },
                                label = { Text(cat.shortCode, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianElevated,
                                    labelColor = PlatinumSilver,
                                    selectedContainerColor = catStyle.primaryColor,
                                    selectedLabelColor = ArcticWhite
                                )
                            )
                        }
                    }

                    // Quick search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Oxygen, Argon, CO2, LPG, Acetylene...", color = PlatinumMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dgr_search_input")
                    )

                    // Gas Product Qty Selector List
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (activeSection == 0) {
                                    "Select FULL cylinders delivered to customer (deducts from exact gas Full stock):"
                                } else {
                                    "Select EMPTY cylinders returned by customer (adds to exact gas Empty stock):"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeSection == 0) ElectricBlueGlow else MetallicOrangeBright,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.6f))

                            if (visibleProducts.isEmpty()) {
                                Text(
                                    text = "No matching gas products found.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PlatinumMuted,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                visibleProducts.forEach { prod ->
                                    val qtyMap = if (activeSection == 0) deliveredQtyMap else returnedQtyMap
                                    val currentQty = qtyMap[prod.id] ?: 0
                                    val availFull = availableFullLookup(prod.id, activeVehicleId)
                                    val isInsufficient = activeSection == 0 && currentQty > availFull
                                    val accent = if (activeSection == 0) ElectricBlueGlow else MetallicOrangeBright
                                    val rowTagPrefix = if (activeSection == 0) "dgr_del" else "dgr_ret"

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isInsufficient -> Color(0xFF3F121D)
                                            currentQty > 0 -> accent.copy(alpha = 0.14f)
                                            else -> ObsidianElevated.copy(alpha = 0.6f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isInsufficient -> Color(0xFFEF4444)
                                                currentQty > 0 -> accent.copy(alpha = 0.65f)
                                                else -> ObsidianBorder.copy(alpha = 0.5f)
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = prod.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ArcticWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (prod.cylinderSizeKg.isNotBlank()) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = prod.cylinderSizeKg,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = PlatinumSilver,
                                                            fontFamily = JetBrainsMono
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (activeSection == 0) {
                                                        "${prod.category.shortCode} • Full Stock Avail: $availFull"
                                                    } else {
                                                        "${prod.category.shortCode} • Adds to Empty Stock"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isInsufficient) Color(0xFFFCA5A5) else PlatinumMuted
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = ObsidianDeep,
                                                    border = BorderStroke(1.dp, ObsidianBorder),
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            if (currentQty > 0) {
                                                                val next = currentQty - 1
                                                                if (next == 0) qtyMap.remove(prod.id) else qtyMap[prod.id] = next
                                                                localError = null
                                                            }
                                                        }
                                                        .testTag("${rowTagPrefix}_dec_${prod.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease ${prod.name}", tint = PlatinumSilver, modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ObsidianDeep,
                                                    border = BorderStroke(1.dp, if (currentQty > 0) accent else ObsidianBorder),
                                                    modifier = Modifier.width(46.dp)
                                                ) {
                                                    BasicTextField(
                                                        value = if (currentQty > 0) currentQty.toString() else "",
                                                        onValueChange = { raw ->
                                                            val digits = raw.filter { it.isDigit() }.take(4)
                                                            val parsed = digits.toIntOrNull() ?: 0
                                                            if (parsed <= 0) qtyMap.remove(prod.id) else qtyMap[prod.id] = parsed
                                                            localError = null
                                                        },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        textStyle = TextStyle(
                                                            color = ArcticWhite,
                                                            fontFamily = JetBrainsMono,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            textAlign = TextAlign.Center
                                                        ),
                                                        cursorBrush = SolidColor(MetallicOrangeBright),
                                                        decorationBox = { inner ->
                                                            Box(
                                                                modifier = Modifier.padding(vertical = 6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (currentQty == 0) {
                                                                    Text("0", color = PlatinumMuted, fontSize = 13.sp, fontFamily = JetBrainsMono)
                                                                }
                                                                inner()
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("${rowTagPrefix}_qty_${prod.id}")
                                                    )
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = accent.copy(alpha = 0.25f),
                                                    border = BorderStroke(1.dp, accent),
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            qtyMap[prod.id] = currentQty + 1
                                                            localError = null
                                                        }
                                                        .testTag("${rowTagPrefix}_inc_${prod.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase ${prod.name}", tint = ArcticWhite, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Stock Impact Preview Card
                    if (deliveredItems.isNotEmpty() || returnedItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dgr_impact_preview")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE STOCK IMPACT (CATEGORY-ISOLATED)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                                if (deliveredItems.isNotEmpty()) {
                                    val delSummary = deliveredItems.joinToString(", ") { item ->
                                        val name = orderedProducts.firstOrNull { it.id == item.productId }?.name ?: "#${item.productId}"
                                        "$name Full -${item.quantity}"
                                    }
                                    Text(
                                        text = "Delivered: $delSummary",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = JetBrainsMono,
                                        color = ElectricBlueGlow
                                    )
                                }
                                if (returnedItems.isNotEmpty()) {
                                    val retSummary = returnedItems.joinToString(", ") { item ->
                                        val name = orderedProducts.firstOrNull { it.id == item.productId }?.name ?: "#${item.productId}"
                                        "$name Empty +${item.quantity}"
                                    }
                                    Text(
                                        text = "Returned: $retSummary",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = JetBrainsMono,
                                        color = MetallicOrangeBright
                                    )
                                }
                            }
                        }
                    }

                    // Notes field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Invoice Ref (optional)") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dgr_notes_input")
                    )

                    if (stockShortages.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3F121D),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Insufficient Full Stock: ${stockShortages.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    if (localError != null) {
                        Text(
                            text = localError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Footer Action Bar
                HorizontalDivider(color = ObsidianBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Cancel", color = PlatinumSilver)
                    }
                    Button(
                        enabled = !isSubmitting && stockShortages.isEmpty(),
                        onClick = {
                            if (isSubmitting) return@Button
                            when {
                                customerName.trim().isEmpty() -> {
                                    localError = "Customer name is required."
                                }
                                deliveredItems.isEmpty() -> {
                                    localError = "Select at least 1 Delivered Full gas cylinder."
                                    activeSection = 0
                                }
                                returnedItems.isEmpty() -> {
                                    localError = "Select at least 1 Returned Empty gas cylinder."
                                    activeSection = 1
                                }
                                stockShortages.isNotEmpty() -> {
                                    localError = "Insufficient Full stock for selected delivered gas."
                                }
                                else -> {
                                    isSubmitting = true
                                    onConfirm(
                                        deliveredItems,
                                        returnedItems,
                                        activeVehicleId,
                                        customerName.trim(),
                                        customerPhone.trim(),
                                        notes.trim(),
                                        submissionKey
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicOrange),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("dgr_save_button")
                    ) {
                        Icon(Icons.Default.Recycling, contentDescription = null, tint = ArcticWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isSubmitting) "Saving..." else "Save Return ($totalDelivered F / $totalReturned E)",
                            fontWeight = FontWeight.Bold,
                            color = ArcticWhite
                        )
                    }
                }
            }
        }
    }
}
