package com.sara.assistant.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import com.sara.assistant.data.settings.BengaliPronunciationQuality
import com.sara.assistant.data.settings.FemaleVoicePersona
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SecureKeyStorage
import com.sara.assistant.data.settings.VoiceGenderPreference
import com.sara.assistant.data.settings.VoiceLanguageOption
import com.sara.assistant.data.settings.VoiceProviderMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

data class VoiceEngineStatus(
    val initialized: Boolean = false,
    val unavailableMessage: String? = null,
    val bengaliSupported: Boolean = false,
    val femaleVoiceAvailable: Boolean = false,
    val neuralNativeFemaleAvailable: Boolean = false,
    val maleVoiceAvailable: Boolean = false,
    val selectedNativeFemaleVoiceName: String? = null,
    val bengaliVoiceInventoryCount: Int = 0,
    val honestQualityNote: String? = null
)

data class VoicePlaybackResult(
    val started: Boolean,
    val providerUsed: String,
    val message: String
)

private sealed class CloudTtsFetch {
    data class Success(
        val wavBytes: ByteArray,
        val providerLabel: String,
        val resolvedVoice: String
    ) : CloudTtsFetch()

    data class Failure(
        val message: String,
        val isQuotaOrAuth: Boolean = false
    ) : CloudTtsFetch()
}

/**
 * Natural Human Female Bengali & English Voice Engine for SARA.
 *
 * Prioritizes:
 * 1. Gemini 3.8 Neural TTS (`gemini-3.8-flash-lite-tts` via `generateContent` or Backend `/api/tts`)
 *    with rich human conversational direction, zero-resampling bit-exact 24kHz playback at 1.0x,
 *    disk WAV caching, and sentence prefetching for natural breathing pauses.
 * 2. Highest-quality ranked on-device neural/network Android Female Bengali (`bn-BD` / `bn-IN`) &
 *    English (`en-US` / `en-GB`) voices with natural inter-sentence silence pauses and AudioFocus.
 */
