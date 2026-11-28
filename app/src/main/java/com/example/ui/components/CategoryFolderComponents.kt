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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProductCategoryEntity
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.ui.theme.ArcticWhite
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
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue

/**
 * Distinct Role & Area header palettes so Admin, Owner View, Staff, Inside Store,
 * and Outside Fleet screens are immediately recognizable at a glance.
 */
object RoleHeaderPalettes {
    // Admin: Obsidian Midnight & Royal Electric Blue with Signature Metallic Orange Accent
    val AdminGradient = listOf(Color(0xFF050D1F), Color(0xFF0A1D47), Color(0xFF13347A))
    val AdminAccent = Color(0xFFFF7A2F)

    // Owner View (Read-Only): Executive Obsidian & Platinum Sapphire with Amber Gold Accent
    val OwnerGradient = listOf(Color(0xFF080E24), Color(0xFF1B2452), Color(0xFF2E3A78))
    val OwnerAccent = Color(0xFFF59E0B)

    // Staff: Obsidian & Electric Cyan-Blue Field Command
    val StaffGradient = listOf(Color(0xFF051329), Color(0xFF0B2A57), Color(0xFF0F4C81))
    val StaffAccent = Color(0xFF38BDF8)

    // Inside (Main Store / Yard): Royal Electric Blue
    val InsideStoreGradient = listOf(Color(0xFF061638), Color(0xFF0E2F73), Color(0xFF1D4ED8))
    val InsideStoreAccent = Color(0xFF60A5FA)

    // Outside (Vehicles / Fleet): Obsidian & Signature Metallic Orange
    val OutsideFleetGradient = listOf(Color(0xFF1A0D0A), Color(0xFF531E09), Color(0xFF9A3412))
    val OutsideFleetAccent = Color(0xFFFF7A2F)
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
        accentColor = Color(0xFF1D4ED8),
        softLightBg = Color(0xFFEAF1FF),
        softDarkBg = Color(0xFF10224D),
        darkTextColor = Color(0xFF0A2463)
    ),
    OUTSIDE_FLEET(
        title = "OUTSIDE • Vehicles",
        badge = "Fleet / Route",
        accentColor = Color(0xFFEA580C),
        softLightBg = Color(0xFFFFF1E8),
        softDarkBg = Color(0xFF3A1A0B),
        darkTextColor = Color(0xFF7C2D12)
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

fun categoryIconForKey(iconKey: String): ImageVector = when (iconKey.trim().uppercase()) {
    "FLAME", "LPG" -> Icons.Default.LocalFireDepartment
    "CUTTING", "TORCH", "BUILD" -> Icons.Default.Build
    "SMALL_CYLINDER", "TANK", "CYLINDER" -> Icons.Default.PropaneTank
    "FACTORY", "INDUSTRIAL" -> Icons.Default.Factory
    "SCIENCE", "MEDICAL", "SPECIALTY" -> Icons.Default.Science
    "SHIPPING", "FLEET" -> Icons.Default.LocalShipping
    "VERIFIED", "PREMIUM" -> Icons.Default.Verified
    "INVENTORY" -> Icons.Default.Inventory2
    else -> Icons.Default.PrecisionManufacturing
}

val CATEGORY_ICON_OPTIONS: List<Pair<String, String>> = listOf(
    "INDUSTRIAL" to "Industrial",
    "FLAME" to "LPG Flame",
    "CUTTING" to "Cutting",
    "SMALL_CYLINDER" to "48 CFT Small",
    "SCIENCE" to "Specialty",
    "FACTORY" to "Plant",
    "INVENTORY" to "Storage",
    "VERIFIED" to "Certified"
)

fun categoryVisualStyle(category: ProductCategory): CategoryVisualStyle = when (category) {
    ProductCategory.INDUSTRIAL_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "INDUSTRIAL GAS – REGULAR",
        primaryColor = Color(0xFF1D4ED8), // Royal Electric Blue
        lightContainer = Color(0xFFEAF1FF),
        darkContainer = Color(0xFF112454),
        lightOnContainer = Color(0xFF0A2463),
        darkOnContainer = Color(0xFFDBEAFE),
        icon = Icons.Default.PrecisionManufacturing
    )
    ProductCategory.SMALL_48_CFT -> CategoryVisualStyle(
        category = category,
        shortTitle = "INDUSTRIAL GAS – 48 CFT SMALL",
        primaryColor = Color(0xFF0284C7), // Precision Arctic Azure
        lightContainer = Color(0xFFE0F2FE),
        darkContainer = Color(0xFF0C2D48),
        lightOnContainer = Color(0xFF075985),
        darkOnContainer = Color(0xFFBAE6FD),
        icon = Icons.Default.PropaneTank
    )
    ProductCategory.COOKING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "LPG GAS",
        primaryColor = Color(0xFFFF6B1A), // Signature Metallic Orange
        lightContainer = Color(0xFFFFF1E8),
        darkContainer = Color(0xFF3E1908),
        lightOnContainer = Color(0xFF7C2D12),
        darkOnContainer = Color(0xFFFFEDD5),
        icon = Icons.Default.LocalFireDepartment
    )
    ProductCategory.MCG_CUTTING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "CUTTING GAS",
        primaryColor = Color(0xFF6366F1), // Metallic Indigo Slate
        lightContainer = Color(0xFFEEF2FF),
        darkContainer = Color(0xFF1E1B4B),
        lightOnContainer = Color(0xFF312E81),
        darkOnContainer = Color(0xFFE0E7FF),
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
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(RoyalBlueDeep, accent, MetallicOrange.copy(alpha = 0.72f))
        )
    } else {
        Brush.horizontalGradient(
            listOf(ObsidianSurface, ObsidianElevated, RoyalBlueDeep.copy(alpha = 0.45f))
        )
    }
    val borderColor = if (selected) MetallicOrangeBright else accent.copy(alpha = 0.5f)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .luxuryTapScale()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) MetallicOrangeBright else ElectricBlueGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ArcticWhite,
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
                color = if (selected) ArcticWhite else PlatinumSilver
            )
        }
    }
}

