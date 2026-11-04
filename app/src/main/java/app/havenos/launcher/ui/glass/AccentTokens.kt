package app.havenos.launcher.ui.glass

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B7 — accent system. Pure Kotlin: no Compose, no Android
// types, so every rule here is unit-testable on the JVM. Colours are ARGB
// ints; composables convert with Color(argb) at the call site.
// ---------------------------------------------------------------------------

/** Accent sources, as offered in Look → Accent. DEFAULT is internal only: it is the legacy Minimal default accent (never offered in the UI) and the migration target for old mode 0. */
enum class AccentSource { DEFAULT, WALLPAPER, SYSTEM, CURATED, CUSTOM }

/** Strength sets how much accent tints surfaces (soft container alpha). Default Subtle. */
enum class AccentStrength { SUBTLE, BALANCED, VIVID }

/** One accent selection: everything the UI needs, nothing it must derive twice. */
data class AccentSelection(
    val source: AccentSource = AccentSource.CURATED,
    val curated: String = "Ember",
    val customArgb: Int = 0,
    val strength: AccentStrength = AccentStrength.SUBTLE,
    /** Newest first, at most 6. Stored as comma-separated 6-digit hex. */
    val recents: List<Int> = emptyList(),
)

/** The twelve curated colours (prompt §B7 starting values, names kept). */
data class CuratedAccent(val name: String, val argb: Int)

val CURATED_ACCENTS: List<CuratedAccent> = listOf(
    CuratedAccent("Ember", 0xFFF0A35E.toInt()),
    CuratedAccent("Clay", 0xFFD98C7A.toInt()),
    CuratedAccent("Rose", 0xFFE58AA6.toInt()),
    CuratedAccent("Orchid", 0xFFC490E4.toInt()),
    CuratedAccent("Iris", 0xFF9B8CFF.toInt()),
    CuratedAccent("Cobalt", 0xFF6FA0FF.toInt()),
    CuratedAccent("Sky", 0xFF7CC4F2.toInt()),
    CuratedAccent("Lagoon", 0xFF5CCBC0.toInt()),
    CuratedAccent("Sage", 0xFF8FC9A0.toInt()),
    CuratedAccent("Moss", 0xFFB5CF7A.toInt()),
    CuratedAccent("Sand", 0xFFE3CFA0.toInt()),
    CuratedAccent("Mono", 0xFFE8E8EA.toInt()),
)

fun curatedArgb(name: String): Int =
    CURATED_ACCENTS.firstOrNull { it.name == name }?.argb ?: CURATED_ACCENTS[0].argb

/** Every role a component may read. Components read only these, never a raw seed. */
data class AccentRoles(
    val seed: Int,
    /** Text and icons on glass: seed clamped to ≥4.5:1 on the mode background. */
    val accent: Int,
    /** Text on accent fills: black or white, whichever passes ≥4.5:1 (one always does). */
    val onAccent: Int,
    /** Soft fills: the seed at the strength alpha. */
    val accentContainer: Int,
    /** Borders and icon outlines: seed clamped to ≥3:1 on the mode background. */
    val accentOutline: Int,
    /** True when the seed was moved to meet the text rule (drives the "Adjusted for readability" note). */
    val clamped: Boolean,
)

/** Soft-container alpha per strength (prompt §B7: ~10% / 18% / 30%). */
fun containerAlpha(strength: AccentStrength): Float = when (strength) {
    AccentStrength.SUBTLE -> 0.10f
    AccentStrength.BALANCED -> 0.18f
    AccentStrength.VIVID -> 0.30f
}

// ---------------------------------------------------------------------------
// Contrast math (WCAG relative luminance). All pure.
// ---------------------------------------------------------------------------

private fun channelToLinear(c: Int): Double {
    val s = c / 255.0
    return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
}

