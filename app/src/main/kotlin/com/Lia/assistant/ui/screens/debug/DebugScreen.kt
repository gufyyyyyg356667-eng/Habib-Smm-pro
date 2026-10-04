package com.Lia.assistant.ui.screens.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.*

@Composable
fun DebugScreen(onBack: () -> Unit) {
    val colors = LocalNovaColors.current
    val state by VoiceSessionManager.state.collectAsStateWithLifecycle()
    val detected by VoiceSessionManager.detectedLanguage.collectAsStateWithLifecycle()
    val latency by VoiceLatencyTracker.current.collectAsStateWithLifecycle()
    val reconnects by VoiceLatencyTracker.reconnectCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()) {
        NovaTopBar(title = "Voice Pipeline Debug", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaGlassCard {
                Text("Session", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                listOf("State" to state.name, "Language" to detected.name, "Reconnects" to reconnects.toString()).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(k, color = colors.textSecondary, modifier = Modifier.weight(1f))
                        Text(v, color = colors.textPrimary)
                    }
                }
            }
            NovaGlassCard {
                Text("Latency", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                val ms = latency.speechEndToFirstAudio
                Text("Speech → first audio: ${if (ms >= 0) "${ms}ms" else "N/A"}", color = colors.textPrimary)
            }
        }
    }
}
