package com.Lia.assistant.voice

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatTurn(val isUser: Boolean, val text: String)

sealed class ChatReplyResult {
    data class Success(val text: String) : ChatReplyResult()
    data class Error(val message: String) : ChatReplyResult()
}

object GeminiTextClient {
    @Volatile private var cachedModel: String? = null

    internal val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json".toMediaType()
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    internal fun resolveModel(apiKey: String): String {
        cachedModel?.let { return it }
        return try {
            val req = Request.Builder()
                .url("$BASE/models?key=$apiKey")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return fallback().also { cachedModel = it }
                val body = resp.body?.string() ?: return fallback().also { cachedModel = it }
                val models = JSONObject(body).optJSONArray("models") ?: return fallback().also { cachedModel = it }
                var flashModel: String? = null
                var anyModel: String? = null
                for (i in 0 until models.length()) {
                    val m = models.getJSONObject(i)
                    val name = m.optString("name")
                    val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                    val supportsGenerate = (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                    if (!supportsGenerate) continue
                    if (anyModel == null) anyModel = name
                    if (name.contains("flash") && !name.contains("live") && !name.contains("image") && !name.contains("tts")) {
                        if (flashModel == null) flashModel = name
                    }
                }
                (flashModel ?: anyModel ?: fallback()).also { cachedModel = it }
            }
        } catch (_: Exception) { fallback().also { cachedModel = it } }
    }

    private fun fallback() = "models/gemini-1.5-flash"

    fun reply(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatTurn>
    ): ChatReplyResult {
        if (apiKey.isBlank()) return ChatReplyResult.Error("No Gemini API key yet — add one in Settings.")
        if (history.isEmpty()) return ChatReplyResult.Error("No message to send.")

        return try {
            val model = resolveModel(apiKey)
            val contents = JSONArray()
            for (turn in history) {
                contents.put(JSONObject()
                    .put("role", if (turn.isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text))))
            }
            val body = JSONObject()
                .put("systemInstruction", JSONObject()
                    .put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                .put("contents", contents)

            val req = Request.Builder()
                .url("$BASE/$model:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody(JSON_TYPE))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return ChatReplyResult.Error("Couldn't reach Gemini. Please try again.")
                }
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                val text = candidates
                    ?.getJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.getJSONObject(0)
                    ?.optString("text", "")
                    ?: ""
                if (text.isBlank()) ChatReplyResult.Error("No response from Gemini. Please try again.")
                else ChatReplyResult.Success(text)
            }
        } catch (_: Exception) {
            ChatReplyResult.Error("Couldn't reach Gemini. Check your connection.")
        }
    }
}
