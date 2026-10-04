package com.Lia.assistant.forge

import android.content.Context
import com.Lia.assistant.data.ApiKeyStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

enum class ForgePhase { IDLE, BUILDING, DONE, FAILED, CANCELLED }

enum class ForgeStage(val label: String) {
    READING_IDEA("Reading your idea"),
    LAYING_OUT("Laying out structure"),
    STYLING("Styling sections"),
    BUILDING_VISUALS("Building 3D visuals"),
    INJECTING_MOTION("Injecting motion"),
    FINALIZING("Finalizing"),
    READY("Ready")
}

data class ForgeMetrics(
    val chars: Int = 0,
    val sections: Int = 0,
    val elapsedSec: Float = 0f,
    val tokensPerSec: Float = 0f
)

data class ForgeUiState(
    val phase: ForgePhase = ForgePhase.IDLE,
    val prompt: String = "",
    val isEdit: Boolean = false,
    val stage: ForgeStage = ForgeStage.READING_IDEA,
    val progress: Float = 0f,
    val metrics: ForgeMetrics = ForgeMetrics(),
    val sections: List<String> = emptyList(),
    val log: List<String> = emptyList(),
    val logSeq: Int = 0,
    val buildId: Int = 0,
    val file: File? = null,
    val error: String? = null
)

sealed class ForgeStartResult {
    object Started : ForgeStartResult()
    object MissingKey : ForgeStartResult()
    object Offline : ForgeStartResult()
    object Busy : ForgeStartResult()
    data class Error(val message: String) : ForgeStartResult()
}

object ForgeController {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow(ForgeUiState())
    val state: StateFlow<ForgeUiState> = _state

    private val _pendingOpen = MutableStateFlow(false)
    val pendingOpen: StateFlow<Boolean> = _pendingOpen

    private var buildJob: Job? = null
    private var buildId = 0

    fun start(ctx: Context, prompt: String): ForgeStartResult {
        if (_state.value.phase == ForgePhase.BUILDING) return ForgeStartResult.Busy
        val apiKey = ApiKeyStore.getKey(ctx)
        if (apiKey.isBlank()) return ForgeStartResult.MissingKey

        buildId++
        val currentBuild = buildId
        _state.value = ForgeUiState(
            phase = ForgePhase.BUILDING,
            prompt = prompt,
            buildId = currentBuild,
            stage = ForgeStage.READING_IDEA
        )
        requestOpen()

        buildJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val client = WebsiteForgeClient()
            var accumulated = StringBuilder()
            var lastEmit = 0L
            var retried = false

            fun elapsed() = (System.currentTimeMillis() - startTime) / 1000f

            fun emitState(extra: ForgeUiState.() -> ForgeUiState = { this }) {
                val html = accumulated.toString()
                val stage = ForgeStageDetector.detect(html)
                val sectionList = ForgeStageDetector.extractSections(html)
                val progress = maxOf(stage.ordinal.toFloat() / 6f, html.length.toFloat() / 60000f).coerceAtMost(0.98f)
                val metrics = ForgeMetrics(
                    chars = html.length,
                    sections = sectionList.size,
                    elapsedSec = elapsed(),
                    tokensPerSec = if (elapsed() > 0) html.length / elapsed() / 4 else 0f
                )
                _state.value = _state.value.copy(
                    stage = stage,
                    progress = progress,
                    metrics = metrics,
                    sections = sectionList
                ).extra()
            }

            suspend fun build(systemPrompt: String, userPrompt: String): Boolean {
                var success = false
                client.stream(
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    onChunk = { delta ->
                        accumulated.append(delta)
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 120) {
                            lastEmit = now
                            val kbWritten = accumulated.length / 1024
                            val newLog = buildList {
                                add("${ForgeStageDetector.detect(accumulated.toString()).label}...")
                                if (kbWritten > 0 && kbWritten % 10 == 0) add("${kbWritten} KB written")
                            }
                            emitState { copy(log = (log + newLog).takeLast(20), logSeq = logSeq + 1) }
                        }
                    },
                    onError = { err ->
                        _state.value = _state.value.copy(phase = ForgePhase.FAILED, error = err)
                    },
                    onDone = { finishReason ->
                        val html = accumulated.toString()
                        val stripped = ForgeHtml.stripFences(html)
                        when {
                            stripped.length < 1500 && !retried -> {
                                // too short — retry
                            }
                            !stripped.contains("</html>") && stripped.length >= 14000 -> {
                                val repaired = ForgeHtml.repairTruncated(stripped)
                                finalize(ctx, repaired, prompt, startTime)
                                success = true
                            }
                            !stripped.contains("</html>") && !retried -> {
                                // needs retry
                            }
                            ForgeHtml.looksLikeHtml(stripped) -> {
                                val safe = ForgeHtml.withSafetyNet(stripped)
                                finalize(ctx, safe, prompt, startTime)
                                success = true
                            }
                            else -> {
                                _state.value = _state.value.copy(
                                    phase = ForgePhase.FAILED,
                                    error = "Gemini stopped before finishing the page."
                                )
                            }
                        }
                    }
                )
                return success
            }

