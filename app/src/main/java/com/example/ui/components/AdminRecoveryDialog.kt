package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AdminRecoveryUiState
import com.example.auth.RecoveryPurpose
import com.example.auth.RecoveryStep
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.JetBrainsMono

/**
 * Interactive Gmail Verification Code dialog for Admin Forgot Password
 * and Forgot Username recovery flows.
 */
@Composable
fun AdminRecoveryDialog(
    state: AdminRecoveryUiState,
    onSendCode: (email: String) -> Unit,
    onVerifyPasswordReset: (code: String, newPassword: String, confirmPassword: String) -> Unit,
    onVerifyUsernameRecovery: (code: String) -> Unit,
    onUpdateRecoveredUsername: (newUsername: String) -> Unit,
    onUseRecoveredUsername: (username: String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit
) {
    val purpose = state.activePurpose ?: return
    var emailInput by remember(purpose, state.targetEmail) { mutableStateOf(state.targetEmail) }
    var codeInput by remember(purpose, state.step) { mutableStateOf("") }
    var newPassword by remember(purpose) { mutableStateOf("") }
    var confirmPassword by remember(purpose) { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var updatedUsernameInput by remember(state.revealedUsername) {
        mutableStateOf(state.revealedUsername.orEmpty())
    }

    LaunchedEffect(state.targetEmail) {
        if (emailInput.isBlank() && state.targetEmail.isNotBlank()) {
            emailInput = state.targetEmail
        }
    }

    AlertDialog(
        onDismissRequest = { if (!state.isSending && !state.isVerifying) onDismiss() },
        icon = {
            Icon(
                imageVector = when {
                    state.step == RecoveryStep.USERNAME_REVEALED -> Icons.Default.CheckCircle
                    state.step == RecoveryStep.VERIFY_CODE -> Icons.Default.MarkEmailRead
                    purpose == RecoveryPurpose.FORGOT_PASSWORD -> Icons.Default.LockReset
                    else -> Icons.Default.Person
                },
                contentDescription = purpose.title,
                tint = BrandGreenPrimary,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = when (state.step) {
                    RecoveryStep.REQUEST_CODE -> purpose.title
                    RecoveryStep.VERIFY_CODE -> "Enter Gmail Verification Code"
                    RecoveryStep.USERNAME_REVEALED -> "Admin Username Recovered"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state.step) {
                    RecoveryStep.REQUEST_CODE -> {
                        Text(
                            text = if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                                "Enter your registered Admin Gmail address. We will send a 6-digit verification code to your Gmail inbox to reset your password."
                            } else {
                                "Enter your registered Admin Gmail address. We will send a 6-digit verification code to your Gmail inbox to recover your Admin username."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                onClearError()
                            },
                            label = { Text("Registered Admin Gmail") },
                            placeholder = { Text("atozgas26@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null)
                            },
                            singleLine = true,
                            enabled = !state.isSending,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_email_input")
                        )
                    }

                    RecoveryStep.VERIFY_CODE -> {
                        Surface(
                            color = BrandGreenContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = state.statusMessage ?: "6-digit verification code sent to ${state.maskedEmail}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandGreenOnContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Check your Gmail inbox (and Spam folder). Code expires in 10 minutes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandGreenOnContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { raw ->
                                codeInput = raw.filter { it.isDigit() }.take(6)
                                onClearError()
                            },
                            label = { Text("6-Digit Gmail Verification Code") },
                            placeholder = { Text("000000") },
                            leadingIcon = {
                                Icon(Icons.Default.Pin, contentDescription = null)
                            },
                            singleLine = true,
                            enabled = !state.isVerifying,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_code_input")
                        )

                        if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    onClearError()
                                },
                                label = { Text("New Admin Password (min 6 chars)") },
                                singleLine = true,
                                enabled = !state.isVerifying,
                                visualTransformation = if (showNewPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                        Icon(
                                            imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_new_password_input")
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    onClearError()
                                },
                                label = { Text("Confirm New Password") },
                                singleLine = true,
                                enabled = !state.isVerifying,
                                visualTransformation = if (showNewPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_confirm_password_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onSendCode(state.targetEmail.ifBlank { emailInput }) },
                                enabled = !state.isSending && !state.isVerifying,
                                modifier = Modifier.testTag("recovery_resend_code_button")
                            ) {
                                Text(if (state.isSending) "Sending..." else "Resend Gmail Code")
                            }
                        }
                    }

                    RecoveryStep.USERNAME_REVEALED -> {
                        val recovered = state.revealedUsername.orEmpty()
                        Surface(
                            color = BrandGreenContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Verified via Gmail • Your Admin Username",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandGreenOnContainer
                                )
                                Text(
                                    text = recovered,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreenOnContainer,
                                    modifier = Modifier.testTag("recovered_username_text")
                                )
                            }
                        }

                        Text(
                            text = "Optional: Change your Admin username below, or tap Use Username to sign in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = updatedUsernameInput,
                            onValueChange = {
                                updatedUsernameInput = it.filter { ch ->
                                    ch.isLetterOrDigit() || ch == '_' || ch == '.' || ch == '-'
                                }.take(40)
                                onClearError()
                            },
                            label = { Text("Change Admin Username (Optional)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_new_username_input")
                        )

                        if (updatedUsernameInput.trim().isNotEmpty() &&
                            updatedUsernameInput.trim() != recovered
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateRecoveredUsername(updatedUsernameInput.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_recovered_username_button")
                            ) {
                                Text("Save New Admin Username")
                            }
                        }
                    }
                }

                if (state.errorMessage != null) {
                    Surface(
                        color = BrandRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = BrandRedBright,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (state.step) {
                RecoveryStep.REQUEST_CODE -> {
                    Button(
                        onClick = { onSendCode(emailInput) },
                        enabled = !state.isSending && emailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier.testTag(
                            if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                                "send_password_reset_code_button"
                            } else {
                                "send_username_recovery_code_button"
                            }
                        )
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sending...")
                        } else {
                            Text("Send Gmail Code")
                        }
                    }
                }

                RecoveryStep.VERIFY_CODE -> {
                    Button(
                        onClick = {
                            if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                                onVerifyPasswordReset(codeInput, newPassword, confirmPassword)
                            } else {
                                onVerifyUsernameRecovery(codeInput)
                            }
                        },
                        enabled = !state.isVerifying && codeInput.length == 6 &&
                            (purpose == RecoveryPurpose.FORGOT_USERNAME ||
                                (newPassword.length >= 6 && confirmPassword.isNotEmpty())),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier.testTag(
                            if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                                "confirm_password_reset_button"
                            } else {
                                "confirm_username_recovery_button"
                            }
                        )
                    ) {
                        if (state.isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Verifying...")
                        } else {
                            Text(
                                if (purpose == RecoveryPurpose.FORGOT_PASSWORD) {
                                    "Reset Password"
                                } else {
                                    "Verify & Reveal"
                                }
                            )
                        }
                    }
                }

                RecoveryStep.USERNAME_REVEALED -> {
                    val usernameToUse = state.revealedUsername.orEmpty()
                    Button(
                        onClick = { onUseRecoveredUsername(usernameToUse) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier.testTag("use_recovered_username_button")
                    ) {
                        Text("Use Username to Sign In")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSending && !state.isVerifying
            ) {
                Text(if (state.step == RecoveryStep.USERNAME_REVEALED) "Close" else "Cancel")
            }
        }
    )
}
