package com.Lia.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LatencyRecord(
    val speechStart: Long = 0,
    val speechEnd: Long = 0,
    val firstResponse: Long = 0,
    val firstAudioChunk: Long = 0,
    val playbackStart: Long = 0
) {
    val speechEndToFirstAudio: Long
        get() = if (speechEnd > 0 && firstAudioChunk > 0) firstAudioChunk - speechEnd else -1
}

object VoiceLatencyTracker {
    private val _current = MutableStateFlow(LatencyRecord())
    val current: StateFlow<LatencyRecord> = _current

    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount

    fun markSpeechStart() { _current.value = _current.value.copy(speechStart = System.currentTimeMillis()) }
    fun markSpeechEnd() { _current.value = _current.value.copy(speechEnd = System.currentTimeMillis()) }
    fun markFirstResponse() {
        if (_current.value.firstResponse == 0L)
            _current.value = _current.value.copy(firstResponse = System.currentTimeMillis())
    }
    fun markFirstAudioChunk() {
        if (_current.value.firstAudioChunk == 0L)
            _current.value = _current.value.copy(firstAudioChunk = System.currentTimeMillis())
    }
    fun markPlaybackStart() {
        if (_current.value.playbackStart == 0L)
            _current.value = _current.value.copy(playbackStart = System.currentTimeMillis())
    }

    fun newTurn() { _current.value = LatencyRecord() }
    fun incrementReconnect() { _reconnectCount.value++ }
    fun resetReconnect() { _reconnectCount.value = 0 }
}
