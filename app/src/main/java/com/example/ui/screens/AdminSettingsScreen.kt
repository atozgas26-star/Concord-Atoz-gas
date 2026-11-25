package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminCredentialsState
import com.example.data.local.ProductEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.AutoLogoutMode
import com.example.data.model.ProductCategory
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import com.example.data.remote.WorkspaceConnectionState
import com.example.export.DriveBackupState
import com.example.permissions.AppPermissionsStatus
import com.example.ui.components.AddStaffVehicleDialog
import com.example.ui.components.BrandHeaderBanner
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSettingsScreen(
    users: List<UserAccountEntity>,
    vehicles: List<VehicleEntity> = emptyList(),
    products: List<ProductEntity>,
    googleEmail: String = "",
    workspaceState: WorkspaceConnectionState = WorkspaceConnectionState.Checking,
    driveBackupState: DriveBackupState = DriveBackupState(),
    permissionsStatus: AppPermissionsStatus = AppPermissionsStatus(),
    pendingDriveRestoreJson: String? = null,
    themePreset: ThemePreset = ThemePreset.EMERALD_CORPORATE,
    darkModeEnabled: Boolean = false,
    soundEnabled: Boolean = true,
    autoLoginEnabled: Boolean = true,
    autoLogoutMode: AutoLogoutMode = AutoLogoutMode.OFF,
    adminCredentials: AdminCredentialsState = AdminCredentialsState(),
    ownerViewConfigured: Boolean = false,
    onThemePresetChanged: (ThemePreset) -> Unit = {},
    onDarkModeChanged: (Boolean) -> Unit = {},
    onSoundEnabledChanged: (Boolean) -> Unit = {},
    onAutoLoginChanged: (Boolean) -> Unit = {},
    onAutoLogoutModeChanged: (AutoLogoutMode) -> Unit = {},
    onUpdateAdminCredentials: (currentPassword: String, newUsername: String, newPassword: String, newRecoveryEmail: String) -> Unit = { _, _, _, _ -> },
    onUpdateOwnerViewPassword: (currentAdminPassword: String, newOwnerPassword: String, confirmOwnerPassword: String) -> Unit = { _, _, _ -> },
    onPreviewStartupSplash: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onGrantAllPermissionsAndDrive: () -> Unit = {},
    onToggleAutoDriveBackup: (Boolean) -> Unit = {},
    onBackUpToDriveNow: () -> Unit = {},
    onRestoreFromDriveNow: () -> Unit = {},
    onConfirmDriveRestore: () -> Unit = {},
    onDismissDriveRestore: () -> Unit = {},
    onRefreshPermissions: () -> Unit = {},
    onAddStaffAndVehicle: (staffName: String, vehicleName: String, pin: String) -> Unit = { _, _, _ -> },
    onUpdateStaffName: (userId: Long, newDisplayName: String, newVehicleName: String?) -> Unit = { _, _, _ -> },
    onUpdatePin: (userId: Long, newPin: String) -> Unit,
    onAddProduct: (ProductCategory, String) -> Unit,
    onToggleProductActive: (ProductEntity) -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onMessage: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableIntStateOf(0) }
    var editingPinUser by remember { mutableStateOf<UserAccountEntity?>(null) }
    var editingNameUser by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showEditAdminCredentialsDialog by remember { mutableStateOf(false) }
    var showEditOwnerViewPasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        onMessage(
            if (granted) "Notification permission granted! Connecting Google Drive..."
            else "Notifications optional. Connecting Google Drive backup..."
        )
        onGrantAllPermissionsAndDrive()
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    onMessage("Database backup exported successfully.")
                } catch (e: Exception) {
                    onMessage("Backup failed: ${e.message}")
                }
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    pendingRestoreJson = json
                }
            } catch (e: Exception) {
                onMessage("Could not read backup file: ${e.message}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandHeaderBanner(
            title = "Admin Settings",
            subtitle = "PIN Security, Product Master & Offline Backup"
        )

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("PINs & Backup") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Product Master (${products.size})") }
            )
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandGreenContainer),
                        border = BorderStroke(1.dp, BrandGreenPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = BrandGreenOnContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Online Cloud Sync (${vehicles.size} Vehicles)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrandGreenOnContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (googleEmail.isNotBlank()) {
                                Text(
                                    text = "Signed in as: $googleEmail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandGreenOnContainer
                                )
                            }
                            if (workspaceState is WorkspaceConnectionState.Connected) {
                                Text(
                                    text = "Staff Invite Code: ${workspaceState.inviteCode}  •  Connected devices: ${workspaceState.memberCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenOnContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onSyncNow,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_sync_now_button")
                                ) {
                                    Text("Sync All Now")
                                }
                                OutlinedButton(
                                    onClick = onGoogleSignOut,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_google_signout_button")
                                ) {
                                    Text("Sign Out Google")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AddToDrive, contentDescription = null, tint = BrandGreenPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "App Permissions & Auto Google Drive",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Play-compliant permissions & automatic Drive cloud backup",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        if (permissionsStatus.requiresNotificationPrompt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (permissionsStatus.notificationsGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (permissionsStatus.notificationsGranted) BrandGreenPrimary else BrandRedSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (permissionsStatus.notificationsGranted) "Notifications: OK" else "Notifications: Grant"
                                        )
                                    },
                                    modifier = Modifier.testTag("perm_chip_notifications")
                                )
                                AssistChip(
                                    onClick = onGrantAllPermissionsAndDrive,
                                    leadingIcon = {
                                        Icon(
                                            if (driveBackupState.driveAuthorized) Icons.Default.CloudDone else Icons.Default.AddToDrive,
                                            contentDescription = null,
                                            tint = if (driveBackupState.driveAuthorized) BrandGreenPrimary else BrandRedSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(if (driveBackupState.driveAuthorized) "Google Drive: Connected" else "Google Drive: Authorize")
                                    },
                                    modifier = Modifier.testTag("perm_chip_drive")
                                )
                                AssistChip(
                                    onClick = onRefreshPermissions,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (permissionsStatus.networkConnected) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(if (permissionsStatus.networkConnected) "Internet: Online" else "Internet: Offline")
                                    }
                                )
                                AssistChip(
                                    onClick = {},
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = BrandGreenPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Storage Picker: SAF Ready") }
                                )
                            }

                            Button(
                                onClick = {
                                    if (permissionsStatus.requiresNotificationPrompt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onGrantAllPermissionsAndDrive()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_all_permissions_drive_button")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (permissionsStatus.allCoreGranted)
                                        "All App Permissions & Auto Drive Active"
                                    else
                                        "Enable All Permissions & Auto Google Drive"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto Backup to Google Drive",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Automatically saves CONCORD_ATOZ_GAS_AUTO_BACKUP.json on changes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = driveBackupState.autoBackupEnabled,
                                    onCheckedChange = onToggleAutoDriveBackup,
                                    modifier = Modifier.testTag("auto_drive_backup_switch")
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = driveBackupState.lastStatusMessage,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (driveBackupState.lastBackupAtMs > 0L) {
                                        val timeLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                                            .format(Date(driveBackupState.lastBackupAtMs))
                                        Text(
                                            text = "Last Drive Backup: $timeLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onBackUpToDriveNow,
                                    enabled = !driveBackupState.isBackingUp && !driveBackupState.isRestoring,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("drive_backup_now_button")
                                ) {
                                    if (driveBackupState.isBackingUp) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Backup Drive")
                                    }
                                }
                                OutlinedButton(
                                    onClick = onRestoreFromDriveNow,
                                    enabled = !driveBackupState.isBackingUp && !driveBackupState.isRestoring,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("drive_restore_now_button")
                                ) {
                                    if (driveBackupState.isRestoring) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Restore Drive")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin & Staff Name / PIN (${users.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showAddStaffDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            modifier = Modifier.testTag("settings_add_staff_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Staff")
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreenPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Admin Username, Password & Gmail Recovery",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (adminCredentials.isConfigured) {
                                                "Username: ${adminCredentials.username}  •  Gmail: ${adminCredentials.recoveryEmail}"
                                            } else {
                                                "Not configured yet — set Admin username & password"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showEditAdminCredentialsDialog = true },
                                    modifier = Modifier.testTag("edit_admin_credentials_button")
                                ) {
                                    Text("Update")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = BrandGreenPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Owner View (Read-Only) Password",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (ownerViewConfigured) {
                                                "Active • Single view-only password (no entry or modification)"
                                            } else {
                                                "Not created yet — set one view-only password for Owner"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showEditOwnerViewPasswordDialog = true },
                                    modifier = Modifier.testTag("edit_owner_view_password_button")
                                ) {
                                    Text(if (ownerViewConfigured) "Change" else "Create")
                                }
                            }
                        }
                    }
                }

                items(users, key = { it.id }) { user ->
                    val isAdmin = user.role == UserRole.ADMIN
                    val linkedVehicle = if (isAdmin) null else vehicles.firstOrNull { it.staffUserId == user.id }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isAdmin) BrandRedSecondary else BrandGreenPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isAdmin) "${user.displayName} (ADMIN)" else user.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (linkedVehicle != null) {
                                        Text(
                                            text = "Vehicle: ${linkedVehicle.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandGreenPrimary
                                        )
                                    }
                                    Text(
                                        text = "Current PIN: ${user.pin}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = JetBrainsMono,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { editingNameUser = user },
                                    modifier = Modifier.testTag("edit_name_${user.username}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit name for ${user.displayName}",
                                        tint = BrandGreenPrimary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { editingPinUser = user },
                                    modifier = Modifier.testTag("change_pin_${user.username}")
                                ) {
                                    Text("Change PIN")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Offline Backup & Restore",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Save a complete offline copy of all staff accounts, products, opening stocks, and transactions, or restore from an earlier backup file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                        backupExportLauncher.launch("concord_gas_backup_$stamp.json")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("backup_export_button")
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Backup Data")
                                }
                                OutlinedButton(
                                    onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("backup_restore_button")
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore Data")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Appearance, Sound & Auto-Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Warehouse Dark Mode", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "High-contrast dark theme for dim storage & loading bays",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = darkModeEnabled,
                                    onCheckedChange = onDarkModeChanged,
                                    modifier = Modifier.testTag("settings_dark_mode_switch")
                                )
                            }

                            Text("App Color Theme", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemePreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = themePreset == preset,
                                        onClick = { onThemePresetChanged(preset) },
                                        label = { Text(preset.title) },
                                        modifier = Modifier.testTag("theme_chip_${preset.name}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("App Sounds & Startup Chime", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Play 3s launch chord & save feedback",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = onSoundEnabledChanged,
                                    modifier = Modifier.testTag("settings_sound_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("One-Time PIN Auto-Login", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Keep Admin/Staff signed in on reopen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = autoLoginEnabled,
                                    onCheckedChange = onAutoLoginChanged,
                                    modifier = Modifier.testTag("settings_autologin_switch")
                                )
                            }

                            Text("Auto-Logout Security Mode", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AutoLogoutMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = autoLogoutMode == mode,
                                        onClick = { onAutoLogoutModeChanged(mode) },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onPreviewStartupSplash,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Preview 3s Launch Screen & Sound")
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout of Admin Session")
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "C&A GAS • Built by Ragavan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Button(
                        onClick = { showAddProductDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_product_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Gas Product")
                    }
                }

                ProductCategory.entries.forEach { category ->
                    val catProducts = products.filter { it.category == category }
                    item(key = "header_${category.name}") {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(catProducts, key = { it.id }) { prod ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = if (prod.isActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Switch(
                                    checked = prod.isActive,
                                    onCheckedChange = { onToggleProductActive(prod) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingPinUser?.let { user ->
        ChangePinDialog(
            user = user,
            onDismiss = { editingPinUser = null },
            onConfirm = { newPin ->
                onUpdatePin(user.id, newPin)
                editingPinUser = null
            }
        )
    }

    editingNameUser?.let { user ->
        val linkedVehicle = if (user.role == UserRole.STAFF) {
            vehicles.firstOrNull { it.staffUserId == user.id }
        } else {
            null
        }
        EditStaffNameDialog(
            user = user,
            linkedVehicle = linkedVehicle,
            onDismiss = { editingNameUser = null },
            onConfirm = { newName, newVehicleName ->
                onUpdateStaffName(user.id, newName, newVehicleName)
                editingNameUser = null
            }
        )
    }

    if (showAddStaffDialog) {
        AddStaffVehicleDialog(
            nextVehicleNumber = vehicles.size + 1,
            onDismiss = { showAddStaffDialog = false },
            onConfirm = onAddStaffAndVehicle
        )
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { cat, name ->
                onAddProduct(cat, name)
                showAddProductDialog = false
            }
        )
    }

    if (showEditAdminCredentialsDialog) {
        EditAdminCredentialsDialog(
            adminCredentials = adminCredentials,
            fallbackEmail = googleEmail.ifBlank { "atozgas26@gmail.com" },
            onDismiss = { showEditAdminCredentialsDialog = false },
            onConfirm = { currentPwd, newUser, newPwd, recoveryGmail ->
                onUpdateAdminCredentials(currentPwd, newUser, newPwd, recoveryGmail)
                showEditAdminCredentialsDialog = false
            }
        )
    }

    if (showEditOwnerViewPasswordDialog) {
        EditOwnerViewPasswordDialog(
            adminCredentialsConfigured = adminCredentials.isConfigured,
            ownerViewConfigured = ownerViewConfigured,
            onDismiss = { showEditOwnerViewPasswordDialog = false },
            onConfirm = { currentAdminPwd, newOwnerPwd, confirmOwnerPwd ->
                onUpdateOwnerViewPassword(currentAdminPwd, newOwnerPwd, confirmOwnerPwd)
                showEditOwnerViewPasswordDialog = false
            }
        )
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Confirm Restore Backup") },
            text = { Text("Restoring this backup will replace all current store, vehicle, user PIN, and transaction data on this phone. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupJson(json)
                        pendingRestoreJson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary)
                ) { Text("Restore Now") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDriveRestoreJson != null) {
        AlertDialog(
            onDismissRequest = onDismissDriveRestore,
            title = { Text("Restore from Google Drive") },
            text = { Text("Downloaded your Google Drive cloud backup. Restoring will replace current store, vehicle, user PIN, and transaction records. Continue?") },
            confirmButton = {
                Button(
                    onClick = onConfirmDriveRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary)
                ) { Text("Restore Drive Backup") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDriveRestore) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditAdminCredentialsDialog(
    adminCredentials: AdminCredentialsState,
    fallbackEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newUsername: String, newPassword: String, newRecoveryEmail: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var username by remember(adminCredentials.username) {
        mutableStateOf(adminCredentials.username.ifBlank { "admin_ragavan" })
    }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var recoveryEmail by remember(adminCredentials.recoveryEmail, fallbackEmail) {
        mutableStateOf(adminCredentials.recoveryEmail.ifBlank { fallbackEmail })
    }
    var showPasswords by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BrandGreenPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (adminCredentials.isConfigured) {
                        "Update Admin Credentials"
                    } else {
                        "Set Up Admin Credentials"
                    }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Manage your Admin login username, password, and Gmail address used for 6-digit recovery verification codes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (adminCredentials.isConfigured) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            error = null
                        },
                        label = { Text("Current Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPasswords = !showPasswords }) {
                                Icon(
                                    imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPasswords) "Hide passwords" else "Show passwords"
                                )
                            }
                        },
                        visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_admin_current_password_input")
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        error = null
                    },
                    label = { Text("Admin Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_admin_username_input")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = null
                    },
                    label = {
                        Text(
                            if (adminCredentials.isConfigured) {
                                "New Password (optional)"
                            } else {
                                "New Password (min 6 chars)"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = if (!adminCredentials.isConfigured) {
                        {
                            IconButton(onClick = { showPasswords = !showPasswords }) {
                                Icon(
                                    imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPasswords) "Hide passwords" else "Show passwords"
                                )
                            }
                        }
                    } else {
                        null
                    },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_admin_new_password_input")
                )

                if (newPassword.isNotEmpty() || !adminCredentials.isConfigured) {
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = {
                            confirmNewPassword = it
                            error = null
                        },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_admin_confirm_password_input")
                    )
                }

                OutlinedTextField(
                    value = recoveryEmail,
                    onValueChange = {
                        recoveryEmail = it
                        error = null
                    },
                    label = { Text("Recovery Gmail Address") },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_admin_recovery_email_input")
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUser = username.trim()
                    val trimmedEmail = recoveryEmail.trim()
                    if (adminCredentials.isConfigured && currentPassword.isEmpty()) {
                        error = "Enter your current Admin password to confirm changes."
                        return@Button
                    }
                    if (trimmedUser.length < 3) {
                        error = "Username must be at least 3 characters."
                        return@Button
                    }
                    if (!adminCredentials.isConfigured && newPassword.length < 6) {
                        error = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (newPassword.isNotEmpty()) {
                        if (newPassword.length < 6) {
                            error = "New password must be at least 6 characters."
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            error = "New password and confirmation do not match."
                            return@Button
                        }
                    }
                    if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                        error = "Enter a valid Gmail address for recovery codes."
                        return@Button
                    }
                    onConfirm(currentPassword, trimmedUser, newPassword, trimmedEmail)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("settings_admin_credentials_save_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditOwnerViewPasswordDialog(
    adminCredentialsConfigured: Boolean,
    ownerViewConfigured: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (currentAdminPassword: String, newOwnerPassword: String, confirmOwnerPassword: String) -> Unit
) {
    var currentAdminPassword by remember { mutableStateOf("") }
    var newOwnerPassword by remember { mutableStateOf("") }
    var confirmOwnerPassword by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = BrandGreenPrimary)
                Spacer(Modifier.width(8.dp))
                Text(if (ownerViewConfigured) "Change Owner View Password" else "Create Owner View Password")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Owner View uses one read-only password. Anyone signed into Owner View can see live stock, vehicles, staff login notifications, and reports, with zero entry or modification access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (adminCredentialsConfigured) {
                    OutlinedTextField(
                        value = currentAdminPassword,
                        onValueChange = {
                            currentAdminPassword = it
                            error = null
                        },
                        label = { Text("Current Admin Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPasswords = !showPasswords }) {
                                Icon(
                                    imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPasswords) "Hide passwords" else "Show passwords"
                                )
                            }
                        },
                        visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_owner_admin_pwd_input")
                    )
                }

                OutlinedTextField(
                    value = newOwnerPassword,
                    onValueChange = {
                        newOwnerPassword = it
                        error = null
                    },
                    label = { Text("Owner View Password (min 6 chars)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = if (!adminCredentialsConfigured) {
                        {
                            IconButton(onClick = { showPasswords = !showPasswords }) {
                                Icon(
                                    imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPasswords) "Hide passwords" else "Show passwords"
                                )
                            }
                        }
                    } else null,
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_owner_new_pwd_input")
                )

                OutlinedTextField(
                    value = confirmOwnerPassword,
                    onValueChange = {
                        confirmOwnerPassword = it
                        error = null
                    },
                    label = { Text("Confirm Owner View Password") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_owner_confirm_pwd_input")
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (adminCredentialsConfigured && currentAdminPassword.isEmpty()) {
                        error = "Enter your current Admin password to confirm."
                        return@Button
                    }
                    if (newOwnerPassword.length < 6) {
                        error = "Owner View password must be at least 6 characters."
                        return@Button
                    }
                    if (newOwnerPassword != confirmOwnerPassword) {
                        error = "Owner View password and confirmation do not match."
                        return@Button
                    }
                    onConfirm(currentAdminPassword, newOwnerPassword, confirmOwnerPassword)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("settings_owner_pwd_save_button")
            ) {
                Text("Save Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChangePinDialog(
    user: UserAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN — ${user.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (user.role == UserRole.ADMIN) "Admin Account" else "Staff / Vehicle Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            pinText = input
                            error = null
                        }
                    },
                    label = { Text("New 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )
                if (error != null) {
                    Text(error!!, color = BrandRedSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length != 4) {
                        error = "PIN must be 4 digits."
                    } else {
                        onConfirm(pinText)
                    }
                },
                modifier = Modifier.testTag("save_pin_button")
            ) { Text("Save PIN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditStaffNameDialog(
    user: UserAccountEntity,
    linkedVehicle: VehicleEntity?,
    onDismiss: () -> Unit,
    onConfirm: (newDisplayName: String, newVehicleName: String?) -> Unit
) {
    var displayName by remember(user.id) { mutableStateOf(user.displayName) }
    var vehicleName by remember(user.id, linkedVehicle?.id) {
        mutableStateOf(linkedVehicle?.name ?: "")
    }
    var syncVehicleWithStaff by remember(user.id, linkedVehicle?.id) {
        mutableStateOf(
            linkedVehicle != null && linkedVehicle.name.equals(user.displayName, ignoreCase = true)
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (user.role == UserRole.ADMIN) "Edit Admin Name" else "Edit Staff Name"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { input ->
                        if (input.length <= 100) {
                            displayName = input
                            if (syncVehicleWithStaff && linkedVehicle != null) {
                                vehicleName = input.trim().uppercase().take(64)
                            }
                            error = null
                        }
                    },
                    label = { Text(if (user.role == UserRole.ADMIN) "Admin Name" else "Staff Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_staff_name_input")
                )

                if (linkedVehicle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = syncVehicleWithStaff,
                            onCheckedChange = { checked ->
                                syncVehicleWithStaff = checked
                                if (checked) {
                                    vehicleName = displayName.trim().uppercase().take(64)
                                }
                            }
                        )
                        Text(
                            text = "Also update Vehicle Name automatically",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = vehicleName,
                        onValueChange = { input ->
                            if (input.length <= 64) {
                                vehicleName = input
                                syncVehicleWithStaff = false
                                error = null
                            }
                        },
                        label = { Text("Assigned Vehicle Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_vehicle_name_input")
                    )
                }

                if (error != null) {
                    Text(error!!, color = BrandRedSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = displayName.trim()
                    val cleanVehicle = if (linkedVehicle != null) vehicleName.trim() else null
                    if (cleanName.isEmpty()) {
                        error = "Name cannot be empty."
                    } else if (linkedVehicle != null && cleanVehicle.isNullOrEmpty()) {
                        error = "Vehicle name cannot be empty."
                    } else {
                        onConfirm(cleanName, cleanVehicle)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("save_staff_name_button")
            ) {
                Text("Save Name")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProductCategory, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ProductCategory.INDUSTRIAL_GAS) }
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Gas Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ProductCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cylinder / Gas Name (e.g., Helium 40L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(selectedCategory, name) }
            ) { Text("Add Product") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
