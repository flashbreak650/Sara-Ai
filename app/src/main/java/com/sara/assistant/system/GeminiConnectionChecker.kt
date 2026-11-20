package com.sara.assistant.system

import com.sara.assistant.data.settings.ApiConnectionState
import com.sara.assistant.data.settings.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ConnectionCheckResult(
    val state: ApiConnectionState,
    val messageBn: String,
    val messageEn: String
)

/**
 * Verifies real connectivity to a configured backend proxy URL or Gemini API endpoint.
 * Never logs or exposes API keys.
 */
object GeminiConnectionChecker {

    suspend fun verifyConnection(
        proxyUrl: String,
        keyStorage: SecureKeyStorage
    ): ConnectionCheckResult = withContext(Dispatchers.IO) {
        val cleanProxy = proxyUrl.trim()
        val storedKey = keyStorage.decryptApiKeyOrNull()

        if (cleanProxy.isEmpty() && storedKey.isNullOrBlank()) {
            return@withContext ConnectionCheckResult(
                state = ApiConnectionState.UNCONFIGURED,
                messageBn = "API কনফিগারেশন বা প্রক্সি URL সেট করা নেই",
                messageEn = "No API key or backend proxy configured"
            )
        }

        val targetUrl = if (cleanProxy.isNotEmpty()) {
            cleanProxy
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models?key=$storedKey"
        }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = conn.responseCode
            if (code in 200..299) {
                ConnectionCheckResult(
                    state = ApiConnectionState.CONNECTED,
                    messageBn = "সংযুক্ত (HTTP $code)",
                    messageEn = "Connected (HTTP $code)"
                )
            } else {
                ConnectionCheckResult(
                    state = ApiConnectionState.ERROR,
                    messageBn = "সংযোগ ব্যর্থ (HTTP $code)",
                    messageEn = "Connection failed (HTTP $code)"
                )
            }
        } catch (e: Exception) {
            ConnectionCheckResult(
                state = ApiConnectionState.ERROR,
                messageBn = "নেটওয়ার্ক ত্রুটি: ${e.javaClass.simpleName}",
                messageEn = "Network error: ${e.javaClass.simpleName}"
            )
        } finally {
            conn?.disconnect()
        }
    }
}
