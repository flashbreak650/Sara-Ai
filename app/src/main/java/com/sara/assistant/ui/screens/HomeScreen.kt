package com.sara.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sara.assistant.ui.components.SaraOrb
import com.sara.assistant.ui.components.SaraOrbState
import com.sara.assistant.ui.theme.CyanBright
import com.sara.assistant.ui.theme.CyanGlow
import com.sara.assistant.ui.theme.NavyBorder
import com.sara.assistant.ui.theme.NavyDeep
import com.sara.assistant.ui.theme.NavyElevated
import com.sara.assistant.ui.theme.NavySurface
import com.sara.assistant.ui.theme.StatusNominal
import com.sara.assistant.ui.theme.TextMuted
import com.sara.assistant.ui.theme.TextPrimary
import com.sara.assistant.ui.theme.TextSecondary
import com.sara.assistant.ui.theme.VioletSoft
import java.util.Calendar

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

/**
 * Step 1: First working SARA Home Screen.
 * Provides bilingual greeting, animated SARA Orb, honest Step 1 feedback,
 * and clean bottom composer ready for Step 2 (Gemini) and Step 3 (Voice).
 */
@Composable
fun HomeScreen() {
    var language by remember { mutableStateOf(AppLanguage.BENGALI) }
    var orbState by remember { mutableStateOf(SaraOrbState.IDLE) }
    var draftMessage by remember { mutableStateOf("") }
    var feedbackNote by remember { mutableStateOf<String?>(null) }

    val greeting = remember(language) { resolveTimeGreeting(language) }
    val subtitle = if (language == AppLanguage.BENGALI) {
        "আমি সারা, আপনার ব্যক্তিগত এআই সহকারী"
    } else {
        "I am SARA, your personal AI assistant"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyDeep, ColorTopWash, NavyDeep)
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
            // Top Bar: Brand Identity + Language Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SARA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ধাপ ১ · মূল ফাউন্ডেশন" else "Step 1 · Core Foundation",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                }

                // Interactive Bilingual Segmented Control
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
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
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Time-Aware Bilingual Greeting
            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall.copy(color = TextPrimary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
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
                        onTap = {
                            orbState = when (orbState) {
                                SaraOrbState.IDLE -> SaraOrbState.ACTIVE_GREETING
                                SaraOrbState.ACTIVE_GREETING -> SaraOrbState.LISTENING_READY
                                SaraOrbState.LISTENING_READY -> SaraOrbState.IDLE
                            }
                            feedbackNote = if (language == AppLanguage.BENGALI) {
                                "সারা প্রস্তুত। পরবর্তী ধাপে জেমিনি এআই এবং ভয়েস যুক্ত হবে।"
                            } else {
                                "SARA UI ready. Gemini AI & Voice arrive in the next steps."
                            }
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
                            color = CyanBright
                        )
                    )

                    AnimatedVisibility(
                        visible = feedbackNote != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = feedbackNote.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }

            // Step 1 Honest Foundation Status Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NavySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FoundationRow(
                        title = if (language == AppLanguage.BENGALI) "নেটিভ Kotlin + Compose UI" else "Native Kotlin + Compose UI",
                        status = if (language == AppLanguage.BENGALI) "সক্রিয়" else "Active",
                        active = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = NavyBorder
                    )
                    FoundationRow(
                        title = if (language == AppLanguage.BENGALI) "জেমিনি এআই ব্রেইন ও ভয়েস" else "Gemini AI Brain & Voice",
                        status = if (language == AppLanguage.BENGALI) "ধাপ ২-এ আসছে" else "Next Step",
                        active = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Composer Bar (Text Input + Mic Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NavyElevated)
                    .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
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
                            text = if (language == AppLanguage.BENGALI) "সারাকে কিছু লিখুন..." else "Message SARA...",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                        )
                    }
                    BasicTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(CyanGlow),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(
                    onClick = {
                        orbState = SaraOrbState.LISTENING_READY
                        feedbackNote = if (language == AppLanguage.BENGALI) {
                            "ভয়েস পারমিশন ও স্পিচ রিকগনিশন ধাপ ৩-এ যুক্ত হবে।"
                        } else {
                            "Microphone permission & Bengali speech will be wired in Step 3."
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanGlow.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Input",
                        tint = CyanBright
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (draftMessage.isNotBlank()) {
                            val captured = draftMessage.trim()
                            draftMessage = ""
                            orbState = SaraOrbState.ACTIVE_GREETING
                            feedbackNote = if (language == AppLanguage.BENGALI) {
                                "\"$captured\" সংরক্ষিত। ধাপ ২-এ Gemini API উত্তর দেবে।"
                            } else {
                                "\"$captured\" captured. Gemini API responses arrive in Step 2."
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
                        .background(CyanGlow)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send Message",
                        tint = NavyDeep
                    )
                }
            }
        }
    }
}

private val ColorTopWash = androidx.compose.ui.graphics.Color(0xFF091229)

@Composable
private fun LanguageTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyanGlow.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) CyanBright else TextSecondary,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (active) StatusNominal else VioletSoft
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
