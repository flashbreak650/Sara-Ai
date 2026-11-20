package com.sara.assistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sara.assistant.BuildConfig
import com.sara.assistant.data.settings.AccentColorOption
import com.sara.assistant.data.settings.AnswerLength
import com.sara.assistant.data.settings.ApiConnectionState
import com.sara.assistant.data.settings.AssistantPersonalityMode
import com.sara.assistant.data.settings.BengaliPronunciationQuality
import com.sara.assistant.data.settings.ConversationEntry
import com.sara.assistant.data.settings.FemaleVoicePersona
import com.sara.assistant.data.settings.GeminiModelOption
import com.sara.assistant.data.settings.MemoryItem
import com.sara.assistant.data.settings.ResponseStyle
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.data.settings.TextSizeOption
import com.sara.assistant.data.settings.ThemeModeOption
import com.sara.assistant.data.settings.VoiceGenderPreference
import com.sara.assistant.data.settings.VoiceLanguageOption
import com.sara.assistant.data.settings.VoiceProviderMode
import com.sara.assistant.system.AndroidPermissionHelper
import com.sara.assistant.system.GeminiConnectionChecker
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.system.SaraWakeService
import com.sara.assistant.system.VoiceEngineStatus
import com.sara.assistant.ui.components.PermissionTone
import com.sara.assistant.ui.components.SaraAvatarFrame
import com.sara.assistant.ui.components.SaraGlassButton
import com.sara.assistant.ui.components.SaraIconGlowButton
import com.sara.assistant.ui.components.SaraOrb
import com.sara.assistant.ui.components.SaraOrbState
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.components.SettingsCategoryCard
import com.sara.assistant.ui.components.SettingsDropdownRow
import com.sara.assistant.ui.components.SettingsPermissionRow
import com.sara.assistant.ui.components.SettingsSegmentedRow
import com.sara.assistant.ui.components.SettingsSliderRow
import com.sara.assistant.ui.components.SettingsSwitchRow
import com.sara.assistant.ui.components.VoiceProviderCard
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.StatusAmber
import com.sara.assistant.ui.theme.StatusDanger
import com.sara.assistant.ui.theme.StatusNominal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Upgraded Premium 10-Category SARA Settings Screen.
 * Adds Natural Male Bengali Voice selection cards, Profile shortcut, glassmorphic cards,
 * and gradient tactile buttons across every functional setting.
 */