class SaraVoiceEngine(
    context: Context,
    private val secureKeyStorage: SecureKeyStorage? = null
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val voiceCacheDir = File(appContext.cacheDir, "sara_voice_cache").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeSpeechJob: Job? = null
    private var activeCompletionCallback: (() -> Unit)? = null
    private var activeFocusRequest: AudioFocusRequest? = null
    private var pendingInitRequest: PendingNativeTtsRequest? = null

    private data class PendingNativeTtsRequest(
        val text: String,
        val settings: SaraSettings,
        val onComplete: (VoicePlaybackResult) -> Unit,
        val onPlaybackFinished: (() -> Unit)?
    )

    private val _status = MutableStateFlow(VoiceEngineStatus())
    val status: StateFlow<VoiceEngineStatus> = _status.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(statusCode: Int) {
        val engine = tts
        if (statusCode != TextToSpeech.SUCCESS || engine == null) {
            _status.value = VoiceEngineStatus(
                initialized = false,
                unavailableMessage = "ডিভাইসের অফলাইন TTS ইঞ্জিন পাওয়া যায়নি; ক্লাউড নিউরাল ভয়েস ব্যবহার করুন।"
            )
            pendingInitRequest?.let { pending ->
                pendingInitRequest = null
                pending.onComplete(
                    VoicePlaybackResult(
                        started = false,
                        providerUsed = "Android TTS",
                        message = "ডিভাইসের TTS ইঞ্জিন প্রস্তুত নয়।"
                    )
                )
                pending.onPlaybackFinished?.invoke()
            }
            return
        }

        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.endsWith("_last") == true) {
                    scope.launch {
                        _isSpeaking.value = false
                        abandonAudioFocus()
                        val cb = activeCompletionCallback
                        activeCompletionCallback = null
                        cb?.invoke()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _isSpeaking.value = false
                    abandonAudioFocus()
                    val cb = activeCompletionCallback
                    activeCompletionCallback = null
                    cb?.invoke()
                }
            }
        })

        val bnBd = engine.isLanguageAvailable(Locale("bn", "BD"))
        val bnIn = engine.isLanguageAvailable(Locale("bn", "IN"))
        val bnSupported = bnBd >= TextToSpeech.LANG_AVAILABLE || bnIn >= TextToSpeech.LANG_AVAILABLE

        val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrElse { emptyList() }
        val bengaliVoices = allVoices.filter { it.locale.language.equals("bn", ignoreCase = true) }

        val bestFemaleBengali = selectBestVoiceForLocale(bengaliVoices, VoiceGenderPreference.FEMALE)
        val bestMaleBengali = selectBestVoiceForLocale(bengaliVoices, VoiceGenderPreference.MALE)

        val anyFemale = bestFemaleBengali ?: allVoices.firstOrNull { isExplicitFemaleVoice(it) }
        val neuralFemaleAvailable = bestFemaleBengali?.let { isHighQualityOrNetworkVoice(it) && isExplicitFemaleVoice(it) } == true

        val qualityNote = when {
            bestFemaleBengali == null && bengaliVoices.isEmpty() ->
                "এই ডিভাইসে অফলাইন বাংলা ভয়েস প্যাক পাওয়া যায়নি। সবচেয়ে সুন্দর ও মানবিক বাংলা নারী কণ্ঠের জন্য Cloud Neural Female (Gemini TTS) সক্রিয় রাখুন।"
            !neuralFemaleAvailable ->
                "৬টি নারী কণ্ঠই ফিমেল টিউনে সক্রিয় আছে; আরও বাস্তব মানুষের মতো বাংলা কণ্ঠের জন্য Cloud Neural Female ব্যবহার করতে পারেন।"
            else ->
                "ডিভাইসে বাংলা নারী ভয়েস (${bestFemaleBengali?.name}) পাওয়া গেছে; ৬টি নারী কণ্ঠই ব্যবহারযোগ্য।"
        }

        _status.value = VoiceEngineStatus(
            initialized = true,
            unavailableMessage = null,
            bengaliSupported = bnSupported,
            femaleVoiceAvailable = anyFemale != null,
            neuralNativeFemaleAvailable = neuralFemaleAvailable,
            maleVoiceAvailable = bestMaleBengali != null,
            selectedNativeFemaleVoiceName = bestFemaleBengali?.name,
            bengaliVoiceInventoryCount = bengaliVoices.size,
            honestQualityNote = qualityNote
        )

        pendingInitRequest?.let { pending ->
            pendingInitRequest = null
            scope.launch {
                val res = speakWithNativeTts(pending.text, pending.settings, pending.onPlaybackFinished)
                if (!res.started) {
                    pending.onPlaybackFinished?.invoke()
                }
                pending.onComplete(res)
            }
        }
    }

    fun speakTest(
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        val sampleText = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            settings.femaleVoicePersona.previewSentenceBn.replace("সারা", settings.assistantName.ifBlank { "সারা" })
        } else {
            settings.femaleVoicePersona.previewSentenceEn.replace("SARA", settings.assistantName.ifBlank { "SARA" })
        }
        speakText(sampleText, settings, onComplete)
    }

    fun previewPersona(
        persona: FemaleVoicePersona,
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        val previewSettings = settings.copy(
            voiceGender = VoiceGenderPreference.FEMALE,
            femaleVoicePersona = persona
        )
        val sampleText = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            persona.previewSentenceBn.replace("সারা", settings.assistantName.ifBlank { "সারা" })
        } else {
            persona.previewSentenceEn.replace("SARA", settings.assistantName.ifBlank { "SARA" })
        }
        speakText(sampleText, previewSettings, onComplete)
    }

    fun speakIfEnabled(
        text: String,
        settings: SaraSettings,
        onPlaybackFinished: (() -> Unit)? = null,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        if (!settings.voiceResponsesEnabled) {
            onPlaybackFinished?.invoke()
            return
        }
        speakText(text, settings, onComplete, onPlaybackFinished)
    }

    fun speakText(
        text: String,
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {},
        onPlaybackFinished: (() -> Unit)? = null
    ) {
        stopPlayback()

        val formattedText = formatNaturalBengaliProsody(text, settings.bengaliPronunciationQuality)
        if (formattedText.isBlank()) {
            onPlaybackFinished?.invoke()
            return
        }

        val endpointUrl = resolveCloudTtsEndpoint(settings)
        val directApiKey = secureKeyStorage?.decryptApiKeyOrNull()?.takeIf { it.isNotBlank() }

        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE &&
            (endpointUrl.isNotEmpty() || directApiKey != null)
        ) {
            activeSpeechJob = scope.launch {
                val segments = splitIntoNaturalSpeechSegments(formattedText)
                var playedAnyCloud = false
                var lastProviderLabel = "Gemini Neural Female"
                var failureNote = ""

                try {
                    requestAssistantAudioFocus()

                    var nextFetch = async(Dispatchers.IO) {
                        fetchCloudAudioSegment(
                            segment = segments.first(),
                            settings = settings,
                            endpointUrl = endpointUrl,
                            directApiKey = directApiKey
                        )
                    }

                    for (index in segments.indices) {
                        val currentResult = nextFetch.await()
                        if (index + 1 < segments.size) {
                            val upcoming = segments[index + 1]
                            nextFetch = async(Dispatchers.IO) {
                                fetchCloudAudioSegment(
                                    segment = upcoming,
                                    settings = settings,
                                    endpointUrl = endpointUrl,
                                    directApiKey = directApiKey
                                )
                            }
                        }

                        when (currentResult) {
                            is CloudTtsFetch.Success -> {
                                if (!playedAnyCloud) {
                                    playedAnyCloud = true
                                    lastProviderLabel = currentResult.providerLabel
                                    onComplete(
                                        VoicePlaybackResult(
                                            started = true,
                                            providerUsed = lastProviderLabel,
                                            message = "প্রাকৃতিক মানবিক নারী কণ্ঠ (${currentResult.resolvedVoice}) চালু হয়েছে।"
                                        )
                                    )
                                }
                                val playedOk = playWavBytesSuspending(currentResult.wavBytes, settings)
                                if (!playedOk) {
                                    failureNote = "অডিও প্লেব্যাক ত্রুটি"
                                    break
                                }
                                // Natural human breathing pause between sentences
                                if (index + 1 < segments.size &&
                                    settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN
                                ) {
                                    delay(160L)
                                }
                            }
                            is CloudTtsFetch.Failure -> {
                                failureNote = currentResult.message
                                break
                            }
                        }
                    }

                    if (playedAnyCloud) {
                        _isSpeaking.value = false
                        abandonAudioFocus()
                        onPlaybackFinished?.invoke()
                    } else {
                        // Fall back cleanly to highest-quality device female neural voice
                        _isSpeaking.value = false
                        abandonAudioFocus()
                        val nativeFallback = speakWithNativeTts(formattedText, settings, onPlaybackFinished)
                        if (!nativeFallback.started) {
                            onPlaybackFinished?.invoke()
                        }
                        onComplete(
                            if (nativeFallback.started) {
                                nativeFallback.copy(
                                    message = "$failureNote — ডিভাইসের সেরা বাংলা নারী কণ্ঠে শোনানো হয়েছে।"
                                )
                            } else {
                                VoicePlaybackResult(
                                    started = false,
                                    providerUsed = lastProviderLabel,
                                    message = failureNote.ifBlank { nativeFallback.message }
                                )
                            }
                        )
                    }
                } catch (ce: CancellationException) {
                    _isSpeaking.value = false
                    abandonAudioFocus()
                    throw ce
                } catch (e: Exception) {
                    _isSpeaking.value = false
                    abandonAudioFocus()
                    if (!playedAnyCloud) {
                        val nativeFallback = speakWithNativeTts(formattedText, settings, onPlaybackFinished)
                        if (!nativeFallback.started) {
                            onPlaybackFinished?.invoke()
                        }
                        onComplete(nativeFallback)
                    } else {
                        onPlaybackFinished?.invoke()
                    }
                }
            }
            return
        }

        if (!_status.value.initialized) {
            pendingInitRequest = PendingNativeTtsRequest(
                text = formattedText,
                settings = settings,
                onComplete = onComplete,
                onPlaybackFinished = onPlaybackFinished
            )
            return
        }

        val nativeResult = speakWithNativeTts(formattedText, settings, onPlaybackFinished)
        if (!nativeResult.started) {
            onPlaybackFinished?.invoke()
        }
        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE &&
            endpointUrl.isEmpty() && directApiKey == null
        ) {
            onComplete(nativeResult)
        } else {
            onComplete(nativeResult)
        }
    }

    private fun fetchCloudAudioSegment(
        segment: String,
        settings: SaraSettings,
        endpointUrl: String,
        directApiKey: String?
    ): CloudTtsFetch {
        val personaId = if (settings.voiceGender == VoiceGenderPreference.MALE) {
            "Charon"
        } else {
            settings.femaleVoicePersona.voiceId
        }
        val detectedLang = detectDominantLanguage(segment, settings.voiceLanguage)
        val useNaturalPauses = settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN
        val paceBucket = (settings.speechSpeed * 10f).toInt()
        val cacheKey = sha256("v2|$personaId|${detectedLang.localeTag}|$useNaturalPauses|$paceBucket|$segment")

        readCachedWav(cacheKey)?.let { cachedBytes ->
            return CloudTtsFetch.Success(
                wavBytes = cachedBytes,
                providerLabel = "Gemini Neural Female ($personaId)",
                resolvedVoice = personaId
            )
        }

        var lastFailure: CloudTtsFetch.Failure? = null

        if (endpointUrl.isNotEmpty()) {
            when (val backendRes = callBackendTts(endpointUrl, segment, detectedLang, settings)) {
                is CloudTtsFetch.Success -> {
                    writeCachedWav(cacheKey, backendRes.wavBytes)
                    return backendRes
                }
                is CloudTtsFetch.Failure -> {
                    lastFailure = backendRes
                }
            }
        }

        if (directApiKey != null) {
            when (val directRes = callDirectGeminiGenerateContentTts(directApiKey, segment, detectedLang, settings)) {
                is CloudTtsFetch.Success -> {
                    writeCachedWav(cacheKey, directRes.wavBytes)
                    return directRes
                }
                is CloudTtsFetch.Failure -> {
                    lastFailure = directRes
                }
            }
        }

        return lastFailure ?: CloudTtsFetch.Failure("Cloud Neural TTS সংযোগ পাওয়া যায়নি")
    }

    private fun resolveCloudTtsEndpoint(settings: SaraSettings): String {
        val explicit = settings.cloudTtsEndpointUrl.trim()
        if (explicit.isNotEmpty()) return explicit

        val proxy = settings.apiProxyUrl.trim().trimEnd('/')
        if (proxy.isEmpty()) return ""
        return when {
            proxy.endsWith("/api/tts") -> proxy
            proxy.endsWith("/api/chat") -> proxy.removeSuffix("/api/chat") + "/api/tts"
            else -> "$proxy/api/tts"
        }
    }

    private fun callBackendTts(
        endpointUrl: String,
        text: String,
        detectedLang: VoiceLanguageOption,
        settings: SaraSettings
    ): CloudTtsFetch {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 9000
                readTimeout = 22000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val voiceName = when (settings.voiceGender) {
                VoiceGenderPreference.MALE -> "Charon"
                VoiceGenderPreference.FEMALE, VoiceGenderPreference.SYSTEM_DEFAULT ->
                    settings.femaleVoicePersona.voiceId
            }

            val payload = JSONObject().apply {
                put("text", text)
                put("language", if (detectedLang == VoiceLanguageOption.BENGALI) "bn" else "en")
                put("voiceGender", if (settings.voiceGender == VoiceGenderPreference.MALE) "male" else "female")
                put("voiceName", voiceName)
                put("assistantName", settings.assistantName.ifBlank { "SARA" })
                put("naturalPauses", settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN)
                put("speechSpeed", settings.speechSpeed.toDouble())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                return CloudTtsFetch.Failure(
                    message = "Cloud TTS HTTP $code ত্রুটি",
                    isQuotaOrAuth = code == 429 || code == 401 || code == 403
                )
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val audioB64 = json.optString("audioBase64", "")
            if (audioB64.isBlank()) {
                return CloudTtsFetch.Failure("Cloud TTS অডিও পাওয়া যায়নি")
            }

            val wavBytes = Base64.decode(audioB64, Base64.DEFAULT)
            val resolvedVoice = json.optString("voiceName", voiceName)
            CloudTtsFetch.Success(
                wavBytes = wavBytes,
                providerLabel = "Gemini Neural Female ($resolvedVoice)",
                resolvedVoice = resolvedVoice
            )
        } catch (e: Exception) {
            CloudTtsFetch.Failure("Cloud TTS সংযোগ ব্যর্থ (${e.javaClass.simpleName})")
        } finally {
            conn?.disconnect()
        }
    }

    private fun callDirectGeminiGenerateContentTts(
        apiKey: String,
        text: String,
        detectedLang: VoiceLanguageOption,
        settings: SaraSettings
    ): CloudTtsFetch {
        val isBn = detectedLang == VoiceLanguageOption.BENGALI
        val persona = settings.femaleVoicePersona
        val primaryVoice = if (settings.voiceGender == VoiceGenderPreference.MALE) "Charon" else persona.geminiVoiceId
        val fallbackVoice = if (settings.voiceGender == VoiceGenderPreference.MALE) "Charon" else persona.fallbackVoiceId
        val displayLabel = if (settings.voiceGender == VoiceGenderPreference.MALE) "Charon" else persona.voiceId
        val styleDesc = buildNaturalStylePrompt(
            personaKey = displayLabel,
            isBengali = isBn,
            assistantName = settings.assistantName.ifBlank { "SARA" },
            speed = settings.speechSpeed,
            naturalPauses = settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN
        )

        val primaryAttempt = requestGeminiGenerateContentTts(
            apiKey = apiKey,
            model = "gemini-3.8-flash-lite-tts",
            text = text,
            style = styleDesc,
            voiceName = primaryVoice
        )
        if (primaryAttempt is CloudTtsFetch.Success) {
            return primaryAttempt.copy(
                providerLabel = "Gemini 3.8 Neural ($displayLabel)",
                resolvedVoice = displayLabel
            )
        }

        if (primaryAttempt is CloudTtsFetch.Failure && !primaryAttempt.isQuotaOrAuth && fallbackVoice != primaryVoice) {
            val fallbackAttempt = requestGeminiGenerateContentTts(
                apiKey = apiKey,
                model = "gemini-3.8-flash-lite-tts",
                text = text,
                style = styleDesc,
                voiceName = fallbackVoice
            )
            if (fallbackAttempt is CloudTtsFetch.Success) {
                return fallbackAttempt.copy(
                    providerLabel = "Gemini 3.8 Neural ($displayLabel)",
                    resolvedVoice = displayLabel
                )
            }
        }

        return primaryAttempt
    }

    private fun requestGeminiGenerateContentTts(
        apiKey: String,
        model: String,
        text: String,
        style: String,
        voiceName: String
    ): CloudTtsFetch {
        var conn: HttpURLConnection? = null
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 9000
                readTimeout = 22000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            val bodyJson = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().apply {
                                        put("text", text)
                                        put(
                                            "speechMetadata",
                                            JSONObject().put("style", style)
                                        )
                                    }
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put(
                            "speechConfig",
                            JSONObject().put(
                                "voiceConfig",
                                JSONObject().put(
                                    "prebuiltVoiceConfig",
                                    JSONObject().put("voiceName", voiceName)
                                )
                            )
                        )
                    }
                )
            }

            OutputStreamWriter(conn.outputStream).use { it.write(bodyJson.toString()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                return CloudTtsFetch.Failure(
                    message = if (code == 429) "Gemini TTS দৈনিক ফ্রি কোটা পূর্ণ হয়েছে (HTTP 429)" else "Gemini TTS HTTP $code",
                    isQuotaOrAuth = code == 429 || code == 401 || code == 403
                )
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val b64 = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optJSONObject("inlineData")
                ?.optString("data", "")
                .orEmpty()

            if (b64.isBlank()) {
                return CloudTtsFetch.Failure("Gemini TTS অডিও পাওয়া যায়নি।")
            }

            CloudTtsFetch.Success(
                wavBytes = Base64.decode(b64, Base64.DEFAULT),
                providerLabel = "Gemini 3.8 Neural ($voiceName)",
                resolvedVoice = voiceName
            )
        } catch (e: Exception) {
            CloudTtsFetch.Failure("Gemini TTS নেটওয়ার্ক ত্রুটি (${e.javaClass.simpleName})")
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun playWavBytesSuspending(
        wavBytes: ByteArray,
        settings: SaraSettings
    ): Boolean = withContext(Dispatchers.Main) {
        val tempFile = File(appContext.cacheDir, "sara_active_utterance.wav")
        runCatching { tempFile.writeBytes(wavBytes) }.getOrElse { return@withContext false }

        suspendCancellableCoroutine { cont ->
            releaseMediaPlayerOnly()
            _isSpeaking.value = true

            val mp = MediaPlayer()
            mediaPlayer = mp
            var resumed = false

            fun finishPlay(success: Boolean) {
                if (!resumed) {
                    resumed = true
                    runCatching { mp.release() }
                    if (mediaPlayer === mp) mediaPlayer = null
                    cont.resume(success)
                }
            }

            cont.invokeOnCancellation {
                runCatching {
                    mp.stop()
                    mp.release()
                }
                if (mediaPlayer === mp) mediaPlayer = null
                _isSpeaking.value = false
            }

            runCatching {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(tempFile.absolutePath)
                val vol = settings.voiceVolume.coerceIn(0f, 1f)
                mp.setVolume(vol, vol)
                mp.setOnCompletionListener { finishPlay(true) }
                mp.setOnErrorListener { _, _, _ ->
                    finishPlay(false)
                    true
                }
                mp.prepare()

                // Keep 24kHz neural audio bit-exact at default 1.0x; only resample when user shifts sliders
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val speedDelta = abs(settings.speechSpeed - 1.0f)
                    val pitchDelta = abs(settings.speechPitch - 1.0f)
                    if (speedDelta > 0.03f || pitchDelta > 0.03f) {
                        runCatching {
                            val params = mp.playbackParams
                            if (speedDelta > 0.03f) {
                                params.speed = settings.speechSpeed.coerceIn(0.6f, 1.8f)
                            }
                            if (pitchDelta > 0.03f) {
                                params.pitch = settings.speechPitch.coerceIn(0.88f, 1.14f)
                            }
                            mp.playbackParams = params
                        }
                    }
                }
                if (!mp.isPlaying) {
                    mp.start()
                }
            }.onFailure {
                finishPlay(false)
            }
        }
    }

    private fun speakWithNativeTts(
        text: String,
        settings: SaraSettings,
        onPlaybackFinished: (() -> Unit)? = null
    ): VoicePlaybackResult {
        val engine = tts ?: return VoicePlaybackResult(
            started = false,
            providerUsed = "Android TTS",
            message = "Android TTS ইঞ্জিন প্রস্তুত নয়।"
        )
        if (!_status.value.initialized) {
            return VoicePlaybackResult(
                started = false,
                providerUsed = "Android TTS",
                message = _status.value.unavailableMessage ?: "TTS unavailable."
            )
        }

        requestAssistantAudioFocus()

        val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrElse { emptyList() }
        val bnVoices = allVoices.filter { it.locale.language.equals("bn", ignoreCase = true) }
        val enVoices = allVoices.filter { it.locale.language.equals("en", ignoreCase = true) }

        val bestBnVoice = selectBestVoiceForLocale(bnVoices, settings.voiceGender, settings.femaleVoicePersona)
        val bestEnVoice = selectBestVoiceForLocale(enVoices, settings.voiceGender, settings.femaleVoicePersona)

        val bnFallbackLocale = bestBnVoice?.locale ?: when {
            engine.isLanguageAvailable(Locale("bn", "IN")) >= TextToSpeech.LANG_AVAILABLE -> Locale("bn", "IN")
            engine.isLanguageAvailable(Locale("bn", "BD")) >= TextToSpeech.LANG_AVAILABLE -> Locale("bn", "BD")
            else -> Locale.US
        }

        val segments = splitIntoNaturalSpeechSegments(text, maxChars = 220)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.0f, 1.0f))
        }

        val speedMod = when (settings.femaleVoicePersona) {
            FemaleVoicePersona.LEDA -> -0.06f
            FemaleVoicePersona.SARA_VELVET -> -0.08f
            FemaleVoicePersona.ZEPHYR -> -0.03f
            FemaleVoicePersona.AOEDE -> +0.04f
            FemaleVoicePersona.SARA_SWEET -> +0.02f
            FemaleVoicePersona.KORE -> 0f
        }
        engine.setSpeechRate((settings.speechSpeed + speedMod).coerceIn(0.65f, 1.75f))

        val primaryChosen = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) bestBnVoice else bestEnVoice
        val isVerifiedFemaleVoice = primaryChosen?.let { isExplicitFemaleVoice(it) } == true
        val isConfirmedMaleFallback = primaryChosen?.let { isExplicitMaleVoice(it) } == true

        // Distinct feminine tonal curves per persona so all 6 female voice buttons sound female & unique
        val personaPitchOffset = when (settings.femaleVoicePersona) {
            FemaleVoicePersona.KORE -> 0.06f
            FemaleVoicePersona.ZEPHYR -> 0.10f
            FemaleVoicePersona.AOEDE -> 0.14f
            FemaleVoicePersona.LEDA -> 0.03f
            FemaleVoicePersona.SARA_SWEET -> 0.17f
            FemaleVoicePersona.SARA_VELVET -> 0.01f
        }

        val effectivePitch = when {
            settings.voiceGender == VoiceGenderPreference.MALE -> settings.speechPitch.coerceIn(0.75f, 1.0f)
            isVerifiedFemaleVoice -> (settings.speechPitch + personaPitchOffset).coerceIn(0.95f, 1.32f)
            isConfirmedMaleFallback -> (settings.speechPitch + 0.24f + personaPitchOffset * 0.7f).coerceIn(1.18f, 1.45f)
            else -> (settings.speechPitch + 0.15f + personaPitchOffset * 0.8f).coerceIn(1.08f, 1.38f)
        }
        engine.setPitch(effectivePitch)

        activeCompletionCallback = onPlaybackFinished

        var startedAny = false
        val stamp = System.currentTimeMillis()
        for (idx in segments.indices) {
            val seg = segments[idx]
            val segLang = detectDominantLanguage(seg, settings.voiceLanguage)
            val segVoice = if (segLang == VoiceLanguageOption.BENGALI) bestBnVoice else bestEnVoice
            val segLocale = segVoice?.locale
                ?: if (segLang == VoiceLanguageOption.BENGALI) bnFallbackLocale else Locale.US

            engine.language = segLocale
            if (segVoice != null) {
                engine.voice = segVoice
            }

            val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val isLast = idx == segments.lastIndex
            val utteranceId = if (isLast) "sara_tts_${stamp}_last" else "sara_seg_${stamp}_$idx"
            val code = engine.speak(seg, queueMode, params, utteranceId)
            if (code == TextToSpeech.SUCCESS) {
                startedAny = true
            }
            if (!isLast && settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN) {
                engine.playSilentUtterance(155L, TextToSpeech.QUEUE_ADD, "sara_pause_${stamp}_$idx")
            }
        }

        val voiceLabel = settings.femaleVoicePersona.voiceId
        val nativeId = primaryChosen?.name ?: bnFallbackLocale.displayName
        val notice = when {
            !startedAny -> "Android TTS প্লেব্যাক ব্যর্থ হয়েছে।"
            isVerifiedFemaleVoice -> "বাংলা নারী কণ্ঠ ($voiceLabel · $nativeId) চালু হয়েছে।"
            else -> "নারী কণ্ঠ প্রোফাইল ($voiceLabel) সক্রিয় হয়েছে।"
        }

        return VoicePlaybackResult(
            started = startedAny,
            providerUsed = voiceLabel,
            message = notice
        )
    }

    fun stopPlayback() {
        pendingInitRequest = null
        activeCompletionCallback = null
        activeSpeechJob?.cancel()
        activeSpeechJob = null
        tts?.stop()
        releaseMediaPlayerOnly()
        abandonAudioFocus()
        _isSpeaking.value = false
    }

    private fun releaseMediaPlayerOnly() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    private fun requestAssistantAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            activeFocusRequest = focusReq
            runCatching { audioManager.requestAudioFocus(focusReq) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeFocusRequest?.let { req ->
                runCatching { audioManager.abandonAudioFocusRequest(req) }
            }
            activeFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            runCatching { audioManager.abandonAudioFocus(null) }
        }
    }

    private fun formatNaturalBengaliProsody(
        raw: String,
        quality: BengaliPronunciationQuality
    ): String {
        var cleaned = raw
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("[*_~#>]{1,3}"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Expand common Bengali spoken abbreviations and currency cleanly
        cleaned = cleaned
            .replace(Regex("৳\\s*([0-9০-৯,.]+)"), "$1 টাকা")
            .replace("ডা.", "ডাক্তার")
            .replace("প্রো.", "প্রোফেসর")
            .replace("কি.মি.", "কিলোমিটার")
            .replace("মি.", "মিটার")
            .replace(Regex("([\\u0980-\\u09FF])([A-Za-z0-9])"), "$1 $2")
            .replace(Regex("([A-Za-z0-9])([\\u0980-\\u09FF])"), "$1 $2")

        if (quality == BengaliPronunciationQuality.NATURAL_HUMAN) {
            cleaned = cleaned
                .replace("আসসালামু আলাইকুম!", "আসসালামু আলাইকুম! ")
                .replace("নমস্কার!", "নমস্কার! ")
                .replace("—", ", ")
                .replace("।", "। ")
                .replace("!", "! ")
                .replace("?", "? ")
                .replace(",", ", ")
        }

        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    private fun splitIntoNaturalSpeechSegments(text: String, maxChars: Int = 210): List<String> {
        if (text.length <= maxChars) return listOf(text)

        val sentences = text
            .split(Regex("(?<=[।.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sentences.isEmpty()) return listOf(text)

        val segments = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            if (current.isNotEmpty() && current.length + sentence.length + 1 > maxChars) {
                segments.add(current.toString().trim())
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) {
            segments.add(current.toString().trim())
        }
        return if (segments.isNotEmpty()) segments else listOf(text)
    }

    private fun detectDominantLanguage(text: String, preferred: VoiceLanguageOption): VoiceLanguageOption {
        var bnCount = 0
        var enCount = 0
        for (ch in text) {
            when {
                ch in '\u0980'..'\u09FF' -> bnCount++
                ch in 'A'..'Z' || ch in 'a'..'z' -> enCount++
            }
        }
        return when {
            bnCount == 0 && enCount >= 3 -> VoiceLanguageOption.ENGLISH
            bnCount >= 2 -> VoiceLanguageOption.BENGALI
            else -> preferred
        }
    }

    private fun buildNaturalStylePrompt(
        personaKey: String,
        isBengali: Boolean,
        assistantName: String,
        speed: Float,
        naturalPauses: Boolean
    ): String {
        val paceHint = when {
            speed < 0.9f -> "Speak slightly slower and gently unhurried."
            speed > 1.15f -> "Speak at a lively, crisp natural pace while keeping every syllable clear."
            else -> "Speak at a relaxed, natural human conversational pace."
        }
        val pauseHint = if (naturalPauses) {
            "Include soft natural human breathing pauses between phrases and gentle emotional warmth."
        } else {
            "Keep phrasing direct with clean clarity."
        }

        if (personaKey == "Charon") {
            return if (isBengali) {
                "Warm, friendly adult Bengali-speaking man talking naturally with clear Dhaka/Kolkata Bengali diction. $paceHint $pauseHint"
            } else {
                "Warm, friendly adult male voice speaking natural clear English. $paceHint $pauseHint"
            }
        }

        return when (personaKey) {
            "Zephyr" -> if (isBengali) {
                "Sweet, soft, caring young adult Bengali-speaking woman named $assistantName talking gently in person. Smooth silky tone without metallic artifacts, crystal-clear Bengali pronunciation and fluent English for English words, affectionate warmth, and soft conversational rhythm. $paceHint $pauseHint"
            } else {
                "Sweet, soft, caring adult woman named $assistantName speaking natural conversational English with gentle warmth. $paceHint $pauseHint"
            }
            "Aoede" -> if (isBengali) {
                "Melodic, bright, lively adult Bengali-speaking woman named $assistantName speaking with expressive musical clarity, pleasant warmth, effortless native Bengali pronunciation, and natural intonation. $paceHint $pauseHint"
            } else {
                "Melodic, bright, lively adult woman named $assistantName speaking expressive conversational English with cheerful warmth. $paceHint $pauseHint"
            }
            "Leda" -> if (isBengali) {
                "Calm, affectionate, poised adult Bengali-speaking woman named $assistantName speaking softly with soothing reassurance, refined Bengali diction, and gentle human phrasing. $paceHint $pauseHint"
            } else {
                "Calm, affectionate, poised adult woman named $assistantName speaking softly with soothing reassurance and natural warmth. $paceHint $pauseHint"
            }
            "SaraSweet" -> if (isBengali) {
                "Joyful, sweet, caring best-friend adult Bengali-speaking woman named $assistantName chatting warmly like a real person sitting beside you. Gentle smiling tone, crystal-clear Bengali pronunciation, zero robotic quality. $paceHint $pauseHint"
            } else {
                "Joyful, sweet, caring adult woman named $assistantName chatting warmly like a close friend with natural human cadence. $paceHint $pauseHint"
            }
            "SaraVelvet" -> if (isBengali) {
                "Velvety, serene, deeply soothing adult Bengali-speaking woman named $assistantName with soft breathy warmth, graceful natural intonation, and clear Bengali and English pronunciation. $paceHint $pauseHint"
            } else {
                "Velvety, serene, deeply soothing adult woman named $assistantName with calm graceful pacing and warm human clarity. $paceHint $pauseHint"
            }
            else -> if (isBengali) {
                "Extremely natural, sweet, warm, and gentle adult Bengali-speaking woman named $assistantName speaking in an intimate face-to-face conversation. Smooth human voice with zero metallic or robotic synthesis, crystal-clear native Bengali pronunciation (and fluent natural English for English words), soft emotional expressiveness, and caring personality. $paceHint $pauseHint"
            } else {
                "Extremely natural, sweet, warm, and gentle adult woman named $assistantName speaking fluent conversational English with soft emotional warmth and natural human phrasing. $paceHint $pauseHint"
            }
        }
    }

    private fun readCachedWav(cacheKey: String): ByteArray? {
        val file = File(voiceCacheDir, "$cacheKey.wav")
        if (!file.exists() || file.length() < 64L) return null
        return runCatching {
            file.setLastModified(System.currentTimeMillis())
            file.readBytes()
        }.getOrNull()
    }

    private fun writeCachedWav(cacheKey: String, wavBytes: ByteArray) {
        if (wavBytes.size < 64) return
        runCatching {
            val file = File(voiceCacheDir, "$cacheKey.wav")
            file.writeBytes(wavBytes)

            val existing = voiceCacheDir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
            if (existing.size > 24) {
                existing.take(existing.size - 24).forEach { it.delete() }
            }
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun selectBestVoiceForLocale(
        voices: List<Voice>,
        preference: VoiceGenderPreference,
        persona: FemaleVoicePersona = FemaleVoicePersona.KORE
    ): Voice? {
        if (voices.isEmpty()) return null

        val genderFiltered = when (preference) {
            VoiceGenderPreference.FEMALE -> {
                val explicit = voices.filter { isExplicitFemaleVoice(it) }
                if (explicit.isNotEmpty()) {
                    explicit
                } else {
                    voices.filterNot { isExplicitMaleVoice(it) }
                }
            }
            VoiceGenderPreference.MALE -> voices.filter { isExplicitMaleVoice(it) }
            VoiceGenderPreference.SYSTEM_DEFAULT -> voices
        }

        val pool = if (genderFiltered.isNotEmpty()) genderFiltered else voices
        val ranked = pool.sortedByDescending { voice ->
            var score = 0
            if (preference == VoiceGenderPreference.FEMALE) {
                if (isExplicitFemaleVoice(voice)) score += 240
                if (isExplicitMaleVoice(voice)) score -= 500
            } else if (preference == VoiceGenderPreference.MALE) {
                if (isExplicitMaleVoice(voice)) score += 240
            }
            if (voice.locale.country.equals("BD", ignoreCase = true)) score += 30
            else if (voice.locale.country.equals("IN", ignoreCase = true)) score += 25
            else if (voice.locale.country.equals("US", ignoreCase = true)) score += 20
            if (voice.isNetworkConnectionRequired || isHighQualityOrNetworkVoice(voice)) score += 55
            if (voice.quality >= Voice.QUALITY_VERY_HIGH) score += 40
            else if (voice.quality >= Voice.QUALITY_HIGH) score += 25
            score
        }

        if (preference == VoiceGenderPreference.FEMALE) {
            val distinctFemalePool = ranked.filter { isExplicitFemaleVoice(it) }
            if (distinctFemalePool.size > 1) {
                return distinctFemalePool[persona.ordinal % distinctFemalePool.size]
            }
        }
        return ranked.firstOrNull()
    }

    private fun isHighQualityOrNetworkVoice(voice: Voice): Boolean {
        val n = voice.name.lowercase(Locale.ROOT)
        return voice.isNetworkConnectionRequired ||
            voice.quality >= Voice.QUALITY_HIGH ||
            n.contains("network") ||
            n.contains("neural") ||
            n.contains("wavenet")
    }

    private fun isExplicitMaleVoice(voice: Voice): Boolean {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val featureText = voice.features?.joinToString(",")?.lowercase(Locale.ROOT).orEmpty()
        return Regex("(^|[-_#\\s])male($|[-_#\\s])").containsMatchIn(nameLower) ||
            nameLower.contains("-bnm-") ||
            nameLower.contains("-bnc-") ||
            nameLower.contains("-bnd-") ||
            nameLower.contains("-ban-") ||
            nameLower.contains("-bab-") ||
            nameLower.contains("-bac-") ||
            nameLower.contains("-bad-") ||
            nameLower.contains("-bar-") ||
            nameLower.contains("-m-") ||
            nameLower.contains("bashkar") ||
            nameLower.contains("pradeep") ||
            nameLower.contains("tanmoy") ||
            Regex("(^|[,\\s=])male($|[,\\s])").containsMatchIn(featureText)
    }

    private fun isExplicitFemaleVoice(voice: Voice): Boolean {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val featureText = voice.features?.joinToString(",")?.lowercase(Locale.ROOT).orEmpty()
        if (isExplicitMaleVoice(voice)) return false
        return nameLower.contains("female") ||
            nameLower.contains("-bnf-") ||
            nameLower.contains("-bne-") ||
            nameLower.contains("-bna-") ||
            nameLower.contains("-baf-") ||
            nameLower.contains("-baa-") ||
            nameLower.contains("-sfg-") ||
            nameLower.contains("-tpf-") ||
            nameLower.contains("-f-") ||
            nameLower.contains("tanishaa") ||
            nameLower.contains("nabanita") ||
            nameLower.contains("kalpana") ||
            nameLower.contains("heera") ||
            featureText.contains("female")
    }

    fun shutdown() {
        stopPlayback()
        tts?.shutdown()
        tts = null
        scope.cancel()
    }
}
