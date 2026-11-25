package com.example.ui.viewmodel

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProductEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AuthSession
import com.example.data.model.AutoLogoutMode
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
import com.example.data.model.ThemePreset
import com.example.data.model.TransactionDisplayItem
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.data.model.VehicleStockRow
import com.example.data.remote.CloudLoginMetadata
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.WorkspaceConnectionState
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.DriveAuthResult
import com.example.export.DriveBackupState
import com.example.export.GoogleDriveBackupManager
import com.example.export.ReportExporter
import com.example.export.ReportTable
import com.example.permissions.AppPermissionsManager
import com.example.permissions.AppPermissionsStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WarningAlert(
    val title: String,
    val message: String
)

private enum class PendingDriveConsentAction {
    NONE,
    ENABLE_AUTO_AND_BACKUP,
    BACKUP_NOW,
    RESTORE_NOW
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: InventoryRepository,
    private val backupManager: BackupManager,
    private val cloudSync: CloudSyncRepository? = null,
    private val sessionPreferences: SessionPreferences? = null,
    private val driveBackupManager: GoogleDriveBackupManager? = null,
    private val appContext: Context? = null
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

    val cloudLoginMetadata: StateFlow<CloudLoginMetadata> =
        cloudSync?.loginMetadata ?: MutableStateFlow(CloudLoginMetadata()).asStateFlow()

