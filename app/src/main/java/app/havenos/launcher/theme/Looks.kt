package app.havenos.launcher.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar

// Minimal-only theme (2026-10-23 user direction): the former Looks system
// (Glass/Soft/Futuristic/Classy/Retro/Premium, Look Editor, .havenlook
// files) was removed. [Look] survives as a single-value enum only so
// existing call sites and stored strings keep compiling; every resolution
// below returns Minimal tokens. The glass surface treatment lives on as
// part of Minimal's material (see Glass.kt, unchanged).

enum class Look {
    Minimal;

    companion object {
        /** Any stored name (old Look, custom id, blank) resolves to Minimal. */
        fun fromName(name: String): Look = Minimal
    }
}

data class Palette(
    val bg: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color,
    val accent: Color,
    val glass: Color,
    val glassBorder: Color,
    val dock: Color,
)

data class LookTokens(
    val light: Palette,
    val dark: Palette,
    val corner: Dp,
    val glassAlpha: Float,
    val blur: Boolean,
    /** Drop-shadow elevation for glass surfaces. */
    val shadow: Dp = 6.dp,
    /** Alpha of the 1px specular border (top edge reads brighter via a gradient). */
    val borderAlpha: Float = 0.28f,
    /** Alpha of the soft inner highlight strip at a surface's top edge. */
    val highlightAlpha: Float = 0.18f,
    /** Clock face style this Look prefers (serif editorial vs clean sans). */
    val serifClock: Boolean = false,
)

private fun pal(
    bg: Long, surface: Long, onSurface: Long, muted: Long,
    accent: Long, glass: Long, glassBorder: Long, dock: Long,
) = Palette(
    Color(bg), Color(surface), Color(onSurface), Color(muted),
    Color(accent), Color(glass), Color(glassBorder), Color(dock),
)

/** Minimal tokens, always — the argument is kept for call-site compatibility. */
fun tokensFor(look: Look): LookTokens = LookTokens(
    light = pal(0xFFFAFAFA, 0xFFFFFFFF, 0xFF111111, 0xFF6E6E6E, 0xFF44586C, 0xFFFFFFFF, 0x14000000, 0xFFFFFFFF),
    dark = pal(0xFF000000, 0xFF0D0D0D, 0xFFF5F5F5, 0xFF8A8A8A, 0xFFB8C4D0, 0xFF141414, 0x24FFFFFF, 0xFF0A0A0A),
    corner = 12.dp, glassAlpha = 0.92f, blur = false,
    shadow = 2.dp, borderAlpha = 0.16f, highlightAlpha = 0.06f,
)

/**
 * Resolve the active theme to concrete tokens. Always Minimal; the live
 * user controls in Settings → Style & Theme are then applied on top:
 * accent (palette/custom only — wallpaper-derived is kept for backup round-trip only, no
 * colour extraction exists), surface material (the old Cat1 mapping:
 * Solid 0.92/flat, Translucent 0.78/flat, Frosted 0.62/blur), and corner
 * radius (-1 = Minimal's 12dp). Nothing else from the old Looks system
 * survives: stored look names and custom-look JSON are ignored.
 */
fun resolveTokens(state: app.havenos.launcher.data.SettingsState): LookTokens {
    val base = tokensFor(Look.Minimal)
    val withSurface = when (state.surfaceMaterial) {
        0 -> base.copy(glassAlpha = 0.92f, blur = false)
        1 -> base.copy(glassAlpha = 0.78f, blur = false)
        else -> base.copy(glassAlpha = 0.62f, blur = true)
    }
    val withCorner = if (state.cornerDp >= 0) {
        withSurface.copy(corner = state.cornerDp.dp)
    } else {
        withSurface
    }
    val accentColor = if ((state.accentMode == 1 || state.accentMode == 2) && state.accentArgb != 0) {
        Color(state.accentArgb)
    } else {
        null
    }
    if (accentColor == null) return withCorner
    return withCorner.copy(
        light = withCorner.light.copy(accent = accentColor),
        dark = withCorner.dark.copy(accent = accentColor),
    )
}

val LocalPalette = staticCompositionLocalOf<Palette> { error("No palette") }
val LocalTokens = staticCompositionLocalOf<LookTokens> { error("No tokens") }
val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** The full settings state, for leaf composables (icon size, labels, badges…). */
val LocalState = staticCompositionLocalOf { app.havenos.launcher.data.SettingsState() }

@Composable
fun HavenTheme(
    state: app.havenos.launcher.data.SettingsState,
    content: @Composable () -> Unit,
) {
    val tokens = remember(
        state.accentMode, state.accentArgb, state.surfaceMaterial, state.cornerDp,
    ) { resolveTokens(state) }
    val dark = when (state.darkMode) {
        1 -> false
        2 -> true
        3 -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 19 || hour < 7
        }
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) tokens.dark else tokens.light
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.bg,
            surface = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.bg,
            surface = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
        )
    }
    val font = when (state.fontChoice) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    CompositionLocalProvider(
        LocalTokens provides tokens,
        LocalPalette provides palette,
        LocalFont provides font,
        LocalState provides state,
    ) {
        MaterialTheme(colorScheme = scheme) {
            content()
        }
    }
}
