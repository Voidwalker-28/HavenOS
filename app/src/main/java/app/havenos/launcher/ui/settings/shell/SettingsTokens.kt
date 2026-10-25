package app.havenos.launcher.ui.settings.shell

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.theme.Palette

// ---------------------------------------------------------------------------
// M6 shell tokens (v2 §9). Tonal surfaces, no outlines: a card is a flat
// fill on the panel, never a bordered box. Light AND dark are both defined
// below; the live accent always overrides the default accent (and the
// accent-tinted surfaces derived from it) via shellTokensFor().
// ---------------------------------------------------------------------------

/** Semantic colours for the settings panel and its cards. */
data class ShellTokens(
    val panelSurface: Color,
    val cardSurface: Color,
    /** Accent at ~16% alpha over the panel — selected states, chips. */
    val cardSelected: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val danger: Color,
    val success: Color,
    val focusRing: Color,
)

/** Light tokens. Secondary text on the panel surface stays above 4.5:1. */
val ShellLightTokens: ShellTokens = ShellTokens(
    panelSurface = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFF1F1F1),
    cardSelected = Color(0xFF44586C).copy(alpha = 0.16f),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF6E6E6E),
    accent = Color(0xFF44586C),
    danger = Color(0xFFC62828),
    success = Color(0xFF2E7D32),
    focusRing = Color(0xFF44586C),
)

/** Dark tokens, same structure as light. */
val ShellDarkTokens: ShellTokens = ShellTokens(
    panelSurface = Color(0xFF0D0D0D),
    cardSurface = Color(0xFF1D1D1D),
    cardSelected = Color(0xFFB8C4D0).copy(alpha = 0.16f),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF8A8A8A),
    accent = Color(0xFFB8C4D0),
    danger = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
    focusRing = Color(0xFFB8C4D0),
)

/**
 * Live tokens for the current theme: pick light/dark from the palette's
 * background luminance, then replace the default accent (and everything
 * derived from it) with the user's accent, so the panel re-themes live
 * as the accent changes. The panel host provides the result through
 * [LocalShellTokens]; hub screens read it, never the static sets above.
 */
fun shellTokensFor(palette: Palette): ShellTokens {
    val dark = palette.bg.luminance() < 0.5f
    val base = if (dark) ShellDarkTokens else ShellLightTokens
    return base.copy(
        accent = palette.accent,
        cardSelected = palette.accent.copy(alpha = 0.16f),
        focusRing = palette.accent,
    )
}

val LocalShellTokens: androidx.compose.runtime.ProvidableCompositionLocal<ShellTokens> =
    staticCompositionLocalOf { ShellLightTokens }

/** Radii (v2 §9): panel 28dp, cards 20dp, chips and inputs 12dp. */
object ShellRadii {
    val panel = 28.dp
    val card = 20.dp
    val chip = 12.dp
}

/** Spacing scale (v2 §9): 4 / 8 / 12 / 16 / 24 / 32dp. */
object ShellSpacing {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    /** Card padding and the gap between cards (16dp / 12dp). */
    val cardPadding = 16.dp
    val cardGap = 12.dp
    val sectionGap = 24.dp
}

/** Type scale (v2 §9). Callers copy with LocalFont for the family. */
object ShellText {
    val title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    val section = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
    )
    val rowTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val value = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val help = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
}
