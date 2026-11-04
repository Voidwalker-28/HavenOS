package app.havenos.launcher.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.theme.GlassTier
import app.havenos.launcher.theme.LocalGlass
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B1–B3 — Haven Glass tokens. One entry point for every
// non-home surface: Modifier.glass(level), the components in
// GlassComponents.kt, and the tokens below. No screen may build its own
// surface colour, radius, or shadow after Phase C adopts this module.
//
// Blur honesty: glass() adds NO new blur mechanism. It draws the
// level tint + hairline; where the host window set blur-behind, the
// existing window blur shows through (the same Partial-tier mechanism
// GlassSurface uses). Backdrop sampling from the pre-blurred wallpaper
// copy stays in GlassSurface for home/dock only and is deliberately NOT
// duplicated here. Depth comes from blur, tint, and hairline — never
// shadows: glass() draws no shadow.
// ---------------------------------------------------------------------------

/** Glass1 cards/widget frames/grouped lists, Glass2 panels/sheets/drawer, Glass3 menus/popovers/dialogs/bubbles. */
enum class GlassLevel { GLASS1, GLASS2, GLASS3 }

/** Tint alpha per level and mode (prompt §B2). Glass3-light is unspecified in the prompt: white 85% is the inference (menus/dialogs must stay legible over wallpaper). */
fun glassTintAlpha(level: GlassLevel, isDark: Boolean): Float = when (level) {
    GlassLevel.GLASS1 -> if (isDark) 0.07f else 0.05f
    GlassLevel.GLASS2 -> if (isDark) 0.12f else 0.70f
    GlassLevel.GLASS3 -> if (isDark) 0.16f else 0.85f
}

/** Tint base colour per level and mode. */
fun glassTintBase(level: GlassLevel, isDark: Boolean): Color = when (level) {
    GlassLevel.GLASS1 -> if (isDark) Color.White else Color.Black
    else -> Color.White
}

/** Hairline alpha per level (white in dark, black in light). */
fun glassHairlineAlpha(level: GlassLevel): Float = when (level) {
    GlassLevel.GLASS1 -> 0.12f
    GlassLevel.GLASS2 -> 0.16f
    GlassLevel.GLASS3 -> 0.18f
}

/** Fallback tint bump when blur is unavailable or Reduce Effects is on (prompt §B2: +6 points, same layout). */
const val GLASS_FALLBACK_BUMP: Float = 0.06f

/** Legibility scrim under text-carrying glass: ≥40% dark scrim (dark) / 60% light scrim (light) for 4.5:1 on any wallpaper. */
fun glassScrimAlpha(isDark: Boolean): Float = if (isDark) 0.40f else 0.60f

fun glassScrimColor(isDark: Boolean): Color =
    if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.60f)

/** Spacing scale (prompt §B3): exactly 4 / 8 / 12 / 16 / 24 / 32 dp. */
object HavenSpacing {
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
}

/** Radii (prompt §B3): card 28, group 20, control 14, chip full, icon tile 12. */
object HavenRadii {
    val card: Dp = 28.dp
    val group: Dp = 20.dp
    val control: Dp = 14.dp
    val iconTile: Dp = 12.dp
    val chip: Shape = RoundedCornerShape(percent = 50)

    fun cardShape(): Shape = RoundedCornerShape(card)
    fun groupShape(): Shape = RoundedCornerShape(group)
    fun controlShape(): Shape = RoundedCornerShape(control)
    fun iconTileShape(): Shape = RoundedCornerShape(iconTile)
    fun sheetShape(): Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    fun menuShape(): Shape = RoundedCornerShape(20.dp)
    fun dialogShape(): Shape = RoundedCornerShape(28.dp)
}

/** Default shape per level (callers pass an explicit shape when the level has two, e.g. Glass1 group vs card). */
fun GlassLevel.defaultShape(): Shape = when (this) {
    GlassLevel.GLASS1 -> HavenRadii.cardShape()
    GlassLevel.GLASS2 -> HavenRadii.cardShape()
    GlassLevel.GLASS3 -> HavenRadii.menuShape()
}

/**
 * Type scale (prompt §B3). Line height ~1.35× size. Font family is left
 * unspecified: callers copy with LocalFont, following the ShellText
 * convention. Section labels render in the accent colour at the call site.
 */
object HavenType {
    val displayNumerals = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Light, lineHeight = 76.sp)
    val heroNumerals = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Light, lineHeight = 46.sp)
    val title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, lineHeight = 38.sp)
    val sectionLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp, lineHeight = 16.sp)
    val rowLabel = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
    val rowValue = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
}

/** Row height minimum (prompt §B3): 56 dp, growing for two lines. Touch targets 48 dp. */
val HAVEN_ROW_MIN_HEIGHT: Dp = 56.dp
val HAVEN_TOUCH_TARGET: Dp = 48.dp

/**
 * The one glass surface modifier. Clips to [shape], draws the level tint
 * and hairline. Fallback (Reduce Effects on, or Flat tier meaning no
 * blur is available): same layout, tint alpha +6 points, never a fully
 * transparent text background. Never draws a shadow.
 */
@Composable
fun Modifier.glass(level: GlassLevel, shape: Shape = level.defaultShape()): Modifier {
    val palette = LocalPalette.current
    val glass = LocalGlass.current
    val state = LocalState.current
    val isDark = palette.bg.luminance() < 0.5f
    val fallback = state.reduceEffects || glass.tier == GlassTier.Flat
    val tintAlpha = (glassTintAlpha(level, isDark) + if (fallback) GLASS_FALLBACK_BUMP else 0f).coerceAtMost(1f)
    val tint = glassTintBase(level, isDark).copy(alpha = tintAlpha)
    val hairline = (if (isDark) Color.White else Color.Black).copy(alpha = glassHairlineAlpha(level))
    return this
        .clip(shape)
        .background(tint, shape)
        .border(1.dp, hairline, shape)
}
