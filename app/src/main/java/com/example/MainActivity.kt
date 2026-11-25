package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundEffectsManager
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricAvailability
import com.example.auth.GoogleAuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogle
import com.example.data.model.AutoLogoutMode
import com.example.data.model.StockCalculator
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.data.remote.WorkspaceConnectionState
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.FloatingCalculatorIconButton
import com.example.ui.components.InsufficientStockWarningDialog
import com.example.ui.components.QuickCalculatorPopupDialog
import com.example.ui.components.StartupSplashOverlay
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.CompanyInventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.StoreManagementScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.screens.VehicleManagementScreen
import com.example.ui.theme.ConcordGasTheme
import com.example.ui.viewmodel.AppViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

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

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ConcordGasApp
        setContent {
            val themePreset by app.container.sessionPreferences.themePresetFlow
                .collectAsStateWithLifecycle(initialValue = ThemePreset.EMERALD_CORPORATE)
            val darkModeEnabled by app.container.sessionPreferences.darkModeEnabledFlow
                .collectAsStateWithLifecycle(initialValue = false)
            val soundEnabled by app.container.sessionPreferences.soundEnabledFlow
                .collectAsStateWithLifecycle(initialValue = true)
            val rootScope = rememberCoroutineScope()
            val effectiveDarkMode = darkModeEnabled || themePreset == ThemePreset.MIDNIGHT_DARK
            var showSplash by rememberSaveable { mutableStateOf(true) }

            ConcordGasTheme(
                themePreset = themePreset,
                darkTheme = effectiveDarkMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val auth = remember { Firebase.auth }
                    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
                    val user = currentUser

                    if (user == null) {
                        GoogleAuthScreen(
                            onAuthSuccess = { /* authStateFlow emits updated user */ },
                            darkModeEnabled = effectiveDarkMode,
                            onToggleDarkMode = {
                                SoundEffectsManager.playTapTick(soundEnabled)
                                val nextDark = !effectiveDarkMode
                                rootScope.launch {
                                    app.container.sessionPreferences.setDarkModeEnabled(nextDark)
                                    if (!nextDark && themePreset == ThemePreset.MIDNIGHT_DARK) {
                                        app.container.sessionPreferences.setThemePreset(ThemePreset.EMERALD_CORPORATE)
                                    }
                                }
                            }
                        )
                    } else {
                        val uid = user.uid
                        key(uid) {
                            val viewModel: AppViewModel = viewModel(
                                key = uid,
                                factory = AppViewModel.Factory(
                                    repository = app.container.repository,
                                    backupManager = app.container.backupManager,
                                    cloudSync = app.container.cloudSyncRepository,
                                    sessionPreferences = app.container.sessionPreferences,
                                    driveBackupManager = app.container.driveBackupManager,
                                    appContext = app.applicationContext,
                                    gmailVerificationManager = app.container.gmailVerificationManager
                                )
                            )
                            ConcordGasRootApp(
                                viewModel = viewModel,
                                googleEmail = user.email.orEmpty(),
                                isSplashVisible = showSplash,
                                onPreviewStartupSplash = { showSplash = true }
                            )
                        }
                    }

                    if (showSplash) {
                        LaunchedEffect(Unit) {
                            SoundEffectsManager.playStartupChime(soundEnabled)
                        }
                        StartupSplashOverlay(
                            themePreset = themePreset,
                            soundEnabled = soundEnabled,
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConcordGasRootApp(
    viewModel: AppViewModel,
    googleEmail: String = "",
    isSplashVisible: Boolean = false,
    onPreviewStartupSplash: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceConnectionState.collectAsStateWithLifecycle()
    val cloudLoginMetadata by viewModel.cloudLoginMetadata.collectAsStateWithLifecycle()
    val cloudBusy by viewModel.cloudActionBusy.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val effectiveDarkMode = darkModeEnabled || themePreset == ThemePreset.MIDNIGHT_DARK
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val autoLoginEnabled by viewModel.autoLoginEnabled.collectAsStateWithLifecycle()
    val autoLogoutMode by viewModel.autoLogoutMode.collectAsStateWithLifecycle()
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
    val recentPurchaseCompanies by viewModel.recentPurchaseCompanies.collectAsStateWithLifecycle()
    val recentCustomerNames by viewModel.recentCustomerNames.collectAsStateWithLifecycle()
    val allCustomerPendingRows by viewModel.allCustomerPendingRows.collectAsStateWithLifecycle()
    val selectedVehiclePendingRows by viewModel.selectedVehiclePendingRows.collectAsStateWithLifecycle()
    val staffVehiclePendingRows by viewModel.staffVehiclePendingRows.collectAsStateWithLifecycle()
    val warningAlert by viewModel.warningAlert.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val driveBackupState by viewModel.driveBackupState.collectAsStateWithLifecycle()
    val driveConsentRequest by viewModel.driveConsentRequest.collectAsStateWithLifecycle()
    val pendingDriveRestoreJson by viewModel.pendingDriveRestoreJson.collectAsStateWithLifecycle()
    val permissionsStatus by viewModel.permissionsStatus.collectAsStateWithLifecycle()
    val lowStockAlert by viewModel.lowStockAlertState.collectAsStateWithLifecycle()
    val adminCredentials by viewModel.adminCredentialsState.collectAsStateWithLifecycle()
    val adminRecoveryState by viewModel.adminRecoveryState.collectAsStateWithLifecycle()
    val gmailConsentRequest by viewModel.gmailConsentRequest.collectAsStateWithLifecycle()
    val ownerViewConfigured by viewModel.ownerViewConfigured.collectAsStateWithLifecycle()
    val staffLoginNotifications by viewModel.staffLoginNotifications.collectAsStateWithLifecycle()
    val latestStaffLoginBanner by viewModel.latestStaffLoginBanner.collectAsStateWithLifecycle()
    val biometricUnlockState by viewModel.biometricUnlockState.collectAsStateWithLifecycle()
    val biometricAvailability by viewModel.biometricAvailability.collectAsStateWithLifecycle()
    val shouldAutoPromptBiometric by viewModel.shouldAutoPromptBiometric.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    var showAppUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var checkedUpdateOnce by rememberSaveable { mutableStateOf(false) }
    var showQuickCalculator by rememberSaveable { mutableStateOf(false) }
    var promptedNotifPermission by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authSession != null) {
        if (authSession != null && !checkedUpdateOnce) {
            checkedUpdateOnce = true
            viewModel.checkForAppUpdate(silent = true)
        }
    }

    val triggerBiometricUnlock: () -> Unit = {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity == null) {
            viewModel.showMessage("Biometric prompt unavailable in current window. Sign in with Password / PIN.")
        } else {
            val roleLabel = when (biometricUnlockState.role) {
                UserRole.ADMIN -> "Admin"
                UserRole.OWNER_VIEW -> "Owner View"
                UserRole.STAFF -> "Staff"
                null -> "User"
            }
            val userDisplay = biometricUnlockState.displayName.ifBlank { roleLabel }
            BiometricAuthManager.authenticate(
                activity = fragmentActivity,
                title = "Concord & Atoz Gas Trading LLC",
                subtitle = "Unlock $roleLabel ($userDisplay) with Fingerprint or Face",
                negativeButtonText = "Use Password / PIN",
                onSuccess = {
                    SoundEffectsManager.playSuccessChime(soundEnabled)
                    viewModel.completeBiometricUnlock()
                },
                onError = { message ->
                    viewModel.showMessage(message)
                },
                onCancelled = { /* Stay on Login screen so user can tap retry or enter password */ }
            )
        }
    }

    LaunchedEffect(
        authSession == null,
        isSplashVisible,
        shouldAutoPromptBiometric,
        biometricUnlockState.isReadyForBiometric,
        biometricAvailability
    ) {
        if (authSession == null &&
            !isSplashVisible &&
            shouldAutoPromptBiometric &&
            biometricUnlockState.isReadyForBiometric &&
            biometricAvailability == BiometricAvailability.AVAILABLE
        ) {
            viewModel.consumeAutoPromptBiometric()
            triggerBiometricUnlock()
        }
    }

    // Subscribe to report flows so report tables stay live
    viewModel.monthlySalesReport.collectAsStateWithLifecycle()
    viewModel.productWiseSales.collectAsStateWithLifecycle()
    viewModel.staffWiseSales.collectAsStateWithLifecycle()
    val companyStockReport by viewModel.companyStockReport.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsStatus()
    }

    LaunchedEffect(authSession?.user?.role, permissionsStatus.requiresNotificationPrompt) {
        if (authSession != null &&
            !promptedNotifPermission &&
            permissionsStatus.requiresNotificationPrompt &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            promptedNotifPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        viewModel.handleDriveConsentResult(res.resultCode, res.data)
    }

    val gmailConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        viewModel.handleGmailConsentResult(res.resultCode, res.data)
    }

    LaunchedEffect(driveConsentRequest) {
        driveConsentRequest?.let { pending ->
            try {
                driveConsentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } catch (_: Exception) {
                viewModel.dismissDriveConsentRequest()
            }
        }
    }

    LaunchedEffect(gmailConsentRequest) {
        gmailConsentRequest?.let { pending ->
            try {
                gmailConsentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } catch (_: Exception) {
                viewModel.dismissGmailConsentRequest()
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            SoundEffectsManager.playSuccessChime(soundEnabled)
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(warningAlert) {
        if (warningAlert != null) {
            SoundEffectsManager.playWarningTone(soundEnabled)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundedAtMs by remember { mutableStateOf<Long?>(null) }
    DisposableEffect(lifecycleOwner, autoLogoutMode, authSession != null) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (authSession != null) {
                        backgroundedAtMs = System.currentTimeMillis()
                        if (autoLogoutMode == AutoLogoutMode.ON_MINIMIZE) {
                            SoundEffectsManager.playWarningTone(soundEnabled)
                            viewModel.lockSessionOnAutoLogout("Session locked automatically on minimize.")
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissionsStatus()
                    viewModel.refreshBiometricAvailability()
                    val pausedAt = backgroundedAtMs
                    backgroundedAtMs = null
                    if (authSession != null && pausedAt != null) {
                        val elapsed = System.currentTimeMillis() - pausedAt
                        val shouldLock = when (autoLogoutMode) {
                            AutoLogoutMode.OFF -> false
                            AutoLogoutMode.ON_MINIMIZE -> true
                            AutoLogoutMode.IDLE_5_MIN,
                            AutoLogoutMode.IDLE_15_MIN -> elapsed >= autoLogoutMode.idleTimeoutMs
                        }
                        if (shouldLock) {
                            SoundEffectsManager.playWarningTone(soundEnabled)
                            viewModel.lockSessionOnAutoLogout("Session locked by ${autoLogoutMode.label}.")
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val handleGoogleSignOut: () -> Unit = {
        signOutGoogle(
            credentialManager = credentialManager,
            scope = scope,
            onPreSignOutCleanup = { viewModel.stopCloudSyncBeforeSignOut() }
        )
    }

    val session = authSession
    var calcBottomClearanceDp by remember { mutableStateOf(20.dp) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (session == null) {
            calcBottomClearanceDp = 20.dp
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    LoginScreen(
                        users = users,
                        loginError = loginError,
                        googleEmail = googleEmail,
                        workspaceState = workspaceState,
                        cloudBusy = cloudBusy,
                        isAuthenticating = isAuthenticating,
                        linkedStaffUsername = cloudLoginMetadata.linkedStaffUsername,
                        linkedStaffRole = cloudLoginMetadata.linkedStaffRole,
                        themePreset = themePreset,
                        darkModeEnabled = effectiveDarkMode,
                        soundEnabled = soundEnabled,
                        autoLoginEnabled = autoLoginEnabled,
                        adminCredentials = adminCredentials,
                        adminRecoveryState = adminRecoveryState,
                        ownerViewConfigured = ownerViewConfigured,
                        biometricUnlockState = biometricUnlockState,
                        biometricAvailability = biometricAvailability,
                        onRequestBiometricUnlock = triggerBiometricUnlock,
                        onThemeCycle = {
                            SoundEffectsManager.playTapTick(soundEnabled)
                            val next = ThemePreset.entries[(themePreset.ordinal + 1) % ThemePreset.entries.size]
                            viewModel.setThemePreset(next)
                        },
                        onToggleDarkMode = {
                            SoundEffectsManager.playTapTick(soundEnabled)
                            viewModel.toggleDarkMode()
                        },
                        onToggleSound = {
                            val next = !soundEnabled
                            viewModel.setSoundEnabled(next)
                            if (next) SoundEffectsManager.playSuccessChime(true)
                        },
                        onToggleAutoLogin = viewModel::setAutoLoginEnabled,
                        onJoinWorkspace = viewModel::joinWorkspace,
                        onCreateWorkspace = viewModel::createStandaloneWorkspace,
                        onRetryCloud = viewModel::retryCloudConnect,
                        onGoogleSignOut = handleGoogleSignOut,
                        onLogin = viewModel::login,
                        onSetupAdminCredentials = viewModel::setupInitialAdminCredentials,
                        onLoginAdminWithPassword = viewModel::loginAdminWithPassword,
                        onSetupOwnerViewPassword = viewModel::setupOwnerViewPassword,
                        onLoginOwnerViewWithPassword = viewModel::loginOwnerViewWithPassword,
                        onOpenAdminRecovery = { purpose -> viewModel.openAdminRecovery(purpose, googleEmail) },
                        onSendRecoveryCode = { email -> viewModel.sendAdminRecoveryCode(email) },
                        onVerifyPasswordReset = viewModel::verifyCodeAndResetAdminPassword,
                        onVerifyUsernameRecovery = viewModel::verifyCodeAndRecoverAdminUsername,
                        onUpdateRecoveredUsername = viewModel::updateAdminUsernameAfterRecovery,
                        onClearRecoveryError = viewModel::clearAdminRecoveryError,
                        onDismissRecovery = viewModel::dismissAdminRecovery,
                        onClearError = viewModel::clearLoginError
                    )
                }
            }
        } else if (session.user.role == UserRole.STAFF) {
            calcBottomClearanceDp = 20.dp
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
                        allVehicles = vehicles,
                        selectedDate = selectedDate,
                        vehicleRows = staffVehicleRows,
                        storeRows = storeRows,
                        products = activeProducts,
                        allTransactions = allTransactions,
                        pendingCustomers = staffVehiclePendingRows,
                        recentCustomerNames = recentCustomerNames,
                        isOnlineSynced = workspaceState is WorkspaceConnectionState.Connected,
                        autoLoginEnabled = autoLoginEnabled,
                        autoLogoutMode = autoLogoutMode,
                        darkModeEnabled = effectiveDarkMode,
                        onToggleDarkMode = {
                            SoundEffectsManager.playTapTick(soundEnabled)
                            viewModel.toggleDarkMode()
                        },
                        onAutoLoginChanged = viewModel::setAutoLoginEnabled,
                        onAutoLogoutModeChanged = viewModel::setAutoLogoutMode,
                        onDateSelected = viewModel::setSelectedDate,
                        onRecordTransaction = { type, pId, vId, qty, notes, custName, empNow ->
                            viewModel.recordTransaction(
                                type = type,
                                productId = pId,
                                vehicleId = vId,
                                quantity = qty,
                                notes = notes,
                                customerName = custName,
                                emptyCollectedNow = empNow
                            )
                        },
                        onTransferBetweenVehicles = viewModel::transferBetweenVehicles,
                        appUpdateState = appUpdateState,
                        onOpenAppUpdate = {
                            showAppUpdateDialog = true
                            viewModel.checkForAppUpdate(silent = false)
                        },
                        onDismissAppUpdateBanner = viewModel::dismissAppUpdateBanner,
                        onLogout = viewModel::logout
                    )
                }
            }
        } else {
            val isReadOnly = session.user.role == UserRole.OWNER_VIEW
            val navTabs = remember(isReadOnly) {
                if (isReadOnly) {
                    AdminNavTab.entries.filter { it != AdminNavTab.SETTINGS }
                } else {
                    AdminNavTab.entries
                }
            }
            var currentTab by rememberSaveable { mutableStateOf(AdminNavTab.DASHBOARD) }
            var viewingVehicleDetail by rememberSaveable { mutableStateOf(false) }
            var viewingCompanyInventory by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(isReadOnly) {
                if (isReadOnly && currentTab == AdminNavTab.SETTINGS) {
                    currentTab = AdminNavTab.DASHBOARD
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp
                val showBottomBar = !useRail && !viewingVehicleDetail && !viewingCompanyInventory
                calcBottomClearanceDp = if (showBottomBar) 92.dp else 20.dp

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                navTabs.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            currentTab = tab
                                            viewingVehicleDetail = false
                                            viewingCompanyInventory = false
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
                        if (useRail && !viewingVehicleDetail && !viewingCompanyInventory) {
                            NavigationRail(modifier = Modifier.statusBarsPadding()) {
                                navTabs.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            currentTab = tab
                                            viewingVehicleDetail = false
                                            viewingCompanyInventory = false
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
                            if (viewingCompanyInventory) {
                                CompanyInventoryScreen(
                                    selectedDate = selectedDate,
                                    stockRows = companyStockReport,
                                    isReadOnly = isReadOnly,
                                    onDateSelected = viewModel::setSelectedDate,
                                    onOpenProductMaster = {
                                        if (!isReadOnly) {
                                            viewingCompanyInventory = false
                                            currentTab = AdminNavTab.SETTINGS
                                        }
                                    },
                                    onOpenStockReport = {
                                        viewingCompanyInventory = false
                                        currentTab = AdminNavTab.REPORTS
                                    },
                                    onBack = { viewingCompanyInventory = false }
                                )
                            } else if (viewingVehicleDetail) {
                                val activeVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
                                VehicleDetailScreen(
                                    vehicle = activeVehicle,
                                    allVehicles = vehicles,
                                    selectedDate = selectedDate,
                                    rows = selectedVehicleRows,
                                    storeRows = storeRows,
                                    products = activeProducts,
                                    transactions = allTransactions,
                                    pendingCustomers = selectedVehiclePendingRows,
                                    recentCustomerNames = recentCustomerNames,
                                    isReadOnly = isReadOnly,
                                    onDateSelected = viewModel::setSelectedDate,
                                    onBack = { viewingVehicleDetail = false },
                                    onRecordTransaction = { type, pId, vId, qty, notes, custName, empNow ->
                                        viewModel.recordTransaction(
                                            type = type,
                                            productId = pId,
                                            vehicleId = vId,
                                            quantity = qty,
                                            notes = notes,
                                            customerName = custName,
                                            emptyCollectedNow = empNow
                                        )
                                    },
                                    onTransferBetweenVehicles = viewModel::transferBetweenVehicles,
                                    onUpdateTransaction = viewModel::updateTransaction,
                                    onDeleteTransaction = viewModel::deleteTransaction,
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
                                val effectiveTab = if (isReadOnly && currentTab == AdminNavTab.SETTINGS) {
                                    AdminNavTab.DASHBOARD
                                } else {
                                    currentTab
                                }
                                when (effectiveTab) {
                                    AdminNavTab.DASHBOARD -> AdminDashboardScreen(
                                        adminName = session.user.displayName,
                                        selectedDate = selectedDate,
                                        summary = dashboardSummary,
                                        storeRows = storeRows,
                                        products = allProducts,
                                        pendingCustomerCount = allCustomerPendingRows.count { it.pendingEmptyBalance > 0 },
                                        totalPendingEmptyCount = allCustomerPendingRows.sumOf { it.pendingEmptyBalance.coerceAtLeast(0) },
                                        lowStockAlert = lowStockAlert,
                                        isReadOnly = isReadOnly,
                                        staffLoginNotifications = staffLoginNotifications,
                                        latestStaffLoginBanner = latestStaffLoginBanner,
                                        onDismissStaffLoginBanner = viewModel::dismissStaffLoginBanner,
                                        darkModeEnabled = effectiveDarkMode,
                                        onToggleDarkMode = {
                                            SoundEffectsManager.playTapTick(soundEnabled)
                                            viewModel.toggleDarkMode()
                                        },
                                        appUpdateState = appUpdateState,
                                        onOpenAppUpdate = {
                                            showAppUpdateDialog = true
                                            viewModel.checkForAppUpdate(silent = false)
                                        },
                                        onDismissAppUpdateBanner = viewModel::dismissAppUpdateBanner,
                                        onDateSelected = viewModel::setSelectedDate,
                                        onOpenInventory = { viewingCompanyInventory = true },
                                        onOpenStore = { currentTab = AdminNavTab.STORE },
                                        onOpenVehicles = { currentTab = AdminNavTab.VEHICLES },
                                        onOpenVehicleDetail = { vehicleId ->
                                            viewModel.selectVehicle(vehicleId)
                                            currentTab = AdminNavTab.VEHICLES
                                            viewingVehicleDetail = true
                                        },
                                        onOpenReports = { currentTab = AdminNavTab.REPORTS },
                                        onUpdateLowStockThreshold = viewModel::updateLowStockThreshold,
                                        onDismissLowStockAlert = viewModel::dismissLowStockAlert,
                                        onUpdateTransaction = viewModel::updateTransaction,
                                        onLogout = viewModel::logout
                                    )

                                    AdminNavTab.STORE -> StoreManagementScreen(
                                        selectedDate = selectedDate,
                                        storeRows = storeRows,
                                        products = activeProducts,
                                        allProducts = allProducts,
                                        vehicles = vehicles,
                                        transactions = allTransactions,
                                        recentPurchaseCompanies = recentPurchaseCompanies,
                                        recentCustomerNames = recentCustomerNames,
                                        isReadOnly = isReadOnly,
                                        onDateSelected = viewModel::setSelectedDate,
                                        onRecordTransaction = { type, pId, vId, qty, notes, company, custName ->
                                            viewModel.recordTransaction(
                                                type = type,
                                                productId = pId,
                                                vehicleId = vId,
                                                quantity = qty,
                                                notes = notes,
                                                purchaseCompanyName = company,
                                                customerName = custName
                                            )
                                        },
                                        onUpdateOpeningStock = { productId, full, empty ->
                                            viewModel.updateOpeningStock(
                                                scope = StockCalculator.SCOPE_STORE,
                                                vehicleId = 0L,
                                                productId = productId,
                                                openingFull = full,
                                                openingEmpty = empty
                                            )
                                        },
                                        onUpdateTransaction = viewModel::updateTransaction,
                                        onDeleteTransaction = viewModel::deleteTransaction
                                    )

                                    AdminNavTab.VEHICLES -> VehicleManagementScreen(
                                        selectedDate = selectedDate,
                                        vehicleOverviews = dashboardSummary?.vehicleOverviews.orEmpty(),
                                        isReadOnly = isReadOnly,
                                        onDateSelected = viewModel::setSelectedDate,
                                        onSelectVehicle = { vehicleId ->
                                            viewModel.selectVehicle(vehicleId)
                                            viewingVehicleDetail = true
                                        },
                                        onAddStaffAndVehicle = viewModel::addStaffAndVehicle
                                    )

                                    AdminNavTab.REPORTS -> ReportsScreen(
                                        selectedDate = selectedDate,
                                        selectedYearMonth = selectedYearMonth,
                                        vehicles = vehicles,
                                        selectedVehicleId = selectedVehicleId,
                                        recentPurchaseCompanies = recentPurchaseCompanies,
                                        isReadOnly = isReadOnly,
                                        buildReportTable = viewModel::buildReportTable,
                                        onDateSelected = viewModel::setSelectedDate,
                                        onYearMonthSelected = viewModel::setSelectedYearMonth,
                                        onSelectVehicle = { id -> if (id != null) viewModel.selectVehicle(id) },
                                        onSaveReportCloudAndDrive = viewModel::saveReportToCloudAndDrive,
                                        onMessage = viewModel::showMessage
                                    )

                                    AdminNavTab.SETTINGS -> AdminSettingsScreen(
                                        users = users,
                                        vehicles = vehicles,
                                        products = allProducts,
                                        googleEmail = googleEmail,
                                        workspaceState = workspaceState,
                                        driveBackupState = driveBackupState,
                                        permissionsStatus = permissionsStatus,
                                        pendingDriveRestoreJson = pendingDriveRestoreJson,
                                        themePreset = themePreset,
                                        darkModeEnabled = effectiveDarkMode,
                                        soundEnabled = soundEnabled,
                                        autoLoginEnabled = autoLoginEnabled,
                                        autoLogoutMode = autoLogoutMode,
                                        adminCredentials = adminCredentials,
                                        ownerViewConfigured = ownerViewConfigured,
                                        appUpdateState = appUpdateState,
                                        onCheckForAppUpdate = { viewModel.checkForAppUpdate(silent = false) },
                                        onOpenAppUpdate = { showAppUpdateDialog = true },
                                        onThemePresetChanged = { preset ->
                                            SoundEffectsManager.playTapTick(soundEnabled)
                                            viewModel.setThemePreset(preset)
                                        },
                                        onDarkModeChanged = { enabled ->
                                            SoundEffectsManager.playTapTick(soundEnabled)
                                            viewModel.setDarkModeEnabled(enabled)
                                        },
                                        onSoundEnabledChanged = { enabled ->
                                            viewModel.setSoundEnabled(enabled)
                                            if (enabled) SoundEffectsManager.playSuccessChime(true)
                                        },
                                        onAutoLoginChanged = viewModel::setAutoLoginEnabled,
                                        onAutoLogoutModeChanged = viewModel::setAutoLogoutMode,
                                        onUpdateAdminCredentials = viewModel::updateAdminCredentialsFromSettings,
                                        onUpdateOwnerViewPassword = viewModel::updateOwnerViewPasswordFromSettings,
                                        onPreviewStartupSplash = onPreviewStartupSplash,
                                        onSyncNow = viewModel::syncNow,
                                        onGoogleSignOut = handleGoogleSignOut,
                                        onGrantAllPermissionsAndDrive = viewModel::requestGoogleDriveAuthorizationAndEnable,
                                        onToggleAutoDriveBackup = viewModel::setAutoDriveBackupEnabled,
                                        onBackUpToDriveNow = { viewModel.backUpToGoogleDriveNow() },
                                        onRestoreFromDriveNow = { viewModel.restoreFromGoogleDriveNow() },
                                        onConfirmDriveRestore = viewModel::confirmPendingDriveRestore,
                                        onDismissDriveRestore = viewModel::dismissPendingDriveRestore,
                                        onRefreshPermissions = { viewModel.refreshPermissionsStatus() },
                                        onAddStaffAndVehicle = viewModel::addStaffAndVehicle,
                                        onUpdateStaffName = viewModel::updateStaffName,
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

        // Small always-accessible floating calculator icon button
        FloatingCalculatorIconButton(
            onClick = {
                SoundEffectsManager.playTapTick(soundEnabled)
                showQuickCalculator = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = calcBottomClearanceDp)
        )
    }

    if (showQuickCalculator) {
        QuickCalculatorPopupDialog(
            onDismiss = { showQuickCalculator = false },
            onResultCopied = { msg -> viewModel.showMessage(msg) }
        )
    }

    if (showAppUpdateDialog) {
        val roleLabel = when (authSession?.user?.role) {
            UserRole.ADMIN -> "Admin Build"
            UserRole.OWNER_VIEW -> "Owner View Build"
            UserRole.STAFF -> "Staff Build"
            null -> "C&A Gas Build"
        }
        AppUpdateDialog(
            state = appUpdateState,
            roleLabel = roleLabel,
            onCheckForUpdate = { viewModel.checkForAppUpdate(silent = false) },
            onDismiss = { showAppUpdateDialog = false },
            onMessage = viewModel::showMessage
        )
    }

    warningAlert?.let { alert ->
        InsufficientStockWarningDialog(
            alert = alert,
            onDismiss = viewModel::dismissWarningAlert
        )
    }
}
