package com.sara.assistant.system

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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

/**
 * Natural Human Female Bengali Voice Engine for SARA.
 *
 * Tier 1: Gemini 3.8 Neural TTS ('gemini-3.8-flash-tts', voices 'Kore' / 'Zephyr')
 *   - Supports backend /api/tts proxy OR direct Keystore-secured v1beta/interactions REST.
 *   - Produces natural human breathing pauses, warm conversational intonation, and crystal-clear Bengali & English.
 * Tier 2: Highest-Quality Android Device TTS Female Bengali Voice
 *   - Ranks Google network/neural Bengali female voices (bn-BD / bn-IN) ahead of basic offline local voices.
 *   - Applies natural Bengali sentence rhythm & pause formatting without harsh pitch distortion.
 */
class SaraVoiceEngine(
    context: Context,
    private val secureKeyStorage: SecureKeyStorage? = null
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

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
            return
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
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
        val neuralFemaleAvailable = bestFemaleBengali?.let { isHighQualityOrNetworkVoice(it) } == true

        val qualityNote = when {
            bestFemaleBengali == null && bengaliVoices.isEmpty() ->
                "এই ডিভাইসে অফলাইন বাংলা ভয়েস প্যাক পাওয়া যায়নি। সবচেয়ে স্বাভাবিক মানবিক বাংলা নারী কণ্ঠের জন্য Cloud Neural Female (Gemini TTS) সক্রিয় রাখুন।"
            !neuralFemaleAvailable ->
                "অত্যন্ত মিষ্টি ও বাস্তব মানুষের মতো বাংলা নারী কণ্ঠের জন্য Cloud Neural Female (Kore / Zephyr) ব্যবহার করা উত্তম।"
            else ->
                "ডিভাইসে উচ্চমানের বাংলা নারী নেটওয়ার্ক ভয়েস (${bestFemaleBengali?.name}) পাওয়া গেছে; ক্লাউড নিউরাল কণ্ঠ আরো বেশি মানবিক ও স্বাভাবিক।"
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
            persona.previewSentenceBn
        } else {
            persona.previewSentenceEn
        }
        speakText(sampleText, previewSettings, onComplete)
    }

    fun speakIfEnabled(
        text: String,
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        if (!settings.voiceResponsesEnabled) return
        speakText(text, settings, onComplete)
    }

    fun speakText(
        text: String,
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        val cleanText = formatNaturalBengaliProsody(text, settings.bengaliPronunciationQuality)
        val endpointUrl = resolveCloudTtsEndpoint(settings)
        val directApiKey = secureKeyStorage?.decryptApiKeyOrNull()?.takeIf { it.isNotBlank() }

        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE) {
            if (endpointUrl.isNotEmpty()) {
                scope.launch {
                    val cloudResult = playViaBackendTts(endpointUrl, cleanText, settings)
                    if (cloudResult.started) {
                        onComplete(cloudResult)
                    } else if (directApiKey != null) {
                        val directResult = playViaDirectGeminiTts(directApiKey, cleanText, settings)
                        finishWithFallbackIfNeeded(directResult, cleanText, settings, onComplete)
                    } else {
                        finishWithFallbackIfNeeded(cloudResult, cleanText, settings, onComplete)
                    }
                }
                return
            } else if (directApiKey != null) {
                scope.launch {
                    val directResult = playViaDirectGeminiTts(directApiKey, cleanText, settings)
                    finishWithFallbackIfNeeded(directResult, cleanText, settings, onComplete)
                }
                return
            }
        }

        val nativeResult = speakWithNativeTts(cleanText, settings)
        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_FEMALE && endpointUrl.isEmpty() && directApiKey == null) {
            onComplete(
                nativeResult.copy(
                    message = if (nativeResult.started) {
                        "${nativeResult.message} (সর্বোচ্চ মানবিক নিউরাল নারী কণ্ঠের জন্য Settings > Cloud TTS URL বা API Key যোগ করুন)"
                    } else {
                        nativeResult.message
                    }
                )
            )
        } else {
            onComplete(nativeResult)
        }
    }

    private fun finishWithFallbackIfNeeded(
        cloudResult: VoicePlaybackResult,
        cleanText: String,
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit
    ) {
        if (cloudResult.started) {
            onComplete(cloudResult)
            return
        }
        val nativeFallback = speakWithNativeTts(cleanText, settings)
        onComplete(
            if (nativeFallback.started) {
                nativeFallback.copy(
                    message = "${cloudResult.message} — ডিভাইসের সেরা বাংলা নারী কণ্ঠে শোনানো হয়েছে।"
                )
            } else {
                cloudResult
            }
        )
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

    private suspend fun playViaBackendTts(
        endpointUrl: String,
        text: String,
        settings: SaraSettings
    ): VoicePlaybackResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 25000
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
                put("language", if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) "bn" else "en")
                put("voiceGender", if (settings.voiceGender == VoiceGenderPreference.MALE) "male" else "female")
                put("voiceName", voiceName)
                put("assistantName", settings.assistantName.ifBlank { "SARA" })
                put("naturalPauses", settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext VoicePlaybackResult(
                    started = false,
                    providerUsed = "Cloud Neural TTS",
                    message = "Cloud TTS HTTP $code ত্রুটি"
                )
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val audioB64 = json.optString("audioBase64", "")
            if (audioB64.isBlank()) {
                return@withContext VoicePlaybackResult(
                    started = false,
                    providerUsed = "Cloud Neural TTS",
                    message = "Cloud TTS অডিও পাওয়া যায়নি"
                )
            }

            val wavBytes = Base64.decode(audioB64, Base64.DEFAULT)
            playWavBytes(wavBytes, settings)

            val resolvedVoice = json.optString("voiceName", voiceName)
            VoicePlaybackResult(
                started = true,
                providerUsed = "Gemini Neural Female ($resolvedVoice)",
                message = "প্রাকৃতিক মানবিক নারী কণ্ঠ ($resolvedVoice) চালু হয়েছে।"
            )
        } catch (e: Exception) {
            VoicePlaybackResult(
                started = false,
                providerUsed = "Cloud Neural TTS",
                message = "Cloud TTS সংযোগ ব্যর্থ (${e.javaClass.simpleName})"
            )
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun playViaDirectGeminiTts(
        apiKey: String,
        text: String,
        settings: SaraSettings
    ): VoicePlaybackResult = withContext(Dispatchers.IO) {
        val isBn = settings.voiceLanguage == VoiceLanguageOption.BENGALI
        val voiceName = if (settings.voiceGender == VoiceGenderPreference.MALE) {
            "Charon"
        } else {
            settings.femaleVoicePersona.voiceId
        }
        val useBreaths = settings.bengaliPronunciationQuality == BengaliPronunciationQuality.NATURAL_HUMAN
        val styledText = if (useBreaths) formatBreathTags(text, isBn) else text
        val styleDesc = buildNaturalStylePrompt(voiceName, isBn, settings.assistantName.ifBlank { "SARA" })
        val speechLang = if (isBn) "bn-BD" else "en-US"

        val primary = callGeminiInteractionsTts(
            apiKey = apiKey,
            model = "gemini-3.8-flash-tts",
            text = styledText,
            style = styleDesc,
            language = speechLang,
            voiceName = voiceName
        )
        val audioBytes = primary ?: callGeminiInteractionsTts(
            apiKey = apiKey,
            model = "gemini-3.8-flash-lite-tts",
            text = text,
            style = styleDesc,
            language = speechLang,
            voiceName = voiceName
        )

        if (audioBytes == null || audioBytes.isEmpty()) {
            return@withContext VoicePlaybackResult(
                started = false,
                providerUsed = "Gemini Direct Neural TTS",
                message = "Gemini TTS অডিও পাওয়া যায়নি।"
            )
        }

        playWavBytes(audioBytes, settings)
        VoicePlaybackResult(
            started = true,
            providerUsed = "Gemini 3.8 Neural ($voiceName)",
            message = "স্বাভাবিক মানবিক নারী কণ্ঠ ($voiceName) চালু হয়েছে।"
        )
    }

    private fun callGeminiInteractionsTts(
        apiKey: String,
        model: String,
        text: String,
        style: String,
        language: String,
        voiceName: String
    ): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/interactions"
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            val bodyJson = JSONObject().apply {
                put("model", model)
                put(
                    "input",
                    JSONArray().put(
                        JSONObject().apply {
                            put("type", "text")
                            put("text", text)
                            put(
                                "annotations",
                                JSONArray().put(
                                    JSONObject().apply {
                                        put("type", "speech_metadata")
                                        put("style", style)
                                    }
                                )
                            )
                        }
                    )
                )
                put(
                    "response_format",
                    JSONObject().put("type", "audio")
                )
                put(
                    "generation_config",
                    JSONObject().put(
                        "speech_config",
                        JSONArray().put(
                            JSONObject().apply {
                                put("language", language)
                                put("voice", voiceName)
                            }
                        )
                    )
                )
            }

            OutputStreamWriter(conn.outputStream).use { it.write(bodyJson.toString()) }
            if (conn.responseCode !in 200..299) return null

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val b64 = extractAudioBase64(root) ?: return null
            Base64.decode(b64, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractAudioBase64(root: JSONObject): String? {
        val outputs = root.optJSONArray("outputs")
        if (outputs != null) {
            for (i in 0 until outputs.length()) {
                val item = outputs.optJSONObject(i) ?: continue
                if (item.optString("type") == "audio") {
                    val data = item.optString("data", "")
                    if (data.isNotBlank()) return data
                }
            }
        }
        val steps = root.optJSONArray("steps")
        if (steps != null) {
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i) ?: continue
                val content = step.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val part = content.optJSONObject(j) ?: continue
                    if (part.optString("type") == "audio") {
                        val data = part.optString("data", "")
                        if (data.isNotBlank()) return data
                    }
                }
            }
        }
        return null
    }

    private suspend fun playWavBytes(wavBytes: ByteArray, settings: SaraSettings) {
        val tempFile = File(appContext.cacheDir, "sara_neural_female_voice.wav")
        tempFile.writeBytes(wavBytes)

        withContext(Dispatchers.Main) {
            stopPlayback()
            _isSpeaking.value = true
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                val vol = settings.voiceVolume.coerceIn(0f, 1f)
                setVolume(vol, vol)
                setOnCompletionListener { mp ->
                    _isSpeaking.value = false
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    _isSpeaking.value = false
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                    true
                }
                prepare()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    runCatching {
                        playbackParams = playbackParams.setSpeed(settings.speechSpeed.coerceIn(0.6f, 1.8f))
                    }
                }
                start()
            }
        }
    }

    private fun speakWithNativeTts(text: String, settings: SaraSettings): VoicePlaybackResult {
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

        val targetLocale = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            when {
                engine.isLanguageAvailable(Locale("bn", "BD")) >= TextToSpeech.LANG_AVAILABLE -> Locale("bn", "BD")
                engine.isLanguageAvailable(Locale("bn", "IN")) >= TextToSpeech.LANG_AVAILABLE -> Locale("bn", "IN")
                else -> Locale.US
            }
        } else {
            Locale.US
        }

        val allLocaleVoices = runCatching {
            engine.voices?.filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }.orEmpty()
        }.getOrElse { emptyList() }

        val chosenVoice = selectBestVoiceForLocale(allLocaleVoices, settings.voiceGender)
        engine.language = chosenVoice?.locale ?: targetLocale
        if (chosenVoice != null) {
            engine.voice = chosenVoice
        }

        // Smooth natural speed and natural pitch (never fake voice gender via artificial pitch distortion)
        engine.setSpeechRate(settings.speechSpeed.coerceIn(0.6f, 1.8f))
        engine.setPitch(settings.speechPitch.coerceIn(0.80f, 1.35f))

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.0f, 1.0f))
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sara_tts_${System.currentTimeMillis()}")

        val isNeural = chosenVoice?.let { isHighQualityOrNetworkVoice(it) } == true
        val voiceLabel = chosenVoice?.name ?: targetLocale.displayName
        val notice = when {
            result != TextToSpeech.SUCCESS -> "Android TTS প্লেব্যাক ব্যর্থ হয়েছে।"
            isNeural -> "উচ্চমানের বাংলা নারী নেটওয়ার্ক ভয়েস ($voiceLabel) চালু হয়েছে।"
            else -> "ডিভাইস অফলাইন নারী TTS ($voiceLabel) চালু হয়েছে।"
        }

        return VoicePlaybackResult(
            started = result == TextToSpeech.SUCCESS,
            providerUsed = voiceLabel,
            message = notice
        )
    }

    fun stopPlayback() {
        tts?.stop()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _isSpeaking.value = false
    }

    private fun formatNaturalBengaliProsody(raw: String, quality: BengaliPronunciationQuality): String {
        val trimmed = raw.replace(Regex("\\s+"), " ").trim()
        if (quality != BengaliPronunciationQuality.NATURAL_HUMAN) return trimmed
        return trimmed
            .replace("আসসালামু আলাইকুম!", "আসসালামু আলাইকুম! ")
            .replace("নমস্কার!", "নমস্কার! ")
            .replace("।", "। ")
            .replace("!", "! ")
            .replace("?", "? ")
            .replace(",", ", ")
            .replace("—", " — ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatBreathTags(text: String, isBengali: Boolean): String {
        return if (isBengali) {
            text
                .replace("আসসালামু আলাইকুম!", "আসসালামু আলাইকুম! <breath>")
                .replace("নমস্কার!", "নমস্কার! <breath>")
                .replace(Regex("।\\s+"), "। <breath> ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } else {
            text
                .replace(Regex("\\.\\s+"), ". <breath> ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private fun buildNaturalStylePrompt(voiceName: String, isBengali: Boolean, assistantName: String): String {
        if (voiceName == "Charon") {
            return if (isBengali) {
                "Warm, friendly adult male voice speaking natural, clear educated Bengali with gentle pacing and human pauses."
            } else {
                "Warm, friendly adult male voice speaking natural clear English with conversational pacing."
            }
        }
        if (voiceName == "Zephyr") {
            return if (isBengali) {
                "Sweet, soft, caring young adult Bengali-speaking woman ($assistantName) talking gently in person with crystal-clear Bengali diction, warm tone, natural breathing pauses, and zero robotic stiffness."
            } else {
                "Sweet, soft, caring adult woman ($assistantName) speaking natural conversational English with gentle warmth and human breathing pauses."
            }
        }
        return if (isBengali) {
            "Extremely natural, sweet, warm, and gentle adult Bengali-speaking woman ($assistantName) talking like a caring real person in face-to-face conversation. Smooth velvety tone without metallic artifacts, crystal-clear Bengali pronunciation, soft emotional expressiveness, and natural breathing pauses."
        } else {
            "Extremely natural, sweet, warm, and gentle adult woman ($assistantName) speaking fluent conversational English with soft emotional warmth and natural human breathing pauses."
        }
    }

    private fun selectBestVoiceForLocale(
        voices: List<Voice>,
        preference: VoiceGenderPreference
    ): Voice? {
        if (voices.isEmpty()) return null

        val genderFiltered = when (preference) {
            VoiceGenderPreference.FEMALE -> {
                val explicit = voices.filter { isExplicitFemaleVoice(it) }
                if (explicit.isNotEmpty()) explicit else voices.filterNot { isExplicitMaleVoice(it) }
            }
            VoiceGenderPreference.MALE -> voices.filter { isExplicitMaleVoice(it) }
            VoiceGenderPreference.SYSTEM_DEFAULT -> voices
        }

        val candidates = if (genderFiltered.isNotEmpty()) genderFiltered else voices
        return candidates.maxByOrNull { voice ->
            var score = 0
            if (voice.locale.country.equals("BD", ignoreCase = true)) score += 40
            else if (voice.locale.country.equals("IN", ignoreCase = true)) score += 20
            if (voice.isNetworkConnectionRequired || isHighQualityOrNetworkVoice(voice)) score += 50
            score += (voice.quality / 10)
            if (isExplicitFemaleVoice(voice) && preference == VoiceGenderPreference.FEMALE) score += 25
            score
        }
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
        return nameLower.contains("male") ||
            nameLower.contains("-bnm-") ||
            nameLower.contains("-ban-") ||
            nameLower.contains("-bnc-") ||
            nameLower.contains("-m-") ||
            featureText.contains("male")
    }

    private fun isExplicitFemaleVoice(voice: Voice): Boolean {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val featureText = voice.features?.joinToString(",")?.lowercase(Locale.ROOT).orEmpty()
        return nameLower.contains("female") ||
            nameLower.contains("-bnf-") ||
            nameLower.contains("-bnd-") ||
            nameLower.contains("-f-") ||
            featureText.contains("female")
    }

    fun shutdown() {
        stopPlayback()
        tts?.shutdown()
        tts = null
        scope.cancel()
    }
}
