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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.havenos.launcher.ui.glass.AccentRoles
import app.havenos.launcher.ui.glass.AccentSource
import app.havenos.launcher.ui.glass.accentRolesFor
import app.havenos.launcher.ui.glass.accentSelectionOf
import app.havenos.launcher.ui.glass.resolveAccentSeed
import app.havenos.launcher.ui.glass.systemSeedColor
import app.havenos.launcher.ui.glass.wallpaperSeedColors
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
    /**
     * 3.1.1 (item 6): fully opaque drawer surface. Dark = near-black
     * neutral with ~4% accent tint at alpha 1.0 (applied in
     * resolveTokens); light = off-white. Text on it keeps ≥ 4.5:1
     * contrast. No drawer surface may use alpha below 1.0.
     */
    val drawerSurface: Color,
    /**
     * 3.1.1 (item 6): fully opaque drawer field — search field,
     * history/hidden chips, sticky letter headers, letter bubble.
     * A solid fill distinct from drawerSurface, never translucent.
     */
    val drawerField: Color,
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
    drawerSurface: Long, drawerField: Long,
) = Palette(
    Color(bg), Color(surface), Color(onSurface), Color(muted),
    Color(accent), Color(glass), Color(glassBorder), Color(dock),
    Color(drawerSurface), Color(drawerField),
)

/**
 * Opaque mix: [ratio] of [tint] over [base], alpha forced to 1.0.
 * Used for the drawer tokens so no translucent color can leak in.
 */
fun mixOpaque(base: Color, tint: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - r) + tint.red * r,
        green = base.green * (1f - r) + tint.green * r,
        blue = base.blue * (1f - r) + tint.blue * r,
        alpha = 1f,
    )
}

/** Minimal tokens, always — the argument is kept for call-site compatibility. */
fun tokensFor(look: Look): LookTokens = LookTokens(
    light = pal(0xFFFAFAFA, 0xFFFFFFFF, 0xFF111111, 0xFF6E6E6E, 0xFF44586C, 0xFFFFFFFF, 0x14000000, 0xFFFFFFFF, 0xFFFAFAF8, 0xFFECEAE6),
    dark = pal(0xFF000000, 0xFF0D0D0D, 0xFFF5F5F5, 0xFF8A8A8A, 0xFFB8C4D0, 0xFF141414, 0x24FFFFFF, 0xFF0A0A0A, 0xFF0A0A0A, 0xFF1A1A1A),
    corner = 12.dp, glassAlpha = 0.92f, blur = false,
    shadow = 2.dp, borderAlpha = 0.16f, highlightAlpha = 0.06f,
)

/**
 * Resolve the active theme to concrete tokens. Always Minimal; the live
 * user controls in Settings → Look are then applied on top:
 * accent (3.1 Phase B7 selection: curated/custom/wallpaper/system seeds
 * resolved through accent roles, clamped per mode), surface material
 * (Solid 0.92/flat, Translucent 0.78/flat, Frosted 0.62/blur), and corner
 * radius (-1 = Minimal's 12dp). A null seed means the Minimal default
 * accent. Nothing else from the old Looks system survives: stored look
 * names and custom-look JSON are ignored.
 */
fun resolveTokens(
    state: app.havenos.launcher.data.SettingsState,
    wallpaperArgb: Int? = null,
    systemArgb: Int? = null,
): LookTokens {
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
    val selection = accentSelectionOf(
        state.accentSource, state.accentCurated, state.accentCustomArgb,
        state.accentStrength, state.accentRecents,
    )
    // 3.1.1 (item 6): the dark drawer surface is the near-black base
    // mixed with ~4% of the resolved accent (opaque); the drawer
    // field lifts it a further 8% toward white. With no seed the
    // base accents stand in, so the tint still applies. Accent
    // values are identical to the previous code in both branches.
    val seed = resolveAccentSeed(selection, wallpaperArgb, systemArgb)
    val lightAccent = if (seed != null) {
        Color(accentRolesFor(seed, false, selection.strength).accent)
    } else {
        withCorner.light.accent
    }
    val darkAccent = if (seed != null) {
        Color(accentRolesFor(seed, true, selection.strength).accent)
    } else {
        withCorner.dark.accent
    }
    val darkDrawer = mixOpaque(Color(0xFF0A0A0A), darkAccent, 0.04f)
    return withCorner.copy(
        light = withCorner.light.copy(accent = lightAccent),
        dark = withCorner.dark.copy(
            accent = darkAccent,
            drawerSurface = darkDrawer,
            drawerField = mixOpaque(darkDrawer, Color.White, 0.08f),
        ),
    )
}

