package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.UaePhoneValidator
import com.example.data.local.AuthorizedMobileEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityAuditLogEntity
import com.example.data.local.UserAccountEntity
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.UserRole
import com.example.ui.theme.ArcticWhite
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBackground
import com.example.ui.theme.SignatureRoyalBorder
import com.example.ui.theme.SignatureRoyalElevated
import com.example.ui.theme.SignatureRoyalSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Corporate Security Center Dialog:
 * - Tab 0: UAE +971 Authorized Mobiles & Staff Feature Permissions (with Protected Owner lock)
 * - Tab 1: Registered Devices & Remote Revocation / Logout All Devices
 * - Tab 2: Live Security Alerts & Login Audit Logs
 * Strictly read-only when isReadOnly == true (Owner View).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityCenterDialog(
    authorizedMobiles: List<AuthorizedMobileEntity>,
    registeredDevices: List<RegisteredDeviceEntity>,
    securityLogs: List<SecurityAuditLogEntity>,
    staffUsers: List<UserAccountEntity>,
    isReadOnly: Boolean,
    onUpsertAuthorizedMobile: (
        phoneInput: String,
        displayName: String,
        role: UserRole,
        linkedStaffUserId: Long?,
        permissions: Set<StaffFeaturePermission>,
        isProtectedOwner: Boolean
    ) -> Unit,
    onUpsertAuthorizedMobileExtended: ((
        mobileId: Long?,
        phoneInput: String,
        displayName: String,
        role: UserRole,
        linkedStaffUserId: Long?,
        permissions: Set<StaffFeaturePermission>,
        isActive: Boolean,
        isProtectedOwner: Boolean,
        firstTimeRegEnabled: Boolean
    ) -> Unit)? = null,
    onSetStaffActiveStatus: ((userId: Long, isActive: Boolean) -> Unit)? = null,
    onUpdateStaffPin: ((userId: Long, newPin: String) -> Unit)? = null,
    onDeleteAuthorizedMobile: (AuthorizedMobileEntity) -> Unit,
    onRevokeDevice: (RegisteredDeviceEntity) -> Unit,
    onLogoutAllDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var showAddForm by remember { mutableStateOf(false) }
    var showLogoutAllConfirm by remember { mutableStateOf(false) }
    var resetPinTargetUser by remember { mutableStateOf<UserAccountEntity?>(null) }

    // Form state for adding/editing UAE +971 authorized mobile
    var editingMobileId by remember { mutableStateOf<Long?>(null) }
    var phoneInput by remember { mutableStateOf("+971") }
    var displayNameInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
    var selectedLinkedStaff by remember { mutableStateOf<UserAccountEntity?>(null) }
    var staffMenuExpanded by remember { mutableStateOf(false) }
    var selectedPermissions by remember { mutableStateOf(StaffFeaturePermission.entries.toSet()) }
    var mobileIsActive by remember { mutableStateOf(true) }
    var firstTimeRegEnabled by remember { mutableStateOf(true) }
    var formError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ArcticWhite,
        unfocusedTextColor = ArcticWhite,
        focusedContainerColor = SignatureRoyalElevated,
        unfocusedContainerColor = SignatureRoyalSurface,
        focusedBorderColor = ElectricBlueGlow,
        unfocusedBorderColor = SignatureRoyalBorder,
        focusedLabelColor = ElectricBlueGlow,
        unfocusedLabelColor = PlatinumMuted,
        cursorColor = MetallicOrangeBright
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SignatureRoyalBackground,
            border = BorderStroke(
                1.3.dp,
                Brush.linearGradient(listOf(ElectricBlueGlow.copy(alpha = 0.7f), MetallicOrangeBright.copy(alpha = 0.65f)))
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp)
                .testTag("security_center_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(RoyalBlueDeep, SignatureRoyalSurface)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MetallicOrange.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, MetallicOrangeBright),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MetallicOrangeBright, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isReadOnly) "Executive Security Monitor" else "Corporate Security Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticWhite
                                )
                                Text(
                                    text = if (isReadOnly) {
                                        "Read-Only • Devices, +971 Access & Alerts"
                                    } else {
                                        "UAE +971 Allowlist • Staff Permissions • Devices"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PlatinumSilver
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MetallicOrange,
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("security_center_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Security Center", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                SignatureAccentHairline(height = 2.dp)

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecurityTabButton(
                        title = "+971 Access (${authorizedMobiles.size})",
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        tag = "security_tab_mobiles",
                        modifier = Modifier.weight(1f)
                    )
                    SecurityTabButton(
                        title = "Devices (${registeredDevices.count { !it.isRevoked }})",
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        tag = "security_tab_devices",
                        modifier = Modifier.weight(1f)
                    )
                    val alertCount = securityLogs.count { it.isAlert }
                    SecurityTabButton(
                        title = if (alertCount > 0) "Alerts ($alertCount)" else "Logs (${securityLogs.size})",
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        tag = "security_tab_logs",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            if (!isReadOnly) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Authorized UAE +971 Numbers",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ArcticWhite
                                        )
                                        Button(
                                            onClick = {
                                                if (showAddForm) {
                                                    showAddForm = false
                                                    editingMobileId = null
                                                } else {
                                                    editingMobileId = null
                                                    phoneInput = "+971"
                                                    displayNameInput = ""
                                                    selectedRole = UserRole.STAFF
                                                    selectedLinkedStaff = null
                                                    selectedPermissions = StaffFeaturePermission.entries.toSet()
                                                    mobileIsActive = true
                                                    firstTimeRegEnabled = true
                                                    showAddForm = true
                                                }
                                                formError = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetallicOrange),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("security_toggle_add_mobile_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (showAddForm) "Hide Form" else "Authorize +971", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (showAddForm) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = SignatureRoyalSurface,
                                            border = BorderStroke(1.dp, ElectricBlueBright.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (editingMobileId != null) {
                                                        "Edit Authorized UAE Mobile (+971 5X XXX XXXX)"
                                                    } else {
                                                        "Register Authorized UAE Mobile (+971 5X XXX XXXX)"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElectricBlueGlow
                                                )
                                                OutlinedTextField(
                                                    value = phoneInput,
                                                    onValueChange = {
                                                        phoneInput = it.take(18)
                                                        formError = null
                                                    },
                                                    label = { Text("UAE Mobile Number (+9715XXXXXXXX)") },
                                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MetallicOrangeBright, modifier = Modifier.size(18.dp)) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    singleLine = true,
                                                    colors = fieldColors,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("security_mobile_phone_input")
                                                )
                                                OutlinedTextField(
                                                    value = displayNameInput,
                                                    onValueChange = {
                                                        displayNameInput = it
                                                        formError = null
                                                    },
                                                    label = { Text("Authorized Holder Name") },
                                                    singleLine = true,
                                                    colors = fieldColors,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("security_mobile_name_input")
                                                )

                                                // Role selector
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(UserRole.STAFF, UserRole.ADMIN, UserRole.OWNER_VIEW).forEach { role ->
                                                        val label = when (role) {
                                                            UserRole.STAFF -> "Staff"
                                                            UserRole.ADMIN -> "Admin"
                                                            UserRole.OWNER_VIEW -> "Owner (View)"
                                                        }
                                                        FilterChip(
                                                            selected = selectedRole == role,
                                                            onClick = { selectedRole = role },
                                                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = SignatureRoyalElevated,
                                                                labelColor = PlatinumSilver,
                                                                selectedContainerColor = RoyalElectricBlue,
                                                                selectedLabelColor = Color.White
                                                            )
                                                        )
                                                    }
                                                }

                                                // Link to Staff Account (when Staff role selected)
                                                if (selectedRole == UserRole.STAFF && staffUsers.isNotEmpty()) {
                                                    Box(modifier = Modifier.fillMaxWidth()) {
                                                        OutlinedButton(
                                                            onClick = { staffMenuExpanded = true },
                                                            border = BorderStroke(1.dp, SignatureRoyalBorder),
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = selectedLinkedStaff?.let { "Linked Staff: ${it.displayName} (@${it.username})" }
                                                                    ?: "Link to Staff Account (Optional)",
                                                                color = ArcticWhite,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                        DropdownMenu(
                                                            expanded = staffMenuExpanded,
                                                            onDismissRequest = { staffMenuExpanded = false }
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("No linked staff account") },
                                                                onClick = {
                                                                    selectedLinkedStaff = null
                                                                    staffMenuExpanded = false
                                                                }
                                                            )
                                                            staffUsers.forEach { st ->
                                                                DropdownMenuItem(
                                                                    text = { Text("${st.displayName} (@${st.username})") },
                                                                    onClick = {
                                                                        selectedLinkedStaff = st
                                                                        if (displayNameInput.isBlank()) {
                                                                            displayNameInput = st.displayName
                                                                        }
                                                                        staffMenuExpanded = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Text(
                                                        text = "Authorized Staff Features:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = PlatinumSilver,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        StaffFeaturePermission.entries.forEach { perm ->
                                                            val checked = perm in selectedPermissions
                                                            FilterChip(
                                                                selected = checked,
                                                                onClick = {
                                                                    selectedPermissions = if (checked) {
                                                                        selectedPermissions - perm
                                                                    } else {
                                                                        selectedPermissions + perm
                                                                    }
                                                                },
                                                                label = { Text(perm.label, fontSize = 10.sp) },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    containerColor = SignatureRoyalElevated,
                                                                    labelColor = PlatinumSilver,
                                                                    selectedContainerColor = MetallicOrange,
                                                                    selectedLabelColor = Color.White
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // First-Time Mobile Registration & Active Status toggles
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "First-Time OTP Registration",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ArcticWhite
                                                        )
                                                        Text(
                                                            text = "Allow UAE +971 SMS OTP + Existing PIN device registration",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = PlatinumMuted
                                                        )
                                                    }
                                                    Switch(
                                                        checked = firstTimeRegEnabled,
                                                        onCheckedChange = { firstTimeRegEnabled = it },
                                                        modifier = Modifier.testTag("security_first_time_reg_switch")
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (mobileIsActive) "Mobile Access: Active" else "Mobile Access: Inactive",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (mobileIsActive) BrandGreenLight else MetallicOrangeBright
                                                    )
                                                    Switch(
                                                        checked = mobileIsActive,
                                                        onCheckedChange = { mobileIsActive = it },
                                                        modifier = Modifier.testTag("security_mobile_active_switch")
                                                    )
                                                }

                                                if (formError != null) {
                                                    Text(
                                                        text = formError!!,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFFCA5A5)
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        val normalized = UaePhoneValidator.normalizeUaePhone(phoneInput)
                                                        if (normalized == null) {
                                                            formError = "Enter a valid UAE mobile number (+971 5X XXX XXXX)."
                                                        } else if (displayNameInput.trim().isEmpty()) {
                                                            formError = "Enter holder display name."
                                                        } else {
                                                            if (onUpsertAuthorizedMobileExtended != null) {
                                                                onUpsertAuthorizedMobileExtended(
                                                                    editingMobileId,
                                                                    normalized,
                                                                    displayNameInput.trim(),
                                                                    selectedRole,
                                                                    if (selectedRole == UserRole.STAFF) selectedLinkedStaff?.id else null,
                                                                    selectedPermissions,
                                                                    mobileIsActive,
                                                                    false,
                                                                    firstTimeRegEnabled
                                                                )
                                                            } else {
                                                                onUpsertAuthorizedMobile(
                                                                    normalized,
                                                                    displayNameInput.trim(),
                                                                    selectedRole,
                                                                    if (selectedRole == UserRole.STAFF) selectedLinkedStaff?.id else null,
                                                                    selectedPermissions,
                                                                    false
                                                                )
                                                            }
                                                            editingMobileId = null
                                                            phoneInput = "+971"
                                                            displayNameInput = ""
                                                            selectedLinkedStaff = null
                                                            mobileIsActive = true
                                                            firstTimeRegEnabled = true
                                                            showAddForm = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricBlue),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("security_save_mobile_btn")
                                                ) {
                                                    Text(
                                                        if (editingMobileId != null) "Update Authorized +971 Mobile" else "Save Authorized +971 Mobile",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (authorizedMobiles.isEmpty()) {
                                item {
                                    SecurityEmptyCard("No UAE +971 authorized mobile numbers registered yet.")
                                }
                            } else {
                                items(authorizedMobiles, key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SignatureRoyalSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (item.isProtectedOwner) MetallicOrangeGold else SignatureRoyalBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = item.displayName,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ArcticWhite
                                                        )
                                                        if (item.isProtectedOwner) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = "Protected Owner",
                                                                tint = MetallicOrangeGold,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = UaePhoneValidator.formatDisplay(item.phoneE164),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = JetBrainsMono,
                                                        color = ElectricBlueGlow
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = when (item.role) {
                                                            UserRole.OWNER_VIEW -> MetallicOrange.copy(alpha = 0.25f)
                                                            UserRole.ADMIN -> RoyalElectricBlue.copy(alpha = 0.35f)
                                                            UserRole.STAFF -> BrandGreenLight.copy(alpha = 0.2f)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (item.isProtectedOwner) "PROTECTED OWNER" else item.role.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = ArcticWhite,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    if (!isReadOnly && !item.isProtectedOwner) {
                                                        IconButton(
                                                            onClick = {
                                                                editingMobileId = item.id
                                                                phoneInput = item.phoneE164
                                                                displayNameInput = item.displayName
                                                                selectedRole = item.role
                                                                selectedLinkedStaff = staffUsers.firstOrNull { it.id == item.staffUserId }
                                                                selectedPermissions = StaffFeaturePermission.parseCsv(item.allowedFeaturesCsv)
                                                                mobileIsActive = item.isActive
                                                                firstTimeRegEnabled = item.firstTimeRegEnabled
                                                                showAddForm = true
                                                                formError = null
                                                            },
                                                            modifier = Modifier.testTag("security_edit_mobile_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit authorized number",
                                                                tint = ElectricBlueGlow,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { onDeleteAuthorizedMobile(item) },
                                                            modifier = Modifier.testTag("security_delete_mobile_${item.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Remove authorized number",
                                                                tint = BrandRedBright,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            // Registration Status, OTP Status, Device & Lockout badges
                                            val regStatusText = when {
                                                !item.isActive -> "DEACTIVATED"
                                                item.isRegistered -> "DEVICE REGISTERED"
                                                !item.firstTimeRegEnabled -> "FIRST-TIME REG OFF"
                                                else -> "PENDING FIRST-TIME REG"
                                            }
                                            val regColor = when {
                                                !item.isActive -> BrandRedBright
                                                item.isRegistered -> BrandGreenLight
                                                else -> MetallicOrangeBright
                                            }
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = regColor.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = regStatusText,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = regColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (item.otpVerifiedAtMs > 0L) {
                                                    val otpTime = SimpleDateFormat("dd MMM hh:mm a", Locale.US).format(Date(item.otpVerifiedAtMs))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = ElectricBlueBright.copy(alpha = 0.18f)
                                                    ) {
                                                        Text(
                                                            text = "OTP Verified: $otpTime",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = ElectricBlueGlow,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (item.failedLoginAttempts > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF45141E)
                                                    ) {
                                                        Text(
                                                            text = "Failed Attempts: ${item.failedLoginAttempts}",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFCA5A5),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (item.registeredDeviceId.isNotBlank()) {
                                                Text(
                                                    text = "Registered Device ID: ${item.registeredDeviceId}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMono,
                                                    color = PlatinumMuted
                                                )
                                            }
                                            if (item.role == UserRole.STAFF) {
                                                val perms = StaffFeaturePermission.parseCsv(item.allowedFeaturesCsv)
                                                Text(
                                                    text = "Features: ${perms.joinToString(" • ") { it.label }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PlatinumMuted
                                                )
                                                val linkedStaff = staffUsers.firstOrNull { it.id == item.staffUserId }
                                                if (!isReadOnly && linkedStaff != null && (onUpdateStaffPin != null || onSetStaffActiveStatus != null)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (onUpdateStaffPin != null) {
                                                            OutlinedButton(
                                                                onClick = { resetPinTargetUser = linkedStaff },
                                                                border = BorderStroke(1.dp, ElectricBlueGlow.copy(alpha = 0.7f)),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                                modifier = Modifier.testTag("security_reset_pin_${linkedStaff.id}")
                                                            ) {
                                                                Icon(Icons.Default.Key, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(14.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Reset PIN", color = ElectricBlueGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (onSetStaffActiveStatus != null) {
                                                            OutlinedButton(
                                                                onClick = { onSetStaffActiveStatus(linkedStaff.id, !linkedStaff.isActive) },
                                                                border = BorderStroke(1.dp, if (linkedStaff.isActive) MetallicOrangeBright else BrandGreenLight),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                                modifier = Modifier.testTag("security_toggle_staff_active_${linkedStaff.id}")
                                                            ) {
                                                                Text(
                                                                    if (linkedStaff.isActive) "Deactivate Account" else "Activate Account",
                                                                    color = if (linkedStaff.isActive) MetallicOrangeBright else BrandGreenLight,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
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
                        }

                        1 -> {
                            if (!isReadOnly && registeredDevices.any { !it.isRevoked }) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { showLogoutAllConfirm = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("security_logout_all_devices_btn")
                                        ) {
                                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Logout All Devices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (registeredDevices.isEmpty()) {
                                item { SecurityEmptyCard("No registered devices logged yet.") }
                            } else {
                                items(registeredDevices, key = { it.id }) { dev ->
                                    val timeStr = remember(dev.lastActiveMs) {
                                        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(dev.lastActiveMs))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SignatureRoyalSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (dev.isRevoked) Color(0xFFEF4444).copy(alpha = 0.6f) else SignatureRoyalBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Devices, contentDescription = null, tint = ElectricBlueGlow, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = dev.deviceLabel,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ArcticWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${dev.displayName} (${dev.role.name}) • $timeStr",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PlatinumSilver
                                                )
                                                if (dev.phoneE164.isNotBlank()) {
                                                    Text(
                                                        text = UaePhoneValidator.formatDisplay(dev.phoneE164),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontFamily = JetBrainsMono,
                                                        color = PlatinumMuted
                                                    )
                                                }
                                            }
                                            if (dev.isRevoked) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF45141E)
                                                ) {
                                                    Text(
                                                        text = "REVOKED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFCA5A5),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else if (!isReadOnly) {
                                                OutlinedButton(
                                                    onClick = { onRevokeDevice(dev) },
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("security_revoke_device_${dev.id}")
                                                ) {
                                                    Text("Revoke", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = BrandGreenLight.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandGreenLight,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            if (securityLogs.isEmpty()) {
                                item { SecurityEmptyCard("No security or login audit events recorded yet.") }
                            } else {
                                items(securityLogs, key = { it.id }) { log ->
                                    val timeStr = remember(log.timestampMs) {
                                        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(log.timestampMs))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (log.isAlert) Color(0xFF2B1226) else SignatureRoyalSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (log.isAlert) MetallicOrangeBright else SignatureRoyalBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (log.isAlert) Icons.Default.Warning else Icons.Default.VerifiedUser,
                                                        contentDescription = null,
                                                        tint = if (log.isAlert) MetallicOrangeBright else ElectricBlueGlow,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = log.eventType,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (log.isAlert) MetallicOrangeBright else ArcticWhite
                                                    )
                                                }
                                                Text(
                                                    text = timeStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMono,
                                                    color = PlatinumMuted
                                                )
                                            }
                                            Text(
                                                text = log.details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PlatinumSilver
                                            )
                                            val meta = listOfNotNull(
                                                log.actorName.takeIf { it.isNotBlank() },
                                                log.phoneE164.takeIf { it.isNotBlank() },
                                                log.deviceLabel.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")
                                            if (meta.isNotBlank()) {
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PlatinumMuted
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
        }
    }

    resetPinTargetUser?.let { staffTarget ->
        var newPin by remember(staffTarget.id) { mutableStateOf("") }
        var confirmPin by remember(staffTarget.id) { mutableStateOf("") }
        var pinErr by remember(staffTarget.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { resetPinTargetUser = null },
            containerColor = SignatureRoyalSurface,
            title = {
                Text(
                    text = "Reset Individual PIN • ${staffTarget.displayName}",
                    color = ArcticWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a new 4-digit individual PIN. Stored securely with salted PBKDF2 hashing (never plain text).",
                        style = MaterialTheme.typography.bodySmall,
                        color = PlatinumSilver
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { raw ->
                            newPin = raw.filter { it.isDigit() }.take(4)
                            pinErr = null
                        },
                        label = { Text("New 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { raw ->
                            confirmPin = raw.filter { it.isDigit() }.take(4)
                            pinErr = null
                        },
                        label = { Text("Confirm 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinErr != null) {
                        Text(pinErr!!, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFCA5A5))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPin.length != 4 -> pinErr = "PIN must be exactly 4 digits."
                            newPin != confirmPin -> pinErr = "PIN confirmation does not match."
                            else -> {
                                onUpdateStaffPin?.invoke(staffTarget.id, newPin)
                                resetPinTargetUser = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicOrange)
                ) {
                    Text("Save New PIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetPinTargetUser = null }) {
                    Text("Cancel", color = PlatinumSilver)
                }
            }
        )
    }

    if (showLogoutAllConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutAllConfirm = false },
            containerColor = SignatureRoyalSurface,
            title = { Text("Logout All Devices?", color = ArcticWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will mark all registered devices as revoked and require fresh login authentication across connected devices.",
                    color = PlatinumSilver
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutAllConfirm = false
                        onLogoutAllDevices()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Revoke All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllConfirm = false }) {
                    Text("Cancel", color = PlatinumSilver)
                }
            }
        )
    }
}

@Composable
private fun SecurityTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RoyalElectricBlue else SignatureRoyalSurface,
        border = BorderStroke(1.dp, if (selected) ElectricBlueGlow else SignatureRoyalBorder),
        modifier = modifier.testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else PlatinumSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SecurityEmptyCard(message: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SignatureRoyalSurface,
        border = BorderStroke(1.dp, SignatureRoyalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = PlatinumMuted)
        }
    }
}
