package com.Lia.assistant.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = LocalNovaColors.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        NovaTopBar(title = "About", onBack = onBack)
        Spacer(Modifier.height(32.dp))
        NovaOrb(state = NovaOrbState.IDLE, amplitude = 0f, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text(AssistantBrand.FULL_NAME, style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        Text("Version 1.0.0", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text(AssistantBrand.TAGLINE, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}
