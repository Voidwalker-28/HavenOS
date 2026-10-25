package app.havenos.launcher.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * The legibility engine (Home Screen v2 prompt §5.2, adapted: M8 —
 * cells are grid cells of the current page grid of the horizontal
 * pager).
 *
 * How it works: on wallpaper change, the wallpaper bitmap is downsampled
 * to ~32px wide and mean luminance + variance are computed per grid
 * cell, OFF the main thread (see [analyzeWallpaper]). The result is
 * cached per wallpaper + size ([WallpaperLegibilityCache]) and recomputed
 * on wallpaper change, dim change, or label-mode change — never during
 * scrolling.
 *
 * The pure core is [Legibility]: no Android types, no I/O, safe to call
 * from tests and from any thread.
 *
 * ---------------------------------------------------------------------------
 * M8 — call-site hook (Worker C; the coordinator wires it into the pager)
 * ---------------------------------------------------------------------------
 * The M7 home never fed this engine real stats, so Auto fell back to the
 * mid-grey default. M8 closes that gap with exactly one call-site helper,
 * [rememberLegibilityCells]. The coordinator calls it ONCE above the
 * horizontal pager (never per icon, never inside a scrolling item) and
 * passes the returned list down to every page:
 *
 * ```
 * val cells = rememberLegibilityCells(wallpaperKey, bitmap, cols, rows)
 * // ... per grid cell (row, col) of the current page:
 * val style = labelStyleFor(
 *     labelModeFromKey(state.labelMode),
 *     cells[row * cols + col].luminance,
 *     cells[row * cols + col].variance,
 * )
 * ```
 *
 * Where the arguments come from:
 * - `wallpaperKey`: a caller-owned identity string that MUST change on
 *   every wallpaper change (e.g. wallpaper mode + kind + URI + tint +
 *   solid colour, as in `WallpaperStore.refresh`'s key). The bitmap
 *   alone never invalidates anything: if the bitmap content changes
 *   without the key changing, the cached sample is (by contract)
 *   still the sample for that key.
 * - `bitmap`: the HavenOS-mode wallpaper bitmap, i.e. the value of
 *   `app.wallpaper.source` (`WallpaperStore.source`, a
 *   `StateFlow<Bitmap?>`) — the same bitmap `WallpaperLayer` draws.
 *   Pass it as-is; do NOT pass `WallpaperStore.blurred` (a 180px
 *   pre-blurred copy — blurring destroys the variance Auto needs).
 * - `cols` / `rows`: the current page grid size. The helper coerces
 *   values below 1 to 1.
 *
 * Cache rule (authoritative for M8): the cache key is
 * `wallpaperKey + grid size`, so a stale sample is never reused across
 * a wallpaper or grid change. Dim and label-mode changes must NOT
 * invalidate the cache — they only change rendering: dim is applied
 * by the caller scaling luminance toward black before
 * [labelStyleFor], and the mode only selects the [labelStyleFor]
 * branch. (The pre-M8 header text that listed dim/mode as recompute
 * triggers is superseded by this rule; [WallpaperLegibilityCache]
 * already documents it.)
 *
 * Threading: analysis runs in [analyzeWallpaper] on
 * `Dispatchers.Default` via a `LaunchedEffect` keyed on the cache key
 * + the bitmap, so it fires on wallpaper/grid change and when a
 * late-loading bitmap first arrives — never on the main thread, never
 * during scrolling (scrolling changes neither key). While computing,
 * the helper returns the mid-grey default list
 * (`CellStats(0.5f, 0f)` per cell, size `cols * rows`, index
 * `row * cols + col`); the state update when analysis finishes swaps
 * in the real stats.
 *
 * Parallax note: stats are computed for the CURRENT page grid over
 * the full wallpaper. Pager parallax shifts the wallpaper by at most
 * 8%, i.e. less than half a grid cell at any shipped grid size, so
 * the per-cell stats stay valid across pages — no per-page
 * resampling is needed.
 *
 * System-wallpaper degradation (no workaround in M8): in system mode
 * (`wallpaperMode == 0`) the window is transparent and the OS draws
 * the wallpaper. Reading it via `WallpaperManager.getWallpaperFile`
 * throws `SecurityException` on API 33+ (probed, never assumed, in
 * `WallpaperStore.probeSystem`, which only records readability and a
 * blurred copy — it never exposes the full bitmap). The coordinator
 * therefore passes `bitmap = null` in system mode, and Auto degrades
 * to the mid-grey default (near-black text, no pill) until a HavenOS
 * wallpaper is used. No `WallpaperManager.set*` path exists or is
 * added here; do not add one to work around this.
 */

// ---------------------------------------------------------------------------
// Label mode setting
// ---------------------------------------------------------------------------

/**
 * Setting key for the label mode. Referenced by name only: the field
 * itself lives in SettingsState (the coordinator adds it to Settings.kt
 * as a String with values "auto" / "pill" / "off"). This file must not
 * read SettingsState directly, or it would not compile until that lands.
 */
const val LABEL_MODE_KEY = "labelMode"

/** Label modes (prompt §5.2): Auto (default), Pill, Off. */
enum class LabelMode { Auto, Pill, Off }

fun labelModeFromKey(value: String?): LabelMode = when (value) {
    "pill" -> LabelMode.Pill
    "off" -> LabelMode.Off
    else -> LabelMode.Auto // "auto", blank, unknown, and null all default to Auto
}

// ---------------------------------------------------------------------------
// Pure core: relative luminance + contrast (WCAG 2.x, sRGB)
// ---------------------------------------------------------------------------

/** The two text colours Auto ever picks: pure white, and near-black. */
val LEGIBILITY_WHITE: Color = Color.White
val LEGIBILITY_NEAR_BLACK: Color = Color(0xFF111111)

/** Luminance of [LEGIBILITY_NEAR_BLACK] (≈ 0.0056); white is 1.0. */
private const val NEAR_BLACK_LUMINANCE = 0.0056f

/**
 * Luminance at which white and near-black text tie for contrast against
 * the background: (1.05 / (L + 0.05)) == ((L + 0.05) / (NEAR_BLACK + 0.05))
 * solves to L ≈ 0.19. Above it, near-black wins; below it, white wins.
 */
private const val TEXT_SWITCH_LUMINANCE = 0.19f

/** Pill threshold (prompt §5.2): text must reach 4.5:1 against its cell. */
const val LEGIBILITY_MIN_CONTRAST = 4.5

/**
 * Variance above which a cell counts as "busy" even if its mean
 * luminance would give a passing contrast. Variance here is the variance
 * of per-pixel luminance (range 0–0.25); 0.04 is a standard deviation of
 * 0.20, i.e. the cell mixes clearly light and dark artwork.
 */
const val LEGIBILITY_HIGH_VARIANCE = 0.04f

/** Soft shadow Auto adds to wallpaper text (prompt §5.2). */
val LEGIBILITY_SHADOW: Shadow =
    Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 4f)

