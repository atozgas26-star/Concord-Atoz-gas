package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.StockCalculator
import com.example.data.model.UserRole
import com.example.ui.components.InsufficientStockWarningDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.StoreManagementScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.screens.VehicleManagementScreen
import com.example.ui.theme.ConcordGasTheme
import com.example.ui.viewmodel.AppViewModel

private enum class AdminNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    STORE("Store", Icons.Filled.Store, Icons.Outlined.Store),
    VEHICLES("Vehicles", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ConcordGasApp
        setContent {
            ConcordGasTheme {
                val viewModel: AppViewModel = viewModel(
                    factory = AppViewModel.Factory(
                        repository = app.container.repository,
                        backupManager = app.container.backupManager
                    )
                )
                ConcordGasRootApp(viewModel)
            }
        }
    }
}

@Composable
fun ConcordGasRootApp(viewModel: AppViewModel) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val storeRows by viewModel.storeRows.collectAsStateWithLifecycle()
    val selectedVehicleRows by viewModel.selectedVehicleRows.collectAsStateWithLifecycle()
    val staffVehicleRows by viewModel.staffVehicleRows.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactionDisplayItems.collectAsStateWithLifecycle()
    val warningAlert by viewModel.warningAlert.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Subscribe to report flows so report tables stay live
    viewModel.monthlySalesReport.collectAsStateWithLifecycle()
    viewModel.productWiseSales.collectAsStateWithLifecycle()
    viewModel.staffWiseSales.collectAsStateWithLifecycle()
    viewModel.companyStockReport.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = authSession
    if (session == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                LoginScreen(
                    users = users,
                    loginError = loginError,
                    onLogin = viewModel::login,
                    onClearError = viewModel::clearLoginError
                )
            }
        }
    } else if (session.user.role == UserRole.STAFF) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                StaffDashboardScreen(
                    staffUser = session.user,
                    assignedVehicle = session.assignedVehicle,
                    selectedDate = selectedDate,
                    vehicleRows = staffVehicleRows,
                    storeRows = storeRows,
                    products = activeProducts,
                    allTransactions = allTransactions,
                    onDateSelected = viewModel::setSelectedDate,
                    onRecordTransaction = viewModel::recordTransaction,
                    onLogout = viewModel::logout
                )
            }
        }
    } else {
        var currentTab by rememberSaveable { mutableStateOf(AdminNavTab.DASHBOARD) }
        var viewingVehicleDetail by rememberSaveable { mutableStateOf(false) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && !viewingVehicleDetail) {
                        NavigationBar {
                            AdminNavTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        currentTab = tab
                                        viewingVehicleDetail = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !viewingVehicleDetail) {
                        NavigationRail(modifier = Modifier.statusBarsPadding()) {
                            AdminNavTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        currentTab = tab
                                        viewingVehicleDetail = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (viewingVehicleDetail) {
                            val activeVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
                            VehicleDetailScreen(
                                vehicle = activeVehicle,
                                selectedDate = selectedDate,
                                rows = selectedVehicleRows,
                                storeRows = storeRows,
                                products = activeProducts,
                                onDateSelected = viewModel::setSelectedDate,
                                onBack = { viewingVehicleDetail = false },
                                onRecordTransaction = viewModel::recordTransaction,
                                onUpdateVehicleOpening = { vId, pId, oldFull, oldEmpty ->
                                    viewModel.updateOpeningStock(
                                        scope = StockCalculator.SCOPE_VEHICLE,
                                        vehicleId = vId,
                                        productId = pId,
                                        openingFull = oldFull,
                                        openingEmpty = oldEmpty
                                    )
                                }
                            )
                        } else {
                            when (currentTab) {
                                AdminNavTab.DASHBOARD -> AdminDashboardScreen(
                                    adminName = session.user.displayName,
                                    selectedDate = selectedDate,
                                    summary = dashboardSummary,
                                    onDateSelected = viewModel::setSelectedDate,
                                    onOpenStore = { currentTab = AdminNavTab.STORE },
                                    onOpenVehicleDetail = { vehicleId ->
                                        viewModel.selectVehicle(vehicleId)
                                        currentTab = AdminNavTab.VEHICLES
                                        viewingVehicleDetail = true
                                    },
                                    onOpenReports = { currentTab = AdminNavTab.REPORTS },
                                    onLogout = viewModel::logout
                                )

                                AdminNavTab.STORE -> StoreManagementScreen(
                                    selectedDate = selectedDate,
                                    storeRows = storeRows,
                                    products = activeProducts,
                                    vehicles = vehicles,
                                    transactions = allTransactions,
                                    onDateSelected = viewModel::setSelectedDate,
                                    onRecordTransaction = viewModel::recordTransaction,
                                    onUpdateOpeningStock = { productId, full, empty ->
                                        viewModel.updateOpeningStock(
                                            scope = StockCalculator.SCOPE_STORE,
                                            vehicleId = 0L,
                                            productId = productId,
                                            openingFull = full,
                                            openingEmpty = empty
                                        )
                                    },
                                    onDeleteTransaction = viewModel::deleteTransaction
                                )

                                AdminNavTab.VEHICLES -> VehicleManagementScreen(
                                    selectedDate = selectedDate,
                                    vehicleOverviews = dashboardSummary?.vehicleOverviews.orEmpty(),
                                    onDateSelected = viewModel::setSelectedDate,
                                    onSelectVehicle = { vehicleId ->
                                        viewModel.selectVehicle(vehicleId)
                                        viewingVehicleDetail = true
                                    }
                                )

                                AdminNavTab.REPORTS -> ReportsScreen(
                                    selectedDate = selectedDate,
                                    selectedYearMonth = selectedYearMonth,
                                    vehicles = vehicles,
                                    selectedVehicleId = selectedVehicleId,
                                    buildReportTable = viewModel::buildReportTable,
                                    onDateSelected = viewModel::setSelectedDate,
                                    onYearMonthSelected = viewModel::setSelectedYearMonth,
                                    onSelectVehicle = { id -> if (id != null) viewModel.selectVehicle(id) },
                                    onMessage = viewModel::showMessage
                                )

                                AdminNavTab.SETTINGS -> AdminSettingsScreen(
                                    users = users,
                                    products = allProducts,
                                    onUpdatePin = viewModel::updateUserPin,
                                    onAddProduct = viewModel::addProduct,
                                    onToggleProductActive = viewModel::toggleProductActive,
                                    onExportBackupJson = viewModel::exportBackupJson,
                                    onRestoreBackupJson = viewModel::restoreBackupJson,
                                    onMessage = viewModel::showMessage,
                                    onLogout = viewModel::logout
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    warningAlert?.let { alert ->
        InsufficientStockWarningDialog(
            alert = alert,
            onDismiss = viewModel::dismissWarningAlert
        )
    }
}
