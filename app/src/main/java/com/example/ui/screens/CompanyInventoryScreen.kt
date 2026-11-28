package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductCategoryEntity
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFolderGrid
import com.example.ui.components.CategoryFolderStat
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.DynamicCategoryFolderGrid
import com.example.ui.components.DynamicCategoryFolderStat
import com.example.ui.components.Futuristic3DCylinderVisual
import com.example.ui.components.FuturisticSpaceBackdrop
import com.example.ui.components.InsideOutsideScopeToggle
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.components.OpenedCategoryFolderHeader
import com.example.ui.components.OpenedDynamicCategoryFolderHeader
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.SignatureAccentHairline
import com.example.ui.components.StockPill
import com.example.ui.components.StockZoneScope
import com.example.ui.components.categoryVisualStyle
import com.example.ui.components.obsidianRoyalAtmosphere
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBackground
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun CompanyInventoryScreen(
    selectedDate: String,
    stockRows: List<CompanyStockRow>,
    categories: List<ProductCategoryEntity> = emptyList(),
    isReadOnly: Boolean = false,
    onDateSelected: (String) -> Unit,
    onOpenProductMaster: () -> Unit,
    onOpenStockReport: () -> Unit,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var activeZone by remember { mutableStateOf(StockZoneScope.INSIDE_STORE) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var selectedDynamicCategoryId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val sortedCategories = remember(categories) {
        categories.filter { it.isActive }.sortedWith(compareBy<ProductCategoryEntity> { it.sortOrder }.thenBy { it.id })
    }
    val useDynamicFolders = sortedCategories.isNotEmpty()
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val openedDynamicCategory = remember(sortedCategories, selectedDynamicCategoryId) {
        sortedCategories.firstOrNull { it.id == selectedDynamicCategoryId }
    }

    val filteredRows = remember(stockRows, selectedCategory, openedDynamicCategory, useDynamicFolders, searchQuery, categoryById) {
        stockRows.filter { row ->
            val catMatch = if (useDynamicFolders) {
                openedDynamicCategory == null ||
                    row.product.categoryId == openedDynamicCategory.id ||
                    (row.product.categoryId == 0L && row.product.category == openedDynamicCategory.legacyEnum)
            } else {
                selectedCategory == null || row.product.category == selectedCategory
            }
            val liveCatName = categoryById[row.product.categoryId]?.name ?: row.product.category.displayName
            catMatch && (searchQuery.isBlank() ||
                row.product.name.contains(searchQuery, ignoreCase = true) ||
                liveCatName.contains(searchQuery, ignoreCase = true) ||
                row.product.category.displayName.contains(searchQuery, ignoreCase = true))
        }
    }

    val allInsideFull = remember(stockRows) { stockRows.sumOf { it.storeFull } }
    val allInsideEmpty = remember(stockRows) { stockRows.sumOf { it.storeEmpty } }
    val allOutsideFull = remember(stockRows) { stockRows.sumOf { it.vehiclesFull } }
    val allOutsideEmpty = remember(stockRows) { stockRows.sumOf { it.vehiclesEmpty } }

    val dynamicFolderStats = remember(stockRows, sortedCategories, activeZone) {
        sortedCategories.map { cat ->
            val catRows = stockRows.filter {
                it.product.categoryId == cat.id || (it.product.categoryId == 0L && it.product.category == cat.legacyEnum)
            }
            DynamicCategoryFolderStat(
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

    val folderStats = remember(stockRows, activeZone) {
        ProductCategory.activeEntries.map { cat ->
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

    val isDark = LocalIsDarkTheme.current
    val screenBody: @Composable () -> Unit = {
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
                DateSelectorBar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    midnightSurface = isDark
                )
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
                        accentColor = ElectricBlueBright,
                        containerColor = BrandGreenContainer,
                        onContainerColor = BrandGreenOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "ALL EMPTY",
                        value = totalEmpty.toString(),
                        subtitle = "Inside $storeEmpty • Outside $vehiclesEmpty",
                        icon = Icons.Default.Recycling,
                        accentColor = MetallicOrange,
                        containerColor = BrandRedContainer,
                        onContainerColor = BrandRedOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "inv_executive_visualization") {
                val grandTotal = (totalFull + totalEmpty).coerceAtLeast(1)
                val fullRatioTarget = (totalFull.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f)
                val fullRatio by animateFloatAsState(
                    targetValue = fullRatioTarget,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                    label = "inv_full_ratio"
                )
                val fullPercent = (fullRatio * 100f).toInt()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ElectricBlueGlow.copy(alpha = 0.28f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.obsidianRoyalAtmosphere()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STOCK COMPOSITION • ${totalFull + totalEmpty} CYLINDERS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlueGlow
                                )
                                Text(
                                    text = "$fullPercent% FULL",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MetallicOrangeBright
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fullRatio.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(ElectricBlueBright, ElectricBlueGlow, MetallicOrangeBright)
                                            )
                                        )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Store Yard: ${storeFull}F / ${storeEmpty}E",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.TextSecondaryDark
                                )
                                Text(
                                    text = "On Fleet: ${vehiclesFull}F / ${vehiclesEmpty}E",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.TextSecondaryDark
                                )
                            }
                        }
                        SignatureAccentHairline(height = 2.dp)
                    }
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
                val folderTitle = if (activeZone == StockZoneScope.INSIDE_STORE) {
                    "Inside Store Category Folders"
                } else {
                    "Outside Fleet Category Folders"
                }
                if (useDynamicFolders) {
                    DynamicCategoryFolderGrid(
                        stats = dynamicFolderStats,
                        selectedCategoryId = selectedDynamicCategoryId,
                        onSelectCategoryId = { id ->
                            selectedDynamicCategoryId = id
                            selectedCategory = sortedCategories.firstOrNull { it.id == id }?.legacyEnum
                        },
                        sectionTitle = folderTitle
                    )
                } else {
                    CategoryFolderGrid(
                        stats = folderStats,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        sectionTitle = folderTitle,
                        onMidnightBackground = isDark
                    )
                }
            }

            item(key = "inv_search") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LuxurySectionHeader(
                        eyebrow = "ENTERPRISE CATALOG",
                        title = "Product Stock Search",
                        accentColor = if (isDark) ElectricBlueGlow else RoyalElectricBlue
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search cylinder or gas category...",
                                color = if (isDark) PlatinumMuted else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isDark) ElectricBlueGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = if (isDark) {
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianSurface,
                                unfocusedContainerColor = ObsidianSurface,
                                focusedBorderColor = ElectricBlueBright,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = ArcticWhite,
                                unfocusedTextColor = ArcticWhite
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val showProducts = (if (useDynamicFolders) openedDynamicCategory != null else selectedCategory != null) || searchQuery.isNotBlank()
            if (showProducts) {
                if (useDynamicFolders && openedDynamicCategory != null) {
                    item(key = "inv_opened_dynamic_${openedDynamicCategory.id}") {
                        OpenedDynamicCategoryFolderHeader(
                            category = openedDynamicCategory,
                            itemCount = filteredRows.size,
                            subtitle = (if (activeZone == StockZoneScope.INSIDE_STORE) "Inside Store" else "Outside Fleet") +
                                " • ${filteredRows.size} products inside folder",
                            onClose = {
                                selectedDynamicCategoryId = null
                                selectedCategory = null
                            }
                        )
                    }
                } else selectedCategory?.let { openCat ->
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
                        val itemTotal = (row.totalFull + row.totalEmpty).coerceAtLeast(1)
                        val itemFullRatio by animateFloatAsState(
                            targetValue = (row.totalFull.toFloat() / itemTotal.toFloat()).coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 360),
                            label = "row_full_ratio"
                        )
                        LuxuryGlassCard(
                            accentGlow = catStyle.primaryColor,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Futuristic3DCylinderVisual(
                                        fullCount = primaryFull,
                                        emptyCount = primaryEmpty,
                                        accentColor = catStyle.primaryColor,
                                        modifier = Modifier.size(width = 36.dp, height = 52.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        val isDarkCard = LocalIsDarkTheme.current
                                        Surface(
                                            color = if (isDarkCard) catStyle.darkContainer else catStyle.lightContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            val liveCategoryLabel = categoryById[row.product.categoryId]?.name ?: row.product.category.displayName
                                            Text(
                                                text = liveCategoryLabel.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDarkCard) catStyle.darkOnContainer else catStyle.lightOnContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
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

                                Spacer(Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) MetallicOrange.copy(alpha = 0.28f)
                                            else BrandRedContainer.copy(alpha = 0.7f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(itemFullRatio.coerceAtLeast(0.02f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(catStyle.primaryColor, ElectricBlueBright)
                                                )
                                            )
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f)
                                        else BrandGreenContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (isDark) BorderStroke(0.8.dp, ElectricBlueBright.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "INSIDE Store: ${row.storeFull}F / ${row.storeEmpty}E",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            color = if (isDark) ElectricBlueGlow else BrandGreenOnContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (isDark) ObsidianMidnight.copy(alpha = 0.72f)
                                        else BrandRedContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (isDark) BorderStroke(0.8.dp, MetallicOrangeBright.copy(alpha = 0.35f)) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "OUTSIDE Fleet: ${row.vehiclesFull}F / ${row.vehiclesEmpty}E",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            color = if (isDark) MetallicOrangeBright else BrandRedOnContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

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
    if (isDark) {
        FuturisticSpaceBackdrop(
            showEnergyParticles = true,
            modifier = Modifier.fillMaxSize()
        ) {
            screenBody()
        }
    } else {
        screenBody()
    }
}
