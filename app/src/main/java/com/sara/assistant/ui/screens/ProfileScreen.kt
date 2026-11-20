package com.sara.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sara.assistant.data.settings.AnswerLength
import com.sara.assistant.data.settings.AssistantPersonalityMode
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.data.settings.VoiceProviderMode
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.system.VoiceEngineStatus
import com.sara.assistant.ui.components.SaraAvatarFrame
import com.sara.assistant.ui.components.SaraGlassButton
import com.sara.assistant.ui.components.SaraIconGlowButton
import com.sara.assistant.ui.components.SaraOrbState
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.components.SettingsCategoryCard
import com.sara.assistant.ui.components.SettingsSegmentedRow
import com.sara.assistant.ui.components.VoiceWaveform
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusAmber
import com.sara.assistant.ui.theme.StatusNominal

/**
 * SARA Futuristic AI Assistant Profile Screen.
 * Showcases SARA's animated character portrait, online activity, voice personality, and quick personalization.
 */
@Composable
fun ProfileScreen(
    settings: SaraSettings,
    memoryCount: Int,
    conversationCount: Int,
    voiceStatus: VoiceEngineStatus,
    wakeServiceRunning: Boolean,
    isSpeaking: Boolean,
    repository: SaraSettingsRepository,
    voiceEngine: SaraVoiceEngine,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalSaraColors.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var voiceFeedback by remember { mutableStateOf<String?>(null) }

    val assistantName = settings.assistantName.ifBlank { "SARA" }
    val subtitle = settings.assistantSubtitle.ifBlank { "Your Personal AI" }

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
                    .background(colors.surface.copy(alpha = 0.88f))
                    .border(BorderStroke(1.dp, colors.border))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaraIconGlowButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "এআই প্রোফাইল",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "$assistantName Identity & Voice Persona",
                            style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                        )
                    }
                }

                SaraGlassButton(
                    text = "এডিট",
                    icon = Icons.Rounded.Edit,
                    onClick = { showEditProfileDialog = true }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Portrait Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colors.accentPrimary.copy(alpha = 0.16f),
                                            colors.accentSecondary.copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SaraAvatarFrame(
                                    size = 136.dp,
                                    animationsEnabled = settings.animationsEnabled,
                                    showOnlineBadge = true
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = assistantName.uppercase(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = colors.accentPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Assistant Status Row
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.canvas.copy(alpha = 0.72f))
                                        .border(1.dp, StatusNominal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusNominal)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSpeaking) {
                                            "কথা বলছি · Voice Active"
                                        } else {
                                            "অনলাইন · সহায়তার জন্য প্রস্তুত"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(color = StatusNominal)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = settings.personalityDescription,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                VoiceWaveform(
                                    state = if (isSpeaking) SaraOrbState.SPEAKING else SaraOrbState.ACTIVE_GREETING,
                                    animationsEnabled = settings.animationsEnabled
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SaraPrimaryGradientButton(
                                        text = if (isSpeaking) "চলছে..." else "কণ্ঠ শুনুন",
                                        icon = Icons.Rounded.VolumeUp,
                                        onClick = {
                                            voiceEngine.speakTest(settings) { result ->
                                                voiceFeedback = result.message
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SaraGlassButton(
                                        text = "প্রোফাইল এডিট",
                                        icon = Icons.Rounded.Edit,
                                        onClick = { showEditProfileDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (voiceFeedback != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = voiceFeedback.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.accentPrimary),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Personality Card
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.RecordVoiceOver,
                        titleBn = "কণ্ঠের ব্যক্তিত্ব · Voice Persona",
                        subtitleEn = "Warm Adult Male Bengali Profile"
                    ) {
                        ProfileKeyValueRow(
                            label = "ভয়েস ইঞ্জিন · Engine",
                            value = settings.voiceProvider.labelEn,
                            highlight = true
                        )
                        HorizontalDivider(color = colors.border)
                        ProfileKeyValueRow(
                            label = "কণ্ঠের ধরন · Tone",
                            value = "${settings.voiceGender.labelBn} · ${settings.voiceGender.labelEn}"
                        )
                        HorizontalDivider(color = colors.border)
                        ProfileKeyValueRow(
                            label = "বাংলা উচ্চারণ · Pronunciation",
                            value = settings.bengaliPronunciationQuality.labelEn
                        )
                        HorizontalDivider(color = colors.border)
                        ProfileKeyValueRow(
                            label = "গতি / ভলিউম · Speed / Vol",
                            value = "${String.format("%.2fx", settings.speechSpeed)} · ${(settings.voiceVolume * 100).toInt()}%"
                        )

                        if (voiceStatus.honestMaleLimitationNote != null &&
                            settings.voiceProvider == VoiceProviderMode.ANDROID_NATIVE_TTS
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusAmber.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = voiceStatus.honestMaleLimitationNote,
                                    style = MaterialTheme.typography.bodySmall.copy(color = StatusAmber),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Assistant Activity Status Card
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.GraphicEq,
                        titleBn = "অ্যাসিস্ট্যান্ট অ্যাক্টিভিটি স্ট্যাটাস",
                        subtitleEn = "Live System & Memory Metrics"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ActivityStatTile(
                                label = "মেমোরি · Memories",
                                value = "$memoryCount",
                                modifier = Modifier.weight(1f)
                            )
                            ActivityStatTile(
                                label = "কথোপকথন · Turns",
                                value = "$conversationCount",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ActivityStatTile(
                                label = "এআই মডেল · Model",
                                value = settings.selectedModel.title.replace("Gemini ", "G"),
                                modifier = Modifier.weight(1f)
                            )
                            ActivityStatTile(
                                label = "ওয়েক ওয়ার্ড · Wake",
                                value = if (wakeServiceRunning) "Active" else "Standby",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Personalization Quick Controls
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.AutoAwesome,
                        titleBn = "পার্সোনালাইজেশন · Personalization",
                        subtitleEn = "Adjust behavior & speech preference instantly"
                    ) {
                        SettingsSegmentedRow(
                            titleBn = "আচরণের ধরন",
                            subtitleEn = "Personality Mode",
                            options = AssistantPersonalityMode.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.personalityMode,
                            onSelect = { mode ->
                                repository.updateSettings { it.copy(personalityMode = mode) }
                            }
                        )

                        SettingsSegmentedRow(
                            titleBn = "উত্তরের দৈর্ঘ্য",
                            subtitleEn = "Answer Length",
                            options = AnswerLength.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.answerLength,
                            onSelect = { length ->
                                repository.updateSettings { it.copy(answerLength = length) }
                            }
                        )

                        SettingsSegmentedRow(
                            titleBn = "ভয়েস প্রোভাইডার",
                            subtitleEn = "Voice Source",
                            options = listOf(
                                VoiceProviderMode.CLOUD_NEURAL_MALE to "ক্লাউড পুরুষ কণ্ঠ",
                                VoiceProviderMode.ANDROID_NATIVE_TTS to "ডিভাইস TTS"
                            ),
                            selected = settings.voiceProvider,
                            onSelect = { provider ->
                                repository.updateSettings { it.copy(voiceProvider = provider) }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SaraGlassButton(
                            text = "সব সেটিংস খুলুন · Open All Settings",
                            icon = Icons.Rounded.Settings,
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = settings.assistantName,
            initialSubtitle = settings.assistantSubtitle,
            initialBio = settings.personalityDescription,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, sub, bio ->
                repository.updateSettings {
                    it.copy(
                        assistantName = name.ifBlank { "SARA" },
                        assistantSubtitle = sub.ifBlank { "Your Personal AI" },
                        personalityDescription = bio.ifBlank { it.personalityDescription }
                    )
                }
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
private fun ProfileKeyValueRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    val colors = LocalSaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (highlight) colors.accentPrimary else colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ActivityStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.canvas.copy(alpha = 0.72f))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = colors.accentPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialSubtitle: String,
    initialBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var sub by remember { mutableStateOf(initialSubtitle) }
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
        title = { Text("প্রোফাইল এডিট · Edit AI Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Assistant Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sub,
                    onValueChange = { sub = it },
                    label = { Text("Subtitle (e.g. Your Personal AI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Personality & Voice Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), sub.trim(), bio.trim()) }) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
