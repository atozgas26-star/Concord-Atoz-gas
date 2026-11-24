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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UserAccountEntity
import com.example.data.model.UserRole
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
    onLogin: (userId: Long, pin: String) -> Unit,
    onClearError: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
    val adminUser = remember(users) { users.firstOrNull { it.role == UserRole.ADMIN } }
    val staffUsers = remember(users) { users.filter { it.role == UserRole.STAFF } }
    var selectedStaffId by remember(staffUsers) { mutableStateOf(staffUsers.firstOrNull()?.id) }
    var pinInput by remember { mutableStateOf("") }

    val targetUser = if (selectedRole == UserRole.ADMIN) {
        adminUser
    } else {
        staffUsers.firstOrNull { it.id == selectedStaffId } ?: staffUsers.firstOrNull()
    }

    LaunchedEffect(selectedRole, selectedStaffId) {
        pinInput = ""
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
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
                                text = "OFFLINE SECURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "CONCORD & A TO Z GAS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cylinder Store, Vehicle & Sales Management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
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
                // Role Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleToggleButton(
                        title = "Staff Login",
                        subtitle = "7 Vehicle Accounts",
                        icon = Icons.Default.LocalShipping,
                        selected = selectedRole == UserRole.STAFF,
                        onClick = { selectedRole = UserRole.STAFF },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_staff_tab")
                    )
                    RoleToggleButton(
                        title = "Admin Login",
                        subtitle = "Ragavan (Full Access)",
                        icon = Icons.Default.AdminPanelSettings,
                        selected = selectedRole == UserRole.ADMIN,
                        onClick = { selectedRole = UserRole.ADMIN },
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

                // PIN Pad Card
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
                                text = "Enter 4-Digit PIN for ${targetUser?.displayName ?: ""}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

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
                                                            onLogin(targetUser.id, updated)
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
                                onLogin(user.id, pinInput)
                            },
                            enabled = pinInput.length == 4 && targetUser != null,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button")
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (selectedRole == UserRole.ADMIN && adminUser?.pin == "1725") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Initial Admin PIN: 1725 (Change anytime in Settings)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleToggleButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BrandGreenPrimary else MaterialTheme.colorScheme.surface,
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
                tint = if (selected) Color.White else BrandGreenPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
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
