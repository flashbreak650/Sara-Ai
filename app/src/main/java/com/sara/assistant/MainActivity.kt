package com.sara.assistant

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sara.assistant.data.settings.InboxCategory
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.system.AndroidPermissionHelper
import com.sara.assistant.system.SaraAssistantBrain
import com.sara.assistant.system.SaraBootReceiver
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.system.SaraWakeService
import com.sara.assistant.ui.components.SaraAvatarFrame
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.screens.HomeScreen
import com.sara.assistant.ui.screens.InboxScreen
import com.sara.assistant.ui.screens.SettingsFeaturePage
import com.sara.assistant.ui.screens.SettingsScreen
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.SaraTheme

private enum class SaraDestination {
    HOME,
    INBOX,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: SaraSettingsRepository
    private lateinit var voiceEngine: SaraVoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SaraSettingsRepository.getInstance(applicationContext)
        voiceEngine = SaraVoiceEngine(applicationContext, repository.secureKeyStorage)

        setContent {
            val settings by repository.settings.collectAsState()
            val memories by repository.memories.collectAsState()
            val conversationHistory by repository.conversationHistory.collectAsState()
            val inboxItems by repository.inboxItems.collectAsState()
            val voiceStatus by voiceEngine.status.collectAsState()
            val isSpeaking by voiceEngine.isSpeaking.collectAsState()
            val backgroundState by SaraWakeService.assistantState.collectAsState()

            var destination by remember { mutableStateOf(SaraDestination.HOME) }
            var settingsInitialPage by remember { mutableStateOf(SettingsFeaturePage.HUB) }
            var unlocked by remember { mutableStateOf(!settings.appLockEnabled) }

            val unreadInboxCount = remember(inboxItems) { inboxItems.count { !it.isRead } }

            val micLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
                repository.updatePermissionStateSnapshot(
                    micGranted = snap.microphoneGranted,
                    overlayGranted = snap.overlayPermissionGranted,
                    batteryUnrestricted = snap.batteryOptimizationIgnored,
                    serviceActive = SaraWakeService.isRunning.value
                )
                if (granted) {
                    val started = SaraWakeService.start(
                        applicationContext,
                        settings.customWakeWord,
                        startPaused = false
                    )
                    if (started) {
                        repository.updateSettings {
                            it.copy(wakeWordEnabled = true, wakeListeningPaused = false)
                        }
                    }
                }
            }

            // Capture permission snapshot & restore persistent background assistant state on app start
            LaunchedEffect(Unit) {
                val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
                val fromBootNotif = intent?.getBooleanExtra(SaraBootReceiver.EXTRA_RESUME_WAKE_FROM_BOOT, false) == true
                if ((settings.wakeWordEnabled || fromBootNotif) && snap.microphoneGranted) {
                    SaraWakeService.start(
                        context = applicationContext,
                        wakeWord = settings.customWakeWord,
                        startPaused = if (fromBootNotif) false else settings.wakeListeningPaused
                    )
                }
                repository.updatePermissionStateSnapshot(
                    micGranted = snap.microphoneGranted,
                    overlayGranted = snap.overlayPermissionGranted,
                    batteryUnrestricted = snap.batteryOptimizationIgnored,
                    serviceActive = SaraWakeService.isRunning.value
                )
            }

