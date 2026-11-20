package com.sara.assistant.system

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sara.assistant.MainActivity
import com.sara.assistant.data.settings.InboxCategory
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SaraSettingsRepository
import com.sara.assistant.data.settings.VoiceLanguageOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class BackgroundAssistantPhase {
    IDLE,
    LISTENING_WAKE,
    LISTENING_COMMAND,
    THINKING,
    SPEAKING,
    PAUSED
}

data class BackgroundAssistantState(
    val serviceRunning: Boolean = false,
    val isPaused: Boolean = false,
    val phase: BackgroundAssistantPhase = BackgroundAssistantPhase.IDLE,
    val lastHeardText: String = "",
    val lastResponseText: String = "",
    val statusMessage: String = "স্ট্যান্ডবাই",
    val overlayVisible: Boolean = false
)

/**
 * Real Android Foreground Service enabling SARA outside the app:
 * - Listens continuously for wake word ("Hey SARA" / custom phrase) when permitted and unpaused.
 * - Handles full voice conversation outside the app through phone speaker via SaraVoiceEngine.
 * - Renders optional draggable floating bubble + compact conversation card via SaraFloatingOverlayManager.
 * - Displays transparent ongoing notification with Talk / Pause-Resume / Stop controls.
 */
class SaraWakeService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: SaraSettingsRepository
    private lateinit var voiceEngine: SaraVoiceEngine
    private lateinit var overlayManager: SaraFloatingOverlayManager

    private var speechRecognizer: SpeechRecognizer? = null
    private var listeningMode: BackgroundAssistantPhase = BackgroundAssistantPhase.IDLE
    private var isDeviceScreenLocked = false
    private var restartScheduled = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isDeviceScreenLocked = true
                    if (repository.settings.value.micPrivacyAutoMuteOnLock) {
                        pauseRecognizerForPrivacy()
                    }
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    isDeviceScreenLocked = false
                    if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                        scheduleWakeLoop(350L)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = SaraSettingsRepository.getInstance(applicationContext)
        voiceEngine = SaraVoiceEngine(applicationContext, repository.secureKeyStorage)
        overlayManager = SaraFloatingOverlayManager(
            context = applicationContext,
            onTalkClick = { triggerManualVoiceConversation() },
            onPauseResumeClick = { togglePauseFromControl() },
            onStopConversationClick = { stopConversationAndPause() },
            onOpenAppClick = {
                val appIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(appIntent)
            },
            onDismissOverlayClick = {
                repository.updateSettings { it.copy(floatingOverlayEnabled = false) }
                overlayManager.hideAll()
                updateState { it.copy(overlayVisible = false) }
            }
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAllAndShutdownService()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                setPausedState(true)
                return START_STICKY
            }
            ACTION_RESUME -> {
                setPausedState(false)
                return START_STICKY
            }
            ACTION_TALK_NOW -> {
                ensureForegroundStarted(repository.settings.value)
                triggerManualVoiceConversation()
                return START_STICKY
            }
            ACTION_STOP_CONVERSATION -> {
                stopConversationAndPause()
                return START_STICKY
            }
            ACTION_REFRESH_OVERLAY -> {
                if (_assistantState.value.serviceRunning) {
                    syncOverlayVisibility()
                }
                return START_STICKY
            }
        }

        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) {
            _isRunning.value = false
            updateState {
                it.copy(
                    serviceRunning = false,
                    phase = BackgroundAssistantPhase.IDLE,
                    statusMessage = "মাইক্রোফোন অনুমতি প্রয়োজন"
                )
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val settings = repository.settings.value
        val startPaused = intent?.getBooleanExtra(EXTRA_START_PAUSED, settings.wakeListeningPaused)
            ?: settings.wakeListeningPaused

        ensureForegroundStarted(settings)
        _isRunning.value = true

        repository.updateSettings {
            it.copy(
                wakeWordEnabled = true,
                wakeListeningPaused = startPaused,
                lastServiceActive = true
            )
        }

        if (startPaused) {
            setPausedState(true)
        } else {
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = false,
                    phase = BackgroundAssistantPhase.LISTENING_WAKE,
                    statusMessage = "\"${settings.customWakeWord}\" শোনার জন্য প্রস্তুত"
                )
            }
            syncOverlayVisibility()
            scheduleWakeLoop(250L)
        }
        return START_STICKY
    }

    private fun ensureForegroundStarted(settings: SaraSettings) {
        createNotificationChannel()
        val notification = buildNotification(settings, _assistantState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun togglePauseFromControl() {
        val nextPaused = !_assistantState.value.isPaused
        setPausedState(nextPaused)
    }

    private fun setPausedState(paused: Boolean) {
        repository.updateSettings { it.copy(wakeListeningPaused = paused) }
        if (paused) {
            destroyRecognizer()
            voiceEngine.stopPlayback()
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = true,
                    phase = BackgroundAssistantPhase.PAUSED,
                    statusMessage = "লিসেনিং বিরতিতে আছে (Paused)"
                )
            }
        } else {
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = false,
                    phase = BackgroundAssistantPhase.LISTENING_WAKE,
                    statusMessage = "\"${repository.settings.value.customWakeWord}\" শোনার জন্য প্রস্তুত"
                )
            }
            scheduleWakeLoop(200L)
        }
        syncOverlayVisibility()
        refreshNotification()
    }

    private fun stopConversationAndPause() {
        voiceEngine.stopPlayback()
        destroyRecognizer()
        setPausedState(true)
    }

    private fun triggerManualVoiceConversation() {
        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) return

        voiceEngine.stopPlayback()
        repository.updateSettings { it.copy(wakeListeningPaused = false) }
        if (overlayManager.canDrawOverlay() && repository.settings.value.floatingOverlayEnabled) {
            overlayManager.expandToCard()
        }
        startCommandListening()
    }

    private fun pauseRecognizerForPrivacy() {
        destroyRecognizer()
        updateState {
            it.copy(
                phase = BackgroundAssistantPhase.PAUSED,
                statusMessage = "স্ক্রিন লক প্রাইভেসি: মাইক্রোফোন স্থগিত"
            )
        }
        refreshNotification()
    }

    private fun syncOverlayVisibility() {
        val settings = repository.settings.value
        val shouldShow = settings.floatingOverlayEnabled &&
            overlayManager.canDrawOverlay() &&
            _assistantState.value.serviceRunning
        if (shouldShow) {
            overlayManager.showBubble()
            updateState { it.copy(overlayVisible = true) }
        } else {
            overlayManager.hideAll()
            updateState { it.copy(overlayVisible = false) }
        }
    }

    private fun scheduleWakeLoop(delayMs: Long) {
        if (restartScheduled) return
        restartScheduled = true
        mainHandler.postDelayed({
            restartScheduled = false
            val state = _assistantState.value
            val settings = repository.settings.value
            if (!state.serviceRunning || state.isPaused || (isDeviceScreenLocked && settings.micPrivacyAutoMuteOnLock)) {
                return@postDelayed
            }
            if (state.phase == BackgroundAssistantPhase.SPEAKING || state.phase == BackgroundAssistantPhase.THINKING) {
                return@postDelayed
            }
            if (listeningMode != BackgroundAssistantPhase.LISTENING_COMMAND) {
                startWakeWordListening()
            }
        }, delayMs)
    }

    private fun startWakeWordListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            updateState { it.copy(statusMessage = "ডিভাইসে স্পিচ রিকগনিশন পাওয়া যায়নি") }
            return
        }
        listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
        updateState {
            it.copy(
                phase = BackgroundAssistantPhase.LISTENING_WAKE,
                statusMessage = "\"${repository.settings.value.customWakeWord}\" শুনছি..."
            )
        }
        refreshNotification()
        launchRecognizer(isCommandMode = false)
    }

    private fun startCommandListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            updateState { it.copy(statusMessage = "ডিভাইসে স্পিচ রিকগনিশন পাওয়া যায়নি") }
            return
        }
        listeningMode = BackgroundAssistantPhase.LISTENING_COMMAND
        updateState {
            it.copy(
                isPaused = false,
                phase = BackgroundAssistantPhase.LISTENING_COMMAND,
                statusMessage = "বলুন, আমি শুনছি..."
            )
        }
        refreshNotification()
        launchRecognizer(isCommandMode = true)
    }

    private fun launchRecognizer(isCommandMode: Boolean) {
        destroyRecognizer()
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(applicationContext) }.getOrNull()
        if (recognizer == null) {
            scheduleWakeLoop(2000L)
            return
        }
        speechRecognizer = recognizer

        val settings = repository.settings.value
        val languageTag = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) "bn-BD" else "en-US"

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (!_assistantState.value.serviceRunning || _assistantState.value.isPaused) return
                if (isCommandMode && error == SpeechRecognizer.ERROR_NO_MATCH) {
                    listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
                    scheduleWakeLoop(500L)
                } else {
                    val backoff = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1200L else 600L
                    scheduleWakeLoop(backoff)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (isCommandMode) return
                val heard = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (heard.isNotBlank() && matchesWakeWord(heard, repository.settings.value.customWakeWord)) {
                    val inlineCommand = extractAfterWakeWord(heard, repository.settings.value.customWakeWord)
                    if (inlineCommand.length >= 4) {
                        // Wait slightly for final results unless user paused
                        return
                    }
                    destroyRecognizer()
                    onWakeWordTriggered("")
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = matches.firstOrNull().orEmpty().trim()
                if (best.isBlank()) {
                    listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
                    scheduleWakeLoop(400L)
                    return
                }

                if (isCommandMode) {
                    processUserVoiceQuery(best)
                } else {
                    val wakePhrase = repository.settings.value.customWakeWord
                    val matched = matches.firstOrNull { matchesWakeWord(it, wakePhrase) }
                    if (matched != null) {
                        val inlineCommand = extractAfterWakeWord(matched, wakePhrase)
                        onWakeWordTriggered(inlineCommand)
                    } else {
                        scheduleWakeLoop(350L)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, !isCommandMode)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, if (isCommandMode) 1800L else 1200L)
        }
        runCatching { recognizer.startListening(intent) }.onFailure {
            scheduleWakeLoop(1500L)
        }
    }

    private fun onWakeWordTriggered(inlineCommand: String) {
        val settings = repository.settings.value
        if (overlayManager.canDrawOverlay() && settings.floatingOverlayEnabled) {
            overlayManager.expandToCard()
        }
        if (inlineCommand.isNotBlank()) {
            processUserVoiceQuery(inlineCommand)
            return
        }

        val ack = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            "হ্যাঁ, বলুন! আমি শুনছি।"
        } else {
            "Yes, I'm listening!"
        }
        updateState {
            it.copy(
                phase = BackgroundAssistantPhase.SPEAKING,
                lastHeardText = settings.customWakeWord,
                lastResponseText = ack,
                statusMessage = "সারা জেগে উঠেছে..."
            )
        }
        refreshNotification()

        voiceEngine.speakIfEnabled(
            text = ack,
            settings = settings,
            onPlaybackFinished = {
                if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                    startCommandListening()
                }
            }
        )
    }

    private fun processUserVoiceQuery(userQuery: String) {
        listeningMode = BackgroundAssistantPhase.THINKING
        updateState {
            it.copy(
                phase = BackgroundAssistantPhase.THINKING,
                lastHeardText = userQuery,
                statusMessage = "উত্তর তৈরি হচ্ছে..."
            )
        }
        refreshNotification()
        repository.recordConversationMessage(userQuery, isAssistant = false)

        serviceScope.launch {
            val currentSettings = repository.settings.value
            val reply = SaraAssistantBrain.generateReply(
                context = applicationContext,
                userText = userQuery,
                settings = currentSettings,
                memories = repository.memories.value,
                history = repository.conversationHistory.value,
                secureKeyStorage = repository.secureKeyStorage
            )

            repository.recordConversationMessage(reply.text, isAssistant = true)
            repository.addInboxMessage(
                title = userQuery.take(48),
                body = reply.text,
                category = InboxCategory.WAKE_ALERT,
                senderVoicePersona = currentSettings.femaleVoicePersona.voiceId
            )

            updateState {
                it.copy(
                    phase = BackgroundAssistantPhase.SPEAKING,
                    lastResponseText = reply.text,
                    statusMessage = "সারা উত্তর দিচ্ছে..."
                )
            }
            refreshNotification()

            voiceEngine.speakIfEnabled(
                text = reply.text,
                settings = currentSettings,
                onPlaybackFinished = {
                    listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
                    if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                        scheduleWakeLoop(550L)
                    }
                }
            )
        }
    }

    private fun matchesWakeWord(transcript: String, customWakeWord: String): Boolean {
        val norm = transcript.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9\\u0980-\\u09FF\\s]"), " ").trim()
        val target = customWakeWord.lowercase(Locale.ROOT).trim()
        if (target.isNotEmpty() && norm.contains(target)) return true
        return norm.contains("hey sara") ||
            norm.contains("hi sara") ||
            norm.contains("ok sara") ||
            norm.contains("hey sarah") ||
            norm.contains("হেই সারা") ||
            norm.contains("হে সারা") ||
            norm == "সারা" ||
            norm.startsWith("সারা ")
    }

    private fun extractAfterWakeWord(transcript: String, customWakeWord: String): String {
        val lower = transcript.lowercase(Locale.ROOT)
        val triggers = listOf(
            customWakeWord.lowercase(Locale.ROOT).trim(),
            "hey sara",
            "hey sarah",
            "hi sara",
            "ok sara",
            "হেই সারা",
            "হে সারা",
            "সারা"
        ).filter { it.isNotEmpty() }

        for (trigger in triggers) {
            val idx = lower.indexOf(trigger)
            if (idx >= 0) {
                return transcript.substring(idx + trigger.length).trim(' ', ',', '।', '!', '?')
            }
        }
        return ""
    }

    private fun destroyRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    private fun stopAllAndShutdownService() {
        destroyRecognizer()
        voiceEngine.stopPlayback()
        overlayManager.hideAll()
        _isRunning.value = false
        repository.updateSettings {
            it.copy(
                wakeWordEnabled = false,
                wakeListeningPaused = false,
                lastServiceActive = false
            )
        }
        updateState {
            BackgroundAssistantState(
                serviceRunning = false,
                isPaused = false,
                phase = BackgroundAssistantPhase.IDLE,
                statusMessage = "সার্ভিস বন্ধ আছে",
                overlayVisible = false
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun refreshNotification() {
        if (!_assistantState.value.serviceRunning) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(repository.settings.value, _assistantState.value))
    }

    private fun buildNotification(
        settings: SaraSettings,
        state: BackgroundAssistantState
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentPending = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val talkPending = PendingIntent.getService(
            this,
            1,
            Intent(this, SaraWakeService::class.java).apply { action = ACTION_TALK_NOW },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumePending = PendingIntent.getService(
            this,
            2,
            Intent(this, SaraWakeService::class.java).apply {
                action = if (state.isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this,
            3,
            Intent(this, SaraWakeService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${settings.assistantName.ifBlank { "SARA" }} ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট"
        val body = when {
            state.isPaused -> "বিরতিতে আছে · Resume ট্যাপ করলে আবার শুনবে"
            state.phase == BackgroundAssistantPhase.SPEAKING -> "কথা বলছি · ${state.lastResponseText.take(42)}"
            state.phase == BackgroundAssistantPhase.LISTENING_COMMAND -> "আপনার কথা শুনছি..."
            else -> "\"${settings.customWakeWord}\" শোনার জন্য সক্রিয়"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPending)
            .addAction(android.R.drawable.ic_btn_speak_now, "Talk", talkPending)
            .addAction(
                if (state.isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (state.isPaused) "Resume" else "Pause",
                pauseResumePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SARA Background Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active while SARA listens outside the app & powers floating control"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateState(transform: (BackgroundAssistantState) -> BackgroundAssistantState) {
        val next = transform(_assistantState.value)
        _assistantState.value = next
        if (::overlayManager.isInitialized && overlayManager.isShowing()) {
            overlayManager.updateState(next)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        destroyRecognizer()
        if (::overlayManager.isInitialized) overlayManager.hideAll()
        if (::voiceEngine.isInitialized) voiceEngine.shutdown()
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "sara_wake_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.sara.assistant.action.START_WAKE"
        const val ACTION_STOP = "com.sara.assistant.action.STOP_WAKE"
        const val ACTION_PAUSE = "com.sara.assistant.action.PAUSE_WAKE"
        const val ACTION_RESUME = "com.sara.assistant.action.RESUME_WAKE"
        const val ACTION_TALK_NOW = "com.sara.assistant.action.TALK_NOW"
        const val ACTION_STOP_CONVERSATION = "com.sara.assistant.action.STOP_CONVERSATION"
        const val ACTION_REFRESH_OVERLAY = "com.sara.assistant.action.REFRESH_OVERLAY"
        const val EXTRA_WAKE_WORD = "extra_wake_word"
        const val EXTRA_START_PAUSED = "extra_start_paused"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _assistantState = MutableStateFlow(BackgroundAssistantState())
        val assistantState: StateFlow<BackgroundAssistantState> = _assistantState.asStateFlow()

        fun start(context: Context, wakeWord: String, startPaused: Boolean = false): Boolean {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) return false

            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WAKE_WORD, wakeWord)
                putExtra(EXTRA_START_PAUSED, startPaused)
            }
            return runCatching {
                ContextCompat.startForegroundService(context, intent)
                true
            }.getOrDefault(false)
        }

        fun pause(context: Context) {
            val intent = Intent(context, SaraWakeService::class.java).apply { action = ACTION_PAUSE }
            runCatching { context.startService(intent) }
        }

        fun resume(context: Context) {
            val intent = Intent(context, SaraWakeService::class.java).apply { action = ACTION_RESUME }
            runCatching { context.startService(intent) }
        }

        fun talkNow(context: Context, wakeWord: String) {
            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_TALK_NOW
                putExtra(EXTRA_WAKE_WORD, wakeWord)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stopConversation(context: Context) {
            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_STOP_CONVERSATION
            }
            runCatching { context.startService(intent) }
        }

        fun refreshOverlay(context: Context) {
            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_REFRESH_OVERLAY
            }
            runCatching { context.startService(intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
