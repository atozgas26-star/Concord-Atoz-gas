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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BatchProductEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun BrandHeaderBanner(
    title: String,
    subtitle: String,
    headerGradient: List<Color>? = null,
    accentBarColor: Color? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val gradientColors = headerGradient ?: listOf(
        com.example.ui.theme.ObsidianMidnight,
        com.example.ui.theme.ObsidianDeep,
        com.example.ui.theme.RoyalBlueDeep
    )
    val contentColor = com.example.ui.theme.ArcticWhite
    val accentLine = accentBarColor ?: com.example.ui.theme.MetallicOrangeBright

    val context = androidx.compose.ui.platform.LocalContext.current
    val emblemBitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        context
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_ca_red_logo,
                    opts
                )?.asImageBitmap()
            }.getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(horizontal = 16.dp, vertical = 13.dp)
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
                        color = com.example.ui.theme.ObsidianMidnight,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    com.example.ui.theme.PlatinumSilver.copy(alpha = 0.7f),
                                    accentLine.copy(alpha = 0.85f)
                                )
                            )
                        ),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (emblemBitmap != null) {
                            Image(
                                bitmap = emblemBitmap!!,
                                contentDescription = "C&A Gas Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "C&A",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.PlatinumSilver.copy(alpha = 0.88f),
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
                .height(2.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            com.example.ui.theme.ElectricBlueBright,
                            accentLine,
                            com.example.ui.theme.MetallicOrangeGold.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorBar(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    midnightSurface: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current || midnightSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isDark) com.example.ui.theme.ObsidianSurface else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDark) 2.dp else 1.dp,
        shadowElevation = if (isDark) 5.dp else 1.dp,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) com.example.ui.theme.ObsidianBorder else com.example.ui.theme.PlatinumSilver
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onDateSelected(shiftIsoDate(selectedDate, -1)) },
                modifier = Modifier.testTag("date_prev_button")
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = if (isDark) com.example.ui.theme.ElectricBlueGlow else MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) com.example.ui.theme.ObsidianElevated
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                border = if (isDark) BorderStroke(
                    0.8.dp,
                    com.example.ui.theme.ElectricBlueBright.copy(alpha = 0.35f)
                ) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPicker = true }
                    .testTag("date_picker_trigger")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select date",
                        tint = if (isDark) com.example.ui.theme.MetallicOrangeBright else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatDisplayDate(selectedDate),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) com.example.ui.theme.ArcticWhite else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(
                onClick = { onDateSelected(shiftIsoDate(selectedDate, 1)) },
                modifier = Modifier.testTag("date_next_button")
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (isDark) com.example.ui.theme.ElectricBlueGlow else MaterialTheme.colorScheme.primary
                )
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
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDark) 2.dp else 1.dp,
        shadowElevation = if (isDark) 4.dp else 2.dp,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = if (isDark) 0.38f else 0.25f),
                    MaterialTheme.colorScheme.outlineVariant
                )
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.2f))
                        )
                    )
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerColor.copy(alpha = if (isDark) 0.25f else 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isDark) accentColor else onContainerColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val numericValue = value.toIntOrNull()
                if (numericValue != null) {
                    AnimatedStockCounter(
                        count = numericValue,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.height(2.dp))
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
}

