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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.example.data.local.ProductCategoryEntity
import com.example.data.model.CompanyStockRow
import com.example.data.model.ProductCategory
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.CategoryElectricBlue
import com.example.ui.theme.CategoryEmeraldGreen
import com.example.ui.theme.CategoryMetallicOrange
import com.example.ui.theme.CategoryVioletBlue
import com.example.ui.theme.ChromeBorderLight
import com.example.ui.theme.DarkCharcoalSecondary
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.GlossyCardBlueTint
import com.example.ui.theme.GlossyCardWhite
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
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue

/**
 * Distinct Role & Area header palettes so Admin, Owner View, Staff, Inside Store,
 * and Outside Fleet screens are immediately recognizable at a glance.
 */
object RoleHeaderPalettes {
    // Admin: Bright Premium Electric Blue Executive Gradient + Metallic Orange Accent
    val AdminGradient = listOf(Color(0xFF1348C9), Color(0xFF1D63ED), Color(0xFF3B82F6))
    val AdminAccent = Color(0xFFFF7A2F)

    // Owner View (Read-Only): Bright Royal Violet-Sapphire Executive Gradient + Warm Gold Accent
    val OwnerGradient = listOf(Color(0xFF3B28CC), Color(0xFF5441E8), Color(0xFF3B82F6))
    val OwnerAccent = Color(0xFFF59E0B)

