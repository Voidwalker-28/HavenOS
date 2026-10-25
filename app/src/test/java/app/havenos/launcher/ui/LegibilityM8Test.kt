package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M8 legibility tests (Worker C). Boundary-focused complements to
 * HomeTokensTest's M7 coverage — no test is duplicated here:
 * HomeTokensTest already covers plain contrastRatio, plain
 * textColorFor light/dark, plain needsPill on/off, uniform and
 * half-black/half-white cellStatsFor, and labelModeFromKey parsing.
 *
 * Pure functions only — no Android runtime, no Compose runtime
 * needed beyond value types. Written, NOT run: the sandbox Gradle
 * test executor is known-broken (see docs/UNVERIFIED.md); the
 * coordinator's build is the first run of these tests.
 */
class LegibilityM8Test {

    // ---------- textColorFor: exact switch at luminance 0.19 ----------

    @Test
    fun textColorFor_exactlyAtSwitchIsWhite() {
        // Switch is `luminance > 0.19` -> near-black; 0.19 itself ties
        // to white.
        assertEquals(LEGIBILITY_WHITE, Legibility.textColorFor(0.19f))
    }

    @Test
    fun textColorFor_justAboveSwitchIsNearBlack() {
        assertEquals(LEGIBILITY_NEAR_BLACK, Legibility.textColorFor(0.1901f))
    }

    // ---------- needsPill: the low-contrast mid-luminance trap ----------

    @Test
    fun needsPill_trueInMidZoneWhereNeitherTextPasses() {
        // Luminance 0.19: white contrast 1.05/0.24 = 4.375, near-black
        // contrast 0.24/0.0556 = 4.317 — both below 4.5:1, so a pill is
        // required even though the cell is perfectly calm (variance 0).
        assertTrue(Legibility.needsPill(0.19f, 0f))
    }

    @Test
    fun needsPill_falseJustBelowMidZone() {
        // Luminance 0.18: white contrast 1.05/0.23 = 4.565, above 4.5.
        assertFalse(Legibility.needsPill(0.18f, 0f))
    }

    @Test
    fun needsPill_falseJustAboveMidZone() {
        // Luminance 0.21: near-black contrast 0.26/0.0556 = 4.676.
        assertFalse(Legibility.needsPill(0.21f, 0f))
    }

    @Test
    fun needsPill_trueAtExactVarianceThreshold() {
        // Variance threshold is `>= 0.04`: a cell at exactly 0.04 is
        // busy even with a perfect-contrast mean (black, 21:1 white).
        assertTrue(Legibility.needsPill(0f, LEGIBILITY_HIGH_VARIANCE))
    }

    @Test
    fun needsPill_falseJustBelowVarianceThreshold() {
        assertFalse(Legibility.needsPill(0f, 0.0399f))
    }

    @Test
    fun needsPill_busyCellNeedsPillEvenWithPassingMean() {
        // White mean: near-black text passes contrast, but high
        // variance alone forces the pill.
        assertTrue(Legibility.needsPill(1f, 0.25f))
    }

    @Test
    fun needsPill_lowContrastCellNeedsPillEvenWhenCalm() {
        // Calm mid-zone cell (variance 0): pill is about contrast,
        // not busyness.
        assertTrue(Legibility.needsPill(0.19f, 0f))
    }

    // ---------- cellStatsFor: exact mean / variance math ----------

    @Test
    fun cellStatsFor_twoExtremesMeanHalfVarianceQuarter() {
        val stats = cellStatsFor(floatArrayOf(0f, 1f))
        assertEquals(0.5f, stats.luminance, 0.001f)
        assertEquals(0.25f, stats.variance, 0.001f)
    }

    @Test
    fun cellStatsFor_twoDarkOneLight() {
        // Mean 1/3; squared deviations 1/9 + 1/9 + 4/9 = 6/9, /3 = 2/9.
        val stats = cellStatsFor(floatArrayOf(0f, 0f, 1f))
        assertEquals(1f / 3f, stats.luminance, 0.001f)
        assertEquals(2f / 9f, stats.variance, 0.001f)
    }

