package com.sara.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sara.assistant.data.settings.ConversationEntry
import com.sara.assistant.data.settings.MemoryItem
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.VoiceLanguageOption
import com.sara.assistant.system.BackgroundAssistantPhase
import com.sara.assistant.system.BackgroundAssistantState
import com.sara.assistant.ui.components.SaraAvatarFrame
import com.sara.assistant.ui.components.SaraGlassButton
import com.sara.assistant.ui.components.SaraIconGlowButton
import com.sara.assistant.ui.components.SaraLogo
import com.sara.assistant.ui.components.SaraOrb
import com.sara.assistant.ui.components.SaraOrbState
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.components.VoiceWaveform
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusAmber
import com.sara.assistant.ui.theme.StatusDanger
import com.sara.assistant.ui.theme.StatusNominal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

/**
 * Light-Only Premium SARA Home Screen:
 * - Animated 3D-style AI Orb & Voice Waveform
 * - SARA Inbox header badge & quick action card
 * - Outside-App Background Assistant Start / Pause / Stop / Talk control panel
 * - Quick actions (Voice Test, SARA Inbox, Save Memory, Background & Floating)
 * - Real conversational input connected to SaraAssistantBrain
 */
@Composable
fun HomeScreen(
    settings: SaraSettings,
    memories: List<MemoryItem>,
    conversationHistory: List<ConversationEntry>,
    inboxUnreadCount: Int,
    isSpeaking: Boolean,
    backgroundState: BackgroundAssistantState,
    onOpenInbox: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onStartWakeService: () -> Unit,
    onTogglePauseService: () -> Unit,
    onStopWakeService: () -> Unit,
    onTalkOutside: () -> Unit,
    onOrbSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSubmitMessage: suspend (String) -> String,
    onAddMemory: (String) -> Unit
) {
    val colors = LocalSaraColors.current
    val scope = rememberCoroutineScope()

    var language by remember(settings.voiceLanguage) {
        mutableStateOf(
            if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) AppLanguage.BENGALI else AppLanguage.ENGLISH
        )
    }
    var orbState by remember { mutableStateOf(SaraOrbState.IDLE) }
    var draftMessage by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var feedbackNote by remember { mutableStateOf<String?>(null) }
    var showQuickMemoryDialog by remember { mutableStateOf(false) }

    val assistantName = settings.assistantName.ifBlank { "SARA" }
    val assistantSubtitle = settings.assistantSubtitle.ifBlank { "Your Personal AI" }
    val timeGreeting = remember(language) { resolveTimeGreeting(language) }
    val effectiveOrbState = when {
        isSpeaking -> SaraOrbState.SPEAKING
        isThinking -> SaraOrbState.ACTIVE_GREETING
        backgroundState.phase == BackgroundAssistantPhase.LISTENING_COMMAND -> SaraOrbState.LISTENING
        else -> orbState
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
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Brand & Navigation Header (No Profile navigation — clean Inbox + Language + Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface.copy(alpha = 0.92f))
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaraAvatarFrame(
                        size = 38.dp,
                        animationsEnabled = settings.animationsEnabled,
                        showOnlineBadge = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaraLogo(size = 16.dp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = assistantName.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = assistantSubtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.accentPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.94f))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageTab(
                            label = "বাং",
                            selected = language == AppLanguage.BENGALI,
                            onClick = { language = AppLanguage.BENGALI }
                        )
                        LanguageTab(
                            label = "EN",
                            selected = language == AppLanguage.ENGLISH,
                            onClick = { language = AppLanguage.ENGLISH }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        SaraIconGlowButton(
                            icon = Icons.Rounded.Inbox,
                            contentDescription = "Open SARA Inbox",
                            highlighted = inboxUnreadCount > 0,
                            onClick = onOpenInbox,
                            size = 42.dp
                        )
                        if (inboxUnreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(StatusDanger)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (inboxUnreadCount > 9) "9+" else inboxUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    SaraIconGlowButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Open Settings",
                        onClick = onOpenSettings,
                        size = 42.dp
                    )
                }
            }

            // Scrollable Center Workspace
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = timeGreeting,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.BENGALI) {
                        "আসসালামু আলাইকুম! আমি $assistantName। আপনাকে কীভাবে সাহায্য করতে পারি?"
                    } else {
                        "Assalamu Alaikum! I am $assistantName. How can I assist you today?"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SaraOrb(
                    state = effectiveOrbState,
                    orbSize = 186.dp,
                    animationsEnabled = settings.animationsEnabled,
                    onTap = {
                        if (isSpeaking) {
                            onStopSpeaking()
                            orbState = SaraOrbState.IDLE
                            return@SaraOrb
                        }
                        orbState = when (orbState) {
                            SaraOrbState.IDLE -> SaraOrbState.ACTIVE_GREETING
                            SaraOrbState.ACTIVE_GREETING -> SaraOrbState.LISTENING
                            SaraOrbState.LISTENING, SaraOrbState.SPEAKING -> SaraOrbState.IDLE
                        }
                        val spoken = if (language == AppLanguage.BENGALI) {
                            "আসসালামু আলাইকুম! আমি $assistantName। আপনার ${settings.femaleVoicePersona.voiceId} ভয়েস, ইনবক্স ও ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট প্রস্তুত।"
                        } else {
                            "Hello! I am $assistantName. Your ${settings.femaleVoicePersona.voiceId} voice, inbox and background assistant are ready."
                        }
                        feedbackNote = spoken
                        onOrbSpeak(spoken)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                VoiceWaveform(
                    state = effectiveOrbState,
                    animationsEnabled = settings.animationsEnabled
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isThinking -> if (language == AppLanguage.BENGALI) "চিন্তা করছি..." else "Thinking..."
                        effectiveOrbState == SaraOrbState.SPEAKING ->
                            if (language == AppLanguage.BENGALI) "মিষ্টি নারী কণ্ঠে শোনানো হচ্ছে (${settings.femaleVoicePersona.voiceId})..." else "Speaking with ${settings.femaleVoicePersona.voiceId}..."
                        effectiveOrbState == SaraOrbState.LISTENING ->
                            if (language == AppLanguage.BENGALI) "শোনার জন্য প্রস্তুত" else "Listening mode active"
                        effectiveOrbState == SaraOrbState.ACTIVE_GREETING ->
                            if (language == AppLanguage.BENGALI) "অভ্যর্থনা মোড সক্রিয়" else "Greeting mode active"
                        else -> if (language == AppLanguage.BENGALI) "অর্ব ট্যাপ করে কথা বলুন" else "Tap the orb to interact"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                if (isSpeaking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SaraGlassButton(
                        text = if (language == AppLanguage.BENGALI) "ভয়েস থামান" else "Stop Speaking",
                        icon = Icons.Rounded.Stop,
                        onClick = onStopSpeaking
                    )
                }

                AnimatedVisibility(
                    visible = feedbackNote != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = feedbackNote.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Outside-App Home Screen Assistant Compact Control Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = colors.accentPrimary.copy(alpha = 0.14f),
                            spotColor = colors.accentPrimary.copy(alpha = 0.14f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (backgroundState.serviceRunning && !backgroundState.isPaused) {
                            colors.accentPrimary.copy(alpha = 0.45f)
                        } else {
                            colors.border
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.BENGALI) {
                                        "হোম স্ক্রিন ও ব্যাকগ্রাউন্ড সারা"
                                    } else {
                                        "Outside-App Background SARA"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = backgroundState.statusMessage,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = when {
                                            !backgroundState.serviceRunning -> colors.textSecondary
                                            backgroundState.isPaused -> StatusAmber
                                            else -> StatusNominal
                                        }
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.elevated)
                                    .clickable(onClick = onOpenBackgroundSettings)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BENGALI) "সেটআপ" else "Setup",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!backgroundState.serviceRunning) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SaraPrimaryGradientButton(
                                    text = if (language == AppLanguage.BENGALI) "ব্যাকগ্রাউন্ড চালু করুন" else "Start Outside-App",
                                    icon = Icons.Rounded.PlayArrow,
                                    onClick = onStartWakeService,
                                    modifier = Modifier.weight(1f)
                                )
                                SaraGlassButton(
                                    text = if (language == AppLanguage.BENGALI) "ফ্লোটিং বাটন" else "Overlay",
                                    icon = Icons.Rounded.GraphicEq,
                                    onClick = onOpenBackgroundSettings
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SaraPrimaryGradientButton(
                                    text = if (language == AppLanguage.BENGALI) "কথা বলুন" else "Talk",
                                    icon = Icons.Rounded.Mic,
                                    onClick = onTalkOutside,
                                    modifier = Modifier.weight(1f)
                                )
                                SaraGlassButton(
                                    text = if (backgroundState.isPaused) {
                                        if (language == AppLanguage.BENGALI) "চালু" else "Resume"
                                    } else {
                                        if (language == AppLanguage.BENGALI) "বিরতি" else "Pause"
                                    },
                                    icon = if (backgroundState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    onClick = onTogglePauseService,
                                    modifier = Modifier.weight(1f)
                                )
                                SaraGlassButton(
                                    text = if (language == AppLanguage.BENGALI) "বন্ধ" else "Stop",
                                    icon = Icons.Rounded.Stop,
                                    tint = StatusDanger,
                                    onClick = onStopWakeService
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Cards Grid (2 x 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = if (language == AppLanguage.BENGALI) "ভয়েস পরীক্ষা" else "Test Voice",
                        subtitle = settings.femaleVoicePersona.labelBn,
                        onClick = {
                            val sample = if (language == AppLanguage.BENGALI) {
                                settings.femaleVoicePersona.previewSentenceBn.replace("সারা", assistantName)
                            } else {
                                settings.femaleVoicePersona.previewSentenceEn.replace("SARA", assistantName)
                            }
                            feedbackNote = sample
                            onOrbSpeak(sample)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Rounded.Inbox,
                        title = if (language == AppLanguage.BENGALI) "সারা ইনবক্স" else "SARA Inbox",
                        subtitle = if (inboxUnreadCount > 0) {
                            if (language == AppLanguage.BENGALI) "$inboxUnreadCount টি নতুন বার্তা" else "$inboxUnreadCount unread alerts"
                        } else {
                            if (language == AppLanguage.BENGALI) "সব বার্তা দেখুন" else "View all messages"
                        },
                        onClick = onOpenInbox,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.AddCircleOutline,
                        title = if (language == AppLanguage.BENGALI) "মেমোরি সেভ (${memories.size})" else "Save Note (${memories.size})",
                        subtitle = if (settings.memoryEnabled) {
                            if (language == AppLanguage.BENGALI) "নোট সংরক্ষণ করুন" else "Memory active"
                        } else {
                            if (language == AppLanguage.BENGALI) "সেটিংস থেকে চালু করুন" else "Enable in Settings"
                        },
                        onClick = {
                            if (settings.memoryEnabled) {
                                showQuickMemoryDialog = true
                            } else {
                                onOpenSettings()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Rounded.GraphicEq,
                        title = if (language == AppLanguage.BENGALI) "ওয়েক ওয়ার্ড" else "Wake Word",
                        subtitle = if (backgroundState.serviceRunning) {
                            "${settings.customWakeWord} · Active"
                        } else {
                            if (language == AppLanguage.BENGALI) "হোম স্ক্রিন অ্যাসিস্ট্যান্ট" else "Configure Outside-App"
                        },
                        onClick = onOpenBackgroundSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recent Conversations Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সাম্প্রতিক কথোপকথন" else "Recent Conversations",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "${conversationHistory.size} ${if (language == AppLanguage.BENGALI) "টি" else "items"}",
                                style = MaterialTheme.typography.labelMedium.copy(color = colors.accentPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (conversationHistory.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) {
                                    "এখনো কোনো কথোপকথন সংরক্ষিত নেই। নিচে লিখুন অথবা অর্ব ট্যাপ করুন।"
                                } else {
                                    "No saved messages yet. Type below or tap the orb to begin."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        } else {
                            conversationHistory.take(5).forEachIndexed { index, entry ->
                                val timeStr = remember(entry.timestampMillis) {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestampMillis))
                                }
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = colors.border.copy(alpha = 0.7f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = if (entry.isAssistant) "$assistantName: ${entry.message}" else "আপনি: ${entry.message}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (entry.isAssistant) colors.accentPrimary else colors.textPrimary,
                                            fontWeight = if (entry.isAssistant) FontWeight.Medium else FontWeight.Normal
                                        ),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Floating Crisp Light Glass Composer Dock
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = colors.accentPrimary.copy(alpha = 0.22f),
                        spotColor = colors.accentPrimary.copy(alpha = 0.22f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.34f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 9.dp)
                    ) {
                        if (draftMessage.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) {
                                    "${assistantName}-কে আপনার কথা লিখুন..."
                                } else {
                                    "Ask $assistantName anything..."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted)
                            )
                        }
                        BasicTextField(
                            value = draftMessage,
                            onValueChange = { draftMessage = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accentPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SaraIconGlowButton(
                        icon = Icons.Rounded.Mic,
                        contentDescription = "Voice Input",
                        highlighted = effectiveOrbState == SaraOrbState.LISTENING,
                        onClick = {
                            if (backgroundState.serviceRunning) {
                                onTalkOutside()
                            } else {
                                onOpenBackgroundSettings()
                            }
                        },
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(colors.accentPrimary, colors.accentSecondary)
                                )
                            )
                            .clickable(enabled = !isThinking) {
                                val captured = draftMessage.trim()
                                if (captured.isNotBlank()) {
                                    draftMessage = ""
                                    isThinking = true
                                    scope.launch {
                                        val replyText = onSubmitMessage(captured)
                                        isThinking = false
                                        orbState = SaraOrbState.ACTIVE_GREETING
                                        feedbackNote = replyText
                                    }
                                } else {
                                    feedbackNote = if (language == AppLanguage.BENGALI) {
                                        "প্রথমে একটি বার্তা লিখুন।"
                                    } else {
                                        "Type a message first."
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showQuickMemoryDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickMemoryDialog = false },
            title = { Text(if (language == AppLanguage.BENGALI) "নতুন মেমোরি যোগ করুন" else "Save Memory Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(if (language == AppLanguage.BENGALI) "তথ্য বা পছন্দ লিখুন" else "Fact or preference") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            onAddMemory(noteText)
                            feedbackNote = if (language == AppLanguage.BENGALI) {
                                "নতুন মেমোরি সংরক্ষিত হয়েছে।"
                            } else {
                                "Memory note saved."
                            }
                        }
                        showQuickMemoryDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = colors.accentPrimary.copy(alpha = 0.12f),
                spotColor = colors.accentPrimary.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.accentPrimary.copy(alpha = 0.20f),
                                colors.accentSecondary.copy(alpha = 0.14f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LanguageTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSaraColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            colors.accentPrimary.copy(alpha = 0.22f),
                            colors.accentSecondary.copy(alpha = 0.16f)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) colors.accentPrimary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

private fun resolveTimeGreeting(language: AppLanguage): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (language == AppLanguage.BENGALI) {
        when (hour) {
            in 5..11 -> "শুভ সকাল"
            in 12..15 -> "শুভ দুপুর"
            in 16..18 -> "শুভ বিকাল"
            in 19..21 -> "শুভ সন্ধ্যা"
            else -> "শুভ রাত্রি"
        }
    } else {
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