/**
 * Compact Small-List Style Category Folders.
 * Renders each gas category as a clean single-row bar (~48dp height) with its color-coded
 * folder icon badge, title, item count, live Full/Empty mono pill, and expand chevron.
 */
@Composable
fun CategoryFolderGrid(
    stats: List<CategoryFolderStat>,
    selectedCategory: ProductCategory?,
    onSelectCategory: (ProductCategory?) -> Unit,
    sectionTitle: String = "Category Folders",
    headerTrailingAction: (@Composable () -> Unit)? = null,
    onMidnightBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current || onMidnightBackground
    val orderedStats = remember(stats) {
        ProductCategory.activeEntries.map { cat ->
            stats.firstOrNull { it.category == cat } ?: CategoryFolderStat(cat, 0, 0, 0)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MetallicOrangeBright,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ArcticWhite else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectedCategory != null) {
                TextButton(
                    onClick = { onSelectCategory(null) },
                    modifier = Modifier.testTag("category_folder_show_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close folder",
                        tint = MetallicOrangeBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetallicOrangeBright
                    )
                }
            } else if (headerTrailingAction != null) {
                headerTrailingAction()
            }
        }

        // Compact Small-List Style Folder Rows in Midnight Royal Blue + Electric Blue gradient + Metallic Orange accent
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            orderedStats.forEach { stat ->
                val style = categoryVisualStyle(stat.category)
                val isSelected = selectedCategory == stat.category
                val cardGradient = if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(RoyalBlueDeep, RoyalElectricBlue, ObsidianElevated)
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(ObsidianSurface, ObsidianElevated, RoyalBlueDeep.copy(alpha = 0.62f))
                    )
                }
                val borderBrush = if (isSelected) {
                    Brush.horizontalGradient(listOf(ElectricBlueGlow, MetallicOrangeBright))
                } else {
                    Brush.linearGradient(
                        listOf(
                            ElectricBlueBright.copy(alpha = 0.38f),
                            ObsidianBorder.copy(alpha = 0.85f),
                            MetallicOrangeBright.copy(alpha = 0.24f)
                        )
                    )
                }
                val badgeBg = ObsidianMidnight.copy(alpha = 0.82f)
                val badgeBorder = if (isSelected) {
                    MetallicOrangeBright.copy(alpha = 0.85f)
                } else {
                    ElectricBlueBright.copy(alpha = 0.42f)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .luxuryTapScale()
                        .clickable {
                            onSelectCategory(if (isSelected) null else stat.category)
                        }
                        .testTag("category_folder_${stat.category.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) 1.6.dp else 1.dp,
                        brush = borderBrush
                    ),
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardGradient)
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Midnight Folder Icon Badge (zero white background or wash)
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, badgeBorder),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = style.shortTitle,
                                    tint = if (isSelected) MetallicOrangeBright else style.darkOnContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Category Name + Item Count
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = style.shortTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ArcticWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stat.category.shortCode} • ${stat.itemCount} items",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = PlatinumSilver,
                                maxLines = 1
                            )
                        }

                        // Compact High-Contrast Full / Empty Mono Pill (zero white wash)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            border = BorderStroke(
                                0.9.dp,
                                if (isSelected) MetallicOrangeBright.copy(alpha = 0.72f)
                                else ElectricBlueBright.copy(alpha = 0.36f)
                            )
                        ) {
                            Text(
                                text = "F:${stat.fullTotal} E:${stat.emptyTotal}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MetallicOrangeBright else ElectricBlueGlow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Expand / Collapse Chevron
                        Icon(
                            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isSelected) "Collapse folder" else "Open folder",
                            tint = if (isSelected) MetallicOrangeBright else ElectricBlueGlow,
                            modifier = Modifier.size(18.dp)
                        )
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
        ProductCategory.activeEntries.map { cat ->
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

    LuxuryGlassCard(
        shape = RoundedCornerShape(20.dp),
        accentGlow = activeZone.accentColor,
        forceMidnight = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_inside_outside_folders_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite
                    )
                    Text(
                        text = "Separate Inside (Main Store) & Outside (Vehicles) category view",
                        style = MaterialTheme.typography.bodySmall,
                        color = PlatinumSilver
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
                        color = MetallicOrangeBright
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
                onMidnightBackground = true,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianMidnight.copy(alpha = 0.78f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${selectedCategory?.displayName.orEmpty()} — " +
                            if (activeZone == StockZoneScope.INSIDE_STORE) "Inside Store Stock" else "Outside Vehicles Stock",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MetallicOrangeBright
                    )
                    HorizontalDivider(color = ElectricBlueBright.copy(alpha = 0.35f))
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
                                    color = ArcticWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$altLabel Full: $altFull • Total Full: ${row.totalFull}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PlatinumSilver
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StockPill(label = "F", value = fullVal, isFull = true, forceMidnight = true)
                                StockPill(label = "E", value = empVal, isFull = false, forceMidnight = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner header shown right above the items inside an opened gas category folder.
 * Gives clear visual feedback of which folder is currently open and provides a 1-tap Close action.
 */
@Composable
fun OpenedCategoryFolderHeader(
    category: ProductCategory,
    itemCount: Int,
    subtitle: String = "$itemCount products inside folder",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = categoryVisualStyle(category)
    OpenedSectionFolderHeader(
        title = category.displayName,
        subtitle = subtitle,
        accentColor = ElectricBlueBright,
        containerColor = ObsidianDeep,
        onContainerColor = ArcticWhite,
        icon = style.icon,
        onClose = onClose,
        modifier = modifier.testTag("opened_category_folder_header_${category.name.lowercase()}")
    )
}

/**
 * Reusable opened-folder header bar for any folder section (gas category or system settings).
 */
@Composable
fun OpenedSectionFolderHeader(
    title: String,
    subtitle: String,
    accentColor: Color,
    containerColor: Color,
    onContainerColor: Color,
    icon: ImageVector,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(listOf(ElectricBlueGlow.copy(alpha = 0.72f), MetallicOrangeBright.copy(alpha = 0.72f)))
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ObsidianMidnight, ObsidianDeep, RoyalBlueDeep.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MetallicOrange, MetallicOrangeBright))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = ArcticWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcticWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = PlatinumSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = onClose,
                border = BorderStroke(1.dp, MetallicOrangeBright.copy(alpha = 0.8f)),
                modifier = Modifier.testTag("close_opened_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close folder",
                    tint = MetallicOrangeBright,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MetallicOrangeBright
                )
            }
        }
    }
}

