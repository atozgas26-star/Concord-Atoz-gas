package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserAccountEntity
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.ErrorDarkRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MetallicOrange
import com.example.ui.theme.MetallicOrangeBright
import com.example.ui.theme.RoyalElectricBlue
import kotlin.math.roundToInt

private data class IphoneKeySpec(
    val digit: String,
    val letters: String
)

private val IPHONE_KEY_ROWS = listOf(
    listOf(IphoneKeySpec("1", ""), IphoneKeySpec("2", "ABC"), IphoneKeySpec("3", "DEF")),
    listOf(IphoneKeySpec("4", "GHI"), IphoneKeySpec("5", "JKL"), IphoneKeySpec("6", "MNO")),
    listOf(IphoneKeySpec("7", "PQRS"), IphoneKeySpec("8", "TUV"), IphoneKeySpec("9", "WXYZ"))
)

private val CrimsonAccent = RoyalElectricBlue

/**
 * One-click staff selector card displayed on the Staff Login portal.
 * A single tap opens the full Staff Name Picker Bottom Sheet so staff can pick their name
 * and immediately enter their iPhone-style numeric passcode.
 */
@Composable
fun StaffOneClickSelectorCard(
    selectedStaff: UserAccountEntity?,
    totalStaffCount: Int,
    onOpenStaffList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = remember(selectedStaff?.displayName) {
        buildStaffInitials(selectedStaff?.displayName.orEmpty())
    }
    Surface(
        onClick = onOpenStaffList,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.25.dp, RoyalElectricBlue.copy(alpha = 0.45f)),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .luxuryTapScale()
            .testTag("staff_picker_open_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(RoyalElectricBlue, ElectricBlueBright, MetallicOrangeBright)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedStaff != null) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Choose Staff",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STAFF / VEHICLE NAME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = CrimsonAccent
                )
                Text(
                    text = selectedStaff?.displayName ?: "Tap to Choose Staff Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("selected_staff_name_label")
                )
                Text(
                    text = if (selectedStaff != null) {
                        "Tap to switch • $totalStaffCount staff available"
                    } else {
                        "One tap opens full staff list ($totalStaffCount)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ElectricBlueSoft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "List",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAccent
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Open Staff List",
                        tint = CrimsonAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean iOS-style Modal Bottom Sheet displaying all active staff members.
 * Tapping any staff row selects that staff member in one click and returns straight to the passcode pad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListPickerSheet(
    staffUsers: List<UserAccountEntity>,
    selectedStaffId: Long?,
    onSelectStaff: (UserAccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredStaff = remember(staffUsers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) staffUsers
        else staffUsers.filter {
            it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("staff_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Choose Staff Name",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "One tap selects staff & opens passcode login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("staff_picker_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Staff List"
                    )
                }
            }

            if (staffUsers.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search staff name...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Staff"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("staff_picker_search_input")
                )
            }

            if (filteredStaff.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No matching staff found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(filteredStaff, key = { it.id }) { staff ->
                        val isSelected = staff.id == selectedStaffId
                        val initials = buildStaffInitials(staff.displayName)
                        Surface(
                            onClick = { onSelectStaff(staff) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                ElectricBlueSoft.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CrimsonAccent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .luxuryTapScale()
                                .testTag("staff_list_item_${staff.username}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) CrimsonAccent
                                            else CrimsonAccent.copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else CrimsonAccent
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = staff.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Vehicle Staff • @${staff.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = CrimsonAccent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Login",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
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

/**
 * Authentic iPhone-style numeric passcode keypad with circular translucent buttons,
 * classic ABC/DEF letter subtitles, animated passcode dots with wrong-PIN shake feedback,
 * and optional biometric shortcut on the bottom-left key.
 */
@Composable
fun IphonePasscodePad(
    title: String,
    subtitle: String,
    pinInput: String,
    maxLength: Int = 4,
    minSubmitLength: Int = maxLength,
    autoSubmitAtMaxLength: Boolean = true,
    isAuthenticating: Boolean = false,
    errorMessage: String? = null,
    submitButtonLabel: String = "Sign In",
    submitEnabled: Boolean = true,
    showBiometricShortcut: Boolean = false,
    onBiometricShortcutClick: () -> Unit = {},
    onPinChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Classic iPhone horizontal shake animation when an incorrect passcode error appears
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            val offsets = listOf(-18f, 18f, -14f, 14f, -8f, 8f, 0f)
            offsets.forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 42))
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    LuxuryGlassCard(
        shape = RoundedCornerShape(26.dp),
        accentGlow = CrimsonAccent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("iphone_passcode_pad")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CrimsonAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                            MetallicOrange.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock header + target name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Passcode",
                    tint = CrimsonAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // Animated iPhone Passcode Dots
            val dotCount = if (maxLength <= 6) maxLength else maxOf(6, pinInput.length.coerceAtMost(maxLength))
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (dotCount > 6) 10.dp else 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .testTag("iphone_passcode_dots")
            ) {
                repeat(dotCount) { index ->
                    val filled = index < pinInput.length
                    val dotScale by animateFloatAsState(
                        targetValue = if (filled) 1.16f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dotScale"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            errorMessage != null && filled -> BrandRedBright
                            filled -> CrimsonAccent
                            else -> Color.Transparent
                        },
                        animationSpec = tween(150),
                        label = "dotColor"
                    )
                    val borderColor = when {
                        errorMessage != null -> BrandRedBright
                        filled -> CrimsonAccent
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(1.75.dp, borderColor, CircleShape)
                    )
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isDarkPad = LocalIsDarkTheme.current
                Surface(
                    color = if (isDarkPad) Color(0xFF3F121D) else BrandRedContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = if (isDarkPad) BrandRedBright else ErrorDarkRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Responsive circular iPhone numeric keypad
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val keySpacing = if (maxWidth < 300.dp) 14.dp else 20.dp
                val keySize = ((maxWidth - keySpacing * 2) / 3f).coerceIn(58.dp, 74.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IPHONE_KEY_ROWS.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(keySpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { spec ->
                                IphoneCircleDigitKey(
                                    digit = spec.digit,
                                    letters = spec.letters,
                                    size = keySize,
                                    enabled = !isAuthenticating,
                                    onClick = {
                                        if (pinInput.length < maxLength) {
                                            val updated = pinInput + spec.digit
                                            onPinChange(updated)
                                            if (autoSubmitAtMaxLength && updated.length == maxLength && submitEnabled) {
                                                onSubmit(updated)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("pin_key_${spec.digit}")
                                )
                            }
                        }
                    }

                    // Bottom row: Clear / Biometric | 0 (+) | Backspace
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(keySpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pinInput.isEmpty() && showBiometricShortcut) {
                            IphoneActionKey(
                                size = keySize,
                                enabled = !isAuthenticating,
                                onClick = onBiometricShortcutClick,
                                modifier = Modifier.testTag("pin_key_BIO")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Unlock with Fingerprint or Face",
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = "Face/Bio",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CrimsonAccent
                                    )
                                }
                            }
                        } else {
                            IphoneActionKey(
                                size = keySize,
                                enabled = !isAuthenticating && pinInput.isNotEmpty(),
                                onClick = { onPinChange("") },
                                modifier = Modifier.testTag("pin_key_CLR")
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pinInput.isNotEmpty()) {
                                        CrimsonAccent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        IphoneCircleDigitKey(
                            digit = "0",
                            letters = "+",
                            size = keySize,
                            enabled = !isAuthenticating,
                            onClick = {
                                if (pinInput.length < maxLength) {
                                    val updated = pinInput + "0"
                                    onPinChange(updated)
                                    if (autoSubmitAtMaxLength && updated.length == maxLength && submitEnabled) {
                                        onSubmit(updated)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("pin_key_0")
                        )

                        IphoneActionKey(
                            size = keySize,
                            enabled = !isAuthenticating && pinInput.isNotEmpty(),
                            onClick = {
                                if (pinInput.isNotEmpty()) {
                                    onPinChange(pinInput.dropLast(1))
                                }
                            },
                            modifier = Modifier.testTag("pin_key_DEL")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete digit",
                                tint = if (pinInput.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            Button(
                onClick = { onSubmit(pinInput) },
                enabled = !isAuthenticating && pinInput.length >= minSubmitLength && submitEnabled,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonAccent,
                    contentColor = Color.White
                ),
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
                        text = "Verifying Passcode...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = submitButtonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun IphoneCircleDigitKey(
    digit: String,
    letters: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) {
            CrimsonAccent.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        animationSpec = tween(100),
        label = "keyBg"
    )
    val borderColor = if (isPressed) {
        CrimsonAccent
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CrimsonAccent),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IphoneActionKey(
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun buildStaffInitials(name: String): String {
    val parts = name.trim().split(" ", "-", "_").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "ST"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
