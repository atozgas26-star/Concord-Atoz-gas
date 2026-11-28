package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.ProductCategoryEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ExchangePeriodFilter
import com.example.data.model.ExchangeSectionFilter
import com.example.data.model.ProductCategory
import com.example.data.model.ReportType
import com.example.export.ReportExporter
import com.example.export.ReportTable
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.components.CategoryFolderGrid
import com.example.ui.components.CategoryFolderStat
import com.example.ui.components.DateSelectorBar
import com.example.ui.components.DynamicCategoryFolderGrid
import com.example.ui.components.DynamicCategoryFolderStat
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.OpenedCategoryFolderHeader
import com.example.ui.components.OpenedDynamicCategoryFolderHeader
import com.example.ui.components.RoleHeaderPalettes
import com.example.ui.components.SignatureAccentHairline
import com.example.ui.components.luxuryTapScale
import com.example.ui.components.obsidianRoyalAtmosphere
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.DarkGreenContainer
import com.example.ui.theme.DarkGreenOnContainer
import com.example.ui.theme.DarkRedContainer
import com.example.ui.theme.DarkRedOnContainer
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.ElectricBlueOnSoft
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeOnSoft
import com.example.ui.theme.MetallicOrangeSoft
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.RoyalElectricBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(
    selectedDate: String,
    selectedYearMonth: String,
    vehicles: List<VehicleEntity>,
    categories: List<ProductCategoryEntity> = emptyList(),
    selectedVehicleId: Long?,
    recentPurchaseCompanies: List<String> = emptyList(),
    isReadOnly: Boolean = false,
    initialReportType: ReportType = ReportType.DAILY_STORE,
    buildReportTable: (ReportType, Long?, Boolean, String?) -> ReportTable,
    buildExchangeReportTable: ((Long?, ExchangePeriodFilter, ExchangeSectionFilter) -> ReportTable)? = null,
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
    var exchangePeriod by remember { mutableStateOf(ExchangePeriodFilter.MONTHLY) }
    var exchangeSection by remember { mutableStateOf(ExchangeSectionFilter.ALL) }
    var purchaseCompanyFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember(selectedReport, vehicleFilterId) { mutableStateOf<ProductCategory?>(null) }
    var selectedDynamicCategoryId by remember(selectedReport, vehicleFilterId) { mutableStateOf<Long?>(null) }
    var showAllReportRows by remember(selectedReport, vehicleFilterId) { mutableStateOf(false) }

    val sortedCategories = remember(categories) {
        categories.filter { it.isActive }.sortedWith(compareBy<ProductCategoryEntity> { it.sortOrder }.thenBy { it.id })
    }
    val useDynamicFolders = sortedCategories.isNotEmpty()
    val openedDynamicCategory = remember(sortedCategories, selectedDynamicCategoryId) {
        sortedCategories.firstOrNull { it.id == selectedDynamicCategoryId }
    }

    val baseTable = if (selectedReport == ReportType.LPG_EXCHANGE_REPORT && buildExchangeReportTable != null) {
        buildExchangeReportTable(vehicleFilterId, exchangePeriod, exchangeSection)
    } else {
        buildReportTable(selectedReport, vehicleFilterId, purchaseDailyMode, purchaseCompanyFilter)
    }

    val supportsCategoryFolders = remember(baseTable, sortedCategories) {
        baseTable.rows.any { row ->
            detectRowDynamicCategory(row, sortedCategories) != null || detectRowCategory(row) != null
        }
    }

    val dynamicFolderStats = remember(baseTable, supportsCategoryFolders, selectedReport, sortedCategories) {
        if (!supportsCategoryFolders || !useDynamicFolders) emptyList()
        else sortedCategories.map { cat ->
            val matching = baseTable.rows.filter { detectRowDynamicCategory(it, sortedCategories)?.id == cat.id }
            val (fullTotal, emptyTotal) = computeFolderPreviewTotals(selectedReport, matching)
            DynamicCategoryFolderStat(
                category = cat,
                itemCount = matching.size,
                fullTotal = fullTotal,
                emptyTotal = emptyTotal
            )
        }
    }

    val folderStats = remember(baseTable, supportsCategoryFolders, selectedReport) {
        if (!supportsCategoryFolders) emptyList()
        else ProductCategory.activeEntries.map { cat ->
            val matching = baseTable.rows.filter { detectRowCategory(it) == cat }
            val (fullTotal, emptyTotal) = computeFolderPreviewTotals(selectedReport, matching)
            CategoryFolderStat(
                category = cat,
                itemCount = matching.size,
                fullTotal = fullTotal,
                emptyTotal = emptyTotal
            )
        }
    }

    val table = remember(baseTable, selectedCategory, openedDynamicCategory, useDynamicFolders, sortedCategories) {
        when {
            useDynamicFolders && openedDynamicCategory != null ->
                filterReportTableByDynamicCategory(baseTable, openedDynamicCategory, sortedCategories)
            !useDynamicFolders && selectedCategory != null ->
                filterReportTableByCategory(baseTable, selectedCategory!!)
            else -> baseTable
        }
    }

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
    val hasWideSecondCol = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
        selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
        selectedReport == ReportType.PRODUCT_WISE_SALES ||
        selectedReport == ReportType.FULL_EMPTY_STOCK

    fun colWidth(idx: Int) = when {
        idx == 0 -> 150.dp
        idx == 1 && hasWideSecondCol -> 148.dp
        else -> 86.dp
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = if (isReadOnly) "Reports (View Only)" else "Reports & Export",
            subtitle = "Concord & Atoz Gas Trading LLC • Category Folders, PDF & Excel",
            headerGradient = if (isReadOnly) RoleHeaderPalettes.OwnerGradient else RoleHeaderPalettes.AdminGradient,
            accentBarColor = if (isReadOnly) RoleHeaderPalettes.OwnerAccent else RoleHeaderPalettes.AdminAccent
        )

        // Single smooth vertical LazyColumn containing controls, Category Folder Grid, and full report table
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reports_smooth_scroll_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "report_type_chips") {
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
            }

            // Period Toggle (Daily / Weekly / Monthly for LPG Exchange; Daily / Monthly for Sales/Purchase)
            val supportsDailyMonthlyToggle = selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT ||
                selectedReport == ReportType.PURCHASE_REGISTER ||
                selectedReport == ReportType.STAFF_WISE_SALES

            if (selectedReport == ReportType.LPG_EXCHANGE_REPORT) {
                item(key = "exchange_report_period_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExchangePeriodFilter.entries.forEach { period ->
                            FilterChip(
                                selected = exchangePeriod == period,
                                onClick = {
                                    exchangePeriod = period
                                    purchaseDailyMode = (period == ExchangePeriodFilter.DAILY)
                                },
                                label = { Text("${period.label} Report") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (period == ExchangePeriodFilter.WEEKLY) RoyalElectricBlue else BrandGreenPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("exchange_period_${period.name.lowercase()}")
                            )
                        }
                    }
                }
                item(key = "exchange_report_section_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExchangeSectionFilter.entries.forEach { sec ->
                            FilterChip(
                                selected = exchangeSection == sec,
                                onClick = { exchangeSection = sec },
                                label = { Text(sec.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MetallicOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("exchange_section_${sec.name.lowercase()}")
                            )
                        }
                    }
                }
            } else if (supportsDailyMonthlyToggle) {
                item(key = "report_period_toggle") {
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
            }

            // Supplier / Purchase Company filter chips for Purchase reports
            if ((selectedReport == ReportType.PURCHASE_REGISTER || selectedReport == ReportType.PURCHASE_COMPANY_PRODUCT) &&
                recentPurchaseCompanies.isNotEmpty()
            ) {
                item(key = "report_supplier_chips") {
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
            }

            item(key = "report_date_or_month") {
                when {
                    selectedReport == ReportType.LPG_EXCHANGE_REPORT -> {
                        if (exchangePeriod == ExchangePeriodFilter.MONTHLY) {
                            MonthSelectorBar(
                                yearMonth = selectedYearMonth,
                                onYearMonthChanged = onYearMonthSelected
                            )
                        } else {
                            DateSelectorBar(selectedDate = selectedDate, onDateSelected = onDateSelected)
                        }
                    }
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
            }

            // Staff / Vehicle filter for Daily Vehicle, LPG Exchange, Staff Product Sales & Customer Pending Reports
            if (selectedReport == ReportType.DAILY_VEHICLE ||
                selectedReport == ReportType.LPG_EXCHANGE_REPORT ||
                selectedReport == ReportType.STAFF_PRODUCT_SALES ||
                selectedReport == ReportType.CUSTOMER_PENDING
            ) {
                item(key = "report_vehicle_chips") {
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
            }

            // Compact Small-List Style Category Folders for instant category-wise report view & export
            if (supportsCategoryFolders) {
                item(key = "report_category_folders") {
                    val trailingAllAction: (@Composable () -> Unit)? = if (!showAllReportRows && baseTable.rows.isNotEmpty()) {
                        {
                            TextButton(
                                onClick = { showAllReportRows = true },
                                modifier = Modifier.testTag("report_view_all_rows_button")
                            ) {
                                Text(
                                    text = "All (${baseTable.rows.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else null
                    if (useDynamicFolders) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DynamicCategoryFolderGrid(
                                stats = dynamicFolderStats,
                                selectedCategoryId = selectedDynamicCategoryId,
                                onSelectCategoryId = { id ->
                                    selectedDynamicCategoryId = id
                                    selectedCategory = sortedCategories.firstOrNull { it.id == id }?.legacyEnum
                                    if (id != null) showAllReportRows = false
                                },
                                sectionTitle = "Report Category Folders"
                            )
                            if (trailingAllAction != null && selectedDynamicCategoryId == null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    trailingAllAction()
                                }
                            }
                        }
                    } else {
                        CategoryFolderGrid(
                            stats = folderStats,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { cat ->
                                selectedCategory = cat
                                if (cat != null) showAllReportRows = false
                            },
                            sectionTitle = "Report Category Folders",
                            headerTrailingAction = trailingAllAction
                        )
                    }
                }
            }

            // Compact Small-Icon Export Toolbar (PDF Save/Share, Excel Save/Share, Cloud & Drive)
            item(key = "report_export_actions") {
                LuxuryGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    accentGlow = RoyalElectricBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactReportIconAction(
                            icon = Icons.Default.PictureAsPdf,
                            label = "PDF",
                            contentDescription = "Save PDF",
                            accentColor = MetallicOrange,
                            filled = true,
                            onClick = { savePdfLauncher.launch("${selectedReport.name.lowercase()}_$selectedDate.pdf") },
                            modifier = Modifier.testTag("export_pdf_save")
                        )
                        CompactReportIconAction(
                            icon = Icons.Default.Share,
                            label = "Share",
                            contentDescription = "Share PDF",
                            accentColor = MetallicOrange,
                            filled = false,
                            onClick = { shareFile(isPdf = true) },
                            modifier = Modifier.testTag("export_pdf_share")
                        )
                        CompactReportIconAction(
                            icon = Icons.Default.TableView,
                            label = "Excel",
                            contentDescription = "Save Excel",
                            accentColor = RoyalElectricBlue,
                            filled = true,
                            onClick = { saveExcelLauncher.launch("${selectedReport.name.lowercase()}_$selectedDate.xlsx") },
                            modifier = Modifier.testTag("export_excel_save")
                        )
                        CompactReportIconAction(
                            icon = Icons.Default.Download,
                            label = "Share",
                            contentDescription = "Share Excel",
                            accentColor = RoyalElectricBlue,
                            filled = false,
                            onClick = { shareFile(isPdf = false) },
                            modifier = Modifier.testTag("export_excel_share")
                        )
                        if (!isReadOnly) {
                            CompactReportIconAction(
                                icon = Icons.Default.CloudUpload,
                                label = "Cloud",
                                contentDescription = "Save Cloud Snapshot",
                                accentColor = MaterialTheme.colorScheme.primary,
                                filled = false,
                                onClick = { onSaveReportCloudAndDrive(selectedReport, table, false) },
                                modifier = Modifier.testTag("report_save_cloud_button")
                            )
                            CompactReportIconAction(
                                icon = Icons.Default.AddToDrive,
                                label = "Drive",
                                contentDescription = "Upload PDF to Drive",
                                accentColor = MaterialTheme.colorScheme.primary,
                                filled = false,
                                onClick = { onSaveReportCloudAndDrive(selectedReport, table, true) },
                                modifier = Modifier.testTag("report_save_drive_button")
                            )
                        }
                    }
                }
            }

            // Report Preview: Keep product rows neatly inside Category Folders until opened
            val hasOpenFolder = if (useDynamicFolders) openedDynamicCategory != null else selectedCategory != null
            val showTable = !supportsCategoryFolders || hasOpenFolder || showAllReportRows
            if (showTable) {
                if (useDynamicFolders && openedDynamicCategory != null) {
                    item(key = "report_opened_dynamic_${openedDynamicCategory.id}") {
                        OpenedDynamicCategoryFolderHeader(
                            category = openedDynamicCategory,
                            itemCount = table.rows.size,
                            subtitle = "${selectedReport.title} • ${table.rows.size} items inside folder",
                            onClose = {
                                selectedDynamicCategoryId = null
                                selectedCategory = null
                            }
                        )
                    }
                } else selectedCategory?.let { openCat ->
                    item(key = "report_opened_folder_header_${openCat.name}") {
                        OpenedCategoryFolderHeader(
                            category = openCat,
                            itemCount = table.rows.size,
                            subtitle = "${selectedReport.title} • ${table.rows.size} items inside folder",
                            onClose = { selectedCategory = null }
                        )
                    }
                }
                if (supportsCategoryFolders && !hasOpenFolder && showAllReportRows) {
                    item(key = "report_collapse_all_bar") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing all ${baseTable.rows.size} items across folders",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showAllReportRows = false }) {
                                Text("Back to Folders", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Report Preview Header + Horizontally Scrollable Table Card
                item(key = "report_preview_table") {
                    LuxuryGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        accentGlow = RoyalElectricBlue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .obsidianRoyalAtmosphere()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = table.reportTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = table.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.TextSecondaryDark
                                )
                            }
                            SignatureAccentHairline(height = 2.dp)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Column {
                                val isDarkTable = LocalIsDarkTheme.current
                                Row(
                                    modifier = Modifier
                                        .background(if (isDarkTable) ObsidianElevated else ElectricBlueSoft)
                                        .padding(vertical = 9.dp, horizontal = 8.dp)
                                ) {
                                    table.headers.forEachIndexed { idx, header ->
                                        Text(
                                            text = header,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTable) ElectricBlueGlow else ElectricBlueOnSoft,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .width(colWidth(idx))
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }

                                if (table.rows.isEmpty()) {
                                    Text(
                                        text = "No entries in this category folder for the selected period.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    table.rows.forEachIndexed { rowIdx, row ->
                                        val bg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                        Row(
                                            modifier = Modifier
                                                .background(bg)
                                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                        ) {
                                            row.forEachIndexed { idx, value ->
                                                val isNumericCol = idx > if (hasWideSecondCol) 1 else 0
                                                Text(
                                                    text = value,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = if (isNumericCol) JetBrainsMono else MaterialTheme.typography.bodySmall.fontFamily,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .width(colWidth(idx))
                                                        .padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                table.footerTotals?.let { totals ->
                                    val isDarkFooter = LocalIsDarkTheme.current
                                    Row(
                                        modifier = Modifier
                                            .background(if (isDarkFooter) ObsidianMidnight else MetallicOrangeSoft)
                                            .padding(vertical = 10.dp, horizontal = 8.dp)
                                    ) {
                                        totals.forEachIndexed { idx, value ->
                                            val isNumericCol = idx > if (hasWideSecondCol) 1 else 0
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = if (isNumericCol) JetBrainsMono else MaterialTheme.typography.labelMedium.fontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkFooter) MetallicOrangeBright else MetallicOrangeOnSoft,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .width(colWidth(idx))
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
            }

            item(key = "report_bottom_spacer") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

private fun detectRowCategory(row: List<String>): ProductCategory? {
    for (cell in row.take(4)) {
        val trimmed = cell.trim()
        ProductCategory.entries.firstOrNull { cat ->
            trimmed.equals(cat.displayName, ignoreCase = true) ||
                trimmed.startsWith("${cat.shortCode} -", ignoreCase = true)
        }?.let { return it }
    }
    return null
}

private fun computeFolderPreviewTotals(
    reportType: ReportType,
    rows: List<List<String>>
): Pair<Int, Int> {
    if (rows.isEmpty()) return 0 to 0
    return when (reportType) {
        ReportType.DAILY_STORE -> {
            val fullBal = rows.sumOf { it.getOrNull(5)?.toIntOrNull() ?: 0 }
            val empBal = rows.sumOf { it.getOrNull(10)?.toIntOrNull() ?: 0 }
            fullBal to empBal
        }
        ReportType.DAILY_VEHICLE -> {
            val fullBal = rows.sumOf { it.getOrNull(5)?.toIntOrNull() ?: 0 }
            val empBal = rows.sumOf { it.getOrNull(9)?.toIntOrNull() ?: 0 }
            fullBal to empBal
        }
        ReportType.FULL_EMPTY_STOCK -> {
            val fullBal = rows.sumOf { it.getOrNull(4)?.toIntOrNull() ?: 0 }
            val empBal = rows.sumOf { it.getOrNull(7)?.toIntOrNull() ?: 0 }
            fullBal to empBal
        }
        ReportType.PRODUCT_WISE_SALES -> {
            val sold = rows.sumOf { it.getOrNull(2)?.toIntOrNull() ?: 0 }
            val emp = rows.sumOf { it.getOrNull(3)?.toIntOrNull() ?: 0 }
            sold to emp
        }
        ReportType.STAFF_PRODUCT_SALES -> {
            val sold = rows.sumOf { it.getOrNull(5)?.toIntOrNull() ?: 0 }
            val emp = rows.sumOf { it.getOrNull(6)?.toIntOrNull() ?: 0 }
            sold to emp
        }
        ReportType.PURCHASE_COMPANY_PRODUCT -> {
            val fullPur = rows.sumOf { it.getOrNull(2)?.toIntOrNull() ?: 0 }
            val empPur = rows.sumOf { it.getOrNull(3)?.toIntOrNull() ?: 0 }
            fullPur to empPur
        }
        else -> {
            val qty = rows.sumOf { it.getOrNull(4)?.toIntOrNull() ?: 0 }
            qty to 0
        }
    }
}

private fun detectRowDynamicCategory(
    row: List<String>,
    categories: List<ProductCategoryEntity>
): ProductCategoryEntity? {
    if (categories.isEmpty()) return null
    for (cell in row.take(4)) {
        val trimmed = cell.trim()
        categories.firstOrNull { cat ->
            trimmed.equals(cat.name, ignoreCase = true) ||
                trimmed.startsWith("${cat.name} -", ignoreCase = true) ||
                trimmed.equals(cat.legacyEnum.displayName, ignoreCase = true) ||
                trimmed.startsWith("${cat.legacyEnum.shortCode} -", ignoreCase = true)
        }?.let { return it }
    }
    return null
}

private fun filterReportTableByDynamicCategory(
    base: ReportTable,
    category: ProductCategoryEntity,
    allCategories: List<ProductCategoryEntity>
): ReportTable {
    val filteredRows = base.rows.filter { detectRowDynamicCategory(it, allCategories)?.id == category.id }
    val recalculatedFooter = base.footerTotals?.mapIndexed { colIdx, origVal ->
        when {
            colIdx == 0 -> "${category.name.take(12).uppercase()} TOTAL"
            origVal.toIntOrNull() != null -> {
                filteredRows.sumOf { row -> row.getOrNull(colIdx)?.toIntOrNull() ?: 0 }.toString()
            }
            origVal.contains("Items", ignoreCase = true) ||
                origVal.contains("Products", ignoreCase = true) ||
                origVal.contains("Cylinders", ignoreCase = true) ||
                origVal.contains("Entries", ignoreCase = true) -> {
                "${filteredRows.size} Items"
            }
            else -> origVal
        }
    }
    return base.copy(
        reportTitle = "${base.reportTitle} • ${category.name}",
        subtitle = "${base.subtitle}  •  Folder: ${category.name} (${filteredRows.size} rows)",
        rows = filteredRows,
        footerTotals = recalculatedFooter
    )
}

private fun filterReportTableByCategory(
    base: ReportTable,
    category: ProductCategory
): ReportTable {
    val filteredRows = base.rows.filter { detectRowCategory(it) == category }
    val recalculatedFooter = base.footerTotals?.mapIndexed { colIdx, origVal ->
        when {
            colIdx == 0 -> "${category.shortCode} TOTAL"
            origVal.toIntOrNull() != null -> {
                filteredRows.sumOf { row -> row.getOrNull(colIdx)?.toIntOrNull() ?: 0 }.toString()
            }
            origVal.contains("Items", ignoreCase = true) ||
                origVal.contains("Products", ignoreCase = true) ||
                origVal.contains("Cylinders", ignoreCase = true) ||
                origVal.contains("Entries", ignoreCase = true) -> {
                "${filteredRows.size} Items"
            }
            else -> origVal
        }
    }
    return base.copy(
        reportTitle = "${base.reportTitle} • ${category.displayName}",
        subtitle = "${base.subtitle}  •  Folder: ${category.displayName} (${filteredRows.size} rows)",
        rows = filteredRows,
        footerTotals = recalculatedFooter
    )
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

@Composable
private fun CompactReportIconAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    accentColor: Color,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .luxuryTapScale()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (filled) accentColor else accentColor.copy(alpha = 0.12f),
        border = if (filled) null else BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) Color.White else accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (filled) Color.White else accentColor,
                maxLines = 1
            )
        }
    }
}

