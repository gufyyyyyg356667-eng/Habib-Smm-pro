package com.Lia.assistant.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.Lia.assistant.ui.screens.settings.AccessibilityDisclosureScreen

object AccessKeyManager {
    fun isActive(ctx: Context): Boolean = true // Simplified — full impl in step 13
    fun hasKey(ctx: Context): Boolean = true
    fun refreshInfo(ctx: Context) {}
}

object OverlayEdgeGlowController {
    fun start(ctx: Context) {}
    fun stop(ctx: Context) {}
}

fun NavGraphBuilder.registerAccessibilityDisclosureRoute(
    navigateBack: () -> Unit
) {
    composable("accessibility_disclosure") {
        AccessibilityDisclosureScreen(onBack = navigateBack)
    }
}
