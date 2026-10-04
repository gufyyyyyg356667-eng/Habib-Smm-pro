package com.Lia.assistant.ui.screens.quickactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun QuickActionsScreen(appState: NovaAppState, onBack: () -> Unit, onOpenForge: () -> Unit) {
    val colors = LocalNovaColors.current
    val tiles = listOf(
        Triple(Icons.Default.Mic, "Talk to Lia", "Start voice session"),
        Triple(Icons.Default.Apps, "Open App", "YouTube kholo"),
        Triple(Icons.Default.Phone, "Call", "Mummy ko call karo"),
        Triple(Icons.Default.Message, "Message", "WhatsApp bhejo"),
        Triple(Icons.Default.Language, "Build Website", "Cafe ki site banao"),
        Triple(Icons.Default.Chat, "Ask", "Koi bhi sawaal")
    )
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Quick Actions", onBack = onBack)
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tiles.size) { i ->
                val (icon, title, sub) = tiles[i]
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = colors.accent.copy(alpha = 0.15f), modifier = Modifier.fillMaxSize()) {}
                        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
                    Text(sub, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
