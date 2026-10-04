package com.Lia.assistant.ui.screens.orbstyle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState

@Composable
fun OrbStyleScreen(appState: NovaAppState, onBack: () -> Unit) {
    var previewState by remember { mutableStateOf(NovaOrbState.IDLE) }
    val selected = runCatching { NovaOrbStyle.valueOf(appState.orbStyle) }.getOrDefault(NovaOrbStyle.NOVA)
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Orb Style", onBack = onBack)
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            NovaOrb(state = previewState, amplitude = if (previewState == NovaOrbState.SPEAKING) 0.6f else 0.2f, modifier = Modifier.size(160.dp), style = selected)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf(NovaOrbState.IDLE, NovaOrbState.LISTENING, NovaOrbState.SPEAKING).forEach { s ->
                FilterChip(selected = previewState == s, onClick = { previewState = s }, label = { Text(s.name, style = MaterialTheme.typography.labelMedium) }, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(NovaOrbStyle.values()) { style ->
                NovaSettingsRow(title = style.name.lowercase().replaceFirstChar { it.uppercase() }, value = if (selected == style) "✓" else "", onClick = { appState.applyOrbStyle(style.name) })
            }
        }
    }
}
