package com.Lia.assistant.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val colors = LocalNovaColors.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Privacy", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            NovaGlassCard {
                Text("Data & Privacy", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("• Gemini API key stored locally only.\n• Voice audio processed by Google Gemini Live.\n• No data stored by this app.\n• Generated websites saved to device storage.",
                    color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
