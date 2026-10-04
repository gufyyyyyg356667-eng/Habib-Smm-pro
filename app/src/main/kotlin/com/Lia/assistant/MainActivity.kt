package com.Lia.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.NovaApp
import com.Lia.assistant.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_FORGE = "open_forge"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleForgeIntent(intent)

        setContent {
            val appState = remember { NovaAppState(this) }
            NovaTheme(mode = appState.themeMode, reducedMotion = appState.reducedMotion) {
                NovaApp(appState = appState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleForgeIntent(intent)
    }

    private fun handleForgeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_FORGE, false) == true) {
            ForgeController.requestOpen()
        }
    }
}
