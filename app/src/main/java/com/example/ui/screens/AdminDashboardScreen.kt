package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DashboardSummary
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StockPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateOnContainer
import com.example.ui.theme.SlateTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    adminName: String,
    selectedDate: String,
    summary: DashboardSummary?,
    onDateSelected: (String) -> Unit,
    onOpenStore: () -> Unit,
    onOpenVehicleDetail: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "CONCORD & A TO Z GAS",
            subtitle = "Admin Dashboard • Welcome, $adminName",
            trailingContent = {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("admin_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }

            if (summary != null) {
                // Hero KPIs: Total Full & Total Empty
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "TOTAL FULL",
                            value = summary.totalFullCylinders.toString(),
                            subtitle = "Store ${summary.storeFullBalance} + Veh ${summary.allVehiclesFullBalance}",
                            icon = Icons.Default.PropaneTank,
                            accentColor = BrandGreenPrimary,
                            containerColor = BrandGreenContainer,
                            onContainerColor = BrandGreenOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TOTAL EMPTY",
                            value = summary.totalEmptyCylinders.toString(),
                            subtitle = "Store ${summary.storeEmptyBalance} + Veh ${summary.allVehiclesEmptyBalance}",
                            icon = Icons.Default.Recycling,
                            accentColor = BrandRedSecondary,
                            containerColor = BrandRedContainer,
                            onContainerColor = BrandRedOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Store Balance & Today's Purchases / Dispatch / Sales
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "STORE BALANCE",
                            value = "${summary.storeFullBalance}/${summary.storeEmptyBalance}",
                            subtitle = "Full / Empty in Yard",
                            icon = Icons.Default.Store,
                            accentColor = SlateTertiary,
                            containerColor = SlateContainer,
                            onContainerColor = SlateOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TODAY'S SALES",
                            value = summary.todaySales.toString(),
                            subtitle = "Cylinders sold today",
                            icon = Icons.Default.Sell,
                            accentColor = BrandGreenPrimary,
                            containerColor = BrandGreenContainer,
                            onContainerColor = BrandGreenOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "TODAY PURCHASES",
                            value = "${summary.todayFullPurchases + summary.todayEmptyPurchases}",
                            subtitle = "Full ${summary.todayFullPurchases} • Emp ${summary.todayEmptyPurchases}",
                            icon = Icons.Default.AddShoppingCart,
                            accentColor = AmberWarning,
                            containerColor = AmberContainer,
                            onContainerColor = AmberOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "TODAY DISPATCH",
                            value = "${summary.todayFullDispatch}",
                            subtitle = "Full out • Emp out ${summary.todayEmptyDispatch}",
                            icon = Icons.Default.LocalShipping,
                            accentColor = BrandRedSecondary,
                            containerColor = BrandRedContainer,
                            onContainerColor = BrandRedOnContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick Navigation Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenStore,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_store_button")
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Store Entry")
                        }
                        FilledTonalButton(
                            onClick = onOpenReports,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_reports_button")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Daily & Monthly")
                        }
                    }
                }

                // Vehicle-Wise Stock Section
                item {
                    Text(
                        text = "Vehicle-Wise Stock (7 Vehicles)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(summary.vehicleOverviews, key = { it.vehicle.id }) { overview ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenVehicleDetail(overview.vehicle.id) }
                            .testTag("dashboard_vehicle_${overview.vehicle.name}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = overview.vehicle.name,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = overview.vehicle.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StockPill(label = "Full", value = overview.totalFullBalance, isFull = true)
                                    StockPill(label = "Empty", value = overview.totalEmptyBalance, isFull = false)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recd: ${overview.totalFullReceived}  •  Sold: ${overview.totalCustomerSales}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMono,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Cust Emp: ${overview.totalCustomerEmptyReceived}  •  Ret: ${overview.totalEmptyReturnedToStore}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMono,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Staff Activity Section
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Staff activity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Staff Activity ($selectedDate)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (summary.recentTransactions.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No transactions recorded for $selectedDate yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(summary.recentTransactions, key = { it.transaction.id }) { item ->
                        val tx = item.transaction
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.timestamp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.type.label} • ${item.productName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.productCategory.displayName} • By ${tx.staffName}" +
                                            (tx.vehicleName?.let { " • Veh: $it" } ?: "") +
                                            " • $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = BrandGreenContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "×${tx.quantity}",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenOnContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
