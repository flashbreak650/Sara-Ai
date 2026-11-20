package com.sara.assistant.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Permanent Jetpack DataStore-backed repository shared across MainActivity, SaraWakeService, and SaraBootReceiver.
 * Never resets or overwrites saved settings on refresh, app close, or device reboot.
 */
class SaraSettingsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val secureKeyStorage = SecureKeyStorage(appContext)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(SharedPreferencesMigration(appContext, LEGACY_PREFS_NAME)),
        scope = ioScope,
        produceFile = { File(appContext.filesDir, "datastore/sara_settings.preferences_pb") }
    )

    private val initialPrefs: Preferences = runBlocking(Dispatchers.IO) {
        dataStore.data.first()
    }

    private val _settings = MutableStateFlow(decodeSettings(initialPrefs))
    val settings: StateFlow<SaraSettings> = _settings.asStateFlow()

    private val _memories = MutableStateFlow(decodeMemories(initialPrefs[KEY_MEMORIES]))
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _conversationHistory = MutableStateFlow(decodeConversations(initialPrefs[KEY_CONVERSATIONS]))
    val conversationHistory: StateFlow<List<ConversationEntry>> = _conversationHistory.asStateFlow()

    private val _inboxItems = MutableStateFlow(loadOrInitializeInbox(initialPrefs))
    val inboxItems: StateFlow<List<SaraInboxItem>> = _inboxItems.asStateFlow()

    fun updateSettings(transform: (SaraSettings) -> SaraSettings) {
        val updated = transform(_settings.value).copy(
            hasStoredApiKey = secureKeyStorage.hasApiKey()
        )
        _settings.value = updated
        persistSettingsAsync(updated)
    }

    fun updatePermissionStateSnapshot(
        micGranted: Boolean,
        overlayGranted: Boolean,
        batteryUnrestricted: Boolean,
        serviceActive: Boolean
    ) {
        val cur = _settings.value
        if (cur.lastRecordedMicPermission == micGranted &&
            cur.lastRecordedOverlayPermission == overlayGranted &&
            cur.lastRecordedBatteryUnrestricted == batteryUnrestricted &&
            cur.lastServiceActive == serviceActive
        ) {
            return
        }
        updateSettings {
            it.copy(
                lastRecordedMicPermission = micGranted,
                lastRecordedOverlayPermission = overlayGranted,
                lastRecordedBatteryUnrestricted = batteryUnrestricted,
                lastServiceActive = serviceActive
            )
        }
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
        persistJsonAsync(KEY_MEMORIES, encodeMemories(next))
    }

    fun deleteMemory(id: String) {
        val next = _memories.value.filterNot { it.id == id }
        _memories.value = next
        persistJsonAsync(KEY_MEMORIES, encodeMemories(next))
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
        persistJsonAsync(KEY_CONVERSATIONS, encodeConversations(next))
    }

    fun clearConversationHistory() {
        _conversationHistory.value = emptyList()
        ioScope.launch {
            dataStore.edit { it.remove(KEY_CONVERSATIONS) }
        }
    }

    fun addInboxMessage(
        title: String,
        body: String,
        category: InboxCategory = InboxCategory.VOICE_REPLY,
        senderVoicePersona: String? = null
    ) {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) return
        val item = SaraInboxItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "SARA বার্তা" },
            body = cleanBody,
            category = category,
            timestampMillis = System.currentTimeMillis(),
            isRead = false,
            senderVoicePersona = senderVoicePersona
        )
        val next = (listOf(item) + _inboxItems.value).take(80)
        _inboxItems.value = next
        ioScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_INBOX_ITEMS] = encodeInboxItems(next)
                prefs[KEY_INBOX_INITIALIZED] = true
            }
        }
    }

    fun markInboxRead(id: String) {
        val next = _inboxItems.value.map { if (it.id == id) it.copy(isRead = true) else it }
        _inboxItems.value = next
        persistJsonAsync(KEY_INBOX_ITEMS, encodeInboxItems(next))
    }

    fun markAllInboxRead() {
        val next = _inboxItems.value.map { it.copy(isRead = true) }
        _inboxItems.value = next
        persistJsonAsync(KEY_INBOX_ITEMS, encodeInboxItems(next))
    }

    fun deleteInboxItem(id: String) {
        val next = _inboxItems.value.filterNot { it.id == id }
        _inboxItems.value = next
        persistJsonAsync(KEY_INBOX_ITEMS, encodeInboxItems(next))
    }

    fun clearInbox() {
        _inboxItems.value = emptyList()
        ioScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_INBOX_ITEMS] = "[]"
                prefs[KEY_INBOX_INITIALIZED] = true
            }
        }
    }

    /**
     * Explicit user-triggered Reset Settings (separate from Clear Personal Data).
     * Never called automatically on app start or refresh.
     */
    fun resetSettingsToDefaults() {
        val defaults = SaraSettings(hasStoredApiKey = secureKeyStorage.hasApiKey())
        _settings.value = defaults
        persistSettingsAsync(defaults)
    }

    fun clearAllPersonalData() {
        _memories.value = emptyList()
        _conversationHistory.value = emptyList()
        _inboxItems.value = emptyList()
        secureKeyStorage.clearApiKey()
        ioScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(KEY_MEMORIES)
                prefs.remove(KEY_CONVERSATIONS)
                prefs[KEY_INBOX_ITEMS] = "[]"
                prefs[KEY_INBOX_INITIALIZED] = true
                prefs.remove(KEY_API_PROXY_URL)
                prefs.remove(KEY_CLOUD_TTS_URL)
            }
        }
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

    private fun loadOrInitializeInbox(prefs: Preferences): List<SaraInboxItem> {
        val initialized = prefs[KEY_INBOX_INITIALIZED] ?: false
        val raw = prefs[KEY_INBOX_ITEMS]
        if (!raw.isNullOrBlank()) {
            return decodeInboxItems(raw)
        }
        if (initialized) return emptyList()

        val starter = listOf(
            SaraInboxItem(
                id = "sara_welcome_inbox_1",
                title = "SARA ইনবক্সে স্বাগতম",
                body = "এখন থেকে ফোনের হোম স্ক্রিনে বা ব্যাকগ্রাউন্ডে \"Hey SARA\" বললে সারার উত্তর ও রিমাইন্ডার এখানে জমা থাকবে।",
                category = InboxCategory.SYSTEM,
                timestampMillis = System.currentTimeMillis(),
                isRead = false,
                senderVoicePersona = FemaleVoicePersona.KORE.voiceId
            )
        )
        ioScope.launch {
            dataStore.edit { editor ->
                editor[KEY_INBOX_ITEMS] = encodeInboxItems(starter)
                editor[KEY_INBOX_INITIALIZED] = true
            }
        }
        return starter
    }

    private fun decodeSettings(prefs: Preferences): SaraSettings {
        val defaults = SaraSettings()
        return SaraSettings(
            assistantName = prefs[KEY_ASSISTANT_NAME] ?: defaults.assistantName,
            assistantSubtitle = prefs[KEY_ASSISTANT_SUBTITLE] ?: defaults.assistantSubtitle,
            personalityDescription = prefs[KEY_PERSONALITY_DESC] ?: defaults.personalityDescription,
            personalityMode = enumOrDefault(prefs[KEY_PERSONALITY_MODE], defaults.personalityMode),
            responseStyle = enumOrDefault(prefs[KEY_RESPONSE_STYLE], defaults.responseStyle),
            answerLength = enumOrDefault(prefs[KEY_ANSWER_LENGTH], defaults.answerLength),

            voiceResponsesEnabled = prefs[KEY_VOICE_ENABLED] ?: defaults.voiceResponsesEnabled,
            voiceProvider = loadVoiceProvider(prefs[KEY_VOICE_PROVIDER], defaults.voiceProvider),
            voiceGender = enumOrDefault(prefs[KEY_VOICE_GENDER], defaults.voiceGender),
            femaleVoicePersona = enumOrDefault(prefs[KEY_VOICE_PERSONA], defaults.femaleVoicePersona),
            speechSpeed = (prefs[KEY_SPEECH_SPEED] ?: defaults.speechSpeed).coerceIn(0.5f, 2.0f),
            speechPitch = (prefs[KEY_SPEECH_PITCH] ?: defaults.speechPitch).coerceIn(0.5f, 2.0f),
            voiceLanguage = enumOrDefault(prefs[KEY_VOICE_LANGUAGE], defaults.voiceLanguage),
            voiceVolume = (prefs[KEY_VOICE_VOLUME] ?: defaults.voiceVolume).coerceIn(0.0f, 1.0f),
            bengaliPronunciationQuality = enumOrDefault(
                prefs[KEY_BN_PRONUNCIATION],
                defaults.bengaliPronunciationQuality
            ),
            cloudTtsEndpointUrl = prefs[KEY_CLOUD_TTS_URL] ?: defaults.cloudTtsEndpointUrl,

            wakeWordEnabled = prefs[KEY_WAKE_WORD_ENABLED] ?: defaults.wakeWordEnabled,
            wakeListeningPaused = prefs[KEY_WAKE_PAUSED] ?: defaults.wakeListeningPaused,
            customWakeWord = prefs[KEY_CUSTOM_WAKE_WORD] ?: defaults.customWakeWord,
            floatingOverlayEnabled = prefs[KEY_FLOATING_OVERLAY] ?: defaults.floatingOverlayEnabled,
            autoRestoreOnBoot = prefs[KEY_AUTO_RESTORE_BOOT] ?: defaults.autoRestoreOnBoot,

            lastServiceActive = prefs[KEY_LAST_SERVICE_ACTIVE] ?: defaults.lastServiceActive,
            lastRecordedMicPermission = prefs[KEY_LAST_MIC_GRANTED] ?: defaults.lastRecordedMicPermission,
            lastRecordedOverlayPermission = prefs[KEY_LAST_OVERLAY_GRANTED] ?: defaults.lastRecordedOverlayPermission,
            lastRecordedBatteryUnrestricted = prefs[KEY_LAST_BATTERY_UNRESTRICTED] ?: defaults.lastRecordedBatteryUnrestricted,

            selectedModel = enumOrDefault(prefs[KEY_GEMINI_MODEL], defaults.selectedModel),
            internetSearchEnabled = prefs[KEY_INTERNET_SEARCH] ?: defaults.internetSearchEnabled,
            rememberContextEnabled = prefs[KEY_REMEMBER_CONTEXT] ?: defaults.rememberContextEnabled,
            streamResponsesEnabled = prefs[KEY_STREAM_RESPONSES] ?: defaults.streamResponsesEnabled,
            apiProxyUrl = prefs[KEY_API_PROXY_URL] ?: defaults.apiProxyUrl,
            hasStoredApiKey = secureKeyStorage.hasApiKey(),

            memoryEnabled = prefs[KEY_MEMORY_ENABLED] ?: defaults.memoryEnabled,
            memoryPrivacyMask = prefs[KEY_MEMORY_PRIVACY_MASK] ?: defaults.memoryPrivacyMask,

            accentColor = resolveAccentWithAuraMigration(prefs, defaults.accentColor),
            animationsEnabled = prefs[KEY_ANIMATIONS_ENABLED] ?: defaults.animationsEnabled,
            textSize = enumOrDefault(prefs[KEY_TEXT_SIZE], defaults.textSize),

            assistantNotificationsEnabled = prefs[KEY_NOTIF_ASSISTANT] ?: defaults.assistantNotificationsEnabled,
            voiceNotificationsEnabled = prefs[KEY_NOTIF_VOICE] ?: defaults.voiceNotificationsEnabled,
            backgroundServiceNotificationEnabled = prefs[KEY_NOTIF_BG_SERVICE]
                ?: defaults.backgroundServiceNotificationEnabled,

            micPrivacyAutoMuteOnLock = prefs[KEY_MIC_PRIVACY_LOCK] ?: defaults.micPrivacyAutoMuteOnLock,
            screenCaptureAlwaysConfirm = prefs[KEY_SCREEN_PRIVACY] ?: defaults.screenCaptureAlwaysConfirm,
            appLockEnabled = prefs[KEY_APP_LOCK] ?: defaults.appLockEnabled
        )
    }

    private fun persistSettingsAsync(s: SaraSettings) {
        ioScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ASSISTANT_NAME] = s.assistantName.ifBlank { "SARA" }
                prefs[KEY_ASSISTANT_SUBTITLE] = s.assistantSubtitle.ifBlank { "Your Personal AI" }
                prefs[KEY_PERSONALITY_DESC] = s.personalityDescription
                prefs[KEY_PERSONALITY_MODE] = s.personalityMode.name
                prefs[KEY_RESPONSE_STYLE] = s.responseStyle.name
                prefs[KEY_ANSWER_LENGTH] = s.answerLength.name

                prefs[KEY_VOICE_ENABLED] = s.voiceResponsesEnabled
                prefs[KEY_VOICE_PROVIDER] = s.voiceProvider.name
                prefs[KEY_VOICE_GENDER] = s.voiceGender.name
                prefs[KEY_VOICE_PERSONA] = s.femaleVoicePersona.name
                prefs[KEY_SPEECH_SPEED] = s.speechSpeed
                prefs[KEY_SPEECH_PITCH] = s.speechPitch
                prefs[KEY_VOICE_LANGUAGE] = s.voiceLanguage.name
                prefs[KEY_VOICE_VOLUME] = s.voiceVolume
                prefs[KEY_BN_PRONUNCIATION] = s.bengaliPronunciationQuality.name
                prefs[KEY_CLOUD_TTS_URL] = s.cloudTtsEndpointUrl.trim()

                prefs[KEY_WAKE_WORD_ENABLED] = s.wakeWordEnabled
                prefs[KEY_WAKE_PAUSED] = s.wakeListeningPaused
                prefs[KEY_CUSTOM_WAKE_WORD] = s.customWakeWord.ifBlank { "Hey SARA" }
                prefs[KEY_FLOATING_OVERLAY] = s.floatingOverlayEnabled
                prefs[KEY_AUTO_RESTORE_BOOT] = s.autoRestoreOnBoot

                prefs[KEY_LAST_SERVICE_ACTIVE] = s.lastServiceActive
                prefs[KEY_LAST_MIC_GRANTED] = s.lastRecordedMicPermission
                prefs[KEY_LAST_OVERLAY_GRANTED] = s.lastRecordedOverlayPermission
                prefs[KEY_LAST_BATTERY_UNRESTRICTED] = s.lastRecordedBatteryUnrestricted

                prefs[KEY_GEMINI_MODEL] = s.selectedModel.name
                prefs[KEY_INTERNET_SEARCH] = s.internetSearchEnabled
                prefs[KEY_REMEMBER_CONTEXT] = s.rememberContextEnabled
                prefs[KEY_STREAM_RESPONSES] = s.streamResponsesEnabled
                prefs[KEY_API_PROXY_URL] = s.apiProxyUrl.trim()

                prefs[KEY_MEMORY_ENABLED] = s.memoryEnabled
                prefs[KEY_MEMORY_PRIVACY_MASK] = s.memoryPrivacyMask

                prefs[KEY_ACCENT_COLOR] = s.accentColor.name
                prefs[KEY_AURA_MIGRATED_V5] = true
                prefs[KEY_ANIMATIONS_ENABLED] = s.animationsEnabled
                prefs[KEY_TEXT_SIZE] = s.textSize.name

                prefs[KEY_NOTIF_ASSISTANT] = s.assistantNotificationsEnabled
                prefs[KEY_NOTIF_VOICE] = s.voiceNotificationsEnabled
                prefs[KEY_NOTIF_BG_SERVICE] = s.backgroundServiceNotificationEnabled

                prefs[KEY_MIC_PRIVACY_LOCK] = s.micPrivacyAutoMuteOnLock
                prefs[KEY_SCREEN_PRIVACY] = s.screenCaptureAlwaysConfirm
                prefs[KEY_APP_LOCK] = s.appLockEnabled
            }
        }
    }

    private fun persistJsonAsync(key: Preferences.Key<String>, json: String) {
        ioScope.launch {
            dataStore.edit { it[key] = json }
        }
    }

    private fun decodeMemories(raw: String?): List<MemoryItem> {
        if (raw.isNullOrBlank()) return emptyList()
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

    private fun encodeMemories(items: List<MemoryItem>): String {
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
        return arr.toString()
    }

    private fun decodeConversations(raw: String?): List<ConversationEntry> {
        if (raw.isNullOrBlank()) return emptyList()
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

    private fun encodeConversations(items: List<ConversationEntry>): String {
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
        return arr.toString()
    }

    private fun decodeInboxItems(raw: String?): List<SaraInboxItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SaraInboxItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            body = obj.getString("body"),
                            category = enumOrDefault(obj.optString("category"), InboxCategory.VOICE_REPLY),
                            timestampMillis = obj.getLong("timestampMillis"),
                            isRead = obj.optBoolean("isRead", false),
                            senderVoicePersona = obj.optString("senderVoicePersona").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeInboxItems(items: List<SaraInboxItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("category", item.category.name)
                    put("timestampMillis", item.timestampMillis)
                    put("isRead", item.isRead)
                    put("senderVoicePersona", item.senderVoicePersona ?: "")
                }
            )
        }
        return arr.toString()
    }

    private fun loadVoiceProvider(raw: String?, fallback: VoiceProviderMode): VoiceProviderMode {
        if (raw.isNullOrBlank()) return fallback
        if (raw == "CLOUD_NEURAL_MALE") return VoiceProviderMode.CLOUD_NEURAL_FEMALE
        return enumOrDefault(raw, fallback)
    }

    private fun resolveAccentWithAuraMigration(
        prefs: Preferences,
        fallback: AccentColorOption
    ): AccentColorOption {
        val migrated = prefs[KEY_AURA_MIGRATED_V5] ?: false
        if (!migrated) {
            ioScope.launch {
                dataStore.edit { editor ->
                    editor[KEY_ACCENT_COLOR] = AccentColorOption.CRIMSON_AURA.name
                    editor[KEY_AURA_MIGRATED_V5] = true
                }
            }
            return AccentColorOption.CRIMSON_AURA
        }
        return enumOrDefault(prefs[KEY_ACCENT_COLOR], fallback)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return try {
            enumValueOf<T>(raw)
        } catch (_: Exception) {
            fallback
        }
    }

    companion object {
        @Volatile
        private var instance: SaraSettingsRepository? = null

        fun getInstance(context: Context): SaraSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SaraSettingsRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val LEGACY_PREFS_NAME = "sara_settings_prefs"

        private val KEY_ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        private val KEY_ASSISTANT_SUBTITLE = stringPreferencesKey("assistant_subtitle")
        private val KEY_PERSONALITY_DESC = stringPreferencesKey("personality_desc")
        private val KEY_PERSONALITY_MODE = stringPreferencesKey("personality_mode")
        private val KEY_RESPONSE_STYLE = stringPreferencesKey("response_style")
        private val KEY_ANSWER_LENGTH = stringPreferencesKey("answer_length")

        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_VOICE_PROVIDER = stringPreferencesKey("voice_provider")
        private val KEY_VOICE_GENDER = stringPreferencesKey("voice_gender")
        private val KEY_VOICE_PERSONA = stringPreferencesKey("female_voice_persona")
        private val KEY_SPEECH_SPEED = floatPreferencesKey("speech_speed")
        private val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        private val KEY_VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        private val KEY_VOICE_VOLUME = floatPreferencesKey("voice_volume")
        private val KEY_BN_PRONUNCIATION = stringPreferencesKey("bn_pronunciation")
        private val KEY_CLOUD_TTS_URL = stringPreferencesKey("cloud_tts_url")

        private val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val KEY_WAKE_PAUSED = booleanPreferencesKey("wake_listening_paused")
        private val KEY_CUSTOM_WAKE_WORD = stringPreferencesKey("custom_wake_word")
        private val KEY_FLOATING_OVERLAY = booleanPreferencesKey("floating_overlay_enabled")
        private val KEY_AUTO_RESTORE_BOOT = booleanPreferencesKey("auto_restore_boot")

        private val KEY_LAST_SERVICE_ACTIVE = booleanPreferencesKey("last_service_active")
        private val KEY_LAST_MIC_GRANTED = booleanPreferencesKey("last_mic_granted")
        private val KEY_LAST_OVERLAY_GRANTED = booleanPreferencesKey("last_overlay_granted")
        private val KEY_LAST_BATTERY_UNRESTRICTED = booleanPreferencesKey("last_battery_unrestricted")

        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_INTERNET_SEARCH = booleanPreferencesKey("internet_search")
        private val KEY_REMEMBER_CONTEXT = booleanPreferencesKey("remember_context")
        private val KEY_STREAM_RESPONSES = booleanPreferencesKey("stream_responses")
        private val KEY_API_PROXY_URL = stringPreferencesKey("api_proxy_url")

        private val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        private val KEY_MEMORY_PRIVACY_MASK = booleanPreferencesKey("memory_privacy_mask")

        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_AURA_MIGRATED_V5 = booleanPreferencesKey("aura_crimson_migrated_v5")
        private val KEY_ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")

        private val KEY_NOTIF_ASSISTANT = booleanPreferencesKey("notif_assistant")
        private val KEY_NOTIF_VOICE = booleanPreferencesKey("notif_voice")
        private val KEY_NOTIF_BG_SERVICE = booleanPreferencesKey("notif_bg_service")

        private val KEY_MIC_PRIVACY_LOCK = booleanPreferencesKey("mic_privacy_lock")
        private val KEY_SCREEN_PRIVACY = booleanPreferencesKey("screen_privacy")
        private val KEY_APP_LOCK = booleanPreferencesKey("app_lock")

        private val KEY_MEMORIES = stringPreferencesKey("saved_memories_json")
        private val KEY_CONVERSATIONS = stringPreferencesKey("conversation_history_json")
        private val KEY_INBOX_ITEMS = stringPreferencesKey("sara_inbox_items_json")
        private val KEY_INBOX_INITIALIZED = booleanPreferencesKey("sara_inbox_initialized")
    }
}
