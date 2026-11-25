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
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AdminRecoveryUiState
import com.example.auth.RecoveryPurpose
import com.example.data.local.AdminCredentialsState
import com.example.data.local.UserAccountEntity
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.data.remote.WorkspaceConnectionState
import com.example.ui.components.AdminRecoveryDialog
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
    var pinInput by remember { mutableStateOf("") }

    // Admin Username & Password inputs
    var adminUsernameInput by remember { mutableStateOf("") }
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

    LaunchedEffect(isStaffRestrictedCloud, linkedStaffRole, linkedStaffUsername, users) {
        if (isStaffRestrictedCloud) {
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

    val targetUser = if (selectedRole == UserRole.ADMIN) {
        adminUser
    } else {
        staffUsers.firstOrNull { it.id == selectedStaffId } ?: staffUsers.firstOrNull()
    }

    LaunchedEffect(selectedRole, selectedStaffId) {
        pinInput = ""
        adminPasswordInput = ""
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
                    contentDescription = "Concord & A to Z Gas Industrial Cylinders",
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
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_concord_logo_v2),
                                contentDescription = "Concord & A to Z Gas Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONCORD & A TO Z GAS",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cylinder Store, Vehicle & Sales Management",
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

                // Role Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleToggleButton(
                        title = "Staff Login",
                        subtitle = "${staffUsers.size} Vehicle Accounts",
                        icon = Icons.Default.LocalShipping,
                        selected = selectedRole == UserRole.STAFF,
                        onClick = {
                            userTouchedRole = true
                            selectedRole = UserRole.STAFF
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_staff_tab")
                    )
                    RoleToggleButton(
                        title = "Admin Login",
                        subtitle = if (isStaffRestrictedCloud) "Admin Only (Restricted)" else "Ragavan (Full Access)",
                        icon = Icons.Default.AdminPanelSettings,
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
                }

                // Account Selection
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (selectedRole == UserRole.ADMIN) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = BrandGreenOnContainer
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Admin: ${adminUser?.displayName ?: "Ragavan"}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Full Store, Vehicle, Reports & PIN Control",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Select Staff / Vehicle Account",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                staffUsers.forEach { staff ->
                                    val isSelected = staff.id == targetUser?.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStaffId = staff.id },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Badge,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = staff.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandGreenPrimary,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("staff_chip_${staff.username}")
                                    )
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
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
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
                } else {
                    // Staff 4-Digit Vehicle PIN Pad Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "PIN",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Enter Unique 4-Digit ${selectedRole.name} PIN (${targetUser?.displayName ?: ""})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (workspaceState is WorkspaceConnectionState.Connected) {
                                    "Firebase Auth active • Validating role & PIN via Firestore"
                                } else {
                                    "Firebase Auth active • Local PIN cache ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(14.dp))

                            // 4-Digit Masked PIN Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { index ->
                                    val filled = index < pinInput.length
                                    Surface(
                                        modifier = Modifier.size(width = 48.dp, height = 52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (filled) BrandGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (filled) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (filled) "●" else "",
                                                fontSize = 22.sp,
                                                fontFamily = JetBrainsMono,
                                                color = BrandGreenOnContainer
                                            )
                                        }
                                    }
                                }
                            }

                            if (loginError != null) {
                                Spacer(Modifier.height(10.dp))
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

                            Spacer(Modifier.height(14.dp))

                            // Tactile Numeric Keypad
                            val rows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("CLR", "0", "DEL")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rows.forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            KeypadButton(
                                                label = key,
                                                enabled = !isAuthenticating,
                                                onClick = {
                                                    onClearError()
                                                    when (key) {
                                                        "CLR" -> pinInput = ""
                                                        "DEL" -> if (pinInput.isNotEmpty()) {
                                                            pinInput = pinInput.dropLast(1)
                                                        }
                                                        else -> if (pinInput.length < 4) {
                                                            val updated = pinInput + key
                                                            pinInput = updated
                                                            if (updated.length == 4 && targetUser != null) {
                                                                onLogin(targetUser.id, updated, selectedRole)
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pin_key_$key")
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val user = targetUser ?: return@Button
                                    onLogin(user.id, pinInput, selectedRole)
                                },
                                enabled = !isAuthenticating && pinInput.length == 4 && targetUser != null,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button")
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Verifying with Firestore...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                } else {
                                    Text(
                                        text = "Verify & Sign In (${selectedRole.name})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
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
                    text = "CONCORD & A TO Z GAS • Built by Ragavan",
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
private fun RoleToggleButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            selected -> BrandGreenPrimary
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 0.dp else 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (selected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.outline
                    selected -> Color.White
                    else -> BrandGreenPrimary
                },
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        selected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        selected -> Color.White.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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

@Composable
private fun KeypadButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (label == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete digit",
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = if (label == "CLR") 13.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
