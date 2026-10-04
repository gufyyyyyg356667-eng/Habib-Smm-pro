package com.Lia.assistant.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Lia.assistant.data.*
import com.Lia.assistant.ui.Routes
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun SettingsScreen(appState: NovaAppState, navController: NavController) {
    val ctx = LocalContext.current
    val colors = LocalNovaColors.current
    var apiKey by remember { mutableStateOf(ApiKeyStore.getKey(ctx)) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()) {
        NovaTopBar(title = "Settings")
        NovaGlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = colors.accent.copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = colors.accent, modifier = Modifier.size(32.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(appState.assistantName, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(AssistantBrand.TAGLINE, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
        }
        NovaSectionHeader("AI & Voice")
        NovaSettingsRow(Icons.Default.Psychology, "Personality", value = appState.personality.displayName, onClick = { navController.navigate(Routes.PERSONALITY) })
        NovaSettingsRow(Icons.Default.Language, "Language", value = appState.language.displayName)
        NovaSectionHeader("Gemini API Key")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            NovaTextField(value = apiKey, onValueChange = { apiKey = it }, placeholder = "Paste your Gemini API key", leadingIcon = Icons.Default.Key, isPassword = true)
            Spacer(Modifier.height(8.dp))
            NovaButton(text = "Save Key", onClick = { ApiKeyStore.saveKey(ctx, apiKey) })
        }
        NovaSectionHeader("Appearance")
        NovaSettingsRow(Icons.Default.Palette, "Orb Style", value = appState.orbStyle, onClick = { navController.navigate(Routes.ORB_STYLE) })
        NovaSettingsRow(Icons.Default.DarkMode, "Dark Mode", isSwitch = true, switchState = appState.themeMode != "LIGHT", onSwitchChange = { appState.applyThemeMode(if (it) "DARK" else "LIGHT") })
        NovaSettingsRow(Icons.Default.Animation, "Reduce Motion", isSwitch = true, switchState = appState.reducedMotion, onSwitchChange = { appState.applyReducedMotion(it) })
        NovaSectionHeader("More")
        NovaSettingsRow(Icons.Default.Security, "Permissions", onClick = { navController.navigate(Routes.PERMISSIONS) })
        NovaSettingsRow(Icons.Default.PrivacyTip, "Privacy", onClick = { navController.navigate(Routes.PRIVACY) })
        NovaSettingsRow(Icons.Default.Info, "About", onClick = { navController.navigate(Routes.ABOUT) })
        NovaSettingsRow(Icons.Default.BugReport, "Debug", onClick = { navController.navigate(Routes.DEBUG) })
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AccessibilityDisclosureScreen(onBack: () -> Unit) {
    val colors = LocalNovaColors.current
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Accessibility", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            NovaGlassCard {
                Text("Accessibility Service", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("Lia uses Android Accessibility to read the screen and perform actions when you ask. It is never used to collect personal data.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
