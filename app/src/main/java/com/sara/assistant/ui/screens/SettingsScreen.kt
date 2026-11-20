package com.sara.assistant.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
import com.sara.assistant.data.settings.VoiceGenderPreference
import com.sara.assistant.data.settings.VoiceLanguageOption
import com.sara.assistant.data.settings.VoiceProviderMode
import com.sara.assistant.system.AndroidPermissionHelper
import com.sara.assistant.system.BackgroundAssistantState
import com.sara.assistant.system.GeminiConnectionChecker
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.system.SaraWakeService
import com.sara.assistant.system.VoiceEngineStatus
import com.sara.assistant.ui.components.PermissionTone
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

enum class SettingsFeaturePage(
    val titleBn: String,
    val titleEn: String
) {
    HUB("AURA CONTROL", "অরা কন্ট্রোল ও সিস্টেম হাব"),
    BACKGROUND_ASSISTANT("ব্যাকগ্রাউন্ড ও ফ্লোটিং", "Outside-App Assistant"),
    FEMALE_VOICE("মধুর বাংলা নারী কণ্ঠ", "6 Female Voices & TTS"),
    AI_PERSONALITY("এআই ব্যক্তিত্ব ও পরিচয়", "Identity & Style"),
    AI_BRAIN("এআই ব্রেইন ও মডেল", "Gemini Brain & Search"),
    PHONE_CONTROL("ফোন কন্ট্রোল ও পারমিশন", "Overlay, Calls & SMS"),
    MEMORY("মেমোরি ও কথোপকথন", "Saved Notes & History"),
    APPEARANCE("অরা সিগনেচার ও থিম", "Obsidian Aura & Text"),
    NOTIFICATIONS("নোটিফিকেশন ও অ্যালার্ট", "Alerts & Service"),
    PRIVACY_RESET("প্রাইভেসি ও রিসেট সেটিংস", "Security, Lock & Reset"),
    ABOUT("সারা সম্পর্কে", "About SARA & Help")
}

/**
 * Obsidian Aura Control Settings Hub.
 * Visual Aura Signature switcher cards + matte dark gradient Feature Bento grid opening dedicated sub-pages.
 */