/** Linearise one sRGB channel (0–1) for relative-luminance computation. */
fun linearizeChannel(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

/** Relative luminance of a colour (0 = black, 1 = white), sRGB coefficients. */
fun relativeLuminance(color: Color): Float =
    0.2126f * linearizeChannel(color.red) +
        0.7152f * linearizeChannel(color.green) +
        0.0722f * linearizeChannel(color.blue)

/**
 * Pure legibility decisions. These three functions are the contract
 * other workers call — their signatures are fixed.
 */
object Legibility {

    /**
     * Auto text colour for a cell of the given mean luminance: white on
     * dark cells, near-black on light cells. Always pair with
     * [LEGIBILITY_SHADOW]; check [needsPill] for whether a pill is also
     * needed.
     */
    fun textColorFor(luminance: Float): Color =
        if (luminance > TEXT_SWITCH_LUMINANCE) LEGIBILITY_NEAR_BLACK else LEGIBILITY_WHITE

    /**
     * True when a label on this cell needs a frosted pill behind it:
     * the cell is busy (high variance), or even the better of white /
     * near-black text would still fall below 4.5:1 against the cell's
     * mean luminance.
     */
    fun needsPill(luminance: Float, variance: Float): Boolean {
        if (variance >= LEGIBILITY_HIGH_VARIANCE) return true
        val lum = luminance.coerceIn(0f, 1f)
        val whiteContrast = contrastRatio(1.0f, lum)
        val darkContrast = contrastRatio(NEAR_BLACK_LUMINANCE, lum)
        return maxOf(whiteContrast, darkContrast) < LEGIBILITY_MIN_CONTRAST
    }

    /**
     * WCAG contrast ratio between two luminances:
     * (lighter + 0.05) / (darker + 0.05). Inputs are clamped to 0–1, so
     * the result is always in 1.0–21.0.
     */
    fun contrastRatio(fgLuminance: Float, bgLuminance: Float): Double {
        val fg = fgLuminance.coerceIn(0f, 1f).toDouble()
        val bg = bgLuminance.coerceIn(0f, 1f).toDouble()
        val lighter = maxOf(fg, bg)
        val darker = minOf(fg, bg)
        return (lighter + 0.05) / (darker + 0.05)
    }
}

// ---------------------------------------------------------------------------
// Label rendering decision (mode + cell stats -> what the call site draws)
// ---------------------------------------------------------------------------

/** What a label call site should draw for one cell. */
data class LabelStyle(
    /** Null means draw no label at all (mode Off). */
    val textColor: Color?,
    /** True means draw a small frosted pill behind the label text. */
    val needsPill: Boolean,
)

/**
 * Resolve a label's rendering from its mode and its cell's stats.
 * Auto: colour from luminance, pill only when [Legibility.needsPill]
 * says so. Pill: always a pill (colour still from luminance, so the
 * text stays readable inside it). Off: no label.
 */
fun labelStyleFor(mode: LabelMode, luminance: Float, variance: Float): LabelStyle =
    when (mode) {
        LabelMode.Off -> LabelStyle(textColor = null, needsPill = false)
        LabelMode.Pill -> LabelStyle(
            textColor = Legibility.textColorFor(luminance),
            needsPill = true,
        )
        LabelMode.Auto -> LabelStyle(
            textColor = Legibility.textColorFor(luminance),
            needsPill = Legibility.needsPill(luminance, variance),
        )
    }

// ---------------------------------------------------------------------------
// Wallpaper sampling (off the main thread) + cache
// ---------------------------------------------------------------------------

/** Mean luminance + variance of one wallpaper grid cell. */
data class CellStats(val luminance: Float, val variance: Float)

/** Width, in pixels, that wallpaper sampling downsamples to (prompt §5.2: ~32px). */
const val LEGIBILITY_SAMPLE_WIDTH = 32

/**
 * Pure per-pixel stats for one cell's luminance samples: mean and
 * population variance. Kept separate from Bitmap so it is unit-testable.
 */
fun cellStatsFor(samples: FloatArray): CellStats {
    if (samples.isEmpty()) return CellStats(luminance = 0f, variance = 0f)
    var sum = 0.0
    for (s in samples) sum += s.toDouble()
    val mean = sum / samples.size
    var sq = 0.0
    for (s in samples) {
        val d = s - mean
        sq += d * d
    }
    return CellStats(
        luminance = mean.toFloat().coerceIn(0f, 1f),
        variance = (sq / samples.size).toFloat().coerceIn(0f, 0.25f),
    )
}

/**
 * Downsample [bitmap] to ~32px wide (aspect preserved) and compute one
 * [CellStats] per cell of a [cols] × [rows] grid over it. Runs on
 * [Dispatchers.Default] — never call this on the main thread, and never
 * during scrolling. A null bitmap (e.g. system wallpaper unreadable)
 * yields mid-grey cells (luminance 0.5, variance 0), for which Auto
 * picks near-black text without a pill — a safe, legible default.
 *
 * Recompute triggers (callers): wallpaper change, dim change, mode
 * change. Dim is applied by the caller scaling luminance toward black
 * before calling [labelStyleFor]; this function reports the raw
 * wallpaper so one cached sample serves every dim level.
 */
suspend fun analyzeWallpaper(
    bitmap: Bitmap?,
    cols: Int,
    rows: Int,
): List<CellStats> = withContext(Dispatchers.Default) {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
        return@withContext List(safeCols * safeRows) { CellStats(0.5f, 0f) }
    }
    val targetW = LEGIBILITY_SAMPLE_WIDTH
    val targetH = maxOf(1, (bitmap.height * targetW) / bitmap.width)
    val small = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    try {
        val pixels = IntArray(targetW * targetH)
        small.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        val out = ArrayList<CellStats>(safeCols * safeRows)
        for (row in 0 until safeRows) {
            val y0 = (row * targetH) / safeRows
            val y1 = ((row + 1) * targetH) / safeRows
            for (col in 0 until safeCols) {
                val x0 = (col * targetW) / safeCols
                val x1 = ((col + 1) * targetW) / safeCols
                val samples = FloatArray(maxOf(1, (x1 - x0) * (y1 - y0)))
                var i = 0
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val px = pixels[y * targetW + x]
                        val r = linearizeChannel(((px shr 16) and 0xFF) / 255f)
                        val g = linearizeChannel(((px shr 8) and 0xFF) / 255f)
                        val b = linearizeChannel((px and 0xFF) / 255f)
                        samples[i++] = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    }
                }
                out += cellStatsFor(samples.copyOf(i))
            }
        }
        out
    } finally {
        if (small !== bitmap) small.recycle()
    }
}