    // Staff: Bright Electric Azure-Blue Field Command
    val StaffGradient = listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF2563EB))
    val StaffAccent = Color(0xFFFF8533)

    // Inside (Main Store / Yard): Vibrant Electric Blue
    val InsideStoreGradient = listOf(Color(0xFF154AC9), Color(0xFF1D63ED), Color(0xFF38BDF8))
    val InsideStoreAccent = Color(0xFFFFFFFF)

    // Outside (Vehicles / Fleet): Bright Metallic Orange Energy
    val OutsideFleetGradient = listOf(Color(0xFFC2410C), Color(0xFFEA580C), Color(0xFFFF7A2F))
    val OutsideFleetAccent = Color(0xFFFFFFFF)
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
        primaryColor = CategoryElectricBlue, // Electric Blue
        lightContainer = Color(0xFFE6F0FF),
        darkContainer = Color(0xFF112454),
        lightOnContainer = Color(0xFF0A2463),
        darkOnContainer = Color(0xFFDBEAFE),
        icon = Icons.Default.PrecisionManufacturing
    )
    ProductCategory.SMALL_48_CFT -> CategoryVisualStyle(
        category = category,
        shortTitle = "INDUSTRIAL GAS – 48 CFT SMALL",
        primaryColor = CategoryVioletBlue, // Violet Blue
        lightContainer = Color(0xFFEDE9FE),
        darkContainer = Color(0xFF26185B),
        lightOnContainer = Color(0xFF3B1D8F),
        darkOnContainer = Color(0xFFDDD6FE),
        icon = Icons.Default.PropaneTank
    )
    ProductCategory.COOKING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "LPG GAS",
        primaryColor = CategoryMetallicOrange, // Metallic Orange
        lightContainer = Color(0xFFFFEFE5),
        darkContainer = Color(0xFF3E1908),
        lightOnContainer = Color(0xFF8A2C06),
        darkOnContainer = Color(0xFFFFEDD5),
        icon = Icons.Default.LocalFireDepartment
    )
    ProductCategory.MCG_CUTTING_GAS -> CategoryVisualStyle(
        category = category,
        shortTitle = "CUTTING GAS",
        primaryColor = CategoryEmeraldGreen, // Emerald Green
        lightContainer = Color(0xFFD1FAE5),
        darkContainer = Color(0xFF064E3B),
        lightOnContainer = Color(0xFF065F46),
        darkOnContainer = Color(0xFFA7F3D0),
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
    val isDark = LocalIsDarkTheme.current
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(accent, accent.copy(alpha = 0.88f), MetallicOrange.copy(alpha = 0.82f))
        )
    } else if (isDark) {
        Brush.horizontalGradient(
            listOf(ObsidianSurface, ObsidianElevated, RoyalBlueDeep.copy(alpha = 0.45f))
        )
    } else {
        Brush.horizontalGradient(
            listOf(GlossyCardWhite, GlossyCardBlueTint, softBg)
        )
    }
    val borderColor = if (selected) MetallicOrangeBright else if (isDark) accent.copy(alpha = 0.5f) else accent.copy(alpha = 0.35f)
    val textColor = if (selected || isDark) ArcticWhite else DeepNavyText
    val subTextColor = if (selected) PlatinumSilver else if (isDark) PlatinumSilver else DarkCharcoalSecondary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .luxuryTapScale()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(if (selected) 1.6.dp else 1.2.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 7.dp else 3.dp
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
                    tint = if (selected) PureWhite else accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
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
                color = subTextColor
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
                val cardGradient = when {
                    isSelected -> Brush.horizontalGradient(
                        listOf(style.primaryColor, style.primaryColor.copy(alpha = 0.86f), RoyalElectricBlue)
                    )
                    isDark -> Brush.horizontalGradient(
                        listOf(ObsidianSurface, ObsidianElevated, style.darkContainer.copy(alpha = 0.72f))
                    )
                    else -> Brush.horizontalGradient(
                        listOf(GlossyCardWhite, GlossyCardBlueTint, style.lightContainer.copy(alpha = 0.55f))
                    )
                }
                val borderBrush = when {
                    isSelected -> Brush.horizontalGradient(listOf(PureWhite, MetallicOrangeBright))
                    isDark -> Brush.linearGradient(
                        listOf(style.primaryColor.copy(alpha = 0.55f), ObsidianBorder, MetallicOrangeBright.copy(alpha = 0.24f))
                    )
                    else -> Brush.linearGradient(
                        listOf(style.primaryColor.copy(alpha = 0.48f), ChromeBorderLight, PureWhite)
                    )
                }
                val badgeBg = when {
                    isSelected -> PureWhite.copy(alpha = 0.20f)
                    isDark -> ObsidianMidnight.copy(alpha = 0.82f)
                    else -> style.lightContainer
                }
                val badgeBorder = if (isSelected) PureWhite.copy(alpha = 0.7f) else style.primaryColor.copy(alpha = 0.45f)
                val titleColor = if (isSelected || isDark) ArcticWhite else DeepNavyText
                val subColor = if (isSelected) PlatinumSilver else if (isDark) PlatinumSilver else DarkCharcoalSecondary

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .luxuryTapScale()
                        .clickable {
                            onSelectCategory(if (isSelected) null else stat.category)
                        }
                        .testTag("category_folder_${stat.category.name.lowercase()}"),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) 1.6.dp else 1.2.dp,
                        brush = borderBrush
                    ),
                    shadowElevation = if (isSelected) 7.dp else 4.dp,
                    tonalElevation = 0.dp
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
                            border = BorderStroke(1.dp, badgeBorder),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.FolderOpen else style.icon,
                                    contentDescription = style.shortTitle,
                                    tint = if (isSelected) PureWhite else style.primaryColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = style.shortTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stat.category.shortCode} • ${stat.itemCount} items",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                color = subColor,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PureWhite.copy(alpha = 0.22f) else if (isDark) ObsidianMidnight.copy(alpha = 0.8f) else style.lightContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PureWhite.copy(alpha = 0.65f) else style.primaryColor.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = "F:${stat.fullTotal} E:${stat.emptyTotal}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) PureWhite else if (isDark) ElectricBlueGlow else style.lightOnContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isSelected) "Collapse folder" else "Open folder",
                            tint = if (isSelected) PureWhite else style.primaryColor,
                            modifier = Modifier.size(19.dp)
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
 * Dynamic Opened-Folder header showing the live edited category name, icon, and brand color.
 */
