package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ProductCategory
import com.example.data.model.VehicleStockRow
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.DarkGreenContainer
import com.example.ui.theme.DarkRedContainer
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.RoyalElectricBlue

/**
 * Interactive Vehicle-to-Vehicle cylinder transfer dialog allowing Staff or Admin
 * to move Full cylinders, Empty cylinders, or both from [sourceVehicle] to another vehicle.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleTransferDialog(
    sourceVehicle: VehicleEntity,
    allVehicles: List<VehicleEntity>,
    products: List<ProductEntity>,
    sourceVehicleRows: List<VehicleStockRow>,
    onDismiss: () -> Unit,
    onConfirmTransfer: (
        targetVehicleId: Long,
        productId: Long,
        fullQuantity: Int,
        emptyQuantity: Int,
        extraFullQuantity: Int,
        extraEmptyQuantity: Int,
        notes: String
    ) -> Unit
) {
    val destinationVehicles = remember(allVehicles, sourceVehicle.id) {
        allVehicles.filter { it.id != sourceVehicle.id }
    }
    var selectedTargetVehicle by remember(destinationVehicles) {
        mutableStateOf(destinationVehicles.firstOrNull())
    }
    var selectedCategory by remember { mutableStateOf(ProductCategory.INDUSTRIAL_GAS) }
    val categoryProducts = remember(products, selectedCategory) {
        products.filter { it.category == selectedCategory }
    }
    var selectedProduct by remember(categoryProducts) { mutableStateOf(categoryProducts.firstOrNull()) }
    var productExpanded by remember { mutableStateOf(false) }
    var fullQtyText by remember { mutableStateOf("") }
    var emptyQtyText by remember { mutableStateOf("") }
    var extraFullQtyText by remember { mutableStateOf("") }
    var extraEmptyQtyText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val activeRow = remember(sourceVehicleRows, selectedProduct) {
        selectedProduct?.let { prod -> sourceVehicleRows.firstOrNull { it.product.id == prod.id } }
    }
    val availableFull = activeRow?.fullBalance ?: 0
    val availableEmpty = activeRow?.emptyBalance ?: 0

    val fullQty = fullQtyText.toIntOrNull() ?: 0
    val emptyQty = emptyQtyText.toIntOrNull() ?: 0
    val extraFullQty = extraFullQtyText.toIntOrNull() ?: 0
    val extraEmptyQty = extraEmptyQtyText.toIntOrNull() ?: 0
    val totalFull = fullQty + extraFullQty
    val totalEmpty = emptyQty + extraEmptyQty
    val grandTotal = totalFull + totalEmpty
    val hasExtra = extraFullQty > 0 || extraEmptyQty > 0

    val hasValidQty = grandTotal > 0 && fullQty >= 0 && emptyQty >= 0 && extraFullQty >= 0 && extraEmptyQty >= 0
    val exceedsFull = totalFull > availableFull
    val exceedsEmpty = totalEmpty > availableEmpty
    val canSubmit = selectedTargetVehicle != null &&
        selectedProduct != null &&
        hasValidQty &&
        !exceedsFull &&
        !exceedsEmpty

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Vehicle to Vehicle Transfer",
                tint = RoyalElectricBlue,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Vehicle → Vehicle Transfer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Source Vehicle Banner
                val isDarkTransfer = LocalIsDarkTheme.current
                Surface(
                    color = if (isDarkTransfer) DarkGreenContainer else BrandGreenContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (isDarkTransfer) ElectricBlueGlow else BrandGreenOnContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "From Vehicle: ${sourceVehicle.name}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTransfer) ArcticWhite else BrandGreenOnContainer
                        )
                    }
                }

                // Destination Vehicle Chips
                Text(
                    text = "Select Destination Vehicle",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (destinationVehicles.isEmpty()) {
                    Text(
                        text = "No other vehicles available to transfer cylinders to.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandRedSecondary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        destinationVehicles.forEach { vehicle ->
                            val isSelected = selectedTargetVehicle?.id == vehicle.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTargetVehicle = vehicle },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = vehicle.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGreenPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("transfer_target_${vehicle.name}")
                            )
                        }
                    }
                }

                // Category Folder Selector so products are grouped inside folders
                Text(
                    text = "Select Category Folder",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProductCategory.activeEntries.forEach { cat ->
                        val catStyle = categoryVisualStyle(cat)
                        val isCatSelected = selectedCategory == cat
                        FilterChip(
                            selected = isCatSelected,
                            onClick = {
                                selectedCategory = cat
                                selectedProduct = products.firstOrNull { it.category == cat }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = catStyle.shortTitle,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catStyle.primaryColor,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("transfer_folder_${cat.name.lowercase()}")
                        )
                    }
                }

                // Product Selector Dropdown (scoped to opened category folder)
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.category.shortCode} • ${it.name}" } ?: "Select gas cylinder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gas Cylinder (${categoryVisualStyle(selectedCategory).shortTitle})") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("transfer_product_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        categoryProducts.forEach { product ->
                            val row = sourceVehicleRows.firstOrNull { it.product.id == product.id }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${product.category.shortCode} • ${product.name}",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Full: ${row?.fullBalance ?: 0}  •  Empty: ${row?.emptyBalance ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedProduct = product
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                // Live Available Balance Pill Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isDarkTransfer) DarkGreenContainer else BrandGreenContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isDarkTransfer) BorderStroke(0.8.dp, ElectricBlueBright.copy(alpha = 0.4f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AVAILABLE FULL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTransfer) ElectricBlueGlow else BrandGreenOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = availableFull.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTransfer) ArcticWhite else BrandGreenOnContainer
                            )
                        }
                    }
                    Surface(
                        color = if (isDarkTransfer) DarkRedContainer else BrandRedContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isDarkTransfer) BorderStroke(0.8.dp, MetallicOrangeBright.copy(alpha = 0.4f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AVAILABLE EMPTY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTransfer) MetallicOrangeBright else BrandRedOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = availableEmpty.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTransfer) ArcticWhite else BrandRedOnContainer
                            )
                        }
                    }
                }

                // Standard Full & Empty Quantity Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fullQtyText,
                        onValueChange = { raw ->
                            fullQtyText = raw.filter { it.isDigit() }.take(5)
                        },
                        label = { Text("Full Qty") },
                        placeholder = { Text("0") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PropaneTank,
                                contentDescription = null,
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        isError = exceedsFull,
                        supportingText = if (exceedsFull) {
                            { Text("Max $availableFull") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_full_qty_input")
                    )
                    OutlinedTextField(
                        value = emptyQtyText,
                        onValueChange = { raw ->
                            emptyQtyText = raw.filter { it.isDigit() }.take(5)
                        },
                        label = { Text("Empty Qty") },
                        placeholder = { Text("0") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Recycling,
                                contentDescription = null,
                                tint = BrandRedSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        isError = exceedsEmpty,
                        supportingText = if (exceedsEmpty) {
                            { Text("Max $availableEmpty") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_empty_qty_input")
                    )
                }

                // Optional Extra Full & Extra Empty Collect (Auto-Summed)
                Text(
                    text = "Extra Cylinders Collected (Optional — Auto Sum)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = extraFullQtyText,
                        onValueChange = { raw ->
                            extraFullQtyText = raw.filter { it.isDigit() }.take(5)
                        },
                        label = { Text("+Extra Full") },
                        placeholder = { Text("0") },
                        isError = exceedsFull,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_extra_full_qty_input")
                    )
                    OutlinedTextField(
                        value = extraEmptyQtyText,
                        onValueChange = { raw ->
                            extraEmptyQtyText = raw.filter { it.isDigit() }.take(5)
                        },
                        label = { Text("+Extra Empty") },
                        placeholder = { Text("0") },
                        isError = exceedsEmpty,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_extra_empty_qty_input")
                    )
                }

                // Live Auto-Sum Preview + Auto Destination Staff Bell Alert Info
                if (hasValidQty && selectedTargetVehicle != null && selectedProduct != null) {
                    Surface(
                        color = when {
                            hasExtra && isDarkTransfer -> DarkRedContainer
                            hasExtra -> BrandRedContainer.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hasExtra) BrandRedSecondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_auto_sum_preview")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${sourceVehicle.name} → ${selectedTargetVehicle!!.name}  •  Auto-Sum Total: $grandTotal",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    hasExtra && isDarkTransfer -> MetallicOrangeBright
                                    hasExtra -> BrandRedSecondary
                                    isDarkTransfer -> ElectricBlueGlow
                                    else -> BrandGreenPrimary
                                }
                            )
                            if (totalFull > 0) {
                                val fullDetail = if (extraFullQty > 0) "$fullQty + Extra $extraFullQty = $totalFull" else "$totalFull"
                                Text(
                                    text = "Full ($fullDetail): $availableFull → ${availableFull - totalFull} (Target +$totalFull)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            if (totalEmpty > 0) {
                                val emptyDetail = if (extraEmptyQty > 0) "$emptyQty + Extra $extraEmptyQty = $totalEmpty" else "$totalEmpty"
                                Text(
                                    text = "Empty ($emptyDetail): $availableEmpty → ${availableEmpty - totalEmpty} (Target +$totalEmpty)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            Text(
                                text = if (hasExtra) {
                                    "Bell Alert + Reminder Sound will notify ${selectedTargetVehicle!!.name} staff & Admin about extra cylinders."
                                } else {
                                    "Bell notification will alert ${selectedTargetVehicle!!.name} staff automatically."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it.take(180) },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Route handoff / extra cylinder collect") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedTargetVehicle ?: return@Button
                    val product = selectedProduct ?: return@Button
                    onConfirmTransfer(
                        target.id,
                        product.id,
                        fullQty,
                        emptyQty,
                        extraFullQty,
                        extraEmptyQty,
                        notesText.trim()
                    )
                    onDismiss()
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("confirm_vehicle_transfer_button")
            ) {
                Text(if (hasExtra) "Confirm (+Extra)" else "Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
