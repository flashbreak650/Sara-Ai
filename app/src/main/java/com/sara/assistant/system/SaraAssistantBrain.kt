package com.sara.assistant.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.sara.assistant.data.settings.AnswerLength
import com.sara.assistant.data.settings.AssistantPersonalityMode
import com.sara.assistant.data.settings.ConversationEntry
import com.sara.assistant.data.settings.MemoryItem
import com.sara.assistant.data.settings.SaraSettings
import com.sara.assistant.data.settings.SecureKeyStorage
import com.sara.assistant.data.settings.VoiceLanguageOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BrainReply(
    val text: String,
    val usedGemini: Boolean
)

/**
 * High-speed conversational intelligence for both in-app HomeScreen and outside-app SaraWakeService.
 * Uses <5ms instant local responses for everyday commands and low-thinking Gemini API calls for fast AI replies.
 */
object SaraAssistantBrain {

    suspend fun generateReply(
        context: Context,
        userText: String,
        settings: SaraSettings,
        memories: List<MemoryItem>,
        history: List<ConversationEntry>,
        secureKeyStorage: SecureKeyStorage
    ): BrainReply {
        val clean = userText.trim()
        val isBn = settings.voiceLanguage == VoiceLanguageOption.BENGALI
        val name = settings.assistantName.ifBlank { "SARA" }

        if (clean.isEmpty()) {
            return BrainReply(
                text = if (isBn) "হ্যাঁ বলুন, আমি আপনার কথা শুনছি।" else "Yes, I am listening. How can I help?",
                usedGemini = false
            )
        }

        // 1. Instant zero-latency (<5ms) device/utility & conversational fast-path
        handleLocalCommand(context, clean, isBn, name)?.let { localReply ->
            return BrainReply(text = localReply, usedGemini = false)
        }

        // 2. Try configured backend /api/chat proxy
        val proxyChatUrl = resolveChatProxyUrl(settings.apiProxyUrl)
        if (proxyChatUrl.isNotEmpty()) {
            callProxyChat(proxyChatUrl, clean, settings, memories, isBn)?.let { reply ->
                return BrainReply(text = reply, usedGemini = true)
            }
        }

        // 3. Try direct Keystore-encrypted Gemini API key (with LOW/MINIMAL thinking for fast replies)
        val apiKey = secureKeyStorage.decryptApiKeyOrNull()?.takeIf { it.isNotBlank() }
        if (apiKey != null) {
            callDirectGeminiChat(apiKey, clean, settings, memories, history, isBn)?.let { reply ->
                return BrainReply(text = reply, usedGemini = true)
            }
        }

        // 4. Honest helpful offline/unconfigured response
        val offlineMsg = if (isBn) {
            "আমি $name, আপনার কথা শুনেছি: \"$clean\"। পূর্ণাঙ্গ এআই উত্তরের জন্য Aura Control > AI Brain-এ আপনার Gemini API Key বা Proxy URL যোগ করুন।"
        } else {
            "I heard \"$clean\". To enable full Gemini AI answers, please add your Gemini API Key or Proxy URL in Aura Control > AI Brain."
        }
        return BrainReply(text = offlineMsg, usedGemini = false)
    }

