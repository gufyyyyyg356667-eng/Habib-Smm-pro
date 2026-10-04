package com.Lia.assistant.ui.screens.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonStyle
import com.Lia.assistant.ui.components.NovaVoiceVisualizer
import com.Lia.assistant.ui.fx.NovaOrb
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.*

@Composable
fun VoiceScreen(appState: NovaAppState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val colors = LocalNovaColors.current
    val state by VoiceSessionManager.state.collectAsStateWithLifecycle()
    val amplitude by VoiceSessionManager.amplitude.collectAsStateWithLifecycle()
    val muted by VoiceSessionManager.muted.collectAsStateWithLifecycle()
    val statusText by VoiceSessionManager.statusText.collectAsStateWithLifecycle()
    val lastError by VoiceSessionManager.lastError.collectAsStateWithLifecycle()

    var hasMicPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMicPermission = it
        if (it) VoiceSessionManager.startService(ctx)
    }

    LaunchedEffect(Unit) {
        hasMicPermission = ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) VoiceSessionManager.startService(ctx)
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(80.dp))

            // Big orb
            NovaOrb(
                state = state, amplitude = amplitude,
                modifier = Modifier.size(280.dp),
                style = runCatching { NovaOrbStyle.valueOf(appState.orbStyle) }.getOrDefault(NovaOrbStyle.NOVA)
            )

            Spacer(Modifier.height(16.dp))

            // Voice visualizer ring (speaking only)
            if (state == NovaOrbState.SPEAKING) {
                NovaVoiceVisualizer(
                    amplitude = amplitude,
                    modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Status text
            Text(
                text = when {
                    !hasMicPermission -> "Microphone permission needed"
                    lastError != null -> lastError!!
                    else -> statusText.ifBlank { "${appState.assistantName} is ready..." }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = if (lastError != null) colors.error else colors.textSecondary
            )

            Spacer(Modifier.weight(1f))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                IconButton(
                    onClick = { VoiceSessionManager.toggleMute() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                        if (muted) "Unmute" else "Mute",
                        tint = if (muted) colors.error else colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Stop
                NovaButton(
                    text = "Stop",
                    onClick = {
                        VoiceSessionManager.stopService(ctx)
                        onClose()
                    },
                    style = NovaButtonStyle.PRIMARY
                )

                // Close (keep running in background)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Minimise", tint = colors.textSecondary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
