package com.sara.assistant.system

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.VoiceGenderPreference
import com.sara.assistant.data.settings.VoiceLanguageOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceEngineStatus(
    val initialized: Boolean = false,
    val unavailableMessage: String? = null,
    val bengaliSupported: Boolean = false,
    val femaleVoiceAvailable: Boolean = false,
    val maleVoiceAvailable: Boolean = false
)

/**
 * Real Android TextToSpeech wrapper supporting Bengali/English voices, speed, pitch, volume,
 * and genuine device voice capability inspection.
 */
class SaraVoiceEngine(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _status = MutableStateFlow(VoiceEngineStatus())
    val status: StateFlow<VoiceEngineStatus> = _status.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(statusCode: Int) {
        val engine = tts
        if (statusCode != TextToSpeech.SUCCESS || engine == null) {
            _status.value = VoiceEngineStatus(
                initialized = false,
                unavailableMessage = "Text-to-Speech engine unavailable on this device."
            )
            return
        }

        val bnResult = engine.isLanguageAvailable(Locale("bn", "BD"))
        val bnSupported = bnResult >= TextToSpeech.LANG_AVAILABLE

        val voices = runCatching { engine.voices?.toList().orEmpty() }.getOrElse { emptyList() }
        val hasFemale = voices.any { isGenderMatch(it, VoiceGenderPreference.FEMALE) }
        val hasMale = voices.any { isGenderMatch(it, VoiceGenderPreference.MALE) }

        _status.value = VoiceEngineStatus(
            initialized = true,
            unavailableMessage = null,
            bengaliSupported = bnSupported,
            femaleVoiceAvailable = hasFemale,
            maleVoiceAvailable = hasMale
        )
    }

    fun speakTest(settings: SaraSettings): Boolean {
        val text = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            "নমস্কার, আমি ${settings.assistantName}। আপনার ভয়েস সেটিংস সঠিকভাবে কাজ করছে।"
        } else {
            "Hello, I am ${settings.assistantName}. Your voice settings are active and ready."
        }
        return speakInternal(text, settings)
    }

    fun speakIfEnabled(text: String, settings: SaraSettings) {
        if (!settings.voiceResponsesEnabled) return
        speakInternal(text, settings)
    }

    private fun speakInternal(text: String, settings: SaraSettings): Boolean {
        val engine = tts ?: return false
        if (!_status.value.initialized) return false

        val targetLocale = if (settings.voiceLanguage == VoiceLanguageOption.BENGALI) {
            if (_status.value.bengaliSupported) Locale("bn", "BD") else Locale.US
        } else {
            Locale.US
        }

        engine.language = targetLocale
        engine.setSpeechRate(settings.speechSpeed.coerceIn(0.5f, 2.0f))
        engine.setPitch(settings.speechPitch.coerceIn(0.5f, 2.0f))

        if (settings.voiceGender != VoiceGenderPreference.SYSTEM_DEFAULT) {
            val matchingVoice = runCatching {
                engine.voices
                    ?.filter { it.locale.language == targetLocale.language }
                    ?.firstOrNull { isGenderMatch(it, settings.voiceGender) }
            }.getOrNull()
            if (matchingVoice != null) {
                engine.voice = matchingVoice
            }
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.0f, 1.0f))
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sara_tts_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    private fun isGenderMatch(voice: Voice, preference: VoiceGenderPreference): Boolean {
        val nameLower = voice.name.lowercase(Locale.ROOT)
        val featureText = voice.features?.joinToString(",")?.lowercase(Locale.ROOT).orEmpty()
        return when (preference) {
            VoiceGenderPreference.FEMALE ->
                nameLower.contains("female") || nameLower.contains("-f-") || nameLower.endsWith("f") || featureText.contains("female")
            VoiceGenderPreference.MALE ->
                nameLower.contains("male") || nameLower.contains("-m-") || nameLower.endsWith("m") || featureText.contains("male")
            VoiceGenderPreference.SYSTEM_DEFAULT -> true
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
