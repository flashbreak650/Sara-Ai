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
    FEMALE("মধুর নারী কণ্ঠ (প্রস্তাবিত)", "Natural Female"),
    MALE("পুরুষ কণ্ঠ", "Male"),
    SYSTEM_DEFAULT("সিস্টেম ডিফল্ট", "System Default")
}

enum class VoiceProviderMode(
    val labelBn: String,
    val labelEn: String,
    val descriptionBn: String,
    val descriptionEn: String
) {
    CLOUD_NEURAL_FEMALE(
        labelBn = "ক্লাউড নিউরাল নারী কণ্ঠ",
        labelEn = "Cloud Neural Female (Gemini TTS)",
        descriptionBn = "৬টি মিষ্টি, উষ্ণ ও মানবিক বাংলা নারী কণ্ঠ (Kore, Zephyr, Aoede, Leda ও আরও)",
        descriptionEn = "Six sweet, warm, human-like Bengali female voices via Gemini Neural TTS"
    ),
    ANDROID_NATIVE_TTS(
        labelBn = "ডিভাইস অফলাইন TTS",
        labelEn = "On-Device Android TTS",
        descriptionBn = "ফোনের সর্বোচ্চ মানের বাংলা নারী নিউরাল/অফলাইন কণ্ঠ ব্যবহার করে",
        descriptionEn = "Uses highest-quality available Bengali female voice on device"
    )
}

enum class FemaleVoicePersona(
    val voiceId: String,
    val geminiVoiceId: String,
    val fallbackVoiceId: String,
    val labelBn: String,
    val labelEn: String,
    val descriptionBn: String,
    val previewSentenceBn: String,
    val previewSentenceEn: String
) {
    KORE(
        voiceId = "Kore",
        geminiVoiceId = "Kore",
        fallbackVoiceId = "Kore",
        labelBn = "কোরি — মিষ্টি ও উষ্ণ",
        labelEn = "Kore — Sweet & Warm",
        descriptionBn = "সবচেয়ে স্বাভাবিক, প্রাণবন্ত ও আন্তরিক বাংলা নারী কণ্ঠ।",
        previewSentenceBn = "আসসালামু আলাইকুম! আমি সারা। আমি আপনার পাশে আছি—বলুন, আজ কীভাবে সাহায্য করতে পারি?",
        previewSentenceEn = "Hello! I am SARA. I'm right here with you—how can I help today?"
    ),
    ZEPHYR(
        voiceId = "Zephyr",
        geminiVoiceId = "Zephyr",
        fallbackVoiceId = "Zephyr",
        labelBn = "জেফির — কোমল ও স্নিগ্ধ",
        labelEn = "Zephyr — Soft & Gentle",
        descriptionBn = "মৃদু, শান্ত ও আরামদায়ক ভঙ্গির বাংলা নারী কণ্ঠ।",
        previewSentenceBn = "নমস্কার! আমি সারা। চলুন শান্তভাবে আপনার কাজগুলো একসাথে গুছিয়ে নিই।",
        previewSentenceEn = "Hi there! I am SARA. Let's calmly organize everything you need today."
    ),
    AOEDE(
        voiceId = "Aoede",
        geminiVoiceId = "Aoede",
        fallbackVoiceId = "Kore",
        labelBn = "আওয়েদি — সুরেলা ও উজ্জ্বল",
        labelEn = "Aoede — Melodic & Bright",
        descriptionBn = "সুরেলা, উজ্জ্বল ও স্পষ্ট উচ্চারণের মধুর বাংলা কণ্ঠ।",
        previewSentenceBn = "হ্যালো! আপনার দিনটি সুন্দর হোক। যেকোনো প্রশ্ন বা কাজের কথা আমাকে বলুন।",
        previewSentenceEn = "Hello! Wishing you a bright day. Ask me anything whenever you're ready."
    ),
    LEDA(
        voiceId = "Leda",
        geminiVoiceId = "Leda",
        fallbackVoiceId = "Zephyr",
        labelBn = "লেদা — শান্ত ও মমতাময়ী",
        labelEn = "Leda — Calm & Affectionate",
        descriptionBn = "ধীরস্থির, যত্নশীল ও মমতাময়ী বাংলা নারী কণ্ঠ।",
        previewSentenceBn = "আমি সারা, সবসময় আপনার কথা শুনতে প্রস্তুত। নিশ্চিন্তে বলুন কী প্রয়োজন।",
        previewSentenceEn = "I'm SARA, always listening attentively. Tell me calmly what you need."
    ),
    SARA_SWEET(
        voiceId = "SaraSweet",
        geminiVoiceId = "Despina",
        fallbackVoiceId = "Kore",
        labelBn = "সারা সুইট — মধুর আড্ডা",
        labelEn = "Sara Sweet — Warm Companion",
        descriptionBn = "বন্ধুত্বপূর্ণ, হাসিখুশি ও ঘরোয়া আলাপের মধুর কণ্ঠ।",
        previewSentenceBn = "আরে বাহ! আপনি ডেকেছেন শুনেই চলে এলাম। বলুন, আজ কোন কাজটি আগে করব?",
        previewSentenceEn = "Hey! I'm right here as soon as you called. What should we do first today?"
    ),
    SARA_VELVET(
        voiceId = "SaraVelvet",
        geminiVoiceId = "Sulafat",
        fallbackVoiceId = "Zephyr",
        labelBn = "সারা ভেলভেট — রেশমি প্রশান্ত",
        labelEn = "Sara Velvet — Soothing Serene",
        descriptionBn = "সন্ধ্যার শান্ত আলাপ ও রিমাইন্ডারের জন্য রেশমি কোমল কণ্ঠ।",
        previewSentenceBn = "একটু বিশ্রাম নিন, আমি আপনার সব রিমাইন্ডার ও মেসেজ গুছিয়ে রাখছি।",
        previewSentenceEn = "Take a calm breath—I'm keeping all your messages and reminders organized."
    )
}

