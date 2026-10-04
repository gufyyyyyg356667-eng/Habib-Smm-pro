package com.Lia.assistant.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.fx.NovaOrb
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val colors = LocalNovaColors.current
    val pages = listOf(
        Triple("Meet Lia", "Your intelligent AI companion.", NovaOrbState.IDLE),
        Triple("Just Talk", "Hindi, Hinglish or English — Lia understands.", NovaOrbState.LISTENING),
        Triple("Your Assistant", "Opens apps, calls contacts, builds websites.", NovaOrbState.THINKING),
        Triple("Your Privacy", "Your API key stays on your device only.", NovaOrbState.IDLE)
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Skip", color = colors.textSecondary) }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val (title, subtitle, state) = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NovaOrb(state = state, amplitude = 0.3f, modifier = Modifier.size(180.dp))
                Spacer(Modifier.height(40.dp))
                Text(title, style = MaterialTheme.typography.displayMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val sel = pagerState.currentPage == i
                Surface(shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (sel) colors.accent else colors.textTertiary,
                    modifier = Modifier.padding(4.dp).size(if (sel) 10.dp else 6.dp)) {}
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            NovaButton(
                text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Continue",
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) onDone()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            )
        }
    }
}
