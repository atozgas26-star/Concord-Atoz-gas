package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Distinct Role & Area header palettes so Admin, Owner View, Staff, Inside Store,
 * and Outside Fleet screens are immediately recognizable at a glance.
 */
object RoleHeaderPalettes {
    // Admin: Royal Navy & Crimson
    val AdminGradient = listOf(Color(0xFF092C6C), Color(0xFF1E3A8A))
    val AdminAccent = Color(0xFFDC2626)

    // Owner View (Read-Only): Executive Deep Indigo & Amber Gold
    val OwnerGradient = listOf(Color(0xFF312E81), Color(0xFF4338CA))
    val OwnerAccent = Color(0xFFF59E0B)

    // Staff: Energetic Deep Teal & Emerald
    val StaffGradient = listOf(Color(0xFF064E3B), Color(0xFF0F766E))
    val StaffAccent = Color(0xFF10B981)

    // Inside (Main Store / Yard): Deep Emerald
    val InsideStoreGradient = listOf(Color(0xFF065F46), Color(0xFF047857))
    val InsideStoreAccent = Color(0xFF34D399)

    // Outside (Vehicles / Fleet): Rich Crimson-Slate
    val OutsideFleetGradient = listOf(Color(0xFF7C2D12), Color(0xFFB91C1C))
    val OutsideFleetAccent = Color(0xFFF59E0B)
}

enum class StockZoneScope(
    val title: String,
    val badge: String,
    val accentColor: Color,
    val softLightBg: Color,
    val softDarkBg: Color,
    val darkTextColor: Color
) {
    INSIDE_STORE(
        title = "INSIDE • Store",
        badge = "Main Yard",
        accentColor = Color(0xFF0F766E),
        softLightBg = Color(0xFFCCFBF1),
        softDarkBg = Color(0xFF0F2D2B),
        darkTextColor = Color(0xFF115E59)
    ),
    OUTSIDE_FLEET(
        title = "OUTSIDE • Vehicles",
        badge = "Fleet / Route",
        accentColor = Color(0xFFC2410C),
        softLightBg = Color(0xFFFFEDD5),
        softDarkBg = Color(0xFF331B0E),
        darkTextColor = Color(0xFF9A3412)
    )
}

data class CategoryVisualStyle(
    val category: ProductCategory,
    val shortTitle: String,
    val primaryColor: Color,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightOnContainer: Color,
    val darkOnContainer: Color,
    val icon: ImageVector
)

fun categoryVisualStyle(category: ProductCategory): CategoryVisualStyle = when (category) {
    ProductCategory.INDUSTRIAL_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "Industrial Gas",
        primaryColor = Color(0xFF1D4ED8), // Royal Blue
        lightContainer = Color(0xFFDBEAFE),
        darkContainer = Color(0xFF172554),
        lightOnContainer = Color(0xFF1E3A8A),
        darkOnContainer = Color(0xFFBFDBFE),
        icon = Icons.Default.PrecisionManufacturing
    )
    ProductCategory.SMALL_48_CFT -> CategoryVisualStyle(
        category = category,
        shortTitle = "Small 48 CFT",
        primaryColor = Color(0xFF0F766E), // Vivid Teal
        lightContainer = Color(0xFFCCFBF1),
        darkContainer = Color(0xFF134E4A),
        lightOnContainer = Color(0xFF115E59),
        darkOnContainer = Color(0xFF99F6E4),
        icon = Icons.Default.PropaneTank
    )
    ProductCategory.COOKING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "Cooking Gas",
        primaryColor = Color(0xFFC2410C), // Fiery Orange
        lightContainer = Color(0xFFFFEDD5),
        darkContainer = Color(0xFF431407),
        lightOnContainer = Color(0xFF7C2D12),
        darkOnContainer = Color(0xFFFED7AA),
        icon = Icons.Default.LocalFireDepartment
    )
    ProductCategory.MCG_CUTTING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "MCG Cutting Gas",
        primaryColor = Color(0xFF7E22CE), // Royal Purple
        lightContainer = Color(0xFFF3E8FF),
        darkContainer = Color(0xFF3B0764),
        lightOnContainer = Color(0xFF581C87),
        darkOnContainer = Color(0xFFE9D5FF),
        icon = Icons.Default.Build
    )
}

data class CategoryFolderStat(
    val category: ProductCategory,
    val itemCount: Int,
    val fullTotal: Int,
    val emptyTotal: Int
)

