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
import com.example.data.model.VehicleStockRow
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono

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
        notes: String
    ) -> Unit
) {
    val destinationVehicles = remember(allVehicles, sourceVehicle.id) {
        allVehicles.filter { it.id != sourceVehicle.id }
    }
    var selectedTargetVehicle by remember(destinationVehicles) {
        mutableStateOf(destinationVehicles.firstOrNull())
    }
    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var productExpanded by remember { mutableStateOf(false) }
    var fullQtyText by remember { mutableStateOf("") }
    var emptyQtyText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val activeRow = remember(sourceVehicleRows, selectedProduct) {
        selectedProduct?.let { prod -> sourceVehicleRows.firstOrNull { it.product.id == prod.id } }
    }
    val availableFull = activeRow?.fullBalance ?: 0
    val availableEmpty = activeRow?.emptyBalance ?: 0

    val fullQty = fullQtyText.toIntOrNull() ?: 0
    val emptyQty = emptyQtyText.toIntOrNull() ?: 0
    val hasValidQty = (fullQty > 0 || emptyQty > 0) && fullQty >= 0 && emptyQty >= 0
    val exceedsFull = fullQty > availableFull
    val exceedsEmpty = emptyQty > availableEmpty
    val canSubmit = selectedTargetVehicle != null &&
        selectedProduct != null &&
        hasValidQty &&
        !exceedsFull &&
        !exceedsEmpty

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Vehicle to Vehicle Transfer",
                tint = BrandGreenPrimary,
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
                Surface(
                    color = BrandGreenContainer,
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
                            tint = BrandGreenOnContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "From Vehicle: ${sourceVehicle.name}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenOnContainer
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

                // Product Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.category.shortCode} • ${it.name}" } ?: "Select gas cylinder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gas Cylinder") },
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
                        products.forEach { product ->
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
                        color = BrandGreenContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AVAILABLE FULL",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreenOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = availableFull.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandGreenOnContainer
                            )
                        }
                    }
                    Surface(
                        color = BrandRedContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AVAILABLE EMPTY",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandRedOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = availableEmpty.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandRedOnContainer
                            )
                        }
                    }
                }

                // Full & Empty Quantity Inputs side-by-side
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

                // Live Preview
                if (hasValidQty && selectedTargetVehicle != null && selectedProduct != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${sourceVehicle.name} → ${selectedTargetVehicle!!.name}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenPrimary
                            )
                            if (fullQty > 0) {
                                Text(
                                    text = "Full: $availableFull → ${availableFull - fullQty} (Target +$fullQty)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            if (emptyQty > 0) {
                                Text(
                                    text = "Empty: $availableEmpty → ${availableEmpty - emptyQty} (Target +$emptyQty)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it.take(180) },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Route handoff / cylinder swap") },
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
                    onConfirmTransfer(target.id, product.id, fullQty, emptyQty, notesText.trim())
                    onDismiss()
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("confirm_vehicle_transfer_button")
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