    @Test
    fun cellStatsFor_clampsOutOfRangeMeans() {
        // Samples above 1 clamp the mean to 1 (variance stays 0).
        val stats = cellStatsFor(floatArrayOf(2f, 2f))
        assertEquals(1f, stats.luminance, 0.001f)
        assertEquals(0f, stats.variance, 0.001f)
    }

    @Test
    fun cellStatsFor_singleSampleHasZeroVariance() {
        val stats = cellStatsFor(floatArrayOf(0.7f))
        assertEquals(0.7f, stats.luminance, 0.001f)
        assertEquals(0f, stats.variance, 0.001f)
    }

    // ---------- labelStyleFor: all three modes ----------

    @Test
    fun labelStyleFor_autoBusyCellIsWhiteWithPill() {
        val style = labelStyleFor(LabelMode.Auto, 0f, 0.05f)
        assertEquals(LEGIBILITY_WHITE, style.textColor)
        assertTrue(style.needsPill)
    }

    @Test
    fun labelStyleFor_autoCalmLightCellHasNoPill() {
        val style = labelStyleFor(LabelMode.Auto, 0.9f, 0f)
        assertEquals(LEGIBILITY_NEAR_BLACK, style.textColor)
        assertFalse(style.needsPill)
    }

    @Test
    fun labelStyleFor_autoMidZoneCellGetsPill() {
        val style = labelStyleFor(LabelMode.Auto, 0.19f, 0f)
        assertEquals(LEGIBILITY_WHITE, style.textColor)
        assertTrue(style.needsPill)
    }

    @Test
    fun labelStyleFor_pillAlwaysPillsEvenOnCalmCell() {
        val style = labelStyleFor(LabelMode.Pill, 0.9f, 0f)
        assertEquals(LEGIBILITY_NEAR_BLACK, style.textColor)
        assertTrue(style.needsPill)
    }

    @Test
    fun labelStyleFor_offHidesLabelEvenOnBusyCell() {
        val style = labelStyleFor(LabelMode.Off, 0f, 0.25f)
        assertNull(style.textColor)
        assertFalse(style.needsPill)
    }

    // ---------- WallpaperLegibilityCache: key semantics ----------

    @Test
    fun cache_computesOncePerKey() {
        val cache = WallpaperLegibilityCache()
        var calls = 0
        val first = cache.getOrPut("wp-a|4x6") {
            calls++
            listOf(CellStats(0.2f, 0f))
        }
        val second = cache.getOrPut("wp-a|4x6") {
            calls++
            listOf(CellStats(0.9f, 0f))
        }
        assertEquals(1, calls)
        assertEquals(first, second)
    }

    @Test
    fun cache_differentGridSizeIsDifferentKey() {
        // Same wallpaper, different grid: the M8 helper builds the key
        // as wallpaperKey + grid size, so these must not share a sample.
        val cache = WallpaperLegibilityCache()
        var calls = 0
        cache.getOrPut("wp-a|4x6") { calls++; listOf(CellStats(0.2f, 0f)) }
        cache.getOrPut("wp-a|5x6") { calls++; listOf(CellStats(0.2f, 0f)) }
        assertEquals(2, calls)
    }

    @Test
    fun cache_invalidateKeyRemovesOnlyThatKey() {
        val cache = WallpaperLegibilityCache()
        cache.getOrPut("wp-a|4x6") { listOf(CellStats(0.2f, 0f)) }
        cache.getOrPut("wp-b|4x6") { listOf(CellStats(0.8f, 0f)) }
        cache.invalidateKey("wp-a|4x6")
        var recomputed = false
        cache.getOrPut("wp-b|4x6") { recomputed = true; listOf(CellStats(0.8f, 0f)) }
        assertFalse(recomputed)
        cache.getOrPut("wp-a|4x6") { recomputed = true; listOf(CellStats(0.2f, 0f)) }
        assertTrue(recomputed)
    }

    @Test
    fun cache_invalidateClearsAll() {
        val cache = WallpaperLegibilityCache()
        cache.getOrPut("wp-a|4x6") { listOf(CellStats(0.2f, 0f)) }
        cache.invalidate()
        var recomputed = false
        cache.getOrPut("wp-a|4x6") { recomputed = true; listOf(CellStats(0.2f, 0f)) }
        assertTrue(recomputed)
    }
}
