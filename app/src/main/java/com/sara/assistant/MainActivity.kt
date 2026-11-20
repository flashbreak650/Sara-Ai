package com.sara.assistant

import android.app.KeyguardManager
import android.content.Context
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
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.system.SaraVoiceEngine
import com.sara.assistant.system.SaraWakeService
import com.sara.assistant.ui.components.SaraAvatarFrame
import com.sara.assistant.ui.components.SaraPrimaryGradientButton
import com.sara.assistant.ui.screens.HomeScreen
import com.sara.assistant.ui.screens.ProfileScreen
import com.sara.assistant.ui.screens.SettingsScreen
import com.sara.assistant.ui.theme.LocalSaraColors
import com.sara.assistant.ui.theme.SaraTheme

private enum class SaraDestination {
    HOME,
    PROFILE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: SaraSettingsRepository
    private lateinit var voiceEngine: SaraVoiceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = SaraSettingsRepository(applicationContext)
        voiceEngine = SaraVoiceEngine(applicationContext, repository.secureKeyStorage)

        setContent {
            val settings by repository.settings.collectAsState()
            val memories by repository.memories.collectAsState()
            val conversationHistory by repository.conversationHistory.collectAsState()
            val voiceStatus by voiceEngine.status.collectAsState()
            val isSpeaking by voiceEngine.isSpeaking.collectAsState()
            val wakeServiceRunning by SaraWakeService.isRunning.collectAsState()

            var destination by remember { mutableStateOf(SaraDestination.HOME) }
            var unlocked by remember { mutableStateOf(!settings.appLockEnabled) }

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
                    val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                        "${settings.assistantName} Security Lock",
                        "Unlock ${settings.assistantName} with your device PIN or biometric"
                    )
                    if (intent != null) {
                        unlockLauncher.launch(intent)
                    } else {
                        unlocked = true
                    }
                } else if (!settings.appLockEnabled) {
                    unlocked = true
                }
            }

            SaraTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor,
                textSize = settings.textSize
            ) {
                if (!unlocked) {
                    AppLockGateScreen(
                        assistantName = settings.assistantName,
                        onUnlockRequest = {
                            @Suppress("DEPRECATION")
                            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                                "${settings.assistantName} Security Lock",
                                "Unlock ${settings.assistantName}"
                            )
                            if (intent != null) {
                                unlockLauncher.launch(intent)
                            } else {
                                unlocked = true
                            }
                        }
                    )
                } else {
                    Crossfade(
                        targetState = destination,
                        animationSpec = tween(durationMillis = if (settings.animationsEnabled) 260 else 0),
                        label = "sara_nav_crossfade"
                    ) { screen ->
                        when (screen) {
                            SaraDestination.HOME -> {
                                HomeScreen(
                                    settings = settings,
                                    memories = memories,
                                    conversationHistory = conversationHistory,
                                    isSpeaking = isSpeaking,
                                    wakeServiceRunning = wakeServiceRunning,
                                    onOpenProfile = { destination = SaraDestination.PROFILE },
                                    onOpenSettings = { destination = SaraDestination.SETTINGS },
                                    onOrbSpeak = { message ->
                                        voiceEngine.speakIfEnabled(message, settings)
                                    },
                                    onStopSpeaking = {
                                        voiceEngine.stopPlayback()
                                    },
                                    onSubmitMessage = { message ->
                                        repository.recordConversationMessage(message)
                                    },
                                    onAddMemory = { note ->
                                        repository.addMemory(note)
                                    }
                                )
                            }

                            SaraDestination.PROFILE -> {
                                BackHandler {
                                    destination = SaraDestination.HOME
                                }
                                ProfileScreen(
                                    settings = settings,
                                    memoryCount = memories.size,
                                    conversationCount = conversationHistory.size,
                                    voiceStatus = voiceStatus,
                                    wakeServiceRunning = wakeServiceRunning,
                                    isSpeaking = isSpeaking,
                                    repository = repository,
                                    voiceEngine = voiceEngine,
                                    onBack = { destination = SaraDestination.HOME },
                                    onOpenSettings = { destination = SaraDestination.SETTINGS }
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
                                    voiceStatus = voiceStatus,
                                    wakeServiceRunning = wakeServiceRunning,
                                    isSpeaking = isSpeaking,
                                    repository = repository,
                                    voiceEngine = voiceEngine,
                                    onBack = { destination = SaraDestination.HOME },
                                    onOpenProfile = { destination = SaraDestination.PROFILE }
                                )
                            }
                        }
                    }
                }
            }
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