    private val _cloudActionBusy = MutableStateFlow(false)
    val cloudActionBusy: StateFlow<Boolean> = _cloudActionBusy.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    val themePreset: StateFlow<ThemePreset> =
        (sessionPreferences?.themePresetFlow ?: flowOf(ThemePreset.EMERALD_CORPORATE))
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreset.EMERALD_CORPORATE)

    val soundEnabled: StateFlow<Boolean> =
        (sessionPreferences?.soundEnabledFlow ?: flowOf(true))
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoLoginEnabled: StateFlow<Boolean> =
        (sessionPreferences?.autoLoginEnabledFlow ?: flowOf(true))
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoLogoutMode: StateFlow<AutoLogoutMode> =
        (sessionPreferences?.autoLogoutModeFlow ?: flowOf(AutoLogoutMode.OFF))
            .stateIn(viewModelScope, SharingStarted.Eagerly, AutoLogoutMode.OFF)

    private val _driveBackupState = MutableStateFlow(DriveBackupState())
    val driveBackupState: StateFlow<DriveBackupState> = _driveBackupState.asStateFlow()

    private val _driveConsentRequest = MutableStateFlow<PendingIntent?>(null)
    val driveConsentRequest: StateFlow<PendingIntent?> = _driveConsentRequest.asStateFlow()

    private val _pendingDriveRestoreJson = MutableStateFlow<String?>(null)
    val pendingDriveRestoreJson: StateFlow<String?> = _pendingDriveRestoreJson.asStateFlow()

    private val _permissionsStatus = MutableStateFlow(AppPermissionsStatus())
    val permissionsStatus: StateFlow<AppPermissionsStatus> = _permissionsStatus.asStateFlow()

    private var pendingDriveConsentAction = PendingDriveConsentAction.NONE
    private var autoDriveBackupJob: Job? = null

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
            val shouldAutoLogin = sessionPreferences?.autoLoginEnabledFlow?.first() ?: true
            val logoutMode = sessionPreferences?.autoLogoutModeFlow?.first() ?: AutoLogoutMode.OFF
            val savedUserId = if (shouldAutoLogin && logoutMode != AutoLogoutMode.ON_MINIMIZE) {
                sessionPreferences?.getLoggedInUserId()
            } else {
                null
            }
            if (savedUserId != null) {
                val restored = repository.getSessionForUserId(savedUserId)
                if (restored != null) {
                    _authSession.value = restored
                    if (restored.assignedVehicle != null) {
                        _selectedVehicleId.value = restored.assignedVehicle.id
                    }
                } else {
                    sessionPreferences?.clearLoggedInUserId()
                }
            }
            _isInitialized.value = true
            cloudSync?.connectForSignedInUser()
            loadDriveAndPermissionState()
        }
    }

    private suspend fun loadDriveAndPermissionState() {
        val enabled = sessionPreferences?.autoDriveBackupEnabledFlow?.first() ?: false
        val lastAt = sessionPreferences?.lastDriveBackupAtMsFlow?.first() ?: 0L
        val lastId = sessionPreferences?.lastDriveFileIdFlow?.first() ?: ""
        val authorized = when (driveBackupManager?.authorizeDriveAccess(allowConsentPrompt = false)) {
            is DriveAuthResult.Authorized -> true
            else -> false
        }
        val statusMsg = when {
            enabled && authorized && lastAt > 0L -> "Auto Drive Backup active"
            authorized -> "Google Drive connected"
            enabled -> "Tap Authorize Google Drive to resume auto backup"
            else -> "Google Drive backup not yet connected"
        }
        _driveBackupState.value = _driveBackupState.value.copy(
            autoBackupEnabled = enabled,
            driveAuthorized = authorized,
            lastBackupAtMs = lastAt,
            lastFileId = lastId,
            lastStatusMessage = statusMsg
        )
        refreshPermissionsStatus(authorized)
    }

    fun refreshPermissionsStatus(driveAuthorizedOverride: Boolean? = null) {
        val ctx = appContext ?: return
        val driveOk = driveAuthorizedOverride ?: _driveBackupState.value.driveAuthorized
        _permissionsStatus.value = AppPermissionsManager.evaluateStatus(ctx, driveAuthorized = driveOk)
    }

    fun requestGoogleDriveAuthorizationAndEnable() {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can configure Google Drive backup storage.")
            return
        }
        setAutoDriveBackupEnabled(true)
    }

    fun setAutoDriveBackupEnabled(enabled: Boolean) {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can manage Google Drive auto-backup.")
            return
        }
        viewModelScope.launch {
            if (!enabled) {
                sessionPreferences?.setAutoDriveBackupEnabled(false)
                _driveBackupState.value = _driveBackupState.value.copy(
                    autoBackupEnabled = false,
                    lastStatusMessage = "Auto Google Drive backup paused"
                )
                syncAdminDriveSettingsToCloud()
                return@launch
            }

            val mgr = driveBackupManager
            if (mgr == null) {
                _snackbarMessage.value = "Google Drive backup manager unavailable."
                return@launch
            }

            when (val authRes = mgr.authorizeDriveAccess(allowConsentPrompt = true)) {
                is DriveAuthResult.Authorized -> {
                    sessionPreferences?.setAutoDriveBackupEnabled(true)
                    _driveBackupState.value = _driveBackupState.value.copy(
                        autoBackupEnabled = true,
                        driveAuthorized = true,
                        lastStatusMessage = "Auto Drive backup enabled"
                    )
                    refreshPermissionsStatus(true)
                    backUpToGoogleDriveNow(accessTokenOverride = authRes.accessToken)
                }
                is DriveAuthResult.NeedsConsent -> {
                    pendingDriveConsentAction = PendingDriveConsentAction.ENABLE_AUTO_AND_BACKUP
                    _driveConsentRequest.value = authRes.pendingIntent
                }
                is DriveAuthResult.Error -> {
                    _driveBackupState.value = _driveBackupState.value.copy(
                        lastStatusMessage = authRes.message
                    )
                    _warningAlert.value = WarningAlert("Google Drive Setup", authRes.message)
                }
            }
        }
    }

    fun backUpToGoogleDriveNow(accessTokenOverride: String? = null) {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can back up company data to Google Drive.")
            return
        }
        val mgr = driveBackupManager ?: return
        if (_driveBackupState.value.isBackingUp) return

        viewModelScope.launch {
            _driveBackupState.value = _driveBackupState.value.copy(
                isBackingUp = true,
                lastStatusMessage = "Uploading backup to Google Drive..."
            )
            try {
                val token = accessTokenOverride ?: when (val authRes = mgr.authorizeDriveAccess(allowConsentPrompt = true)) {
                    is DriveAuthResult.Authorized -> authRes.accessToken
                    is DriveAuthResult.NeedsConsent -> {
                        pendingDriveConsentAction = PendingDriveConsentAction.BACKUP_NOW
                        _driveConsentRequest.value = authRes.pendingIntent
                        _driveBackupState.value = _driveBackupState.value.copy(
                            isBackingUp = false,
                            lastStatusMessage = "Waiting for Google Drive consent..."
                        )
                        return@launch
                    }
                    is DriveAuthResult.Error -> {
                        _driveBackupState.value = _driveBackupState.value.copy(
                            isBackingUp = false,
                            driveAuthorized = false,
                            lastStatusMessage = authRes.message
                        )
                        refreshPermissionsStatus(false)
                        _warningAlert.value = WarningAlert("Google Drive Auth", authRes.message)
                        return@launch
                    }
                }

                val json = backupManager.exportBackupJson()
                val uploadRes = mgr.uploadJsonBackup(jsonContent = json, accessTokenOverride = token)
                uploadRes.onSuccess { receipt ->
                    sessionPreferences?.saveDriveBackupReceipt(receipt.uploadedAtMs, receipt.fileId)
                    _driveBackupState.value = _driveBackupState.value.copy(
                        isBackingUp = false,
                        driveAuthorized = true,
                        lastBackupAtMs = receipt.uploadedAtMs,
                        lastFileId = receipt.fileId,
                        lastStatusMessage = "Backed up to Google Drive (${receipt.fileName})"
                    )
                    refreshPermissionsStatus(true)
                    syncAdminDriveSettingsToCloud()
                    appContext?.let { ctx ->
                        AppPermissionsManager.postDriveBackupNotification(
                            context = ctx,
                            title = "Concord & A to Z Gas Backup",
                            message = "Google Drive cloud backup saved automatically."
                        )
                    }
                    _snackbarMessage.value = "Backed up to Google Drive."
                }.onFailure { err ->
                    _driveBackupState.value = _driveBackupState.value.copy(
                        isBackingUp = false,
                        lastStatusMessage = err.localizedMessage ?: "Google Drive upload failed"
                    )
                    _warningAlert.value = WarningAlert(
                        "Google Drive Backup Failed",
                        err.localizedMessage ?: "Unable to upload backup to Google Drive."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                if (_driveBackupState.value.isBackingUp) {
                    _driveBackupState.value = _driveBackupState.value.copy(isBackingUp = false)
                }
            }
        }
    }

    fun restoreFromGoogleDriveNow(accessTokenOverride: String? = null) {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can restore from Google Drive.")
            return
        }
        val mgr = driveBackupManager ?: return
        if (_driveBackupState.value.isRestoring) return

        viewModelScope.launch {
            _driveBackupState.value = _driveBackupState.value.copy(
                isRestoring = true,
                lastStatusMessage = "Downloading backup from Google Drive..."
            )
            try {
                val token = accessTokenOverride ?: when (val authRes = mgr.authorizeDriveAccess(allowConsentPrompt = true)) {
                    is DriveAuthResult.Authorized -> authRes.accessToken
                    is DriveAuthResult.NeedsConsent -> {
                        pendingDriveConsentAction = PendingDriveConsentAction.RESTORE_NOW
                        _driveConsentRequest.value = authRes.pendingIntent
                        _driveBackupState.value = _driveBackupState.value.copy(isRestoring = false)
                        return@launch
                    }
                    is DriveAuthResult.Error -> {
                        _driveBackupState.value = _driveBackupState.value.copy(
                            isRestoring = false,
                            lastStatusMessage = authRes.message
                        )
                        _warningAlert.value = WarningAlert("Google Drive Auth", authRes.message)
                        return@launch
                    }
                }

                mgr.downloadLatestBackupJson(accessTokenOverride = token)
                    .onSuccess { json ->
                        _driveBackupState.value = _driveBackupState.value.copy(
                            isRestoring = false,
                            driveAuthorized = true,
                            lastStatusMessage = "Drive backup downloaded — confirm restore"
                        )
                        refreshPermissionsStatus(true)
                        _pendingDriveRestoreJson.value = json
                    }
                    .onFailure { err ->
                        _driveBackupState.value = _driveBackupState.value.copy(
                            isRestoring = false,
                            lastStatusMessage = err.localizedMessage ?: "Drive restore failed"
                        )
                        _warningAlert.value = WarningAlert(
                            "Google Drive Restore",
                            err.localizedMessage ?: "Could not fetch backup from Google Drive."
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } finally {
                if (_driveBackupState.value.isRestoring) {
                    _driveBackupState.value = _driveBackupState.value.copy(isRestoring = false)
                }
            }
        }
    }

    fun confirmPendingDriveRestore() {
        val json = _pendingDriveRestoreJson.value ?: return
        _pendingDriveRestoreJson.value = null
        restoreBackupJson(json)
    }

    fun dismissPendingDriveRestore() {
        _pendingDriveRestoreJson.value = null
    }

    fun handleDriveConsentResult(resultCode: Int, data: Intent?) {
        _driveConsentRequest.value = null
        val action = pendingDriveConsentAction
        pendingDriveConsentAction = PendingDriveConsentAction.NONE

        val mgr = driveBackupManager ?: return
        if (resultCode != Activity.RESULT_OK) {
            _driveBackupState.value = _driveBackupState.value.copy(
                lastStatusMessage = "Google Drive authorization cancelled"
            )
            _snackbarMessage.value = "Google Drive permission was not granted."
            return
        }

        val token = mgr.extractTokenFromConsentResult(data)
        if (token.isNullOrBlank()) {
            _driveBackupState.value = _driveBackupState.value.copy(
                lastStatusMessage = "Google Drive authorization returned no token"
            )
            return
        }

        viewModelScope.launch {
            _driveBackupState.value = _driveBackupState.value.copy(
                driveAuthorized = true,
                lastStatusMessage = "Google Drive authorized"
            )
            refreshPermissionsStatus(true)
            when (action) {
                PendingDriveConsentAction.ENABLE_AUTO_AND_BACKUP -> {
                    sessionPreferences?.setAutoDriveBackupEnabled(true)
                    _driveBackupState.value = _driveBackupState.value.copy(autoBackupEnabled = true)
                    backUpToGoogleDriveNow(accessTokenOverride = token)
                }
                PendingDriveConsentAction.BACKUP_NOW -> backUpToGoogleDriveNow(accessTokenOverride = token)
                PendingDriveConsentAction.RESTORE_NOW -> restoreFromGoogleDriveNow(accessTokenOverride = token)
                PendingDriveConsentAction.NONE -> syncAdminDriveSettingsToCloud()
            }
        }
    }

    fun dismissDriveConsentRequest() {
        _driveConsentRequest.value = null
        pendingDriveConsentAction = PendingDriveConsentAction.NONE
    }

    private fun scheduleAutoDriveBackup() {
        if (driveBackupManager == null || !_driveBackupState.value.autoBackupEnabled) return
        val session = _authSession.value
        val conn = workspaceConnectionState.value
        val canAdminBackup = session?.user?.role == UserRole.ADMIN ||
            (conn is WorkspaceConnectionState.Connected && conn.isOwnerOrAdmin)
        if (!canAdminBackup) return

        autoDriveBackupJob?.cancel()
        autoDriveBackupJob = viewModelScope.launch {
            delay(2500L)
            val authRes = driveBackupManager.authorizeDriveAccess(allowConsentPrompt = false)
            if (authRes is DriveAuthResult.Authorized) {
                val json = backupManager.exportBackupJson()
                driveBackupManager.uploadJsonBackup(json, accessTokenOverride = authRes.accessToken)
                    .onSuccess { receipt ->
                        sessionPreferences?.saveDriveBackupReceipt(receipt.uploadedAtMs, receipt.fileId)
                        _driveBackupState.value = _driveBackupState.value.copy(
                            driveAuthorized = true,
                            lastBackupAtMs = receipt.uploadedAtMs,
                            lastFileId = receipt.fileId,
                            lastStatusMessage = "Auto-saved to Google Drive"
                        )
                        syncAdminDriveSettingsToCloud()
                    }
                    .onFailure { err ->
                        _driveBackupState.value = _driveBackupState.value.copy(
                            lastStatusMessage = "Auto-backup deferred: ${err.localizedMessage ?: "Offline"}"
                        )
                    }
            } else {
                _driveBackupState.value = _driveBackupState.value.copy(
                    driveAuthorized = false,
                    lastStatusMessage = "Auto-backup paused: re-authorize Google Drive"
                )
                refreshPermissionsStatus(false)
            }
        }
    }

    private suspend fun syncAdminDriveSettingsToCloud() {
        val conn = workspaceConnectionState.value
        if (conn !is WorkspaceConnectionState.Connected || !conn.isOwnerOrAdmin) return
        val st = _driveBackupState.value
        cloudSync?.saveAdminDriveSettings(
            autoDriveBackupEnabled = st.autoBackupEnabled,
            lastDriveBackupAtMs = st.lastBackupAtMs,
            lastDriveFileId = st.lastFileId,
            lastDriveBackupStatus = st.lastStatusMessage,
            themePreset = themePreset.value.name,
            autoLogoutMode = autoLogoutMode.value.name
        )
    }

    fun saveReportToCloudAndDrive(reportType: ReportType, table: ReportTable, uploadPdfToDrive: Boolean) {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Restricted: Only Admin can archive company reports.")
            return
        }
        viewModelScope.launch {
            var driveFileId = ""
            if (uploadPdfToDrive && driveBackupManager != null) {
                when (val authRes = driveBackupManager.authorizeDriveAccess(allowConsentPrompt = true)) {
                    is DriveAuthResult.Authorized -> {
                        val out = ByteArrayOutputStream()
                        ReportExporter.writePdf(table, out)
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        val fileName = "Concord_${reportType.name.lowercase()}_$stamp.pdf"
                        val uploadRes = driveBackupManager.uploadReportDocument(
                            fileName = fileName,
                            mimeType = "application/pdf",
                            bytes = out.toByteArray(),
                            accessTokenOverride = authRes.accessToken
                        )
                        uploadRes.onSuccess { receipt ->
                            driveFileId = receipt.fileId
                        }.onFailure { err ->
                            _snackbarMessage.value = "Drive report upload failed: ${err.localizedMessage}"
                        }
                    }
                    is DriveAuthResult.NeedsConsent -> {
                        pendingDriveConsentAction = PendingDriveConsentAction.NONE
                        _driveConsentRequest.value = authRes.pendingIntent
                    }
                    is DriveAuthResult.Error -> {
                        _snackbarMessage.value = authRes.message
                    }
                }
            }

            val totalsStr = table.footerTotals?.joinToString(" | ").orEmpty()
            val reportId = "${reportType.name}_${_selectedDate.value.replace("-", "_")}"
            val cloudRes = cloudSync?.saveAdminReportSnapshot(
                reportId = reportId,
                reportType = reportType.name,
                reportTitle = table.reportTitle,
                subtitle = table.subtitle,
                businessDate = _selectedDate.value,
                yearMonth = _selectedYearMonth.value,
                generatedBy = session.user.displayName,
                rowCount = table.rows.size,
                summaryTotals = totalsStr,
                driveFileId = driveFileId
            )

            _snackbarMessage.value = when {
                driveFileId.isNotBlank() && cloudRes?.isSuccess == true ->
                    "Report saved to Google Drive & Admin Cloud!"
                driveFileId.isNotBlank() ->
                    "Report uploaded to Google Drive!"
                cloudRes?.isSuccess == true ->
                    "Report snapshot synced to Admin Cloud."
                else -> cloudRes?.exceptionOrNull()?.localizedMessage ?: "Report save attempted."
            }
        }
    }

    fun setThemePreset(preset: ThemePreset) {
        viewModelScope.launch { sessionPreferences?.setThemePreset(preset) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { sessionPreferences?.setSoundEnabled(enabled) }
    }

    fun setAutoLoginEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences?.setAutoLoginEnabled(enabled)
            if (!enabled) {
                sessionPreferences?.clearLoggedInUserId()
            } else {
                _authSession.value?.user?.id?.let { sessionPreferences?.saveLoggedInUserId(it) }
            }
        }
    }

    fun setAutoLogoutMode(mode: AutoLogoutMode) {
        viewModelScope.launch { sessionPreferences?.setAutoLogoutMode(mode) }
    }

    fun lockSessionOnAutoLogout(reason: String = "Session locked by Auto-Logout.") {
        if (_authSession.value != null) {
            _authSession.value = null
            _loginError.value = reason
            viewModelScope.launch {
                sessionPreferences?.clearLoggedInUserId()
                cloudSync?.clearLinkedStaffMetadata()
            }
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

    fun login(userId: Long, pin: String, expectedRole: UserRole? = null) {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val localUser = repository.getUserById(userId)
                if (localUser == null || !localUser.isActive) {
                    _loginError.value = "Selected account was not found or is inactive."
                    return@launch
                }

                val targetRole = expectedRole ?: localUser.role
                if (localUser.role != targetRole) {
                    _loginError.value = "Account '${localUser.displayName}' belongs to ${localUser.role.name}. Select the ${localUser.role.name} tab."
                    return@launch
                }

                if (cloudSync != null) {
                    val cloudCheck = cloudSync.verifyRolePinAndSyncProfile(
                        localUser = localUser,
                        expectedRole = targetRole,
                        pin = pin
                    )
                    if (cloudCheck.isFailure) {
                        _loginError.value = cloudCheck.exceptionOrNull()?.localizedMessage
                            ?: "Firebase PIN verification failed."
                        return@launch
                    }
                }

                val session = repository.authenticate(userId, pin, targetRole)
                if (session != null) {
                    if (autoLoginEnabled.value) {
                        sessionPreferences?.saveLoggedInUserId(session.user.id)
                    } else {
                        sessionPreferences?.clearLoggedInUserId()
                    }
                    _authSession.value = session
                    _loginError.value = null
                    _selectedDate.value = InventoryRepository.todayIsoDate()
                    if (session.assignedVehicle != null) {
                        _selectedVehicleId.value = session.assignedVehicle.id
                    }
                } else {
                    _loginError.value = "Invalid or duplicate PIN for ${targetRole.name} '${localUser.displayName}'."
                }
            } finally {
                _isAuthenticating.value = false
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
            cloudSync?.clearLinkedStaffMetadata()
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
                TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
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
            is OperationResult.Success -> {
                _snackbarMessage.value = result.message
                scheduleAutoDriveBackup()
            }
            is OperationResult.InsufficientStock -> _warningAlert.value = WarningAlert(result.title, result.message)
            is OperationResult.ValidationError -> _warningAlert.value = WarningAlert("Validation Warning", result.message)
        }
    }

    fun buildReportTable(
        reportType: ReportType,
        vehicleFilterId: Long? = null,
        purchaseDailyMode: Boolean = false,
        purchaseCompanyFilter: String? = null
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
                val cleanCompanyFilter = purchaseCompanyFilter?.trim().orEmpty()
                val purchaseItems = allTransactionDisplayItems.value.filter { item ->
                    val t = item.transaction
                    val periodMatch = if (purchaseDailyMode) {
                        t.businessDate == date
                    } else {
                        t.businessDate.startsWith(ym)
                    }
                    val typeMatch = t.type == TransactionType.STORE_PURCHASE_FULL ||
                        t.type == TransactionType.STORE_PURCHASE_EMPTY ||
                        t.type == TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE
                    val companyMatch = cleanCompanyFilter.isEmpty() ||
                        t.purchaseCompanyName.trim().equals(cleanCompanyFilter, ignoreCase = true)
                    periodMatch && typeMatch && companyMatch
                }.sortedByDescending { it.transaction.businessDate }

                val totalFullPur = purchaseItems
                    .filter { it.transaction.type == TransactionType.STORE_PURCHASE_FULL }
                    .sumOf { it.transaction.quantity }
                val totalEmptyPur = purchaseItems
                    .filter { it.transaction.type == TransactionType.STORE_PURCHASE_EMPTY }
                    .sumOf { it.transaction.quantity }
                val totalCompRepl = purchaseItems
                    .filter { it.transaction.type == TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE }
                    .sumOf { it.transaction.quantity }

                val periodLabel = if (purchaseDailyMode) "Daily ($date)" else "Monthly ($ym)"
                val companySuffix = if (cleanCompanyFilter.isNotEmpty()) " • $cleanCompanyFilter" else ""

                ReportTable(
                    reportTitle = "Purchase & Complaint Report ($periodLabel)",
                    subtitle = "$periodLabel$companySuffix • Full: $totalFullPur | Empty: $totalEmptyPur | Comp.Repl: $totalCompRepl",
                    generatedBy = actorName,
                    headers = listOf("Company Name", "Date", "Entry Type", "Product", "Qty", "Entered By", "Notes"),
                    rows = purchaseItems.map { item ->
                        val t = item.transaction
                        val typeLabel = when (t.type) {
                            TransactionType.STORE_PURCHASE_FULL -> "Full Pur"
                            TransactionType.STORE_PURCHASE_EMPTY -> "Empty Pur"
                            TransactionType.STORE_PURCHASE_COMPLAINT_REPLACE -> "Comp.Repl"
                            else -> t.type.label
                        }
                        listOf(
                            t.purchaseCompanyName.ifBlank { "Unspecified Supplier" },
                            t.businessDate,
                            typeLabel,
                            "${item.productCategory.shortCode} - ${item.productName}",
                            t.quantity.toString(),
                            t.staffName,
                            t.notes
                        )
                    },
                    footerTotals = listOf(
                        "TOTAL",
                        "${purchaseItems.size} Entries",
                        "F:$totalFullPur E:$totalEmptyPur",
                        "Repl:$totalCompRepl",
                        (totalFullPur + totalEmptyPur + totalCompRepl).toString(),
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
        private val sessionPreferences: SessionPreferences? = null,
        private val driveBackupManager: GoogleDriveBackupManager? = null,
        private val appContext: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                repository = repository,
                backupManager = backupManager,
                cloudSync = cloudSync,
                sessionPreferences = sessionPreferences,
                driveBackupManager = driveBackupManager,
                appContext = appContext
            ) as T
        }
    }
}
