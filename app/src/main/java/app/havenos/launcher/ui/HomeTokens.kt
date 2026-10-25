package app.havenos.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.theme.GlassState
import app.havenos.launcher.theme.GlassTier
import app.havenos.launcher.theme.LocalGlass
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.Palette
import kotlin.math.floor

/**
 * Home tokens for HavenOS M7 Wave 1 (Home Screen v2 prompt §§3, 5.2, 5.7,
 * adapted to the user's latest directions).
 *
 * EXTENDS the existing token system, it does not fork it: colours resolve
 * from [Palette] (theme/Looks.kt), surfaces draw with GlassSurface
 * (theme/Glass.kt), and the material descriptor below only names which
 * Glass tier is in effect. Minimal only — there are no preset overrides
 * here: circle icons, flat surfaces, airy spacing, fast motion, and a
 * single mono accent. Accent recolours buttons, highlights and active
 * states only; it never recolours a whole surface.
 *
 * There is no page concept anywhere in this file: home is one continuous
 * scroll (favorites → widgets/folders → A–Z, alphabet rail).
 */

/** Android window size classes, from width only (prompt §3.1). */
enum class HomeSizeClass { Compact, Medium, Expanded }

/** Size-class boundaries in dp: Compact < 600, Medium 600–840, Expanded ≥ 840. */
fun sizeClassFor(widthDp: Float): HomeSizeClass = when {
    widthDp < 600f -> HomeSizeClass.Compact
    widthDp < 840f -> HomeSizeClass.Medium
    else -> HomeSizeClass.Expanded
}

fun sizeClassFor(widthDp: Int): HomeSizeClass = sizeClassFor(widthDp.toFloat())

/**
 * Recomputes on configuration change: [LocalConfiguration] is a
 * composition local, so rotation / split-screen / resize re-runs this
 * and every consumer re-lays out. Nothing is cached across a config
 * change and nothing reads a device model.
 */
@Composable
fun rememberHomeSizeClass(): HomeSizeClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) { sizeClassFor(widthDp) }
}

/** Spacing scale (prompt §3.2): exactly 4 / 8 / 12 / 16 / 24 / 32 / 48dp. */
object HomeSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

/** Radii (prompt §3.2): dock = full pill, cards/folders/panels = 28dp, chips = 12dp. */
object HomeRadii {
    /** Full pill: a shape, not a fixed dp, so it stays a pill at any height. */
    val dock: Shape = RoundedCornerShape(percent = 50)
    val card: Dp = 28.dp
    val folder: Dp = 28.dp
    val panel: Dp = 28.dp
    val chip: Dp = 12.dp

    fun cardShape(): Shape = RoundedCornerShape(card)
    fun folderShape(): Shape = RoundedCornerShape(folder)
    fun panelShape(): Shape = RoundedCornerShape(panel)
    fun chipShape(): Shape = RoundedCornerShape(chip)
}

/**
 * Type scale (prompt §3.2). Only the label size varies by size class:
 * 12sp on Compact/Medium, 13sp on Expanded. Calm list is a range
 * (28–34sp light): [calmList] is the Expanded end; Compact/Medium use
 * [calmListCompact].
 */
data class HomeType(
    val glance: TextStyle,
    val todayTitle: TextStyle,
    val label: TextStyle,
    val sectionHeader: TextStyle,
    val searchField: TextStyle,
    val calmList: TextStyle,
    val calmListCompact: TextStyle,
)

fun homeTypeFor(sizeClass: HomeSizeClass): HomeType {
    val labelSize: TextUnit = when (sizeClass) {
        HomeSizeClass.Expanded -> 13.sp
        else -> 12.sp
    }
    return HomeType(
        glance = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
        todayTitle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        label = TextStyle(fontSize = labelSize),
        // Section header: 12sp caps, muted — the muted colour comes from
        // HomeColors.textSecondary; caps are applied at the call site
        // (uppercase()) so the style stays a plain size here.
        sectionHeader = TextStyle(fontSize = 12.sp),
        searchField = TextStyle(fontSize = 17.sp),
        calmList = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Light),
        calmListCompact = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Light),
    )
}

/**
 * Semantic colours (prompt §3.2). Resolved from the existing [Palette]
 * so light/dark variants and the user's accent stay in one place.
 *
 * Contrast (WCAG, text on its usual background — checked against the
 * Minimal palette in Looks.kt):
 * - light: textPrimary #111111 on surface #FFFFFF ≈ 18.9:1; textSecondary
 *   #6E6E6E on #FFFFFF ≈ 5.0:1; both pass 4.5:1.
 * - dark: textPrimary #F5F5F5 on surface #0D0D0D ≈ 18.1:1; textSecondary
 *   #8A8A8A on #0D0D0D ≈ 5.4:1; both pass 4.5:1.
 * - danger: #B3261E on white ≈ 5.9:1; #F2B8B5 on #0D0D0D ≈ 10.2:1.
 * - textOnWallpaper is NOT a fixed colour: it is chosen per wallpaper
 *   cell by Legibility.textColorFor (see Legibility.kt). The two fields
 *   here are its two possible values.
 */
