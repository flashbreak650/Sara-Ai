package com.sara.assistant.system

import android.Manifest
import android.annotation.SuppressLint
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
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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
import java.util.concurrent.atomic.AtomicInteger

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
    val continuousModeActive: Boolean = false,
    val phase: BackgroundAssistantPhase = BackgroundAssistantPhase.IDLE,
    val lastHeardText: String = "",
    val lastResponseText: String = "",
    val statusMessage: String = "স্ট্যান্ডবাই",
    val overlayVisible: Boolean = false,
    val reconnecting: Boolean = false
)

/**
 * Unified Android Foreground Service powering SARA's Continuous Voice Mode & Outside-App Assistant:
 * - Single active microphone session lock (AtomicInteger token guard prevents duplicate recognizers).
 * - Continuous hands-free loop: automatically resumes listening after SARA finishes speaking and stays
 *   active across natural pauses until the user explicitly turns OFF / Stop / Pause.
 * - Silent recognition chime suppressor: briefly mutes recognizer system/notification/music chimes
 *   during start/cancel transitions and restores cleanly before SARA speaks.
 * - Background + screen-off execution via FOREGROUND_SERVICE_MICROPHONE + PARTIAL_WAKE_LOCK.
 * - ConnectivityManager.NetworkCallback + exponential backoff auto-reconnect.
 */
