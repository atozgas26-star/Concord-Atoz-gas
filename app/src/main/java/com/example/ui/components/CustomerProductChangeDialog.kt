package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProductEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.CustomerChangeMode
import com.example.data.model.CustomerExchangeItem
import com.example.data.model.ProductCategory
import com.example.data.model.StoreStockRow
import com.example.data.model.VehicleStockRow
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.DarkGreenContainer
import com.example.ui.theme.DarkGreenOnContainer
import com.example.ui.theme.DarkRedContainer
import com.example.ui.theme.DarkRedOnContainer
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.RoyalElectricBlue

/**
 * Reusable Multi-Product, Multi-Category dialog for both Staff (assigned Vehicle) and Admin (Main Store or Vehicle):
 * 1. Full -> Full multiple cylinder exchange (Return multiple Full cylinders across any categories, Give multiple Full cylinders)
 * 2. Empty -> Empty multiple cylinder exchange (Return multiple Empty cylinders across any categories, Give multiple Empty cylinders)
 * 3. Return Empty multiple cylinders from customer (settles pending empty balance)
 * Saves all selected entries atomically with ONE "SAVE ENTRY" button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerProductChangeDialog(
    vehicle: VehicleEntity?,
    products: List<ProductEntity>,
    vehicleRows: List<VehicleStockRow> = emptyList(),
    storeRows: List<StoreStockRow> = emptyList(),
    recentCustomerNames: List<String> = emptyList(),
    initialMode: CustomerChangeMode = CustomerChangeMode.FULL_TO_FULL,
    onDismiss: () -> Unit,
    onConfirmMulti: ((
        mode: CustomerChangeMode,
        returnedItems: List<CustomerExchangeItem>,
        givenItems: List<CustomerExchangeItem>,
        customerName: String,
        notes: String
    ) -> Unit)? = null,
    onConfirm: (
        mode: CustomerChangeMode,
        returnedProductId: Long,
        givenProductId: Long?,
        quantity: Int,
        customerName: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    var mode by remember { mutableStateOf(initialMode) }
    val returnedQtyMap = remember { mutableStateMapOf<Long, String>() }
    val givenQtyMap = remember { mutableStateMapOf<Long, String>() }

    var retCategoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var givenCategoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var retSearchQuery by remember { mutableStateOf("") }
    var givenSearchQuery by remember { mutableStateOf("") }

    var customerName by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val sortedProducts = remember(products) {
        products.sortedWith(compareBy({ it.category.sortOrder }, { it.sortOrder }, { it.name }))
    }

    fun fullBalanceOf(productId: Long): Int {
        return if (vehicle != null) {
            vehicleRows.firstOrNull { it.product.id == productId }?.fullBalance ?: 0
        } else {
            storeRows.firstOrNull { it.product.id == productId }?.storeFullBalance ?: 0
        }
    }

    fun emptyBalanceOf(productId: Long): Int {
        return if (vehicle != null) {
            vehicleRows.firstOrNull { it.product.id == productId }?.emptyBalance ?: 0
        } else {
            storeRows.firstOrNull { it.product.id == productId }?.storeEmptyBalance ?: 0
        }
    }

    val isSwapMode = mode != CustomerChangeMode.RETURN_EMPTY

    val returnedItems = sortedProducts.mapNotNull { prod ->
        val q = returnedQtyMap[prod.id]?.toIntOrNull() ?: 0
        if (q > 0) CustomerExchangeItem(prod.id, q) else null
    }
    val givenItems = if (isSwapMode) {
        sortedProducts.mapNotNull { prod ->
            val q = givenQtyMap[prod.id]?.toIntOrNull() ?: 0
            if (q > 0) CustomerExchangeItem(prod.id, q) else null
        }
    } else {
        emptyList()
    }

    val returnedByProd = remember(returnedItems) {
        returnedItems.associate { it.productId to it.quantity }
    }

    val insufficientProducts = remember(mode, returnedByProd, givenItems, vehicleRows, storeRows) {
        if (!isSwapMode) emptyList()
        else {
            givenItems.mapNotNull { item ->
                val prod = sortedProducts.firstOrNull { it.id == item.productId } ?: return@mapNotNull null
                val baseAvail = if (mode == CustomerChangeMode.FULL_TO_FULL) {
                    fullBalanceOf(prod.id)
                } else {
                    emptyBalanceOf(prod.id)
                }
                val sameRet = returnedByProd[prod.id] ?: 0
                val effectiveAvail = baseAvail + sameRet
                if (item.quantity > effectiveAvail) {
                    "${prod.name} (Need ${item.quantity}, Avail $effectiveAvail)"
                } else null
            }
        }
    }

    val visibleRetProducts = remember(sortedProducts, retCategoryFilter, retSearchQuery) {
        sortedProducts.filter { p ->
            (retCategoryFilter == null || p.category == retCategoryFilter) &&
                (retSearchQuery.isBlank() ||
                    p.name.contains(retSearchQuery, ignoreCase = true) ||
                    p.category.displayName.contains(retSearchQuery, ignoreCase = true) ||
                    p.category.shortCode.contains(retSearchQuery, ignoreCase = true))
        }
    }

    val visibleGivenProducts = remember(sortedProducts, givenCategoryFilter, givenSearchQuery) {
        sortedProducts.filter { p ->
            (givenCategoryFilter == null || p.category == givenCategoryFilter) &&
                (givenSearchQuery.isBlank() ||
                    p.name.contains(givenSearchQuery, ignoreCase = true) ||
                    p.category.displayName.contains(givenSearchQuery, ignoreCase = true) ||
                    p.category.shortCode.contains(givenSearchQuery, ignoreCase = true))
        }
    }

    val totalRetQty = returnedItems.sumOf { it.quantity }
    val totalGivenQty = givenItems.sumOf { it.quantity }
    val hasExceedsStock = insufficientProducts.isNotEmpty()
    val canSubmit = returnedItems.isNotEmpty() &&
        (!isSwapMode || givenItems.isNotEmpty()) &&
        !hasExceedsStock

    val scopeLabel = vehicle?.let { "Vehicle: ${it.name}" } ?: "Inside Store • Main Yard"
    val unitLabel = if (mode == CustomerChangeMode.FULL_TO_FULL) "Full" else "Empty"

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
                .heightIn(max = 740.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (mode == CustomerChangeMode.RETURN_EMPTY) Icons.Default.Recycling else Icons.Default.SwapHoriz,
                            contentDescription = "Customer Product Change",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Customer Multiple Exchange",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$scopeLabel • Multi-Category Entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 3-Mode Switcher: Full -> Full | Empty -> Empty | Return Empty
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomerChangeMode.entries.forEach { entry ->
                        val selected = mode == entry
                        val chipColor = when (entry) {
                            CustomerChangeMode.FULL_TO_FULL -> BrandGreenPrimary
                            CustomerChangeMode.EMPTY_TO_EMPTY -> MetallicOrange
                            CustomerChangeMode.RETURN_EMPTY -> RoyalElectricBlue
                        }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                mode = entry
                                localError = null
                            },
                            label = {
                                Text(
                                    text = entry.shortLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("customer_change_mode_${entry.name.lowercase()}")
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Summary bar
                val isDarkDialog = LocalIsDarkTheme.current
                Surface(
                    color = when {
                        (returnedItems.isNotEmpty() || givenItems.isNotEmpty()) && isDarkDialog -> DarkGreenContainer
                        (returnedItems.isNotEmpty() || givenItems.isNotEmpty()) -> BrandGreenContainer.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            hasExceedsStock -> BrandRedSecondary
                            canSubmit && isDarkDialog -> ElectricBlueBright
                            canSubmit -> BrandGreenPrimary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_change_summary_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSwapMode) {
                                "In: ${returnedItems.size} prod ($totalRetQty)  •  Out: ${givenItems.size} prod ($totalGivenQty)"
                            } else {
                                "Return Empty: ${returnedItems.size} prod ($totalRetQty)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                hasExceedsStock && isDarkDialog -> DarkRedOnContainer
                                hasExceedsStock -> BrandRedOnContainer
                                isDarkDialog -> ArcticWhite
                                else -> BrandGreenOnContainer
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (returnedItems.isNotEmpty() || givenItems.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    returnedQtyMap.clear()
                                    givenQtyMap.clear()
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

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scope pill + Customer Name
                    Surface(
                        color = if (isDarkDialog) DarkGreenContainer else BrandGreenContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (vehicle != null) Icons.Default.LocalShipping else Icons.Default.Store,
                                contentDescription = null,
                                tint = if (isDarkDialog) ElectricBlueGlow else BrandGreenOnContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = scopeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkDialog) ArcticWhite else BrandGreenOnContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it.take(100) },
                        label = { Text("Customer Name") },
                        placeholder = { Text("Enter customer name...") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_change_customer_input")
                    )
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
                                    onClick = { customerName = name },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // SECTION 1: Returned Products (Multi-Category, Multi-Product)
                    val returnedTitle = when (mode) {
                        CustomerChangeMode.FULL_TO_FULL -> "1. Customer Returns Full (+)"
                        CustomerChangeMode.EMPTY_TO_EMPTY -> "1. Customer Returns Empty (+)"
                        CustomerChangeMode.RETURN_EMPTY -> "1. Returned Empty Cylinders (+)"
                    }
                    Text(
                        text = returnedTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreenPrimary
                    )

                    ExchangeCategoryChipsRow(
                        products = sortedProducts,
                        qtyMap = returnedQtyMap,
                        selectedCategory = retCategoryFilter,
                        onSelectCategory = { retCategoryFilter = it },
                        testTagPrefix = "customer_change_ret_cat"
                    )

                    OutlinedTextField(
                        value = retSearchQuery,
                        onValueChange = { retSearchQuery = it },
                        placeholder = { Text("Filter returned products...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (retSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { retSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_change_ret_search")
                    )

                    ExchangeProductListBox(
                        visibleProducts = visibleRetProducts,
                        qtyMap = returnedQtyMap,
                        testTagPrefix = "customer_change_ret_prod",
                        stockSubtitle = { prod ->
                            "Full: ${fullBalanceOf(prod.id)}  •  Empty: ${emptyBalanceOf(prod.id)}"
                        },
                        previewLine = { prod, qty ->
                            if (qty <= 0) null
                            else {
                                val cur = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                "+$qty $unitLabel: $cur → ${cur + qty}"
                            }
                        },
                        isErrorLine = { _, _ -> false },
                        onQtyChange = { prodId, clean ->
                            localError = null
                            if (clean.isEmpty() || clean == "0") returnedQtyMap.remove(prodId)
                            else returnedQtyMap[prodId] = clean
                        }
                    )

                    // SECTION 2: Given Products (when mode != RETURN_EMPTY)
                    if (isSwapMode) {
                        Spacer(Modifier.height(2.dp))
                        val givenTitle = if (mode == CustomerChangeMode.FULL_TO_FULL) {
                            "2. Give to Customer Full (-)"
                        } else {
                            "2. Give to Customer Empty (-)"
                        }
                        Text(
                            text = givenTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandRedSecondary
                        )

                        ExchangeCategoryChipsRow(
                            products = sortedProducts,
                            qtyMap = givenQtyMap,
                            selectedCategory = givenCategoryFilter,
                            onSelectCategory = { givenCategoryFilter = it },
                            testTagPrefix = "customer_change_giv_cat"
                        )

                        OutlinedTextField(
                            value = givenSearchQuery,
                            onValueChange = { givenSearchQuery = it },
                            placeholder = { Text("Filter replacement products...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (givenSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { givenSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(15.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_change_giv_search")
                        )

                        ExchangeProductListBox(
                            visibleProducts = visibleGivenProducts,
                            qtyMap = givenQtyMap,
                            testTagPrefix = "customer_change_giv_prod",
                            stockSubtitle = { prod ->
                                val base = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                val sameRet = returnedByProd[prod.id] ?: 0
                                val avail = base + sameRet
                                if (sameRet > 0) {
                                    "$unitLabel Avail: $base (+$sameRet ret = $avail)"
                                } else {
                                    "$unitLabel Avail: $avail  (Full: ${fullBalanceOf(prod.id)}, Empty: ${emptyBalanceOf(prod.id)})"
                                }
                            },
                            previewLine = { prod, qty ->
                                if (qty <= 0) null
                                else {
                                    val base = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                    val sameRet = returnedByProd[prod.id] ?: 0
                                    val netAfter = base + sameRet - qty
                                    if (netAfter < 0) {
                                        "Insufficient $unitLabel! Avail ${base + sameRet}, Need $qty"
                                    } else {
                                        "-$qty $unitLabel: ${base + sameRet} → $netAfter"
                                    }
                                }
                            },
                            isErrorLine = { prod, qty ->
                                if (qty <= 0) false
                                else {
                                    val base = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                    val sameRet = returnedByProd[prod.id] ?: 0
                                    qty > base + sameRet
                                }
                            },
                            onQtyChange = { prodId, clean ->
                                localError = null
                                if (clean.isEmpty() || clean == "0") givenQtyMap.remove(prodId)
                                else givenQtyMap[prodId] = clean
                            }
                        )
                    }

                    // Live Before -> After Stock Preview Card
                    if (returnedItems.isNotEmpty() || givenItems.isNotEmpty()) {
                        Surface(
                            color = when {
                                hasExceedsStock && isDarkDialog -> DarkRedContainer
                                hasExceedsStock -> BrandRedContainer.copy(alpha = 0.55f)
                                isDarkDialog -> DarkGreenContainer
                                else -> BrandGreenContainer.copy(alpha = 0.45f)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    hasExceedsStock -> BrandRedSecondary.copy(alpha = 0.5f)
                                    isDarkDialog -> ElectricBlueBright.copy(alpha = 0.45f)
                                    else -> BrandGreenPrimary.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_change_preview_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Live Exchange Preview (${mode.shortLabel})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        hasExceedsStock && isDarkDialog -> DarkRedOnContainer
                                        hasExceedsStock -> BrandRedOnContainer
                                        isDarkDialog -> ArcticWhite
                                        else -> BrandGreenOnContainer
                                    }
                                )
                                returnedItems.forEach { item ->
                                    val prod = sortedProducts.firstOrNull { it.id == item.productId }
                                    if (prod != null) {
                                        val before = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                        Text(
                                            text = "+In ${prod.category.shortCode} ${prod.name} $unitLabel: $before → ${before + item.quantity}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            color = if (isDarkDialog) ElectricBlueGlow else BrandGreenOnContainer
                                        )
                                    }
                                }
                                givenItems.forEach { item ->
                                    val prod = sortedProducts.firstOrNull { it.id == item.productId }
                                    if (prod != null) {
                                        val base = if (mode == CustomerChangeMode.FULL_TO_FULL) fullBalanceOf(prod.id) else emptyBalanceOf(prod.id)
                                        val sameRet = returnedByProd[prod.id] ?: 0
                                        val effective = base + sameRet
                                        val after = effective - item.quantity
                                        Text(
                                            text = "-Out ${prod.category.shortCode} ${prod.name} $unitLabel: $effective → $after",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            color = when {
                                                after < 0 -> BrandRedBright
                                                isDarkDialog -> MetallicOrangeBright
                                                else -> BrandRedOnContainer
                                            }
                                        )
                                    }
                                }
                                if (hasExceedsStock) {
                                    Text(
                                        text = "Insufficient $unitLabel: ${insufficientProducts.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandRedBright
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it.take(180) },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("e.g. Multi-cylinder exchange or valve swap") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_change_notes_input")
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
                            when {
                                returnedItems.isEmpty() -> {
                                    localError = "Enter quantity (> 0) for at least one returned product."
                                }
                                isSwapMode && givenItems.isEmpty() -> {
                                    localError = "Enter quantity (> 0) for at least one given product."
                                }
                                hasExceedsStock -> {
                                    localError = "Insufficient stock: ${insufficientProducts.first()}"
                                }
                                else -> {
                                    val cleanCust = customerName.trim()
                                    val cleanNotes = notesText.trim()
                                    if (onConfirmMulti != null) {
                                        onConfirmMulti(mode, returnedItems, givenItems, cleanCust, cleanNotes)
                                    } else {
                                        val firstRet = returnedItems.first()
                                        val firstGiv = givenItems.firstOrNull()
                                        onConfirm(mode, firstRet.productId, firstGiv?.productId, firstRet.quantity, cleanCust, cleanNotes)
                                    }
                                    onDismiss()
                                }
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("customer_change_confirm_button")
                    ) {
                        val totalItemsCount = returnedItems.size + givenItems.size
                        Text(
                            text = if (totalItemsCount > 1) "SAVE ENTRY ($totalItemsCount)" else "SAVE ENTRY",
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
private fun ExchangeCategoryChipsRow(
    products: List<ProductEntity>,
    qtyMap: Map<Long, String>,
    selectedCategory: ProductCategory?,
    onSelectCategory: (ProductCategory?) -> Unit,
    testTagPrefix: String
) {
    val totalSelected = products.count { (qtyMap[it.id]?.toIntOrNull() ?: 0) > 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = {
                Text(
                    text = if (totalSelected == 0) "All (4 Folders)" else "All ($totalSelected)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier.testTag("${testTagPrefix}_all")
        )
        ProductCategory.entries.forEach { cat ->
            val catStyle = categoryVisualStyle(cat)
            val catCount = products.count { it.category == cat && (qtyMap[it.id]?.toIntOrNull() ?: 0) > 0 }
            val isSel = selectedCategory == cat
            FilterChip(
                selected = isSel,
                onClick = { onSelectCategory(if (isSel) null else cat) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = if (catCount > 0) "${cat.shortCode} ($catCount)" else cat.shortCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = catStyle.primaryColor,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                modifier = Modifier.testTag("${testTagPrefix}_${cat.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun ExchangeProductListBox(
    visibleProducts: List<ProductEntity>,
    qtyMap: Map<Long, String>,
    testTagPrefix: String,
    stockSubtitle: (ProductEntity) -> String,
    previewLine: (ProductEntity, Int) -> String?,
    isErrorLine: (ProductEntity, Int) -> Boolean,
    onQtyChange: (Long, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 210.dp)
    ) {
        if (visibleProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching products.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val grouped = visibleProducts.groupBy { it.category }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (category, catProducts) ->
                    val catStyle = categoryVisualStyle(category)
                    item(key = "${testTagPrefix}_hdr_${category.name}") {
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
                    items(catProducts, key = { "${testTagPrefix}_${it.id}" }) { product ->
                        val qtyStr = qtyMap[product.id].orEmpty()
                        val parsedQty = qtyStr.toIntOrNull() ?: 0
                        val isActive = parsedQty > 0
                        val hasErr = isErrorLine(product, parsedQty)
                        val preview = previewLine(product, parsedQty)
                        val isDarkRow = LocalIsDarkTheme.current

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                hasErr && isDarkRow -> DarkRedContainer
                                hasErr -> BrandRedContainer.copy(alpha = 0.45f)
                                isActive && isDarkRow -> DarkGreenContainer
                                isActive -> BrandGreenContainer.copy(alpha = 0.42f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isActive) 1.4.dp else 1.dp,
                                color = when {
                                    hasErr -> BrandRedSecondary
                                    isActive && isDarkRow -> ElectricBlueBright
                                    isActive -> BrandGreenPrimary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("${testTagPrefix}_${product.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                                                color = catStyle.primaryColor.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = product.category.shortCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catStyle.primaryColor,
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
                                        Text(
                                            text = stockSubtitle(product),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontFamily = JetBrainsMono,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    CompactInlineStepper(
                                        valueText = qtyStr,
                                        onValueChange = { raw ->
                                            onQtyChange(product.id, raw.filter { it.isDigit() }.take(5))
                                        },
                                        testTagPrefix = "${testTagPrefix}_${product.id}_qty"
                                    )
                                }

                                if (!preview.isNullOrBlank()) {
                                    Text(
                                        text = preview,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            hasErr -> BrandRedBright
                                            isDarkRow -> ElectricBlueGlow
                                            else -> BrandGreenOnContainer
                                        }
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
