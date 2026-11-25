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
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StockPill
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

    val totalFull = filteredRows.sumOf { it.totalFull }
    val totalEmpty = filteredRows.sumOf { it.totalEmpty }
    val storeFull = filteredRows.sumOf { it.storeFull }
    val vehiclesFull = filteredRows.sumOf { it.vehiclesFull }
    val storeEmpty = filteredRows.sumOf { it.storeEmpty }
    val vehiclesEmpty = filteredRows.sumOf { it.vehiclesEmpty }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Company Inventory (View Only)" else "Company Inventory",
            subtitle = "Combined Store + Fleet Stock" + if (isReadOnly) " • Read-Only" else "",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "ALL FULL",
                    value = totalFull.toString(),
                    subtitle = "Store $storeFull • Vehicles $vehiclesFull",
                    icon = Icons.Default.PropaneTank,
                    accentColor = BrandGreenPrimary,
                    containerColor = BrandGreenContainer,
                    onContainerColor = BrandGreenOnContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "ALL EMPTY",
                    value = totalEmpty.toString(),
                    subtitle = "Store $storeEmpty • Vehicles $vehiclesEmpty",
                    icon = Icons.Default.Recycling,
                    accentColor = BrandRedSecondary,
                    containerColor = BrandRedContainer,
                    onContainerColor = BrandRedOnContainer,
                    modifier = Modifier.weight(1f)
                )
            }

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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cylinder or gas category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredRows, key = { it.product.id }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                    color = BrandGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = row.product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StockPill(label = "Total Full", value = row.totalFull, isFull = true)
                                StockPill(label = "Total Emp", value = row.totalEmpty, isFull = false)
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
                                    text = "FULL -> Store: ${row.storeFull}  |  Veh: ${row.vehiclesFull}",
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
                                    text = "EMPTY -> Store: ${row.storeEmpty}  |  Veh: ${row.vehiclesEmpty}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = BrandRedOnContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
