package com.sara.assistant.data.settings

enum class AssistantPersonalityMode(val labelBn: String, val labelEn: String) {
    FRIENDLY("বন্ধুত্বপূর্ণ", "Friendly"),
    PROFESSIONAL("পেশাদার", "Professional")
}

enum class ResponseStyle(val labelBn: String, val labelEn: String) {
    BALANCED("ভারসাম্যপূর্ণ", "Balanced"),
    CONCISE("সরাসরি ও সংক্ষিপ্ত", "Direct & Concise"),
    EMPATHETIC("সহানুভূতিশীল", "Warm & Empathetic"),
    ANALYTICAL("বিশ্লেষণধর্মী", "Analytical")
}

enum class AnswerLength(val labelBn: String, val labelEn: String) {
    SHORT("সংক্ষিপ্ত", "Short"),
    DETAILED("বিস্তারিত", "Detailed")
}

enum class VoiceGenderPreference(val labelBn: String, val labelEn: String) {
    MALE("পুরুষ কণ্ঠ (প্রস্তাবিত)", "Natural Male"),
    FEMALE("নারী কণ্ঠ", "Female"),
    SYSTEM_DEFAULT("সিস্টেম ডিফল্ট", "System Default")
}

enum class VoiceProviderMode(
    val labelBn: String,
    val labelEn: String,
    val descriptionBn: String,
    val descriptionEn: String
) {
    CLOUD_NEURAL_MALE(
        labelBn = "ক্লাউড নিউরাল পুরুষ কণ্ঠ",
        labelEn = "Cloud Neural Male (Gemini TTS)",
        descriptionBn = "স্বাভাবিক, উষ্ণ ও মানবিক বাংলা পুরুষ কণ্ঠ (Charon)",
        descriptionEn = "Warm, natural human-like Bengali male voice via Cloud TTS"
    ),
    ANDROID_NATIVE_TTS(
        labelBn = "ডিভাইস অফলাইন TTS",
        labelEn = "On-Device Android TTS",
        descriptionBn = "ফোনের নিজস্ব ইঞ্জিন ব্যবহার করে (পুরুষ কণ্ঠ সীমিত হতে পারে)",
        descriptionEn = "Uses installed phone TTS engine (male Bengali depends on device)"
    )
}

enum class BengaliPronunciationQuality(val labelBn: String, val labelEn: String) {
    NATURAL_HUMAN("প্রাকৃতিক ও স্পষ্ট (Natural Pauses)", "Natural Pauses & Tone"),
    STANDARD_FAST("দ্রুত ও সরাসরি", "Standard Direct")
}

enum class VoiceLanguageOption(val labelBn: String, val labelEn: String, val localeTag: String) {
    BENGALI("বাংলা", "Bengali", "bn-BD"),
    ENGLISH("ইংরেজি", "English", "en-US")
}

enum class GeminiModelOption(
    val modelId: String,
    val title: String,
    val descriptionBn: String,
    val descriptionEn: String
) {
    GEMINI_HYDROGEN(
        modelId = "gemini-4-hydrogen",
        title = "Gemini 4 Hydrogen",
        descriptionBn = "দৈনন্দিন বাংলা আলাপ ও দ্রুত উত্তরের জন্য",
        descriptionEn = "Fast everyday Bengali conversation & Q&A"
    ),
    GEMINI_NEON(
        modelId = "gemini-4-neon",
        title = "Gemini 4 Neon",
        descriptionBn = "জটিল বিশ্লেষণ, কোডিং ও যুক্তির জন্য",
        descriptionEn = "Complex reasoning, coding & deep analysis"
    ),
    GEMINI_FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        title = "Gemini 3.1 Flash Lite",
        descriptionBn = "সর্বনিম্ন ডেটা ও অতি দ্রুত রেসপন্সের জন্য",
        descriptionEn = "Lightweight ultra-fast responses"
    )
}

