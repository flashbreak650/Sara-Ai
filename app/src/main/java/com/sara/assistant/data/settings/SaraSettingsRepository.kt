package com.sara.assistant.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Single source of truth for persisted SARA preferences, assistant profile, memories, and conversations.
 */
class SaraSettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val secureKeyStorage = SecureKeyStorage(context.applicationContext)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SaraSettings> = _settings.asStateFlow()

    private val _memories = MutableStateFlow(loadMemories())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _conversationHistory = MutableStateFlow(loadConversationHistory())
    val conversationHistory: StateFlow<List<ConversationEntry>> = _conversationHistory.asStateFlow()

    fun updateSettings(transform: (SaraSettings) -> SaraSettings) {
        val updated = transform(_settings.value).copy(
            hasStoredApiKey = secureKeyStorage.hasApiKey()
        )
        _settings.value = updated
        persistSettings(updated)
    }

    fun saveApiKey(rawKey: String): Boolean {
        val saved = secureKeyStorage.saveApiKey(rawKey)
        if (saved) {
            _settings.value = _settings.value.copy(hasStoredApiKey = true)
        }
        return saved
    }

    fun clearApiKey() {
        secureKeyStorage.clearApiKey()
        _settings.value = _settings.value.copy(hasStoredApiKey = false)
    }

    fun addMemory(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || !_settings.value.memoryEnabled) return
        val item = MemoryItem(
            id = UUID.randomUUID().toString(),
            content = clean,
            createdAtMillis = System.currentTimeMillis()
        )
        val next = listOf(item) + _memories.value
        _memories.value = next
        persistMemories(next)
    }

    fun deleteMemory(id: String) {
        val next = _memories.value.filterNot { it.id == id }
        _memories.value = next
        persistMemories(next)
    }

    fun recordConversationMessage(text: String, isAssistant: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val entry = ConversationEntry(
            id = UUID.randomUUID().toString(),
            message = clean,
            timestampMillis = System.currentTimeMillis(),
            isAssistant = isAssistant
        )
        val next = (listOf(entry) + _conversationHistory.value).take(50)
        _conversationHistory.value = next
        persistConversationHistory(next)
    }

    fun clearConversationHistory() {
        _conversationHistory.value = emptyList()
        prefs.edit().remove(KEY_CONVERSATIONS).apply()
    }

    fun clearAllPersonalData() {
        _memories.value = emptyList()
        _conversationHistory.value = emptyList()
        secureKeyStorage.clearApiKey()
        prefs.edit()
            .remove(KEY_MEMORIES)
            .remove(KEY_CONVERSATIONS)
            .remove(KEY_API_PROXY_URL)
            .remove(KEY_CLOUD_TTS_URL)
            .apply()

        updateSettings {
            it.copy(
                assistantName = "SARA",
                assistantSubtitle = "Your Personal AI",
                customWakeWord = "Hey SARA",
                apiProxyUrl = "",
                cloudTtsEndpointUrl = "",
                hasStoredApiKey = false
            )
        }
    }

    private fun loadSettings(): SaraSettings {
        val defaults = SaraSettings()
        return SaraSettings(
            assistantName = prefs.getString(KEY_ASSISTANT_NAME, defaults.assistantName) ?: defaults.assistantName,
            assistantSubtitle = prefs.getString(KEY_ASSISTANT_SUBTITLE, defaults.assistantSubtitle)
                ?: defaults.assistantSubtitle,
            personalityDescription = prefs.getString(KEY_PERSONALITY_DESC, defaults.personalityDescription)
                ?: defaults.personalityDescription,
            personalityMode = enumOrDefault(prefs.getString(KEY_PERSONALITY_MODE, null), defaults.personalityMode),
            responseStyle = enumOrDefault(prefs.getString(KEY_RESPONSE_STYLE, null), defaults.responseStyle),
            answerLength = enumOrDefault(prefs.getString(KEY_ANSWER_LENGTH, null), defaults.answerLength),

            voiceResponsesEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, defaults.voiceResponsesEnabled),
            voiceProvider = loadVoiceProvider(prefs.getString(KEY_VOICE_PROVIDER, null), defaults.voiceProvider),
            voiceGender = enumOrDefault(prefs.getString(KEY_VOICE_GENDER, null), defaults.voiceGender),
            femaleVoicePersona = enumOrDefault(prefs.getString(KEY_VOICE_PERSONA, null), defaults.femaleVoicePersona),
            speechSpeed = prefs.getFloat(KEY_SPEECH_SPEED, defaults.speechSpeed).coerceIn(0.5f, 2.0f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, defaults.speechPitch).coerceIn(0.5f, 2.0f),
            voiceLanguage = enumOrDefault(prefs.getString(KEY_VOICE_LANGUAGE, null), defaults.voiceLanguage),
            voiceVolume = prefs.getFloat(KEY_VOICE_VOLUME, defaults.voiceVolume).coerceIn(0.0f, 1.0f),
            bengaliPronunciationQuality = enumOrDefault(
                prefs.getString(KEY_BN_PRONUNCIATION, null),
                defaults.bengaliPronunciationQuality
            ),
            cloudTtsEndpointUrl = prefs.getString(KEY_CLOUD_TTS_URL, defaults.cloudTtsEndpointUrl)
                ?: defaults.cloudTtsEndpointUrl,

            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, defaults.wakeWordEnabled),
            customWakeWord = prefs.getString(KEY_CUSTOM_WAKE_WORD, defaults.customWakeWord)
                ?: defaults.customWakeWord,

            selectedModel = enumOrDefault(prefs.getString(KEY_GEMINI_MODEL, null), defaults.selectedModel),
            internetSearchEnabled = prefs.getBoolean(KEY_INTERNET_SEARCH, defaults.internetSearchEnabled),
            rememberContextEnabled = prefs.getBoolean(KEY_REMEMBER_CONTEXT, defaults.rememberContextEnabled),
            streamResponsesEnabled = prefs.getBoolean(KEY_STREAM_RESPONSES, defaults.streamResponsesEnabled),
            apiProxyUrl = prefs.getString(KEY_API_PROXY_URL, defaults.apiProxyUrl) ?: defaults.apiProxyUrl,
            hasStoredApiKey = secureKeyStorage.hasApiKey(),

            memoryEnabled = prefs.getBoolean(KEY_MEMORY_ENABLED, defaults.memoryEnabled),
            memoryPrivacyMask = prefs.getBoolean(KEY_MEMORY_PRIVACY_MASK, defaults.memoryPrivacyMask),

            themeMode = enumOrDefault(prefs.getString(KEY_THEME_MODE, null), defaults.themeMode),
            accentColor = enumOrDefault(prefs.getString(KEY_ACCENT_COLOR, null), defaults.accentColor),
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, defaults.animationsEnabled),
            textSize = enumOrDefault(prefs.getString(KEY_TEXT_SIZE, null), defaults.textSize),

            assistantNotificationsEnabled = prefs.getBoolean(KEY_NOTIF_ASSISTANT, defaults.assistantNotificationsEnabled),
            voiceNotificationsEnabled = prefs.getBoolean(KEY_NOTIF_VOICE, defaults.voiceNotificationsEnabled),
            backgroundServiceNotificationEnabled = prefs.getBoolean(
                KEY_NOTIF_BG_SERVICE,
                defaults.backgroundServiceNotificationEnabled
            ),

            micPrivacyAutoMuteOnLock = prefs.getBoolean(KEY_MIC_PRIVACY_LOCK, defaults.micPrivacyAutoMuteOnLock),
            screenCaptureAlwaysConfirm = prefs.getBoolean(KEY_SCREEN_PRIVACY, defaults.screenCaptureAlwaysConfirm),
            appLockEnabled = prefs.getBoolean(KEY_APP_LOCK, defaults.appLockEnabled)
        )
    }

    private fun persistSettings(s: SaraSettings) {
        prefs.edit()
            .putString(KEY_ASSISTANT_NAME, s.assistantName.ifBlank { "SARA" })
            .putString(KEY_ASSISTANT_SUBTITLE, s.assistantSubtitle.ifBlank { "Your Personal AI" })
            .putString(KEY_PERSONALITY_DESC, s.personalityDescription)
            .putString(KEY_PERSONALITY_MODE, s.personalityMode.name)
            .putString(KEY_RESPONSE_STYLE, s.responseStyle.name)
            .putString(KEY_ANSWER_LENGTH, s.answerLength.name)

            .putBoolean(KEY_VOICE_ENABLED, s.voiceResponsesEnabled)
            .putString(KEY_VOICE_PROVIDER, s.voiceProvider.name)
            .putString(KEY_VOICE_GENDER, s.voiceGender.name)
            .putString(KEY_VOICE_PERSONA, s.femaleVoicePersona.name)
            .putFloat(KEY_SPEECH_SPEED, s.speechSpeed)
            .putFloat(KEY_SPEECH_PITCH, s.speechPitch)
            .putString(KEY_VOICE_LANGUAGE, s.voiceLanguage.name)
            .putFloat(KEY_VOICE_VOLUME, s.voiceVolume)
            .putString(KEY_BN_PRONUNCIATION, s.bengaliPronunciationQuality.name)
            .putString(KEY_CLOUD_TTS_URL, s.cloudTtsEndpointUrl.trim())

            .putBoolean(KEY_WAKE_WORD_ENABLED, s.wakeWordEnabled)
            .putString(KEY_CUSTOM_WAKE_WORD, s.customWakeWord.ifBlank { "Hey SARA" })

            .putString(KEY_GEMINI_MODEL, s.selectedModel.name)
            .putBoolean(KEY_INTERNET_SEARCH, s.internetSearchEnabled)
            .putBoolean(KEY_REMEMBER_CONTEXT, s.rememberContextEnabled)
            .putBoolean(KEY_STREAM_RESPONSES, s.streamResponsesEnabled)
            .putString(KEY_API_PROXY_URL, s.apiProxyUrl.trim())

            .putBoolean(KEY_MEMORY_ENABLED, s.memoryEnabled)
            .putBoolean(KEY_MEMORY_PRIVACY_MASK, s.memoryPrivacyMask)

            .putString(KEY_THEME_MODE, s.themeMode.name)
            .putString(KEY_ACCENT_COLOR, s.accentColor.name)
            .putBoolean(KEY_ANIMATIONS_ENABLED, s.animationsEnabled)
            .putString(KEY_TEXT_SIZE, s.textSize.name)

            .putBoolean(KEY_NOTIF_ASSISTANT, s.assistantNotificationsEnabled)
            .putBoolean(KEY_NOTIF_VOICE, s.voiceNotificationsEnabled)
            .putBoolean(KEY_NOTIF_BG_SERVICE, s.backgroundServiceNotificationEnabled)

            .putBoolean(KEY_MIC_PRIVACY_LOCK, s.micPrivacyAutoMuteOnLock)
            .putBoolean(KEY_SCREEN_PRIVACY, s.screenCaptureAlwaysConfirm)
            .putBoolean(KEY_APP_LOCK, s.appLockEnabled)
            .apply()
    }

    private fun loadMemories(): List<MemoryItem> {
        val raw = prefs.getString(KEY_MEMORIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        MemoryItem(
                            id = obj.getString("id"),
                            content = obj.getString("content"),
                            createdAtMillis = obj.getLong("createdAtMillis")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistMemories(items: List<MemoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("content", item.content)
                    put("createdAtMillis", item.createdAtMillis)
                }
            )
        }
        prefs.edit().putString(KEY_MEMORIES, arr.toString()).apply()
    }

    private fun loadConversationHistory(): List<ConversationEntry> {
        val raw = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        ConversationEntry(
                            id = obj.getString("id"),
                            message = obj.getString("message"),
                            timestampMillis = obj.getLong("timestampMillis"),
                            isAssistant = obj.optBoolean("isAssistant", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistConversationHistory(items: List<ConversationEntry>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("message", item.message)
                    put("timestampMillis", item.timestampMillis)
                    put("isAssistant", item.isAssistant)
                }
            )
        }
        prefs.edit().putString(KEY_CONVERSATIONS, arr.toString()).apply()
    }

    private fun loadVoiceProvider(raw: String?, fallback: VoiceProviderMode): VoiceProviderMode {
        if (raw.isNullOrBlank()) return fallback
        if (raw == "CLOUD_NEURAL_MALE") return VoiceProviderMode.CLOUD_NEURAL_FEMALE
        return enumOrDefault(raw, fallback)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return try {
            enumValueOf<T>(raw)
        } catch (_: Exception) {
            fallback
        }
    }

    private companion object {
        const val PREFS_NAME = "sara_settings_prefs"
        const val KEY_ASSISTANT_NAME = "assistant_name"
        const val KEY_ASSISTANT_SUBTITLE = "assistant_subtitle"
        const val KEY_PERSONALITY_DESC = "personality_desc"
        const val KEY_PERSONALITY_MODE = "personality_mode"
        const val KEY_RESPONSE_STYLE = "response_style"
        const val KEY_ANSWER_LENGTH = "answer_length"
        const val KEY_VOICE_ENABLED = "voice_enabled"
        const val KEY_VOICE_PROVIDER = "voice_provider"
        const val KEY_VOICE_GENDER = "voice_gender"
        const val KEY_VOICE_PERSONA = "female_voice_persona"
        const val KEY_SPEECH_SPEED = "speech_speed"
        const val KEY_SPEECH_PITCH = "speech_pitch"
        const val KEY_VOICE_LANGUAGE = "voice_language"
        const val KEY_VOICE_VOLUME = "voice_volume"
        const val KEY_BN_PRONUNCIATION = "bn_pronunciation"
        const val KEY_CLOUD_TTS_URL = "cloud_tts_url"
        const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        const val KEY_CUSTOM_WAKE_WORD = "custom_wake_word"
        const val KEY_GEMINI_MODEL = "gemini_model"
        const val KEY_INTERNET_SEARCH = "internet_search"
        const val KEY_REMEMBER_CONTEXT = "remember_context"
        const val KEY_STREAM_RESPONSES = "stream_responses"
        const val KEY_API_PROXY_URL = "api_proxy_url"
        const val KEY_MEMORY_ENABLED = "memory_enabled"
        const val KEY_MEMORY_PRIVACY_MASK = "memory_privacy_mask"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_NOTIF_ASSISTANT = "notif_assistant"
        const val KEY_NOTIF_VOICE = "notif_voice"
        const val KEY_NOTIF_BG_SERVICE = "notif_bg_service"
        const val KEY_MIC_PRIVACY_LOCK = "mic_privacy_lock"
        const val KEY_SCREEN_PRIVACY = "screen_privacy"
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_MEMORIES = "saved_memories_json"
        const val KEY_CONVERSATIONS = "conversation_history_json"
    }
}