            val systemPrompt = ForgeSystemPrompt.forNewSite(prompt)
            val success = build(systemPrompt, prompt)

            if (!success && _state.value.phase == ForgePhase.BUILDING) {
                // One retry
                retried = true
                accumulated = StringBuilder()
                val retryPrompt = "$prompt\n\nIMPORTANT: Write completely original markup and code. Finish the complete page."
                val retrySystem = ForgeSystemPrompt.forNewSite(retryPrompt)
                build(retrySystem, retryPrompt)
            }

            if (_state.value.phase == ForgePhase.BUILDING) {
                _state.value = _state.value.copy(
                    phase = ForgePhase.FAILED,
                    error = "Gemini could not complete the website. Please try again."
                )
            }
        }

        return ForgeStartResult.Started
    }

    private fun finalize(ctx: Context, html: String, prompt: String, startTime: Long) {
        val dir = File(ctx.filesDir, "Websites").also { it.mkdirs() }
        val file = File(dir, "website_${System.currentTimeMillis()}.html")
        file.writeText(html)
        _state.value = _state.value.copy(
            phase = ForgePhase.DONE,
            progress = 1f,
            stage = ForgeStage.READY,
            file = file,
            log = _state.value.log + "Website complete!"
        )
    }

    fun cancel() {
        buildJob?.cancel()
        _state.value = _state.value.copy(phase = ForgePhase.CANCELLED)
    }

    fun reset() {
        buildJob?.cancel()
        _state.value = ForgeUiState()
    }

    fun edit(ctx: Context, change: String) {
        val currentFile = _state.value.file ?: return
        val currentHtml = currentFile.readText()
        val apiKey = ApiKeyStore.getKey(ctx)
        if (apiKey.isBlank()) return

        buildId++
        _state.value = _state.value.copy(
            phase = ForgePhase.BUILDING,
            isEdit = true,
            buildId = buildId,
            stage = ForgeStage.READING_IDEA
        )

        buildJob = scope.launch {
            val client = WebsiteForgeClient()
            val sb = StringBuilder()
            client.stream(
                apiKey = apiKey,
                systemPrompt = ForgeSystemPrompt.forEdit(currentHtml, change),
                userPrompt = change,
                onChunk = { sb.append(it) },
                onError = { _state.value = _state.value.copy(phase = ForgePhase.FAILED, error = it) },
                onDone = {
                    val html = ForgeHtml.stripFences(sb.toString())
                    if (ForgeHtml.looksLikeHtml(html)) {
                        finalize(ctx, ForgeHtml.withSafetyNet(html), change, System.currentTimeMillis())
                    } else {
                        _state.value = _state.value.copy(phase = ForgePhase.FAILED, error = "Edit failed.")
                    }
                }
            )
        }
    }

    fun requestOpen() { _pendingOpen.value = true }
    fun consumeOpen() { _pendingOpen.value = false }
    fun partialHtml(): String = "" // exposed for preview
}
