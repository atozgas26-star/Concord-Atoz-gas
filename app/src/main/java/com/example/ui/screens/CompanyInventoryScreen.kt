package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFolderGrid
import com.example.ui.components.CategoryFolderStat
import com.example.ui.components.ClosedFolderPromptCard
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.InsideOutsideScopeToggle
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.OpenedCategoryFolderHeader
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.StockPill
import com.example.ui.components.StockZoneScope
import com.example.ui.components.categoryVisualStyle
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono

@Composable
fun CompanyInventoryScreen(
    selectedDate: String,
    stockRows: List<CompanyStockRow>,
    isReadOnly: Boolean = false,
    onDateSelected: (String) -> Unit,
    onOpenProductMaster: () -> Unit,
    onOpenStockReport: () -> Unit,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var activeZone by remember { mutableStateOf(StockZoneScope.INSIDE_STORE) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRows = remember(stockRows, selectedCategory, searchQuery) {
        stockRows.filter { row ->
            (selectedCategory == null || row.product.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    row.product.name.contains(searchQuery, ignoreCase = true) ||
                    row.product.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    val allInsideFull = remember(stockRows) { stockRows.sumOf { it.storeFull } }
    val allInsideEmpty = remember(stockRows) { stockRows.sumOf { it.storeEmpty } }
    val allOutsideFull = remember(stockRows) { stockRows.sumOf { it.vehiclesFull } }
    val allOutsideEmpty = remember(stockRows) { stockRows.sumOf { it.vehiclesEmpty } }

    val folderStats = remember(stockRows, activeZone) {
        ProductCategory.entries.map { cat ->
            val catRows = stockRows.filter { it.product.category == cat }
            CategoryFolderStat(
                category = cat,
                itemCount = catRows.size,
                fullTotal = if (activeZone == StockZoneScope.INSIDE_STORE) {
                    catRows.sumOf { it.storeFull }
                } else {
                    catRows.sumOf { it.vehiclesFull }
                },
                emptyTotal = if (activeZone == StockZoneScope.INSIDE_STORE) {
                    catRows.sumOf { it.storeEmpty }
                } else {
                    catRows.sumOf { it.vehiclesEmpty }
                }
            )
        }
    }

    val totalFull = filteredRows.sumOf { it.totalFull }
    val totalEmpty = filteredRows.sumOf { it.totalEmpty }
    val storeFull = filteredRows.sumOf { it.storeFull }
    val vehiclesFull = filteredRows.sumOf { it.vehiclesFull }
    val storeEmpty = filteredRows.sumOf { it.storeEmpty }
    val vehiclesEmpty = filteredRows.sumOf { it.vehiclesEmpty }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Company Inventory (View Only)" else "Company Inventory",
            subtitle = "Inside Store + Outside Fleet Folders" + if (isReadOnly) " • Read-Only" else "",
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.AdminGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.AdminAccent,
            trailingContent = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("inventory_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "inv_date") {
                DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
            }

            item(key = "inv_kpis") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "ALL FULL",
                        value = totalFull.toString(),
                        subtitle = "Inside $storeFull • Outside $vehiclesFull",
                        icon = Icons.Default.PropaneTank,
                        accentColor = BrandGreenPrimary,
                        containerColor = BrandGreenContainer,
                        onContainerColor = BrandGreenOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "ALL EMPTY",
                        value = totalEmpty.toString(),
                        subtitle = "Inside $storeEmpty • Outside $vehiclesEmpty",
                        icon = Icons.Default.Recycling,
                        accentColor = BrandRedSecondary,
                        containerColor = BrandRedContainer,
                        onContainerColor = BrandRedOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "inv_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenStockReport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_export_report_button")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Stock")
                    }
                    if (!isReadOnly) {
                        OutlinedButton(
                            onClick = onOpenProductMaster,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_product_master_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Product Master")
                        }
                    }
                }
            }

            item(key = "inv_inside_outside_toggle") {
                InsideOutsideScopeToggle(
                    selectedScope = activeZone,
                    insideTitle = "INSIDE • Store",
                    outsideTitle = "OUTSIDE • Fleet",
                    insideFull = allInsideFull,
                    insideEmpty = allInsideEmpty,
                    outsideFull = allOutsideFull,
                    outsideEmpty = allOutsideEmpty,
                    onScopeSelected = { activeZone = it }
                )
            }

            item(key = "inv_category_folders") {
                CategoryFolderGrid(
                    stats = folderStats,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    sectionTitle = if (activeZone == StockZoneScope.INSIDE_STORE) {
                        "Inside Store Category Folders"
                    } else {
                        "Outside Fleet Category Folders"
                    }
                )
            }

            item(key = "inv_search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cylinder or gas category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val showProducts = selectedCategory != null || searchQuery.isNotBlank()
            if (!showProducts) {
                item(key = "inv_closed_folder_prompt") {
                    ClosedFolderPromptCard(
                        title = "Company Products Inside Folders",
                        subtitle = "Tap any category folder above to open Inside Store & Outside Fleet products, or search by cylinder name.",
                        accentColor = activeZone.accentColor
                    )
                }
            } else {
                selectedCategory?.let { openCat ->
                    item(key = "inv_opened_folder_header_${openCat.name}") {
                        OpenedCategoryFolderHeader(
                            category = openCat,
                            itemCount = filteredRows.size,
                            subtitle = (if (activeZone == StockZoneScope.INSIDE_STORE) "Inside Store" else "Outside Fleet") +
                                " • ${filteredRows.size} products inside folder",
                            onClose = { selectedCategory = null }
                        )
                    }
                }
                if (filteredRows.isEmpty()) {
                    item(key = "inv_empty_search_notice") {
                        Text(
                            text = "No matching products found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(filteredRows, key = { it.product.id }) { row ->
                val catStyle = categoryVisualStyle(row.product.category)
                val primaryFull = if (activeZone == StockZoneScope.INSIDE_STORE) row.storeFull else row.vehiclesFull
                val primaryEmpty = if (activeZone == StockZoneScope.INSIDE_STORE) row.storeEmpty else row.vehiclesEmpty
                val zonePrefix = if (activeZone == StockZoneScope.INSIDE_STORE) "In" else "Out"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, catStyle.primaryColor.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.product.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catStyle.primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = row.product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StockPill(label = "$zonePrefix Full", value = primaryFull, isFull = true)
                                StockPill(label = "$zonePrefix Emp", value = primaryEmpty, isFull = false)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = BrandGreenContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "INSIDE Store: ${row.storeFull}F / ${row.storeEmpty}E",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = BrandGreenOnContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                color = BrandRedContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "OUTSIDE Fleet: ${row.vehiclesFull}F / ${row.vehiclesEmpty}E",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = BrandRedOnContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Company Total: ${row.overallTotalCylinders} cyl (${row.totalFull}F + ${row.totalEmpty}E)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (row.newCustomerSoldOut > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "New Cust Set: -${row.newCustomerSoldOut}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
}
