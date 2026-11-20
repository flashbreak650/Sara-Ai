package com.sara.assistant.system

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import com.sara.assistant.data.settings.BengaliPronunciationQuality
import com.sara.assistant.data.settings.SaraSettings
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
    val maleVoiceAvailable: Boolean = false,
    val selectedNativeMaleVoiceName: String? = null,
    val bengaliVoiceInventoryCount: Int = 0,
    val honestMaleLimitationNote: String? = null
)

data class VoicePlaybackResult(
    val started: Boolean,
    val providerUsed: String,
    val message: String
)

/**
 * Dual-mode SARA Voice Engine:
 * 1. Cloud Neural Male Voice (Gemini 3.8 TTS 'Charon' via backend /api/tts) for genuinely warm human-like Bengali male speech.
 * 2. On-Device Android TextToSpeech with honest voice gender inspection (never fakes a male voice via pitch distortion alone).
 */
class SaraVoiceEngine(context: Context) : TextToSpeech.OnInitListener {

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
                unavailableMessage = "On-device Text-to-Speech engine unavailable. Use Cloud Neural TTS for Bengali male speech."
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

        val bengaliMaleVoice = bengaliVoices.firstOrNull { isExplicitMaleVoice(it) }
        val bengaliFemaleVoice = bengaliVoices.firstOrNull { isExplicitFemaleVoice(it) }

        val anyMaleVoice = bengaliMaleVoice ?: allVoices.firstOrNull { isExplicitMaleVoice(it) }
        val anyFemaleVoice = bengaliFemaleVoice ?: allVoices.firstOrNull { isExplicitFemaleVoice(it) }

        val limitationNote = if (bengaliMaleVoice == null) {
            "এই ফোনের অফলাইন TTS ইঞ্জিনে কোনো প্রাকৃতিক বাংলা পুরুষ কণ্ঠ (Male Bengali Voice) পাওয়া যায়নি। সত্যিকারের উষ্ণ ও মানবিক বাংলা পুরুষ কণ্ঠের জন্য Cloud Neural TTS (Gemini 3.8 'Charon' / Google Cloud WaveNet Male) ব্যবহার করুন।"
        } else {
            null
        }