/** Relative luminance of an ARGB colour (alpha ignored). */
fun relativeLuminance(argb: Int): Double {
    val r = channelToLinear((argb shr 16) and 0xFF)
    val g = channelToLinear((argb shr 8) and 0xFF)
    val b = channelToLinear(argb and 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG contrast ratio between two ARGB colours (1.0–21.0). */
fun contrastRatio(a: Int, b: Int): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val (hi, lo) = if (l1 >= l2) l1 to l2 else l2 to l1
    return (hi + 0.05) / (lo + 0.05)
}

private data class Hsl(val h: Float, val s: Float, val l: Float)

private fun rgbToHsl(argb: Int): Hsl {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f
    if (maxC == minC) return Hsl(0f, 0f, l)
    val d = maxC - minC
    val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
    val h = when (maxC) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    return Hsl(h, s, l)
}

private fun hslToArgb(h: Float, s: Float, l: Float): Int {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun ch(v: Float): Int = ((v + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}

/** Public HSL helpers for the Custom accent picker (Look hub). */
fun argbToHslTriple(argb: Int): Triple<Float, Float, Float> {
    val hsl = rgbToHsl(argb)
    return Triple(hsl.h, hsl.s, hsl.l)
}

fun hslTripleToArgb(h: Float, s: Float, l: Float): Int = hslToArgb(h, s, l)

/**
 * Move [seed] in lightness (hue and saturation kept) until it reaches
 * [minRatio] against [background]. Returns the adjusted colour and
 * whether it moved. A seed that fails is clamped, never rejected: if
 * neither pole reaches the ratio, the extreme with the better ratio wins.
 */
fun clampForContrast(seed: Int, background: Int, minRatio: Double): Pair<Int, Boolean> {
    if (contrastRatio(seed, background) >= minRatio) return seed to false
    val hsl = rgbToHsl(seed)
    // Walk lightness away from the background's lightness first (nearest fix),
    // then toward the opposite pole; 1% steps, at most 100.
    val bgLight = rgbToHsl(background).l
    val directions = if (hsl.l > bgLight) {
        listOf(1f, -1f)
    } else {
        listOf(-1f, 1f)
    }
    for (dir in directions) {
        var step = 1
        while (step <= 100) {
            val l = (hsl.l + dir * step / 100f).coerceIn(0f, 1f)
            val candidate = hslToArgb(hsl.h, hsl.s, l)
            if (contrastRatio(candidate, background) >= minRatio) return candidate to true
            if ((dir > 0 && l >= 1f) || (dir < 0 && l <= 0f)) break
            step++
        }
    }
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    return (if (contrastRatio(black, background) >= contrastRatio(white, background)) black else white) to true
}

/** Reference backgrounds for the contrast rules: Minimal dark surface / light surface. */
const val DARK_GLASS_BG: Int = 0xFF141414.toInt()
const val LIGHT_GLASS_BG: Int = 0xFFFFFFFF.toInt()

/**
 * One seed → all roles (prompt §B7). Deterministic: same inputs always
 * produce the same roles. Dark and light are separate calls.
 */
fun accentRolesFor(seed: Int, isDark: Boolean, strength: AccentStrength): AccentRoles {
    val bg = if (isDark) DARK_GLASS_BG else LIGHT_GLASS_BG
    val (accent, clamped) = clampForContrast(seed, bg, 4.5)
    val (outline, _) = clampForContrast(seed, bg, 3.0)
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    val onAccent = if (contrastRatio(black, accent) >= contrastRatio(white, accent)) black else white
    val alpha = (containerAlpha(strength) * 255f).roundToInt().coerceIn(0, 255)
    val container = (alpha shl 24) or (seed and 0xFFFFFF)
    return AccentRoles(seed, accent, onAccent, container, outline, clamped)
}

// ---------------------------------------------------------------------------
// HSV helpers for the Custom picker (hue strip + saturation/lightness square).
// ---------------------------------------------------------------------------

/** ARGB → (hue 0–360, saturation 0–1, value 0–1). */
fun argbToHsv(argb: Int): Triple<Float, Float, Float> {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val d = maxC - minC
    val s = if (maxC == 0f) 0f else d / maxC
    val h = when {
        d == 0f -> 0f
        maxC == r -> (60f * ((g - b) / d) + 360f) % 360f
        maxC == g -> 60f * ((b - r) / d) + 120f
        else -> 60f * ((r - g) / d) + 240f
    }
    return Triple(h, s, maxC)
}

/** (hue 0–360, saturation 0–1, value 0–1) → opaque ARGB. */
fun hsvToArgb(h: Float, s: Float, v: Float): Int {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun ch(f: Float): Int = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}

// ---------------------------------------------------------------------------
// Selection: resolution, migration, names, recents.
// ---------------------------------------------------------------------------

/**
 * Resolve a selection to a seed, given the runtime seeds (null when the
 * source has no seed yet). Null means "use the Minimal default accent".
 * Wallpaper and System never auto-apply a colour: with no runtime seed
 * they fall back to the default until the user picks a suggestion.
 */
fun resolveAccentSeed(
    selection: AccentSelection,
    wallpaperArgb: Int?,
    systemArgb: Int?,
): Int? = when (selection.source) {
    AccentSource.DEFAULT -> null
    AccentSource.CURATED -> curatedArgb(selection.curated)
    AccentSource.CUSTOM -> if (selection.customArgb != 0) selection.customArgb else curatedArgb("Ember")
    AccentSource.WALLPAPER -> wallpaperArgb
    AccentSource.SYSTEM -> systemArgb
}

/**
 * Migrate a pre-3.1 accent (old mode/argb) to a selection. Additive and
 * forward-only: old keys are kept for backup round-trip, never dropped.
 * - mode 0 (Minimal default) → DEFAULT (visible accent unchanged).
 * - mode 1 (palette) → nearest curated; argb 0 → DEFAULT (old code fell
 *   through to the default for argb 0 too).
 * - mode 2 (custom hex) → CUSTOM, colour kept, seeded into recents.
 * - mode 3 (wallpaper round-trip, never rendered) → WALLPAPER; the stored
 *   argb was never visible, so there is nothing visible to preserve.
 */
fun migrateAccent(oldMode: Int, oldArgb: Int): AccentSelection = when (oldMode) {
    1 -> if (oldArgb != 0) {
        AccentSelection(AccentSource.CURATED, nearestCurated(oldArgb).name)
    } else {
        AccentSelection(AccentSource.DEFAULT)
    }
    2 -> if (oldArgb != 0) {
        AccentSelection(AccentSource.CUSTOM, customArgb = oldArgb, recents = listOf(oldArgb))
    } else {
        AccentSelection(AccentSource.DEFAULT)
    }
    3 -> AccentSelection(AccentSource.WALLPAPER)
    else -> AccentSelection(AccentSource.DEFAULT)
}

/** Nearest curated colour by redmean RGB distance (cheap perceptual approximation). */
fun nearestCurated(argb: Int): CuratedAccent {
    val r = ((argb shr 16) and 0xFF).toDouble()
    val g = ((argb shr 8) and 0xFF).toDouble()
    val b = (argb and 0xFF).toDouble()
    return CURATED_ACCENTS.minByOrNull { c ->
        val cr = ((c.argb shr 16) and 0xFF).toDouble()
        val cg = ((c.argb shr 8) and 0xFF).toDouble()
        val cb = (c.argb and 0xFF).toDouble()
        val rMean = (r + cr) / 2.0
        val dr = r - cr
        val dg = g - cg
        val db = b - cb
        (2.0 + rMean / 256.0) * dr * dr + 4.0 * dg * dg + (2.0 + (255.0 - rMean) / 256.0) * db * db
    } ?: CURATED_ACCENTS[0]
}

/** Display name for a selection, for the Changes list. Never a raw int. */
fun accentDisplayName(selection: AccentSelection): String = when (selection.source) {
    AccentSource.DEFAULT -> "Default"
    AccentSource.WALLPAPER -> "Wallpaper"
    AccentSource.SYSTEM -> "System"
    AccentSource.CURATED -> if (CURATED_ACCENTS.any { it.name == selection.curated }) selection.curated else "Ember"
    AccentSource.CUSTOM -> if (selection.customArgb != 0) {
        "Custom #%06X".format(selection.customArgb and 0xFFFFFF)
    } else {
        "Custom"
    }
}

/** Parse a stored source name; unknown names fall back to Curated. */
fun accentSourceFromName(name: String): AccentSource =
    AccentSource.entries.firstOrNull { it.name == name } ?: AccentSource.CURATED

/** Build a selection from stored primitives (SettingsState fields). */
fun accentSelectionOf(
    source: String,
    curated: String,
    customArgb: Int,
    strength: Int,
    recents: String,
): AccentSelection = AccentSelection(
    source = accentSourceFromName(source),
    curated = curated,
    customArgb = customArgb,
    strength = AccentStrength.entries.getOrNull(strength.coerceIn(0, 2)) ?: AccentStrength.SUBTLE,
    recents = parseRecents(recents),
)

/** Parse the stored recents string (comma-separated hex, newest first). */
fun parseRecents(stored: String): List<Int> {
    if (stored.isBlank()) return emptyList()
    return stored.split(",").mapNotNull { part ->
        part.trim().takeIf { it.isNotEmpty() }?.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
    }.distinct().take(6)
}

/** Encode recents (newest first, at most 6, deduplicated). */
fun encodeRecents(recents: List<Int>): String =
    recents.distinct().take(6).joinToString(",") { "%06X".format(it and 0xFFFFFF) }

/** Push a colour onto recents (newest first, at most 6). */
fun pushRecent(recents: List<Int>, argb: Int): List<Int> =
    (listOf(argb) + recents.filter { it != argb }).take(6)