/**
 * Distinct color-coded INSIDE (Store) vs OUTSIDE (Vehicles / My Vehicle) toggle tabs.
 */
@Composable
fun InsideOutsideScopeToggle(
    selectedScope: StockZoneScope,
    insideTitle: String = "INSIDE • Store",
    outsideTitle: String = "OUTSIDE • Vehicles",
    insideFull: Int,
    insideEmpty: Int,
    outsideFull: Int,
    outsideEmpty: Int,
    onScopeSelected: (StockZoneScope) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ZoneScopePillCard(
            title = insideTitle,
            fullCount = insideFull,
            emptyCount = insideEmpty,
            icon = Icons.Default.Store,
            accent = StockZoneScope.INSIDE_STORE.accentColor,
            softBg = if (isDark) StockZoneScope.INSIDE_STORE.softDarkBg else StockZoneScope.INSIDE_STORE.softLightBg,
            selected = selectedScope == StockZoneScope.INSIDE_STORE,
            onClick = { onScopeSelected(StockZoneScope.INSIDE_STORE) },
            modifier = Modifier
                .weight(1f)
                .testTag("zone_scope_inside_button")
        )
        ZoneScopePillCard(
            title = outsideTitle,
            fullCount = outsideFull,
            emptyCount = outsideEmpty,
            icon = Icons.Default.LocalShipping,
            accent = StockZoneScope.OUTSIDE_FLEET.accentColor,
            softBg = if (isDark) StockZoneScope.OUTSIDE_FLEET.softDarkBg else StockZoneScope.OUTSIDE_FLEET.softLightBg,
            selected = selectedScope == StockZoneScope.OUTSIDE_FLEET,
            onClick = { onScopeSelected(StockZoneScope.OUTSIDE_FLEET) },
            modifier = Modifier
                .weight(1f)
                .testTag("zone_scope_outside_button")
        )
    }
}

@Composable
private fun ZoneScopePillCard(
    title: String,
    fullCount: Int,
    emptyCount: Int,
    icon: ImageVector,
    accent: Color,
    softBg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent else softBg.copy(alpha = 0.72f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else accent.copy(alpha = 0.4f)),
        tonalElevation = if (selected) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) Color.White else accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Full: $fullCount  •  Emp: $emptyCount",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White.copy(alpha = 0.94f) else accent
            )
        }
    }
}

/**
 * Vibrant 2x2 color-coded Category Folders grid.
 * Each gas category renders as a physical folder silhouette with its own distinct color,
 * icon, product count, and live Full / Empty cylinder totals.
 */