/** Accent roles for the active mode. Falls back to the Minimal default accent when the selection has no seed. */
fun resolveAccentRoles(
    state: app.havenos.launcher.data.SettingsState,
    wallpaperArgb: Int?,
    systemArgb: Int?,
    isDark: Boolean,
): AccentRoles {
    val selection = accentSelectionOf(
        state.accentSource, state.accentCurated, state.accentCustomArgb,
        state.accentStrength, state.accentRecents,
    )
    val seed = resolveAccentSeed(selection, wallpaperArgb, systemArgb)
        ?: if (isDark) 0xFFB8C4D0.toInt() else 0xFF44586C.toInt()
    return accentRolesFor(seed, isDark, selection.strength)
}

val LocalPalette = staticCompositionLocalOf<Palette> { error("No palette") }
val LocalTokens = staticCompositionLocalOf<LookTokens> { error("No tokens") }
val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** Accent roles for the active mode, provided by HavenTheme. Components read only these. */
val LocalAccentRoles = staticCompositionLocalOf<AccentRoles> { error("No accent roles") }

/** The full settings state, for leaf composables (icon size, labels, badges…). */
val LocalState = staticCompositionLocalOf { app.havenos.launcher.data.SettingsState() }

@Composable
fun HavenTheme(
    state: app.havenos.launcher.data.SettingsState,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val dark = when (state.darkMode) {
        1 -> false
        2 -> true
        3 -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 19 || hour < 7
        }
        else -> isSystemInDarkTheme()
    }
    // 3.1 Phase B7 runtime seeds. Wallpaper colours are re-read when the
    // source or the wallpaper itself changes; a system-wallpaper change
    // while Wallpaper is already selected picks up on next launch (the
    // system posts no wallpaper-change signal this host listens to).
    val wallpaperArgb = remember(
        state.accentSource, state.wallpaperMode, state.wpUri, state.wpKind,
    ) {
        if (state.accentSource == AccentSource.WALLPAPER.name) {
            wallpaperSeedColors(ctx).firstOrNull()
        } else {
            null
        }
    }
    val systemArgb = remember(state.accentSource, dark) {
        if (state.accentSource == AccentSource.SYSTEM.name) systemSeedColor(ctx, dark) else null
    }
    val tokens = remember(
        state.accentSource, state.accentCurated, state.accentCustomArgb,
        state.accentStrength, state.accentRecents, state.surfaceMaterial,
        state.cornerDp, wallpaperArgb, systemArgb,
    ) { resolveTokens(state, wallpaperArgb, systemArgb) }
    val palette = if (dark) tokens.dark else tokens.light
    val roles = remember(
        state.accentSource, state.accentCurated, state.accentCustomArgb,
        state.accentStrength, state.accentRecents, wallpaperArgb, systemArgb, dark,
    ) { resolveAccentRoles(state, wallpaperArgb, systemArgb, dark) }
    // 3.1 Phase B7 purple fix: every container slot is set from accent
    // roles. Unset slots previously fell back to Material's baseline
    // purple, which is what rendered the Default-launcher card purple.
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(roles.onAccent),
            primaryContainer = Color(roles.accentContainer),
            onPrimaryContainer = palette.onSurface,
            secondary = palette.accent,
            secondaryContainer = Color(roles.accentContainer),
            tertiary = palette.accent,
            tertiaryContainer = Color(roles.accentContainer),
            background = palette.bg,
            surface = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color(roles.onAccent),
            primaryContainer = Color(roles.accentContainer),
            onPrimaryContainer = palette.onSurface,
            secondary = palette.accent,
            secondaryContainer = Color(roles.accentContainer),
            tertiary = palette.accent,
            tertiaryContainer = Color(roles.accentContainer),
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
        LocalAccentRoles provides roles,
    ) {
        MaterialTheme(colorScheme = scheme) {
            content()
        }
    }
}
