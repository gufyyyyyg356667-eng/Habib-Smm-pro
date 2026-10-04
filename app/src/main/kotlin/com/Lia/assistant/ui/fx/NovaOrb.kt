package com.Lia.assistant.ui.fx

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import com.Lia.assistant.voice.NovaOrbState
import kotlin.math.*

enum class NovaOrbStyle {
    NOVA, AURORA, PLASMA, GLASS, ENERGY, MINIMAL, ARCHER
}

@Composable
fun NovaOrb(
    state: NovaOrbState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    size: Float = 240f
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "breathe"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                when (state) {
                    NovaOrbState.THINKING -> 2600
                    NovaOrbState.CONNECTING -> 1100
                    else -> 9000
                }, easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val scale = when (state) {
        NovaOrbState.LISTENING -> 1.12f + amplitude * 0.28f
        NovaOrbState.SPEAKING -> 1.05f + amplitude * 0.35f
        else -> 1.0f
    }

    // Core colour by state
    val coreColor = when (state) {
        NovaOrbState.LISTENING -> colors.lagoon
        NovaOrbState.SPEAKING -> colors.lotus.copy(alpha = 0.9f + amplitude * 0.1f)
        NovaOrbState.THINKING -> colors.accent
        NovaOrbState.CONNECTING -> Color(0xFFFFA000)
        NovaOrbState.ERROR -> colors.error
        else -> colors.lotus.copy(alpha = 0.6f)
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Lia status: $state"
        }
    ) {
        val radius = (size / 2) * scale
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val breatheOffset = if (reducedMotion) 0f else sin(breathe) * radius * 0.035f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(cx, cy), radius = radius * 1.6f
            ), radius = radius * 1.6f, center = Offset(cx, cy)
        )

        // Listening rings
        if (state == NovaOrbState.LISTENING && !reducedMotion) {
            repeat(2) { i ->
                val ring = rememberInfiniteTransition(label = "ring$i")
                // Simplified rings
                drawCircle(
                    color = coreColor.copy(alpha = 0.15f - i * 0.07f),
                    radius = radius * (1.3f + i * 0.25f),
                    center = Offset(cx, cy),
                    style = Stroke(2f)
                )
            }
        }

        // Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.95f),
                    coreColor.copy(alpha = 0.5f),
                    colors.background.copy(alpha = 0.2f)
                ),
                center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                radius = radius
            ),
            radius = (radius + breatheOffset).coerceAtLeast(1f),
            center = Offset(cx, cy)
        )

        // Rotating arc for connecting
        if (state == NovaOrbState.CONNECTING) {
            val sweepAngle = 100f
            drawArc(
                color = coreColor,
                startAngle = rotation,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(3f, cap = StrokeCap.Round)
            )
        }

        // Highlight arc for glass style
        if (style == NovaOrbStyle.GLASS) {
            drawArc(
                color = Color.White.copy(alpha = 0.4f),
                startAngle = -145f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(cx - radius * 0.7f, cy - radius * 0.7f),
                size = Size(radius * 1.4f, radius * 1.4f),
                style = Stroke(4f, cap = StrokeCap.Round)
            )
        }

        // Error pulse
        if (state == NovaOrbState.ERROR) {
            drawCircle(
                color = colors.error.copy(alpha = 0.3f),
                radius = radius * 1.2f,
                center = Offset(cx, cy),
                style = Stroke(2f)
            )
        }
    }
}

// ── Splash session tracker ────────────────────────────────────────────────────
object SplashSession {
    var played = false
}

// ── Listening Edge Glow ───────────────────────────────────────────────────────
@Composable
fun ListeningEdgeGlow(state: NovaOrbState, modifier: Modifier = Modifier) {
    if (state == NovaOrbState.IDLE || state == NovaOrbState.CONNECTING) return
    val color = when (state) {
        NovaOrbState.LISTENING -> Color(0xFF8B5CF6)
        NovaOrbState.SPEAKING -> Color(0xFFFF6FAE)
        NovaOrbState.THINKING -> Color(0xFFFFB547)
        NovaOrbState.ERROR -> Color(0xFFFF7A85)
        else -> Color(0xFF8B5CF6)
    }
    val alpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    Canvas(modifier = modifier) {
        val thickness = 26.dp.toPx()
        // Top
        drawRect(Brush.verticalGradient(listOf(color.copy(alpha), Color.Transparent)), size = Size(this.size.width, thickness))
        // Bottom
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha))),
            topLeft = Offset(0f, this.size.height - thickness), size = Size(this.size.width, thickness))
        // Left
        drawRect(Brush.horizontalGradient(listOf(color.copy(alpha), Color.Transparent)), size = Size(thickness, this.size.height))
        // Right
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha))),
            topLeft = Offset(this.size.width - thickness, 0f), size = Size(thickness, this.size.height))
    }
}

@Composable
private fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