@Composable
fun CategoryFolderGrid(
    stats: List<CategoryFolderStat>,
    selectedCategory: ProductCategory?,
    onSelectCategory: (ProductCategory?) -> Unit,
    sectionTitle: String = "Category Folders",
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val orderedStats = remember(stats) {
        ProductCategory.entries.map { cat ->
            stats.firstOrNull { it.category == cat } ?: CategoryFolderStat(cat, 0, 0, 0)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (selectedCategory != null) {
                TextButton(
                    onClick = { onSelectCategory(null) },
                    modifier = Modifier.testTag("category_folder_show_all_button")
                ) {
                    Text(
                        text = "Show All (${orderedStats.sumOf { it.itemCount }})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Tap a folder to open",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2x2 Folder Grid
        orderedStats.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { stat ->
                    val style = categoryVisualStyle(stat.category)
                    val isSelected = selectedCategory == stat.category
                    val bgColor = if (isSelected) {
                        style.primaryColor
                    } else if (isDark) {
                        style.darkContainer
                    } else {
                        style.lightContainer
                    }
                    val textColor = if (isSelected) {
                        Color.White
                    } else if (isDark) {
                        style.darkOnContainer
                    } else {
                        style.lightOnContainer
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectCategory(if (isSelected) null else stat.category)
                            }
                            .testTag("category_folder_${stat.category.name.lowercase()}")
                    ) {
                        // Folder top tab ear
                        Row(verticalAlignment = Alignment.Bottom) {
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    .background(style.primaryColor),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = stat.category.shortCode,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        // Folder main body
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 14.dp,
                                bottomStart = 14.dp,
                                bottomEnd = 14.dp
                            ),
                            color = bgColor,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.2.dp,
                                color = if (isSelected) style.primaryColor else style.primaryColor.copy(alpha = 0.45f)
                            ),
                            tonalElevation = if (isSelected) 4.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                                else style.primaryColor.copy(alpha = 0.16f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = style.icon,
                                            contentDescription = style.shortTitle,
                                            tint = if (isSelected) Color.White else style.primaryColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else style.primaryColor.copy(alpha = 0.72f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = style.shortTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.2f)
                                        else style.primaryColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "F:${stat.fullTotal}  E:${stat.emptyTotal}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else style.primaryColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${stat.itemCount} items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.8f)
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

/**
 * Reusable Admin & Owner Dashboard Card offering 1-tap Inside (Store) vs Outside (Vehicles/Fleet)
 * switching and 4 vibrant color-coded Category Folders with instant inline cylinder breakdown.
 */
@Composable
fun DashboardInsideOutsideFolderCard(
    companyStockRows: List<CompanyStockRow>,
    onOpenStore: () -> Unit,
    onOpenVehicles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeZone by remember { mutableStateOf(StockZoneScope.INSIDE_STORE) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }

    val insideFull = remember(companyStockRows) { companyStockRows.sumOf { it.storeFull } }
    val insideEmpty = remember(companyStockRows) { companyStockRows.sumOf { it.storeEmpty } }
    val outsideFull = remember(companyStockRows) { companyStockRows.sumOf { it.vehiclesFull } }
    val outsideEmpty = remember(companyStockRows) { companyStockRows.sumOf { it.vehiclesEmpty } }

    val folderStats = remember(companyStockRows, activeZone) {
        ProductCategory.entries.map { cat ->
            val catRows = companyStockRows.filter { it.product.category == cat }
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

    val expandedRows = remember(companyStockRows, selectedCategory) {
        if (selectedCategory == null) emptyList()
        else companyStockRows.filter { it.product.category == selectedCategory }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_inside_outside_folders_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, activeZone.accentColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inside Store & Outside Fleet Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Separate Inside (Main Store) & Outside (Vehicles) category view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        if (activeZone == StockZoneScope.INSIDE_STORE) onOpenStore() else onOpenVehicles()
                    }
                ) {
                    Text(
                        text = if (activeZone == StockZoneScope.INSIDE_STORE) "Open Store" else "Open Fleet",
                        fontWeight = FontWeight.Bold,
                        color = activeZone.accentColor
                    )
                }
            }

            InsideOutsideScopeToggle(
                selectedScope = activeZone,
                insideTitle = "INSIDE • Store",
                outsideTitle = "OUTSIDE • Fleet",
                insideFull = insideFull,
                insideEmpty = insideEmpty,
                outsideFull = outsideFull,
                outsideEmpty = outsideEmpty,
                onScopeSelected = { activeZone = it }
            )

            CategoryFolderGrid(
                stats = folderStats,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                sectionTitle = if (activeZone == StockZoneScope.INSIDE_STORE) {
                    "Inside Main Store Folders"
                } else {
                    "Outside Vehicles / Fleet Folders"
                }
            )

            AnimatedVisibility(
                visible = selectedCategory != null && expandedRows.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val catStyle = selectedCategory?.let { categoryVisualStyle(it) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            (catStyle?.primaryColor ?: activeZone.accentColor).copy(alpha = 0.08f)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${selectedCategory?.displayName.orEmpty()} — " +
                            if (activeZone == StockZoneScope.INSIDE_STORE) "Inside Store Stock" else "Outside Vehicles Stock",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = catStyle?.primaryColor ?: activeZone.accentColor
                    )
                    HorizontalDivider(color = (catStyle?.primaryColor ?: activeZone.accentColor).copy(alpha = 0.25f))
                    expandedRows.forEach { row ->
                        val fullVal = if (activeZone == StockZoneScope.INSIDE_STORE) row.storeFull else row.vehiclesFull
                        val empVal = if (activeZone == StockZoneScope.INSIDE_STORE) row.storeEmpty else row.vehiclesEmpty
                        val altFull = if (activeZone == StockZoneScope.INSIDE_STORE) row.vehiclesFull else row.storeFull
                        val altLabel = if (activeZone == StockZoneScope.INSIDE_STORE) "Out" else "In"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$altLabel Full: $altFull • Total Full: ${row.totalFull}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StockPill(label = "F", value = fullVal, isFull = true)
                                StockPill(label = "E", value = empVal, isFull = false)
                            }
                        }
                    }
                }
            }
        }
    }
}
