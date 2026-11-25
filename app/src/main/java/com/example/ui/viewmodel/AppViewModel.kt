package com.example.ui.viewmodel

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.AdminCredentialCrypto
import com.example.auth.AdminRecoveryUiState
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricAvailability
import com.example.auth.GmailAuthResult
import com.example.auth.GmailVerificationManager
import com.example.auth.RecoveryPurpose
import com.example.auth.RecoveryStep
import com.example.data.local.AdminCredentialsState
import com.example.data.local.BiometricUnlockState
import com.example.data.local.ProductEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AuthSession
import com.example.data.model.AutoLogoutMode
import com.example.data.model.CompanyStockRow
import com.example.data.model.CustomerPendingRow
import com.example.data.model.DashboardSummary
import com.example.data.model.LowStockAlertState
import com.example.data.model.LowStockProductItem
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
import com.example.notifications.StaffLoginNotificationItem
import com.example.notifications.StaffLoginNotificationManager
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

private data class ActiveRecoveryChallenge(
    val purpose: RecoveryPurpose,
    val email: String,
    val codeSalt: String,
    val codeHash: String,
    val expiresAtMs: Long,
    val attemptsUsed: Int = 0
)

private data class PendingGmailConsentAction(
    val purpose: RecoveryPurpose,
    val email: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: InventoryRepository,
    private val backupManager: BackupManager,
    private val cloudSync: CloudSyncRepository? = null,
    private val sessionPreferences: SessionPreferences? = null,
    private val driveBackupManager: GoogleDriveBackupManager? = null,
    private val appContext: Context? = null,
    private val gmailVerificationManager: GmailVerificationManager? = null
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

    val darkModeEnabled: StateFlow<Boolean> =
        (sessionPreferences?.darkModeEnabledFlow ?: flowOf(false))
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _lowStockThreshold = MutableStateFlow(LowStockAlertState.DEFAULT_LOW_STOCK_THRESHOLD)
    val lowStockThreshold: StateFlow<Int> = _lowStockThreshold.asStateFlow()

    private val _lowStockAlertState = MutableStateFlow(LowStockAlertState())
    val lowStockAlertState: StateFlow<LowStockAlertState> = _lowStockAlertState.asStateFlow()

    val adminCredentialsState: StateFlow<AdminCredentialsState> =
        (sessionPreferences?.adminCredentialsFlow ?: flowOf(AdminCredentialsState()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, AdminCredentialsState())

    val ownerViewConfigured: StateFlow<Boolean> =
        (sessionPreferences?.ownerViewConfiguredFlow ?: flowOf(false))
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val biometricUnlockState: StateFlow<BiometricUnlockState> =
        (sessionPreferences?.biometricUnlockStateFlow ?: flowOf(BiometricUnlockState()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, BiometricUnlockState())

    private val _biometricAvailability = MutableStateFlow(BiometricAvailability.UNAVAILABLE)
    val biometricAvailability: StateFlow<BiometricAvailability> = _biometricAvailability.asStateFlow()

    private val _shouldAutoPromptBiometric = MutableStateFlow(false)
    val shouldAutoPromptBiometric: StateFlow<Boolean> = _shouldAutoPromptBiometric.asStateFlow()

    private val _adminRecoveryState = MutableStateFlow(AdminRecoveryUiState())
    val adminRecoveryState: StateFlow<AdminRecoveryUiState> = _adminRecoveryState.asStateFlow()

    private val _gmailConsentRequest = MutableStateFlow<PendingIntent?>(null)
    val gmailConsentRequest: StateFlow<PendingIntent?> = _gmailConsentRequest.asStateFlow()

    private val _localStaffLoginEvents = MutableStateFlow<List<StaffLoginNotificationItem>>(emptyList())
    private val _latestStaffLoginBanner = MutableStateFlow<StaffLoginNotificationItem?>(null)
    val latestStaffLoginBanner: StateFlow<StaffLoginNotificationItem?> = _latestStaffLoginBanner.asStateFlow()

    val staffLoginNotifications: StateFlow<List<StaffLoginNotificationItem>> = combine(
        _localStaffLoginEvents,
        cloudSync?.staffLoginEvents ?: flowOf(emptyList())
    ) { localEvents, cloudEvents ->
        val mappedCloud = cloudEvents.map { ev ->
            StaffLoginNotificationItem(
                eventId = ev.eventId.ifBlank { ev.id },
                staffId = ev.staffId,
                staffName = ev.staffName,
                vehicleName = ev.vehicleName,
                loginTimestampMs = ev.loginTimestampMs
            )
        }
        (localEvents + mappedCloud)
            .distinctBy { "${it.staffId}_${it.loginTimestampMs / 2000L}" }
            .sortedByDescending { it.loginTimestampMs }
            .take(30)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var activeRecoveryChallenge: ActiveRecoveryChallenge? = null
    private var pendingGmailConsentAction: PendingGmailConsentAction? = null
    private var failedAdminLoginAttempts: Int = 0
    private var adminLoginLockoutUntilMs: Long = 0L
    private var failedOwnerLoginAttempts: Int = 0
    private var ownerLoginLockoutUntilMs: Long = 0L
    private var seenCloudLoginEventIds: MutableSet<String> = mutableSetOf()
    private var initialCloudLoginSnapshotLoaded: Boolean = false

    private var lastAlertSignature: List<Pair<Long, Int>> = emptyList()
    private var latestCompanyStockRows: List<CompanyStockRow> = emptyList()

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
            val bioAvail = appContext?.let { BiometricAuthManager.checkAvailability(it) }
                ?: BiometricAvailability.UNAVAILABLE
            _biometricAvailability.value = bioAvail

            val bioState = sessionPreferences?.biometricUnlockStateFlow?.first() ?: BiometricUnlockState()
            val shouldAutoLogin = sessionPreferences?.autoLoginEnabledFlow?.first() ?: true
            val logoutMode = sessionPreferences?.autoLogoutModeFlow?.first() ?: AutoLogoutMode.OFF

            if (bioState.isReadyForBiometric && bioAvail == BiometricAvailability.AVAILABLE) {
                // Armed after first-time password login: require Fingerprint / Face Unlock on next app open
                _authSession.value = null
                _shouldAutoPromptBiometric.value = true
            } else {
                val canRestoreWithoutPrompt = shouldAutoLogin &&
                    logoutMode != AutoLogoutMode.ON_MINIMIZE &&
                    !bioState.passwordRequiredAfterLogout
                val savedRole = if (canRestoreWithoutPrompt) sessionPreferences?.getLoggedInRole() else null
                val savedUserId = if (canRestoreWithoutPrompt) sessionPreferences?.getLoggedInUserId() else null

                if (savedRole == UserRole.OWNER_VIEW && (sessionPreferences?.ownerViewConfiguredFlow?.first() == true)) {
                    _authSession.value = buildOwnerViewSession()
                } else if (savedUserId != null) {
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
            }
            _isInitialized.value = true
            cloudSync?.connectForSignedInUser()
            loadDriveAndPermissionState()
        }

        if (sessionPreferences != null) {
            viewModelScope.launch {
                sessionPreferences.lowStockThresholdFlow.collect { savedThreshold ->
                    _lowStockThreshold.value = savedThreshold
                }
            }
            if (cloudSync != null) {
                viewModelScope.launch {
                    cloudSync.adminSettings.collect { cloudSettings ->
                        if (cloudSettings != null) {
                            if (cloudSettings.adminLoginUsername.isNotBlank() &&
                                cloudSettings.adminPasswordHash.isNotBlank()
                            ) {
                                val localCreds = sessionPreferences.adminCredentialsFlow.first()
                                if (localCreds.username != cloudSettings.adminLoginUsername ||
                                    localCreds.passwordHash != cloudSettings.adminPasswordHash ||
                                    localCreds.recoveryEmail != cloudSettings.adminRecoveryEmail
                                ) {
                                    sessionPreferences.saveAdminCredentials(
                                        username = cloudSettings.adminLoginUsername,
                                        passwordHash = cloudSettings.adminPasswordHash,
                                        recoveryEmail = cloudSettings.adminRecoveryEmail.ifBlank {
                                            CloudSyncRepository.ADMIN_EMAIL
                                        }
                                    )
                                }
                            }
                            if (cloudSettings.ownerViewPasswordHash.isNotBlank()) {
                                val localOwnerHash = sessionPreferences.ownerViewPasswordHashFlow.first()
                                if (localOwnerHash != cloudSettings.ownerViewPasswordHash) {
                                    sessionPreferences.saveOwnerViewPasswordHash(cloudSettings.ownerViewPasswordHash)
                                }
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    cloudSync.staffLoginEvents.collect { events ->
                        if (!initialCloudLoginSnapshotLoaded) {
                            seenCloudLoginEventIds.addAll(events.map { it.eventId.ifBlank { it.id } })
                            initialCloudLoginSnapshotLoaded = true
                            return@collect
                        }
                        val fresh = events.filter { ev ->
                            val id = ev.eventId.ifBlank { ev.id }
                            id.isNotBlank() && seenCloudLoginEventIds.add(id)
                        }
                        if (fresh.isNotEmpty()) {
                            val newest = fresh.maxBy { it.loginTimestampMs }
                            val item = StaffLoginNotificationItem(
                                eventId = newest.eventId.ifBlank { newest.id },
                                staffId = newest.staffId,
                                staffName = newest.staffName,
                                vehicleName = newest.vehicleName,
                                loginTimestampMs = newest.loginTimestampMs
                            )
                            _latestStaffLoginBanner.value = item
                            appContext?.let { ctx ->
                                StaffLoginNotificationManager.notifyStaffLogin(
                                    context = ctx,
                                    staffName = item.staffName,
                                    vehicleName = item.vehicleName,
                                    loginTimestampMs = item.loginTimestampMs
                                )
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                _selectedDate.flatMapLatest { date -> repository.observeCompanyStockReport(date) },
                _lowStockThreshold
            ) { stockRows, threshold ->
                stockRows to threshold
            }.collect { (stockRows, threshold) ->
                latestCompanyStockRows = stockRows
                checkLowStockLevels(stockRows = stockRows, threshold = threshold)
            }
        }
    }

    /**
     * Checks whether active product store stock levels drop below the pre-set threshold
     * and updates [lowStockAlertState] so the Dashboard can surface an actionable alert.
     */
    fun checkLowStockLevels(
        stockRows: List<CompanyStockRow> = latestCompanyStockRows.ifEmpty { companyStockReport.value },
        threshold: Int = _lowStockThreshold.value
    ): LowStockAlertState {
        val safeThreshold = threshold.coerceIn(0, 999)
        val lowItems = if (safeThreshold <= 0) {
            emptyList()
        } else {
            stockRows
                .filter { it.product.isActive && it.storeFull < safeThreshold }
                .sortedWith(compareBy<CompanyStockRow> { it.storeFull }.thenBy { it.product.name })
                .map { row ->
                    LowStockProductItem(
                        productId = row.product.id,
                        productName = row.product.name,
                        category = row.product.category,
                        storeFullBalance = row.storeFull,
                        vehiclesFullBalance = row.vehiclesFull,
                        totalFullBalance = row.totalFull,
                        threshold = safeThreshold
                    )
                }
        }

        val currentSignature = lowItems.map { it.productId to it.storeFullBalance }
        val previousState = _lowStockAlertState.value
        val keepDismissed = previousState.isDismissedForCurrentAlert &&
            previousState.threshold == safeThreshold &&
            currentSignature == lastAlertSignature &&
            lowItems.isNotEmpty()

        lastAlertSignature = currentSignature
        val updatedState = LowStockAlertState(
            isAlertVisible = lowItems.isNotEmpty(),
            threshold = safeThreshold,
            lowStockItems = lowItems,
            isDismissedForCurrentAlert = keepDismissed
        )
        _lowStockAlertState.value = updatedState
        return updatedState
    }

    fun updateLowStockThreshold(newThreshold: Int) {
        val session = _authSession.value
        if (session != null && session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert(
                "Admin Only",
                "Only Admin can change the low stock alert threshold."
            )
            return
        }
        val safeThreshold = newThreshold.coerceIn(0, 999)
        _lowStockThreshold.value = safeThreshold
        lastAlertSignature = emptyList()
        checkLowStockLevels(threshold = safeThreshold)
        viewModelScope.launch {
            sessionPreferences?.setLowStockThreshold(safeThreshold)
            _snackbarMessage.value = if (safeThreshold == 0) {
                "Low stock alert muted (threshold set to 0)."
            } else {
                "Low stock alert threshold updated to $safeThreshold full cylinders."
            }
        }
    }

    fun dismissLowStockAlert() {
        val current = _lowStockAlertState.value
        if (current.isAlertVisible) {
            _lowStockAlertState.value = current.copy(isDismissedForCurrentAlert = true)
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
                            title = "C&A Gas Backup",
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
        viewModelScope.launch {
            sessionPreferences?.setThemePreset(preset)
            if (preset == ThemePreset.MIDNIGHT_DARK) {
                sessionPreferences?.setDarkModeEnabled(true)
            }
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences?.setDarkModeEnabled(enabled)
            if (!enabled && themePreset.value == ThemePreset.MIDNIGHT_DARK) {
                sessionPreferences?.setThemePreset(ThemePreset.EMERALD_CORPORATE)
            }
        }
    }

    fun toggleDarkMode() {
        val currentlyDark = darkModeEnabled.value || themePreset.value == ThemePreset.MIDNIGHT_DARK
        setDarkModeEnabled(!currentlyDark)
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
                _authSession.value?.user?.let { u ->
                    sessionPreferences?.saveLoggedInUserId(u.id, u.role)
                }
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
            val bioState = biometricUnlockState.value
            if (bioState.isReadyForBiometric && _biometricAvailability.value == BiometricAvailability.AVAILABLE) {
                _shouldAutoPromptBiometric.value = true
            }
            viewModelScope.launch {
                sessionPreferences?.clearLoggedInUserId()
                cloudSync?.clearLinkedStaffMetadata()
            }
        }
    }

    fun refreshBiometricAvailability() {
        _biometricAvailability.value = appContext?.let { BiometricAuthManager.checkAvailability(it) }
            ?: BiometricAvailability.UNAVAILABLE
    }

    fun consumeAutoPromptBiometric() {
        _shouldAutoPromptBiometric.value = false
    }

    fun completeBiometricUnlock() {
        if (_isAuthenticating.value) return
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val bioState = sessionPreferences?.biometricUnlockStateFlow?.first() ?: biometricUnlockState.value
                if (!bioState.isReadyForBiometric) {
                    _loginError.value = "Sign in once with Password / PIN first to unlock with Fingerprint or Face."
                    return@launch
                }
                val role = bioState.role ?: return@launch
                if (role == UserRole.ADMIN && isStaffRestrictedDevice()) {
                    _loginError.value = "Admin login is restricted on this Staff device."
                    return@launch
                }
                val session = when (role) {
                    UserRole.OWNER_VIEW -> {
                        val configured = sessionPreferences?.ownerViewConfiguredFlow?.first() ?: ownerViewConfigured.value
                        if (!configured) {
                            _loginError.value = "Owner View password is not configured."
                            return@launch
                        }
                        buildOwnerViewSession()
                    }
                    UserRole.ADMIN, UserRole.STAFF -> {
                        val restored = repository.getSessionForUserId(bioState.userId)
                        if (restored == null || !restored.user.isActive || restored.user.role != role) {
                            _loginError.value = "Saved account is unavailable. Please sign in with Password / PIN."
                            return@launch
                        }
                        restored
                    }
                }
                if (autoLoginEnabled.value) {
                    sessionPreferences?.saveLoggedInUserId(session.user.id, session.user.role)
                }
                _shouldAutoPromptBiometric.value = false
                _authSession.value = session
                _loginError.value = null
                _selectedDate.value = InventoryRepository.todayIsoDate()
                if (session.assignedVehicle != null) {
                    _selectedVehicleId.value = session.assignedVehicle.id
                }
                if (session.user.role == UserRole.STAFF) {
                    emitStaffLoginNotification(session)
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    private fun emitStaffLoginNotification(session: AuthSession) {
        val nowMs = System.currentTimeMillis()
        val vehicleName = session.assignedVehicle?.name.orEmpty()
        val localEvent = StaffLoginNotificationItem(
            eventId = "local_login_${session.user.id}_$nowMs",
            staffId = session.user.id,
            staffName = session.user.displayName,
            vehicleName = vehicleName,
            loginTimestampMs = nowMs
        )
        _localStaffLoginEvents.value = (listOf(localEvent) + _localStaffLoginEvents.value).take(30)
        _latestStaffLoginBanner.value = localEvent
        appContext?.let { ctx ->
            StaffLoginNotificationManager.notifyStaffLogin(
                context = ctx,
                staffName = session.user.displayName,
                vehicleName = vehicleName,
                loginTimestampMs = nowMs
            )
        }
        val vehicleLabel = vehicleName.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
        _snackbarMessage.value = "Staff Login Alert: ${session.user.displayName}$vehicleLabel signed in"
        viewModelScope.launch {
            cloudSync?.recordStaffLoginEvent(
                staffId = session.user.id,
                staffName = session.user.displayName,
                vehicleName = vehicleName,
                loginTimestampMs = nowMs
            )
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
                _snackbarMessage.value = "Joined C&A Gas Online Sync!"
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
                        sessionPreferences?.saveLoggedInUserId(session.user.id, session.user.role)
                    } else {
                        sessionPreferences?.clearLoggedInUserId()
                    }
                    sessionPreferences?.armBiometricUnlock(
                        userId = session.user.id,
                        role = session.user.role,
                        displayName = session.user.displayName
                    )
                    _shouldAutoPromptBiometric.value = false
                    _authSession.value = session
                    _loginError.value = null
                    _selectedDate.value = InventoryRepository.todayIsoDate()
                    if (session.assignedVehicle != null) {
                        _selectedVehicleId.value = session.assignedVehicle.id
                    }
                    if (session.user.role == UserRole.STAFF) {
                        emitStaffLoginNotification(session)
                    }
                } else {
                    _loginError.value = "Invalid or duplicate PIN for ${targetRole.name} '${localUser.displayName}'."
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun dismissStaffLoginBanner() {
        _latestStaffLoginBanner.value = null
    }

    private fun buildOwnerViewSession(): AuthSession = AuthSession(
        user = UserAccountEntity(
            id = OWNER_VIEW_SYNTHETIC_USER_ID,
            username = "owner_view",
            displayName = "Owner (View Only)",
            role = UserRole.OWNER_VIEW,
            pin = "0000",
            isActive = true
        ),
        assignedVehicle = null
    )

    fun setupOwnerViewPassword(
        password: String,
        confirmPassword: String
    ) {
        if (_isAuthenticating.value) return
        if (isStaffRestrictedDevice()) {
            _loginError.value = "Only Admin can create the Owner View password."
            return
        }
        if (!AdminCredentialCrypto.isValidPassword(password)) {
            _loginError.value = "Owner View password must be 6 to 64 characters."
            return
        }
        if (password != confirmPassword) {
            _loginError.value = "Owner View password and confirmation do not match."
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val hash = AdminCredentialCrypto.hashPassword(password)
                sessionPreferences?.saveOwnerViewPasswordHash(hash)
                cloudSync?.saveOwnerViewPasswordHashToCloud(hash)

                val ownerSession = buildOwnerViewSession()
                if (autoLoginEnabled.value) {
                    sessionPreferences?.saveLoggedInUserId(OWNER_VIEW_SYNTHETIC_USER_ID, UserRole.OWNER_VIEW)
                } else {
                    sessionPreferences?.clearLoggedInUserId()
                }
                sessionPreferences?.armBiometricUnlock(
                    userId = OWNER_VIEW_SYNTHETIC_USER_ID,
                    role = UserRole.OWNER_VIEW,
                    displayName = ownerSession.user.displayName
                )
                _shouldAutoPromptBiometric.value = false
                _authSession.value = ownerSession
                _loginError.value = null
                _selectedDate.value = InventoryRepository.todayIsoDate()
                _snackbarMessage.value = "Owner View password created. Fingerprint & Face Unlock enabled for next open."
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun loginOwnerViewWithPassword(password: String) {
        if (_isAuthenticating.value) return
        val now = System.currentTimeMillis()
        if (ownerLoginLockoutUntilMs > now) {
            val waitSec = ((ownerLoginLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
            _loginError.value = "Too many failed attempts. Please wait ${waitSec}s before trying again."
            return
        }
        if (password.isEmpty()) {
            _loginError.value = "Enter the Owner View password."
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val savedHash = sessionPreferences?.ownerViewPasswordHashFlow?.first().orEmpty()
                if (savedHash.isBlank()) {
                    _loginError.value = "Owner View password has not been created yet."
                    return@launch
                }
                if (!AdminCredentialCrypto.verifyPassword(password, savedHash)) {
                    failedOwnerLoginAttempts++
                    if (failedOwnerLoginAttempts >= MAX_LOGIN_ATTEMPTS) {
                        ownerLoginLockoutUntilMs = System.currentTimeMillis() + LOGIN_LOCKOUT_MS
                        failedOwnerLoginAttempts = 0
                        _loginError.value = "Too many failed Owner View attempts. Locked for 30 seconds."
                    } else {
                        val remaining = MAX_LOGIN_ATTEMPTS - failedOwnerLoginAttempts
                        _loginError.value = "Incorrect Owner View password ($remaining attempt(s) left)."
                    }
                    return@launch
                }

                failedOwnerLoginAttempts = 0
                ownerLoginLockoutUntilMs = 0L
                val ownerSession = buildOwnerViewSession()
                if (autoLoginEnabled.value) {
                    sessionPreferences?.saveLoggedInUserId(OWNER_VIEW_SYNTHETIC_USER_ID, UserRole.OWNER_VIEW)
                } else {
                    sessionPreferences?.clearLoggedInUserId()
                }
                sessionPreferences?.armBiometricUnlock(
                    userId = OWNER_VIEW_SYNTHETIC_USER_ID,
                    role = UserRole.OWNER_VIEW,
                    displayName = ownerSession.user.displayName
                )
                _shouldAutoPromptBiometric.value = false
                _authSession.value = ownerSession
                _loginError.value = null
                _selectedDate.value = InventoryRepository.todayIsoDate()
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun updateOwnerViewPasswordFromSettings(
        currentAdminPassword: String,
        newOwnerPassword: String,
        confirmOwnerPassword: String
    ) {
        val session = _authSession.value ?: return
        if (session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can create or change the Owner View password.")
            return
        }
        if (!AdminCredentialCrypto.isValidPassword(newOwnerPassword)) {
            _warningAlert.value = WarningAlert("Invalid Password", "Owner View password must be 6 to 64 characters.")
            return
        }
        if (newOwnerPassword != confirmOwnerPassword) {
            _warningAlert.value = WarningAlert("Password Mismatch", "Owner View password and confirmation do not match.")
            return
        }

        viewModelScope.launch {
            val adminCreds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
            if (adminCreds.isConfigured && !AdminCredentialCrypto.verifyPassword(currentAdminPassword, adminCreds.passwordHash)) {
                _warningAlert.value = WarningAlert("Incorrect Password", "Current Admin password does not match.")
                return@launch
            }
            val hash = AdminCredentialCrypto.hashPassword(newOwnerPassword)
            sessionPreferences?.saveOwnerViewPasswordHash(hash)
            cloudSync?.saveOwnerViewPasswordHashToCloud(hash)
            _snackbarMessage.value = "Owner View (Read-Only) password saved."
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    private fun isStaffRestrictedDevice(): Boolean {
        val conn = workspaceConnectionState.value
        return conn is WorkspaceConnectionState.Connected &&
            !conn.isOwnerOrAdmin &&
            cloudLoginMetadata.value.linkedStaffRole != UserRole.ADMIN
    }

    fun setupInitialAdminCredentials(
        username: String,
        password: String,
        confirmPassword: String,
        recoveryEmail: String
    ) {
        if (_isAuthenticating.value) return
        if (isStaffRestrictedDevice()) {
            _loginError.value = "Admin setup is restricted to the Admin Google account."
            return
        }
        val cleanUser = username.trim()
        val cleanEmail = recoveryEmail.trim().lowercase()
        when {
            !AdminCredentialCrypto.isValidUsername(cleanUser) -> {
                _loginError.value = "Username must be 3–40 characters (letters, numbers, ., _, -)."
                return
            }
            !AdminCredentialCrypto.isValidPassword(password) -> {
                _loginError.value = "Password must be 6–64 characters."
                return
            }
            password != confirmPassword -> {
                _loginError.value = "Passwords do not match."
                return
            }
            !AdminCredentialCrypto.isValidEmail(cleanEmail) -> {
                _loginError.value = "Enter a valid Recovery Gmail address."
                return
            }
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val adminUser = repository.usersFlow.first().firstOrNull { it.role == UserRole.ADMIN && it.isActive }
                if (adminUser == null) {
                    _loginError.value = "Admin account was not found."
                    return@launch
                }
                val hash = AdminCredentialCrypto.hashPassword(password)
                sessionPreferences?.saveAdminCredentials(
                    username = cleanUser,
                    passwordHash = hash,
                    recoveryEmail = cleanEmail
                )
                cloudSync?.saveAdminCredentialsToCloud(
                    adminLoginUsername = cleanUser,
                    adminPasswordHash = hash,
                    adminRecoveryEmail = cleanEmail
                )
                val session = repository.getSessionForUserId(adminUser.id)
                if (session != null) {
                    if (autoLoginEnabled.value) {
                        sessionPreferences?.saveLoggedInUserId(session.user.id, UserRole.ADMIN)
                    } else {
                        sessionPreferences?.clearLoggedInUserId()
                    }
                    sessionPreferences?.armBiometricUnlock(
                        userId = session.user.id,
                        role = UserRole.ADMIN,
                        displayName = session.user.displayName
                    )
                    _shouldAutoPromptBiometric.value = false
                    _authSession.value = session
                    _loginError.value = null
                    _selectedDate.value = InventoryRepository.todayIsoDate()
                    _snackbarMessage.value = "Admin credentials saved. Fingerprint & Face Unlock enabled for next open."
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun loginAdminWithPassword(username: String, password: String) {
        if (_isAuthenticating.value) return
        if (isStaffRestrictedDevice()) {
            _loginError.value = "Admin login is restricted to the authorized Admin Google account."
            return
        }
        val now = System.currentTimeMillis()
        if (now < adminLoginLockoutUntilMs) {
            val remainingSec = ((adminLoginLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
            _loginError.value = "Too many failed attempts. Wait ${remainingSec}s or use Forgot Password via Gmail."
            return
        }

        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) {
            _loginError.value = "Enter both Admin username and password."
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
                if (!creds.isConfigured) {
                    _loginError.value = "Set up your Admin username and password first."
                    return@launch
                }

                val usernameMatches = creds.username.equals(cleanUser, ignoreCase = true)
                val passwordMatches = AdminCredentialCrypto.verifyPassword(password, creds.passwordHash)
                if (!usernameMatches || !passwordMatches) {
                    failedAdminLoginAttempts += 1
                    if (failedAdminLoginAttempts >= 5) {
                        adminLoginLockoutUntilMs = System.currentTimeMillis() + 30_000L
                        failedAdminLoginAttempts = 0
                        _loginError.value = "5 failed attempts. Locked for 30s — use Forgot Password or Forgot Username via Gmail."
                    } else {
                        _loginError.value = "Invalid Admin username or password."
                    }
                    return@launch
                }

                val adminUser = repository.usersFlow.first().firstOrNull { it.role == UserRole.ADMIN && it.isActive }
                if (adminUser == null) {
                    _loginError.value = "Admin account is unavailable."
                    return@launch
                }

                failedAdminLoginAttempts = 0
                adminLoginLockoutUntilMs = 0L

                val session = repository.getSessionForUserId(adminUser.id)
                if (session != null) {
                    if (autoLoginEnabled.value) {
                        sessionPreferences?.saveLoggedInUserId(session.user.id, UserRole.ADMIN)
                    } else {
                        sessionPreferences?.clearLoggedInUserId()
                    }
                    sessionPreferences?.armBiometricUnlock(
                        userId = session.user.id,
                        role = UserRole.ADMIN,
                        displayName = session.user.displayName
                    )
                    _shouldAutoPromptBiometric.value = false
                    _authSession.value = session
                    _loginError.value = null
                    _selectedDate.value = InventoryRepository.todayIsoDate()
                }
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun openAdminRecovery(purpose: RecoveryPurpose, defaultGoogleEmail: String = "") {
        val creds = adminCredentialsState.value
        val suggestedEmail = creds.recoveryEmail
            .ifBlank { defaultGoogleEmail }
            .ifBlank { CloudSyncRepository.ADMIN_EMAIL }
        _adminRecoveryState.value = AdminRecoveryUiState(
            activePurpose = purpose,
            step = RecoveryStep.REQUEST_CODE,
            targetEmail = suggestedEmail,
            maskedEmail = AdminCredentialCrypto.maskEmail(suggestedEmail)
        )
    }

    fun dismissAdminRecovery() {
        activeRecoveryChallenge = null
        pendingGmailConsentAction = null
        _adminRecoveryState.value = AdminRecoveryUiState()
    }

    fun clearAdminRecoveryError() {
        _adminRecoveryState.value = _adminRecoveryState.value.copy(errorMessage = null)
    }

    fun sendAdminRecoveryCode(
        emailInput: String,
        accessTokenOverride: String? = null
    ) {
        val current = _adminRecoveryState.value
        val purpose = current.activePurpose ?: return
        if (current.isSending) return
        if (isStaffRestrictedDevice()) {
            _adminRecoveryState.value = current.copy(
                errorMessage = "Admin recovery is restricted to the Admin Google account."
            )
            return
        }

        val cleanEmail = emailInput.trim().lowercase()
        if (!AdminCredentialCrypto.isValidEmail(cleanEmail)) {
            _adminRecoveryState.value = current.copy(errorMessage = "Enter a valid Gmail address.")
            return
        }

        val now = System.currentTimeMillis()
        if (accessTokenOverride == null && current.resendAvailableAtMs > now) {
            val waitSec = ((current.resendAvailableAtMs - now) / 1000L).coerceAtLeast(1L)
            _adminRecoveryState.value = current.copy(
                errorMessage = "Please wait ${waitSec}s before requesting another Gmail code."
            )
            return
        }

        val mgr = gmailVerificationManager
        if (mgr == null) {
            _adminRecoveryState.value = current.copy(errorMessage = "Gmail verification service is unavailable.")
            return
        }

        viewModelScope.launch {
            val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
            val registeredEmail = creds.recoveryEmail.trim().lowercase()
                .ifBlank { CloudSyncRepository.ADMIN_EMAIL.lowercase() }
            if (cleanEmail != registeredEmail && !cleanEmail.equals(CloudSyncRepository.ADMIN_EMAIL, ignoreCase = true)) {
                _adminRecoveryState.value = current.copy(
                    errorMessage = "Gmail address does not match the registered Admin recovery Gmail (${AdminCredentialCrypto.maskEmail(registeredEmail)})."
                )
                return@launch
            }

            _adminRecoveryState.value = current.copy(
                targetEmail = cleanEmail,
                maskedEmail = AdminCredentialCrypto.maskEmail(cleanEmail),
                isSending = true,
                errorMessage = null
            )

            try {
                val token = accessTokenOverride ?: when (val authRes = mgr.authorizeGmailSend(allowConsentPrompt = true)) {
                    is GmailAuthResult.Authorized -> authRes.accessToken
                    is GmailAuthResult.NeedsConsent -> {
                        pendingGmailConsentAction = PendingGmailConsentAction(purpose, cleanEmail)
                        _gmailConsentRequest.value = authRes.pendingIntent
                        _adminRecoveryState.value = _adminRecoveryState.value.copy(
                            isSending = false,
                            statusMessage = "Waiting for Gmail permission to send verification code..."
                        )
                        return@launch
                    }
                    is GmailAuthResult.Error -> {
                        _adminRecoveryState.value = _adminRecoveryState.value.copy(
                            isSending = false,
                            errorMessage = authRes.message
                        )
                        return@launch
                    }
                }

                val code = AdminCredentialCrypto.generateSixDigitCode()
                val salt = AdminCredentialCrypto.generateRandomSalt()
                val codeHash = AdminCredentialCrypto.hashVerificationCode(
                    code = code,
                    salt = salt,
                    purpose = purpose.name,
                    email = cleanEmail
                )
                val sendRes = mgr.sendVerificationCodeEmail(
                    recipientEmail = cleanEmail,
                    verificationCode = code,
                    purpose = purpose,
                    accessTokenOverride = token
                )
                sendRes.onSuccess { receipt ->
                    val sentNow = receipt.sentAtMs
                    val expiresAt = sentNow + OTP_EXPIRY_MS
                    activeRecoveryChallenge = ActiveRecoveryChallenge(
                        purpose = purpose,
                        email = cleanEmail,
                        codeSalt = salt,
                        codeHash = codeHash,
                        expiresAtMs = expiresAt,
                        attemptsUsed = 0
                    )
                    _adminRecoveryState.value = _adminRecoveryState.value.copy(
                        step = RecoveryStep.VERIFY_CODE,
                        targetEmail = cleanEmail,
                        maskedEmail = AdminCredentialCrypto.maskEmail(cleanEmail),
                        isSending = false,
                        errorMessage = null,
                        statusMessage = "6-digit verification code sent to ${AdminCredentialCrypto.maskEmail(cleanEmail)}",
                        codeExpiresAtMs = expiresAt,
                        resendAvailableAtMs = sentNow + OTP_RESEND_COOLDOWN_MS
                    )
                }.onFailure { err ->
                    _adminRecoveryState.value = _adminRecoveryState.value.copy(
                        isSending = false,
                        errorMessage = err.localizedMessage ?: "Failed to send verification code via Gmail."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                if (_adminRecoveryState.value.isSending) {
                    _adminRecoveryState.value = _adminRecoveryState.value.copy(isSending = false)
                }
            }
        }
    }

    fun handleGmailConsentResult(resultCode: Int, data: Intent?) {
        _gmailConsentRequest.value = null
        val pending = pendingGmailConsentAction
        pendingGmailConsentAction = null

        val mgr = gmailVerificationManager ?: return
        if (resultCode != Activity.RESULT_OK) {
            _adminRecoveryState.value = _adminRecoveryState.value.copy(
                isSending = false,
                errorMessage = "Gmail send permission was cancelled."
            )
            return
        }
        val token = mgr.extractTokenFromConsentResult(data)
        if (token.isNullOrBlank()) {
            _adminRecoveryState.value = _adminRecoveryState.value.copy(
                isSending = false,
                errorMessage = "Gmail authorization did not return an access token."
            )
            return
        }
        if (pending != null) {
            sendAdminRecoveryCode(emailInput = pending.email, accessTokenOverride = token)
        }
    }

    fun dismissGmailConsentRequest() {
        _gmailConsentRequest.value = null
        pendingGmailConsentAction = null
    }

    fun verifyCodeAndResetAdminPassword(
        codeInput: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val current = _adminRecoveryState.value
        if (current.activePurpose != RecoveryPurpose.FORGOT_PASSWORD || current.isVerifying) return

        val challenge = activeRecoveryChallenge
        if (challenge == null || challenge.purpose != RecoveryPurpose.FORGOT_PASSWORD) {
            _adminRecoveryState.value = current.copy(errorMessage = "Request a Gmail verification code first.")
            return
        }
        if (System.currentTimeMillis() > challenge.expiresAtMs) {
            activeRecoveryChallenge = null
            _adminRecoveryState.value = current.copy(
                errorMessage = "Verification code expired. Tap Resend Gmail Code."
            )
            return
        }
        if (!AdminCredentialCrypto.isValidPassword(newPassword)) {
            _adminRecoveryState.value = current.copy(errorMessage = "New password must be 6–64 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            _adminRecoveryState.value = current.copy(errorMessage = "New passwords do not match.")
            return
        }

        val matches = AdminCredentialCrypto.verifyVerificationCode(
            inputCode = codeInput,
            expectedHash = challenge.codeHash,
            salt = challenge.codeSalt,
            purpose = challenge.purpose.name,
            email = challenge.email
        )
        if (!matches) {
            val attempts = challenge.attemptsUsed + 1
            if (attempts >= OTP_MAX_ATTEMPTS) {
                activeRecoveryChallenge = null
                _adminRecoveryState.value = current.copy(
                    errorMessage = "Too many wrong codes. Please request a new Gmail verification code."
                )
            } else {
                activeRecoveryChallenge = challenge.copy(attemptsUsed = attempts)
                _adminRecoveryState.value = current.copy(
                    errorMessage = "Invalid verification code (${OTP_MAX_ATTEMPTS - attempts} attempts left)."
                )
            }
            return
        }

        viewModelScope.launch {
            _adminRecoveryState.value = current.copy(isVerifying = true, errorMessage = null)
            try {
                val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
                val newHash = AdminCredentialCrypto.hashPassword(newPassword)
                val finalUsername = creds.username.ifBlank { "admin_ragavan" }
                val finalEmail = creds.recoveryEmail.ifBlank { challenge.email }
                sessionPreferences?.saveAdminCredentials(
                    username = finalUsername,
                    passwordHash = newHash,
                    recoveryEmail = finalEmail
                )
                cloudSync?.saveAdminCredentialsToCloud(
                    adminLoginUsername = finalUsername,
                    adminPasswordHash = newHash,
                    adminRecoveryEmail = finalEmail
                )
                activeRecoveryChallenge = null
                failedAdminLoginAttempts = 0
                adminLoginLockoutUntilMs = 0L
                _adminRecoveryState.value = AdminRecoveryUiState()
                _snackbarMessage.value = "Admin password reset successfully! Sign in with your new password."
            } finally {
                if (_adminRecoveryState.value.isVerifying) {
                    _adminRecoveryState.value = _adminRecoveryState.value.copy(isVerifying = false)
                }
            }
        }
    }

    fun verifyCodeAndRecoverAdminUsername(codeInput: String) {
        val current = _adminRecoveryState.value
        if (current.activePurpose != RecoveryPurpose.FORGOT_USERNAME || current.isVerifying) return

        val challenge = activeRecoveryChallenge
        if (challenge == null || challenge.purpose != RecoveryPurpose.FORGOT_USERNAME) {
            _adminRecoveryState.value = current.copy(errorMessage = "Request a Gmail verification code first.")
            return
        }
        if (System.currentTimeMillis() > challenge.expiresAtMs) {
            activeRecoveryChallenge = null
            _adminRecoveryState.value = current.copy(
                errorMessage = "Verification code expired. Tap Resend Gmail Code."
            )
            return
        }

        val matches = AdminCredentialCrypto.verifyVerificationCode(
            inputCode = codeInput,
            expectedHash = challenge.codeHash,
            salt = challenge.codeSalt,
            purpose = challenge.purpose.name,
            email = challenge.email
        )
        if (!matches) {
            val attempts = challenge.attemptsUsed + 1
            if (attempts >= OTP_MAX_ATTEMPTS) {
                activeRecoveryChallenge = null
                _adminRecoveryState.value = current.copy(
                    errorMessage = "Too many wrong codes. Please request a new Gmail verification code."
                )
            } else {
                activeRecoveryChallenge = challenge.copy(attemptsUsed = attempts)
                _adminRecoveryState.value = current.copy(
                    errorMessage = "Invalid verification code (${OTP_MAX_ATTEMPTS - attempts} attempts left)."
                )
            }
            return
        }

        viewModelScope.launch {
            _adminRecoveryState.value = current.copy(isVerifying = true, errorMessage = null)
            try {
                val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
                val recoveredUsername = creds.username.ifBlank { "admin_ragavan" }
                activeRecoveryChallenge = null
                _adminRecoveryState.value = current.copy(
                    step = RecoveryStep.USERNAME_REVEALED,
                    isVerifying = false,
                    errorMessage = null,
                    revealedUsername = recoveredUsername,
                    statusMessage = "Gmail verified! Your Admin username is shown below."
                )
            } finally {
                if (_adminRecoveryState.value.isVerifying) {
                    _adminRecoveryState.value = _adminRecoveryState.value.copy(isVerifying = false)
                }
            }
        }
    }

    fun updateAdminUsernameAfterRecovery(newUsername: String) {
        val current = _adminRecoveryState.value
        if (current.step != RecoveryStep.USERNAME_REVEALED) return
        val cleanUser = newUsername.trim()
        if (!AdminCredentialCrypto.isValidUsername(cleanUser)) {
            _adminRecoveryState.value = current.copy(
                errorMessage = "Username must be 3–40 characters (letters, numbers, ., _, -)."
            )
            return
        }
        viewModelScope.launch {
            val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
            sessionPreferences?.updateAdminUsername(cleanUser)
            if (creds.passwordHash.isNotBlank()) {
                cloudSync?.saveAdminCredentialsToCloud(
                    adminLoginUsername = cleanUser,
                    adminPasswordHash = creds.passwordHash,
                    adminRecoveryEmail = creds.recoveryEmail.ifBlank { current.targetEmail }
                )
            }
            _adminRecoveryState.value = current.copy(
                revealedUsername = cleanUser,
                errorMessage = null,
                statusMessage = "Admin username updated to $cleanUser."
            )
            _snackbarMessage.value = "Admin username updated to $cleanUser."
        }
    }

    fun updateAdminCredentialsFromSettings(
        currentPassword: String,
        newUsername: String,
        newPassword: String,
        newRecoveryEmail: String
    ) {
        val session = _authSession.value
        if (session == null || session.user.role != UserRole.ADMIN) {
            _warningAlert.value = WarningAlert("Admin Only", "Only Admin can update Admin login credentials.")
            return
        }
        val cleanUser = newUsername.trim()
        val cleanEmail = newRecoveryEmail.trim().lowercase()
        when {
            !AdminCredentialCrypto.isValidUsername(cleanUser) -> {
                _warningAlert.value = WarningAlert("Invalid Username", "Username must be 3–40 characters (letters, numbers, ., _, -).")
                return
            }
            !AdminCredentialCrypto.isValidEmail(cleanEmail) -> {
                _warningAlert.value = WarningAlert("Invalid Gmail", "Enter a valid Recovery Gmail address.")
                return
            }
            newPassword.isNotEmpty() && !AdminCredentialCrypto.isValidPassword(newPassword) -> {
                _warningAlert.value = WarningAlert("Invalid Password", "New password must be 6–64 characters.")
                return
            }
        }

        viewModelScope.launch {
            val creds = sessionPreferences?.adminCredentialsFlow?.first() ?: adminCredentialsState.value
            if (creds.isConfigured && !AdminCredentialCrypto.verifyPassword(currentPassword, creds.passwordHash)) {
                _warningAlert.value = WarningAlert("Wrong Current Password", "Enter your current Admin password to confirm changes.")
                return@launch
            }
            val finalHash = if (newPassword.isNotEmpty()) {
                AdminCredentialCrypto.hashPassword(newPassword)
            } else if (creds.passwordHash.isNotBlank()) {
                creds.passwordHash
            } else {
                _warningAlert.value = WarningAlert("Password Required", "Enter a new Admin password (min 6 characters).")
                return@launch
            }
            sessionPreferences?.saveAdminCredentials(
                username = cleanUser,
                passwordHash = finalHash,
                recoveryEmail = cleanEmail
            )
            cloudSync?.saveAdminCredentialsToCloud(
                adminLoginUsername = cleanUser,
                adminPasswordHash = finalHash,
                adminRecoveryEmail = cleanEmail
            )
            _snackbarMessage.value = "Admin username, password & recovery Gmail updated."
        }
    }

    fun logout() {
        _authSession.value = null
        _loginError.value = null
        _shouldAutoPromptBiometric.value = false
        viewModelScope.launch {
            sessionPreferences?.disarmBiometricUnlockOnLogout()
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
        if (session.user.role == UserRole.OWNER_VIEW) {
            _warningAlert.value = WarningAlert(
                title = "Owner View (Read-Only)",
                message = "Owner View is view-only. Entries and modifications are disabled."
            )
            return
        }
        // Enforce strict staff permissions
        if (session.user.role == UserRole.STAFF) {
            val allowedStaffTypes = setOf(
                TransactionType.VEHICLE_FULL_RECEIVED,
                TransactionType.VEHICLE_CUSTOMER_SALE,
                TransactionType.VEHICLE_NEW_CUSTOMER_SALE,
                TransactionType.VEHICLE_CUSTOMER_COMPLAINT_REPLACE,
                TransactionType.VEHICLE_EMPTY_FROM_CUSTOMER,
                TransactionType.VEHICLE_EMPTY_RETURN_TO_STORE,
                TransactionType.VEHICLE_TRANSFER_FULL_OUT,
                TransactionType.VEHICLE_TRANSFER_FULL_IN,
                TransactionType.VEHICLE_TRANSFER_EMPTY_OUT,
                TransactionType.VEHICLE_TRANSFER_EMPTY_IN,
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

    fun transferBetweenVehicles(
        sourceVehicleId: Long,
        targetVehicleId: Long,
        productId: Long,
        fullQuantity: Int,
        emptyQuantity: Int,
        notes: String = ""
    ) {
        val session = _authSession.value ?: return
        if (session.user.role == UserRole.OWNER_VIEW) {
            _warningAlert.value = WarningAlert(
                title = "Owner View (Read-Only)",
                message = "Owner View is view-only. Vehicle transfers and entries are disabled."
            )
            return
        }
        if (session.user.role == UserRole.STAFF && sourceVehicleId != session.assignedVehicle?.id) {
            _warningAlert.value = WarningAlert(
                title = "Permission Denied",
                message = "Staff can only transfer cylinders out of their own assigned vehicle."
            )
            return
        }
        viewModelScope.launch {
            val result = repository.transferBetweenVehicles(
                businessDate = _selectedDate.value,
                sourceVehicleId = sourceVehicleId,
                targetVehicleId = targetVehicleId,
                productId = productId,
                fullQuantity = fullQuantity,
                emptyQuantity = emptyQuantity,
                actor = session.user,
                notes = notes
            )
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

    companion object {
        private const val OTP_EXPIRY_MS = 10 * 60 * 1000L
        private const val OTP_RESEND_COOLDOWN_MS = 30 * 1000L
        private const val OTP_MAX_ATTEMPTS = 5
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOGIN_LOCKOUT_MS = 30_000L
        private const val OWNER_VIEW_SYNTHETIC_USER_ID = -100L
    }

    class Factory(
        private val repository: InventoryRepository,
        private val backupManager: BackupManager,
        private val cloudSync: CloudSyncRepository? = null,
        private val sessionPreferences: SessionPreferences? = null,
        private val driveBackupManager: GoogleDriveBackupManager? = null,
        private val appContext: Context? = null,
        private val gmailVerificationManager: GmailVerificationManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                repository = repository,
                backupManager = backupManager,
                cloudSync = cloudSync,
                sessionPreferences = sessionPreferences,
                driveBackupManager = driveBackupManager,
                appContext = appContext,
                gmailVerificationManager = gmailVerificationManager
            ) as T
        }
    }
}
