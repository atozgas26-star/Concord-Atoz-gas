package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.CustomerPendingRow
import com.example.data.model.ProductCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import com.example.ui.viewmodel.WarningAlert
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.DarkHeaderEnd
import com.example.ui.theme.DarkHeaderStart
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recycling
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun BrandHeaderBanner(
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val startColor = if (isDark) DarkHeaderStart else MaterialTheme.colorScheme.primary
    val endColor = if (isDark) DarkHeaderEnd else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val contentColor = Color.White
    val accentLine = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(startColor, endColor)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFB31B1A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ca_red_logo),
                            contentDescription = "C&A Gas Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentLine)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorBar(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onDateSelected(shiftIsoDate(selectedDate, -1)) },
                modifier = Modifier.testTag("date_prev_button")
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("date_picker_trigger"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDisplayDate(selectedDate),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMono
                )
            }

            IconButton(
                onClick = { onDateSelected(shiftIsoDate(selectedDate, 1)) },
                modifier = Modifier.testTag("date_next_button")
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
            }
        }
    }

    if (showPicker) {
        val initialMillis = parseIsoToUtcMillis(selectedDate)
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(utcMillisToIso(millis))
                    }
                    showPicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onContainerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = onContainerColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMono,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StockPill(
    label: String,
    value: Int,
    isFull: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isFull) BrandGreenContainer else BrandRedContainer
    val fg = if (isFull) BrandGreenOnContainer else BrandRedOnContainer
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: ProductCategory?,
    onCategorySelected: (ProductCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All Gases") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandGreenPrimary,
                selectedLabelColor = Color.White
            )
        )
        ProductCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandGreenPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryDialog(
    title: String,
    transactionType: TransactionType,
    products: List<ProductEntity>,
    vehicles: List<VehicleEntity> = emptyList(),
    fixedVehicle: VehicleEntity? = null,
    requireVehicleSelection: Boolean = false,
    showPurchaseCompanyField: Boolean = false,
    recentPurchaseCompanies: List<String> = emptyList(),
    showCustomerNameField: Boolean = false,
    requireCustomerName: Boolean = false,
    recentCustomerNames: List<String> = emptyList(),
    initialCustomerName: String = "",
    initialQuantity: Int = 1,
    showEmptyCollectedNowField: Boolean = false,
    availableStockHint: ((productId: Long, vehicleId: Long?) -> String?)? = null,
    liveCalculationPreview: ((productId: Long, vehicleId: Long?, qty: Int, emptyCollectedNow: Int) -> String?)? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        productId: Long,
        vehicleId: Long?,
        quantity: Int,
        notes: String,
        purchaseCompanyName: String,
        customerName: String,
        emptyCollectedNow: Int
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ProductCategory.INDUSTRIAL_GAS) }
    val filteredProducts = remember(products, selectedCategory) {
        products.filter { it.category == selectedCategory }
    }
    var selectedProduct by remember(filteredProducts) {
        mutableStateOf(filteredProducts.firstOrNull())
    }
    var productMenuExpanded by remember { mutableStateOf(false) }

    var selectedVehicle by remember(fixedVehicle, vehicles) {
        mutableStateOf(fixedVehicle ?: vehicles.firstOrNull())
    }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    var quantityText by remember(initialQuantity) { mutableStateOf(initialQuantity.coerceAtLeast(1).toString()) }
    var emptyCollectedText by remember { mutableStateOf("0") }
    var purchaseCompanyName by remember { mutableStateOf("") }
    var customerName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val parsedQty = quantityText.toIntOrNull() ?: 0
    val parsedEmptyNow = if (showEmptyCollectedNowField) (emptyCollectedText.toIntOrNull() ?: 0) else 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Text(
                    text = transactionType.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Category chips
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                selectedProduct = products.firstOrNull { it.category == cat }
                            },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Product dropdown
                ExposedDropdownMenuBox(
                    expanded = productMenuExpanded,
                    onExpandedChange = { productMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Select Cylinder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product / Cylinder Size") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("tx_product_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = productMenuExpanded,
                        onDismissRequest = { productMenuExpanded = false }
                    ) {
                        filteredProducts.forEach { product ->
                            DropdownMenuItem(
                                text = { Text("${product.category.shortCode} • ${product.name}") },
                                onClick = {
                                    selectedProduct = product
                                    productMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Vehicle selector if applicable
                if (requireVehicleSelection && fixedVehicle == null && vehicles.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = vehicleMenuExpanded,
                        onExpandedChange = { vehicleMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.name ?: "Select Vehicle",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle / Staff Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("tx_vehicle_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleMenuExpanded,
                            onDismissRequest = { vehicleMenuExpanded = false }
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.name) },
                                    onClick = {
                                        selectedVehicle = vehicle
                                        vehicleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (fixedVehicle != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = BrandGreenContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Vehicle: ${fixedVehicle.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandGreenOnContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Live available stock hint
                val hint = selectedProduct?.let { prod ->
                    availableStockHint?.invoke(prod.id, (fixedVehicle ?: selectedVehicle)?.id)
                }
                if (!hint.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMono,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quantity stepper + numeric field
                Text("Quantity (Cylinders)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val current = quantityText.toIntOrNull() ?: 1
                            if (current > 1) quantityText = (current - 1).toString()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                quantityText = input
                                localError = null
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_quantity_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val current = quantityText.toIntOrNull() ?: 0
                            quantityText = (current + 1).toString()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                    }
                }

                if (showEmptyCollectedNowField) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emptyCollectedText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                emptyCollectedText = input
                                localError = null
                            }
                        },
                        label = { Text("Empty Cylinders Collected Now (Optional)") },
                        placeholder = { Text("0 if pending all") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_empty_collected_now_input")
                    )
                    if (parsedQty > 0) {
                        val pendingDiff = parsedQty - parsedEmptyNow
                        val badgeText = when {
                            pendingDiff > 0 -> "Customer Pending Balance: +$pendingDiff Empty"
                            pendingDiff == 0 -> "All $parsedQty Empty Returned (0 Pending)"
                            else -> "Extra ${-pendingDiff} Previous Pending Empty Settled"
                        }
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = if (pendingDiff > 0) BrandRedContainer else BrandGreenContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingDiff > 0) BrandRedOnContainer else BrandGreenOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Live Calculation Preview (Full / Empty / Return / Dispatch Before -> After)
                val calcText = selectedProduct?.let { prod ->
                    liveCalculationPreview?.invoke(
                        prod.id,
                        (fixedVehicle ?: selectedVehicle)?.id,
                        parsedQty,
                        parsedEmptyNow
                    )
                }
                if (!calcText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = BrandGreenContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Live Calculation",
                                tint = BrandGreenOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = calcText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = BrandGreenOnContainer
                            )
                        }
                    }
                }

                if (showPurchaseCompanyField) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = purchaseCompanyName,
                        onValueChange = { input ->
                            if (input.length <= 120) {
                                purchaseCompanyName = input
                                localError = null
                            }
                        },
                        label = { Text("Purchase Company Name") },
                        placeholder = { Text("e.g., Emirates Gas, ADNOC, Concord Plant") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Purchase Company"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_purchase_company_input")
                    )
                    if (recentPurchaseCompanies.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentPurchaseCompanies.forEach { company ->
                                FilterChip(
                                    selected = purchaseCompanyName.equals(company, ignoreCase = true),
                                    onClick = {
                                        purchaseCompanyName = company
                                        localError = null
                                    },
                                    label = { Text(company, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                if (showCustomerNameField) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { input ->
                            if (input.length <= 120) {
                                customerName = input
                                localError = null
                            }
                        },
                        label = {
                            Text(if (requireCustomerName) "Customer Name *" else "Customer Name (for Empty Balance)")
                        },
                        placeholder = { Text("e.g., Al Noor Hotel, Gulf Workshop") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Customer Name"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_customer_name_input")
                    )
                    if (recentCustomerNames.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentCustomerNames.forEach { cust ->
                                FilterChip(
                                    selected = customerName.equals(cust, ignoreCase = true),
                                    onClick = {
                                        customerName = cust
                                        localError = null
                                    },
                                    label = { Text(cust, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Invoice / Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_notes_input")
                )

                if (localError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = localError!!,
                        color = BrandRedBright,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val prod = selectedProduct
                            val qty = quantityText.toIntOrNull() ?: 0
                            val cleanCompany = purchaseCompanyName.trim()
                            val cleanCustomer = customerName.trim()
                            val empNow = if (showEmptyCollectedNowField) (emptyCollectedText.toIntOrNull() ?: 0) else 0
                            if (prod == null) {
                                localError = "Please select a product."
                            } else if (qty <= 0) {
                                localError = "Enter quantity greater than 0."
                            } else if (showPurchaseCompanyField && cleanCompany.isEmpty()) {
                                localError = "Please enter the purchase company name."
                            } else if (requireCustomerName && cleanCustomer.isEmpty()) {
                                localError = "Please enter the customer name."
                            } else {
                                onConfirm(
                                    prod.id,
                                    if (requireVehicleSelection) selectedVehicle?.id else null,
                                    qty,
                                    notes,
                                    if (showPurchaseCompanyField) cleanCompany else "",
                                    if (showCustomerNameField) cleanCustomer else "",
                                    empNow
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: StockTransactionEntity,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        productId: Long,
        quantity: Int,
        purchaseCompanyName: String,
        customerName: String,
        notes: String
    ) -> Unit
) {
    val initialProd = remember(transaction.productId, products) {
        products.firstOrNull { it.id == transaction.productId } ?: products.firstOrNull()
    }
    var selectedCategory by remember(transaction.id) {
        mutableStateOf(initialProd?.category ?: ProductCategory.INDUSTRIAL_GAS)
    }
    val filteredProducts = remember(products, selectedCategory) {
        products.filter { it.category == selectedCategory }
    }
    var selectedProduct by remember(transaction.id, filteredProducts) {
        mutableStateOf(
            filteredProducts.firstOrNull { it.id == transaction.productId }
                ?: filteredProducts.firstOrNull()
        )
    }
    var productExpanded by remember { mutableStateOf(false) }

    var quantityText by remember(transaction.id) { mutableStateOf(transaction.quantity.toString()) }
    var purchaseCompanyName by remember(transaction.id) { mutableStateOf(transaction.purchaseCompanyName) }
    var customerName by remember(transaction.id) { mutableStateOf(transaction.customerName) }
    var notes by remember(transaction.id) { mutableStateOf(transaction.notes) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isPurchaseType = transaction.type == TransactionType.STORE_PURCHASE_FULL ||
        transaction.type == TransactionType.STORE_PURCHASE_EMPTY ||
        transaction.type == TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE
    val isCustomerType = transaction.type == TransactionType.VEHICLE_CUSTOMER_SALE ||
        transaction.type == TransactionType.VEHICLE_NEW_CUSTOMER_SALE ||
        transaction.type == TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE ||
        transaction.type == TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER ||
        transaction.type == TransactionType.CUSTOMER_EMPTY_PENDING_ADD ||
        transaction.type == TransactionType.STORE_FULL_DISPATCH_DIRECT ||
        transaction.customerName.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Entry",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${transaction.type.label} • ${transaction.businessDate}" +
                                (transaction.vehicleName?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Category chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                selectedProduct = products.firstOrNull { it.category == cat }
                            },
                            label = { Text(cat.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Select Cylinder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product / Cylinder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        filteredProducts.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.category.shortCode} • ${prod.name}") },
                                onClick = {
                                    selectedProduct = prod
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val current = quantityText.toIntOrNull() ?: 1
                            if (current > 1) quantityText = (current - 1).toString()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                quantityText = input
                                localError = null
                            }
                        },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_tx_quantity_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val current = quantityText.toIntOrNull() ?: 0
                            quantityText = (current + 1).toString()
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                if (isPurchaseType || transaction.purchaseCompanyName.isNotBlank()) {
                    OutlinedTextField(
                        value = purchaseCompanyName,
                        onValueChange = { if (it.length <= 120) purchaseCompanyName = it },
                        label = { Text("Purchase Company Name") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tx_purchase_company_input")
                    )
                }

                if (isCustomerType) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { if (it.length <= 120) customerName = it },
                        label = { Text("Customer Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_tx_customer_name_input")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 300) notes = it },
                    label = { Text("Notes / Invoice") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_notes_input")
                )

                if (localError != null) {
                    Text(localError!!, color = BrandRedBright, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val prod = selectedProduct
                            val qty = quantityText.toIntOrNull() ?: 0
                            if (prod == null) {
                                localError = "Select a product."
                            } else if (qty <= 0) {
                                localError = "Enter quantity > 0."
                            } else if (isPurchaseType && purchaseCompanyName.trim().isEmpty()) {
                                localError = "Purchase company name is required."
                            } else {
                                onConfirm(
                                    prod.id,
                                    qty,
                                    purchaseCompanyName.trim(),
                                    customerName.trim(),
                                    notes.trim()
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_tx_save_button")
                    ) {
                        Text("Update Entry")
                    }
                }
            }
        }
    }
}

@Composable
fun AddStaffVehicleDialog(
    nextVehicleNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (staffName: String, vehicleName: String, pin: String) -> Unit
) {
    var staffName by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }
    var vehicleEditedManually by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("10${nextVehicleNumber.coerceIn(10, 99)}") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff & Vehicle (#$nextVehicleNumber)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = { input ->
                        if (input.length <= 100) {
                            staffName = input
                            if (!vehicleEditedManually) {
                                vehicleName = input.trim().uppercase().take(64)
                            }
                            error = null
                        }
                    },
                    label = { Text("Staff Name") },
                    placeholder = { Text("e.g., Imran") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_name_input")
                )
                OutlinedTextField(
                    value = vehicleName,
                    onValueChange = { input ->
                        if (input.length <= 64) {
                            vehicleName = input.uppercase()
                            vehicleEditedManually = true
                            error = null
                        }
                    },
                    label = { Text("Vehicle Name") },
                    placeholder = { Text("e.g., IMRAN / TRUCK $nextVehicleNumber") },
                    leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_vehicle_name_input")
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            pin = input
                            error = null
                        }
                    },
                    label = { Text("4-Digit Login PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_pin_input")
                )
                if (error != null) {
                    Text(error!!, color = BrandRedBright, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = staffName.trim()
                    val v = vehicleName.trim().uppercase()
                    if (s.isEmpty()) {
                        error = "Enter staff name."
                    } else if (v.isEmpty()) {
                        error = "Enter vehicle name."
                    } else if (pin.length != 4) {
                        error = "PIN must be 4 digits."
                    } else {
                        onConfirm(s, v, pin)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("save_new_staff_button")
            ) {
                Text("Add Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PendingCustomerCard(
    row: CustomerPendingRow,
    onCollectEmpty: () -> Unit,
    modifier: Modifier = Modifier,
    showCollectAction: Boolean = true
) {
    val isPending = row.pendingEmptyBalance > 0
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sold: ${row.fullSold} • Emp Recd: ${row.emptyCollected}" +
                        (if (row.manualPendingAdded > 0) " • +Add: ${row.manualPendingAdded}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${row.primaryVehicleName} • Last: ${row.lastActivityDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = if (isPending) BrandRedContainer else BrandGreenContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPending) "Pending: ${row.pendingEmptyBalance}" else "Settled (${row.pendingEmptyBalance})",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = if (isPending) BrandRedOnContainer else BrandGreenOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (showCollectAction) {
                    OutlinedButton(
                        onClick = onCollectEmpty,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Recycling, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Collect Empty", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun OpeningStockEditDialog(
    title: String,
    productName: String,
    initialFull: Int,
    initialEmpty: Int,
    onDismiss: () -> Unit,
    onConfirm: (full: Int, empty: Int) -> Unit
) {
    var fullText by remember { mutableStateOf(initialFull.toString()) }
    var emptyText by remember { mutableStateOf(initialEmpty.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = fullText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) fullText = it },
                    label = { Text("Opening Full Cylinders") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("opening_full_input")
                )
                OutlinedTextField(
                    value = emptyText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) emptyText = it },
                    label = { Text("Opening Empty Cylinders") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("opening_empty_input")
                )
                if (errorText != null) {
                    Text(errorText!!, color = BrandRedBright, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = fullText.toIntOrNull()
                    val e = emptyText.toIntOrNull()
                    if (f == null || e == null || f < 0 || e < 0) {
                        errorText = "Enter valid non-negative integers."
                    } else {
                        onConfirm(f, e)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("opening_save_button")
            ) {
                Text("Update Opening")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InsufficientStockWarningDialog(
    alert: WarningAlert,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = BrandRedSecondary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = alert.title,
                color = BrandRedSecondary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                modifier = Modifier.testTag("warning_ok_button")
            ) {
                Text("Understood")
            }
        }
    )
}

fun formatDisplayDate(isoDate: String): String {
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outFmt = SimpleDateFormat("dd MMM yyyy (EEE)", Locale.US)
        val d = inFmt.parse(isoDate) ?: return isoDate
        outFmt.format(d)
    } catch (_: Exception) {
        isoDate
    }
}

fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = fmt.parse(isoDate) ?: Date()
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        fmt.format(cal.time)
    } catch (_: Exception) {
        isoDate
    }
}

fun parseIsoToUtcMillis(isoDate: String): Long {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        fmt.parse(isoDate)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

fun utcMillisToIso(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCalculatorDialog(
    products: List<ProductEntity>,
    storeFullProvider: (Long) -> Int = { 0 },
    storeEmptyProvider: (Long) -> Int = { 0 },
    vehicleName: String? = null,
    vehicleFullProvider: ((Long) -> Int)? = null,
    vehicleEmptyProvider: ((Long) -> Int)? = null,
    onDismiss: () -> Unit
) {
    val calcModes = listOf(
        "Cust Sale + Empty",
        "New Cust Full Sale",
        "Cust Complaint Repl",
        "Supplier Purchase",
        "Supplier Complaint",
        "Dispatch to Vehicle"
    )
    var selectedModeIdx by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf(ProductCategory.INDUSTRIAL_GAS) }
    val filteredProducts = remember(products, selectedCategory) {
        products.filter { it.category == selectedCategory }
    }
    var selectedProduct by remember(filteredProducts) {
        mutableStateOf(filteredProducts.firstOrNull())
    }
    var productExpanded by remember { mutableStateOf(false) }

    var qtyText by remember { mutableStateOf("5") }
    var emptyNowText by remember { mutableStateOf("3") }
    var prevPendingText by remember { mutableStateOf("0") }

    val qty = qtyText.toIntOrNull() ?: 0
    val empNow = emptyNowText.toIntOrNull() ?: 0
    val prevPending = prevPendingText.toIntOrNull() ?: 0

    val prodId = selectedProduct?.id ?: 0L
    val sFull = storeFullProvider(prodId)
    val sEmpty = storeEmptyProvider(prodId)
    val vFull = vehicleFullProvider?.invoke(prodId) ?: 0
    val vEmpty = vehicleEmptyProvider?.invoke(prodId) ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Stock Calculator",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cylinder Calculator",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close calculator")
                    }
                }

                // Calculation Scenario Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    calcModes.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedModeIdx == idx,
                            onClick = { selectedModeIdx = idx },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Product Category + Picker
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                selectedProduct = products.firstOrNull { it.category == cat }
                            },
                            label = { Text(cat.shortCode, fontSize = 11.sp) }
                        )
                    }
                }

                if (filteredProducts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cylinder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            filteredProducts.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.category.shortCode} • ${prod.name}") },
                                    onClick = {
                                        selectedProduct = prod
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) qtyText = it },
                        label = { Text("Full / Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_qty_input")
                    )
                    if (selectedModeIdx == 0) {
                        OutlinedTextField(
                            value = emptyNowText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) emptyNowText = it },
                            label = { Text("Empty Recd") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calc_empty_input")
                        )
                    }
                }

                if (selectedModeIdx == 0) {
                    OutlinedTextField(
                        value = prevPendingText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) prevPendingText = it },
                        label = { Text("Previous Customer Pending Empty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Result Card
                val summaryLines: List<String> = when (selectedModeIdx) {
                    0 -> {
                        val newVFull = vFull - qty
                        val newVEmp = vEmpty + empNow
                        val newPending = prevPending + qty - empNow
                        listOfNotNull(
                            vehicleName?.let { "$it Full: $vFull -> $newVFull" },
                            vehicleName?.let { "$it Empty: $vEmpty -> $newVEmp" },
                            "Cust Pending: $prevPending + $qty - $empNow = $newPending",
                            if (vehicleFullProvider != null && newVFull < 0) "Warning: Insufficient Full Stock" else null
                        )
                    }
                    1 -> {
                        val newVFull = vFull - qty
                        listOfNotNull(
                            "New Customer Connection Sale: $qty Full",
                            vehicleName?.let { "$it Full: $vFull -> $newVFull" },
                            "Customer Pending Debt Added: 0 (Brand New Cylinder Sale)",
                            if (vehicleFullProvider != null && newVFull < 0) "Warning: Insufficient Full Stock" else null
                        )
                    }
                    2 -> {
                        val newVFull = vFull - qty
                        val newVEmp = vEmpty + qty
                        listOfNotNull(
                            "1-for-1 Customer Complaint Swap: $qty Cylinder(s)",
                            vehicleName?.let { "$it Full: $vFull -> $newVFull" },
                            vehicleName?.let { "$it Empty: $vEmpty -> $newVEmp" },
                            "Pending Empty Debt Change: 0",
                            if (vehicleFullProvider != null && newVFull < 0) "Warning: Insufficient Full Stock" else null
                        )
                    }
                    3 -> {
                        listOf(
                            "Store Purchase Add: +$qty Full",
                            "Store Full: $sFull -> ${sFull + qty}",
                            "Store Empty: $sEmpty (unchanged)"
                        )
                    }
                    4 -> {
                        val newSEmp = sEmpty - qty
                        listOfNotNull(
                            "Supplier Complaint Swap: $qty Defective Empty -> Fresh Full",
                            "Store Full: $sFull -> ${sFull + qty}",
                            "Store Empty: $sEmpty -> $newSEmp",
                            if (newSEmp < 0) "Warning: Insufficient Empty Stock in Store" else null
                        )
                    }
                    else -> {
                        val newSFull = sFull - qty
                        val newVFull = vFull + qty
                        listOfNotNull(
                            "Store Dispatch to ${vehicleName ?: "Vehicle"}: $qty Full",
                            "Store Full: $sFull -> $newSFull",
                            vehicleName?.let { "$it Full: $vFull -> $newVFull" },
                            if (newSFull < 0) "Warning: Insufficient Store Full Stock" else null
                        )
                    }
                }

                Surface(
                    color = BrandGreenContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CALCULATED RESULT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenOnContainer
                        )
                        summaryLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                                color = if (line.startsWith("Warning")) BrandRedBright else BrandGreenOnContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
