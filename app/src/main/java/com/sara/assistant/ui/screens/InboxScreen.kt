package com.sara.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sara.assistant.data.settings.InboxCategory
import com.sara.assistant.data.settings.SaraInboxItem
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.ui.components.SaraGlassButton
import com.sara.assistant.ui.components.SaraIconGlowButton
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusDanger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxFilter(val labelBn: String) {
    ALL("সব"),
    UNREAD("অপঠিত"),
    OUTSIDE_APP("ব্যাকগ্রাউন্ড"),
    REMINDERS("রিমাইন্ডার")
}

/**
 * Dedicated SARA Inbox screen showing outside-the-app responses, voice replies,
 * reminders, and assistant alerts with speak-aloud and permanent DataStore persistence.
 */
@Composable
fun InboxScreen(
    settings: SaraSettings,
    inboxItems: List<SaraInboxItem>,
    isSpeaking: Boolean,
    repository: SaraSettingsRepository,
    voiceEngine: SaraVoiceEngine,
    onBack: () -> Unit
) {
    val colors = LocalSaraColors.current
    var activeFilter by remember { mutableStateOf(InboxFilter.ALL) }
    var showComposeDialog by remember { mutableStateOf(false) }

    val unreadCount = remember(inboxItems) { inboxItems.count { !it.isRead } }
    val filteredItems = remember(inboxItems, activeFilter) {
        when (activeFilter) {
            InboxFilter.ALL -> inboxItems
            InboxFilter.UNREAD -> inboxItems.filter { !it.isRead }
            InboxFilter.OUTSIDE_APP -> inboxItems.filter {
                it.category == InboxCategory.WAKE_ALERT || it.category == InboxCategory.VOICE_REPLY
            }
            InboxFilter.REMINDERS -> inboxItems.filter {
                it.category == InboxCategory.REMINDER || it.category == InboxCategory.SYSTEM
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.canvas, colors.topWash, colors.canvas)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.94f))
                    .border(BorderStroke(1.dp, colors.border))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaraIconGlowButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সারা ইনবক্স · SARA Inbox",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (unreadCount > 0) "$unreadCount টি অপঠিত বার্তা" else "সব বার্তা পড়া হয়েছে",
                        style = MaterialTheme.typography.labelMedium.copy(color = colors.accentPrimary)
                    )
                }
                SaraPrimaryGradientButton(
                    text = "+ নতুন",
                    icon = Icons.Rounded.Add,
                    onClick = { showComposeDialog = true }
                )
            }

            // Filter Bar + Quick Bulk Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(InboxFilter.entries) { filter ->
                        val selected = filter == activeFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) colors.accentPrimary.copy(alpha = 0.16f) else colors.surface
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) colors.accentPrimary else colors.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { activeFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter.labelBn,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selected) colors.accentPrimary else colors.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                if (inboxItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SaraGlassButton(
                            text = "সব পঠিত · Mark All",
                            icon = Icons.Rounded.DoneAll,
                            enabled = unreadCount > 0,
                            onClick = { repository.markAllInboxRead() },
                            modifier = Modifier.weight(1f)
                        )
                        SaraGlassButton(
                            text = "ইনবক্স মুছুন · Clear",
                            icon = Icons.Rounded.DeleteOutline,
                            tint = StatusDanger,
                            onClick = { repository.clearInbox() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ইনবক্স খালি আছে",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "হোম স্ক্রিনে \"Hey SARA\" বললে বা অ্যাপে কথা বললে সারার বার্তাগুলো এখানে সংরক্ষিত হবে।",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        InboxMessageCard(
                            item = item,
                            isSpeaking = isSpeaking,
                            onSpeak = {
                                repository.markInboxRead(item.id)
                                voiceEngine.speakText(item.body, settings)
                            },
                            onMarkRead = { repository.markInboxRead(item.id) },
                            onDelete = { repository.deleteInboxItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showComposeDialog) {
        var titleDraft by remember { mutableStateOf("") }
        var bodyDraft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showComposeDialog = false },
            title = { Text("ইনবক্সে নতুন রিমাইন্ডার") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        label = { Text("শিরোনাম · Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bodyDraft,
                        onValueChange = { bodyDraft = it },
                        label = { Text("বার্তা বা নোট · Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (bodyDraft.isNotBlank()) {
                            repository.addInboxMessage(
                                title = titleDraft.ifBlank { "ব্যক্তিগত নোট · Reminder" },
                                body = bodyDraft,
                                category = InboxCategory.REMINDER,
                                senderVoicePersona = settings.femaleVoicePersona.voiceId
                            )
                        }
                        showComposeDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showComposeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InboxMessageCard(
    item: SaraInboxItem,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalSaraColors.current
    val timeText = remember(item.timestampMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestampMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (!item.isRead) 8.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = colors.accentPrimary.copy(alpha = 0.16f),
                spotColor = colors.accentPrimary.copy(alpha = 0.16f)
            )
            .clickable { onMarkRead() },
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (!item.isRead) 1.5.dp else 1.dp,
            color = if (!item.isRead) colors.accentPrimary.copy(alpha = 0.6f) else colors.border
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.accentPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.category.labelBn,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (!item.senderVoicePersona.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.senderVoicePersona,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                        )
                    }
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaraGlassButton(
                    text = if (isSpeaking) "শুনুন" else "শোনান · Speak",
                    icon = Icons.Rounded.VolumeUp,
                    onClick = onSpeak,
                    modifier = Modifier.weight(1f)
                )
                if (!item.isRead) {
                    IconButton(onClick = onMarkRead) {
                        Icon(
                            imageVector = Icons.Rounded.MarkEmailRead,
                            contentDescription = "Mark Read",
                            tint = colors.accentPrimary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = StatusDanger
                    )
                }
            }
        }
    }
}
