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
import com.example.data.local.VehicleEntity
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(BrandGreenDark, BrandGreenPrimary)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
        // Signature corporate Red accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BrandRedSecondary)
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
    availableStockHint: ((productId: Long, vehicleId: Long?) -> String?)? = null,
    onDismiss: () -> Unit,
    onConfirm: (productId: Long, vehicleId: Long?, quantity: Int, notes: String) -> Unit
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

    var quantityText by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

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
                        color = MaterialTheme.colorScheme.primary
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
                    availableStockHint?.invoke(prod.id, selectedVehicle?.id)
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

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Customer / Invoice (Optional)") },
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
                            if (prod == null) {
                                localError = "Please select a product."
                            } else if (qty <= 0) {
                                localError = "Enter quantity greater than 0."
                            } else {
                                onConfirm(
                                    prod.id,
                                    if (requireVehicleSelection) selectedVehicle?.id else null,
                                    qty,
                                    notes
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