data class HomeColors(
    val wallpaperScrim: Color,
    val surfaceGlass: Color,
    val surfaceSolid: Color,
    val textOnWallpaperLight: Color,
    val textOnWallpaperDark: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val badge: Color,
    val focusRing: Color,
    val danger: Color,
    val isDark: Boolean,
)

/** Resolve semantic colours from the existing palette. Accent is the palette accent. */
fun homeColorsFor(palette: Palette, isDark: Boolean): HomeColors = HomeColors(
    wallpaperScrim = Color.Black.copy(alpha = 0.35f),
    surfaceGlass = palette.glass,
    surfaceSolid = palette.surface,
    textOnWallpaperLight = Color.White,
    textOnWallpaperDark = Color(0xFF111111),
    textPrimary = palette.onSurface,
    textSecondary = palette.muted,
    accent = palette.accent,
    badge = if (isDark) Color(0xFFEF9A9A) else Color(0xFFE53935),
    focusRing = palette.accent,
    danger = if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E),
    isDark = isDark,
)

@Composable
fun rememberHomeColors(isDark: Boolean): HomeColors {
    val palette = LocalPalette.current
    return remember(palette, isDark) { homeColorsFor(palette, isDark) }
}

/**
 * Material descriptor (prompt §3.2): the user's chosen material
 * (solid / translucent / frosted, from SettingsState.surfaceMaterial)
 * as already resolved into [GlassState] by the home host — see
 * Glass.kt's detectGlassTier + GlassSurface. This file adds no second
 * material implementation.
 *
 * Capability + frame-time guard with automatic fallback to translucent:
 * a Flat tier (no blur available, or Reduce Effects on) never draws a
 * blur — GlassSurface falls back to a flat translucent surface — so
 * [effectiveBlur] is false exactly when the fallback is in effect.
 */
data class HomeMaterial(
    val tier: GlassTier,
    val effectiveBlur: Boolean,
    val fallbackToTranslucent: Boolean,
)

fun homeMaterialFor(glass: GlassState): HomeMaterial {
    val fallback = glass.tier == GlassTier.Flat || glass.reduceEffects
    return HomeMaterial(
        tier = glass.tier,
        effectiveBlur = !fallback && glass.tier != GlassTier.Flat,
        fallbackToTranslucent = fallback,
    )
}

@Composable
fun rememberHomeMaterial(): HomeMaterial {
    val glass = LocalGlass.current
    return remember(glass) { homeMaterialFor(glass) }
}

/** Everything a home composable needs, resolved from the existing theme. */
data class HomeTokens(
    val sizeClass: HomeSizeClass,
    val spacing: HomeSpacing,
    val radii: HomeRadii,
    val type: HomeType,
    val colors: HomeColors,
    val material: HomeMaterial,
)

@Composable
fun rememberHomeTokens(isDark: Boolean): HomeTokens {
    val sizeClass = rememberHomeSizeClass()
    val colors = rememberHomeColors(isDark)
    val material = rememberHomeMaterial()
    return remember(sizeClass, colors, material) {
        HomeTokens(
            sizeClass = sizeClass,
            spacing = HomeSpacing,
            radii = HomeRadii,
            type = homeTypeFor(sizeClass),
            colors = colors,
            material = material,
        )
    }
}

/**
 * Grid columns (prompt §4.1): columns = floor(contentWidth / cellWidth),
 * clamped to [minColumns]..[maxColumns]. Cell width ≈ icon + 56dp; the
 * caller passes the resolved cell width so density settings keep working.
 * Pure — unit-tested in HomeTokensTest.
 */
fun gridColumns(
    contentWidthDp: Float,
    cellWidthDp: Float,
    minColumns: Int = 1,
    maxColumns: Int = 12,
): Int {
    if (contentWidthDp <= 0f || cellWidthDp <= 0f) return minColumns
    val raw = floor(contentWidthDp / cellWidthDp).toInt()
    return raw.coerceIn(minColumns, maxColumns)
}

/** Default cell width for a grid: icon size + 56dp of label/gap room (prompt §4.1). */
fun gridCellWidthDp(iconDp: Float): Float = iconDp + 56f