data class DynamicCategoryFolderStat(
    val category: ProductCategoryEntity,
    val itemCount: Int,
    val activeItemCount: Int,
    val fullTotal: Int,
    val emptyTotal: Int
)

/**
 * Dynamic 3D Glassmorphism Category Folder Grid for Product Master & Inventory.
 * Renders all permanent-ID categories (4 default folders + any Admin-created custom folders)
 * with live product counts, Full/Empty stock summary, Active/Inactive badge, and Admin Edit action.
 */
@Composable
fun DynamicCategoryFolderGrid(
    stats: List<DynamicCategoryFolderStat>,
    selectedCategoryId: Long?,
    onSelectCategoryId: (Long?) -> Unit,
    onEditCategory: ((ProductCategoryEntity) -> Unit)? = null,
    sectionTitle: String = "Product Category Folders",
    modifier: Modifier = Modifier
) {
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MetallicOrangeBright,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArcticWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectedCategoryId != null) {
                TextButton(
                    onClick = { onSelectCategoryId(null) },
                    modifier = Modifier.testTag("dynamic_category_close_all")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close folder", tint = MetallicOrangeBright, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("All Folders", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MetallicOrangeBright)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            stats.forEach { stat ->
                val cat = stat.category
                val isSelected = selectedCategoryId == cat.id
                val visual = categoryVisualStyle(cat.legacyEnum)
                val cardGradient = if (isSelected) {
                    Brush.horizontalGradient(listOf(RoyalBlueDeep, RoyalElectricBlue, ObsidianElevated))
                } else {
                    Brush.horizontalGradient(listOf(ObsidianSurface, ObsidianElevated, RoyalBlueDeep.copy(alpha = 0.62f)))
                }
                val borderBrush = if (isSelected) {
                    Brush.horizontalGradient(listOf(ElectricBlueGlow, MetallicOrangeBright))
                } else if (!cat.isActive) {
                    Brush.horizontalGradient(listOf(MetallicOrange.copy(alpha = 0.45f), ObsidianBorder))
                } else {
                    Brush.linearGradient(listOf(ElectricBlueBright.copy(alpha = 0.42f), ObsidianBorder, MetallicOrangeBright.copy(alpha = 0.25f)))
                }
                val badgeBg = ObsidianMidnight.copy(alpha = 0.86f)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .luxuryTapScale()
                        .clickable { onSelectCategoryId(if (isSelected) null else cat.id) }
                        .testTag("dynamic_category_folder_${cat.id}"),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.Transparent,
                    border = BorderStroke(if (isSelected) 1.6.dp else 1.dp, borderBrush),
                    shadowElevation = if (isSelected) 7.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardGradient)
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, if (isSelected) MetallicOrangeBright else visual.primaryColor.copy(alpha = 0.55f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.FolderOpen else categoryIconForKey(cat.iconKey),
                                    contentDescription = cat.name,
                                    tint = if (isSelected) MetallicOrangeBright else ElectricBlueGlow,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!cat.isActive) {
                                    Surface(shape = RoundedCornerShape(5.dp), color = MetallicOrange.copy(alpha = 0.25f)) {
                                        Text(
                                            text = "INACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MetallicOrangeBright,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${stat.activeItemCount}/${stat.itemCount} active • ${cat.description.ifBlank { cat.categoryType.replace('_', ' ') }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = PlatinumSilver,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            border = BorderStroke(0.9.dp, if (isSelected) MetallicOrangeBright.copy(alpha = 0.72f) else ElectricBlueBright.copy(alpha = 0.36f))
                        ) {
                            Text(
                                text = "F:${stat.fullTotal} E:${stat.emptyTotal}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MetallicOrangeBright else ElectricBlueGlow,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        if (onEditCategory != null) {
                            IconButton(
                                onClick = { onEditCategory(cat) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("edit_category_button_${cat.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit ${cat.name}",
                                    tint = ElectricBlueGlow,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isSelected) "Collapse folder" else "Open folder",
                            tint = if (isSelected) MetallicOrangeBright else ElectricBlueGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}



