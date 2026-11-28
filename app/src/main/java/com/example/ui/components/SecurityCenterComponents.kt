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
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.DarkCharcoalImportant
import com.example.ui.theme.DarkSlateSecondary
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.ElectricBlueTextLight
import com.example.ui.theme.ErrorDarkRed
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.MetallicOrangeGold
import com.example.ui.theme.MetallicOrangeOnSoft
import com.example.ui.theme.MetallicOrangeSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianMidnight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.SignatureRoyalBackground
import com.example.ui.theme.SignatureRoyalBorder
import com.example.ui.theme.SignatureRoyalElevated
import com.example.ui.theme.SuccessDarkGreen
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningDarkOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Corporate Security Center Dialog:
 * - Tab 0: UAE +971 Authorized Mobiles & Staff Feature Permissions (with Protected Owner lock)
 * - Tab 1: Registered Devices & Remote Revocation / Logout All Devices
 * - Tab 2: Live Security Alerts & Login Audit Logs (Category order history, FAILED_PIN_LOGIN, etc.)
 * Strictly read-only when isReadOnly == true (Owner View).
 *
 * High-contrast WCAG AA typography & color palette:
 * - Light background: Deep Navy (#14233B) primary, Dark Charcoal (#202938) important,
 *   Dark Slate (#475569) secondary, Bright White (#FFFFFF) cards, Dark Green/Orange/Red semantics.
 * - Dark background: Pure White (#FFFFFF) primary, Light Silver (#E2E8F0) secondary.
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
    val isDark = LocalIsDarkTheme.current

    // High-contrast semantic palette (zero reduced-opacity text)
    val dialogBg = if (isDark) ObsidianMidnight else SignatureRoyalBackground
    val cardBg = if (isDark) ObsidianSurface else PureWhite
    val elevatedBg = if (isDark) ObsidianElevated else SignatureRoyalElevated
    val cardBorder = if (isDark) ObsidianBorder else SignatureRoyalBorder
    val primaryText = if (isDark) PureWhite else DeepNavyText
    val importantText = if (isDark) PureWhite else DarkCharcoalImportant
    val secondaryText = if (isDark) TextSecondaryDark else DarkSlateSecondary
    val blueAccentText = if (isDark) ElectricBlueGlow else ElectricBlueTextLight
    val successText = if (isDark) BrandGreenLight else SuccessDarkGreen
    val warningText = if (isDark) MetallicOrangeBright else WarningDarkOrange
    val errorText = if (isDark) Color(0xFFFCA5A5) else ErrorDarkRed
    val errorBg = if (isDark) Color(0xFF45141E) else BrandRedContainer
    val alertCardBg = if (isDark) Color(0xFF2B1226) else Color(0xFFFFF5F5)
    val alertCardBorder = if (isDark) MetallicOrangeBright else ErrorDarkRed

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
        focusedTextColor = primaryText,
        unfocusedTextColor = primaryText,
        focusedContainerColor = cardBg,
        unfocusedContainerColor = cardBg,
        focusedBorderColor = if (isDark) ElectricBlueGlow else RoyalElectricBlue,
        unfocusedBorderColor = cardBorder,
        focusedLabelColor = blueAccentText,
        unfocusedLabelColor = secondaryText,
        cursorColor = MetallicOrange
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = BorderStroke(
                1.4.dp,
                Brush.linearGradient(
                    listOf(
                        RoyalElectricBlue,
                        MetallicOrange
                    )
                )
            ),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp)
                .testTag("security_center_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // High-contrast Electric Blue Header with Pure White bold typography
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                if (isDark) listOf(ObsidianDeep, RoyalBlueDeep)
                                else listOf(RoyalBlueDeep, RoyalElectricBlue)
                            )
                        )
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
                                color = MetallicOrange,
                                border = BorderStroke(1.2.dp, PureWhite),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isReadOnly) "Executive Security Monitor" else "Corporate Security Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PureWhite,
                                    modifier = Modifier.testTag("security_center_heading")
                                )
                                Text(
                                    text = if (isReadOnly) {
                                        "Read-Only • Devices, +971 Access & Alerts"
                                    } else {
                                        "UAE +971 Allowlist • Staff Permissions • Devices"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MetallicOrange,
                            border = BorderStroke(1.dp, PureWhite),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("security_center_close_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close Security Center",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
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
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    SecurityTabButton(
                        title = "Devices (${registeredDevices.count { !it.isRevoked }})",
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        tag = "security_tab_devices",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    val alertCount = securityLogs.count { it.isAlert }
                    SecurityTabButton(
                        title = if (alertCount > 0) "Alerts ($alertCount)" else "Logs (${securityLogs.size})",
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        tag = "security_tab_logs",
                        isDark = isDark,
                        isAlertTab = alertCount > 0,
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
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
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MetallicOrange,
                                                contentColor = PureWhite
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("security_toggle_add_mobile_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (showAddForm) "Hide Form" else "Authorize +971",
                                                color = PureWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (showAddForm) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = cardBg,
                                            border = BorderStroke(1.2.dp, if (isDark) ElectricBlueBright else RoyalElectricBlue),
                                            shadowElevation = 3.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = if (editingMobileId != null) {
                                                        "Edit Authorized UAE Record (+971 5X XXX XXXX)"
                                                    } else {
                                                        "Register Authorized UAE Record (+971 5X XXX XXXX)"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = blueAccentText
                                                )
                                                OutlinedTextField(
                                                    value = phoneInput,
                                                    onValueChange = {
                                                        phoneInput = it.take(18)
                                                        formError = null
                                                    },
                                                    label = { Text("UAE Mobile Number (+9715XXXXXXXX)") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Phone,
                                                            contentDescription = null,
                                                            tint = warningText,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
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
                                                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                containerColor = elevatedBg,
                                                                labelColor = primaryText,
                                                                selectedContainerColor = RoyalElectricBlue,
                                                                selectedLabelColor = PureWhite
                                                            )
                                                        )
                                                    }
                                                }

                                                // Link to Staff Account (when Staff role selected)
                                                if (selectedRole == UserRole.STAFF && staffUsers.isNotEmpty()) {
                                                    Box(modifier = Modifier.fillMaxWidth()) {
                                                        OutlinedButton(
                                                            onClick = { staffMenuExpanded = true },
                                                            border = BorderStroke(1.dp, cardBorder),
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = selectedLinkedStaff?.let { "Linked Staff: ${it.displayName} (@${it.username})" }
                                                                    ?: "Link to Staff Account (Optional)",
                                                                color = primaryText,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                        DropdownMenu(
                                                            expanded = staffMenuExpanded,
                                                            onDismissRequest = { staffMenuExpanded = false }
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("No linked staff account", color = primaryText) },
                                                                onClick = {
                                                                    selectedLinkedStaff = null
                                                                    staffMenuExpanded = false
                                                                }
                                                            )
                                                            staffUsers.forEach { st ->
                                                                DropdownMenuItem(
                                                                    text = { Text("${st.displayName} (@${st.username})", color = primaryText) },
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
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = importantText,
                                                        fontWeight = FontWeight.Bold
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
                                                                label = { Text(perm.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    containerColor = elevatedBg,
                                                                    labelColor = primaryText,
                                                                    selectedContainerColor = MetallicOrange,
                                                                    selectedLabelColor = PureWhite
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // Device Binding & Active Status toggles
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Allow New Device Activation",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = primaryText
                                                        )
                                                        Text(
                                                            text = "Permit authorized PIN sign-in from a newly registered corporate device",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = secondaryText
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
                                                        text = if (mobileIsActive) "Record Access: Active" else "Record Access: Inactive",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (mobileIsActive) successText else warningText
                                                    )
                                                    Switch(
                                                        checked = mobileIsActive,
                                                        onCheckedChange = { mobileIsActive = it },
                                                        modifier = Modifier.testTag("security_mobile_active_switch")
                                                    )
                                                }

                                                if (formError != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = errorBg,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = formError!!,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = errorText,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
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
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = RoyalElectricBlue,
                                                        contentColor = PureWhite
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("security_save_mobile_btn")
                                                ) {
                                                    Text(
                                                        if (editingMobileId != null) "Update Authorized +971 Record" else "Save Authorized +971 Record",
                                                        color = PureWhite,
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
                                    SecurityEmptyCard(
                                        message = "No UAE +971 authorized mobile numbers registered yet.",
                                        isDark = isDark
                                    )
                                }
                            } else {
                                items(authorizedMobiles, key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = cardBg,
                                        border = BorderStroke(
                                            1.2.dp,
                                            if (item.isProtectedOwner) MetallicOrange else cardBorder
                                        ),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
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
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = item.displayName,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = primaryText
                                                        )
                                                        if (item.isProtectedOwner) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Lock,
                                                                contentDescription = "Protected Owner",
                                                                tint = warningText,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = UaePhoneValidator.formatDisplay(item.phoneE164),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontFamily = JetBrainsMono,
                                                        fontWeight = FontWeight.Bold,
                                                        color = blueAccentText
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val roleBg = when (item.role) {
                                                        UserRole.OWNER_VIEW -> if (isDark) MetallicOrange.copy(alpha = 0.28f) else MetallicOrangeSoft
                                                        UserRole.ADMIN -> if (isDark) RoyalElectricBlue.copy(alpha = 0.35f) else ElectricBlueSoft
                                                        UserRole.STAFF -> if (isDark) BrandGreenLight.copy(alpha = 0.24f) else BrandGreenContainer
                                                    }
                                                    val roleFg = when (item.role) {
                                                        UserRole.OWNER_VIEW -> if (isDark) PureWhite else MetallicOrangeOnSoft
                                                        UserRole.ADMIN -> if (isDark) PureWhite else ElectricBlueTextLight
                                                        UserRole.STAFF -> if (isDark) PureWhite else BrandGreenOnContainer
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = roleBg
                                                    ) {
                                                        Text(
                                                            text = if (item.isProtectedOwner) "PROTECTED OWNER" else item.role.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = roleFg,
                                                            fontSize = 11.sp,
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
                                                                tint = blueAccentText,
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
                                                                tint = errorText,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Status Chips (High Contrast)
                                            val regStatusText = when {
                                                !item.isActive -> "INACTIVE"
                                                item.isRegistered -> "ACTIVE • REGISTERED"
                                                item.firstTimeRegEnabled -> "ACTIVE • READY FOR PIN LOGIN"
                                                else -> "ACTIVE"
                                            }
                                            val regBg = when {
                                                !item.isActive -> errorBg
                                                item.isRegistered -> if (isDark) BrandGreenLight.copy(alpha = 0.22f) else BrandGreenContainer
                                                else -> if (isDark) MetallicOrange.copy(alpha = 0.22f) else MetallicOrangeSoft
                                            }
                                            val regFg = when {
                                                !item.isActive -> errorText
                                                item.isRegistered -> successText
                                                else -> warningText
                                            }
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = regBg
                                                ) {
                                                    Text(
                                                        text = regStatusText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = regFg,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                if (item.otpVerifiedAtMs > 0L) {
                                                    val verifiedTime = SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(item.otpVerifiedAtMs))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isDark) ElectricBlueBright.copy(alpha = 0.22f) else ElectricBlueSoft
                                                    ) {
                                                        Text(
                                                            text = "Verified: $verifiedTime",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = blueAccentText,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                if (item.failedLoginAttempts > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = errorBg
                                                    ) {
                                                        Text(
                                                            text = "Failed Attempts: ${item.failedLoginAttempts}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = errorText,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (item.registeredDeviceId.isNotBlank()) {
                                                Text(
                                                    text = "Registered Device ID: ${item.registeredDeviceId}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Medium,
                                                    color = secondaryText
                                                )
                                            }
                                            if (item.role == UserRole.STAFF) {
                                                val perms = StaffFeaturePermission.parseCsv(item.allowedFeaturesCsv)
                                                Text(
                                                    text = "Staff Permissions: ${perms.joinToString(" • ") { it.label }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = importantText
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
                                                                border = BorderStroke(1.2.dp, blueAccentText),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                                modifier = Modifier.testTag("security_reset_pin_${linkedStaff.id}")
                                                            ) {
                                                                Icon(Icons.Default.Key, contentDescription = null, tint = blueAccentText, modifier = Modifier.size(15.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Reset PIN", color = blueAccentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        if (onSetStaffActiveStatus != null) {
                                                            val toggleTint = if (linkedStaff.isActive) warningText else successText
                                                            OutlinedButton(
                                                                onClick = { onSetStaffActiveStatus(linkedStaff.id, !linkedStaff.isActive) },
                                                                border = BorderStroke(1.2.dp, toggleTint),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                                modifier = Modifier.testTag("security_toggle_staff_active_${linkedStaff.id}")
                                                            ) {
                                                                Text(
                                                                    if (linkedStaff.isActive) "Deactivate Account" else "Activate Account",
                                                                    color = toggleTint,
                                                                    fontSize = 12.sp,
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
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ErrorDarkRed,
                                                contentColor = PureWhite
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("security_logout_all_devices_btn")
                                        ) {
                                            Icon(Icons.Default.Logout, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Logout All Devices", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (registeredDevices.isEmpty()) {
                                item {
                                    SecurityEmptyCard(
                                        message = "No registered devices logged yet.",
                                        isDark = isDark
                                    )
                                }
                            } else {
                                items(registeredDevices, key = { it.id }) { dev ->
                                    val timeStr = remember(dev.lastActiveMs) {
                                        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(dev.lastActiveMs))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = cardBg,
                                        border = BorderStroke(
                                            1.2.dp,
                                            if (dev.isRevoked) errorText else cardBorder
                                        ),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Devices,
                                                        contentDescription = null,
                                                        tint = blueAccentText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = dev.deviceLabel,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${dev.displayName} (${dev.role.name}) • $timeStr",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = importantText
                                                )
                                                if (dev.phoneE164.isNotBlank()) {
                                                    Text(
                                                        text = UaePhoneValidator.formatDisplay(dev.phoneE164),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = JetBrainsMono,
                                                        fontWeight = FontWeight.Medium,
                                                        color = secondaryText
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            if (dev.isRevoked) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = errorBg
                                                ) {
                                                    Text(
                                                        text = "REVOKED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = errorText,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                    )
                                                }
                                            } else if (!isReadOnly) {
                                                OutlinedButton(
                                                    onClick = { onRevokeDevice(dev) },
                                                    border = BorderStroke(1.2.dp, errorText),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("security_revoke_device_${dev.id}")
                                                ) {
                                                    Text("Revoke", color = errorText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) BrandGreenLight.copy(alpha = 0.22f) else BrandGreenContainer
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = successText,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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
                                item {
                                    SecurityEmptyCard(
                                        message = "No security or login audit events recorded yet.",
                                        isDark = isDark
                                    )
                                }
                            } else {
                                items(securityLogs, key = { it.id }) { log ->
                                    val timeStr = remember(log.timestampMs) {
                                        SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(Date(log.timestampMs))
                                    }
                                    val isCriticalFail = log.isAlert ||
                                        log.eventType.contains("FAILED", ignoreCase = true) ||
                                        log.eventType.contains("LOCKOUT", ignoreCase = true) ||
                                        log.eventType.contains("DENIED", ignoreCase = true)
                                    val titleColor = when {
                                        isCriticalFail -> errorText
                                        log.eventType.contains("REORDER", ignoreCase = true) ||
                                            log.eventType.contains("UPDATE", ignoreCase = true) -> blueAccentText
                                        log.eventType.contains("SUCCESS", ignoreCase = true) ||
                                            log.eventType.contains("LOGIN", ignoreCase = true) -> successText
                                        else -> primaryText
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCriticalFail) alertCardBg else cardBg,
                                        border = BorderStroke(
                                            if (isCriticalFail) 1.4.dp else 1.1.dp,
                                            if (isCriticalFail) alertCardBorder else cardBorder
                                        ),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("security_log_card_${log.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                                    Icon(
                                                        imageVector = if (isCriticalFail) Icons.Default.Warning else Icons.Default.VerifiedUser,
                                                        contentDescription = null,
                                                        tint = titleColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = log.eventType,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = titleColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = timeStr,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Medium,
                                                    color = secondaryText
                                                )
                                            }
                                            Text(
                                                text = log.details,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = importantText
                                            )
                                            val meta = listOfNotNull(
                                                log.actorName.takeIf { it.isNotBlank() },
                                                log.phoneE164.takeIf { it.isNotBlank() }?.let { UaePhoneValidator.formatDisplay(it) },
                                                log.deviceLabel.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")
                                            if (meta.isNotBlank()) {
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = secondaryText
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
            containerColor = cardBg,
            title = {
                Text(
                    text = "Reset Individual PIN • ${staffTarget.displayName}",
                    color = primaryText,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a new 4-digit individual PIN. Stored securely with salted PBKDF2 hashing (never plain text).",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
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
                        Text(
                            text = pinErr!!,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = errorText
                        )
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
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicOrange, contentColor = PureWhite)
                ) {
                    Text("Save New PIN", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetPinTargetUser = null }) {
                    Text("Cancel", color = primaryText, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLogoutAllConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutAllConfirm = false },
            containerColor = cardBg,
            title = { Text("Logout All Devices?", color = primaryText, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "This will mark all registered devices as revoked and require fresh PIN login authentication across connected devices.",
                    color = importantText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutAllConfirm = false
                        onLogoutAllDevices()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorDarkRed, contentColor = PureWhite)
                ) {
                    Text("Revoke All", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllConfirm = false }) {
                    Text("Cancel", color = primaryText, fontWeight = FontWeight.Bold)
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
    isDark: Boolean,
    isAlertTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    val container = when {
        selected && isAlertTab -> ErrorDarkRed
        selected -> RoyalElectricBlue
        isDark -> ObsidianSurface
        else -> PureWhite
    }
    val border = when {
        selected && isAlertTab -> ErrorDarkRed
        selected -> RoyalElectricBlue
        isAlertTab -> if (isDark) MetallicOrangeBright else ErrorDarkRed
        isDark -> ObsidianBorder
        else -> SignatureRoyalBorder
    }
    val labelColor = when {
        selected -> PureWhite
        isAlertTab -> if (isDark) Color(0xFFFCA5A5) else ErrorDarkRed
        isDark -> PureWhite
        else -> DeepNavyText
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.2.dp, border),
        shadowElevation = if (selected) 3.dp else 1.dp,
        modifier = modifier.testTag(tag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SecurityEmptyCard(message: String, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) ObsidianSurface else PureWhite,
        border = BorderStroke(1.1.dp, if (isDark) ObsidianBorder else SignatureRoyalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDark) TextSecondaryDark else DarkSlateSecondary
            )
        }
    }
}
