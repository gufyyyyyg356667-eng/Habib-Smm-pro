package com.Lia.assistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

fun Modifier.nightSky(): Modifier = composed {
    val colors = LocalNovaColors.current
    this.background(
        Brush.verticalGradient(listOf(colors.backgroundGradientTop, colors.background))
    ).drawBehind {
        val rng = Random(42)
        repeat(60) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            val r = rng.nextFloat() * 1.5f + 0.5f
            val alpha = rng.nextFloat() * 0.5f + 0.2f
            drawCircle(Color.White.copy(alpha = alpha), radius = r, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

fun Modifier.depthSurface(
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp
): Modifier = composed {
    val colors = LocalNovaColors.current
    this
        .drawBehind {
            // ambient coloured shadow
            drawRoundRect(
                color = colors.depthShadow,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                topLeft = androidx.compose.ui.geometry.Offset(0f, elevation.toPx()),
                size = this.size
            )
        }
        .background(colors.surface, shape)
        .drawBehind {
            // top rim light
            drawLine(
                color = colors.rimLight,
                start = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 1.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
}