enum class BengaliPronunciationQuality(val labelBn: String, val labelEn: String) {
    NATURAL_HUMAN("প্রাকৃতিক শ্বাস-বিরতি ও ছন্দ", "Natural Breathing Pauses & Rhythm"),
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

enum class AccentColorOption(
    val labelBn: String,
    val labelEn: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val topGlowHex: Long,
    val cardTintHex: Long
) {
    CRIMSON_AURA("ক্রিমসন অরা", "Crimson Aura", 0xFFEF4444, 0xFFDC2626, 0xFF28080E, 0xFF1D1014),
    CYAN("সায়ান", "Cyan Matrix", 0xFF06B6D4, 0xFF0284C7, 0xFF071F2B, 0xFF0F1820),
    ELECTRIC_BLUE("ইলেকট্রিক ব্লু", "Midnight Blue", 0xFF3B82F6, 0xFF2563EB, 0xFF0A1730, 0xFF101624),
    VIOLET("ভায়োলেট", "Violet Pulse", 0xFF8B5CF6, 0xFF6366F1, 0xFF1B0E2E, 0xFF161122),
    EMERALD("এমেরাল্ড", "Emerald Core", 0xFF10B981, 0xFF059669, 0xFF07221B, 0xFF0F1B17)
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

enum class InboxCategory(val labelBn: String, val labelEn: String) {
    VOICE_REPLY("ভয়েস উত্তর", "Voice"),
    WAKE_ALERT("ব্যাকগ্রাউন্ড", "Outside App"),
    REMINDER("রিমাইন্ডার", "Reminder"),
    SYSTEM("সিস্টেম", "System")
}

data class SaraInboxItem(
    val id: String,
    val title: String,
    val body: String,
    val category: InboxCategory = InboxCategory.VOICE_REPLY,
    val timestampMillis: Long,
    val isRead: Boolean = false,
    val senderVoicePersona: String? = null
)

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
    val personalityDescription: String = "Warm, caring, intelligent Bengali-first personal AI companion",
    val personalityMode: AssistantPersonalityMode = AssistantPersonalityMode.FRIENDLY,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val answerLength: AnswerLength = AnswerLength.SHORT,

    // 2. Natural Human Female Bengali Voice Settings
    val voiceResponsesEnabled: Boolean = true,
    val voiceProvider: VoiceProviderMode = VoiceProviderMode.CLOUD_NEURAL_FEMALE,
    val voiceGender: VoiceGenderPreference = VoiceGenderPreference.FEMALE,
    val femaleVoicePersona: FemaleVoicePersona = FemaleVoicePersona.KORE,
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceLanguage: VoiceLanguageOption = VoiceLanguageOption.BENGALI,
    val voiceVolume: Float = 0.92f,
    val bengaliPronunciationQuality: BengaliPronunciationQuality = BengaliPronunciationQuality.NATURAL_HUMAN,
    val cloudTtsEndpointUrl: String = "",

    // 3. Background Home Screen Assistant & Wake Word Settings
    val wakeWordEnabled: Boolean = false,
    val wakeListeningPaused: Boolean = false,
    val customWakeWord: String = "Hey SARA",
    val floatingOverlayEnabled: Boolean = true,
    val autoRestoreOnBoot: Boolean = true,

    // Persisted Permission & Service Snapshots
    val lastServiceActive: Boolean = false,
    val lastRecordedMicPermission: Boolean = false,
    val lastRecordedOverlayPermission: Boolean = false,
    val lastRecordedBatteryUnrestricted: Boolean = false,

    // 5. AI Brain Settings
    val selectedModel: GeminiModelOption = GeminiModelOption.GEMINI_FLASH_LITE,
    val internetSearchEnabled: Boolean = true,
    val rememberContextEnabled: Boolean = true,
    val streamResponsesEnabled: Boolean = true,
    val apiProxyUrl: String = "",
    val hasStoredApiKey: Boolean = false,

    // 6. Memory Settings
    val memoryEnabled: Boolean = true,
    val memoryPrivacyMask: Boolean = true,

    // 7. Appearance (Obsidian + Aura Signature Theme)
    val accentColor: AccentColorOption = AccentColorOption.CRIMSON_AURA,
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
