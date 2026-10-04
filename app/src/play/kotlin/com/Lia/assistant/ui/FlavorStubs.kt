package com.Lia.assistant.ui

import android.content.Context
import androidx.navigation.NavGraphBuilder

// No-op accessibility key manager for play build
object AccessKeyManager {
    fun isActive(ctx: Context): Boolean = true
    fun hasKey(ctx: Context): Boolean = true
    fun refreshInfo(ctx: Context) {}
}

// No-op overlay edge glow for play build
object OverlayEdgeGlowController {
    fun start(ctx: Context) {}
    fun stop(ctx: Context) {}
}

// No-op flavor routes
fun NavGraphBuilder.registerAccessibilityDisclosureRoute(
    navigateBack: () -> Unit
) { /* play flavor: no accessibility disclosure route */ }
