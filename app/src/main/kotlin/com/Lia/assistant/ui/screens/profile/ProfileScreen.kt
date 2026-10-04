package com.Lia.assistant.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun ProfileScreen(appState: NovaAppState, onBack: () -> Unit) {
    var name by remember { mutableStateOf(appState.userName) }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Profile", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NovaGlassCard {
                NovaTextField(value = name, onValueChange = { name = it }, placeholder = "Your name", leadingIcon = Icons.Default.Person)
                Spacer(Modifier.height(8.dp))
                NovaButton(text = "Save", onClick = { appState.applyUserName(name) })
            }
        }
    }
}
