package com.Lia.assistant.ui.screens.forge

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.forge.*
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*

@Composable
fun ForgeScreen(appState: NovaAppState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = LocalNovaColors.current
    val state by ForgeController.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        when (state.phase) {
            ForgePhase.IDLE -> ForgeIdleView(colors, onBack)
            ForgePhase.BUILDING -> ForgeBuildView(state, colors, onBack)
            ForgePhase.DONE -> ForgeDoneView(ctx, state, colors, onBack)
            ForgePhase.FAILED -> ForgeErrorView(state, colors, onBack)
            ForgePhase.CANCELLED -> ForgeIdleView(colors, onBack)
        }
    }
}

@Composable
private fun ForgeIdleView(colors: NovaColorScheme, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Language, null, tint = colors.accent, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Website Forge", style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Text("Say or type what website you want to build", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        Spacer(Modifier.height(32.dp))
        NovaButton(text = "Back", onClick = onBack, style = NovaButtonStyle.SECONDARY)
    }
}

@Composable
private fun ForgeBuildView(state: ForgeUiState, colors: NovaColorScheme, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated background — simplified forge reactor visual
        ForgeReactorCanvas(state = state, modifier = Modifier.fillMaxSize())

        // HUD — top prompt chip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.surfaceGlass,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.prompt.take(50).let { if (state.prompt.length > 50) "$it…" else it },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // HUD — bottom status card
        ForgeStatusCard(state = state, colors = colors, onCancel = {
            ForgeController.cancel()
            onBack()
        }, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}

@Composable
private fun ForgeReactorCanvas(state: ForgeUiState, modifier: Modifier = Modifier) {
    val colors = LocalNovaColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "forge")
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "forge_rot"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2

        // Background glow
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(colors.accent.copy(alpha = 0.15f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                radius = size.minDimension * 0.6f
            ),
            radius = size.minDimension * 0.6f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Progress ring
        val progress = state.progress.coerceIn(0f, 1f)
        drawArc(
            color = colors.accent.copy(alpha = 0.3f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - 100f, cy - 100f),
            size = androidx.compose.ui.geometry.Size(200f, 200f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(4f)
        )
        drawArc(
            color = colors.accent,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - 100f, cy - 100f),
            size = androidx.compose.ui.geometry.Size(200f, 200f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Rotating arc (reactor ring)
        drawArc(
            color = colors.lagoon.copy(alpha = 0.6f),
            startAngle = rotation,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - 130f, cy - 130f),
            size = androidx.compose.ui.geometry.Size(260f, 260f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Core
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(colors.lotus, colors.accent.copy(alpha = 0.5f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                radius = 60f
            ),
            radius = 60f,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // Stage slab indicators
        state.sections.take(6).forEachIndexed { i, _ ->
            val angle = (i * 60f - 90f) * (Math.PI / 180).toFloat()
            val r = 160f
            val sx = cx + r * kotlin.math.cos(angle)
            val sy = cy + r * kotlin.math.sin(angle)
            drawCircle(color = colors.accent.copy(alpha = 0.4f + i * 0.1f), radius = 8f, center = androidx.compose.ui.geometry.Offset(sx, sy))
        }
    }
}

@Composable
private fun ForgeStatusCard(
    state: ForgeUiState,
    colors: NovaColorScheme,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalNovaReducedMotion.current
    val animatedPct by animateFloatAsState(
        targetValue = (state.progress * 100).coerceIn(0f, 98f),
        animationSpec = tween(600), label = "pct"
    )

    NovaGlassCard(modifier = modifier.fillMaxWidth()) {
        // Stage label
        AnimatedContent(
            targetState = state.stage.label,
            transitionSpec = { slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut() },
            label = "stage"
        ) { label ->
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.accent)
        }

        Spacer(Modifier.height(8.dp))

        // Percentage
        Text(
            "${animatedPct.toInt()}%",
            style = MaterialTheme.typography.displayLarge,
            color = colors.textPrimary
        )

        Spacer(Modifier.height(8.dp))

        // Stage track — 6 segments
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val totalStages = 6
            val currentStage = state.stage.ordinal
            repeat(totalStages) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= currentStage) colors.accent else colors.surfaceBorder)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                "${state.metrics.chars / 1024} KB" to "Written",
                "${state.metrics.sections}" to "Sections",
                "${state.metrics.tokensPerSec.toInt()}" to "tok/s",
                "${state.metrics.elapsedSec.toInt()}s" to "Elapsed"
            ).forEach { (value, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Recent log lines
        state.log.takeLast(3).forEach { line ->
            Text("> $line", style = MaterialTheme.typography.labelMedium, color = colors.textTertiary)
        }

        Spacer(Modifier.height(12.dp))

        NovaButton(
            text = "Cancel",
            onClick = onCancel,
            style = NovaButtonStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ForgeDoneView(ctx: Context, state: ForgeUiState, colors: NovaColorScheme, onBack: () -> Unit) {
    var showToolbar by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Sandboxed WebView for generated page
        AndroidView(
            factory = {
                WebView(it).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // External links go to system browser
                            val uri = request.url
                            if (uri.host != "localhost") {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                try { it.startActivity(intent) } catch (_: Exception) {}
                                return true
                            }
                            return false
                        }
                    }
                    val html = state.file?.readText() ?: ""
                    loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Auto-hiding toolbar
        if (showToolbar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceGlass),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary) }
                IconButton(onClick = {
                    val file = state.file ?: return@IconButton
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        ctx, "${ctx.packageName}.forge.fileprovider", file
                    )
                    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/html"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(android.content.Intent.createChooser(share, "Share website"))
                }) { Icon(Icons.Default.Share, "Share", tint = colors.textPrimary) }
                IconButton(onClick = {
                    val html = state.file?.readText() ?: return@IconButton
                    val url = state.prompt.take(20).replace(" ", "_")
                    val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "text/html"
                        putExtra(android.content.Intent.EXTRA_TITLE, "$url.html")
                    }
                    // Requires Activity context for startActivityForResult — simplified here
                }) { Icon(Icons.Default.Save, "Save", tint = colors.textPrimary) }
                IconButton(onClick = { showToolbar = false }) { Icon(Icons.Default.Close, "Hide toolbar", tint = colors.textPrimary) }
            }
        }

        // Handle to bring toolbar back
        if (!showToolbar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent)
            ) {
                IconButton(onClick = { showToolbar = true }) {
                    Icon(Icons.Default.MoreVert, "Show toolbar", tint = colors.onAccent)
                }
            }
        }
    }
}

@Composable
private fun ForgeErrorView(state: ForgeUiState, colors: NovaColorScheme, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = colors.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Build failed", style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        Text(state.error ?: "Something went wrong.", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaButton(text = "Retry", onClick = {
                val ctx = androidx.compose.ui.platform.LocalContext
                // retry handled via ForgeController
            }, style = NovaButtonStyle.PRIMARY)
            NovaButton(text = "Back", onClick = onBack, style = NovaButtonStyle.SECONDARY)
        }
    }
}
