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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.VoiceLanguageOption
import com.sara.assistant.ui.components.SaraOrb
import com.sara.assistant.ui.components.SaraOrbState
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusNominal
import java.util.Calendar

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

/**
 * SARA Main Home Screen.
 * Preserves Step 1 visual identity, animated Orb, bilingual greeting, and bottom composer
 * while integrating live Settings state and Settings screen navigation.
 */
@Composable
fun HomeScreen(
    settings: SaraSettings,
    memoryCount: Int,
    conversationCount: Int,
    onOpenSettings: () -> Unit,
    onOrbSpeak: (String) -> Unit,
    onSubmitMessage: (String) -> Unit
) {
    val colors = LocalSaraColors.current
    var language by remember(settings.voiceLanguage) {
        mutableStateOf(
            if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) AppLanguage.BENGALI else AppLanguage.ENGLISH
        )
    }
    var orbState by remember { mutableStateOf(SaraOrbState.IDLE) }
    var draftMessage by remember { mutableStateOf("") }
    var feedbackNote by remember { mutableStateOf<String?>(null) }

    val assistantName = settings.assistantName.ifBlank { "SARA" }
    val greeting = remember(language) { resolveTimeGreeting(language) }
    val subtitle = if (language == AppLanguage.BENGALI) {
        "আমি $assistantName, আপনার ব্যক্তিগত এআই সহকারী"
    } else {
        "I am $assistantName, your personal AI assistant"
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Brand Identity + Language Switch + Settings Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assistantName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) {
                            "${settings.personalityMode.labelBn} · ${settings.selectedModel.title}"
                        } else {
                            "${settings.personalityMode.labelEn} · ${settings.selectedModel.title}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Interactive Bilingual Segmented Control
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageTab(
                            label = "বাংলা",
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

                    // Settings Navigation Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open Settings",
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Time-Aware Bilingual Greeting
            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall.copy(color = colors.textPrimary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary),
                textAlign = TextAlign.Center
            )

            // Center Focal Anchor: Animated SARA Orb
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SaraOrb(
                        state = orbState,
                        animationsEnabled = settings.animationsEnabled,
                        onTap = {
                            orbState = when (orbState) {
                                SaraOrbState.IDLE -> SaraOrbState.ACTIVE_GREETING
                                SaraOrbState.ACTIVE_GREETING -> SaraOrbState.LISTENING_READY
                                SaraOrbState.LISTENING_READY -> SaraOrbState.IDLE
                            }
                            val msg = if (language == AppLanguage.BENGALI) {
                                "$assistantName প্রস্তুত। ভয়েস, মেমোরি ও সেটিংস সক্রিয় রয়েছে।"
                            } else {
                                "$assistantName is ready. Voice, memory & settings are active."
                            }
                            feedbackNote = msg
                            onOrbSpeak(msg)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when (orbState) {
                            SaraOrbState.IDLE -> if (language == AppLanguage.BENGALI) "ট্যাপ করে পরীক্ষা করুন" else "Tap orb to interact"
                            SaraOrbState.ACTIVE_GREETING -> if (language == AppLanguage.BENGALI) "অভ্যর্থনা মোড সক্রিয়" else "Greeting state active"
                            SaraOrbState.LISTENING_READY -> if (language == AppLanguage.BENGALI) "ভয়েস UI প্রস্তুত" else "Voice UI ready"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.accentPrimary
                        )
                    )

                    AnimatedVisibility(
                        visible = feedbackNote != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = feedbackNote.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }

            // Live Foundation & Settings Status Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FoundationRow(
                        title = if (language == AppLanguage.BENGALI) {
                            "ভয়েস ও সেটিংস সিস্টেম"
                        } else {
                            "Voice & Settings System"
                        },
                        status = if (settings.voiceResponsesEnabled) {
                            if (language == AppLanguage.BENGALI) "সক্রিয়" else "Active"
                        } else {
                            if (language == AppLanguage.BENGALI) "নীরব" else "Muted"
                        },
                        active = settings.voiceResponsesEnabled
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = colors.border
                    )
                    FoundationRow(
                        title = if (language == AppLanguage.BENGALI) {
                            "মেমোরি ($memoryCount) · বার্তা ($conversationCount)"
                        } else {
                            "Memories ($memoryCount) · History ($conversationCount)"
                        },
                        status = if (settings.memoryEnabled) {
                            if (language == AppLanguage.BENGALI) "সংরক্ষণ চালু" else "Memory On"
                        } else {
                            if (language == AppLanguage.BENGALI) "বন্ধ" else "Paused"
                        },
                        active = settings.memoryEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Composer Bar (Text Input + Mic Button + Send)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    if (draftMessage.isEmpty()) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) {
                                "${assistantName}-কে কিছু লিখুন..."
                            } else {
                                "Message $assistantName..."
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(color = colors.textMuted)
                        )
                    }
                    BasicTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(
                    onClick = {
                        orbState = SaraOrbState.LISTENING_READY
                        feedbackNote = if (settings.wakeWordEnabled) {
                            if (language == AppLanguage.BENGALI) {
                                "\"${settings.customWakeWord}\" সক্রিয়। সেটিংস থেকে ভয়েস ও মাইক নিয়ন্ত্রণ করুন।"
                            } else {
                                "\"${settings.customWakeWord}\" active. Manage microphone & voice in Settings."
                            }
                        } else {
                            if (language == AppLanguage.BENGALI) {
                                "সেটিংস > ওয়েক ওয়ার্ড থেকে মাইক্রোফোন অনুমতি দিন।"
                            } else {
                                "Grant Microphone access in Settings > Wake Word."
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Input",
                        tint = colors.accentPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (draftMessage.isNotBlank()) {
                            val captured = draftMessage.trim()
                            draftMessage = ""
                            orbState = SaraOrbState.ACTIVE_GREETING
                            onSubmitMessage(captured)
                            feedbackNote = if (language == AppLanguage.BENGALI) {
                                "\"$captured\" হিস্টোরিতে সংরক্ষিত হয়েছে।"
                            } else {
                                "\"$captured\" saved to conversation history."
                            }
                        } else {
                            feedbackNote = if (language == AppLanguage.BENGALI) {
                                "প্রথমে একটি বার্তা লিখুন।"
                            } else {
                                "Type a message first."
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send Message",
                        tint = colors.canvas
                    )
                }
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accentPrimary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) colors.accentPrimary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}

@Composable
private fun FoundationRow(
    title: String,
    status: String,
    active: Boolean
) {
    val colors = LocalSaraColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (active) StatusNominal else colors.accentSecondary
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
