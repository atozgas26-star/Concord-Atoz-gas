package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppNotificationEntity
import com.example.data.local.PendingStoreTransferEntity
import com.example.data.model.NotificationCategory
import com.example.data.model.StoreTransferDirection
import com.example.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AlertRedDot = Color(0xFFDC2626)
private val BrandBlue = Color(0xFF0B3A8C)
private val ApproveGreen = Color(0xFF15803D)

@Composable
fun HeaderBellIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = Color.White
) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = if (unreadCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                contentDescription = "Notifications Bell",
                tint = tint
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(CircleShape)
                    .background(AlertRedDot)
                    .padding(horizontal = if (unreadCount > 9) 4.dp else 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
fun HeaderChatIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = Color.White
) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Team & Admin Chat",
                tint = tint
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .clip(CircleShape)
                    .background(AlertRedDot)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
fun HeaderUpdateIconButton(
    hasUpdate: Boolean,
    isChecking: Boolean,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = Color.White
) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .testTag(testTag)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = tint
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = if (hasUpdate) "System App Update Available" else "Check App Update",
                    tint = tint
                )
            }
        }
        if (hasUpdate && !isChecking) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(AlertRedDot)
            )
        }
    }
}

@Composable
fun NotificationsBellDialog(
    role: UserRole,
    notifications: List<AppNotificationEntity>,
    pendingTransfers: List<PendingStoreTransferEntity>,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onApproveTransfer: (Long) -> Unit,
    onRejectTransfer: (Long) -> Unit,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAdmin = role == UserRole.ADMIN
    val isAdminOrOwner = role == UserRole.ADMIN || role == UserRole.OWNER_VIEW
    val unreadCount = notifications.count { !it.isRead }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBlue)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifications & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                isAdmin && pendingTransfers.isNotEmpty() ->
                                    "${pendingTransfers.size} store approval(s) • $unreadCount unread alert(s)"
                                unreadCount > 0 -> "$unreadCount unread reminder(s)"
                                else -> "All reminders checked"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("notifications_mark_all_read_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Mark all read",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Read All", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("notifications_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close notifications",
                            tint = Color.White
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Store-to-Vehicle / Vehicle-to-Store Approvals (Admin & Owner View)
                    if (isAdminOrOwner && pendingTransfers.isNotEmpty()) {
                        item {
                            Text(
                                text = "PENDING STORE CYLINDER APPROVALS (${pendingTransfers.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AlertRedDot
                            )
                        }
                        items(pendingTransfers, key = { "req_${it.id}" }) { req ->
                            PendingStoreTransferCard(
                                request = req,
                                canApprove = isAdmin,
                                onApprove = { onApproveTransfer(req.id) },
                                onReject = { onRejectTransfer(req.id) }
                            )
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ALL MESSAGES & REMINDERS (${notifications.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenChat()
                                },
                                modifier = Modifier.testTag("notifications_open_chat_btn")
                            ) {
                                Text("Open Team Chat", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (notifications.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = ApproveGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "No remaining notifications",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Transfers, store approvals, duty punches, app updates, and important chat alerts will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(notifications, key = { "notif_${it.id}" }) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                onClick = {
                                    if (!notif.isRead) onMarkRead(notif.id)
                                    if (notif.category == NotificationCategory.CHAT_IMPORTANT.name) {
                                        onDismiss()
                                        onOpenChat()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingStoreTransferCard(
    request: PendingStoreTransferEntity,
    canApprove: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dirLabel = when (request.direction) {
        StoreTransferDirection.STORE_TO_VEHICLE_FULL.name -> "Full Received from Store → ${request.vehicleName}"
        StoreTransferDirection.VEHICLE_TO_STORE_FULL.name -> "Full Returned to Store ← ${request.vehicleName}"
        else -> "Empty Returned to Store ← ${request.vehicleName}"
    }
    val timeLabel = SimpleDateFormat("hh:mm a", Locale.US).format(Date(request.requestedAtMs))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_store_transfer_card_${request.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = AlertRedDot.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, AlertRedDot.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = AlertRedDot,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${request.quantity} × ${request.productName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dirLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRedDot.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AlertRedDot,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Staff: ${request.staffName} • ${request.businessDate} at $timeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (request.notes.isNotBlank()) {
                Text(
                    text = "Note: ${request.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canApprove) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("approve_store_transfer_btn_${request.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApproveGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Accept Entry", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(0.75f)
                            .heightIn(min = 48.dp)
                            .testTag("reject_store_transfer_btn_${request.id}"),
                        border = BorderStroke(1.dp, AlertRedDot),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reject", color = AlertRedDot, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Owner View (Read-Only) — Awaiting Admin verification",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: AppNotificationEntity,
    onClick: () -> Unit
) {
    val icon = when (notification.category) {
        NotificationCategory.TRANSFER_EXTRA.name -> Icons.Filled.LocalShipping
        NotificationCategory.STORE_APPROVAL.name -> Icons.Filled.Storefront
        NotificationCategory.ATTENDANCE.name -> Icons.Filled.AccessTime
        NotificationCategory.CHAT_IMPORTANT.name -> Icons.Filled.Campaign
        NotificationCategory.SYSTEM_UPDATE.name -> Icons.Filled.SystemUpdate
        else -> Icons.Filled.Info
    }
    val accent = when {
        notification.isUrgent -> AlertRedDot
        notification.category == NotificationCategory.ATTENDANCE.name -> BrandBlue
        else -> MaterialTheme.colorScheme.primary
    }
    val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(notification.createdAtMs))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) {
                accent.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = if (!notification.isRead) BorderStroke(1.dp, accent.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AlertRedDot)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}
