package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.local.VehicleEntity
import com.example.data.model.ReportType
import com.example.export.ReportExporter
import com.example.export.ReportTable
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.DateSelectorBar
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(
    selectedDate: String,
    selectedYearMonth: String,
    vehicles: List<VehicleEntity>,
    selectedVehicleId: Long?,
    recentPurchaseCompanies: List<String> = emptyList(),
    isReadOnly: Boolean = false,
    initialReportType: ReportType = ReportType.DAILY_STORE,
    buildReportTable: (ReportType, Long?, Boolean, String?) -> ReportTable,
    onDateSelected: (String) -> Unit,
    onYearMonthSelected: (String) -> Unit,
    onSelectVehicle: (Long?) -> Unit,
    onSaveReportCloudAndDrive: (ReportType, ReportTable, Boolean) -> Unit = { _, _, _ -> },
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedReport by remember(initialReportType) { mutableStateOf(initialReportType) }
    var vehicleFilterId by remember { mutableStateOf<Long?>(null) }
    var purchaseDailyMode by remember { mutableStateOf(false) }
    var purchaseCompanyFilter by remember { mutableStateOf<String?>(null) }

    val table = buildReportTable(selectedReport, vehicleFilterId, purchaseDailyMode, purchaseCompanyFilter)

    // SAF launchers to save PDF / Excel directly to user's chosen location (Play-policy compliant)
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportExporter.writePdf(table, out)
                }
                onMessage("PDF saved successfully.")
            } catch (e: Exception) {
                onMessage("Failed to save PDF: ${e.message}")
            }
        }
    }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ReportExporter.writeXlsx(table, out)
                }
                onMessage("Excel (.xlsx) saved successfully.")
            } catch (e: Exception) {
                onMessage("Failed to save Excel: ${e.message}")
            }
        }
    }

    fun shareFile(isPdf: Boolean) {
        try {
            val ext = if (isPdf) "pdf" else "xlsx"
            val mime = if (isPdf) "application/pdf"
            else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val file = ReportExporter.createExportFile(context, selectedReport.name, ext)
            if (isPdf) ReportExporter.writePdfToFile(table, file) else ReportExporter.writeXlsxToFile(table, file)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "C&A Gas - ${table.reportTitle}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${selectedReport.title}"))
        } catch (e: Exception) {
            onMessage("Export error: ${e.message}")
        }
    }

    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Reports (View Only)" else "Reports & Export",
            subtitle = "Concord & Atoz Gas Trading LLC • PDF & Excel (.xlsx)"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 6 Report Type Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportType.entries.forEach { reportType ->
                    FilterChip(
                        selected = selectedReport == reportType,
                        onClick = { selectedReport = reportType },
                        label = { Text(reportType.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("report_chip_${reportType.name}")
                    )
                }
            }

            // Period Toggle (Daily / Monthly) for Staff Product Sales, Purchase Co. Product, Purchase Register & Staff-Wise Sales
            val supportsDailyMonthlyToggle = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
                selectedReport == ReportType.PURCHASE_REGISTER ||
                selectedReport == ReportType.STAFF_WISE_SALES

            if (supportsDailyMonthlyToggle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = purchaseDailyMode,
                        onClick = { purchaseDailyMode = true },
                        label = { Text("Daily Report") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRedSecondary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("purchase_report_daily_chip")
                    )
                    FilterChip(
                        selected = !purchaseDailyMode,
                        onClick = { purchaseDailyMode = false },
                        label = { Text("Monthly Report") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("purchase_report_monthly_chip")
                    )
                }
            }

            // Supplier / Purchase Company filter chips for Purchase reports
            if ((selectedReport == ReportType.PURCHASE_REGISTER || selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT) &&
                recentPurchaseCompanies.isNotEmpty()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = purchaseCompanyFilter == null,
                        onClick = { purchaseCompanyFilter = null },
                        label = { Text("All Suppliers") },
                        modifier = Modifier.testTag("report_supplier_all")
                    )
                    recentPurchaseCompanies.forEach { company ->
                        FilterChip(
                            selected = purchaseCompanyFilter.equals(company, ignoreCase = true),
                            onClick = { purchaseCompanyFilter = company },
                            label = { Text(company) }
                        )
                    }
                }
            }

            when {
                supportsDailyMonthlyToggle -> {
                    if (purchaseDailyMode) {
                        DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
                    } else {
                        MonthSelectorBar(
                            yearMonth = selectedYearMonth,
                            onYearMonthChanged = onYearMonthSelected
                        )
                    }
                }
                selectedReport == ReportType.MONTHLY_SALES ||
                    selectedReport == ReportType.PRODUCT_WISE_SALES -> {
                    MonthSelectorBar(
                        yearMonth = selectedYearMonth,
                        onYearMonthChanged = onYearMonthSelected
                    )
                }
                else -> {
                    DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
                }
            }

            // Staff / Vehicle filter for Daily Vehicle, Staff Product Sales & Customer Pending Reports
            if (selectedReport == ReportType.DAILY_VEHICLE ||
                selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                selectedReport == ReportType.CUSTOMER_PENDING
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = vehicleFilterId == null,
                        onClick = {
                            vehicleFilterId = null
                            onSelectVehicle(null)
                        },
                        label = {
                            Text(
                                if (selectedReport == ReportType.STAFF_PRODUCT_SALES) {
                                    "All Staff (${vehicles.size})"
                                } else {
                                    "All ${vehicles.size} Vehicles"
                                }
                            )
                        },
                        modifier = Modifier.testTag("report_vehicle_all")
                    )
                    vehicles.forEach { v ->
                        FilterChip(
                            selected = vehicleFilterId == v.id,
                            onClick = {
                                vehicleFilterId = v.id
                                onSelectVehicle(v.id)
                            },
                            label = { Text(v.name) }
                        )
                    }
                }
            }

            // Export Actions (PDF Save/Share & Excel Save/Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { savePdfLauncher.launch("${selectedReport.name.lowercase()}_$selectedDate.pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_pdf_save")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save PDF")
                }
                OutlinedButton(
                    onClick = { shareFile(isPdf = true) },
                    modifier = Modifier.testTag("export_pdf_share")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = { saveExcelLauncher.launch("${selectedReport.name.lowercase()}_$selectedDate.xlsx") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_excel_save")
                ) {
                    Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Excel")
                }
                OutlinedButton(
                    onClick = { shareFile(isPdf = false) },
                    modifier = Modifier.testTag("export_excel_share")
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Share Excel", modifier = Modifier.size(18.dp))
                }
            }
            if (!isReadOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveReportCloudAndDrive(selectedReport, table, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_save_cloud_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Cloud Snapshot")
                    }
                    OutlinedButton(
                        onClick = { onSaveReportCloudAndDrive(selectedReport, table, true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_save_drive_button")
                    ) {
                        Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload PDF to Drive")
                    }
                }
            }
        }

        // Report Preview Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = table.reportTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = table.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable Multi-Column Report Table
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    val hasWideSecondCol = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                        selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
                        selectedReport == ReportType.PRODUCT_WISE_SALES ||
                        selectedReport == ReportType.FULL_EMPTY_STOCK
                    fun colWidth(idx: Int) = when {
                        idx == 0 -> 150.dp
                        idx == 1 && hasWideSecondCol -> 148.dp
                        else -> 86.dp
                    }
                    Row(
                        modifier = Modifier
                            .background(BrandGreenContainer)
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        table.headers.forEachIndexed { idx, header ->
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenOnContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .width(colWidth(idx))
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                itemsIndexed(table.rows) { rowIdx, row ->
                    val hasWideSecondCol = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                        selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
                        selectedReport == ReportType.PRODUCT_WISE_SALES ||
                        selectedReport == ReportType.FULL_EMPTY_STOCK
                    val bg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    Row(
                        modifier = Modifier
                            .background(bg)
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        row.forEachIndexed { idx, value ->
                            val w = when {
                                idx == 0 -> 150.dp
                                idx == 1 && hasWideSecondCol -> 148.dp
                                else -> 86.dp
                            }
                            val isNumericCol = idx > if (hasWideSecondCol) 1 else 0
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = if (isNumericCol) JetBrainsMono else MaterialTheme.typography.bodySmall.fontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .width(w)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                table.footerTotals?.let { totals ->
                    item {
                        val hasWideSecondCol = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                            selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
                            selectedReport == ReportType.PRODUCT_WISE_SALES ||
                            selectedReport == ReportType.FULL_EMPTY_STOCK
                        Row(
                            modifier = Modifier
                                .background(BrandRedContainer)
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            totals.forEachIndexed { idx, value ->
                                val w = when {
                                    idx == 0 -> 150.dp
                                    idx == 1 && hasWideSecondCol -> 148.dp
                                    else -> 86.dp
                                }
                                val isNumericCol = idx > if (hasWideSecondCol) 1 else 0
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = if (isNumericCol) JetBrainsMono else MaterialTheme.typography.labelMedium.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRedOnContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .width(w)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorBar(
    yearMonth: String,
    onYearMonthChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onYearMonthChanged(shiftMonth(yearMonth, -1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = formatYearMonth(yearMonth),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMono
            )
            IconButton(onClick = { onYearMonthChanged(shiftMonth(yearMonth, 1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
    }
}

private fun shiftMonth(ym: String, delta: Int): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = fmt.parse(ym)!!
        cal.add(Calendar.MONTH, delta)
        fmt.format(cal.time)
    } catch (_: Exception) {
        ym
    }
}

private fun formatYearMonth(ym: String): String {
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val outFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        outFmt.format(inFmt.parse(ym)!!)
    } catch (_: Exception) {
        ym
    }
}
