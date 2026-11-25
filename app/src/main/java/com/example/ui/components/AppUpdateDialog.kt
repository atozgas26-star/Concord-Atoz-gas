package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandGreenContainer
import com.example.ui.theme.BrandGreenOnContainer
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedContainer
import com.example.ui.theme.BrandRedOnContainer
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.JetBrainsMono
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateStatus
import com.example.update.AppUpdateUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppUpdateBanner(
    state: AppUpdateUiState,
    onOpenUpdateDialog: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.shouldShowBanner) return
    Surface(
        color = BrandRedContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandRedSecondary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_update_available_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = "New Version Available",
                tint = BrandRedSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New Version Available: v${state.latestVersion}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandRedOnContainer
                )
                Text(
                    text = "Installed: v${state.currentVersion} • Tap Update to get the latest build",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandRedOnContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = onOpenUpdateDialog,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("app_update_banner_button")
            ) {
                Text("Update")
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("app_update_banner_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Update Banner",
                    tint = BrandRedOnContainer
                )
            }
        }
    }
}

@Composable
fun AppUpdateSettingsCard(
    state: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUpdateAvailable = state.status == AppUpdateStatus.UPDATE_AVAILABLE
    val cardContainer = if (isUpdateAvailable) BrandRedContainer else MaterialTheme.colorScheme.surface
    val cardBorder = if (isUpdateAvailable) BrandRedSecondary else MaterialTheme.colorScheme.outlineVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_app_update_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isUpdateAvailable) Icons.Default.NewReleases else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (isUpdateAvailable) BrandRedSecondary else BrandGreenPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "App System Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current Build: v${state.currentVersion} • Admin, Owner & Staff",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            when (state.status) {
                AppUpdateStatus.UPDATE_AVAILABLE -> {
                    val sizeSuffix = state.apkSizeMb?.let { " (${String.format(Locale.US, "%.1f", it)} MB)" }.orEmpty()
                    Text(
                        text = "New build v${state.latestVersion} is ready$sizeSuffix.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandRedOnContainer
                    )
                }
                AppUpdateStatus.UP_TO_DATE -> {
                    Text(
                        text = "You are running the latest version (v${state.currentVersion}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AppUpdateStatus.ERROR -> {
                    Text(
                        text = state.errorMessage ?: "Could not check for updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                AppUpdateStatus.IDLE -> {
                    Text(
                        text = "Check official Concord & Atoz Gas Trading LLC releases for newer builds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCheckForUpdate,
                    enabled = !state.isChecking,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_check_update_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isChecking) "Checking..." else "Check Update")
                }
                if (isUpdateAvailable) {
                    Button(
                        onClick = onOpenUpdateDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_update_dialog_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Update v${state.latestVersion}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { AppUpdateManager.openTrustedUpdateUrl(context, state.releasePageUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_releases_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Releases")
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    state: AppUpdateUiState,
    roleLabel: String,
    onCheckForUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (state.status) {
                    AppUpdateStatus.UPDATE_AVAILABLE -> Icons.Default.NewReleases
                    AppUpdateStatus.UP_TO_DATE -> Icons.Default.CheckCircle
                    AppUpdateStatus.ERROR -> Icons.Default.ErrorOutline
                    AppUpdateStatus.IDLE -> Icons.Default.SystemUpdate
                },
                contentDescription = "App System Update",
                tint = when (state.status) {
                    AppUpdateStatus.UPDATE_AVAILABLE -> BrandRedSecondary
                    AppUpdateStatus.UP_TO_DATE -> BrandGreenPrimary
                    AppUpdateStatus.ERROR -> MaterialTheme.colorScheme.error
                    AppUpdateStatus.IDLE -> BrandGreenPrimary
                }
            )
        },
        title = {
            Column {
                Text(
                    text = "App System Update",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Concord & Atoz Gas Trading LLC • $roleLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = BrandGreenContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Installed Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreenOnContainer
                            )
                            Text(
                                text = "v${state.currentVersion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenOnContainer
                            )
                        }
                        if (state.latestVersion.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Latest Release",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGreenOnContainer
                                )
                                Text(
                                    text = "v${state.latestVersion}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                                        BrandRedSecondary
                                    } else {
                                        BrandGreenOnContainer
                                    }
                                )
                            }
                        }
                    }
                }

                if (state.isChecking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Checking official releases...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    when (state.status) {
                        AppUpdateStatus.UPDATE_AVAILABLE -> {
                            val sizeText = state.apkSizeMb?.let { " • ${String.format(Locale.US, "%.1f", it)} MB" }.orEmpty()
                            val dateText = state.publishedAt.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                            Text(
                                text = "${state.releaseTitle}$sizeText$dateText",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandRedSecondary
                            )
                            if (state.releaseNotes.isNotBlank()) {
                                HorizontalDivider()
                                Text(
                                    text = state.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Tapping Download opens the verified official release link in your browser.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AppUpdateStatus.UP_TO_DATE -> {
                            Text(
                                text = "C&A Gas is up to date! No newer build is available right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        AppUpdateStatus.ERROR -> {
                            Text(
                                text = state.errorMessage ?: "Unable to check for updates right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        AppUpdateStatus.IDLE -> {
                            Text(
                                text = "Tap Check Now to verify if a newer version of C&A Gas is available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (state.lastCheckedAtMs > 0L) {
                    val checkedAt = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                        .format(Date(state.lastCheckedAtMs))
                    Text(
                        text = "Last checked: $checkedAt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.status == AppUpdateStatus.UPDATE_AVAILABLE) {
                    Button(
                        onClick = {
                            val opened = AppUpdateManager.openTrustedUpdateUrl(context, state.downloadUrl)
                            if (!opened) {
                                onMessage("No browser found to open the update link.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_update_download_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download v${state.latestVersion}", color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val opened = AppUpdateManager.openTrustedUpdateUrl(context, state.releasePageUrl)
                        if (!opened) {
                            onMessage("No browser found to open the releases page.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_update_view_releases_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Official Releases Page")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCheckForUpdate,
                enabled = !state.isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                modifier = Modifier.testTag("app_update_check_now_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (state.isChecking) "Checking..." else "Check Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("app_update_close_button")
            ) {
                Text("Close")
            }
        }
    )
}