            val keyguardManager = remember {
                getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            }
            val unlockLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    unlocked = true
                }
            }

            LaunchedEffect(settings.appLockEnabled) {
                if (settings.appLockEnabled && !unlocked && keyguardManager?.isDeviceSecure == true) {
                    @Suppress("DEPRECATION")
                    val authIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                        "${settings.assistantName} Security Lock",
                        "Unlock ${settings.assistantName} with your device PIN or biometric"
                    )
                    if (authIntent != null) {
                        unlockLauncher.launch(authIntent)
                    } else {
                        unlocked = true
                    }
                } else if (!settings.appLockEnabled) {
                    unlocked = true
                }
            }

            SaraTheme(
                accentColor = settings.accentColor,
                textSize = settings.textSize
            ) {
                if (!unlocked) {
                    AppLockGateScreen(
                        assistantName = settings.assistantName,
                        onUnlockRequest = {
                            @Suppress("DEPRECATION")
                            val authIntent = keyguardManager?.createConfirmDeviceCredentialIntent(
                                "${settings.assistantName} Security Lock",
                                "Unlock ${settings.assistantName}"
                            )
                            if (authIntent != null) {
                                unlockLauncher.launch(authIntent)
                            } else {
                                unlocked = true
                            }
                        }
                    )
                } else {
                    Crossfade(
                        targetState = destination,
                        animationSpec = tween(durationMillis = if (settings.animationsEnabled) 240 else 0),
                        label = "sara_nav_crossfade"
                    ) { screen ->
                        when (screen) {
                            SaraDestination.HOME -> {
                                HomeScreen(
                                    settings = settings,
                                    memories = memories,
                                    conversationHistory = conversationHistory,
                                    inboxUnreadCount = unreadInboxCount,
                                    isSpeaking = isSpeaking,
                                    backgroundState = backgroundState,
                                    onOpenInbox = { destination = SaraDestination.INBOX },
                                    onOpenSettings = {
                                        settingsInitialPage = SettingsFeaturePage.HUB
                                        destination = SaraDestination.SETTINGS
                                    },
                                    onOpenFemaleVoices = {
                                        settingsInitialPage = SettingsFeaturePage.FEMALE_VOICE
                                        destination = SaraDestination.SETTINGS
                                    },
                                    onOpenBackgroundSettings = {
                                        settingsInitialPage = SettingsFeaturePage.BACKGROUND_ASSISTANT
                                        destination = SaraDestination.SETTINGS
                                    },
                                    onStartWakeService = {
                                        val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
                                        if (!snap.microphoneGranted) {
                                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            val started = SaraWakeService.start(
                                                applicationContext,
                                                settings.customWakeWord,
                                                startPaused = false
                                            )
                                            if (started) {
                                                repository.updateSettings {
                                                    it.copy(wakeWordEnabled = true, wakeListeningPaused = false)
                                                }
                                            }
                                        }
                                    },
                                    onToggleContinuousVoice = {
                                        voiceEngine.stopPlayback()
                                        val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
                                        if (!snap.microphoneGranted) {
                                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            SaraWakeService.toggleContinuousVoice(
                                                applicationContext,
                                                settings.customWakeWord
                                            )
                                        }
                                    },
                                    onTogglePauseService = {
                                        if (backgroundState.isPaused) {
                                            SaraWakeService.resume(applicationContext)
                                        } else {
                                            SaraWakeService.pause(applicationContext)
                                        }
                                    },
                                    onStopWakeService = {
                                        SaraWakeService.stop(applicationContext)
                                    },
                                    onTalkOutside = {
                                        val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
                                        if (!snap.microphoneGranted) {
                                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            SaraWakeService.talkNow(applicationContext, settings.customWakeWord)
                                        }
                                    },
                                    onOrbSpeak = { message ->
                                        voiceEngine.speakIfEnabled(message, settings)
                                    },
                                    onStopSpeaking = {
                                        voiceEngine.stopPlayback()
                                    },
                                    onSubmitMessage = { message ->
                                        repository.recordConversationMessage(message, isAssistant = false)
                                        val reply = SaraAssistantBrain.generateReply(
                                            context = applicationContext,
                                            userText = message,
                                            settings = settings,
                                            memories = memories,
                                            history = conversationHistory,
                                            secureKeyStorage = repository.secureKeyStorage
                                        )
                                        repository.recordConversationMessage(reply.text, isAssistant = true)
                                        repository.addInboxMessage(
                                            title = message.take(44),
                                            body = reply.text,
                                            category = InboxCategory.VOICE_REPLY,
                                            senderVoicePersona = settings.femaleVoicePersona.voiceId
                                        )
                                        voiceEngine.speakIfEnabled(reply.text, settings)
                                        reply.text
                                    },
                                    onAddMemory = { note ->
                                        repository.addMemory(note)
                                    }
                                )
                            }

                            SaraDestination.INBOX -> {
                                BackHandler {
                                    destination = SaraDestination.HOME
                                }
                                InboxScreen(
                                    settings = settings,
                                    inboxItems = inboxItems,
                                    isSpeaking = isSpeaking,
                                    repository = repository,
                                    voiceEngine = voiceEngine,
                                    onBack = { destination = SaraDestination.HOME }
                                )
                            }

                            SaraDestination.SETTINGS -> {
                                BackHandler {
                                    destination = SaraDestination.HOME
                                }
                                SettingsScreen(
                                    settings = settings,
                                    memories = memories,
                                    conversationHistory = conversationHistory,
                                    inboxUnreadCount = unreadInboxCount,
                                    voiceStatus = voiceStatus,
                                    backgroundState = backgroundState,
                                    isSpeaking = isSpeaking,
                                    repository = repository,
                                    voiceEngine = voiceEngine,
                                    initialPage = settingsInitialPage,
                                    onBack = { destination = SaraDestination.HOME },
                                    onOpenInbox = { destination = SaraDestination.INBOX }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            val snap = AndroidPermissionHelper.captureSnapshot(applicationContext)
            repository.updatePermissionStateSnapshot(
                micGranted = snap.microphoneGranted,
                overlayGranted = snap.overlayPermissionGranted,
                batteryUnrestricted = snap.batteryOptimizationIgnored,
                serviceActive = SaraWakeService.isRunning.value
            )
            if (SaraWakeService.isRunning.value) {
                SaraWakeService.refreshOverlay(applicationContext)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(SaraBootReceiver.EXTRA_RESUME_WAKE_FROM_BOOT, false)) {
            val settings = repository.settings.value
            SaraWakeService.start(applicationContext, settings.customWakeWord, startPaused = false)
        }
    }

    override fun onDestroy() {
        voiceEngine.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun AppLockGateScreen(
    assistantName: String,
    onUnlockRequest: () -> Unit
) {
    val colors = LocalSaraColors.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SaraAvatarFrame(size = 96.dp, showOnlineBadge = false)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$assistantName লক করা আছে",
                style = MaterialTheme.typography.headlineSmall.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock with your Android screen lock to continue",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(20.dp))
            SaraPrimaryGradientButton(
                text = "Unlock $assistantName",
                onClick = onUnlockRequest
            )
        }
    }
}