@Composable
fun SettingsScreen(
    settings: SaraSettings,
    memories: List<MemoryItem>,
    conversationHistory: List<ConversationEntry>,
    voiceStatus: VoiceEngineStatus,
    wakeServiceRunning: Boolean,
    isSpeaking: Boolean,
    repository: SaraSettingsRepository,
    voiceEngine: SaraVoiceEngine,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSaraColors.current
    val scope = rememberCoroutineScope()

    var permissionSnapshot by remember { mutableStateOf(AndroidPermissionHelper.captureSnapshot(context)) }
    val refreshPermissions = { permissionSnapshot = AndroidPermissionHelper.captureSnapshot(context) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissions()
        if (!granted) {
            repository.updateSettings { it.copy(wakeWordEnabled = false) }
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val systemSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshPermissions() }

    var showNameDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showPersonalityDialog by remember { mutableStateOf(false) }
    var showCloudTtsDialog by remember { mutableStateOf(false) }
    var showWakeWordDialog by remember { mutableStateOf(false) }
    var showApiConfigDialog by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var voiceFeedback by remember { mutableStateOf<String?>(null) }
    var wakeFeedback by remember { mutableStateOf<String?>(null) }
    var apiConnectionState by remember { mutableStateOf(ApiConnectionState.UNCONFIGURED) }
    var apiStatusMessage by remember {
        mutableStateOf(
            if (settings.hasStoredApiKey || settings.apiProxyUrl.isNotBlank()) {
                "কনফিগার করা আছে · সংযোগ পরীক্ষা করুন"
            } else {
                "কনফিগার করা নেই (Not configured)"
            }
        )
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
            // Futuristic Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.90f))
                    .border(BorderStroke(1.dp, colors.border))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaraIconGlowButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Home",
                    onClick = onBack,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সারা সেটিংস",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "SARA Control Center · Premium Settings",
                        style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                    )
                }
                SaraGlassButton(
                    text = "প্রোফাইল",
                    icon = Icons.Rounded.Person,
                    onClick = onOpenProfile
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PROFILE CUSTOMIZATION BANNER
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.32f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SaraAvatarFrame(
                                size = 64.dp,
                                animationsEnabled = settings.animationsEnabled,
                                showOnlineBadge = true,
                                onClick = onOpenProfile
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.assistantName.ifBlank { "SARA" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = settings.assistantSubtitle.ifBlank { "Your Personal AI" },
                                    style = MaterialTheme.typography.labelMedium.copy(color = colors.accentPrimary)
                                )
                                Text(
                                    text = "Voice: ${settings.voiceGender.labelEn}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                            SaraGlassButton(
                                text = "প্রোফাইল",
                                onClick = onOpenProfile
                            )
                        }
                    }
                }

                // 1. AI PERSONALITY
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.AutoAwesome,
                        titleBn = "১. এআই ব্যক্তিত্ব ও পরিচয়",
                        subtitleEn = "AI Identity & Personality"
                    ) {
                        EditableInfoRow(
                            titleBn = "সহকারীর নাম · Assistant Name",
                            value = settings.assistantName,
                            onEdit = { showNameDialog = true }
                        )
                        HorizontalDivider(color = colors.border)
                        EditableInfoRow(
                            titleBn = "পরিচয় সাবটাইটেল · Subtitle",
                            value = settings.assistantSubtitle,
                            onEdit = { showSubtitleDialog = true }
                        )
                        HorizontalDivider(color = colors.border)
                        EditableInfoRow(
                            titleBn = "ব্যক্তিত্বের ধরন · Assistant Personality",
                            value = settings.personalityDescription,
                            onEdit = { showPersonalityDialog = true }
                        )
                        HorizontalDivider(color = colors.border)
                        SettingsSegmentedRow(
                            titleBn = "আচরণের মোড",
                            subtitleEn = "Friendly or Professional mode",
                            options = AssistantPersonalityMode.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.personalityMode,
                            onSelect = { mode ->
                                repository.updateSettings { it.copy(personalityMode = mode) }
                            }
                        )
                        SettingsDropdownRow(
                            titleBn = "উত্তরের ধরন",
                            subtitleEn = "Response Style",
                            selectedLabel = "${settings.responseStyle.labelBn} (${settings.responseStyle.labelEn})",
                            options = ResponseStyle.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            onSelect = { style ->
                                repository.updateSettings { it.copy(responseStyle = style) }
                            }
                        )
                        SettingsSegmentedRow(
                            titleBn = "উত্তরের দৈর্ঘ্য",
                            subtitleEn = "Short or Detailed answers",
                            options = AnswerLength.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.answerLength,
                            onSelect = { len ->
                                repository.updateSettings { it.copy(answerLength = len) }
                            }
                        )
                    }
                }

                // 2. VOICE SETTINGS (NATURAL HUMAN FEMALE BENGALI DEFAULT)
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.RecordVoiceOver,
                        titleBn = "২. স্বাভাবিক বাংলা নারী কণ্ঠ ও ভয়েস",
                        subtitleEn = "Natural Female Bengali Voice & TTS"
                    ) {
                        SettingsSwitchRow(
                            titleBn = "ভয়েস উত্তর চালু রাখুন",
                            subtitleEn = "Enable or disable spoken responses",
                            checked = settings.voiceResponsesEnabled,
                            onCheckedChange = { enabled ->
                                repository.updateSettings { it.copy(voiceResponsesEnabled = enabled) }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ভয়েস সোর্স নির্বাচন · Voice Engine",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        VoiceProviderCard(
                            titleBn = VoiceProviderMode.CLOUD_NEURAL_FEMALE.labelBn,
                            titleEn = VoiceProviderMode.CLOUD_NEURAL_FEMALE.labelEn,
                            descriptionBn = VoiceProviderMode.CLOUD_NEURAL_FEMALE.descriptionBn,
                            badge = "প্রস্তাবিত",
                            selected = settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE,
                            onClick = {
                                repository.updateSettings {
                                    it.copy(
                                        voiceProvider = VoiceProviderMode.CLOUD_NEURAL_FEMALE,
                                        voiceGender = VoiceGenderPreference.FEMALE
                                    )
                                }
                            }
                        )

                        VoiceProviderCard(
                            titleBn = VoiceProviderMode.ANDROID_NATIVE_TTS.labelBn,
                            titleEn = VoiceProviderMode.ANDROID_NATIVE_TTS.labelEn,
                            descriptionBn = VoiceProviderMode.ANDROID_NATIVE_TTS.descriptionBn,
                            badge = when {
                                voiceStatus.neuralNativeFemaleAvailable -> "Neural"
                                voiceStatus.femaleVoiceAvailable -> "Female Found"
                                else -> "Offline"
                            },
                            selected = settings.voiceProvider == VoiceProviderMode.ANDROID_NATIVE_TTS,
                            onClick = {
                                repository.updateSettings {
                                    it.copy(voiceProvider = VoiceProviderMode.ANDROID_NATIVE_TTS)
                                }
                            }
                        )

                        if (voiceStatus.honestQualityNote != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusAmber.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = voiceStatus.honestQualityNote,
                                    style = MaterialTheme.typography.bodySmall.copy(color = StatusAmber),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        EditableInfoRow(
                            titleBn = "ক্লাউড নিউরাল TTS এন্ডপয়েন্ট · Cloud TTS URL",
                            value = settings.cloudTtsEndpointUrl.ifBlank { "Set backend /api/tts URL or save Gemini API Key" },
                            onEdit = { showCloudTtsDialog = true }
                        )
                        HorizontalDivider(color = colors.border)

                        SettingsDropdownRow(
                            titleBn = "নারী কণ্ঠের ধরন (পার্সোনা)",
                            subtitleEn = "Female Voice Persona (${settings.femaleVoicePersona.voiceId})",
                            selectedLabel = settings.femaleVoicePersona.labelEn,
                            options = FemaleVoicePersona.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            onSelect = { persona ->
                                repository.updateSettings {
                                    it.copy(
                                        femaleVoicePersona = persona,
                                        voiceGender = VoiceGenderPreference.FEMALE
                                    )
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FemaleVoicePersona.entries.forEach { persona ->
                                SaraGlassButton(
                                    text = "প্রিভিউ ${persona.voiceId}",
                                    icon = Icons.Rounded.VolumeUp,
                                    onClick = {
                                        voiceFeedback = "কণ্ঠ প্রিভিউ চলছে (${persona.voiceId})..."
                                        voiceEngine.previewPersona(persona, settings) { result ->
                                            voiceFeedback = "${result.providerUsed}: ${result.message}"
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        HorizontalDivider(color = colors.border)

                        SettingsSegmentedRow(
                            titleBn = "ভয়েসের ভাষা",
                            subtitleEn = "Voice language: Bengali or English",
                            options = VoiceLanguageOption.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.voiceLanguage,
                            onSelect = { lang ->
                                repository.updateSettings { it.copy(voiceLanguage = lang) }
                            }
                        )

                        SettingsDropdownRow(
                            titleBn = "কণ্ঠের পছন্দ",
                            subtitleEn = "Default: Natural Female",
                            selectedLabel = "${settings.voiceGender.labelBn} (${settings.voiceGender.labelEn})",
                            options = VoiceGenderPreference.entries.map { pref ->
                                val statusSuffix = when (pref) {
                                    VoiceGenderPreference.FEMALE ->
                                        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE || voiceStatus.femaleVoiceAvailable) {
                                            ""
                                        } else {
                                            " · Cloud Recommended"
                                        }
                                    VoiceGenderPreference.MALE -> ""
                                    VoiceGenderPreference.SYSTEM_DEFAULT -> ""
                                }
                                pref to "${pref.labelBn} · ${pref.labelEn}$statusSuffix"
                            },
                            onSelect = { gender ->
                                repository.updateSettings { it.copy(voiceGender = gender) }
                            }
                        )

                        SettingsSegmentedRow(
                            titleBn = "বাংলা উচ্চারণ ও বিরতি",
                            subtitleEn = "Bengali Pronunciation & Human Pauses",
                            options = BengaliPronunciationQuality.entries.map {
                                it to if (it == BengaliPronunciationQuality.NATURAL_HUMAN) "প্রাকৃতিক · Natural" else "দ্রুত · Standard"
                            },
                            selected = settings.bengaliPronunciationQuality,
                            onSelect = { q ->
                                repository.updateSettings { it.copy(bengaliPronunciationQuality = q) }
                            }
                        )

                        SettingsSliderRow(
                            titleBn = "কথার গতি",
                            subtitleEn = "Speech speed",
                            valueLabel = String.format(Locale.US, "%.2fx", settings.speechSpeed),
                            value = settings.speechSpeed,
                            valueRange = 0.6f..1.8f,
                            onValueChange = { v ->
                                repository.updateSettings { it.copy(speechSpeed = v) }
                            }
                        )
                        SettingsSliderRow(
                            titleBn = "কণ্ঠের পিচ (ডিভাইস TTS)",
                            subtitleEn = "Speech pitch",
                            valueLabel = String.format(Locale.US, "%.2fx", settings.speechPitch),
                            value = settings.speechPitch,
                            valueRange = 0.75f..1.4f,
                            onValueChange = { v ->
                                repository.updateSettings { it.copy(speechPitch = v) }
                            }
                        )
                        SettingsSliderRow(
                            titleBn = "ভয়েস ভলিউম",
                            subtitleEn = "Voice volume",
                            valueLabel = "${(settings.voiceVolume * 100).roundToInt()}%",
                            value = settings.voiceVolume,
                            valueRange = 0.0f..1.0f,
                            onValueChange = { v ->
                                repository.updateSettings { it.copy(voiceVolume = v) }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SaraPrimaryGradientButton(
                                text = if (isSpeaking) "চলছে..." else "ভয়েস পরীক্ষা করুন · Test Voice",
                                icon = Icons.Rounded.VolumeUp,
                                onClick = {
                                    voiceFeedback = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
                                        "কণ্ঠ প্রস্তুত হচ্ছে..."
                                    } else {
                                        "Preparing voice..."
                                    }
                                    voiceEngine.speakTest(settings) { result ->
                                        voiceFeedback = "${result.providerUsed}: ${result.message}"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSpeaking) {
                                SaraGlassButton(
                                    text = "থামান",
                                    icon = Icons.Rounded.Stop,
                                    onClick = { voiceEngine.stopPlayback() }
                                )
                            }
                        }

                        if (voiceFeedback != null) {
                            Text(
                                text = voiceFeedback.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.accentPrimary),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // 3. WAKE WORD SETTINGS
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.GraphicEq,
                        titleBn = "৩. ওয়েক ওয়ার্ড সেটিংস",
                        subtitleEn = "Hey SARA & Background Listening"
                    ) {
                        SettingsSwitchRow(
                            titleBn = "\"Hey SARA\" সক্রিয় করুন",
                            subtitleEn = if (permissionSnapshot.speechRecognitionAvailable) {
                                "Requires Microphone permission & foreground service"
                            } else {
                                "Speech recognition unavailable on this device"
                            },
                            checked = settings.wakeWordEnabled,
                            enabled = permissionSnapshot.speechRecognitionAvailable,
                            onCheckedChange = { enable ->
                                if (enable && !permissionSnapshot.microphoneGranted) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    wakeFeedback = "আগে মাইক্রোফোন অনুমতি দিন।"
                                } else {
                                    repository.updateSettings { it.copy(wakeWordEnabled = enable) }
                                    if (!enable && wakeServiceRunning) {
                                        SaraWakeService.stop(context)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = colors.border)
                        EditableInfoRow(
                            titleBn = "কাস্টম ওয়েক ওয়ার্ড · Custom Wake Word",
                            value = if (permissionSnapshot.speechRecognitionAvailable) {
                                settings.customWakeWord
                            } else {
                                "Unavailable (No speech recognizer)"
                            },
                            onEdit = {
                                if (permissionSnapshot.speechRecognitionAvailable) {
                                    showWakeWordDialog = true
                                } else {
                                    wakeFeedback = "এই ডিভাইসে স্পিচ রিকগনিশন সাপোর্ট পাওয়া যায়নি।"
                                }
                            }
                        )
                        HorizontalDivider(color = colors.border)
                        SettingsPermissionRow(
                            titleBn = "মাইক্রোফোন অনুমতি",
                            subtitleEn = "Microphone permission status",
                            statusLabel = if (permissionSnapshot.microphoneGranted) "অনুমোদিত (Granted)" else "অনুমতি প্রয়োজন (Required)",
                            tone = if (permissionSnapshot.microphoneGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = if (permissionSnapshot.microphoneGranted) "App Info" else "Grant",
                            onAction = {
                                if (permissionSnapshot.microphoneGranted) {
                                    systemSettingsLauncher.launch(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "ব্যাটারি অপটিমাইজেশন",
                            subtitleEn = "Allow background wake service without OS sleep",
                            statusLabel = if (permissionSnapshot.batteryOptimizationIgnored) "অপটিমাইজেশন মুক্ত (Unrestricted)" else "সীমিত হতে পারে (Optimized)",
                            tone = if (permissionSnapshot.batteryOptimizationIgnored) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = "Configure",
                            onAction = {
                                AndroidPermissionHelper.openBatteryOptimizationSettings(context)
                                wakeFeedback = "সিস্টেম সেটিংস থেকে SARA-কে Unrestricted সিলেক্ট করুন।"
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (wakeServiceRunning) "ব্যাকগ্রাউন্ড লিসেনিং চালু" else "ব্যাকগ্রাউন্ড লিসেনিং বন্ধ",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = if (wakeServiceRunning) StatusNominal else colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = if (wakeServiceRunning) "Foreground service running" else "Service stopped",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                                )
                            }
                            if (wakeServiceRunning) {
                                SaraGlassButton(
                                    text = "Stop",
                                    tint = StatusDanger,
                                    onClick = {
                                        SaraWakeService.stop(context)
                                        wakeFeedback = "ব্যাকগ্রাউন্ড লিসেনিং বন্ধ করা হয়েছে।"
                                    }
                                )
                            } else {
                                SaraPrimaryGradientButton(
                                    text = "Start Listening",
                                    onClick = {
                                        if (!permissionSnapshot.microphoneGranted) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            wakeFeedback = "সার্ভিস চালু করতে মাইক্রোফোন অনুমতি দিন।"
                                        } else {
                                            val started = SaraWakeService.start(context, settings.customWakeWord)
                                            if (started) {
                                                repository.updateSettings { it.copy(wakeWordEnabled = true) }
                                                wakeFeedback = "ব্যাকগ্রাউন্ড লিসেনিং সার্ভিস চালু হয়েছে।"
                                            } else {
                                                wakeFeedback = "মাইক্রোফোন অনুমতি ছাড়া চালু করা যায়নি।"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (wakeFeedback != null) {
                            Text(
                                text = wakeFeedback.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.accentPrimary),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                // 4. PHONE CONTROL SETTINGS
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.PhoneAndroid,
                        titleBn = "৪. ফোন কন্ট্রোল সেটিংস",
                        subtitleEn = "Phone Control & Android Permissions"
                    ) {
                        SettingsPermissionRow(
                            titleBn = "অ্যাপ লঞ্চিং ক্ষমতা",
                            subtitleEn = "Open installed applications",
                            statusLabel = if (permissionSnapshot.appLaunchingSupported) "প্রস্তুত (Ready via Launcher Query)" else "সাপোর্ট নেই (Unavailable)",
                            tone = if (permissionSnapshot.appLaunchingSupported) PermissionTone.GRANTED else PermissionTone.UNAVAILABLE,
                            actionLabel = "Manage",
                            onAction = {
                                systemSettingsLauncher.launch(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "ফোন কল অনুমতি",
                            subtitleEn = "android.permission.CALL_PHONE",
                            statusLabel = if (permissionSnapshot.callPhoneGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই (Denied)",
                            tone = if (permissionSnapshot.callPhoneGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = if (permissionSnapshot.callPhoneGranted) "App Settings" else "Request",
                            onAction = {
                                if (permissionSnapshot.callPhoneGranted) {
                                    AndroidPermissionHelper.openAppSettings(context)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "এসএমএস অনুমতি",
                            subtitleEn = "android.permission.SEND_SMS",
                            statusLabel = if (permissionSnapshot.sendSmsGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই (Denied)",
                            tone = if (permissionSnapshot.sendSmsGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = if (permissionSnapshot.sendSmsGranted) "App Settings" else "Request",
                            onAction = {
                                if (permissionSnapshot.sendSmsGranted) {
                                    AndroidPermissionHelper.openAppSettings(context)
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "অ্যাক্সেসিবিলিটি সার্ভিস",
                            subtitleEn = "Accessibility Service status (System Settings)",
                            statusLabel = if (permissionSnapshot.accessibilityEnabled) "সক্রিয় (Enabled)" else "নিষ্ক্রিয় (Disabled)",
                            tone = if (permissionSnapshot.accessibilityEnabled) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = "Open Settings",
                            onAction = {
                                systemSettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "নোটিফিকেশন অ্যাক্সেস",
                            subtitleEn = "Notification Listener access",
                            statusLabel = if (permissionSnapshot.notificationAccessEnabled) "সক্রিয় (Enabled)" else "নিষ্ক্রিয় (Disabled)",
                            tone = if (permissionSnapshot.notificationAccessEnabled) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = "Open Settings",
                            onAction = {
                                systemSettingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                        SettingsPermissionRow(
                            titleBn = "স্ক্রিন ক্যাপচার অনুমতি",
                            subtitleEn = "MediaProjection (Per-session consent required by Android)",
                            statusLabel = if (permissionSnapshot.screenCaptureSupported) "প্রতিরক্ষিত (Consent per capture)" else "সাপোর্ট নেই (Unavailable)",
                            tone = if (permissionSnapshot.screenCaptureSupported) PermissionTone.GRANTED else PermissionTone.UNAVAILABLE,
                            actionLabel = "Refresh",
                            onAction = refreshPermissions
                        )
                    }
                }

                // 5. AI BRAIN SETTINGS
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.Psychology,
                        titleBn = "৫. এআই ব্রেইন সেটিংস",
                        subtitleEn = "Gemini AI Brain & Configuration"
                    ) {
                        SettingsPermissionRow(
                            titleBn = "জেমিনি এপিআই সংযোগ",
                            subtitleEn = apiStatusMessage,
                            statusLabel = when (apiConnectionState) {
                                ApiConnectionState.CONNECTED -> "সংযুক্ত (Connected)"
                                ApiConnectionState.CHECKING -> "পরীক্ষা চলছে..."
                                ApiConnectionState.ERROR -> "সংযোগ ত্রুটি (Error)"
                                ApiConnectionState.UNCONFIGURED -> if (settings.hasStoredApiKey || settings.apiProxyUrl.isNotBlank()) {
                                    "কনফিগার করা (Ready to test)"
                                } else {
                                    "কনফিগার করা নেই (Unconfigured)"
                                }
                            },
                            tone = when (apiConnectionState) {
                                ApiConnectionState.CONNECTED -> PermissionTone.GRANTED
                                ApiConnectionState.ERROR -> PermissionTone.UNAVAILABLE
                                else -> PermissionTone.WARNING
                            },
                            actionLabel = "Test",
                            onAction = {
                                apiConnectionState = ApiConnectionState.CHECKING
                                apiStatusMessage = "সংযোগ যাচাই হচ্ছে..."
                                scope.launch {
                                    val result = GeminiConnectionChecker.verifyConnection(
                                        proxyUrl = settings.apiProxyUrl,
                                        keyStorage = repository.secureKeyStorage
                                    )
                                    apiConnectionState = result.state
                                    apiStatusMessage = "${result.messageBn} · ${result.messageEn}"
                                }
                            }
                        )

                        EditableInfoRow(
                            titleBn = "নিরাপদ API কনফিগারেশন · Protected Keystore / Proxy",
                            value = buildString {
                                append(if (settings.hasStoredApiKey) "API Key: Keystore-এ সুরক্ষিত" else "API Key: সেট করা নেই")
                                if (settings.apiProxyUrl.isNotBlank()) {
                                    append(" · Proxy set")
                                }
                            },
                            onEdit = { showApiConfigDialog = true }
                        )

                        HorizontalDivider(color = colors.border)

                        SettingsDropdownRow(
                            titleBn = "এআই মডেল নির্বাচন",
                            subtitleEn = settings.selectedModel.descriptionBn,
                            selectedLabel = settings.selectedModel.title,
                            options = GeminiModelOption.entries.map { it to "${it.title} (${it.modelId})" },
                            onSelect = { model ->
                                repository.updateSettings { it.copy(selectedModel = model) }
                            }
                        )

                        SettingsSwitchRow(
                            titleBn = "ইন্টারনেট সার্চ ইন্টিগ্রেশন",
                            subtitleEn = "Allow web search grounding when enabled",
                            checked = settings.internetSearchEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(internetSearchEnabled = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "কথোপকথনের প্রসঙ্গ মনে রাখুন",
                            subtitleEn = "Remember conversation context across turns",
                            checked = settings.rememberContextEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(rememberContextEnabled = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "স্ট্রিমিং রেসপন্স",
                            subtitleEn = "Stream AI text tokens as they generate",
                            checked = settings.streamResponsesEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(streamResponsesEnabled = v) }
                            }
                        )
                    }
                }

                // 6. MEMORY SETTINGS
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.Storage,
                        titleBn = "৬. মেমোরি সেটিংস",
                        subtitleEn = "Saved Memories & Conversation History"
                    ) {
                        SettingsSwitchRow(
                            titleBn = "মেমোরি সিস্টেম চালু রাখুন",
                            subtitleEn = "Save user-approved facts and preferences",
                            checked = settings.memoryEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(memoryEnabled = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "সংবেদনশীল মেমোরি মাস্কিং",
                            subtitleEn = "Hide memory text preview in list until revealed",
                            checked = settings.memoryPrivacyMask,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(memoryPrivacyMask = v) }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সংরক্ষিত মেমোরি (${memories.size})",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            SaraGlassButton(
                                text = "+ Add Note",
                                enabled = settings.memoryEnabled,
                                onClick = { showAddMemoryDialog = true }
                            )
                        }

                        if (memories.isEmpty()) {
                            Text(
                                text = "এখনো কোনো মেমোরি সংরক্ষিত নেই (No saved memories yet).",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            memories.forEach { memory ->
                                MemoryListItemRow(
                                    item = memory,
                                    masked = settings.memoryPrivacyMask,
                                    onDelete = { repository.deleteMemory(memory.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SaraGlassButton(
                            text = "কথোপকথনের ইতিহাস মুছুন · Clear History (${conversationHistory.size})",
                            enabled = conversationHistory.isNotEmpty(),
                            tint = StatusDanger,
                            onClick = { showClearHistoryConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 7. APPEARANCE
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.Palette,
                        titleBn = "৭. অ্যাপিয়ারেন্স ও ডিজাইন",
                        subtitleEn = "Theme, Accent Color, Animations & Text Size"
                    ) {
                        SettingsSegmentedRow(
                            titleBn = "থিম মোড",
                            subtitleEn = "Dark, Light, or System Default",
                            options = ThemeModeOption.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.themeMode,
                            onSelect = { mode ->
                                repository.updateSettings { it.copy(themeMode = mode) }
                            }
                        )

                        Text(
                            text = "অ্যাকসেন্ট রঙ · Accent Color",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentColorOption.entries.forEach { accent ->
                                val selected = accent == settings.accentColor
                                val swatchColor = Color(accent.primaryHex)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) swatchColor.copy(alpha = 0.16f) else colors.canvas)
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) swatchColor else colors.border,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            repository.updateSettings { it.copy(accentColor = accent) }
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = accent.labelBn,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (selected) swatchColor else colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSwitchRow(
                            titleBn = "অর্ব ও ইউআই অ্যানিমেশন",
                            subtitleEn = "Enable smooth orbital and transition animations",
                            checked = settings.animationsEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(animationsEnabled = v) }
                            }
                        )
                        SettingsSegmentedRow(
                            titleBn = "লেখার আকার",
                            subtitleEn = "Text size scale",
                            options = TextSizeOption.entries.map { it to "${it.labelBn} · ${it.labelEn}" },
                            selected = settings.textSize,
                            onSelect = { size ->
                                repository.updateSettings { it.copy(textSize = size) }
                            }
                        )
                    }
                }

                // 8. NOTIFICATIONS
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.Notifications,
                        titleBn = "৮. নোটিফিকেশন",
                        subtitleEn = "Assistant, Voice & Background Alerts"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SettingsPermissionRow(
                                titleBn = "অ্যান্ড্রয়েড নোটিফিকেশন অনুমতি",
                                subtitleEn = "POST_NOTIFICATIONS (Android 13+)",
                                statusLabel = if (permissionSnapshot.notificationsGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই (Denied)",
                                tone = if (permissionSnapshot.notificationsGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                                actionLabel = if (permissionSnapshot.notificationsGranted) "Settings" else "Grant",
                                onAction = {
                                    if (permissionSnapshot.notificationsGranted) {
                                        AndroidPermissionHelper.openAppSettings(context)
                                    } else {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                        SettingsSwitchRow(
                            titleBn = "সহকারী নোটিফিকেশন",
                            subtitleEn = "Enable assistant reminder and reply notifications",
                            checked = settings.assistantNotificationsEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(assistantNotificationsEnabled = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "ভয়েস নোটিফিকেশন অ্যালার্ট",
                            subtitleEn = "Speak incoming alert summaries when enabled",
                            checked = settings.voiceNotificationsEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(voiceNotificationsEnabled = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "ব্যাকগ্রাউন্ড সার্ভিস নোটিফিকেশন",
                            subtitleEn = "Show persistent foreground status when Wake Service is active",
                            checked = settings.backgroundServiceNotificationEnabled,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(backgroundServiceNotificationEnabled = v) }
                            }
                        )
                    }
                }

                // 9. PRIVACY & SECURITY
                item {
                    val grantedCount = listOf(
                        permissionSnapshot.microphoneGranted,
                        permissionSnapshot.callPhoneGranted,
                        permissionSnapshot.sendSmsGranted,
                        permissionSnapshot.notificationsGranted,
                        permissionSnapshot.accessibilityEnabled
                    ).count { it }

                    SettingsCategoryCard(
                        icon = Icons.Rounded.Security,
                        titleBn = "৯. প্রাইভেসি ও সিকিউরিটি",
                        subtitleEn = "Permissions, Privacy & App Lock"
                    ) {
                        SettingsPermissionRow(
                            titleBn = "অ্যাপ পারমিশন সারাংশ",
                            subtitleEn = "Active runtime & system permissions overview",
                            statusLabel = "$grantedCount / 5 অনুমোদিত",
                            tone = if (grantedCount > 0) PermissionTone.GRANTED else PermissionTone.WARNING,
                            actionLabel = "OS Settings",
                            onAction = {
                                systemSettingsLauncher.launch(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "স্ক্রিন লক থাকলে মাইক্রোফোন সতর্কতা",
                            subtitleEn = "Microphone privacy: restrict background capture on lock",
                            checked = settings.micPrivacyAutoMuteOnLock,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(micPrivacyAutoMuteOnLock = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "স্ক্রিন ক্যাপচারে প্রতিবার অনুমতি",
                            subtitleEn = "Always ask explicit user consent before screen analysis",
                            checked = settings.screenCaptureAlwaysConfirm,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(screenCaptureAlwaysConfirm = v) }
                            }
                        )
                        SettingsSwitchRow(
                            titleBn = "অ্যাপ লক (ডিভাইস সিকিউরিটি)",
                            subtitleEn = if (permissionSnapshot.deviceLockSupported) {
                                "Require phone PIN / biometric on launch"
                            } else {
                                "Unavailable: Set a phone screen lock in Android Settings first"
                            },
                            checked = settings.appLockEnabled && permissionSnapshot.deviceLockSupported,
                            enabled = permissionSnapshot.deviceLockSupported,
                            onCheckedChange = { v ->
                                repository.updateSettings { it.copy(appLockEnabled = v) }
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SaraGlassButton(
                            text = "সমস্ত ব্যক্তিগত ডেটা মুছুন · Clear Personal Data",
                            tint = StatusDanger,
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 10. ABOUT SARA
                item {
                    SettingsCategoryCard(
                        icon = Icons.Rounded.Info,
                        titleBn = "১০. সারা সম্পর্কে",
                        subtitleEn = "About SARA"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SaraOrb(
                                state = SaraOrbState.IDLE,
                                onTap = {},
                                orbSize = 58.dp,
                                animationsEnabled = settings.animationsEnabled
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "${settings.assistantName} — ${settings.assistantSubtitle}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                                )
                                Text(
                                    text = "Developer: Flash Break · Native Kotlin + Jetpack Compose",
                                    style = MaterialTheme.typography.labelMedium.copy(color = colors.accentPrimary)
                                )
                            }
                        }

                        HorizontalDivider(color = colors.border)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SaraGlassButton(
                                text = "প্রাইভেসি পলিসি",
                                onClick = { showPrivacyPolicyDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            SaraGlassButton(
                                text = "সাহায্য ও সাপোর্ট",
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showNameDialog) {
        TextEditDialog(
            title = "সহকারীর নাম · Assistant Name",
            initialValue = settings.assistantName,
            label = "Name (e.g. SARA)",
            onDismiss = { showNameDialog = false },
            onSave = { value ->
                repository.updateSettings { it.copy(assistantName = value.ifBlank { "SARA" }) }
                showNameDialog = false
            }
        )
    }

    if (showSubtitleDialog) {
        TextEditDialog(
            title = "সাবটাইটেল · Assistant Subtitle",
            initialValue = settings.assistantSubtitle,
            label = "Subtitle (e.g. Your Personal AI)",
            onDismiss = { showSubtitleDialog = false },
            onSave = { value ->
                repository.updateSettings { it.copy(assistantSubtitle = value.ifBlank { "Your Personal AI" }) }
                showSubtitleDialog = false
            }
        )
    }

    if (showPersonalityDialog) {
        TextEditDialog(
            title = "ব্যক্তিত্বের বিবরণ · Personality",
            initialValue = settings.personalityDescription,
            label = "Personality prompt",
            onDismiss = { showPersonalityDialog = false },
            onSave = { value ->
                repository.updateSettings {
                    it.copy(personalityDescription = value.ifBlank { it.personalityDescription })
                }
                showPersonalityDialog = false
            }
        )
    }

    if (showCloudTtsDialog) {
        TextEditDialog(
            title = "Cloud Neural TTS URL",
            initialValue = settings.cloudTtsEndpointUrl,
            label = "Backend /api/tts HTTPS URL",
            onDismiss = { showCloudTtsDialog = false },
            onSave = { value ->
                repository.updateSettings { it.copy(cloudTtsEndpointUrl = value.trim()) }
                voiceFeedback = if (value.isBlank()) {
                    "Cloud TTS URL পরিষ্কার করা হয়েছে।"
                } else {
                    "Cloud TTS URL সংরক্ষিত হয়েছে।"
                }
                showCloudTtsDialog = false
            }
        )
    }

    if (showWakeWordDialog) {
        TextEditDialog(
            title = "কাস্টম ওয়েক ওয়ার্ড · Custom Wake Word",
            initialValue = settings.customWakeWord,
            label = "Wake phrase",
            onDismiss = { showWakeWordDialog = false },
            onSave = { value ->
                repository.updateSettings { it.copy(customWakeWord = value.ifBlank { "Hey SARA" }) }
                showWakeWordDialog = false
            }
        )
    }

    if (showApiConfigDialog) {
        ApiConfigDialog(
            currentProxyUrl = settings.apiProxyUrl,
            hasStoredKey = settings.hasStoredApiKey,
            onDismiss = { showApiConfigDialog = false },
            onSave = { proxyUrl, newKey ->
                repository.updateSettings { it.copy(apiProxyUrl = proxyUrl.trim()) }
                if (newKey.isNotBlank()) {
                    repository.saveApiKey(newKey)
                }
                apiStatusMessage = "কনফিগারেশন সুরক্ষিতভাবে সংরক্ষিত হয়েছে"
                showApiConfigDialog = false
            },
            onClearKey = {
                repository.clearApiKey()
                apiConnectionState = ApiConnectionState.UNCONFIGURED
                apiStatusMessage = "সংরক্ষিত API কী মুছে ফেলা হয়েছে"
                showApiConfigDialog = false
            }
        )
    }

    if (showAddMemoryDialog) {
        TextEditDialog(
            title = "নতুন মেমোরি যোগ করুন · Add Memory",
            initialValue = "",
            label = "Write a fact or preference to remember",
            onDismiss = { showAddMemoryDialog = false },
            onSave = { value ->
                repository.addMemory(value)
                showAddMemoryDialog = false
            }
        )
    }

    if (showClearHistoryConfirm) {
        ConfirmActionDialog(
            title = "ইতিহাস মুছবেন?",
            message = "সব সংরক্ষিত কথোপকথনের বার্তা স্থায়ীভাবে মুছে যাবে।",
            confirmLabel = "Clear History",
            onDismiss = { showClearHistoryConfirm = false },
            onConfirm = {
                repository.clearConversationHistory()
                showClearHistoryConfirm = false
            }
        )
    }

    if (showClearAllConfirm) {
        ConfirmActionDialog(
            title = "সমস্ত ব্যক্তিগত ডেটা মুছবেন?",
            message = "সব মেমোরি, চ্যাট হিস্টোরি, কাস্টম নাম এবং এনক্রিপ্টেড API কী মুছে ফেলা হবে।",
            confirmLabel = "Erase All",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                repository.clearAllPersonalData()
                apiConnectionState = ApiConnectionState.UNCONFIGURED
                apiStatusMessage = "কনফিগার করা নেই (Cleared)"
                showClearAllConfirm = false
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        InfoModalDialog(
            title = "প্রাইভেসি পলিসি · Privacy Policy",
            body = "SARA আপনার ব্যক্তিগত তথ্য গোপন রাখে। সংরক্ষিত API কী Android Keystore (AES-256-GCM) দিয়ে এনক্রিপ্ট থাকে এবং কখনোই লগ বা স্ক্রিনে দেখানো হয় না। মাইক্রোফোন, কল, এসএমএস, নোটিফিকেশন এবং স্ক্রিন ক্যাপচার কেবল আপনার স্পষ্ট অনুমতির পরই ব্যবহার করা হয়।",
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    if (showHelpDialog) {
        InfoModalDialog(
            title = "সাহায্য ও সাপোর্ট · Help & Support",
            body = "• সত্যিকারের মিষ্টি, স্বাভাবিক ও মানবিক বাংলা নারী কণ্ঠের (Kore / Zephyr) জন্য Voice Settings-এ Cloud Neural TTS URL বা Gemini API Key সেট করুন।\n• অফলাইন Android TTS ব্যবহার করলে ফোনের সর্বোচ্চ মানের বাংলা নারী ভয়েস প্যাক ব্যবহৃত হয়।\n• Accessibility ও Notification Access Android সিস্টেম সেটিংস থেকে চালু করতে হয়।",
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
private fun EditableInfoRow(
    titleBn: String,
    value: String,
    onEdit: () -> Unit
) {
    val colors = LocalSaraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = titleBn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
        }
        Text(
            text = "Edit",
            style = MaterialTheme.typography.labelMedium.copy(
                color = colors.accentPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun MemoryListItemRow(
    item: MemoryItem,
    masked: Boolean,
    onDelete: () -> Unit
) {
    val colors = LocalSaraColors.current
    var revealed by remember(item.id) { mutableStateOf(false) }
    val dateLabel = remember(item.createdAtMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.createdAtMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { revealed = !revealed },
        shape = RoundedCornerShape(12.dp),
        color = colors.canvas,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (masked && !revealed) "•••••••••••••••• (Tap to view)" else item.content,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium.copy(color = colors.textMuted)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Memory",
                    tint = StatusDanger
                )
            }
        }
    }
}

@Composable
private fun TextEditDialog(
    title: String,
    initialValue: String,
    label: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ApiConfigDialog(
    currentProxyUrl: String,
    hasStoredKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClearKey: () -> Unit
) {
    var proxyUrl by remember { mutableStateOf(currentProxyUrl) }
    var secretKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API কনফিগারেশন · Secure Config") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "নিরাপত্তার জন্য API কী কখনোই স্ক্রিনে বা লগে দেখানো হবে না; এটি Android Keystore-এ এনক্রিপ্টেড অবস্থায় থাকবে।",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = proxyUrl,
                    onValueChange = { proxyUrl = it },
                    label = { Text("Backend Proxy URL (Recommended)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = {
                        Text(if (hasStoredKey) "Replace Encrypted API Key" else "Gemini API Key (Encrypted)")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasStoredKey) {
                    TextButton(onClick = onClearKey) {
                        Text("Remove Stored Key", color = StatusDanger)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(proxyUrl, secretKey) }) {
                Text("Save Securely")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = StatusDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InfoModalDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