@Composable
fun OpenedDynamicCategoryFolderHeader(
    category: ProductCategoryEntity,
    itemCount: Int,
    subtitle: String = "$itemCount products inside folder",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = categoryVisualStyle(category.legacyEnum)
    OpenedSectionFolderHeader(
        title = category.name,
        subtitle = subtitle,
        accentColor = style.primaryColor,
        containerColor = style.lightContainer,
        onContainerColor = style.lightOnContainer,
        icon = categoryIconForKey(category.iconKey),
        onClose = onClose,
        modifier = modifier.testTag("opened_dynamic_category_header_${category.id}")
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
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.3.dp,
            Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.8f), MetallicOrangeBright.copy(alpha = 0.72f)))
        ),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) {
                        Brush.horizontalGradient(listOf(ObsidianMidnight, ObsidianDeep, RoyalBlueDeep.copy(alpha = 0.85f)))
                    } else {
                        Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.88f), RoyalElectricBlue))
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MetallicOrange, MetallicOrangeBright))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = PureWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = com.example.ui.theme.TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(
                onClick = onClose,
                border = BorderStroke(1.2.dp, PureWhite.copy(alpha = 0.85f)),
                modifier = Modifier.testTag("close_opened_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close folder",
                    tint = PureWhite,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }
        }
    }
}

data class DynamicCategoryFolderStat(
    val category: ProductCategoryEntity,
    val itemCount: Int,
    val activeItemCount: Int = itemCount,
    val fullTotal: Int,
    val emptyTotal: Int
)

/**
 * Dynamic 3D Bright Glassmorphism Category Folder Grid for Product Master, Inventory,
 * Store Stock, Vehicle Stock, and Staff Stock.
 * Renders all permanent-ID categories in Admin-defined sortOrder with live edited names,
 * consistent category colors (Electric Blue, Metallic Orange, Emerald Green, Violet Blue),
 * and Admin Drag-and-Drop + Move Up/Down category folder sorting.
 */