class SaraWakeService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: SaraSettingsRepository
    private lateinit var voiceEngine: SaraVoiceEngine
    private lateinit var overlayManager: SaraFloatingOverlayManager
    private lateinit var audioManager: AudioManager
    private lateinit var connectivityManager: ConnectivityManager

    private var speechRecognizer: SpeechRecognizer? = null
    private var listeningMode: BackgroundAssistantPhase = BackgroundAssistantPhase.IDLE
    private var isDeviceScreenLocked = false
    private var isNetworkAvailable = true
    private var reconnectAttempt = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private val sessionGeneration = AtomicInteger(0)
    private var pendingLoopRunnable: Runnable? = null
    private var pendingUnmuteRunnable: Runnable? = null
    private val mutedStreamsByUs = mutableSetOf<Int>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.post {
                val wasDisconnected = !isNetworkAvailable
                isNetworkAvailable = true
                reconnectAttempt = 0
                if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                    if (_assistantState.value.reconnecting || wasDisconnected) {
                        updateState {
                            it.copy(
                                reconnecting = false,
                                statusMessage = if (repository.settings.value.continuousVoiceMode) {
                                    "সংযোগ ফিরে এসেছে · নিরবচ্ছিন্ন ভয়েস সক্রিয়"
                                } else {
                                    "সংযোগ ফিরে এসেছে · শুনছি"
                                }
                            )
                        }
                        if (!voiceEngine.isSpeaking.value && listeningMode != BackgroundAssistantPhase.THINKING) {
                            scheduleVoiceLoop(280L)
                        }
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            mainHandler.post {
                isNetworkAvailable = hasActiveInternet()
                if (!isNetworkAvailable && _assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                    updateState {
                        it.copy(
                            reconnecting = true,
                            statusMessage = "নেটওয়ার্ক বিচ্ছিন্ন — পুনঃসংযোগের অপেক্ষায়..."
                        )
                    }
                    refreshNotification()
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isDeviceScreenLocked = true
                    if (repository.settings.value.micPrivacyAutoMuteOnLock) {
                        pauseRecognizerForPrivacy()
                    } else if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                        acquireVoiceWakeLock()
                    }
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    isDeviceScreenLocked = false
                    if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                        scheduleVoiceLoop(300L)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = SaraSettingsRepository.getInstance(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkAvailable = hasActiveInternet()

        voiceEngine = SaraVoiceEngine(applicationContext, repository.secureKeyStorage)
        overlayManager = SaraFloatingOverlayManager(
            context = applicationContext,
            onTalkClick = { triggerManualVoiceConversation() },
            onPauseResumeClick = { togglePauseFromControl() },
            onStopConversationClick = { stopAllAndShutdownService() },
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

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
        }

        val netRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(netRequest, networkCallback) }
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
            ACTION_TOGGLE_CONTINUOUS -> {
                if (_isRunning.value && !_assistantState.value.isPaused) {
                    stopAllAndShutdownService()
                    return START_NOT_STICKY
                } else {
                    return startContinuousOrWake(intent, forceActive = true)
                }
            }
            ACTION_TALK_NOW -> {
                if (!hasMicPermission()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureForegroundStarted(repository.settings.value)
                _isRunning.value = true
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

        return startContinuousOrWake(intent, forceActive = false)
    }

    private fun startContinuousOrWake(intent: Intent?, forceActive: Boolean): Int {
        if (!hasMicPermission()) {
            _isRunning.value = false
            updateState {
                it.copy(
                    serviceRunning = false,
                    continuousModeActive = false,
                    phase = BackgroundAssistantPhase.IDLE,
                    statusMessage = "মাইক্রোফোন অনুমতি প্রয়োজন"
                )
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val settings = repository.settings.value
        val startPaused = if (forceActive) {
            false
        } else {
            intent?.getBooleanExtra(EXTRA_START_PAUSED, settings.wakeListeningPaused)
                ?: settings.wakeListeningPaused
        }

        ensureForegroundStarted(settings)
        _isRunning.value = true
        reconnectAttempt = 0

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
            acquireVoiceWakeLock()
            val continuous = settings.continuousVoiceMode
            val initialPhase = if (continuous) {
                BackgroundAssistantPhase.LISTENING_COMMAND
            } else {
                BackgroundAssistantPhase.LISTENING_WAKE
            }
            listeningMode = initialPhase
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = false,
                    continuousModeActive = continuous,
                    phase = initialPhase,
                    reconnecting = false,
                    statusMessage = if (continuous) {
                        "নিরবচ্ছিন্ন ভয়েস সক্রিয় — কথা বলুন..."
                    } else {
                        "\"${settings.customWakeWord}\" শোনার জন্য প্রস্তুত"
                    }
                )
            }
            syncOverlayVisibility()
            scheduleVoiceLoop(180L)
        }
        return START_STICKY
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasActiveInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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

    @SuppressLint("WakelockTimeout")
    private fun acquireVoiceWakeLock() {
        runCatching {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SARA:ContinuousVoiceWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                // Held while user has active Continuous Voice ON; released immediately on Stop/Pause/onDestroy
                wakeLock?.acquire(4 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseVoiceWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    private fun togglePauseFromControl() {
        val nextPaused = !_assistantState.value.isPaused
        setPausedState(nextPaused)
    }

    private fun setPausedState(paused: Boolean) {
        repository.updateSettings { it.copy(wakeListeningPaused = paused) }
        if (paused) {
            cancelPendingVoiceLoop()
            destroyRecognizerSilently()
            unmuteStreamsIfHeld()
            voiceEngine.stopPlayback()
            releaseVoiceWakeLock()
            listeningMode = BackgroundAssistantPhase.PAUSED
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = true,
                    phase = BackgroundAssistantPhase.PAUSED,
                    statusMessage = "ভয়েস অ্যাসিস্ট্যান্ট বিরতিতে (Paused)"
                )
            }
        } else {
            acquireVoiceWakeLock()
            val settings = repository.settings.value
            val continuous = settings.continuousVoiceMode
            listeningMode = if (continuous) {
                BackgroundAssistantPhase.LISTENING_COMMAND
            } else {
                BackgroundAssistantPhase.LISTENING_WAKE
            }
            updateState {
                it.copy(
                    serviceRunning = true,
                    isPaused = false,
                    continuousModeActive = continuous,
                    phase = listeningMode,
                    reconnecting = false,
                    statusMessage = if (continuous) {
                        "নিরবচ্ছিন্ন ভয়েস সক্রিয় — কথা বলুন..."
                    } else {
                        "\"${settings.customWakeWord}\" শোনার জন্য প্রস্তুত"
                    }
                )
            }
            scheduleVoiceLoop(200L)
        }
        syncOverlayVisibility()
        refreshNotification()
    }

    private fun stopConversationAndPause() {
        voiceEngine.stopPlayback()
        cancelPendingVoiceLoop()
        destroyRecognizerSilently()
        unmuteStreamsIfHeld()
        setPausedState(true)
    }

    private fun triggerManualVoiceConversation() {
        if (!hasMicPermission()) return
        cancelPendingVoiceLoop()
        voiceEngine.stopPlayback()
        acquireVoiceWakeLock()
        repository.updateSettings { it.copy(wakeWordEnabled = true, wakeListeningPaused = false) }
        if (overlayManager.canDrawOverlay() && repository.settings.value.floatingOverlayEnabled) {
            overlayManager.expandToCard()
        }
        startCommandListening()
    }

    private fun pauseRecognizerForPrivacy() {
        cancelPendingVoiceLoop()
        destroyRecognizerSilently()
        unmuteStreamsIfHeld()
        releaseVoiceWakeLock()
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

    private fun cancelPendingVoiceLoop() {
        pendingLoopRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingLoopRunnable = null
    }

    private fun scheduleVoiceLoop(delayMs: Long) {
        cancelPendingVoiceLoop()
        val runnable = Runnable {
            pendingLoopRunnable = null
            val state = _assistantState.value
            val settings = repository.settings.value
            if (!state.serviceRunning || state.isPaused || (isDeviceScreenLocked && settings.micPrivacyAutoMuteOnLock)) {
                return@Runnable
            }
            if (voiceEngine.isSpeaking.value ||
                state.phase == BackgroundAssistantPhase.SPEAKING ||
                state.phase == BackgroundAssistantPhase.THINKING
            ) {
                return@Runnable
            }
            if (settings.continuousVoiceMode || listeningMode == BackgroundAssistantPhase.LISTENING_COMMAND) {
                startCommandListening()
            } else {
                startWakeWordListening()
            }
        }
        pendingLoopRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun startWakeWordListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            updateState { it.copy(statusMessage = "ডিভাইসে স্পিচ রিকগনিশন পাওয়া যায়নি") }
            return
        }
        listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
        updateState {
            it.copy(
                continuousModeActive = false,
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
        val continuous = repository.settings.value.continuousVoiceMode
        listeningMode = BackgroundAssistantPhase.LISTENING_COMMAND
        updateState {
            it.copy(
                serviceRunning = true,
                isPaused = false,
                continuousModeActive = continuous,
                phase = BackgroundAssistantPhase.LISTENING_COMMAND,
                statusMessage = if (continuous) "নিরবচ্ছিন্ন শুনছি — বলুন..." else "বলুন, আমি শুনছি..."
            )
        }
        refreshNotification()
        launchRecognizer(isCommandMode = true)
    }

    /**
     * Suppresses Android's SpeechRecognizer start/stop beep/pop chimes cleanly without
     * affecting SARA's spoken voice output. Always restores streams before TTS plays.
     */
    private fun muteRecognizerChimeBriefly(includeMediaStream: Boolean) {
        if (voiceEngine.isSpeaking.value) return
        val targetStreams = buildList {
            add(AudioManager.STREAM_NOTIFICATION)
            add(AudioManager.STREAM_SYSTEM)
            if (includeMediaStream) add(AudioManager.STREAM_MUSIC)
        }
        for (stream in targetStreams) {
            runCatching {
                val alreadyMuted = audioManager.isStreamMute(stream)
                if (!alreadyMuted && !mutedStreamsByUs.contains(stream)) {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                    mutedStreamsByUs.add(stream)
                }
            }
        }
        pendingUnmuteRunnable?.let { mainHandler.removeCallbacks(it) }
        val safetyUnmute = Runnable { unmuteStreamsIfHeld() }
        pendingUnmuteRunnable = safetyUnmute
        mainHandler.postDelayed(safetyUnmute, 420L)
    }

    private fun unmuteStreamsIfHeld() {
        pendingUnmuteRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingUnmuteRunnable = null
        if (mutedStreamsByUs.isEmpty()) return
        val toRestore = mutedStreamsByUs.toList()
        mutedStreamsByUs.clear()
        for (stream in toRestore) {
            runCatching {
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
    }

    private fun launchRecognizer(isCommandMode: Boolean) {
        if (voiceEngine.isSpeaking.value) {
            return
        }

        // Strict Single Microphone Session Lock: invalidate old recognizer before opening a new one
        val sessionToken = sessionGeneration.incrementAndGet()
        destroyRecognizerSilently(incrementGeneration = false)

        muteRecognizerChimeBriefly(includeMediaStream = true)

        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(applicationContext) }.getOrNull()
        if (recognizer == null) {
            unmuteStreamsIfHeld()
            scheduleVoiceLoop(1500L)
            return
        }
        speechRecognizer = recognizer

        val settings = repository.settings.value
        val languageTag = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) "bn-BD" else "en-US"

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (sessionToken != sessionGeneration.get()) return
                reconnectAttempt = 0
                if (_assistantState.value.reconnecting) {
                    updateState { it.copy(reconnecting = false) }
                }
                // Unmute audio streams right after recognizer chime window has passed
                mainHandler.postDelayed({ unmuteStreamsIfHeld() }, 140L)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (sessionToken != sessionGeneration.get()) return
                if (!_assistantState.value.serviceRunning || _assistantState.value.isPaused) {
                    unmuteStreamsIfHeld()
                    return
                }
                if (voiceEngine.isSpeaking.value || listeningMode == BackgroundAssistantPhase.THINKING) {
                    unmuteStreamsIfHeld()
                    return
                }

                muteRecognizerChimeBriefly(includeMediaStream = false)
                destroyRecognizerSilently(incrementGeneration = false)

                val continuous = repository.settings.value.continuousVoiceMode
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // User paused or didn't speak yet — silently keep Continuous Voice active!
                        reconnectAttempt = 0
                        if (continuous) {
                            listeningMode = BackgroundAssistantPhase.LISTENING_COMMAND
                            scheduleVoiceLoop(260L)
                        } else {
                            listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
                            scheduleVoiceLoop(320L)
                        }
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> {
                        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(4)
                        val backoff = (700L * reconnectAttempt).coerceAtMost(3500L)
                        updateState {
                            it.copy(
                                reconnecting = true,
                                statusMessage = "নেটওয়ার্ক পুনঃসংযোগ চেষ্টা করছি..."
                            )
                        }
                        scheduleVoiceLoop(backoff)
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_AUDIO -> {
                        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(3)
                        scheduleVoiceLoop(900L * reconnectAttempt)
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        unmuteStreamsIfHeld()
                        stopAllAndShutdownService()
                    }
                    else -> {
                        scheduleVoiceLoop(550L)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (sessionToken != sessionGeneration.get()) return
                val heard = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (heard.isBlank()) return

                if (isCommandMode) {
                    updateState { it.copy(lastHeardText = heard) }
                } else if (matchesWakeWord(heard, repository.settings.value.customWakeWord)) {
                    val inlineCommand = extractAfterWakeWord(heard, repository.settings.value.customWakeWord)
                    if (inlineCommand.length >= 4) {
                        return
                    }
                    destroyRecognizerSilently()
                    onWakeWordTriggered("")
                }
            }

            override fun onResults(results: Bundle?) {
                if (sessionToken != sessionGeneration.get()) return
                reconnectAttempt = 0
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = matches.firstOrNull().orEmpty().trim()
                val currentSettings = repository.settings.value

                if (best.isBlank()) {
                    if (currentSettings.continuousVoiceMode) {
                        scheduleVoiceLoop(260L)
                    } else {
                        listeningMode = BackgroundAssistantPhase.LISTENING_WAKE
                        scheduleVoiceLoop(350L)
                    }
                    return
                }

                if (isCommandMode) {
                    val wakePhrase = currentSettings.customWakeWord
                    val cleanedQuery = if (matchesWakeWord(best, wakePhrase)) {
                        extractAfterWakeWord(best, wakePhrase).ifBlank { best }
                    } else {
                        best
                    }
                    processUserVoiceQuery(cleanedQuery)
                } else {
                    val wakePhrase = currentSettings.customWakeWord
                    val matched = matches.firstOrNull { matchesWakeWord(it, wakePhrase) }
                    if (matched != null) {
                        val inlineCommand = extractAfterWakeWord(matched, wakePhrase)
                        onWakeWordTriggered(inlineCommand)
                    } else {
                        scheduleVoiceLoop(300L)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, if (isCommandMode) 3000L else 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, if (isCommandMode) 2500L else 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3500L)
        }
        runCatching { recognizer.startListening(intent) }.onFailure {
            unmuteStreamsIfHeld()
            scheduleVoiceLoop(1200L)
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
        unmuteStreamsIfHeld()
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
        cancelPendingVoiceLoop()
        destroyRecognizerSilently()
        unmuteStreamsIfHeld()

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

            if (!_assistantState.value.serviceRunning || _assistantState.value.isPaused) {
                return@launch
            }

            unmuteStreamsIfHeld()
            listeningMode = BackgroundAssistantPhase.SPEAKING
            updateState {
                it.copy(
                    phase = BackgroundAssistantPhase.SPEAKING,
                    lastResponseText = reply.text,
                    reconnecting = false,
                    statusMessage = "সারা উত্তর দিচ্ছে..."
                )
            }
            refreshNotification()

            voiceEngine.speakIfEnabled(
                text = reply.text,
                settings = repository.settings.value,
                onPlaybackFinished = {
                    val latest = repository.settings.value
                    if (_assistantState.value.serviceRunning && !_assistantState.value.isPaused) {
                        listeningMode = if (latest.continuousVoiceMode) {
                            BackgroundAssistantPhase.LISTENING_COMMAND
                        } else {
                            BackgroundAssistantPhase.LISTENING_WAKE
                        }
                        // Slight echo-clearance pause then auto-resume Continuous Voice Mode
                        scheduleVoiceLoop(260L)
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

    private fun destroyRecognizerSilently(incrementGeneration: Boolean = true) {
        if (incrementGeneration) {
            sessionGeneration.incrementAndGet()
        }
        val active = speechRecognizer
        speechRecognizer = null
        if (active != null) {
            muteRecognizerChimeBriefly(includeMediaStream = false)
            runCatching {
                active.cancel()
                active.destroy()
            }
        }
    }

    private fun stopAllAndShutdownService() {
        cancelPendingVoiceLoop()
        destroyRecognizerSilently()
        unmuteStreamsIfHeld()
        voiceEngine.stopPlayback()
        releaseVoiceWakeLock()
        overlayManager.hideAll()
        listeningMode = BackgroundAssistantPhase.IDLE
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
                continuousModeActive = false,
                phase = BackgroundAssistantPhase.IDLE,
                statusMessage = "ভয়েস অ্যাসিস্ট্যান্ট বন্ধ আছে",
                overlayVisible = false,
                reconnecting = false
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

        val title = "${settings.assistantName.ifBlank { "SARA" }} ভয়েস অ্যাসিস্ট্যান্ট"
        val body = when {
            state.isPaused -> "বিরতিতে আছে · Resume ট্যাপ করলে আবার শুনবে"
            state.reconnecting -> "নেটওয়ার্ক পুনঃসংযোগের অপেক্ষায়..."
            state.phase == BackgroundAssistantPhase.SPEAKING -> "কথা বলছি · ${state.lastResponseText.take(42)}"
            state.phase == BackgroundAssistantPhase.LISTENING_COMMAND && settings.continuousVoiceMode ->
                "নিরবচ্ছিন্ন শুনছি · যেকোনো সময় কথা বলুন"
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
                "SARA Continuous Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active while SARA listens continuously in the background"
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
        cancelPendingVoiceLoop()
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        destroyRecognizerSilently()
        unmuteStreamsIfHeld()
        releaseVoiceWakeLock()
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
        const val ACTION_TOGGLE_CONTINUOUS = "com.sara.assistant.action.TOGGLE_CONTINUOUS"
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

        fun toggleContinuousVoice(context: Context, wakeWord: String): Boolean {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) return false

            val intent = Intent(context, SaraWakeService::class.java).apply {
                action = ACTION_TOGGLE_CONTINUOUS
                putExtra(EXTRA_WAKE_WORD, wakeWord)
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
