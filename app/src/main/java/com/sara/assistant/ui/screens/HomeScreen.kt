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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
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
import com.sara.assistant.ui.components.SaraBottomDockBar
import com.sara.assistant.ui.components.SaraGlassButton
import com.sara.assistant.ui.components.SaraNavTab
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
 * Obsidian + Crimson Aura SARA Home Screen matching flagship reference UI:
 * - Clean editorial greeting header + language toggle
 * - Multi-ring Vortex Mic Orb hero centerpiece
 * - Inline prompt composer dock directly below the orb
 * - 2x2 dark gradient Bento action cards with tinted icon badges
 * - Quick Actions + Today's Insight / Latest Reply card + Outside-App Background controls
 * - Bottom Dock Bar with floating glowing Center Orb talk trigger
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
    onOpenFemaleVoices: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onStartWakeService: () -> Unit,
    onToggleContinuousVoice: () -> Unit,
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
    var draftMessage by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var feedbackNote by remember { mutableStateOf<String?>(null) }
    var showQuickMemoryDialog by remember { mutableStateOf(false) }

    val assistantName = settings.assistantName.ifBlank { "SARA" }
    val timeGreeting = remember(language) { resolveTimeGreeting(language) }
    val voiceAssistantOn = backgroundState.serviceRunning && !backgroundState.isPaused
    val activeSpeaking = isSpeaking || backgroundState.phase == BackgroundAssistantPhase.SPEAKING
    val activeThinking = isThinking || backgroundState.phase == BackgroundAssistantPhase.THINKING
    val effectiveOrbState = when {
        activeSpeaking -> SaraOrbState.SPEAKING
        activeThinking -> SaraOrbState.ACTIVE_GREETING
        voiceAssistantOn -> SaraOrbState.LISTENING
        else -> SaraOrbState.IDLE
    }

    val latestAssistantReply = remember(conversationHistory, feedbackNote, backgroundState.lastResponseText) {
        feedbackNote
            ?: backgroundState.lastResponseText.takeIf { it.isNotBlank() }
            ?: conversationHistory.firstOrNull { it.isAssistant }?.message
    }

    val sendPrompt: () -> Unit = {
        val captured = draftMessage.trim()
        if (captured.isNotBlank() && !isThinking) {
            draftMessage = ""
            isThinking = true
            scope.launch {
                val replyText = onSubmitMessage(captured)
                isThinking = false
                feedbackNote = replyText
            }
        } else if (captured.isBlank()) {
            feedbackNote = if (language == AppLanguage.BENGALI) {
                "প্রথমে একটি বার্তা লিখুন।"
            } else {
                "Type a message first."
            }
        }
    }

    val triggerOrbTalk: () -> Unit = {
        if (isSpeaking) {
            onStopSpeaking()
        } else {
            onToggleContinuousVoice()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.topWash, colors.canvas, colors.canvas)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Scrollable Flagship Workspace
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Clean Top Editorial Greeting + Language Pill + Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) {
                                "$timeGreeting, আমি $assistantName"
                            } else {
                                "Hello, I am $assistantName"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == AppLanguage.BENGALI) {
                                "আপনাকে কীভাবে সাহায্য করতে পারি?"
                            } else {
                                "How can I assist you today?"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(11.dp))
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
                        Spacer(modifier = Modifier.width(8.dp))
                        SaraAvatarFrame(
                            size = 38.dp,
                            animationsEnabled = settings.animationsEnabled,
                            showOnlineBadge = true,
                            onClick = onOpenSettings
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Hero Vortex Mic Orb
                SaraOrb(
                    state = effectiveOrbState,
                    orbSize = 196.dp,
                    animationsEnabled = settings.animationsEnabled,
                    onTap = triggerOrbTalk
                )

                if (activeSpeaking || activeThinking || effectiveOrbState != SaraOrbState.IDLE) {
                    VoiceWaveform(
                        state = effectiveOrbState,
                        animationsEnabled = settings.animationsEnabled
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            backgroundState.reconnecting ->
                                if (language == AppLanguage.BENGALI) "নেটওয়ার্ক পুনঃসংযোগ হচ্ছে..." else "Reconnecting..."
                            activeThinking ->
                                if (language == AppLanguage.BENGALI) "দ্রুত উত্তর তৈরি হচ্ছে..." else "Generating reply..."
                            effectiveOrbState == SaraOrbState.SPEAKING ->
                                if (language == AppLanguage.BENGALI) "${settings.femaleVoicePersona.voiceId} নারী কণ্ঠে বলছি..." else "Speaking (${settings.femaleVoicePersona.voiceId})..."
                            voiceAssistantOn ->
                                if (language == AppLanguage.BENGALI) "নিরবচ্ছিন্ন ভয়েস চালু (ON) — কথা বলুন..." else "Continuous Voice ON — Listening..."
                            backgroundState.serviceRunning && backgroundState.isPaused ->
                                if (language == AppLanguage.BENGALI) "ভয়েস বিরতিতে · ট্যাপ করে চালু করুন" else "Voice Paused — Tap to resume"
                            else -> if (language == AppLanguage.BENGALI) "ট্যাপ করে ভয়েস চালু করুন (OFF)" else "Tap orb to turn Voice ON"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (voiceAssistantOn || activeSpeaking) colors.accentPrimary else colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (activeSpeaking) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentPrimary.copy(alpha = 0.16f))
                                .border(1.dp, colors.accentPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onStopSpeaking)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "থামান" else "Stop",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else if (voiceAssistantOn) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentPrimary.copy(alpha = 0.16f))
                                .border(1.dp, colors.accentPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onStopWakeService)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "OFF" else "OFF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Inline Prompt Composer Dock directly under the Orb (matching Screenshot 1 & 2)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = colors.accentPrimary.copy(alpha = 0.20f),
                            spotColor = colors.accentPrimary.copy(alpha = 0.20f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            if (draftMessage.isEmpty()) {
                                Text(
                                    text = if (language == AppLanguage.BENGALI) {
                                        "${assistantName}-কে যেকোনো কিছু বলুন..."
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

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colors.accentPrimary, colors.accentSecondary)
                                    )
                                )
                                .clickable(enabled = !isThinking, onClick = sendPrompt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send Message",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Live Reply / Today's Insight Card (Matches Screenshot 2 bottom highlight card)
                AnimatedVisibility(
                    visible = latestAssistantReply != null || true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.30f))
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colors.cardTint, colors.surface)
                                    )
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (latestAssistantReply != null) {
                                        if (language == AppLanguage.BENGALI) "$assistantName উত্তর" else "$assistantName Reply"
                                    } else {
                                        if (language == AppLanguage.BENGALI) "আজকের ভাবনা · Today's Insight" else "Today's Insight"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = colors.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latestAssistantReply ?: if (language == AppLanguage.BENGALI) {
                                        "আপনার প্রতিটি দিন হোক সুন্দর ও সফল—সারা সবসময় আপনার পাশে আছে।"
                                    } else {
                                        "The future is generated by what you do today, not tomorrow."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.accentPrimary.copy(alpha = 0.16f))
                                    .border(1.dp, colors.accentPrimary.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        val speakText = latestAssistantReply ?: if (language == AppLanguage.BENGALI) {
                                            "আসসালামু আলাইকুম! আমি $assistantName, আপনাকে সাহায্য করতে প্রস্তুত।"
                                        } else {
                                            "Hello! I am $assistantName, ready to help."
                                        }
                                        onOrbSpeak(speakText)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeUp,
                                    contentDescription = "Speak Reply",
                                    tint = colors.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Primary 2x2 Dark Gradient Bento Cards (Matching Screenshot 1 & 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraBentoCard(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = if (language == AppLanguage.BENGALI) "ভয়েস মোড" else "Voice Mode",
                        subtitle = if (voiceAssistantOn) {
                            if (language == AppLanguage.BENGALI) "ON · নিরবচ্ছিন্ন শুনছি" else "ON · Continuous Active"
                        } else {
                            if (language == AppLanguage.BENGALI) {
                                "${settings.femaleVoicePersona.voiceId} · চালু করুন"
                            } else {
                                "OFF · Tap to turn ON"
                            }
                        },
                        highlighted = voiceAssistantOn || activeSpeaking,
                        onClick = onToggleContinuousVoice,
                        modifier = Modifier.weight(1f)
                    )
                    AuraBentoCard(
                        icon = Icons.Rounded.GraphicEq,
                        title = if (language == AppLanguage.BENGALI) "ব্যাকগ্রাউন্ড AI" else "Outside-App AI",
                        subtitle = if (backgroundState.serviceRunning) {
                            if (backgroundState.isPaused) "বিরতি · Paused" else "সক্রিয় · ${settings.customWakeWord}"
                        } else {
                            if (language == AppLanguage.BENGALI) "হোম স্ক্রিনে শুনুন" else "Background Voice"
                        },
                        highlighted = backgroundState.serviceRunning && !backgroundState.isPaused,
                        onClick = onOpenBackgroundSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraBentoCard(
                        icon = Icons.Rounded.Inbox,
                        title = if (language == AppLanguage.BENGALI) "সারা ইনবক্স" else "SARA Inbox",
                        subtitle = if (inboxUnreadCount > 0) {
                            if (language == AppLanguage.BENGALI) "$inboxUnreadCount টি নতুন বার্তা" else "$inboxUnreadCount unread alerts"
                        } else {
                            if (language == AppLanguage.BENGALI) "সব উত্তর ও নোট" else "Replies & Alerts"
                        },
                        highlighted = inboxUnreadCount > 0,
                        onClick = onOpenInbox,
                        modifier = Modifier.weight(1f)
                    )
                    AuraBentoCard(
                        icon = Icons.Rounded.Tune,
                        title = "Aura Control",
                        subtitle = if (language == AppLanguage.BENGALI) "অরা ও সিস্টেম কন্ট্রোল" else "System & Voices",
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Quick Actions Section Header + 2 Compact Obsidian Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "দ্রুত অ্যাকশন · Quick Actions" else "Quick Actions",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${conversationHistory.size} ${if (language == AppLanguage.BENGALI) "টি চ্যাট" else "chats"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.accentPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraCompactActionCard(
                        icon = Icons.Rounded.AddCircleOutline,
                        title = if (language == AppLanguage.BENGALI) "মেমোরি (${memories.size})" else "Memory (${memories.size})",
                        subtitle = if (language == AppLanguage.BENGALI) "নোট সেভ করুন" else "Save personal note",
                        onClick = {
                            if (settings.memoryEnabled) showQuickMemoryDialog = true else onOpenSettings()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuraCompactActionCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = if (language == AppLanguage.BENGALI) "নারী কণ্ঠ (৬টি)" else "6 Female Voices",
                        subtitle = settings.femaleVoicePersona.voiceId,
                        onClick = onOpenFemaleVoices,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Outside-App Live Control Strip (Direct Start / Talk / Pause / Stop)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(colors.cardTint.copy(alpha = 0.7f), colors.surface)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.BENGALI) {
                                        "হোম স্ক্রিন ও ফ্লোটিং সারা"
                                    } else {
                                        "Outside-App Floating SARA"
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
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সেটআপ" else "Setup",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.accentPrimary.copy(alpha = 0.14f))
                                    .clickable(onClick = onOpenBackgroundSettings)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
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
                                    text = if (language == AppLanguage.BENGALI) "ওভারলে" else "Overlay",
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

                // 8. Recent Conversations List (if any)
                if (conversationHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সাম্প্রতিক কথোপকথন" else "Recent Conversations",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            conversationHistory.take(4).forEachIndexed { index, entry ->
                                val timeStr = remember(entry.timestampMillis) {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestampMillis))
                                }
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = colors.border.copy(alpha = 0.6f)
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

            // 9. Sleek Obsidian Bottom Dock Bar with Floating Glowing Center Orb
            SaraBottomDockBar(
                activeTab = SaraNavTab.HOME,
                unreadInboxCount = inboxUnreadCount,
                orbActive = isSpeaking || effectiveOrbState != SaraOrbState.IDLE,
                animationsEnabled = settings.animationsEnabled,
                onHomeClick = {},
                onInboxClick = onOpenInbox,
                onCenterOrbClick = triggerOrbTalk,
                onMemoryClick = {
                    if (settings.memoryEnabled) showQuickMemoryDialog = true else onOpenSettings()
                },
                onSettingsClick = onOpenSettings
            )
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
private fun AuraBentoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (highlighted) 1.3.dp else 1.dp,
            color = if (highlighted) colors.accentPrimary.copy(alpha = 0.55f) else colors.border
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.cardTint, colors.surface)
                    )
                )
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.16f))
                    .border(1.dp, colors.accentPrimary.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
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
private fun AuraCompactActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    colors.accentPrimary.copy(alpha = 0.22f)
                } else {
                    Color.Transparent
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
