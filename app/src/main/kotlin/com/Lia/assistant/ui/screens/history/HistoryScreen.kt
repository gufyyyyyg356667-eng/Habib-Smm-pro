package com.Lia.assistant.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NovaTopBar(title = "History", onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NovaEmptyState(icon = Icons.Default.History, title = "No conversations yet", subtitle = "Your sessions will appear here.")
        }
    }
}
