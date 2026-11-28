package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.data.local.ProductEntity
import com.example.data.model.ProductCategory
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver

private val CYLINDER_SIZE_PRESETS = listOf(
    "47L", "48 CFT", "5 KG", "11 KG", "20 KG", "22 KG", "30 KG", "44 KG", "50L"
)

/**
 * Admin-Only Product Editor Dialog supporting BOTH Add New Product and Edit Existing Product
 * (name, category, cylinder size / KG, and active/inactive toggle).
 */
@Composable
fun ProductEditorDialog(
    existingProduct: ProductEntity? = null,
    initialCategory: ProductCategory = ProductCategory.INDUSTRIAL_GAS,
    onDismiss: () -> Unit,
    onSave: (category: ProductCategory, name: String, cylinderSizeKg: String, isActive: Boolean) -> Unit
) {
    val isEdit = existingProduct != null
    var category by remember(existingProduct, initialCategory) {
        mutableStateOf(existingProduct?.category ?: initialCategory)
    }
    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name.orEmpty()) }
    var cylinderSizeKg by remember(existingProduct) { mutableStateOf(existingProduct?.cylinderSizeKg.orEmpty()) }
    var isActive by remember(existingProduct) { mutableStateOf(existingProduct?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ArcticWhite,
        unfocusedTextColor = ArcticWhite,
        focusedContainerColor = ObsidianElevated.copy(alpha = 0.85f),
        unfocusedContainerColor = ObsidianDeep.copy(alpha = 0.85f),
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
                Brush.linearGradient(listOf(ElectricBlueGlow.copy(alpha = 0.7f), MetallicOrangeBright.copy(alpha = 0.6f)))
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .testTag("product_editor_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(ObsidianMidnight, Color(0xFF0E2A66))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MetallicOrangeBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Edit Gas Product" else "Add New Gas Product",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ArcticWhite
                        )
                    }
                    Surface(shape = CircleShape, color = MetallicOrange, modifier = Modifier.size(34.dp)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("product_editor_close")) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ArcticWhite, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                SignatureAccentHairline(height = 2.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gas Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlueGlow
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProductCategory.activeEntries.forEach { cat ->
                            val isSel = category == cat
                            val catStyle = categoryVisualStyle(cat)
                            FilterChip(
                                selected = isSel,
                                onClick = { category = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianElevated,
                                    labelColor = PlatinumSilver,
                                    selectedContainerColor = catStyle.primaryColor,
                                    selectedLabelColor = ArcticWhite
                                ),
                                modifier = Modifier.testTag("product_cat_chip_${cat.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        label = { Text("Product Name *") },
                        placeholder = { Text("e.g., Oxygen, Argon Mix, CO2 30kg, LPG 22kg", color = PlatinumMuted) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_editor_name_input")
                    )

                    OutlinedTextField(
                        value = cylinderSizeKg,
                        onValueChange = { cylinderSizeKg = it.take(24) },
                        label = { Text("Cylinder Size / KG") },
                        placeholder = { Text("e.g., 47L, 48 CFT, 20 KG, 30 KG, 44 KG", color = PlatinumMuted) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_editor_size_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CYLINDER_SIZE_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = cylinderSizeKg.equals(preset, ignoreCase = true),
                                onClick = { cylinderSizeKg = preset },
                                label = { Text(preset, fontSize = 11.sp, fontFamily = JetBrainsMono) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianElevated,
                                    labelColor = PlatinumSilver,
                                    selectedContainerColor = MetallicOrange,
                                    selectedLabelColor = ArcticWhite
                                )
                            )
                        }
                    }

                    if (isEdit) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isActive) "Product Active" else "Product Inactive",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color(0xFF34D399) else MetallicOrangeBright
                                    )
                                    Text(
                                        text = "Inactive products stay hidden from new entries while preserving history.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PlatinumMuted
                                    )
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    modifier = Modifier.testTag("product_editor_active_switch")
                                )
                            }
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = PlatinumSilver)
                        }
                        Button(
                            onClick = {
                                if (name.trim().isEmpty()) {
                                    error = "Product name is required."
                                    return@Button
                                }
                                onSave(category, name.trim(), cylinderSizeKg.trim(), isActive)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicOrange),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("product_editor_save_button")
                        ) {
                            Text(if (isEdit) "Save Changes" else "Add Product", fontWeight = FontWeight.Bold, color = ArcticWhite)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog explaining safe Delete vs Inactive preservation rule.
 */
@Composable
fun ProductDeleteConfirmDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRedSecondary) },
        title = { Text("Delete \"${product.name}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Safety Rule:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• If this product has ZERO stock and ZERO transaction history, it will be permanently deleted.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• If this product has ANY existing stock or transaction history, it will be safely marked INACTIVE instead so all past records remain intact.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                modifier = Modifier.testTag("confirm_delete_product_button")
            ) {
                Text("Proceed", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Admin-Only Draggable & Reorderable Product Master Row.
 * Supports long-press vertical drag-and-drop AND one-tap Move Up / Move Down buttons,
 * plus Edit, Delete, Cylinder Size/KG badge, and Active switch.
 */
@Composable
fun AdminReorderableProductRow(
    product: ProductEntity,
    index: Int,
    totalCount: Int,
    canReorder: Boolean,
    showCategoryLabel: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragSwap: (deltaSteps: Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val catStyle = categoryVisualStyle(product.category)
    val isDark = LocalIsDarkTheme.current
    var dragOffsetY by remember(product.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(product.id) { mutableStateOf(false) }
    val stepPx = 115f

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .testTag("admin_product_row_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        accentGlow = if (isDragging) MetallicOrangeBright else if (product.isActive) catStyle.primaryColor else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (canReorder) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .pointerInput(product.id, index, totalCount) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            isDragging = true
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            if (dragOffsetY >= stepPx) {
                                                onDragSwap(1)
                                                dragOffsetY -= stepPx
                                            } else if (dragOffsetY <= -stepPx) {
                                                onDragSwap(-1)
                                                dragOffsetY += stepPx
                                            }
                                        }
                                    )
                                }
                                .testTag("product_drag_handle_${product.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Long press and drag to reorder ${product.name}",
                                tint = if (isDragging) MetallicOrangeBright else if (isDark) ElectricBlueGlow else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (product.isActive) catStyle.primaryColor else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (product.cylinderSizeKg.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) ElectricBlueBright.copy(alpha = 0.2f) else catStyle.primaryColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = product.cylinderSizeKg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) ElectricBlueGlow else catStyle.primaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (showCategoryLabel) {
                                Text(
                                    text = "• ${product.category.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catStyle.primaryColor
                                )
                            }
                            if (!product.isActive) {
                                Text(
                                    text = "• INACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MetallicOrangeBright
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = product.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("product_active_switch_${product.id}")
                )
            }

            // Clear Admin Action Row: Move Up, Move Down, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canReorder) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            IconButton(
                                onClick = onMoveUp,
                                enabled = index > 0,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("product_move_up_${product.id}")
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move ${product.name} up",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index < totalCount - 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            IconButton(
                                onClick = onMoveDown,
                                enabled = index < totalCount - 1,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("product_move_down_${product.id}")
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move ${product.name} down",
                                    tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("product_edit_button_${product.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricBlueBright)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandRedSecondary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("product_delete_button_${product.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandRedSecondary)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandRedSecondary)
                    }
                }
            }
        }
    }
}
