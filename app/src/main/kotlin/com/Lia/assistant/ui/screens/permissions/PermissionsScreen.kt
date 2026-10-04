package com.Lia.assistant.ui.screens.permissions

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = LocalNovaColors.current
    fun has(p: String) = ctx.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED

    var hasMic by remember { mutableStateOf(has(Manifest.permission.RECORD_AUDIO)) }
    var hasContacts by remember { mutableStateOf(has(Manifest.permission.READ_CONTACTS)) }
    var hasCall by remember { mutableStateOf(has(Manifest.permission.CALL_PHONE)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMic = has(Manifest.permission.RECORD_AUDIO)
        hasContacts = has(Manifest.permission.READ_CONTACTS)
        hasCall = has(Manifest.permission.CALL_PHONE)
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar(title = "Permissions", onBack = onBack)
        listOf(
            Triple(Icons.Default.Mic, "Microphone" to hasMic, Manifest.permission.RECORD_AUDIO),
            Triple(Icons.Default.Contacts, "Contacts" to hasContacts, Manifest.permission.READ_CONTACTS),
            Triple(Icons.Default.Phone, "Phone" to hasCall, Manifest.permission.CALL_PHONE)
        ).forEach { (icon, pair, perm) ->
            val (title, granted) = pair
            NovaSettingsRow(icon = icon, title = title, value = if (granted) "✓ Granted" else "Tap to grant",
                onClick = { if (!granted) launcher.launch(perm) })
        }
    }
}
