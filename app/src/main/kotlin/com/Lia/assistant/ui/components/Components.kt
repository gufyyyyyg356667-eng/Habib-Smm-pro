package com.Lia.assistant.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.Lia.assistant.ui.theme.*

// ── Glass Card ────────────────────────────────────────────────────────────────
@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content
    )
}

// ── Button ────────────────────────────────────────────────────────────────────
enum class NovaButtonStyle { PRIMARY, SECONDARY, TEXT }

@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NovaButtonStyle = NovaButtonStyle.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = tween(NovaMotion.fast), label = "btn_scale"
    )

    val bg = when (style) {
        NovaButtonStyle.PRIMARY -> if (enabled) colors.accent else colors.textTertiary
        NovaButtonStyle.SECONDARY -> colors.surfaceGlass
        NovaButtonStyle.TEXT -> Color.Transparent
    }
    val textColor = when (style) {
        NovaButtonStyle.PRIMARY -> colors.onAccent
        else -> colors.textPrimary
    }
    val border = when (style) {
        NovaButtonStyle.SECONDARY -> BorderStroke(1.dp, colors.surfaceBorder)
        else -> null
    }

    Row(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}

// ── Text Field ────────────────────────────────────────────────────────────────
@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true
) {
    val colors = LocalNovaColors.current
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
        placeholder = { Text(placeholder, color = colors.textTertiary) },
        leadingIcon = if (leadingIcon != null) { { Icon(leadingIcon, null, tint = colors.textSecondary) } } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide" else "Show",
                        tint = colors.textSecondary
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.surfaceBorder,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent,
            focusedContainerColor = colors.surfaceGlass,
            unfocusedContainerColor = colors.surfaceGlass
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Top Bar ───────────────────────────────────────────────────────────────────
@Composable
fun NovaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary)
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) 16.dp else 0.dp)
        )
        actions()
    }
}

// ── Settings Row ──────────────────────────────────────────────────────────────
@Composable
fun NovaSettingsRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    isSwitch: Boolean = false,
    switchState: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        when {
            isSwitch -> Switch(
                checked = switchState,
                onCheckedChange = onSwitchChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
            )
            value != null -> Text(value, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            onClick != null -> Icon(Icons.Default.ChevronRight, null, tint = colors.textTertiary)
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
@Composable
fun NovaSectionHeader(title: String) {
    val colors = LocalNovaColors.current
    Text(
        title.uppercase(),
        color = colors.accent,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ── Message Bubble ────────────────────────────────────────────────────────────
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

@Composable
fun NovaMessageBubble(message: ChatMessage) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (message.isUser) 20.dp else 4.dp,
                        topEnd = if (message.isUser) 4.dp else 20.dp,
                        bottomStart = 20.dp, bottomEnd = 20.dp
                    )
                )
                .background(if (message.isUser) colors.userBubble else colors.assistantBubble)
                .border(
                    1.dp,
                    if (message.isError) colors.error else colors.surfaceBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                message.text,
                color = if (message.isError) colors.error else colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Typing Dots ───────────────────────────────────────────────────────────────
@Composable
fun NovaTypingDots() {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val alpha by rememberInfiniteTransition(label = "dot$i").animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha$i"
            )
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(colors.accent.copy(alpha = if (reducedMotion) 0.7f else alpha))
            )
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
fun NovaEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = colors.textTertiary)
        Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        if (actionText != null && onAction != null) {
            NovaButton(text = actionText, onClick = onAction)
        }
    }
}

// ── Voice Visualizer ──────────────────────────────────────────────────────────
@Composable
fun NovaVoiceVisualizer(amplitude: Float, modifier: Modifier = Modifier) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    val bars = 48
    Canvas(modifier = modifier) {
        val barWidth = size.width / bars / 1.5f
        val gap = size.width / bars
        for (i in 0 until bars) {
            val normalised = if (reducedMotion) 0.3f else {
                val phase = (i.toFloat() / bars) * Math.PI.toFloat() * 2
                (amplitude * 0.7f + 0.3f) * (0.5f + 0.5f * kotlin.math.sin(phase))
            }
            val barH = size.height * normalised.coerceIn(0.05f, 1f)
            drawRoundRect(
                color = colors.accent.copy(alpha = 0.7f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = i * gap + gap / 2 - barWidth / 2,
                    y = size.height / 2 - barH / 2
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
private fun Canvas(modifier: Modifier, onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