enum class ThemeModeOption(val labelBn: String, val labelEn: String) {
    DARK("ডার্ক", "Dark"),
    LIGHT("লাইট", "Light"),
    SYSTEM("সিস্টেম", "System Default")
}

enum class AccentColorOption(
    val labelBn: String,
    val labelEn: String,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    CYAN("সায়ান", "Cyan", 0xFF22D3EE, 0xFF8B5CF6),
    ELECTRIC_BLUE("ইলেকট্রিক ব্লু", "Electric Blue", 0xFF38BDF8, 0xFF818CF8),
    VIOLET("ভায়োলেট", "Violet", 0xFF8B5CF6, 0xFF22D3EE),
    EMERALD("এমেরাল্ড", "Emerald", 0xFF10B981, 0xFF22D3EE)
}

enum class TextSizeOption(val labelBn: String, val labelEn: String, val scaleFactor: Float) {
    SMALL("ছোট", "Small", 0.90f),
    MEDIUM("মাঝারি", "Medium", 1.00f),
    LARGE("বড়", "Large", 1.14f)
}

enum class ApiConnectionState {
    UNCONFIGURED,
    CHECKING,
    CONNECTED,
    ERROR
}

data class MemoryItem(
    val id: String,
    val content: String,
    val createdAtMillis: Long
)

data class ConversationEntry(
    val id: String,
    val message: String,
    val timestampMillis: Long,
    val isAssistant: Boolean = false
)

data class SaraSettings(
    // 1. Identity & AI Personality
    val assistantName: String = "SARA",
    val assistantSubtitle: String = "Your Personal AI",
    val personalityDescription: String = "Warm, calm, intelligent Bengali-first personal AI companion",
    val personalityMode: AssistantPersonalityMode = AssistantPersonalityMode.FRIENDLY,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val answerLength: AnswerLength = AnswerLength.SHORT,

    // 2. Natural Male Voice Settings
    val voiceResponsesEnabled: Boolean = true,
    val voiceProvider: VoiceProviderMode = VoiceProviderMode.CLOUD_NEURAL_MALE,
    val voiceGender: VoiceGenderPreference = VoiceGenderPreference.MALE,
    val speechSpeed: Float = 0.98f,
    val speechPitch: Float = 1.0f,
    val voiceLanguage: VoiceLanguageOption = VoiceLanguageOption.BENGALI,
    val voiceVolume: Float = 0.90f,
    val bengaliPronunciationQuality: BengaliPronunciationQuality = BengaliPronunciationQuality.NATURAL_HUMAN,
    val cloudTtsEndpointUrl: String = "",

    // 3. Wake Word Settings
    val wakeWordEnabled: Boolean = false,
    val customWakeWord: String = "Hey SARA",

    // 5. AI Brain Settings
    val selectedModel: GeminiModelOption = GeminiModelOption.GEMINI_HYDROGEN,
    val internetSearchEnabled: Boolean = true,
    val rememberContextEnabled: Boolean = true,
    val streamResponsesEnabled: Boolean = true,
    val apiProxyUrl: String = "",
    val hasStoredApiKey: Boolean = false,

    // 6. Memory Settings
    val memoryEnabled: Boolean = true,
    val memoryPrivacyMask: Boolean = true,

    // 7. Appearance
    val themeMode: ThemeModeOption = ThemeModeOption.DARK,
    val accentColor: AccentColorOption = AccentColorOption.CYAN,
    val animationsEnabled: Boolean = true,
    val textSize: TextSizeOption = TextSizeOption.MEDIUM,

    // 8. Notifications
    val assistantNotificationsEnabled: Boolean = true,
    val voiceNotificationsEnabled: Boolean = true,
    val backgroundServiceNotificationEnabled: Boolean = true,

    // 9. Privacy & Security
    val micPrivacyAutoMuteOnLock: Boolean = true,
    val screenCaptureAlwaysConfirm: Boolean = true,
    val appLockEnabled: Boolean = false
)
