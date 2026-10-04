package com.Lia.assistant.ui.screens.personality

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.*
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun PersonalityScreen(appState: NovaAppState, onBack: () -> Unit) {
    val colors = LocalNovaColors.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Personality", onBack = onBack)
        Text("Changes apply immediately", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Personality.values().size) { i ->
                val p = Personality.values()[i]
                val selected = appState.personality == p
                val scale by animateFloatAsState(targetValue = if (selected) 1.04f else 1f, label = "ps")
                NovaGlassCard(modifier = Modifier.fillMaxWidth().scale(scale)) {
                    Icon(p.icon, null, tint = if (selected) colors.accent else colors.textSecondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(p.displayName, color = if (selected) colors.accent else colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    NovaButton(text = if (selected) "Selected" else "Select", onClick = { appState.applyPersonality(p) },
                        style = if (selected) NovaButtonStyle.PRIMARY else NovaButtonStyle.SECONDARY)
                }
            }
        }
    }
}