/**
 * Cache of wallpaper samples, keyed by wallpaper identity + grid size.
 * The caller owns the key: it must change on wallpaper change (e.g. the
 * wallpaper URI / mode / generation) and on grid-size change, so a stale
 * sample is never reused. Dim and label-mode changes do NOT invalidate
 * the cache — they only change how cached stats are rendered.
 * Thread-safe; values are computed once per key by the caller's factory.
 */
class WallpaperLegibilityCache {
    private val cache = ConcurrentHashMap<String, List<CellStats>>()

    fun getOrPut(key: String, compute: () -> List<CellStats>): List<CellStats> =
        cache.getOrPut(key, compute)

    fun invalidate() {
        cache.clear()
    }

    fun invalidateKey(key: String) {
        cache.remove(key)
    }
}

// ---------------------------------------------------------------------------
// M8 call-site helper (the only addition in M8; pure core untouched)
// ---------------------------------------------------------------------------

/**
 * M8 call-site helper: real per-cell wallpaper stats for the home
 * pager. Holds a remembered [WallpaperLegibilityCache], launches
 * [analyzeWallpaper] on a background dispatcher when the wallpaper
 * identity, the grid size, or the bitmap itself changes (never on the
 * main thread, never during scrolling), and returns the cached
 * `List<CellStats>` of size `cols * rows` (index `row * cols + col`),
 * or the mid-grey default list (`CellStats(0.5f, 0f)` per cell) while
 * computing or when [bitmap] is null.
 *
 * Cache key: `"$wallpaperKey|${cols}x${rows}"` — wallpaper identity
 * plus grid size, so a stale sample is never reused. Dim and
 * label-mode changes do NOT invalidate the cache: they only change
 * how the caller renders the cached stats via [labelStyleFor].
 * [wallpaperKey] must change whenever the wallpaper content changes;
 * a bitmap change with an unchanged key keeps the cached sample (see
 * the file header for the full integration hook, the bitmap source,
 * the parallax note, and the system-wallpaper degradation note).
 *
 * Call once above the pager and pass the list down — not once per
 * icon and not inside a scrolling item. This function does not read
 * SettingsState, does not set any wallpaper, and adds no permission
 * or network use.
 */
@Composable
fun rememberLegibilityCells(
    wallpaperKey: String,
    bitmap: Bitmap?,
    cols: Int,
    rows: Int,
): List<CellStats> {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val defaultCells = remember(safeCols, safeRows) {
        List(safeCols * safeRows) { CellStats(0.5f, 0f) }
    }
    val cache = remember { WallpaperLegibilityCache() }
    val cacheKey = "$wallpaperKey|${safeCols}x${safeRows}"
    var cells by remember(cacheKey) { mutableStateOf(defaultCells) }
    // Keys: cacheKey (wallpaper identity + grid size) plus bitmap, so a
    // late-arriving HavenOS bitmap (source starts null) still triggers
    // analysis. Scrolling changes neither key, so scrolling never
    // recomputes. analyzeWallpaper itself moves the work to
    // Dispatchers.Default; this effect only awaits it.
    LaunchedEffect(cacheKey, bitmap) {
        val computed = analyzeWallpaper(bitmap, safeCols, safeRows)
        cells = cache.getOrPut(cacheKey) { computed }
    }
    return cells
}
