package com.Lia.assistant.voice

import android.media.*
import android.media.audiofx.*
import kotlinx.coroutines.*
import kotlin.math.sqrt

class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit
) {
    private val sampleRate = 16000
    private val chunkSize = 2048
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var micJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startMic() {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf, 4096)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufSize
        )

        // Attach audio effects if available
        runCatching { AcousticEchoCanceler.create(rec.audioSessionId) }
        runCatching { NoiseSuppressor.create(rec.audioSessionId) }
        runCatching { AutomaticGainControl.create(rec.audioSessionId) }

        rec.startRecording()
        recorder = rec

        micJob = scope.launch {
            val buf = ByteArray(chunkSize)
            while (isActive) {
                val read = rec.read(buf, 0, chunkSize)
                if (read > 0) {
                    val chunk = buf.copyOf(read)
                    onMicAmplitude(rms(chunk))
                    onMicChunk(chunk)
                }
            }
        }
    }

    fun stopMic() {
        micJob?.cancel()
        micJob = null
        runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
    }

    fun playChunk(bytes: ByteArray): Float {
        if (track == null) initTrack()
        track?.write(bytes, 0, bytes.size)
        return rms(bytes)
    }

    fun flushPlayback() {
        track?.pause()
        track?.flush()
        track?.play()
    }

    fun setVolume(vol: Float) {
        track?.setVolume(vol.coerceIn(0f, 1f))
    }

    private fun initTrack() {
        val sampleRateOut = 24000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRateOut, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRateOut)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        track = AudioTrack(attrs, format, minBuf * 4, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        track?.play()
    }

    fun release() {
        stopMic()
        runCatching { track?.pause(); track?.flush(); track?.release() }
        track = null
        scope.cancel()
    }

    private fun rms(bytes: ByteArray): Float {
        if (bytes.size < 2) return 0f
        var sum = 0.0
        var count = 0
        var i = 0
        while (i + 1 < bytes.size) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            sum += sample.toDouble() * sample.toDouble()
            count++
            i += 2
        }
        if (count == 0) return 0f
        return (sqrt(sum / count) / 32768.0).toFloat().coerceIn(0f, 1f)
    }
}

object EchoGuard {
    private const val ECHO_TAIL_MS = 450L
    private const val WATCHDOG_MS = 60_000L
    @Volatile private var playbackCount = 0
    @Volatile private var micMuted = false
    private var tailJob: Job? = null
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun notifyPlaybackStarted() {
        synchronized(this) {
            playbackCount++
            micMuted = true
            tailJob?.cancel()
            startWatchdog()
        }
    }

    fun notifyPlaybackStopped() {
        synchronized(this) {
            if (playbackCount > 0) playbackCount--
            if (playbackCount <= 0) {
                playbackCount = 0
                tailJob?.cancel()
                tailJob = scope.launch {
                    delay(ECHO_TAIL_MS)
                    micMuted = false
                }
            }
        }
    }

    fun isMicMuted(): Boolean = micMuted

    fun reset() {
        synchronized(this) {
            playbackCount = 0
            micMuted = false
            tailJob?.cancel()
            watchdogJob?.cancel()
        }
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(WATCHDOG_MS)
            playbackCount = 0
            micMuted = false
        }
    }
}
