package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole
import com.example.data.remote.WorkspaceConnectionState
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

@Composable
fun AdminSettingsScreen(
    users: List<UserAccountEntity>,
    vehicles: List<VehicleEntity> = emptyList(),
    products: List<ProductEntity>,
    googleEmail: String = "",
    workspaceState: WorkspaceConnectionState = WorkspaceConnectionState.Checking,
    onSyncNow: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
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
    var showAddProductDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

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
                                    text = "Online Cloud Sync (7 Staff)",
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
                                    text = "7-Staff Invite Code: ${workspaceState.inviteCode}  •  Connected devices: ${workspaceState.memberCount}",
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
                    Text(
                        text = "Admin & Staff Name / PIN Management",
                        style = MaterialTheme.typography.titleMedium
                    )
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

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { cat, name ->
                onAddProduct(cat, name)
                showAddProductDialog = false
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