        _status.value = VoiceEngineStatus(
            initialized = true,
            unavailableMessage = null,
            bengaliSupported = bnSupported,
            femaleVoiceAvailable = anyFemaleVoice != null,
            maleVoiceAvailable = bengaliMaleVoice != null,
            selectedNativeMaleVoiceName = bengaliMaleVoice?.name,
            bengaliVoiceInventoryCount = bengaliVoices.size,
            honestMaleLimitationNote = limitationNote
        )
    }

    fun speakTest(
        settings: SaraSettings,
        onComplete: (VoicePlaybackResult) -> Unit = {}
    ) {
        val sampleText = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            "আসসালামু আলাইকুম! আমি ${settings.assistantName}। আপনার ব্যক্তিগত এআই সহকারী হিসেবে পাশে আছি।"
        } else {
            "Hello! I am ${settings.assistantName}, your personal AI assistant. My voice settings are ready."
        }
        speakText(sampleText, settings, onComplete)
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
        val cleanText = formatBengaliCadence(text, settings.bengaliPronunciationQuality)

        val effectiveCloudUrl = settings.cloudTtsEndpointUrl.trim().ifBlank {
            val proxy = settings.apiProxyUrl.trim()
            if (proxy.endsWith("/api/tts")) proxy else ""
        }

        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_MALE && effectiveCloudUrl.isNotEmpty()) {
            scope.launch {
                val cloudResult = playCloudNeuralVoice(effectiveCloudUrl, cleanText, settings)
                if (cloudResult.started) {
                    onComplete(cloudResult)
                } else {
                    val nativeFallback = speakWithNativeTts(cleanText, settings)
                    onComplete(
                        if (nativeFallback.started) {
                            nativeFallback.copy(
                                message = "${cloudResult.message} — ডিভাইস TTS দিয়ে শোনানো হয়েছে।"
                            )
                        } else {
                            cloudResult
                        }
                    )
                }
            }
            return
        }

        val nativeResult = speakWithNativeTts(cleanText, settings)
        if (settings.voiceProvider == VoiceProviderMode.CLOUD_NEURAL_MALE && effectiveCloudUrl.isEmpty()) {
            onComplete(
                nativeResult.copy(
                    message = if (_status.value.maleVoiceAvailable) {
                        "Cloud TTS URL সেট করা নেই; ডিভাইসের বাংলা পুরুষ কণ্ঠে শোনানো হচ্ছে।"
                    } else {
                        "Cloud Neural TTS URL সেট করা নেই এবং ফোনে প্রাকৃতিক বাংলা পুরুষ কণ্ঠ নেই। Cloud TTS URL যোগ করুন।"
                    }
                )
            )
        } else {
            onComplete(nativeResult)
        }
    }

    private suspend fun playCloudNeuralVoice(
        endpointUrl: String,
        text: String,
        settings: SaraSettings
    ): VoicePlaybackResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("text", text)
                put("language", if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) "bn" else "en")
                put(
                    "voiceGender",
                    when (settings.voiceGender) {
                        VoiceGenderPreference.FEMALE -> "female"
                        VoiceGenderPreference.MALE, VoiceGenderPreference.SYSTEM_DEFAULT -> "male"
                    }
                )
                put("assistantName", settings.assistantName)
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
            val tempFile = File(appContext.cacheDir, "sara_neural_voice.wav")
            tempFile.writeBytes(wavBytes)

            withContext(Dispatchers.Main) {
                stopPlayback()
                _isSpeaking.value = true
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setVolume(settings.voiceVolume.coerceIn(0f, 1f), settings.voiceVolume.coerceIn(0f, 1f))
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

            val voiceName = json.optString("voiceName", "Charon")
            VoicePlaybackResult(
                started = true,
                providerUsed = "Gemini Cloud Neural ($voiceName)",
                message = "প্রাকৃতিক নিউরাল পুরুষ কণ্ঠ ($voiceName) চালু হয়েছে।"
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

        engine.language = targetLocale
        engine.setSpeechRate(settings.speechSpeed.coerceIn(0.5f, 2.0f))
        // Keep natural pitch unless user intentionally tunes it; never fake male voice with harsh pitch drop.
        engine.setPitch(settings.speechPitch.coerceIn(0.75f, 1.5f))

        val voicesForLocale = runCatching {
            engine.voices?.filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }.orEmpty()
        }.getOrElse { emptyList() }

        val chosenVoice: Voice? = when (settings.voiceGender) {
            VoiceGenderPreference.MALE -> voicesForLocale.firstOrNull { isExplicitMaleVoice(it) }
            VoiceGenderPreference.FEMALE -> voicesForLocale.firstOrNull { isExplicitFemaleVoice(it) }
            VoiceGenderPreference.SYSTEM_DEFAULT -> null
        }

        if (chosenVoice != null) {
            engine.voice = chosenVoice
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.0f, 1.0f))
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sara_tts_${System.currentTimeMillis()}")

        val notice = when {
            result != TextToSpeech.SUCCESS -> "Android TTS প্লেব্যাক ব্যর্থ হয়েছে।"
            settings.voiceGender == VoiceGenderPreference.MALE && chosenVoice == null ->
                "এই ডিভাইসের অফলাইন TTS-এ আসল বাংলা পুরুষ কণ্ঠ নেই; স্বাভাবিক কণ্ঠে পড়ে শোনানো হচ্ছে।"
            else -> "ডিভাইস TTS (${chosenVoice?.name ?: targetLocale.displayLanguage}) চালু হয়েছে।"
        }

        return VoicePlaybackResult(
            started = result == TextToSpeech.SUCCESS,
            providerUsed = chosenVoice?.name ?: "Android System TTS",
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

    private fun formatBengaliCadence(raw: String, quality: BengaliPronunciationQuality): String {
        if (quality != BengaliPronunciationQuality.NATURAL_HUMAN) return raw
        return raw
            .replace("।", "। ")
            .replace("!", "! ")
            .replace("?", "? ")
            .replace(",", ", ")
            .replace(Regex("\\s+"), " ")
            .trim()
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
