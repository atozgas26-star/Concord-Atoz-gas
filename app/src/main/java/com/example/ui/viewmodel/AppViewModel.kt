package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProductEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AuthSession
import com.example.data.model.CompanyStockRow
import com.example.data.model.CustomerPendingRow
import com.example.data.model.DashboardSummary
import com.example.data.model.MonthlyDailySaleRow
import com.example.data.model.OperationResult
import com.example.data.model.ProductCategory
import com.example.data.model.ProductSalesRow
import com.example.data.model.ReportType
import com.example.data.model.StaffSalesRow
import com.example.data.model.StockCalculator
import com.example.data.model.StoreStockRow
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.model.VehicleStockRow
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.WorkspaceConnectionState
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.ReportTable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WarningAlert(
    val title: String,
    val message: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: InventoryRepository,
    private val backupManager: BackupManager,
    private val cloudSync: CloudSyncRepository? = null,
    private val sessionPreferences: SessionPreferences? = null
) : ViewModel() {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _authSession = MutableStateFlow<AuthSession?>(null)
    val authSession: StateFlow<AuthSession?> = _authSession.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _selectedDate = MutableStateFlow(InventoryRepository.todayIsoDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow(InventoryRepository.currentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow<Long?>(null)
    val selectedVehicleId: StateFlow<Long?> = _selectedVehicleId.asStateFlow()

    private val _warningAlert = MutableStateFlow<WarningAlert?>(null)
    val warningAlert: StateFlow<WarningAlert?> = _warningAlert.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val workspaceConnectionState: StateFlow<WorkspaceConnectionState> =
        cloudSync?.connectionState ?: MutableStateFlow(WorkspaceConnectionState.Checking).asStateFlow()

    private val _cloudActionBusy = MutableStateFlow(false)
    val cloudActionBusy: StateFlow<Boolean> = _cloudActionBusy.asStateFlow()

    val users: StateFlow<List<UserAccountEntity>> = repository.usersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles: StateFlow<List<VehicleEntity>> = repository.vehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductEntity>> = repository.activeProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary?> = _selectedDate
        .flatMapLatest { date -> repository.observeDashboardSummary(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storeRows: StateFlow<List<StoreStockRow>> = _selectedDate
        .flatMapLatest { date -> repository.observeStoreStock(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleRows: StateFlow<List<VehicleStockRow>> = combine(_selectedVehicleId, _selectedDate) { vId, date ->
        vId to date
    }.flatMapLatest { (vId, date) ->
        if (vId == null) flowOf(emptyList()) else repository.observeVehicleStock(vId, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffVehicleRows: StateFlow<List<VehicleStockRow>> = combine(_authSession, _selectedDate) { session, date ->
        session?.assignedVehicle?.id to date
    }.flatMapLatest { (vId, date) ->
        if (vId == null) flowOf(emptyList()) else repository.observeVehicleStock(vId, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionDisplayItems: StateFlow<List<TransactionDisplayItem>> = combine(
        repository.transactionsFlow,
        repository.allProductsFlow
    ) { txs, products ->
        val pMap = products.associateBy { it.id }
        txs.mapNotNull { tx ->
            val product = pMap[tx.productId] ?: return@mapNotNull null
            TransactionDisplayItem(
                transaction = tx,
                productName = product.name,
                productCategory = product.category
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPurchaseCompanies: StateFlow<List<String>> = repository.recentPurchaseCompaniesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCustomerNames: StateFlow<List<String>> = repository.recentCustomerNamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomerPendingRows: StateFlow<List<CustomerPendingRow>> = repository.observeCustomerPendingBalances(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehiclePendingRows: StateFlow<List<CustomerPendingRow>> = _selectedVehicleId
        .flatMapLatest { vId ->
            if (vId == null) flowOf(emptyList()) else repository.observeCustomerPendingBalances(vId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffVehiclePendingRows: StateFlow<List<CustomerPendingRow>> = _authSession
        .flatMapLatest { session ->
            val vId = session?.assignedVehicle?.id
            if (vId == null) flowOf(emptyList()) else repository.observeCustomerPendingBalances(vId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySalesReport: StateFlow<List<MonthlyDailySaleRow>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.observeMonthlySalesReport(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productWiseSales: StateFlow<List<ProductSalesRow>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.observeProductWiseSales(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffWiseSales: StateFlow<List<StaffSalesRow>> = combine(_selectedYearMonth, _selectedDate) { ym, date ->
        ym to date
    }.flatMapLatest { (ym, date) ->
        repository.observeStaffWiseSales(ym, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companyStockReport: StateFlow<List<CompanyStockRow>> = _selectedDate
        .flatMapLatest { date -> repository.observeCompanyStockReport(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            val savedUserId = sessionPreferences?.getLoggedInUserId()
            if (savedUserId != null) {
                val restored = repository.getSessionForUserId(savedUserId)
                if (restored != null) {
                    _authSession.value = restored
                    if (restored.assignedVehicle != null) {
                        _selectedVehicleId.value = restored.assignedVehicle.id
                    }
                } else {
                    sessionPreferences.clearLoggedInUserId()
                }
            }
            _isInitialized.value = true
            cloudSync?.connectForSignedInUser()
        }
    }

    fun retryCloudConnect() {
        viewModelScope.launch {
            _cloudActionBusy.value = true
            val res = cloudSync?.connectForSignedInUser()
            _cloudActionBusy.value = false
            if (res?.isFailure == true) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Cloud connection failed."
            }
        }
    }

    fun joinWorkspace(inviteCode: String) {
        viewModelScope.launch {
            _cloudActionBusy.value = true
            val res = cloudSync?.joinWorkspaceWithInviteCode(inviteCode)
            _cloudActionBusy.value = false
            if (res?.isSuccess == true) {
                _snackbarMessage.value = "Joined Concord & A to Z Gas Online Sync!"
            } else if (res?.isFailure == true) {
                _warningAlert.value = WarningAlert(
                    "Could Not Join Workspace",
                    res.exceptionOrNull()?.localizedMessage ?: "Invalid or inactive invite code."
                )
            }
        }
    }

    fun createStandaloneWorkspace() {
        viewModelScope.launch {
            _cloudActionBusy.value = true
            val res = cloudSync?.createStandaloneOrCompanyWorkspace()
            _cloudActionBusy.value = false
            if (res?.isSuccess == true) {
                _snackbarMessage.value = "Cloud workspace ready!"
            } else if (res?.isFailure == true) {
                _warningAlert.value = WarningAlert(
                    "Workspace Error",
                    res.exceptionOrNull()?.localizedMessage ?: "Could not create cloud workspace."
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _cloudActionBusy.value = true
            val res = cloudSync?.pushAllLocalDataToCloud()
            _cloudActionBusy.value = false
            if (res?.isSuccess == true) {
                _snackbarMessage.value = "All data synced to cloud."
            } else if (res?.isFailure == true) {
                _snackbarMessage.value = "Sync failed: ${res.exceptionOrNull()?.localizedMessage ?: "Offline"}"
            }
        }
    }

    fun stopCloudSyncBeforeSignOut() {
        _authSession.value = null
        viewModelScope.launch { sessionPreferences?.clearLoggedInUserId() }
        cloudSync?.stopSync()
    }

    override fun onCleared() {
        super.onCleared()
        cloudSync?.stopSync()
    }

    fun login(userId: Long, pin: String) {
        viewModelScope.launch {
            val session = repository.authenticate(userId, pin)
            if (session != null) {
                sessionPreferences?.saveLoggedInUserId(session.user.id)
                _authSession.value = session
                _loginError.value = null
                _selectedDate.value = InventoryRepository.todayIsoDate()
                if (session.assignedVehicle != null) {
                    _selectedVehicleId.value = session.assignedVehicle.id
                }
            } else {
                _loginError.value = "Invalid PIN for selected account."
            }
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun logout() {
        _authSession.value = null
        _loginError.value = null
        viewModelScope.launch {
            sessionPreferences?.clearLoggedInUserId()
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        if (date.length >= 7) {
            _selectedYearMonth.value = date.substring(0, 7)
        }
    }

    fun setSelectedYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun selectVehicle(vehicleId: Long) {
        _selectedVehicleId.value = vehicleId
    }

    fun dismissWarningAlert() {
        _warningAlert.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun recordTransaction(
        type: TransactionType,
        productId: Long,
        vehicleId: Long?,
        quantity: Int,
        notes: String = "",
        purchaseCompanyName: String = "",
        customerName: String = "",
        emptyCollectedNow: Int = 0
    ) {
        val session = _authSession.value ?: return
        // Enforce strict staff permissions
        if (session.user.role == UserRole.STAFF) {
            val allowedStaffTypes = setOf(
                TransactionType.VEHICLE_FULL_RECEIVED,
                TransactionType.VEHICLE_CUSTOMER_SALE,
                TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
                TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                TransactionType.CUSTOMER_EMPTY_PENDING_ADD
            )
            if (type !in allowedStaffTypes || vehicleId != session.assignedVehicle?.id) {
                _warningAlert.value = WarningAlert(
                    title = "Permission Denied",
                    message = "Staff accounts can only record transactions for their own assigned vehicle."
                )
                return
            }
        }

        viewModelScope.launch {
            val result = if (type == TransactionType.VEHICLE_CUSTOMER_SALE && emptyCollectedNow > 0 && vehicleId != null) {
                repository.recordCustomerSaleWithEmpty(
                    businessDate = _selectedDate.value,
                    productId = productId,
                    vehicleId = vehicleId,
                    actor = session.user,
                    saleQty = quantity,
                    emptyCollectedNow = emptyCollectedNow,
                    customerName = customerName,
                    notes = notes
                )
            } else {
                repository.recordTransaction(
                    businessDate = _selectedDate.value,
                    type = type,
                    productId = productId,
                    vehicleId = vehicleId,
                    actor = session.user,
                    quantity = quantity,
                    notes = notes,
                    purchaseCompanyName = purchaseCompanyName,
                    customerName = customerName
                )
            }
            handleOperationResult(result)
        }
    }

    fun updateOpeningStock(
        scope: String,
        vehicleId: Long,
        productId: Long,
        openingFull: Int,
        openingEmpty: Int
    ) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Staff cannot modify opening stock.")
            return
        }
        viewModelScope.launch {
            val result = repository.setOpeningStockForDate(
                scope = scope,
                vehicleId = vehicleId,
                productId = productId,
                date = _selectedDate.value,
                desiredOpeningFull = openingFull,
                desiredOpeningEmpty = openingEmpty,
                admin = session.user
            )
            handleOperationResult(result)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Staff cannot delete transactions.")
            return
        }
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId, session.user)
            handleOperationResult(result)
        }
    }

    fun updateTransaction(
        transactionId: Long,
        newProductId: Long,
        newQuantity: Int,
        newPurchaseCompanyName: String,
        newCustomerName: String,
        newNotes: String
    ) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can edit recorded transactions.")
            return
        }
        viewModelScope.launch {
            val result = repository.updateTransaction(
                transactionId = transactionId,
                newProductId = newProductId,
                newQuantity = newQuantity,
                newPurchaseCompanyName = newPurchaseCompanyName,
                newCustomerName = newCustomerName,
                newNotes = newNotes,
                admin = session.user
            )
            handleOperationResult(result)
        }
    }

    fun updateUserPin(targetUserId: Long, newPin: String) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val result = repository.updateUserPin(targetUserId, newPin)
            handleOperationResult(result)
        }
    }

    fun updateStaffName(
        targetUserId: Long,
        newDisplayName: String,
        newVehicleName: String? = null
    ) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val result = repository.updateStaffName(targetUserId, newDisplayName, newVehicleName)
            if (result is OperationResult.Success && targetUserId == session.user.id) {
                _authSession.value = session.copy(
                    user = session.user.copy(displayName = newDisplayName.trim().take(100))
                )
            }
            handleOperationResult(result)
        }
    }

    fun addStaffAndVehicle(
        staffName: String,
        vehicleName: String,
        pin: String
    ) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can add new staff logins or vehicles.")
            return
        }
        viewModelScope.launch {
            val result = repository.addStaffAndVehicle(staffName, vehicleName, pin)
            handleOperationResult(result)
        }
    }

    fun addProduct(category: ProductCategory, name: String) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val result = repository.addProduct(category, name)
            handleOperationResult(result)
        }
    }

    fun toggleProductActive(product: ProductEntity) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val result = repository.toggleProductActive(product)
            handleOperationResult(result)
        }
    }

    suspend fun exportBackupJson(): String = backupManager.exportBackupJson()

    fun restoreBackupJson(jsonText: String) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val result = backupManager.restoreBackupJson(jsonText)
            if (result is OperationResult.Success) {
                cloudSync?.pushAllLocalDataToCloud()
            }
            handleOperationResult(result)
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    private fun handleOperationResult(result: OperationResult) {
        when (result) {
            is OperationResult.Success -> _snackbarMessage.value = result.message
            is OperationResult.InsufficientStock -> _warningAlert.value = WarningAlert(result.title, result.message)
            is OperationResult.ValidationError -> _warningAlert.value = WarningAlert("Validation Warning", result.message)
        }
    }

    fun buildReportTable(
        reportType: ReportType,
        vehicleFilterId: Long? = null
    ): ReportTable {
        val date = _selectedDate.value
        val ym = _selectedYearMonth.value
        val actorName = _authSession.value?.user?.displayName ?: "Admin"

        return when (reportType) {
            ReportType.DAILY_STORE -> {
                val rows = storeRows.value
                ReportTable(
                    reportTitle = "Daily Store Report",
                    subtitle = "Business Date: $date",
                    generatedBy = actorName,
                    headers = listOf(
                        "Product", "Op.Full", "Pur.Full", "Inward", "Disp.Full",
                        "Bal.Full", "Op.Empty", "Pur.Empty", "Ret.Empty", "Disp.Empty", "Bal.Empty"
                    ),
                    rows = rows.map { r ->
                        listOf(
                            "${r.product.category.shortCode} - ${r.product.name}",
                            r.openingFull.toString(),
                            r.fullPurchases.toString(),
                            r.fullReturns.toString(),
                            r.fullDispatch.toString(),
                            r.storeFullBalance.toString(),
                            r.openingEmpty.toString(),
                            r.emptyPurchases.toString(),
                            r.emptyReturnsToStore.toString(),
                            r.emptyDispatch.toString(),
                            r.storeEmptyBalance.toString()
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        rows.sumOf { it.openingFull }.toString(),
                        rows.sumOf { it.fullPurchases }.toString(),
                        rows.sumOf { it.fullReturns }.toString(),
                        rows.sumOf { it.fullDispatch }.toString(),
                        rows.sumOf { it.storeFullBalance }.toString(),
                        rows.sumOf { it.openingEmpty }.toString(),
                        rows.sumOf { it.emptyPurchases }.toString(),
                        rows.sumOf { it.emptyReturnsToStore }.toString(),
                        rows.sumOf { it.emptyDispatch }.toString(),
                        rows.sumOf { it.storeEmptyBalance }.toString()
                    )
                )
            }

            ReportType.DAILY_VEHICLE -> {
                val selectedVeh = vehicles.value.firstOrNull { it.id == vehicleFilterId }
                if (selectedVeh != null && vehicleFilterId == _selectedVehicleId.value) {
                    val vRows = selectedVehicleRows.value
                    ReportTable(
                        reportTitle = "Daily Vehicle Report — ${selectedVeh.name}",
                        subtitle = "Business Date: $date",
                        generatedBy = actorName,
                        headers = listOf(
                            "Product", "Old Full", "Recd", "Tot.Full", "Sales", "Full Bal",
                            "Old Emp", "Cust Emp", "Ret.Store", "Emp Bal"
                        ),
                        rows = vRows.map { r ->
                            listOf(
                                "${r.product.category.shortCode} - ${r.product.name}",
                                r.oldFull.toString(),
                                r.fullReceived.toString(),
                                r.totalFull.toString(),
                                r.customerSales.toString(),
                                r.fullBalance.toString(),
                                r.oldEmpty.toString(),
                                r.customerEmptyReceived.toString(),
                                r.emptyReturnedToStore.toString(),
                                r.emptyBalance.toString()
                            )
                        },
                        footerTotals = listOf(
                            "TOTAL",
                            vRows.sumOf { it.oldFull }.toString(),
                            vRows.sumOf { it.fullReceived }.toString(),
                            vRows.sumOf { it.totalFull }.toString(),
                            vRows.sumOf { it.customerSales }.toString(),
                            vRows.sumOf { it.fullBalance }.toString(),
                            vRows.sumOf { it.oldEmpty }.toString(),
                            vRows.sumOf { it.customerEmptyReceived }.toString(),
                            vRows.sumOf { it.emptyReturnedToStore }.toString(),
                            vRows.sumOf { it.emptyBalance }.toString()
                        )
                    )
                } else {
                    val overviews = dashboardSummary.value?.vehicleOverviews.orEmpty()
                    ReportTable(
                        reportTitle = "Daily Vehicle Summary Report (All Vehicles)",
                        subtitle = "Business Date: $date",
                        generatedBy = actorName,
                        headers = listOf(
                            "Vehicle", "Old Full", "Recd", "Tot.Full", "Sales", "Full Bal",
                            "Old Emp", "Cust Emp", "Ret.Store", "Emp Bal"
                        ),
                        rows = overviews.map { v ->
                            listOf(
                                v.vehicle.name,
                                v.totalOldFull.toString(),
                                v.totalFullReceived.toString(),
                                v.totalFull.toString(),
                                v.totalCustomerSales.toString(),
                                v.totalFullBalance.toString(),
                                v.totalOldEmpty.toString(),
                                v.totalCustomerEmptyReceived.toString(),
                                v.totalEmptyReturnedToStore.toString(),
                                v.totalEmptyBalance.toString()
                            )
                        },
                        footerTotals = listOf(
                            "TOTAL",
                            overviews.sumOf { it.totalOldFull }.toString(),
                            overviews.sumOf { it.totalFullReceived }.toString(),
                            overviews.sumOf { it.totalFull }.toString(),
                            overviews.sumOf { it.totalCustomerSales }.toString(),
                            overviews.sumOf { it.totalFullBalance }.toString(),
                            overviews.sumOf { it.totalOldEmpty }.toString(),
                            overviews.sumOf { it.totalCustomerEmptyReceived }.toString(),
                            overviews.sumOf { it.totalEmptyReturnedToStore }.toString(),
                            overviews.sumOf { it.totalEmptyBalance }.toString()
                        )
                    )
                }
            }

            ReportType.MONTHLY_SALES -> {
                val rows = monthlySalesReport.value
                ReportTable(
                    reportTitle = "Monthly Sales Report",
                    subtitle = "Month: $ym",
                    generatedBy = actorName,
                    headers = listOf("Date", "Cylinders Sold", "Purchases", "Full Dispatched", "Empty Returned"),
                    rows = rows.map {
                        listOf(
                            it.date,
                            it.totalSold.toString(),
                            it.totalPurchases.toString(),
                            it.totalDispatch.toString(),
                            it.totalEmptyReturned.toString()
                        )
                    },
                    footerTotals = listOf(
                        "MONTH TOTAL",
                        rows.sumOf { it.totalSold }.toString(),
                        rows.sumOf { it.totalPurchases }.toString(),
                        rows.sumOf { it.totalDispatch }.toString(),
                        rows.sumOf { it.totalEmptyReturned }.toString()
                    )
                )
            }

            ReportType.PRODUCT_WISE_SALES -> {
                val rows = productWiseSales.value
                ReportTable(
                    reportTitle = "Product-Wise Sales Report",
                    subtitle = "Month: $ym",
                    generatedBy = actorName,
                    headers = listOf("Category", "Product", "Cylinders Sold", "Empty Collected"),
                    rows = rows.map {
                        listOf(
                            it.product.category.displayName,
                            it.product.name,
                            it.totalSold.toString(),
                            it.emptyCollected.toString()
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        "${rows.size} Products",
                        rows.sumOf { it.totalSold }.toString(),
                        rows.sumOf { it.emptyCollected }.toString()
                    )
                )
            }

            ReportType.STAFF_WISE_SALES -> {
                val rows = staffWiseSales.value
                ReportTable(
                    reportTitle = "Staff-Wise Sales Report",
                    subtitle = "Month: $ym | Closing as of: $date",
                    generatedBy = actorName,
                    headers = listOf(
                        "Staff / Vehicle", "Full Recd", "Sales", "Cust Empty",
                        "Ret.Store", "Closing Full", "Closing Empty"
                    ),
                    rows = rows.map {
                        listOf(
                            it.vehicle.name,
                            it.totalFullReceived.toString(),
                            it.totalSold.toString(),
                            it.totalEmptyCollected.toString(),
                            it.totalEmptyReturned.toString(),
                            it.closingFullBalance.toString(),
                            it.closingEmptyBalance.toString()
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        rows.sumOf { it.totalFullReceived }.toString(),
                        rows.sumOf { it.totalSold }.toString(),
                        rows.sumOf { it.totalEmptyCollected }.toString(),
                        rows.sumOf { it.totalEmptyReturned }.toString(),
                        rows.sumOf { it.closingFullBalance }.toString(),
                        rows.sumOf { it.closingEmptyBalance }.toString()
                    )
                )
            }

            ReportType.FULL_EMPTY_STOCK -> {
                val rows = companyStockReport.value
                ReportTable(
                    reportTitle = "Full & Empty Stock Report",
                    subtitle = "As of Date: $date",
                    generatedBy = actorName,
                    headers = listOf(
                        "Category", "Product", "Store Full", "Vehicles Full",
                        "Total Full", "Store Empty", "Vehicles Empty", "Total Empty"
                    ),
                    rows = rows.map {
                        listOf(
                            it.product.category.displayName,
                            it.product.name,
                            it.storeFull.toString(),
                            it.vehiclesFull.toString(),
                            it.totalFull.toString(),
                            it.storeEmpty.toString(),
                            it.vehiclesEmpty.toString(),
                            it.totalEmpty.toString()
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        "${rows.size} Items",
                        rows.sumOf { it.storeFull }.toString(),
                        rows.sumOf { it.vehiclesFull }.toString(),
                        rows.sumOf { it.totalFull }.toString(),
                        rows.sumOf { it.storeEmpty }.toString(),
                        rows.sumOf { it.vehiclesEmpty }.toString(),
                        rows.sumOf { it.totalEmpty }.toString()
                    )
                )
            }

            ReportType.PURCHASE_REGISTER -> {
                val purchaseItems = allTransactionDisplayItems.value.filter { item ->
                    val t = item.transaction
                    t.businessDate.startsWith(ym) &&
                        (t.type == TransactionType.STORE_PURCHASE_FULL || t.type == TransactionType.STORE_PURCHASE_EMPTY)
                }.sortedByDescending { it.transaction.businessDate }

                val totalFullPur = purchaseItems
                    .filter { it.transaction.type == TransactionType.STORE_PURCHASE_FULL }
                    .sumOf { it.transaction.quantity }
                val totalEmptyPur = purchaseItems
                    .filter { it.transaction.type == TransactionType.STORE_PURCHASE_EMPTY }
                    .sumOf { it.transaction.quantity }

                ReportTable(
                    reportTitle = "Purchase Register (Company-Wise)",
                    subtitle = "Month: $ym • Full: $totalFullPur | Empty: $totalEmptyPur",
                    generatedBy = actorName,
                    headers = listOf("Company Name", "Date", "Purchase Type", "Product", "Qty", "Entered By", "Notes"),
                    rows = purchaseItems.map { item ->
                        val t = item.transaction
                        listOf(
                            t.purchaseCompanyName.ifBlank { "Unspecified Supplier" },
                            t.businessDate,
                            if (t.type == TransactionType.STORE_PURCHASE_FULL) "Full Pur" else "Empty Pur",
                            "${item.productCategory.shortCode} - ${item.productName}",
                            t.quantity.toString(),
                            t.staffName,
                            t.notes
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        "${purchaseItems.size} Entries",
                        "Full:$totalFullPur",
                        "Empty:$totalEmptyPur",
                        (totalFullPur + totalEmptyPur).toString(),
                        "",
                        ""
                    )
                )
            }

            ReportType.CUSTOMER_PENDING -> {
                val pendingRows = allCustomerPendingRows.value.let { list ->
                    if (vehicleFilterId != null) {
                        list.filter { it.primaryVehicleId == vehicleFilterId }
                    } else {
                        list
                    }
                }
                ReportTable(
                    reportTitle = "Customer Empty Pending Balance",
                    subtitle = "As of: $date • Total Pending Empty: ${pendingRows.sumOf { it.pendingEmptyBalance }}",
                    generatedBy = actorName,
                    headers = listOf("Customer Name", "Vehicle", "Full Sold", "+Added", "Emp Recd", "Pending Bal", "Last Date"),
                    rows = pendingRows.map { r ->
                        listOf(
                            r.customerName,
                            r.primaryVehicleName,
                            r.fullSold.toString(),
                            r.manualPendingAdded.toString(),
                            r.emptyCollected.toString(),
                            r.pendingEmptyBalance.toString(),
                            r.lastActivityDate
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        "${pendingRows.size} Cust",
                        pendingRows.sumOf { it.fullSold }.toString(),
                        pendingRows.sumOf { it.manualPendingAdded }.toString(),
                        pendingRows.sumOf { it.emptyCollected }.toString(),
                        pendingRows.sumOf { it.pendingEmptyBalance }.toString(),
                        ""
                    )
                )
            }
        }
    }

    class Factory(
        private val repository: InventoryRepository,
        private val backupManager: BackupManager,
        private val cloudSync: CloudSyncRepository? = null,
        private val sessionPreferences: SessionPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(repository, backupManager, cloudSync, sessionPreferences) as T
        }
    }
}
