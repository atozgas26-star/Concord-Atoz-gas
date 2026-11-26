package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AdminRecoveryUiState
import com.example.auth.BiometricAvailability
import com.example.auth.RecoveryPurpose
import com.example.data.local.AdminCredentialsState
import com.example.data.local.BiometricUnlockState
import com.example.data.local.UserAccountEntity
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.data.remote.WorkspaceConnectionState
import com.example.ui.components.AdminRecoveryDialog
import com.example.ui.components.StaffListPickerSheet
import com.example.ui.components.StaffOneClickSelectorCard
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    users: List<UserAccountEntity>,
    loginError: String?,
    googleEmail: String = "",
    workspaceState: WorkspaceConnectionState = WorkspaceConnectionState.Checking,
    cloudBusy: Boolean = false,
    isAuthenticating: Boolean = false,
    linkedStaffUsername: String = "",
    linkedStaffRole: UserRole? = null,
    themePreset: ThemePreset = ThemePreset.EMERALD_CORPORATE,
    darkModeEnabled: Boolean = false,
    soundEnabled: Boolean = true,
    autoLoginEnabled: Boolean = true,
    adminCredentials: AdminCredentialsState = AdminCredentialsState(),
    adminRecoveryState: AdminRecoveryUiState = AdminRecoveryUiState(),
    ownerViewConfigured: Boolean = false,
    biometricUnlockState: BiometricUnlockState = BiometricUnlockState(),
    biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
    onRequestBiometricUnlock: () -> Unit = {},
    onThemeCycle: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    onToggleSound: () -> Unit = {},
    onToggleAutoLogin: (Boolean) -> Unit = {},
    onJoinWorkspace: (String) -> Unit = {},
    onCreateWorkspace: () -> Unit = {},
    onRetryCloud: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onLogin: (userId: Long, pin: String, role: UserRole) -> Unit,
    onSetupAdminCredentials: (username: String, password: String, confirmPassword: String, recoveryEmail: String) -> Unit = { _, _, _, _ -> },
    onLoginAdminWithPassword: (username: String, password: String) -> Unit = { _, _ -> },
    onSetupOwnerViewPassword: (password: String, confirmPassword: String) -> Unit = { _, _ -> },
    onLoginOwnerViewWithPassword: (password: String) -> Unit = {},
    onOpenAdminRecovery: (RecoveryPurpose) -> Unit = {},
    onSendRecoveryCode: (String) -> Unit = {},
    onVerifyPasswordReset: (code: String, newPassword: String, confirmPassword: String) -> Unit = { _, _, _ -> },
    onVerifyUsernameRecovery: (code: String) -> Unit = {},
    onUpdateRecoveredUsername: (String) -> Unit = {},
    onClearRecoveryError: () -> Unit = {},
    onDismissRecovery: () -> Unit = {},
    onClearError: () -> Unit
) {
    val isStaffRestrictedCloud = workspaceState is WorkspaceConnectionState.Connected &&
        !workspaceState.isOwnerOrAdmin &&
        linkedStaffRole != UserRole.ADMIN
    var userTouchedRole by remember { mutableStateOf(false) }
    var selectedRole by remember {
        mutableStateOf(
            if (isStaffRestrictedCloud) UserRole.STAFF else (linkedStaffRole ?: UserRole.STAFF)
        )
    }
    val adminUser = remember(users) { users.firstOrNull { it.role == UserRole.ADMIN } }
    val staffUsers = remember(users) { users.filter { it.role == UserRole.STAFF } }
    var selectedStaffId by remember(staffUsers) { mutableStateOf(staffUsers.firstOrNull()?.id) }
    var showStaffListSheet by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showStaffPin by remember { mutableStateOf(false) }

    // Admin Username & Password inputs (Username + Password only)
    var adminUsernameInput by remember(adminCredentials.username) {
        mutableStateOf(adminCredentials.username)
    }
    var adminPasswordInput by remember { mutableStateOf("") }
    var showAdminPassword by remember { mutableStateOf(false) }

    // First-run Admin Setup inputs
    var setupUsernameInput by remember { mutableStateOf("admin_ragavan") }
    var setupPasswordInput by remember { mutableStateOf("") }
    var setupConfirmPasswordInput by remember { mutableStateOf("") }
    var setupRecoveryEmailInput by remember(googleEmail) {
        mutableStateOf(googleEmail.ifBlank { "atozgas26@gmail.com" })
    }
    var showSetupPassword by remember { mutableStateOf(false) }

    // Owner View (Read-Only) Password-only inputs
    var ownerPasswordInput by remember { mutableStateOf("") }
    var ownerConfirmPasswordInput by remember { mutableStateOf("") }
    var showOwnerPassword by remember { mutableStateOf(false) }

    LaunchedEffect(isStaffRestrictedCloud, linkedStaffRole, linkedStaffUsername, users) {
        if (isStaffRestrictedCloud && selectedRole == UserRole.ADMIN) {
            selectedRole = UserRole.STAFF
        } else if (!userTouchedRole) {
            linkedStaffRole?.let { selectedRole = it }
            if (linkedStaffUsername.isNotBlank()) {
                staffUsers.firstOrNull { it.username == linkedStaffUsername }?.let { match ->
                    selectedStaffId = match.id
                }
            }
        }
    }

    val targetUser = when (selectedRole) {
        UserRole.ADMIN -> adminUser
        UserRole.OWNER_VIEW -> null
        UserRole.STAFF -> staffUsers.firstOrNull { it.id == selectedStaffId } ?: staffUsers.firstOrNull()
    }

    LaunchedEffect(selectedRole, selectedStaffId) {
        pinInput = ""
        adminPasswordInput = ""
        ownerPasswordInput = ""
        ownerConfirmPasswordInput = ""
        onClearError()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Company Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_company_banner),
                    contentDescription = "C&A Gas Industrial Cylinders",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    BrandGreenDark.copy(alpha = 0.78f),
                                    BrandGreenDark.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = BrandRedSecondary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "INDUSTRIAL & LPG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when (workspaceState) {
                                        is WorkspaceConnectionState.Connected -> "ONLINE SYNC ACTIVE"
                                        is WorkspaceConnectionState.NeedsSetup -> "SETUP SYNC"
                                        is WorkspaceConnectionState.Error -> "LOCAL QUEUE"
                                        WorkspaceConnectionState.Checking -> "CONNECTING..."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.testTag("login_dark_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (darkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (darkModeEnabled) "Switch to Light Mode" else "Switch to Warehouse Dark Mode",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF092C6C),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_ca_red_logo),
                                contentDescription = "C&A Gas Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONCORD & ATOZ GAS TRADING LLC",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("login_company_headline")
                            )
                            Text(
                                text = "C&A Gas • Store, Vehicle & Sales Management",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BrandRedSecondary)
            )

            // Main Login Card
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CloudSyncStatusBanner(
                    googleEmail = googleEmail,
                    workspaceState = workspaceState,
                    busy = cloudBusy,
                    onJoinWorkspace = onJoinWorkspace,
                    onCreateWorkspace = onCreateWorkspace,
                    onRetryCloud = onRetryCloud,
                    onGoogleSignOut = onGoogleSignOut
                )

                // Biometric Quick Unlock (Fingerprint / Face Lock) OR Logout Password-Once Notice
                if (biometricUnlockState.isReadyForBiometric &&
                    biometricAvailability == BiometricAvailability.AVAILABLE &&
                    !(biometricUnlockState.role == UserRole.ADMIN && isStaffRestrictedCloud)
                ) {
                    val savedRoleLabel = when (biometricUnlockState.role) {
                        UserRole.ADMIN -> "Admin"
                        UserRole.OWNER_VIEW -> "Owner View (Read-Only)"
                        UserRole.STAFF -> "Staff"
                        null -> ""
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandGreenContainer),
                        border = BorderStroke(1.5.dp, BrandGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_quick_unlock_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Fingerprint or Face Unlock",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fingerprint / Face Unlock Ready",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenOnContainer
                                    )
                                    Text(
                                        text = "${biometricUnlockState.displayName.ifBlank { savedRoleLabel }} • $savedRoleLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandGreenOnContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Face Unlock",
                                    tint = BrandGreenPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Button(
                                onClick = onRequestBiometricUnlock,
                                enabled = !isAuthenticating,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("biometric_unlock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Unlock with Fingerprint / Face",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (biometricUnlockState.passwordRequiredAfterLogout) {
                    Surface(
                        color = BrandRedContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandRedSecondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_logout_reset_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Required After Logout",
                                tint = BrandRedSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Logged out — Sign in 1 time with Password / PIN to re-enable Fingerprint & Face Lock.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (biometricAvailability == BiometricAvailability.AVAILABLE) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "First-time login uses Password / PIN • Next open unlocks with Fingerprint or Face Lock.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Separate Login Portals with Unique Icons (Admin, Owner View, Staff)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CHOOSE LOGIN PORTAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RolePortalCard(
                            title = "Admin",
                            subtitle = if (isStaffRestrictedCloud) "Locked" else "User + Pass",
                            icon = Icons.Default.AdminPanelSettings,
                            accentColor = Color(0xFF1D4ED8),
                            badgeContainer = Color(0xFFDBEAFE),
                            badgeTint = Color(0xFF1E3A8A),
                            selected = selectedRole == UserRole.ADMIN,
                            enabled = !isStaffRestrictedCloud,
                            onClick = {
                                if (!isStaffRestrictedCloud) {
                                    userTouchedRole = true
                                    selectedRole = UserRole.ADMIN
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_admin_tab")
                        )
                        RolePortalCard(
                            title = "Owner View",
                            subtitle = "4-Digit PIN",
                            icon = Icons.Default.VerifiedUser,
                            accentColor = Color(0xFF4338CA),
                            badgeContainer = Color(0xFFE0E7FF),
                            badgeTint = Color(0xFF312E81),
                            selected = selectedRole == UserRole.OWNER_VIEW,
                            enabled = !isStaffRestrictedCloud || ownerViewConfigured,
                            onClick = {
                                userTouchedRole = true
                                selectedRole = UserRole.OWNER_VIEW
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_owner_view_tab")
                        )
                        RolePortalCard(
                            title = "Staff",
                            subtitle = "${staffUsers.size} Staff • PIN",
                            icon = Icons.Default.LocalShipping,
                            accentColor = Color(0xFF0F766E),
                            badgeContainer = Color(0xFFCCFBF1),
                            badgeTint = Color(0xFF115E59),
                            selected = selectedRole == UserRole.STAFF,
                            enabled = true,
                            onClick = {
                                userTouchedRole = true
                                selectedRole = UserRole.STAFF
                                if (staffUsers.isNotEmpty()) {
                                    showStaffListSheet = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_staff_tab")
                        )
                    }
                }

                // Account Selection
                if (selectedRole == UserRole.STAFF) {
                    StaffOneClickSelectorCard(
                        selectedStaff = targetUser,
                        totalStaffCount = staffUsers.size,
                        onOpenStaffList = { showStaffListSheet = true }
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (selectedRole == UserRole.ADMIN) Color(0xFF1D4ED8).copy(alpha = 0.35f)
                            else Color(0xFF4338CA).copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (selectedRole == UserRole.ADMIN) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDBEAFE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = "Admin Portal",
                                            tint = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Admin Portal • ${adminUser?.displayName ?: "Ragavan"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Full Store, Vehicle, Reports & Staff Management",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = "Owner View Portal",
                                            tint = Color(0xFF78350F),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Owner View Portal (Read-Only)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "View live stock, vehicles, staff logins & reports — no entry or modification allowed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedRole == UserRole.ADMIN) {
                    // Admin Username & Password Card (First-run Setup or Returning Login)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!adminCredentials.isConfigured) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin Setup",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Set Up Admin Username & Password",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Create your Admin username, password (min 6 characters), and recovery Gmail for 6-digit verification codes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = setupUsernameInput,
                                    onValueChange = {
                                        setupUsernameInput = it
                                        onClearError()
                                    },
                                    label = { Text("Admin Username") },
                                    placeholder = { Text("e.g. admin_ragavan") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_setup_username_input")
                                )

                                OutlinedTextField(
                                    value = setupPasswordInput,
                                    onValueChange = {
                                        setupPasswordInput = it
                                        onClearError()
                                    },
                                    label = { Text("Create Password") },
                                    placeholder = { Text("At least 6 characters") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showSetupPassword = !showSetupPassword }) {
                                            Icon(
                                                imageVector = if (showSetupPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showSetupPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSetupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_setup_password_input")
                                )

                                OutlinedTextField(
                                    value = setupConfirmPasswordInput,
                                    onValueChange = {
                                        setupConfirmPasswordInput = it
                                        onClearError()
                                    },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    visualTransformation = if (showSetupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_setup_confirm_password_input")
                                )

                                OutlinedTextField(
                                    value = setupRecoveryEmailInput,
                                    onValueChange = {
                                        setupRecoveryEmailInput = it
                                        onClearError()
                                    },
                                    label = { Text("Recovery Gmail Address") },
                                    placeholder = { Text("atozgas26@gmail.com") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AlternateEmail, contentDescription = null)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_setup_email_input")
                                )

                                if (loginError != null) {
                                    Surface(
                                        color = BrandRedContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = loginError,
                                            color = BrandRedBright,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSetupAdminCredentials(
                                            setupUsernameInput.trim(),
                                            setupPasswordInput,
                                            setupConfirmPasswordInput,
                                            setupRecoveryEmailInput.trim()
                                        )
                                    },
                                    enabled = !isAuthenticating &&
                                        setupUsernameInput.isNotBlank() &&
                                        setupPasswordInput.length >= 6 &&
                                        setupConfirmPasswordInput.isNotEmpty() &&
                                        setupRecoveryEmailInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("admin_setup_submit_button")
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Saving Admin Credentials...")
                                    } else {
                                        Text(
                                            text = "Save Credentials & Sign In (ADMIN)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Admin Login",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Admin Username & Password Sign In",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (workspaceState is WorkspaceConnectionState.Connected) {
                                        "Secured Admin Login • Gmail verification code recovery ready"
                                    } else {
                                        "Local Admin Login • Gmail verification code recovery ready"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = adminUsernameInput,
                                    onValueChange = {
                                        adminUsernameInput = it
                                        onClearError()
                                    },
                                    label = { Text("Admin Username") },
                                    placeholder = { Text("Enter admin username") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_username_input")
                                )

                                OutlinedTextField(
                                    value = adminPasswordInput,
                                    onValueChange = {
                                        adminPasswordInput = it
                                        onClearError()
                                    },
                                    label = { Text("Password") },
                                    placeholder = { Text("Enter admin password") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showAdminPassword = !showAdminPassword }) {
                                            Icon(
                                                imageVector = if (showAdminPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showAdminPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showAdminPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_password_input")
                                )

                                if (loginError != null) {
                                    Surface(
                                        color = BrandRedContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = loginError,
                                            color = BrandRedBright,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onLoginAdminWithPassword(
                                            adminUsernameInput.trim(),
                                            adminPasswordInput
                                        )
                                    },
                                    enabled = !isAuthenticating &&
                                        adminUsernameInput.isNotBlank() &&
                                        adminPasswordInput.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("admin_password_login_button")
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Verifying Admin Credentials...",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Sign In as Admin",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MarkEmailRead,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Gmail Verification Code Recovery",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onClearError()
                                            onOpenAdminRecovery(RecoveryPurpose.FORGOT_PASSWORD)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("forgot_password_button")
                                    ) {
                                        Icon(
                                            Icons.Default.LockReset,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Forgot Password", fontSize = 12.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onClearError()
                                            onOpenAdminRecovery(RecoveryPurpose.FORGOT_USERNAME)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("forgot_username_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Forgot Username", fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedRole == UserRole.OWNER_VIEW) {
                    // Owner View One-Password Card (Strictly Read-Only — No Entry or Modification)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = BrandGreenContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = BrandGreenOnContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "VIEW ONLY • No Entry or Modification",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenOnContainer
                                    )
                                }
                            }

                            if (!ownerViewConfigured) {
                                Text(
                                    text = "Create 4-Digit Owner View PIN",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Set one 4-digit numeric PIN using your mobile keyboard. Owner View shows live stock and reports in strictly read-only mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = ownerPasswordInput,
                                    onValueChange = { raw ->
                                        ownerPasswordInput = raw.filter { it.isDigit() }.take(4)
                                        onClearError()
                                    },
                                    label = { Text("Create 4-Digit Owner PIN") },
                                    placeholder = { Text("4 digits (e.g. 2468)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showOwnerPassword = !showOwnerPassword }) {
                                            Icon(
                                                imageVector = if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showOwnerPassword) "Hide PIN" else "Show PIN"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("owner_setup_password_input")
                                )

                                OutlinedTextField(
                                    value = ownerConfirmPasswordInput,
                                    onValueChange = { raw ->
                                        ownerConfirmPasswordInput = raw.filter { it.isDigit() }.take(4)
                                        onClearError()
                                    },
                                    label = { Text("Confirm 4-Digit Owner PIN") },
                                    placeholder = { Text("Re-enter 4-digit PIN") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (!isAuthenticating && ownerPasswordInput.length == 4 && ownerConfirmPasswordInput.length == 4) {
                                                onSetupOwnerViewPassword(ownerPasswordInput, ownerConfirmPasswordInput)
                                            }
                                        }
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("owner_setup_confirm_password_input")
                                )

                                if (loginError != null) {
                                    Surface(
                                        color = BrandRedContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = loginError,
                                            color = BrandRedBright,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSetupOwnerViewPassword(ownerPasswordInput, ownerConfirmPasswordInput)
                                    },
                                    enabled = !isAuthenticating &&
                                        ownerPasswordInput.length == 4 &&
                                        ownerConfirmPasswordInput.length == 4,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("owner_setup_submit_button")
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Creating Owner View PIN...")
                                    } else {
                                        Text(
                                            text = "Create 4-Digit PIN & Open Owner View",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Owner View 4-Digit PIN Sign In",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter the 4-digit Owner View PIN with your mobile keyboard to open read-only dashboards and reports.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = ownerPasswordInput,
                                    onValueChange = { raw ->
                                        ownerPasswordInput = raw.filter { it.isDigit() }.take(4)
                                        onClearError()
                                    },
                                    label = { Text("4-Digit Owner View PIN") },
                                    placeholder = { Text("Enter 4-digit PIN") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showOwnerPassword = !showOwnerPassword }) {
                                            Icon(
                                                imageVector = if (showOwnerPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showOwnerPassword) "Hide PIN" else "Show PIN"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showOwnerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (!isAuthenticating && ownerPasswordInput.length == 4) {
                                                onLoginOwnerViewWithPassword(ownerPasswordInput)
                                            }
                                        }
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("owner_login_password_input")
                                )

                                if (loginError != null) {
                                    Surface(
                                        color = BrandRedContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = loginError,
                                            color = BrandRedBright,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onLoginOwnerViewWithPassword(ownerPasswordInput) },
                                    enabled = !isAuthenticating && ownerPasswordInput.length == 4,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("owner_login_submit_button")
                                ) {
                                    if (isAuthenticating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Opening Owner View...")
                                    } else {
                                        Text(
                                            text = "Sign In to Owner View (Read-Only)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Staff Native Mobile Keyboard 4-Digit PIN Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (targetUser != null) {
                                    "${targetUser.displayName} • 4-Digit PIN Sign In"
                                } else {
                                    "Choose Staff Name First"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = if (workspaceState is WorkspaceConnectionState.Connected) {
                                    "Enter your 4-digit Staff PIN using your phone keyboard • Cloud Sync Verified"
                                } else {
                                    "Enter your 4-digit Staff PIN using your phone keyboard • Local Cache Ready"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { raw ->
                                    onClearError()
                                    pinInput = raw.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Staff 4-Digit PIN") },
                                placeholder = { Text("Enter 4-digit PIN") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showStaffPin = !showStaffPin }) {
                                        Icon(
                                            imageVector = if (showStaffPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showStaffPin) "Hide PIN" else "Show PIN"
                                        )
                                    }
                                },
                                visualTransformation = if (showStaffPin) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val user = targetUser
                                        if (user != null && !isAuthenticating && pinInput.length == 4) {
                                            onLogin(user.id, pinInput, selectedRole)
                                        }
                                    }
                                ),
                                enabled = targetUser != null && !isAuthenticating,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_pin_input")
                            )

                            if (loginError != null) {
                                Surface(
                                    color = BrandRedContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = loginError,
                                        color = BrandRedBright,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val user = targetUser ?: return@Button
                                    onLogin(user.id, pinInput, selectedRole)
                                },
                                enabled = targetUser != null && !isAuthenticating && pinInput.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("staff_login_submit_button")
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Signing In...")
                                } else {
                                    Text(
                                        text = if (targetUser != null) {
                                            "Sign In • ${targetUser.displayName}"
                                        } else {
                                            "Choose Staff Name to Sign In"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val showStaffBiometric = biometricUnlockState.isReadyForBiometric &&
                                biometricAvailability == BiometricAvailability.AVAILABLE &&
                                !(biometricUnlockState.role == UserRole.ADMIN && isStaffRestrictedCloud)
                            if (showStaffBiometric) {
                                OutlinedButton(
                                    onClick = onRequestBiometricUnlock,
                                    enabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("staff_biometric_shortcut_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Unlock with Fingerprint or Face",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Quick Unlock with Fingerprint / Face")
                                }
                            }
                        }
                    }
                }

                // Quick Controls: Auto-Login, Dark Mode, Theme & Sound + Credit
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = darkModeEnabled,
                        onClick = onToggleDarkMode,
                        leadingIcon = {
                            Icon(
                                if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Warehouse Dark Mode",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (darkModeEnabled) "Dark Mode" else "Light Mode", fontSize = 11.sp) },
                        modifier = Modifier.testTag("login_dark_mode_chip")
                    )
                    FilterChip(
                        selected = autoLoginEnabled,
                        onClick = { onToggleAutoLogin(!autoLoginEnabled) },
                        label = { Text(if (autoLoginEnabled) "Auto-Login: ON" else "Auto-Login: OFF", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onThemeCycle,
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = "Switch Theme", modifier = Modifier.size(16.dp)) },
                        label = { Text(themePreset.title, fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = soundEnabled,
                        onClick = onToggleSound,
                        leadingIcon = {
                            Icon(
                                if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Toggle Sound",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (soundEnabled) "Sound" else "Mute", fontSize = 11.sp) }
                    )
                }

                Text(
                    text = "Concord & Atoz Gas Trading LLC • C&A Gas • Built by Ragavan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }

    if (showStaffListSheet) {
        StaffListPickerSheet(
            staffUsers = staffUsers,
            selectedStaffId = targetUser?.id,
            onSelectStaff = { chosen ->
                selectedStaffId = chosen.id
                pinInput = ""
                onClearError()
                showStaffListSheet = false
            },
            onDismiss = { showStaffListSheet = false }
        )
    }

    if (adminRecoveryState.activePurpose != null) {
        AdminRecoveryDialog(
            state = adminRecoveryState,
            onSendCode = onSendRecoveryCode,
            onVerifyPasswordReset = onVerifyPasswordReset,
            onVerifyUsernameRecovery = onVerifyUsernameRecovery,
            onUpdateRecoveredUsername = onUpdateRecoveredUsername,
            onUseRecoveredUsername = { recovered ->
                adminUsernameInput = recovered
                onDismissRecovery()
            },
            onClearError = onClearRecoveryError,
            onDismiss = onDismissRecovery
        )
    }
}

@Composable
private fun RolePortalCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badgeContainer: Color,
    badgeTint: Color,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            selected -> accentColor
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (selected) 4.dp else 1.dp,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !enabled -> MaterialTheme.colorScheme.surfaceVariant
                            selected -> Color.White.copy(alpha = 0.22f)
                            else -> badgeContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Portal",
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.outline
                        selected -> Color.White
                        else -> badgeTint
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    selected -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    selected -> Color.White.copy(alpha = 0.88f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CloudSyncStatusBanner(
    googleEmail: String,
    workspaceState: WorkspaceConnectionState,
    busy: Boolean,
    onJoinWorkspace: (String) -> Unit,
    onCreateWorkspace: () -> Unit,
    onRetryCloud: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    var inviteCodeInput by remember { mutableStateOf("CONCORD2026") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (workspaceState) {
                        is WorkspaceConnectionState.Connected -> Icons.Default.CloudDone
                        is WorkspaceConnectionState.Error -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudSync
                    }
                    val tint = when (workspaceState) {
                        is WorkspaceConnectionState.Connected -> BrandGreenPrimary
                        is WorkspaceConnectionState.Error -> BrandRedSecondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(icon, contentDescription = "Cloud Sync Status", tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (workspaceState) {
                                is WorkspaceConnectionState.Connected ->
                                    "Online Sync Active (${workspaceState.memberCount} device${if (workspaceState.memberCount == 1) "" else "s"})"
                                is WorkspaceConnectionState.NeedsSetup ->
                                    "Connect Device to Online Sync"
                                is WorkspaceConnectionState.Error ->
                                    "Cloud Sync Reconnecting"
                                WorkspaceConnectionState.Checking ->
                                    "Connecting Online Sync..."
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (googleEmail.isNotBlank()) {
                            Text(
                                text = "Google: $googleEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onGoogleSignOut,
                    modifier = Modifier.testTag("switch_google_account_button")
                ) {
                    Text("Switch Google", style = MaterialTheme.typography.labelMedium)
                }
            }

            when (workspaceState) {
                is WorkspaceConnectionState.Connected -> {
                    Surface(
                        color = if (workspaceState.isOwnerOrAdmin) BrandGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (workspaceState.isOwnerOrAdmin) {
                                "Staff Invite Code: ${workspaceState.inviteCode}  •  Share with staff phones for live sync"
                            } else {
                                "Staff Role Active  •  Admin Settings & Financial Reports are restricted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (workspaceState.isOwnerOrAdmin) BrandGreenOnContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                is WorkspaceConnectionState.NeedsSetup -> {
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.uppercase() },
                        label = { Text("Company Invite Code (e.g. CONCORD2026)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workspace_invite_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onJoinWorkspace(inviteCodeInput) },
                            enabled = !busy && inviteCodeInput.trim().length >= 4,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_workspace_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Join Company")
                            }
                        }
                        OutlinedButton(
                            onClick = onCreateWorkspace,
                            enabled = !busy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_workspace_button")
                        ) {
                            Text("New Workspace")
                        }
                    }
                }

                is WorkspaceConnectionState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workspaceState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandRedBright,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryCloud) {
                            Text("Retry")
                        }
                    }
                }

                WorkspaceConnectionState.Checking -> Unit
            }
        }
    }
}
