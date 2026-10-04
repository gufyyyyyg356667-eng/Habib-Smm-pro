package com.Lia.assistant.voice

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.Lia.assistant.action.ActionExecutor
import com.Lia.assistant.action.LiaToolCatalog
import com.Lia.assistant.data.*
import com.Lia.assistant.forge.ForgeController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

object VoiceSessionManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val reconnectMutex = Mutex()

    private val _state = MutableStateFlow(NovaOrbState.IDLE)
    val state: StateFlow<NovaOrbState> = _state

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _detectedLanguage = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val detectedLanguage: StateFlow<DetectedLanguage> = _detectedLanguage

    private var sessionEpoch = 0
    private var client: GeminiLiveClient? = null
    private var audioIO: LiveAudioIO? = null
    private var micStarted = false
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private const val MAX_RECONNECTS = 5
    private const val RECONNECT_DELAY_MS = 1200L

    private var playbackSilenceWatchdog: Job? = null
    private lateinit var appContext: Context

    fun start(ctx: Context) {
        appContext = ctx.applicationContext
        if (_isActive.value) return
        _isActive.value = true
        _lastError.value = null
        reconnectAttempts = 0
        VoiceLatencyTracker.resetReconnect()
        ConversationMemory.clear()
        EchoGuard.reset()

        // Prewarm installed app cache
        scope.launch { com.Lia.assistant.action.InstalledAppLabelCache.prewarm(appContext) }

        scope.launch { connectSession(isResuming = false) }
    }

    private suspend fun connectSession(isResuming: Boolean) {
        val epoch = ++sessionEpoch
        client?.disconnect()
        client = null

        _state.value = NovaOrbState.CONNECTING
        _statusText.value = "Connecting..."

        val personality = runCatching {
            Personality.valueOf(NovaPreferences.getString(appContext, NovaPreferences.Keys.PERSONALITY, "NORMAL"))
        }.getOrDefault(Personality.NORMAL)

        val language = runCatching {
            LanguagePreference.valueOf(NovaPreferences.getString(appContext, NovaPreferences.Keys.LANGUAGE, "AUTO"))
        }.getOrDefault(LanguagePreference.AUTO)

        val assistantName = NovaPreferences.getString(appContext, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        val voiceName = mapVoiceName(NovaPreferences.getString(appContext, NovaPreferences.Keys.VOICE_NAME, "Aoede"))
        val apiKey = com.Lia.assistant.data.ApiKeyStore.getKey(appContext)

        if (apiKey.isBlank()) {
            _state.value = NovaOrbState.ERROR
            _statusText.value = "No API key — add one in Settings"
            return
        }

        val recap = if (isResuming) ConversationMemory.recap() else null
        val toolInstruction = ActionExecutor.getLiaCapabilityPrompts(appContext).toolInstruction(assistantName)
        val prompt = PersonalityPromptBuilder.build(
            personality, language, _detectedLanguage.value, recap, assistantName, toolInstruction
        )

        val newClient = GeminiLiveClient(apiKey)
        client = newClient

        newClient.connect(
            systemInstruction = prompt,
            voiceName = voiceName,
            toolDeclarations = LiaToolCatalog.declarations(appContext),
            listener = object : GeminiLiveClient.Listener {
                override fun onSetupComplete() {
                    if (epoch != sessionEpoch) return
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "Listening..."
                    if (!micStarted) {
                        micStarted = true
                        audioIO?.startMic()
                    }
                }

                override fun onAudioChunk(bytes: ByteArray) {
                    if (epoch != sessionEpoch) return
                    if (_state.value != NovaOrbState.SPEAKING) {
                        EchoGuard.notifyPlaybackStarted()
                        VoiceLatencyTracker.markFirstAudioChunk()
                        VoiceLatencyTracker.markPlaybackStart()
                    }
                    _state.value = NovaOrbState.SPEAKING
                    val rms = audioIO?.playChunk(bytes) ?: 0f
                    _amplitude.value = rms
                    armPlaybackSilenceWatchdog(epoch)
                }

                override fun onInputTranscript(text: String) {
                    if (epoch != sessionEpoch) return
                    ConversationMemory.add(ConversationTurn.Role.USER, text)
                    _detectedLanguage.value = LanguageDetector.detect(text)
                    VoiceLatencyTracker.markSpeechEnd()
                }

                override fun onOutputTranscript(text: String) {
                    if (epoch != sessionEpoch) return
                    ConversationMemory.add(ConversationTurn.Role.ASSISTANT, text)
                    VoiceLatencyTracker.markFirstResponse()
                }

                override fun onToolCall(id: String, name: String, args: JSONObject) {
                    if (epoch != sessionEpoch) return
                    _state.value = NovaOrbState.THINKING
                    scope.launch {
                        val result = if (name == "build_website") {
                            val prompt = args.optString("prompt", "")
                            ForgeController.start(appContext, prompt)
                            JSONObject().put("result", "forge_started")
                        } else {
                            ActionExecutor.execute(appContext, name, args)
                        }
                        if (epoch == sessionEpoch) {
                            client?.sendToolResponse(id, name, result)
                            _state.value = NovaOrbState.LISTENING
                            _statusText.value = "Listening..."
                        }
                    }
                }

                override fun onTurnComplete() {
                    if (epoch != sessionEpoch) return
                    endTurn()
                }

                override fun onInterrupted() {
                    if (epoch != sessionEpoch) return
                    audioIO?.flushPlayback()
                    endTurn()
                }

                override fun onError(message: String) {
                    if (epoch != sessionEpoch) return
                    if (_isActive.value) scheduleReconnect()
                }

                override fun onClosed() {
                    if (epoch != sessionEpoch) return
                    if (_isActive.value) scheduleReconnect()
                }
            }
        )

        // Init audio IO if not already
        if (audioIO == null) {
            audioIO = LiveAudioIO(
                onMicChunk = { bytes ->
                    if (_isActive.value && !_muted.value && !EchoGuard.isMicMuted()) {
                        client?.sendAudio(bytes)
                        VoiceLatencyTracker.markSpeechStart()
                    }
                },
                onMicAmplitude = { rms ->
                    if (_state.value == NovaOrbState.LISTENING) _amplitude.value = rms
                }
            )
        }
    }

    private fun endTurn() {
        playbackSilenceWatchdog?.cancel()
        EchoGuard.notifyPlaybackStopped()
        _state.value = NovaOrbState.LISTENING
        _statusText.value = "Listening..."
        _amplitude.value = 0f
        VoiceLatencyTracker.newTurn()
    }

    private fun armPlaybackSilenceWatchdog(epoch: Int) {
        playbackSilenceWatchdog?.cancel()
        playbackSilenceWatchdog = scope.launch {
            delay(300)
            if (epoch == sessionEpoch && _state.value == NovaOrbState.SPEAKING) {
                endTurn()
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            reconnectMutex.withLock {
                if (!_isActive.value) return@withLock
                reconnectAttempts++
                VoiceLatencyTracker.incrementReconnect()
                if (reconnectAttempts > MAX_RECONNECTS) {
                    _state.value = NovaOrbState.ERROR
                    _statusText.value = "Connection lost — tap Stop and reopen Voice Mode"
                    _isActive.value = false
                    return@withLock
                }
                delay(RECONNECT_DELAY_MS)
                connectSession(isResuming = true)
            }
        }
    }

    fun refreshInstructions(ctx: Context) {
        if (!_isActive.value) return
        appContext = ctx.applicationContext
        scope.launch { connectSession(isResuming = true) }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun stop() {
        _isActive.value = false
        sessionEpoch++
        playbackSilenceWatchdog?.cancel()
        reconnectJob?.cancel()
        client?.disconnect()
        client = null
        audioIO?.release()
        audioIO = null
        micStarted = false
        EchoGuard.reset()
        ConversationMemory.clear()
        _state.value = NovaOrbState.IDLE
        _statusText.value = ""
        _amplitude.value = 0f
        _muted.value = false
    }

    fun startService(ctx: Context) {
        val intent = Intent(ctx, VoiceForegroundService::class.java)
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun stopService(ctx: Context) {
        ctx.stopService(Intent(ctx, VoiceForegroundService::class.java))
    }

    private fun mapVoiceName(uiName: String): String = when (uiName) {
        "Aoede" -> "Aoede"
        "Charon" -> "Charon"
        "Fenrir" -> "Fenrir"
        "Kore" -> "Kore"
        "Puck" -> "Puck"
        else -> "Aoede"
    }
}