@Composable
fun StockPill(
    label: String,
    value: Int,
    isFull: Boolean,
    forceMidnight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current || forceMidnight
    val bg = when {
        isDark && isFull -> com.example.ui.theme.ElectricBlueBright.copy(alpha = 0.2f)
        isDark && !isFull -> com.example.ui.theme.MetallicOrange.copy(alpha = 0.22f)
        isFull -> BrandGreenContainer
        else -> com.example.ui.theme.MetallicOrangeSoft
    }
    val fg = when {
        isDark && isFull -> com.example.ui.theme.ElectricBlueGlow
        isDark && !isFull -> com.example.ui.theme.MetallicOrangeBright
        isFull -> BrandGreenOnContainer
        else -> com.example.ui.theme.MetallicOrangeOnSoft
    }
    val border = when {
        isDark && isFull -> com.example.ui.theme.ElectricBlueBright.copy(alpha = 0.48f)
        isDark && !isFull -> com.example.ui.theme.MetallicOrangeBright.copy(alpha = 0.52f)
        isFull -> BrandGreenPrimary.copy(alpha = 0.32f)
        else -> com.example.ui.theme.MetallicOrange.copy(alpha = 0.35f)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(0.9.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = if (isDark) com.example.ui.theme.ArcticWhite else fg
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
            label = { Text("All Gases", fontWeight = FontWeight.SemiBold) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        ProductCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName, fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
    onConfirmBatch: ((
        vehicleId: Long?,
        items: List<BatchProductEntry>,
        notes: String,
        purchaseCompanyName: String,
        customerName: String
    ) -> Unit)? = null,
    onConfirm: (
        productId: Long,
        vehicleId: Long?,
        quantity: Int,
        notes: String,
        purchaseCompanyName: String,
        customerName: String,
        emptyCollectedNow: Int
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    // Multi-product quantities persisted across ALL 4 categories
    val qtyByProductId = remember {
        mutableStateMapOf<Long, String>().apply {
            if (initialQuantity > 1 && products.isNotEmpty()) {
                put(products.first().id, initialQuantity.toString())
            }
        }
    }
    val emptyNowByProductId = remember { mutableStateMapOf<Long, String>() }

    var selectedCategoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var productSearchQuery by remember { mutableStateOf("") }

    var selectedVehicle by remember(fixedVehicle, vehicles) {
        mutableStateOf(fixedVehicle ?: vehicles.firstOrNull())
    }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }

    var purchaseCompanyName by remember { mutableStateOf("") }
    var customerName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var notes by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val sortedProducts = remember(products) {
        products.sortedWith(compareBy({ it.category.sortOrder }, { it.sortOrder }, { it.name }))
    }

    val visibleProducts = remember(sortedProducts, selectedCategoryFilter, productSearchQuery) {
        sortedProducts.filter { p ->
            (selectedCategoryFilter == null || p.category == selectedCategoryFilter) &&
                (productSearchQuery.isBlank() ||
                    p.name.contains(productSearchQuery, ignoreCase = true) ||
                    p.category.displayName.contains(productSearchQuery, ignoreCase = true) ||
                    p.category.shortCode.contains(productSearchQuery, ignoreCase = true))
        }
    }

    val selectedEntries = sortedProducts.mapNotNull { prod ->
        val q = qtyByProductId[prod.id]?.toIntOrNull() ?: 0
        if (q > 0) {
            val empNow = if (showEmptyCollectedNowField) (emptyNowByProductId[prod.id]?.toIntOrNull() ?: 0) else 0
            BatchProductEntry(
                productId = prod.id,
                quantity = q,
                emptyCollectedNow = empNow
            )
        } else null
    }
    val totalSelectedQty = selectedEntries.sumOf { it.quantity }
    val activeVehicleId = (fixedVehicle ?: selectedVehicle)?.id

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 580.dp)
                .heightIn(max = 720.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SignatureAccentHairline(height = 2.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${transactionType.label} • Multi-Category Entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Vehicle selector if applicable
                if (requireVehicleSelection && fixedVehicle == null && vehicles.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
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
                    Spacer(Modifier.height(4.dp))
                    val isDarkVehBanner = LocalIsDarkTheme.current
                    Surface(
                        color = if (isDarkVehBanner) com.example.ui.theme.DarkGreenContainer else BrandGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isDarkVehBanner) BorderStroke(0.8.dp, com.example.ui.theme.ElectricBlueBright.copy(alpha = 0.42f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Vehicle: ${fixedVehicle.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkVehBanner) com.example.ui.theme.DarkGreenOnContainer else BrandGreenOnContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Selected summary bar across ALL 4 categories
                val isDarkSummary = LocalIsDarkTheme.current
                Surface(
                    color = when {
                        selectedEntries.isNotEmpty() && isDarkSummary -> com.example.ui.theme.DarkGreenContainer
                        selectedEntries.isNotEmpty() -> BrandGreenContainer.copy(alpha = 0.72f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            selectedEntries.isNotEmpty() && isDarkSummary -> com.example.ui.theme.ElectricBlueBright.copy(alpha = 0.55f)
                            selectedEntries.isNotEmpty() -> BrandGreenPrimary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_multi_summary_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (selectedEntries.isNotEmpty()) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (selectedEntries.isEmpty()) {
                                    "Enter qty on any products across all 4 categories"
                                } else {
                                    "${selectedEntries.size} products selected • Total Qty: $totalSelectedQty"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    selectedEntries.isNotEmpty() && isDarkSummary -> com.example.ui.theme.ArcticWhite
                                    selectedEntries.isNotEmpty() -> BrandGreenOnContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (selectedEntries.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    qtyByProductId.clear()
                                    emptyNowByProductId.clear()
                                    localError = null
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Category folder chips (All + 4 Categories with selected-item count badge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = {
                            Text(
                                text = if (selectedEntries.isEmpty()) "All (4 Folders)" else "All (${selectedEntries.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("tx_cat_chip_all")
                    )
                    ProductCategory.entries.forEach { cat ->
                        val catStyle = categoryVisualStyle(cat)
                        val catSelectedCount = sortedProducts.count {
                            it.category == cat && (qtyByProductId[it.id]?.toIntOrNull() ?: 0) > 0
                        }
                        val isSel = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedCategoryFilter = if (isSel) null else cat
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (catSelectedCount > 0) "${cat.shortCode} ($catSelectedCount)" else cat.shortCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catStyle.primaryColor,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("tx_cat_chip_${cat.name.lowercase()}")
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Quick product filter search
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text("Quick search product (e.g. Oxygen, 48 CFT, LPG, MCG)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (productSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { productSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_product_search_input")
                )

                Spacer(Modifier.height(6.dp))

                // Scrollable body: Multi-product list + Form metadata fields
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Product selection list (bounded LazyColumn so user can scroll many products cleanly)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 260.dp)
                    ) {
                        if (visibleProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching products.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val groupedVisible = visibleProducts.groupBy { it.category }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedVisible.forEach { (category, catProducts) ->
                                    val catStyle = categoryVisualStyle(category)
                                    item(key = "tx_hdr_${category.name}") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(catStyle.primaryColor)
                                            )
                                            Text(
                                                text = category.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = catStyle.primaryColor
                                            )
                                        }
                                    }
                                    items(catProducts, key = { it.id }) { product ->
                                        val qtyStr = qtyByProductId[product.id].orEmpty()
                                        val parsedQty = qtyStr.toIntOrNull() ?: 0
                                        val emptyStr = emptyNowByProductId[product.id].orEmpty()
                                        val parsedEmpNow = if (showEmptyCollectedNowField) (emptyStr.toIntOrNull() ?: 0) else 0
                                        val stockHint = availableStockHint?.invoke(product.id, activeVehicleId)
                                        val calcPreview = if (parsedQty > 0) {
                                            liveCalculationPreview?.invoke(product.id, activeVehicleId, parsedQty, parsedEmpNow)
                                        } else null
                                        val isInsufficient = calcPreview?.contains("Insufficient", ignoreCase = true) == true ||
                                            (calcPreview?.contains("-> -") == true)

                                        BatchProductQtyRow(
                                            product = product,
                                            categoryColor = catStyle.primaryColor,
                                            qtyText = qtyStr,
                                            stockHint = stockHint,
                                            calcPreview = calcPreview,
                                            isInsufficient = isInsufficient,
                                            showEmptyCollectedNow = showEmptyCollectedNowField && parsedQty > 0,
                                            emptyCollectedText = emptyStr,
                                            onQtyChange = { nextText ->
                                                localError = null
                                                val clean = nextText.filter { it.isDigit() }.take(5)
                                                if (clean.isEmpty() || clean == "0") {
                                                    qtyByProductId.remove(product.id)
                                                } else {
                                                    qtyByProductId[product.id] = clean
                                                }
                                            },
                                            onEmptyCollectedChange = { nextEmp ->
                                                localError = null
                                                val clean = nextEmp.filter { it.isDigit() }.take(5)
                                                if (clean.isEmpty() || clean == "0") {
                                                    emptyNowByProductId.remove(product.id)
                                                } else {
                                                    emptyNowByProductId[product.id] = clean
                                                }
                                            },
                                            testTagPrefix = "tx_prod_${product.id}"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected items compact breakdown chips
                    if (selectedEntries.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedEntries.forEach { entry ->
                                val prod = sortedProducts.firstOrNull { it.id == entry.productId }
                                if (prod != null) {
                                    val catColor = categoryVisualStyle(prod.category).primaryColor
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "${prod.category.shortCode} ${prod.name}: ${entry.quantity}" +
                                                if (showEmptyCollectedNowField && entry.emptyCollectedNow > 0) " (Emp:${entry.emptyCollectedNow})" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = catColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showPurchaseCompanyField) {
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
                                Icon(Icons.Default.Business, contentDescription = "Purchase Company")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_purchase_company_input")
                        )
                        if (recentPurchaseCompanies.isNotEmpty()) {
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
                                Icon(Icons.Default.Person, contentDescription = "Customer Name")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_customer_name_input")
                        )
                        if (recentCustomerNames.isNotEmpty()) {
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

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Invoice / Remarks (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_notes_input")
                    )
                }

                if (localError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = localError!!,
                        color = BrandRedBright,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                // ONE SAVE ENTRY Button pinned cleanly at the bottom
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
                            val cleanCompany = purchaseCompanyName.trim()
                            val cleanCustomer = customerName.trim()
                            val targetVehicleId = if (requireVehicleSelection) selectedVehicle?.id else fixedVehicle?.id
                            when {
                                selectedEntries.isEmpty() -> {
                                    localError = "Enter quantity (> 0) for at least one product."
                                }
                                requireVehicleSelection && targetVehicleId == null -> {
                                    localError = "Please select a vehicle."
                                }
                                showPurchaseCompanyField && cleanCompany.isEmpty() -> {
                                    localError = "Please enter the purchase company name."
                                }
                                requireCustomerName && cleanCustomer.isEmpty() -> {
                                    localError = "Please enter the customer name."
                                }
                                else -> {
                                    val finalCompany = if (showPurchaseCompanyField) cleanCompany else ""
                                    val finalCustomer = if (showCustomerNameField) cleanCustomer else ""
                                    val paramVehicleId = if (requireVehicleSelection) selectedVehicle?.id else null
                                    if (onConfirmBatch != null) {
                                        onConfirmBatch(paramVehicleId, selectedEntries, notes, finalCompany, finalCustomer)
                                    } else {
                                        selectedEntries.forEach { entry ->
                                            onConfirm(
                                                entry.productId,
                                                paramVehicleId,
                                                entry.quantity,
                                                notes,
                                                finalCompany,
                                                finalCustomer,
                                                entry.emptyCollectedNow
                                            )
                                        }
                                    }
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("tx_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                    ) {
                        Text(
                            text = if (selectedEntries.size > 1) {
                                "SAVE ENTRY (${selectedEntries.size})"
                            } else {
                                "SAVE ENTRY"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun BatchProductQtyRow(
    product: ProductEntity,
    categoryColor: Color,
    qtyText: String,
    stockHint: String?,
    calcPreview: String?,
    isInsufficient: Boolean,
    showEmptyCollectedNow: Boolean,
    emptyCollectedText: String,
    onQtyChange: (String) -> Unit,
    onEmptyCollectedChange: (String) -> Unit,
    testTagPrefix: String
) {
    val parsedQty = qtyText.toIntOrNull() ?: 0
    val isActive = parsedQty > 0
    val isDarkRow = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isInsufficient && isDarkRow -> com.example.ui.theme.DarkRedContainer
            isInsufficient -> BrandRedContainer.copy(alpha = 0.45f)
            isActive && isDarkRow -> com.example.ui.theme.DarkGreenContainer
            isActive -> BrandGreenContainer.copy(alpha = 0.42f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isActive) 1.4.dp else 1.dp,
            color = when {
                isInsufficient -> BrandRedSecondary
                isActive && isDarkRow -> com.example.ui.theme.ElectricBlueBright
                isActive -> BrandGreenPrimary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagPrefix)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = categoryColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = product.category.shortCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!stockHint.isNullOrBlank()) {
                        Text(
                            text = stockHint,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Inline - [Qty] + stepper
                CompactInlineStepper(
                    valueText = qtyText,
                    onValueChange = onQtyChange,
                    testTagPrefix = "${testTagPrefix}_qty"
                )
            }

            if (showEmptyCollectedNow) {
                val parsedEmp = emptyCollectedText.toIntOrNull() ?: 0
                val pendingDiff = parsedQty - parsedEmp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Empty Recd Now",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                pendingDiff > 0 -> "Pending: +$pendingDiff"
                                pendingDiff == 0 -> "Settled (0 Pending)"
                                else -> "Extra ${-pendingDiff} settled"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = when {
                                pendingDiff > 0 && isDarkRow -> com.example.ui.theme.DarkRedOnContainer
                                pendingDiff > 0 -> BrandRedOnContainer
                                isDarkRow -> com.example.ui.theme.ElectricBlueGlow
                                else -> BrandGreenOnContainer
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    CompactInlineStepper(
                        valueText = emptyCollectedText,
                        onValueChange = onEmptyCollectedChange,
                        testTagPrefix = "${testTagPrefix}_empty"
                    )
                }
            }

            if (!calcPreview.isNullOrBlank()) {
                Text(
                    text = calcPreview,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isInsufficient && isDarkRow -> com.example.ui.theme.DarkRedOnContainer
                        isInsufficient -> BrandRedOnContainer
                        isDarkRow -> com.example.ui.theme.ElectricBlueGlow
                        else -> BrandGreenOnContainer
                    }
                )
            }
        }
    }
}

@Composable
fun CompactInlineStepper(
    valueText: String,
    onValueChange: (String) -> Unit,
    testTagPrefix: String
) {
    val currentVal = valueText.toIntOrNull() ?: 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable {
                    val next = (currentVal - 1).coerceAtLeast(0)
                    onValueChange(if (next == 0) "" else next.toString())
                }
                .testTag("${testTagPrefix}_minus")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease quantity",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .width(44.dp)
                .height(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (valueText.isEmpty()) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = valueText,
                    onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(5)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .testTag("${testTagPrefix}_input")
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(7.dp),
            color = BrandGreenPrimary.copy(alpha = 0.16f),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable {
                    onValueChange((currentVal + 1).toString())
                }
                .testTag("${testTagPrefix}_plus")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase quantity",
                    tint = BrandGreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
        shape = RoundedCornerShape(24.dp),
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
    LuxuryGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        accentGlow = if (isPending) BrandRedSecondary else BrandGreenPrimary
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
            val isDarkPending = LocalIsDarkTheme.current
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = when {
                        isPending && isDarkPending -> com.example.ui.theme.DarkRedContainer
                        isPending -> BrandRedContainer
                        isDarkPending -> com.example.ui.theme.DarkGreenContainer
                        else -> BrandGreenContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPending) "Pending: ${row.pendingEmptyBalance}" else "Settled (${row.pendingEmptyBalance})",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isPending && isDarkPending -> com.example.ui.theme.DarkRedOnContainer
                            isPending -> BrandRedOnContainer
                            isDarkPending -> com.example.ui.theme.DarkGreenOnContainer
                            else -> BrandGreenOnContainer
                        },
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
        shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(24.dp),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
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
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = com.example.ui.theme.MetallicOrange,
                        border = BorderStroke(1.dp, com.example.ui.theme.MetallicOrangeGold.copy(alpha = 0.85f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("stock_calc_close_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close calculator",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CLOSE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
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
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { productExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("stock_calc_product_selector")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cylinder",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedProduct?.name ?: "Select Product",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select cylinder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DropdownMenu(
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

                val isDarkCalcRes = LocalIsDarkTheme.current
                Surface(
                    color = if (isDarkCalcRes) com.example.ui.theme.DarkGreenContainer else BrandGreenContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkCalcRes) com.example.ui.theme.ElectricBlueBright else BrandGreenPrimary
                    ),
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
                            color = if (isDarkCalcRes) com.example.ui.theme.ElectricBlueGlow else BrandGreenOnContainer
                        )
                        summaryLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                                color = when {
                                    line.startsWith("Warning") -> BrandRedBright
                                    isDarkCalcRes -> com.example.ui.theme.ArcticWhite
                                    else -> BrandGreenOnContainer
                                },
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
}
