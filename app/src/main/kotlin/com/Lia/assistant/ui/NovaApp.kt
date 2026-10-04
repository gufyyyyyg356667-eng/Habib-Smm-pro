package com.Lia.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.ui.screens.about.AboutScreen
import com.Lia.assistant.ui.screens.chat.ChatScreen
import com.Lia.assistant.ui.screens.debug.DebugScreen
import com.Lia.assistant.ui.screens.forge.ForgeScreen
import com.Lia.assistant.ui.screens.history.HistoryScreen
import com.Lia.assistant.ui.screens.home.HomeScreen
import com.Lia.assistant.ui.screens.onboarding.OnboardingScreen
import com.Lia.assistant.ui.screens.permissions.PermissionsScreen
import com.Lia.assistant.ui.screens.personality.PersonalityScreen
import com.Lia.assistant.ui.screens.orbstyle.OrbStyleScreen
import com.Lia.assistant.ui.screens.privacy.PrivacyScreen
import com.Lia.assistant.ui.screens.profile.ProfileScreen
import com.Lia.assistant.ui.screens.quickactions.QuickActionsScreen
import com.Lia.assistant.ui.screens.settings.SettingsScreen
import com.Lia.assistant.ui.screens.settings.AccessibilityDisclosureScreen
import com.Lia.assistant.ui.screens.voice.VoiceScreen
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceSessionManager

private val BOTTOM_NAV_ROUTES = setOf("home", "chat", "history", "settings")

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val QUICK_ACTIONS = "quick_actions"
    const val PROFILE = "profile"
    const val PERSONALITY = "personality"
    const val ORB_STYLE = "orb_style"
    const val PRIVACY = "privacy"
    const val PERMISSIONS = "permissions"
    const val ABOUT = "about"
    const val DEBUG = "debug"
    const val FORGE = "forge"
    const val ACCESSIBILITY_DISCLOSURE = "accessibility_disclosure"
}

@Composable
fun NovaApp(appState: NovaAppState) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    val pendingOpen by ForgeController.pendingOpen.collectAsStateWithLifecycle()
    val voiceState by VoiceSessionManager.state.collectAsStateWithLifecycle()
    val colors = LocalNovaColors.current

    // Consume pending Forge open
    LaunchedEffect(pendingOpen) {
        if (pendingOpen) {
            ForgeController.consumeOpen()
            navController.navigate(Routes.FORGE) { launchSingleTop = true }
        }
    }

    Box(modifier = Modifier.fillMaxSize().nightSky()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    LiaBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onTalk = { navController.navigate(Routes.VOICE) { launchSingleTop = true } },
                        voiceState = voiceState
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = if (appState.hasOnboarded) Routes.HOME else Routes.ONBOARDING,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onDone = {
                        appState.applyOnboarded()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        appState = appState,
                        onTalk = { navController.navigate(Routes.VOICE) },
                        onType = { navController.navigate(Routes.CHAT) },
                        onProfile = { navController.navigate(Routes.PROFILE) }
                    )
                }
                composable(Routes.VOICE) {
                    VoiceScreen(appState = appState, onClose = { navController.popBackStack() })
                }
                composable(Routes.CHAT) {
                    ChatScreen(appState = appState, onBack = { navController.popBackStack() })
                }
                composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        appState = appState,
                        navController = navController
                    )
                }
                composable(Routes.QUICK_ACTIONS) {
                    QuickActionsScreen(
                        appState = appState,
                        onBack = { navController.popBackStack() },
                        onOpenForge = { navController.navigate(Routes.FORGE) }
                    )
                }
                composable(Routes.PROFILE) { ProfileScreen(appState = appState, onBack = { navController.popBackStack() }) }
                composable(Routes.PERSONALITY) {
                    PersonalityScreen(appState = appState, onBack = { navController.popBackStack() })
                }
                composable(Routes.ORB_STYLE) {
                    OrbStyleScreen(appState = appState, onBack = { navController.popBackStack() })
                }
                composable(Routes.PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.PERMISSIONS) { PermissionsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.DEBUG) { DebugScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.FORGE) { ForgeScreen(appState = appState, onBack = { navController.popBackStack() }) }
                registerAccessibilityDisclosureRoute { navController.popBackStack() }
            }
        }

        // Edge glow overlay
        if (voiceState != NovaOrbState.IDLE) {
            ListeningEdgeGlow(state = voiceState, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun LiaBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onTalk: () -> Unit,
    voiceState: NovaOrbState
) {
    val colors = LocalNovaColors.current
    val isActive = voiceState != NovaOrbState.IDLE

    NavigationBar(
        containerColor = colors.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onNavigate(Routes.HOME) },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") },
            colors = navItemColors(colors)
        )
        NavigationBarItem(
            selected = currentRoute == Routes.CHAT,
            onClick = { onNavigate(Routes.CHAT) },
            icon = { Icon(Icons.Default.Chat, "Chat") },
            label = { Text("Chat") },
            colors = navItemColors(colors)
        )
        // Centre Talk orb
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isActive) colors.lotus else colors.accent),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onTalk) {
                    Icon(
                        if (isActive) Icons.Default.MicNone else Icons.Default.Mic,
                        "Talk",
                        tint = colors.onAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        NavigationBarItem(
            selected = currentRoute == Routes.HISTORY,
            onClick = { onNavigate(Routes.HISTORY) },
            icon = { Icon(Icons.Default.History, "History") },
            label = { Text("History") },
            colors = navItemColors(colors)
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            colors = navItemColors(colors)
        )
    }
}

@Composable
private fun navItemColors(colors: com.Lia.assistant.ui.theme.NovaColorScheme) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = colors.accent,
        selectedTextColor = colors.accent,
        unselectedIconColor = colors.textSecondary,
        unselectedTextColor = colors.textSecondary,
        indicatorColor = colors.accent.copy(alpha = 0.15f)
    )
