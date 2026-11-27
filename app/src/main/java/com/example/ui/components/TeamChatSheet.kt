package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ChatChannel
import com.example.data.model.UserRole
import com.example.data.remote.CloudTypingStatus
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandRedSecondary
import com.example.ui.theme.RoyalElectricBlue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChatBlue = RoyalElectricBlue
private val ChatRed = BrandRedSecondary
private val ChatGreen = BrandGreenPrimary

@Composable
fun TeamChatDialog(
    currentUserRole: UserRole,
    currentUserName: String,
    teamMessages: List<ChatMessageEntity>,
    adminOwnerMessages: List<ChatMessageEntity>,
    activeTypingStatuses: List<CloudTypingStatus>,
    onSendMessage: (channel: ChatChannel, text: String, isImportant: Boolean) -> Unit,
    onTypingChanged: (channel: ChatChannel, isTyping: Boolean) -> Unit,
    onMarkChatRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAccessAdminOwner = currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.OWNER_VIEW
    var selectedChannel by remember { mutableStateOf(ChatChannel.TEAM_CHAT) }
    var draftText by remember { mutableStateOf("") }
    var isImportant by remember { mutableStateOf(false) }

    val displayedMessages = if (selectedChannel == ChatChannel.ADMIN_OWNER && canAccessAdminOwner) {
        adminOwnerMessages
    } else {
        teamMessages
    }

    val channelTypers = activeTypingStatuses.filter { it.channel == selectedChannel.name }
    val listState = rememberLazyListState()

    LaunchedEffect(displayedMessages.size, selectedChannel) {
        onMarkChatRead()
        if (displayedMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayedMessages.lastIndex)
        }
    }

    // Auto-clear typing indicator after 3 seconds of inactivity
    LaunchedEffect(draftText, selectedChannel) {
        if (draftText.isNotBlank()) {
            onTypingChanged(selectedChannel, true)
            delay(3000L)
            onTypingChanged(selectedChannel, false)
        } else {
            onTypingChanged(selectedChannel, false)
        }
    }

    DisposableEffect(selectedChannel) {
        onDispose {
            onTypingChanged(selectedChannel, false)
        }
    }

    Dialog(
        onDismissRequest = {
            onTypingChanged(selectedChannel, false)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .obsidianRoyalAtmosphere()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "C&A Gas Team & Admin Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedChannel == ChatChannel.ADMIN_OWNER) {
                                "Private Admin & Owner Channel"
                            } else {
                                "Admin, Owner & Staff Conversation"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(
                        onClick = {
                            onTypingChanged(selectedChannel, false)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("team_chat_close_btn")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close chat", tint = Color.White)
                    }
                }
                SignatureAccentHairline(height = 2.dp)

                // Channel selector (Admin & Owner get both tabs; Staff gets Team Chat only)
                if (canAccessAdminOwner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedChannel == ChatChannel.TEAM_CHAT,
                            onClick = { selectedChannel = ChatChannel.TEAM_CHAT },
                            label = { Text("Team & Staff", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_channel_team_btn")
                        )
                        FilterChip(
                            selected = selectedChannel == ChatChannel.ADMIN_OWNER,
                            onClick = { selectedChannel = ChatChannel.ADMIN_OWNER },
                            label = { Text("Admin & Owner", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_channel_admin_owner_btn")
                        )
                    }
                    HorizontalDivider()
                }

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 400.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayedMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No messages yet. Start the conversation below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(displayedMessages, key = { it.remoteId.ifBlank { "m_${it.id}" } }) { msg ->
                            val isMe = msg.senderName.equals(currentUserName, ignoreCase = true) &&
                                msg.senderRole == currentUserRole.name
                            ChatMessageBubble(msg = msg, isMe = isMe)
                        }
                    }
                }

                // Live Typing Indicator Pill
                if (channelTypers.isNotEmpty()) {
                    val typingText = when (channelTypers.size) {
                        1 -> "${channelTypers.first().senderName} (${channelTypers.first().senderRole}) is typing..."
                        else -> "${channelTypers.size} people are typing..."
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .testTag("chat_typing_indicator"),
                        shape = RoundedCornerShape(10.dp),
                        color = ChatBlue.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = typingText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ChatBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Composer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = isImportant,
                            onClick = { isImportant = !isImportant },
                            label = {
                                Text(
                                    text = if (isImportant) "IMPORTANT ALERT ON" else "Mark Important",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChatRed,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("chat_important_toggle")
                        )
                        if (isImportant) {
                            Text(
                                text = "Rings Bell Alert",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChatRed
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it.take(500) },
                            placeholder = {
                                Text(
                                    if (isImportant) "Type important announcement..."
                                    else "Write a message..."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input"),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 3
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (draftText.isNotBlank()) {
                                if (isImportant) ChatRed else ChatBlue
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    val clean = draftText.trim()
                                    if (clean.isNotEmpty()) {
                                        onSendMessage(selectedChannel, clean, isImportant)
                                        draftText = ""
                                        isImportant = false
                                    }
                                },
                                enabled = draftText.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (draftText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessageEntity,
    isMe: Boolean
) {
    val roleColor = when (msg.senderRole) {
        UserRole.ADMIN.name -> ChatRed
        UserRole.OWNER_VIEW.name -> ChatBlue
        else -> ChatGreen
    }
    val roleLabel = when (msg.senderRole) {
        UserRole.ADMIN.name -> "ADMIN"
        UserRole.OWNER_VIEW.name -> "OWNER"
        else -> "STAFF"
    }
    val timeText = SimpleDateFormat("hh:mm a", Locale.US).format(Date(msg.createdAtMs))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .testTag("chat_msg_${msg.id}"),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 14.dp
            ),
            color = when {
                msg.isImportant -> ChatRed.copy(alpha = 0.09f)
                isMe -> ChatBlue.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            border = if (msg.isImportant) BorderStroke(1.5.dp, ChatRed) else null
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = roleColor
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (msg.isImportant) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ChatRed
                        ) {
                            Text(
                                text = "IMPORTANT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = msg.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