@Composable
fun DynamicCategoryFolderGrid(
    stats: List<DynamicCategoryFolderStat>,
    selectedCategoryId: Long?,
    onSelectCategoryId: (Long?) -> Unit,
    onEditCategory: ((ProductCategoryEntity) -> Unit)? = null,
    canReorder: Boolean = false,
    onMoveCategoryUp: ((categoryId: Long) -> Unit)? = null,
    onMoveCategoryDown: ((categoryId: Long) -> Unit)? = null,
    onDragSwapCategory: ((categoryId: Long, deltaSteps: Int) -> Unit)? = null,
    sectionTitle: String = "Product Category Folders",
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
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
                    tint = MetallicOrange,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ArcticWhite else DeepNavyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectedCategoryId != null) {
                TextButton(
                    onClick = { onSelectCategoryId(null) },
                    modifier = Modifier.testTag("dynamic_category_close_all")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close folder", tint = MetallicOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("All Folders", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MetallicOrange)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEachIndexed { idx, stat ->
                val cat = stat.category
                val isSelected = selectedCategoryId == cat.id
                val visual = categoryVisualStyle(cat.legacyEnum)
                var dragOffsetY by remember(cat.id) { mutableFloatStateOf(0f) }
                var isDragging by remember(cat.id) { mutableStateOf(false) }
                val stepPx = 105f

                val cardGradient = when {
                    isSelected -> Brush.horizontalGradient(listOf(visual.primaryColor, visual.primaryColor.copy(alpha = 0.88f), RoyalElectricBlue))
                    isDark -> Brush.horizontalGradient(listOf(ObsidianSurface, ObsidianElevated, visual.darkContainer.copy(alpha = 0.65f)))
                    else -> Brush.horizontalGradient(listOf(GlossyCardWhite, GlossyCardBlueTint, visual.lightContainer.copy(alpha = 0.58f)))
                }
                val borderBrush = when {
                    isDragging -> Brush.horizontalGradient(listOf(MetallicOrange, ElectricBlueBright, MetallicOrangeBright))
                    isSelected -> Brush.horizontalGradient(listOf(PureWhite, MetallicOrangeBright))
                    !cat.isActive -> Brush.horizontalGradient(listOf(MetallicOrange.copy(alpha = 0.55f), ChromeBorderLight))
                    isDark -> Brush.linearGradient(listOf(visual.primaryColor.copy(alpha = 0.52f), ObsidianBorder, MetallicOrangeBright.copy(alpha = 0.25f)))
                    else -> Brush.linearGradient(listOf(visual.primaryColor.copy(alpha = 0.45f), ChromeBorderLight, PureWhite))
                }
                val badgeBg = when {
                    isSelected -> PureWhite.copy(alpha = 0.20f)
                    isDark -> ObsidianMidnight.copy(alpha = 0.86f)
                    else -> visual.lightContainer
                }
                val titleColor = if (isSelected || isDark) ArcticWhite else DeepNavyText
                val subColor = if (isSelected) PlatinumSilver else if (isDark) PlatinumSilver else DarkCharcoalSecondary

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 12f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                        }
                        .heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .luxuryTapScale()
                        .clickable { onSelectCategoryId(if (isSelected) null else cat.id) }
                        .testTag("dynamic_category_folder_${cat.id}"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(if (isDragging || isSelected) 1.8.dp else 1.2.dp, borderBrush),
                    shadowElevation = if (isDragging) 14.dp else if (isSelected) 7.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardGradient)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canReorder && onDragSwapCategory != null) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .pointerInput(cat.id, idx, stats.size) {
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
                                                        onDragSwapCategory(cat.id, 1)
                                                        dragOffsetY -= stepPx
                                                    } else if (dragOffsetY <= -stepPx) {
                                                        onDragSwapCategory(cat.id, -1)
                                                        dragOffsetY += stepPx
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("category_drag_handle_${cat.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Hold and drag to reorder ${cat.name}",
                                        tint = if (isDragging) MetallicOrange else if (isSelected) PureWhite else visual.primaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeBg,
                                border = BorderStroke(1.dp, if (isSelected) PureWhite.copy(alpha = 0.7f) else visual.primaryColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.FolderOpen else categoryIconForKey(cat.iconKey),
                                        contentDescription = cat.name,
                                        tint = if (isSelected) PureWhite else visual.primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (!cat.isActive) {
                                        Surface(shape = RoundedCornerShape(5.dp), color = MetallicOrange.copy(alpha = 0.2f)) {
                                            Text(
                                                text = "INACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) PureWhite else MetallicOrange,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${stat.activeItemCount}/${stat.itemCount} active • ${cat.description.ifBlank { cat.categoryType.replace('_', ' ') }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = subColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg,
                                border = BorderStroke(1.dp, if (isSelected) PureWhite.copy(alpha = 0.65f) else visual.primaryColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "F:${stat.fullTotal} E:${stat.emptyTotal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) PureWhite else if (isDark) ElectricBlueGlow else visual.lightOnContainer,
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
                                        tint = if (isSelected) PureWhite else visual.primaryColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isSelected) "Collapse folder" else "Open folder",
                                tint = if (isSelected) PureWhite else visual.primaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        if (canReorder && (onMoveCategoryUp != null || onMoveCategoryDown != null)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) PureWhite.copy(alpha = 0.10f) else if (isDark) ObsidianMidnight.copy(alpha = 0.4f) else visual.lightContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order #${idx + 1} • Hold grip to drag or tap arrows",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = subColor
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onMoveCategoryUp?.invoke(cat.id) },
                                        enabled = idx > 0,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("category_move_up_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move ${cat.name} up",
                                            tint = if (idx > 0) (if (isSelected) PureWhite else visual.primaryColor) else subColor.copy(alpha = 0.35f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveCategoryDown?.invoke(cat.id) },
                                        enabled = idx < stats.lastIndex,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("category_move_down_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move ${cat.name} down",
                                            tint = if (idx < stats.lastIndex) (if (isSelected) PureWhite else visual.primaryColor) else subColor.copy(alpha = 0.35f),
                                            modifier = Modifier.size(16.dp)
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