    private fun handleLocalCommand(
        context: Context,
        query: String,
        isBn: Boolean,
        assistantName: String
    ): String? {
        val q = query.lowercase(Locale.ROOT).trim()

        if (q == "hi" || q == "hello" || q == "hey" || q == "hey sara" ||
            q == "হ্যালো" || q == "হাই" || q == "সারা" || q.contains("আসসালামু আলাইকুম")
        ) {
            return if (isBn) {
                "আসসালামু আলাইকুম! আমি $assistantName, আপনার সাথেই আছি। বলুন, কীভাবে সাহায্য করতে পারি?"
            } else {
                "Hello! I'm $assistantName, ready to help. What can I do for you?"
            }
        }

        if (q.contains("তোমার নাম") || q.contains("তুমি কে") || q.contains("who are you") || q.contains("your name")) {
            return if (isBn) {
                "আমি $assistantName—আপনার ব্যক্তিগত বাংলা ও ইংরেজি এআই ভয়েস অ্যাসিস্ট্যান্ট।"
            } else {
                "I am $assistantName—your personal Bengali and English AI voice assistant."
            }
        }

        if (q.contains("কী করতে পারো") || q.contains("কি করতে পারো") || q.contains("what can you do")) {
            return if (isBn) {
                "আমি আপনার সাথে বাংলা ও ইংরেজিতে কথা বলতে পারি, অ্যাপ খুলতে পারি, সময় ও তারিখ জানাতে পারি, নোট সেভ করতে পারি এবং ব্যাকগ্রাউন্ডে সাড়া দিতে পারি।"
            } else {
                "I can chat in Bengali and English, open apps, tell the time and date, save memories, and listen in the background."
            }
        }

        if (q.contains("কয়টা বাজে") || q.contains("সময় কত") || q.contains("what time") || q == "time") {
            val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return if (isBn) "এখন সময় $now।" else "The current time is $now."
        }

        if (q.contains("আজ কত তারিখ") || q.contains("কত তারিখ") || q.contains("what date") || q.contains("today's date")) {
            val today = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            return if (isBn) "আজ $today।" else "Today is $today."
        }

        if (q.contains("সেটিংস খোলো") || q.contains("open settings")) {
            runCatching {
                val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return if (isBn) "ডিভাইসের সেটিংস খুলে দিয়েছি।" else "Opening Android Settings."
        }

        if (q.contains("ওয়াইফাই") || q.contains("wifi settings")) {
            runCatching {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return if (isBn) "ওয়াই-ফাই সেটিংস খুলে দিয়েছি।" else "Opening Wi-Fi settings."
        }

        if (q.contains("ব্লুটুথ") || q.contains("bluetooth")) {
            runCatching {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return if (isBn) "ব্লুটুথ সেটিংস খুলে দিয়েছি।" else "Opening Bluetooth settings."
        }

        if (q.startsWith("open ") || q.contains("খোলো")) {
            val appQuery = q.removePrefix("open ").replace("খোলো", "").trim()
            if (appQuery.length >= 2) {
                val launched = tryLaunchInstalledApp(context, appQuery)
                if (launched != null) {
                    return if (isBn) "$launched অ্যাপটি চালু করেছি।" else "Opening $launched."
                }
            }
        }

        if (q == "কেমন আছো" || q == "কেমন আছেন" || q == "how are you") {
            return if (isBn) {
                "আলহামদুলিল্লাহ, আমি ভালো আছি! আমি $assistantName। আপনার কোনো কাজে সাহায্য করতে পারি?"
            } else {
                "I'm doing wonderful, thank you! I am $assistantName—how can I assist you?"
            }
        }

        return null
    }

    private fun tryLaunchInstalledApp(context: Context, appQuery: String): String? {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching { pm.queryIntentActivities(mainIntent, 0) }.getOrElse { emptyList() }
        val match = apps.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString().lowercase(Locale.ROOT)
            label == appQuery || label.contains(appQuery)
        } ?: return null

        val launchIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return null
        return if (runCatching { context.startActivity(launchIntent) }.isSuccess) {
            match.loadLabel(pm).toString()
        } else {
            null
        }
    }

    private fun resolveChatProxyUrl(proxyUrl: String): String {
        val clean = proxyUrl.trim().trimEnd('/')
        if (clean.isEmpty()) return ""
        return when {
            clean.endsWith("/api/chat") -> clean
            clean.endsWith("/api/tts") -> clean.removeSuffix("/api/tts") + "/api/chat"
            else -> "$clean/api/chat"
        }
    }

    private suspend fun callProxyChat(
        chatUrl: String,
        message: String,
        settings: SaraSettings,
        memories: List<MemoryItem>,
        isBn: Boolean
    ): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(chatUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 14000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().apply {
                put("message", message)
                put("language", if (isBn) "bn" else "en")
                put("model", settings.selectedModel.modelId)
                put("assistantName", settings.assistantName.ifBlank { "SARA" })
                put("personalityMode", if (settings.personalityMode == AssistantPersonalityMode.FRIENDLY) "friendly" else "professional")
                put("responseStyle", settings.responseStyle.name.lowercase(Locale.ROOT))
                put("answerLength", if (settings.answerLength == AnswerLength.SHORT) "short" else "detailed")
                put("internetSearchEnabled", settings.internetSearchEnabled)
                put(
                    "memories",
                    JSONArray().apply {
                        if (settings.memoryEnabled) {
                            memories.take(6).forEach { put(it.content) }
                        }
                    }
                )
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            if (conn.responseCode !in 200..299) return@withContext null
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val reply = JSONObject(responseText).optString("reply", "").trim()
            reply.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun callDirectGeminiChat(
        apiKey: String,
        message: String,
        settings: SaraSettings,
        memories: List<MemoryItem>,
        history: List<ConversationEntry>,
        isBn: Boolean
    ): String? = withContext(Dispatchers.IO) {
        val candidateModels = listOf(
            settings.selectedModel.modelId,
            "gemini-3.1-flash-lite",
            "gemini-4-neon",
            "gemini-4-hydrogen"
        ).distinct()

        val systemInstruction = buildString {
            append("You are ${settings.assistantName.ifBlank { "SARA" }}, a caring, ultra-responsive personal AI voice assistant. ")
            append(
                if (isBn) {
                    "Always reply naturally in clear, polite Bengali (unless user writes purely in English). "
                } else {
                    "Reply clearly in English. "
                }
            )
            append(
                if (settings.answerLength == AnswerLength.SHORT) {
                    "Keep answers concise and natural for spoken voice playback (1-3 sentences)."
                } else {
                    "Provide thorough, well-structured helpful explanations."
                }
            )
            if (settings.memoryEnabled && memories.isNotEmpty()) {
                append(" User memories: ")
                append(memories.take(6).joinToString("; ") { it.content })
                append(".")
            }
            if (settings.rememberContextEnabled && history.isNotEmpty()) {
                append(" Recent context: ")
                append(history.take(4).reversed().joinToString(" | ") { it.message })
            }
        }

        for (modelId in candidateModels) {
            var conn: HttpURLConnection? = null
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                val thinkingLevel = if (modelId == "gemini-3.1-flash-lite") "MINIMAL" else "LOW"
                val body = JSONObject().apply {
                    put(
                        "systemInstruction",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", systemInstruction))
                        )
                    )
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", message))
                            )
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().put(
                            "thinkingConfig",
                            JSONObject().put("thinkingLevel", thinkingLevel)
                        )
                    )
                }

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                if (conn.responseCode !in 200..299) continue

                val respText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(respText)
                val candidates = root.optJSONArray("candidates") ?: continue
                val first = candidates.optJSONObject(0) ?: continue
                val content = first.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                val reply = parts.optJSONObject(0)?.optString("text", "")?.trim()
                if (!reply.isNullOrBlank()) return@withContext reply
            } catch (_: Exception) {
                continue
            } finally {
                conn?.disconnect()
            }
        }
        null
    }
}