@Composable
fun SettingsScreen(
    settings: SaraSettings,
    memories: List<MemoryItem>,
    conversationHistory: List<ConversationEntry>,
    inboxUnreadCount: Int,
    voiceStatus: VoiceEngineStatus,
    backgroundState: BackgroundAssistantState,
    isSpeaking: Boolean,
    repository: SaraSettingsRepository,
    voiceEngine: SaraVoiceEngine,
    initialPage: SettingsFeaturePage = SettingsFeaturePage.HUB,
    onBack: () -> Unit,
    onOpenInbox: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSaraColors.current
    val scope = rememberCoroutineScope()

    var currentPage by remember(initialPage) { mutableStateOf(initialPage) }

    BackHandler(enabled = currentPage != SettingsFeaturePage.HUB) {
        currentPage = SettingsFeaturePage.HUB
    }

    var permissionSnapshot by remember { mutableStateOf(AndroidPermissionHelper.captureSnapshot(context)) }
    val refreshPermissions = {
        val snap = AndroidPermissionHelper.captureSnapshot(context)
        permissionSnapshot = snap
        repository.updatePermissionStateSnapshot(
            micGranted = snap.microphoneGranted,
            overlayGranted = snap.overlayPermissionGranted,
            batteryUnrestricted = snap.batteryOptimizationIgnored,
            serviceActive = backgroundState.serviceRunning
        )
    }

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
    var showResetSettingsConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var voiceFeedback by remember { mutableStateOf<String?>(null) }
    var wakeFeedback by remember { mutableStateOf<String?>(null) }
    var resetFeedback by remember { mutableStateOf<String?>(null) }
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

    val grantedCount = listOf(
        permissionSnapshot.microphoneGranted,
        permissionSnapshot.overlayPermissionGranted,
        permissionSnapshot.callPhoneGranted,
        permissionSnapshot.sendSmsGranted,
        permissionSnapshot.notificationsGranted,
        permissionSnapshot.accessibilityEnabled
    ).count { it }

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
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Obsidian Aura Control Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.96f))
                    .border(BorderStroke(1.dp, colors.border))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaraIconGlowButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = {
                        if (currentPage == SettingsFeaturePage.HUB) {
                            onBack()
                        } else {
                            currentPage = SettingsFeaturePage.HUB
                        }
                    },
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentPage.titleBn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = currentPage.titleEn,
                        style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                    )
                }
                SaraGlassButton(
                    text = if (inboxUnreadCount > 0) "ইনবক্স ($inboxUnreadCount)" else "ইনবক্স",
                    icon = Icons.Rounded.Inbox,
                    onClick = onOpenInbox
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (currentPage) {
                    SettingsFeaturePage.HUB -> {
                        // Visual Aura Signature Selector Card (matching Screenshot 3 "Visual Identity")
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.34f))
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.accentPrimary.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Aura Signature",
                                                tint = colors.accentPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "অরা সিগনেচার · Aura Signature",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Current: ${settings.accentColor.labelEn} · Tap to switch",
                                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AccentColorOption.entries.forEach { accent ->
                                            val selected = accent == settings.accentColor
                                            val swatchColor = Color(accent.primaryHex)
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (selected) swatchColor.copy(alpha = 0.16f) else colors.canvas.copy(alpha = 0.72f)
                                                    )
                                                    .border(
                                                        width = if (selected) 1.5.dp else 1.dp,
                                                        color = if (selected) swatchColor else colors.border,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        repository.updateSettings { it.copy(accentColor = accent) }
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(swatchColor)
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = accent.labelEn.split(" ").first(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (selected) swatchColor else colors.textSecondary,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Row 1: Outside-App Background Assistant + 6 Female Voices
                        item {
                            FeatureHubRow(
                                left = FeatureHubTileData(
                                    icon = Icons.Rounded.GraphicEq,
                                    titleBn = "ব্যাকগ্রাউন্ড ও ফ্লোটিং",
                                    subtitleEn = "Outside-App AI",
                                    statusText = when {
                                        backgroundState.serviceRunning && backgroundState.isPaused -> "বিরতি · Paused"
                                        backgroundState.serviceRunning -> "সক্রিয় · ${settings.customWakeWord}"
                                        else -> "বন্ধ · চালু করতে ট্যাপ করুন"
                                    },
                                    highlight = backgroundState.serviceRunning,
                                    onClick = { currentPage = SettingsFeaturePage.BACKGROUND_ASSISTANT }
                                ),
                                right = FeatureHubTileData(
                                    icon = Icons.Rounded.RecordVoiceOver,
                                    titleBn = "সুন্দর নারী কণ্ঠ (৬টি)",
                                    subtitleEn = "Female Voices & TTS",
                                    statusText = "${settings.femaleVoicePersona.voiceId} · ${settings.voiceLanguage.labelBn}",
                                    highlight = settings.voiceResponsesEnabled,
                                    onClick = { currentPage = SettingsFeaturePage.FEMALE_VOICE }
                                )
                            )
                        }

                        // Row 2: SARA Inbox + AI Personality
                        item {
                            FeatureHubRow(
                                left = FeatureHubTileData(
                                    icon = Icons.Rounded.Inbox,
                                    titleBn = "সারা ইনবক্স",
                                    subtitleEn = "SARA Inbox",
                                    statusText = if (inboxUnreadCount > 0) "$inboxUnreadCount টি অপঠিত" else "সব পড়া হয়েছে",
                                    highlight = inboxUnreadCount > 0,
                                    onClick = onOpenInbox
                                ),
                                right = FeatureHubTileData(
                                    icon = Icons.Rounded.AutoAwesome,
                                    titleBn = "এআই ব্যক্তিত্ব",
                                    subtitleEn = "Identity & Style",
                                    statusText = "${settings.assistantName} · ${settings.personalityMode.labelBn}",
                                    onClick = { currentPage = SettingsFeaturePage.AI_PERSONALITY }
                                )
                            )
                        }

                        // Row 3: AI Brain + Phone Control & Permissions
                        item {
                            FeatureHubRow(
                                left = FeatureHubTileData(
                                    icon = Icons.Rounded.Psychology,
                                    titleBn = "এআই ব্রেইন ও এপিআই",
                                    subtitleEn = "Gemini Model & Search",
                                    statusText = settings.selectedModel.title.replace("Gemini ", "G"),
                                    onClick = { currentPage = SettingsFeaturePage.AI_BRAIN }
                                ),
                                right = FeatureHubTileData(
                                    icon = Icons.Rounded.PhoneAndroid,
                                    titleBn = "ফোন ও ওভারলে পারমিশন",
                                    subtitleEn = "Phone Control & Overlay",
                                    statusText = "$grantedCount / 6 অনুমোদিত",
                                    onClick = { currentPage = SettingsFeaturePage.PHONE_CONTROL }
                                )
                            )
                        }

                        // Row 4: Memory + Appearance (Light)
                        item {
                            FeatureHubRow(
                                left = FeatureHubTileData(
                                    icon = Icons.Rounded.Storage,
                                    titleBn = "মেমোরি ও ইতিহাস",
                                    subtitleEn = "Memory & History",
                                    statusText = "${memories.size} নোট · ${conversationHistory.size} মেসেজ",
                                    onClick = { currentPage = SettingsFeaturePage.MEMORY }
                                ),
                                right = FeatureHubTileData(
                                    icon = Icons.Rounded.Palette,
                                    titleBn = "অরা ও টেক্সট",
                                    subtitleEn = "Obsidian Aura & Font",
                                    statusText = "${settings.accentColor.labelEn.split(" ").first()} · ${settings.textSize.labelBn}",
                                    onClick = { currentPage = SettingsFeaturePage.APPEARANCE }
                                )
                            )
                        }

                        // Row 5: Notifications + Privacy & Reset Settings
                        item {
                            FeatureHubRow(
                                left = FeatureHubTileData(
                                    icon = Icons.Rounded.Notifications,
                                    titleBn = "নোটিফিকেশন",
                                    subtitleEn = "Alerts & Service Bar",
                                    statusText = if (settings.assistantNotificationsEnabled) "চালু (Active)" else "বন্ধ",
                                    onClick = { currentPage = SettingsFeaturePage.NOTIFICATIONS }
                                ),
                                right = FeatureHubTileData(
                                    icon = Icons.Rounded.Security,
                                    titleBn = "প্রাইভেসি ও রিসেট",
                                    subtitleEn = "Security & Reset Settings",
                                    statusText = if (settings.appLockEnabled) "লক সক্রিয়" else "সেটিংস রিসেট",
                                    onClick = { currentPage = SettingsFeaturePage.PRIVACY_RESET }
                                )
                            )
                        }

                        // Row 6: About & Help full-width card
                        item {
                            FeatureHubWideCard(
                                icon = Icons.Rounded.Info,
                                titleBn = "সারা সম্পর্কে ও সহায়তা · About SARA",
                                subtitleEn = "Version ${BuildConfig.VERSION_NAME} · Privacy Policy · Quick Guide",
                                onClick = { currentPage = SettingsFeaturePage.ABOUT }
                            )
                        }
                    }

                    // 1. OUTSIDE-THE-APP BACKGROUND ASSISTANT & FLOATING OVERLAY
                    SettingsFeaturePage.BACKGROUND_ASSISTANT -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.GraphicEq,
                                titleBn = "হোম স্ক্রিন ও ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট",
                                subtitleEn = "Work outside app + Wake Word + Floating Overlay"
                            ) {
                                SettingsSwitchRow(
                                    titleBn = "\"${settings.customWakeWord}\" ব্যাকগ্রাউন্ড লিসেনিং",
                                    subtitleEn = if (permissionSnapshot.speechRecognitionAvailable) {
                                        "Listen even when you return to Android Home Screen"
                                    } else {
                                        "Speech recognizer unavailable on this device"
                                    },
                                    checked = settings.wakeWordEnabled,
                                    enabled = permissionSnapshot.speechRecognitionAvailable,
                                    onCheckedChange = { enable ->
                                        if (enable && !permissionSnapshot.microphoneGranted) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            wakeFeedback = "প্রথমে মাইক্রোফোন অনুমতি দিন।"
                                        } else if (enable) {
                                            val started = SaraWakeService.start(
                                                context,
                                                settings.customWakeWord,
                                                startPaused = false
                                            )
                                            repository.updateSettings {
                                                it.copy(wakeWordEnabled = started, wakeListeningPaused = false)
                                            }
                                            wakeFeedback = if (started) {
                                                "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট চালু হয়েছে!"
                                            } else {
                                                "সার্ভিস চালু করা যায়নি।"
                                            }
                                        } else {
                                            repository.updateSettings {
                                                it.copy(wakeWordEnabled = false, wakeListeningPaused = false)
                                            }
                                            SaraWakeService.stop(context)
                                            wakeFeedback = "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট বন্ধ হয়েছে।"
                                        }
                                    }
                                )

                                HorizontalDivider(color = colors.border)

                                SettingsSwitchRow(
                                    titleBn = "ফ্লোটিং SARA বাটন ও পপআপ কার্ড",
                                    subtitleEn = "Show floating assistant bubble outside the app (needs Overlay permission)",
                                    checked = settings.floatingOverlayEnabled,
                                    onCheckedChange = { enabled ->
                                        repository.updateSettings { it.copy(floatingOverlayEnabled = enabled) }
                                        if (enabled && !permissionSnapshot.overlayPermissionGranted) {
                                            AndroidPermissionHelper.openOverlayPermissionSettings(context)
                                            wakeFeedback = "ফ্লোটিং বাটনের জন্য Display Over Other Apps অনুমতি দিন।"
                                        } else {
                                            SaraWakeService.refreshOverlay(context)
                                        }
                                    }
                                )

                                SettingsSwitchRow(
                                    titleBn = "ফোন রিস্টার্টের পর স্বয়ংক্রিয়ভাবে চালু",
                                    subtitleEn = "Restore assistant after reboot (Android 15 posts tap-to-resume alert if restricted)",
                                    checked = settings.autoRestoreOnBoot,
                                    onCheckedChange = { enabled ->
                                        repository.updateSettings { it.copy(autoRestoreOnBoot = enabled) }
                                    }
                                )

                                HorizontalDivider(color = colors.border)

                                EditableInfoRow(
                                    titleBn = "কাস্টম ওয়েক ওয়ার্ড · Wake Phrase",
                                    value = settings.customWakeWord,
                                    onEdit = { showWakeWordDialog = true }
                                )

                                HorizontalDivider(color = colors.border)

                                SettingsPermissionRow(
                                    titleBn = "মাইক্রোফোন অনুমতি · Microphone",
                                    subtitleEn = "Required for \"Hey SARA\" and voice talk",
                                    statusLabel = if (permissionSnapshot.microphoneGranted) "অনুমোদিত (Granted)" else "অনুমতি প্রয়োজন",
                                    tone = if (permissionSnapshot.microphoneGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = if (permissionSnapshot.microphoneGranted) "App Info" else "Grant",
                                    onAction = {
                                        if (permissionSnapshot.microphoneGranted) {
                                            AndroidPermissionHelper.openAppSettings(context)
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                )

                                SettingsPermissionRow(
                                    titleBn = "ওভারলে অনুমতি · Floating Overlay",
                                    subtitleEn = "SYSTEM_ALERT_WINDOW (Requested separately)",
                                    statusLabel = if (permissionSnapshot.overlayPermissionGranted) "অনুমোদিত (Overlay Ready)" else "অনুমতি প্রয়োজন",
                                    tone = if (permissionSnapshot.overlayPermissionGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = if (permissionSnapshot.overlayPermissionGranted) "Manage" else "Grant Overlay",
                                    onAction = {
                                        AndroidPermissionHelper.openOverlayPermissionSettings(context)
                                        wakeFeedback = "ফিরে এসে Refresh বা Start চাপুন।"
                                    }
                                )

                                SettingsPermissionRow(
                                    titleBn = "ব্যাটারি অপটিমাইজেশন",
                                    subtitleEn = "Keep foreground service alive when leaving the app",
                                    statusLabel = if (permissionSnapshot.batteryOptimizationIgnored) "Unrestricted" else "Optimized (সীমিত)",
                                    tone = if (permissionSnapshot.batteryOptimizationIgnored) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = "Configure",
                                    onAction = {
                                        AndroidPermissionHelper.openBatteryOptimizationSettings(context)
                                        wakeFeedback = "SARA-কে Unrestricted হিসেবে সেট করুন।"
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.elevated,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "অবস্থা: ${backgroundState.statusMessage}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = if (backgroundState.serviceRunning && !backgroundState.isPaused) StatusNominal else colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        if (!backgroundState.serviceRunning) {
                                            SaraPrimaryGradientButton(
                                                text = "ব্যাকগ্রাউন্ড সার্ভিস চালু করুন · Start",
                                                icon = Icons.Rounded.PlayArrow,
                                                onClick = {
                                                    if (!permissionSnapshot.microphoneGranted) {
                                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        wakeFeedback = "সার্ভিস চালু করতে মাইক্রোফোন অনুমতি দিন।"
                                                    } else {
                                                        val ok = SaraWakeService.start(context, settings.customWakeWord, startPaused = false)
                                                        if (ok) {
                                                            repository.updateSettings {
                                                                it.copy(wakeWordEnabled = true, wakeListeningPaused = false)
                                                            }
                                                            wakeFeedback = "ব্যাকগ্রাউন্ড ও ফ্লোটিং অ্যাসিস্ট্যান্ট চালু হয়েছে।"
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SaraPrimaryGradientButton(
                                                    text = "Talk",
                                                    icon = Icons.Rounded.RecordVoiceOver,
                                                    onClick = { SaraWakeService.talkNow(context, settings.customWakeWord) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                SaraGlassButton(
                                                    text = if (backgroundState.isPaused) "Resume" else "Pause",
                                                    icon = if (backgroundState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                                    onClick = {
                                                        if (backgroundState.isPaused) {
                                                            SaraWakeService.resume(context)
                                                        } else {
                                                            SaraWakeService.pause(context)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                SaraGlassButton(
                                                    text = "Stop",
                                                    icon = Icons.Rounded.Stop,
                                                    tint = StatusDanger,
                                                    onClick = {
                                                        SaraWakeService.stop(context)
                                                        wakeFeedback = "সার্ভিস বন্ধ করা হয়েছে।"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (wakeFeedback != null) {
                                    Text(
                                        text = wakeFeedback.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.accentPrimary),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. MULTIPLE BEAUTIFUL FEMALE VOICES & VOICE SETTINGS
                    SettingsFeaturePage.FEMALE_VOICE -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.RecordVoiceOver,
                                titleBn = "স্বাভাবিক বাংলা নারী কণ্ঠ (৬টি কণ্ঠ)",
                                subtitleEn = "Natural Female Voices, Prosody & Live Previews"
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
                                    text = "ভয়েস ইঞ্জিন · Voice Engine",
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
                                        voiceStatus.femaleVoiceAvailable -> "Female"
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

                                Spacer(modifier = Modifier.height(8.dp))
                                EditableInfoRow(
                                    titleBn = "ক্লাউড নিউরাল TTS URL",
                                    value = settings.cloudTtsEndpointUrl.ifBlank { "Backend /api/tts URL অথবা Gemini API Key ব্যবহার করুন" },
                                    onEdit = { showCloudTtsDialog = true }
                                )

                                HorizontalDivider(color = colors.border)
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "সুন্দর নারী কণ্ঠ বেছে নিন ও শুনুন (৬টি কণ্ঠ)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Select & preview any of SARA's 6 natural female voices",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                FemaleVoicePersona.entries.forEach { persona ->
                                    val isSelected = persona == settings.femaleVoicePersona
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                repository.updateSettings {
                                                    it.copy(
                                                        femaleVoicePersona = persona,
                                                        voiceGender = VoiceGenderPreference.FEMALE
                                                    )
                                                }
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) colors.elevated else colors.canvas,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) colors.accentPrimary else colors.border
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isSelected) colors.accentPrimary else colors.border,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = persona.labelBn,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = colors.textPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = persona.labelEn,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = colors.accentPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Text(
                                                    text = persona.descriptionBn,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            SaraGlassButton(
                                                text = "শুনুন",
                                                icon = Icons.Rounded.VolumeUp,
                                                onClick = {
                                                    repository.updateSettings {
                                                        it.copy(
                                                            femaleVoicePersona = persona,
                                                            voiceGender = VoiceGenderPreference.FEMALE
                                                        )
                                                    }
                                                    voiceFeedback = "${persona.labelBn} প্রিভিউ চলছে..."
                                                    voiceEngine.previewPersona(persona, settings) { result ->
                                                        voiceFeedback = "${result.providerUsed}: ${result.message}"
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))

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
                                        pref to "${pref.labelBn} · ${pref.labelEn}"
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
                                    titleBn = "কণ্ঠের পিচ",
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
                                        text = if (isSpeaking) "চলছে..." else "ভয়েস পরীক্ষা করুন",
                                        icon = Icons.Rounded.VolumeUp,
                                        onClick = {
                                            voiceFeedback = "কণ্ঠ প্রস্তুত হচ্ছে..."
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
                    }

                    // 3. AI PERSONALITY
                    SettingsFeaturePage.AI_PERSONALITY -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.AutoAwesome,
                                titleBn = "এআই ব্যক্তিত্ব ও পরিচয়",
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
                    }

                    // 4. AI BRAIN
                    SettingsFeaturePage.AI_BRAIN -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Psychology,
                                titleBn = "এআই ব্রেইন ও কনফিগারেশন",
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
                                        if (settings.apiProxyUrl.isNotBlank()) append(" · Proxy set")
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
                    }

                    // 5. PHONE CONTROL & PERMISSIONS
                    SettingsFeaturePage.PHONE_CONTROL -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.PhoneAndroid,
                                titleBn = "ফোন কন্ট্রোল ও অ্যান্ড্রয়েড পারমিশন",
                                subtitleEn = "Phone Control, Overlay & System Permissions"
                            ) {
                                SettingsPermissionRow(
                                    titleBn = "ফ্লোটিং ওভারলে অনুমতি",
                                    subtitleEn = "SYSTEM_ALERT_WINDOW (Display over home screen & apps)",
                                    statusLabel = if (permissionSnapshot.overlayPermissionGranted) "অনুমোদিত (Granted)" else "অনুমতি প্রয়োজন",
                                    tone = if (permissionSnapshot.overlayPermissionGranted) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = if (permissionSnapshot.overlayPermissionGranted) "Manage" else "Grant Overlay",
                                    onAction = { AndroidPermissionHelper.openOverlayPermissionSettings(context) }
                                )
                                SettingsPermissionRow(
                                    titleBn = "অ্যাপ লঞ্চিং ক্ষমতা",
                                    subtitleEn = "Open installed applications",
                                    statusLabel = if (permissionSnapshot.appLaunchingSupported) "প্রস্তুত (Ready)" else "সাপোর্ট নেই",
                                    tone = if (permissionSnapshot.appLaunchingSupported) PermissionTone.GRANTED else PermissionTone.UNAVAILABLE,
                                    actionLabel = "Manage",
                                    onAction = { AndroidPermissionHelper.openAppSettings(context) }
                                )
                                SettingsPermissionRow(
                                    titleBn = "ফোন কল অনুমতি",
                                    subtitleEn = "android.permission.CALL_PHONE",
                                    statusLabel = if (permissionSnapshot.callPhoneGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই",
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
                                    statusLabel = if (permissionSnapshot.sendSmsGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই",
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
                                    statusLabel = if (permissionSnapshot.accessibilityEnabled) "সক্রিয় (Enabled)" else "নিষ্ক্রিয়",
                                    tone = if (permissionSnapshot.accessibilityEnabled) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = "Open Settings",
                                    onAction = {
                                        systemSettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    }
                                )
                                SettingsPermissionRow(
                                    titleBn = "নোটিফিকেশন অ্যাক্সেস",
                                    subtitleEn = "Notification Listener access",
                                    statusLabel = if (permissionSnapshot.notificationAccessEnabled) "সক্রিয় (Enabled)" else "নিষ্ক্রিয়",
                                    tone = if (permissionSnapshot.notificationAccessEnabled) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = "Open Settings",
                                    onAction = {
                                        systemSettingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    }
                                )
                                SettingsPermissionRow(
                                    titleBn = "স্ক্রিন ক্যাপচার অনুমতি",
                                    subtitleEn = "MediaProjection (Per-session consent required by Android)",
                                    statusLabel = if (permissionSnapshot.screenCaptureSupported) "প্রতিরক্ষিত (Consent per capture)" else "Unavailable",
                                    tone = if (permissionSnapshot.screenCaptureSupported) PermissionTone.GRANTED else PermissionTone.UNAVAILABLE,
                                    actionLabel = "Refresh",
                                    onAction = refreshPermissions
                                )
                            }
                        }
                    }

                    // 6. MEMORY
                    SettingsFeaturePage.MEMORY -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Storage,
                                titleBn = "মেমোরি ও কথোপকথনের ইতিহাস",
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
                                        text = "এখনো কোনো মেমোরি সংরক্ষিত নেই।",
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
                                    text = "কথোপকথনের ইতিহাস মুছুন · Clear (${conversationHistory.size})",
                                    enabled = conversationHistory.isNotEmpty(),
                                    tint = StatusDanger,
                                    onClick = { showClearHistoryConfirm = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 7. APPEARANCE (OBSIDIAN AURA THEME)
                    SettingsFeaturePage.APPEARANCE -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Palette,
                                titleBn = "অরা সিগনেচার ও থিম",
                                subtitleEn = "Obsidian Aura Theme, Accent, Animations & Text Size"
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.elevated,
                                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.38f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Obsidian Aura Engine সক্রিয় · ${settings.accentColor.labelEn}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = colors.accentPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "প্রিমিয়াম ডার্ক অবসিডিয়ান সারফেস ও আপনার পছন্দের অরা গ্লো স্থায়ীভাবে DataStore-এ সেভ থাকবে।",
                                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
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
                                                .background(if (selected) swatchColor.copy(alpha = 0.14f) else colors.canvas)
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
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                    }

                    // 8. NOTIFICATIONS
                    SettingsFeaturePage.NOTIFICATIONS -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Notifications,
                                titleBn = "নোটিফিকেশন সেটিংস",
                                subtitleEn = "Assistant, Voice & Background Service Alerts"
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    SettingsPermissionRow(
                                        titleBn = "অ্যান্ড্রয়েড নোটিফিকেশন অনুমতি",
                                        subtitleEn = "POST_NOTIFICATIONS (Android 13+)",
                                        statusLabel = if (permissionSnapshot.notificationsGranted) "অনুমোদিত (Granted)" else "অনুমতি নেই",
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
                                    subtitleEn = "Show persistent notification when Outside-App service runs",
                                    checked = settings.backgroundServiceNotificationEnabled,
                                    onCheckedChange = { v ->
                                        repository.updateSettings { it.copy(backgroundServiceNotificationEnabled = v) }
                                    }
                                )
                            }
                        }
                    }

                    // 9. PRIVACY & EXPLICIT RESET SETTINGS
                    SettingsFeaturePage.PRIVACY_RESET -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Security,
                                titleBn = "প্রাইভেসি, সিকিউরিটি ও রিসেট",
                                subtitleEn = "Permissions, Privacy, App Lock & Reset Settings"
                            ) {
                                SettingsPermissionRow(
                                    titleBn = "অ্যাপ পারমিশন সারাংশ",
                                    subtitleEn = "Active runtime, overlay & system permissions overview",
                                    statusLabel = "$grantedCount / 6 অনুমোদিত",
                                    tone = if (grantedCount > 0) PermissionTone.GRANTED else PermissionTone.WARNING,
                                    actionLabel = "OS Settings",
                                    onAction = { AndroidPermissionHelper.openAppSettings(context) }
                                )
                                SettingsSwitchRow(
                                    titleBn = "স্ক্রিন লক থাকলে মাইক্রোফোন বিরতি",
                                    subtitleEn = "Automatically mute background wake mic when screen is locked",
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
                                        "Set a phone screen lock in Android Settings first"
                                    },
                                    checked = settings.appLockEnabled && permissionSnapshot.deviceLockSupported,
                                    enabled = permissionSnapshot.deviceLockSupported,
                                    onCheckedChange = { v ->
                                        repository.updateSettings { it.copy(appLockEnabled = v) }
                                    }
                                )

                                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))

                                // Separate Explicit Reset Settings Control
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.elevated,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "সেটিংস রিসেট · Reset Settings",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "আপনার সব সেটিংস DataStore-এ নিরাপদে সেভ থাকে। কেবল আপনি নিজে এই বাটনে ট্যাপ করলেই ডিফল্টে রিসেট হবে।",
                                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SaraGlassButton(
                                            text = "ডিফল্ট সেটিংস রিসেট করুন · Reset Settings",
                                            icon = Icons.Rounded.RestartAlt,
                                            tint = StatusAmber,
                                            onClick = { showResetSettingsConfirm = true },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                SaraGlassButton(
                                    text = "সমস্ত ব্যক্তিগত ডেটা মুছুন · Clear Personal Data",
                                    tint = StatusDanger,
                                    onClick = { showClearAllConfirm = true },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (resetFeedback != null) {
                                    Text(
                                        text = resetFeedback.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.accentPrimary),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 10. ABOUT SARA
                    SettingsFeaturePage.ABOUT -> {
                        item {
                            SettingsCategoryCard(
                                icon = Icons.Rounded.Info,
                                titleBn = "সারা সম্পর্কে · About SARA",
                                subtitleEn = "Application Version & Support"
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
                                            text = "Developer: Flash Break · Native Kotlin + Jetpack DataStore",
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

    if (showResetSettingsConfirm) {
        ConfirmActionDialog(
            title = "সেটিংস ডিফল্টে রিসেট করবেন?",
            message = "ভয়েস, ওয়েক ওয়ার্ড, অ্যাকসেন্ট রঙ ও এআই সেটিংস ডিফল্টে ফিরে যাবে। সংরক্ষিত মেমোরি বা ইনবক্স মুছবে না।",
            confirmLabel = "Reset Settings",
            onDismiss = { showResetSettingsConfirm = false },
            onConfirm = {
                SaraWakeService.stop(context)
                repository.resetSettingsToDefaults()
                resetFeedback = "সব সেটিংস সফলভাবে ডিফল্টে রিসেট হয়েছে।"
                showResetSettingsConfirm = false
            }
        )
    }

    if (showClearAllConfirm) {
        ConfirmActionDialog(
            title = "সমস্ত ব্যক্তিগত ডেটা মুছবেন?",
            message = "সব মেমোরি, চ্যাট হিস্টোরি, ইনবক্স এবং এনক্রিপ্টেড API কী মুছে ফেলা হবে।",
            confirmLabel = "Erase All",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                repository.clearAllPersonalData()
                apiConnectionState = ApiConnectionState.UNCONFIGURED
                apiStatusMessage = "কনফিগার করা নেই (Cleared)"
                resetFeedback = "সমস্ত ব্যক্তিগত ডেটা মুছে ফেলা হয়েছে।"
                showClearAllConfirm = false
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        InfoModalDialog(
            title = "প্রাইভেসি পলিসি · Privacy Policy",
            body = "SARA আপনার ব্যক্তিগত তথ্য গোপন রাখে। API কী Android Keystore (AES-256-GCM)-এ এনক্রিপ্ট থাকে। মাইক্রোফোন, ওভারলে, কল, এসএমএস ও নোটিফিকেশন কেবল আপনার অনুমতির পরই ব্যবহৃত হয় এবং কখনোই গোপনে অডিও রেকর্ড করা হয় না।",
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    if (showHelpDialog) {
        InfoModalDialog(
            title = "সাহায্য ও সাপোর্ট · Help & Support",
            body = "• হোম স্ক্রিন থেকে SARA ব্যবহার করতে 'ব্যাকগ্রাউন্ড ও ফ্লোটিং' পেজ থেকে মাইক্রোফোন ও ওভারলে অনুমতি দিয়ে Start চাপুন।\n• সুন্দর ৬টি বাংলা নারী কণ্ঠের (Kore, Zephyr, Aoede, Leda, SaraSweet, SaraVelvet) জন্য 'সুন্দর নারী কণ্ঠ' পেজে প্রতিটি কণ্ঠ শুনুন।\n• সমস্ত সেটিংস DataStore-এ স্থায়ীভাবে সেভ থাকে।",
            onDismiss = { showHelpDialog = false }
        )
    }
}

private data class FeatureHubTileData(
    val icon: ImageVector,
    val titleBn: String,
    val subtitleEn: String,
    val statusText: String,
    val highlight: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun FeatureHubRow(
    left: FeatureHubTileData,
    right: FeatureHubTileData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureHubTile(data = left, modifier = Modifier.weight(1f))
        FeatureHubTile(data = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeatureHubTile(
    data: FeatureHubTileData,
    modifier: Modifier = Modifier
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (data.highlight) 8.dp else 3.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = colors.accentPrimary.copy(alpha = 0.22f),
                spotColor = colors.accentPrimary.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = data.onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = if (data.highlight) 1.4.dp else 1.dp,
            color = if (data.highlight) colors.accentPrimary.copy(alpha = 0.55f) else colors.border
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
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.16f))
                    .border(1.dp, colors.accentPrimary.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.titleBn,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = data.titleBn,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = data.subtitleEn,
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = data.statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeatureHubWideCard(
    icon: ImageVector,
    titleBn: String,
    subtitleEn: String,
    onClick: () -> Unit
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border)
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titleBn,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleBn,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitleEn,
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                )
            }
        }
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
            TextButton(onClick = { onSave(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            TextButton(onClick = { onSave(proxyUrl, secretKey) }) { Text("Save Securely") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
