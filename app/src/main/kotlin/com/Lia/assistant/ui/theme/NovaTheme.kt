package com.Lia.assistant.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp

// ── Colours ───────────────────────────────────────────────────────────────────
data class NovaColorScheme(
    val background: Color,
    val backgroundGradientTop: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceBorder: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val userBubble: Color,
    val assistantBubble: Color,
    val depthShadow: Color,
    val rimLight: Color,
    val lagoon: Color,
    val lotus: Color
)

val DarkNovaColors = NovaColorScheme(
    background = Color(0xFF06050E),
    backgroundGradientTop = Color(0xFF15113A),
    surface = Color(0xFF17143A),
    surfaceGlass = Color(0x2217143A),
    surfaceBorder = Color(0x33A7A0CC),
    surfaceRaised = Color(0xFF221E4D),
    accent = Color(0xFFFFB547),
    accentSecondary = Color(0xFFFF8A3D),
    accentGlow = Color(0x33FFB547),
    onAccent = Color(0xFF1A1000),
    textPrimary = Color(0xFFEEE9FF),
    textSecondary = Color(0xFFA7A0CC),
    textTertiary = Color(0xFF6B6490),
    success = Color(0xFF3FE0D0),
    warning = Color(0xFFFFB547),
    error = Color(0xFFFF7A85),
    userBubble = Color(0xFF2A2060),
    assistantBubble = Color(0xFF17143A),
    depthShadow = Color(0x661A1060),
    rimLight = Color(0x33FFFFFF),
    lagoon = Color(0xFF3FE0D0),
    lotus = Color(0xFFFF6FAE)
)

val LightNovaColors = NovaColorScheme(
    background = Color(0xFFF3F0FB),
    backgroundGradientTop = Color(0xFFE8E3F8),
    surface = Color(0xFFFFFFFF),
    surfaceGlass = Color(0xCCFFFFFF),
    surfaceBorder = Color(0x2215112E),
    surfaceRaised = Color(0xFFF0ECF8),
    accent = Color(0xFFB86A00),
    accentSecondary = Color(0xFFCC5500),
    accentGlow = Color(0x33B86A00),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF15112E),
    textSecondary = Color(0xFF4A4570),
    textTertiary = Color(0xFF8880AA),
    success = Color(0xFF00897B),
    warning = Color(0xFFB86A00),
    error = Color(0xFFD32F2F),
    userBubble = Color(0xFFE8E3F8),
    assistantBubble = Color(0xFFFFFFFF),
    depthShadow = Color(0x3315112E),
    rimLight = Color(0x55FFFFFF),
    lagoon = Color(0xFF00897B),
    lotus = Color(0xFFC2185B)
)

val LocalNovaColors = staticCompositionLocalOf { DarkNovaColors }
val LocalNovaReducedMotion = staticCompositionLocalOf { false }

// ── Typography ─────────────────────────────────────────────────────────────
val ManropeFontFamily = FontFamily(Font(com.Lia.assistant.R.font.manrope_variable))
val InstrumentSerifFontFamily = FontFamily(Font(com.Lia.assistant.R.font.instrument_serif_regular))

val NovaTypography = Typography(
    displayLarge = TextStyle(fontFamily = InstrumentSerifFontFamily, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = InstrumentSerifFontFamily, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = ManropeFontFamily, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = ManropeFontFamily, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = ManropeFontFamily, fontSize = 12.sp, lineHeight = 16.sp),
)

// ── Theme composable ──────────────────────────────────────────────────────────
@Composable
fun NovaTheme(
    mode: String = "DARK",
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (mode == "LIGHT") LightNovaColors else DarkNovaColors
    val isDark = mode != "LIGHT"

    val m3Colors = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            error = colors.error
        )
    }

    CompositionLocalProvider(
        LocalNovaColors provides colors,
        LocalNovaReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = NovaTypography,
            content = content
        )
    }
}

// ── Spacing & Shape tokens ────────────────────────────────────────────────────
object NovaSpacing {
    val xs = 4
    val sm = 8
    val md = 12
    val lg = 16
    val xl = 24
    val xxl = 32
    val xxxl = 48
}

object NovaMotion {
    const val fast = 150
    const val normal = 300
    const val slow = 500
}
