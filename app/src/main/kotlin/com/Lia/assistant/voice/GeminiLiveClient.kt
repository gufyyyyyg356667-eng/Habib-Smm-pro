package com.Lia.assistant.voice

import android.util.Base64
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(private val apiKey: String) {
    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(bytes: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed()
    }

    companion object {
        private const val MODEL = "models/gemini-2.0-flash-live-001"
        private const val WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private val deduplicator = ToolCallDeduplicator()
    private var ws: WebSocket? = null
    private var listener: Listener? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(
        systemInstruction: String,
        voiceName: String = "Aoede",
        toolDeclarations: JSONArray,
        listener: Listener
    ) {
        this.listener = listener
        deduplicator.clear()

        val url = "$WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setup = buildSetup(systemInstruction, voiceName, toolDeclarations)
                webSocket.send(setup.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Server sometimes sends JSON as binary frames
                handleMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t.message ?: "WebSocket failure")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code == 1000) listener.onClosed()
                else listener.onError("Socket closed: $code")
            }
        })
    }

    fun sendAudio(pcmBytes: ByteArray) {
        val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val msg = JSONObject()
            .put("realtimeInput", JSONObject()
                .put("audio", JSONObject()
                    .put("mimeType", "audio/pcm;rate=16000")
                    .put("data", b64)))
        ws?.send(msg.toString())
    }

    fun sendToolResponse(id: String, name: String, resultJson: JSONObject) {
        val msg = JSONObject()
            .put("toolResponse", JSONObject()
                .put("functionResponses", JSONArray()
                    .put(JSONObject()
                        .put("id", id)
                        .put("name", name)
                        .put("response", resultJson))))
        ws?.send(msg.toString())
    }

    fun disconnect() {
        ws?.close(1000, "disconnect")
        ws = null
    }

    private fun buildSetup(instruction: String, voiceName: String, tools: JSONArray): JSONObject {
        return JSONObject()
            .put("setup", JSONObject()
                .put("model", MODEL)
                .put("generationConfig", JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO")))
                .put("speechConfig", JSONObject()
                    .put("voiceConfig", JSONObject()
                        .put("prebuiltVoiceConfig", JSONObject()
                            .put("voiceName", voiceName))))
                .put("systemInstruction", JSONObject()
                    .put("parts", JSONArray()
                        .put(JSONObject().put("text", instruction))))
                .put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject())
                .put("tools", JSONArray()
                    .put(JSONObject().put("functionDeclarations", tools))))
    }

    private fun handleMessage(text: String) {
        if (text.isBlank()) return
        try {
            val json = JSONObject(text)
            when {
                json.has("setupComplete") -> listener?.onSetupComplete()
                json.has("serverContent") -> handleServerContent(json.getJSONObject("serverContent"))
                json.has("toolCall") -> handleToolCall(json.getJSONObject("toolCall"))
                // silently ignore sessionResumptionUpdate, goAway
            }
        } catch (_: Exception) {
            // malformed frame — skip
        }
    }

    private fun handleServerContent(sc: JSONObject) {
        // Input transcription
        if (sc.has("inputTranscription")) {
            val t = sc.getJSONObject("inputTranscription").optString("text")
            if (t.isNotBlank()) listener?.onInputTranscript(t)
        }
        // Output transcription
        if (sc.has("outputTranscription")) {
            val t = sc.getJSONObject("outputTranscription").optString("text")
            if (t.isNotBlank()) listener?.onOutputTranscript(t)
        }
        // Model turn audio
        if (sc.has("modelTurn")) {
            val parts = sc.getJSONObject("modelTurn").optJSONArray("parts") ?: return
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("inlineData")) {
                    val inline = part.getJSONObject("inlineData")
                    val mime = inline.optString("mimeType", "")
                    if (mime.startsWith("audio/pcm")) {
                        val b64 = inline.optString("data", "")
                        if (b64.isNotBlank()) {
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            listener?.onAudioChunk(bytes)
                        }
                    }
                }
            }
        }
        if (sc.optBoolean("turnComplete", false)) listener?.onTurnComplete()
        if (sc.optBoolean("interrupted", false)) listener?.onInterrupted()
    }

    private fun handleToolCall(tc: JSONObject) {
        val calls = tc.optJSONArray("functionCalls") ?: return
        for (i in 0 until calls.length()) {
            val call = calls.getJSONObject(i)
            val id = call.optString("id")
            val name = call.optString("name")
            val args = call.optJSONObject("args") ?: JSONObject()
            if (id.isNotBlank() && !deduplicator.seen(id)) {
                listener?.onToolCall(id, name, args)
            }
        }
    }
}

class ToolCallDeduplicator {
    private val seen = LinkedHashSet<String>()
    private val maxSize = 200

    fun seen(id: String): Boolean {
        if (seen.contains(id)) return true
        if (seen.size >= maxSize) seen.iterator().apply { next(); remove() }
        seen.add(id)
        return false
    }

    fun clear() { seen.clear() }
}
