package com.Lia.assistant.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.NovaOrb
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.VoiceSessionManager
import java.util.Calendar

@Composable
fun HomeScreen(
    appState: NovaAppState,
    onTalk: () -> Unit,
    onType: () -> Unit,
    onProfile: () -> Unit
) {
    val colors = LocalNovaColors.current
    val voiceState by VoiceSessionManager.state.collectAsStateWithLifecycle()
    val amplitude by VoiceSessionManager.amplitude.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val name = appState.assistantName

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onProfile) {
                Icon(Icons.Default.Person, "Profile", tint = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Greeting
        Text(
            "$greeting,",
            style = MaterialTheme.typography.displayMedium.copy(fontStyle = FontStyle.Italic),
            color = colors.textPrimary
        )
        if (appState.userName.isNotBlank()) {
            Text(
                appState.userName,
                style = MaterialTheme.typography.displayMedium,
                color = colors.accent
            )
        }

        Spacer(Modifier.height(32.dp))

        // 3D Orb
        NovaOrb(
            state = voiceState,
            amplitude = amplitude,
            modifier = Modifier.size(220.dp),
            style = runCatching { NovaOrbStyle.valueOf(appState.orbStyle) }.getOrDefault(NovaOrbStyle.NOVA)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Tap me to talk",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = colors.textSecondary
        )

        Spacer(Modifier.height(32.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaButton(text = "Talk", onClick = onTalk, leadingIcon = Icons.Default.Mic)
            NovaButton(
                text = "Type", onClick = onType,
                style = NovaButtonStyle.SECONDARY,
                leadingIcon = Icons.Default.Chat
            )
        }

        Spacer(Modifier.height(32.dp))

        // Try saying
        NovaSectionHeader("Try saying")
        Spacer(Modifier.height(8.dp))
        val suggestions = listOf(
            "YouTube kholo", "Mummy ko call karo",
            "Ek cafe ki website banao", "Rahul ko WhatsApp message bhejo",
            "Weather kaisa hai?", "Joke sunao"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suggestions.size) { i ->
                NovaGlassCard(modifier = Modifier.width(180.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(suggestions[i], color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
