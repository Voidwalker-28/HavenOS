package app.havenos.launcher.ui

import app.havenos.launcher.data.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M9 Worker B — Today page shell model, pager mapping, glance
 * legibility, grid fit per size class, and the label contrast routine
 * on three synthetic wallpapers. Pure functions only ([localityOnly],
 * [todayLeftColumnWidthDp], [todayPagerPageCount], [pagerIndexForGrid],
 * [gridPageForTodayPager], [glanceStyleFor], [pagedGridCells],
 * [cellStatsFor], [labelStyleFor], [Legibility]): no Compose runtime,
 * no Android framework.
 *
 * Written, NOT run: the sandbox Gradle test executor is known-broken
 * (see docs/UNVERIFIED.md); the coordinator's build is the first run
 * of these tests.
 */
class TodayM9Test {

    // ---------- localityOnly (weather city, no state name) ----------

    @Test
    fun locality_fullStateNameTrimsToCity() {
        assertEquals("Delhi", localityOnly("Delhi, National Capital Territory of Delhi"))
    }

    @Test
    fun locality_noCommaUnchanged() {
        assertEquals("Mumbai", localityOnly("Mumbai"))
    }

    @Test
    fun locality_blankBeforeCommaFallsBack() {
        assertEquals(", nowhere", localityOnly(", nowhere"))
    }

    // ---------- Today left column: 38% clamped 320-460dp ----------

    @Test
    fun leftColumn_narrowClampsToFloor() {
        // 500 * 0.38 = 190 -> 320 floor.
        assertEquals(320f, todayLeftColumnWidthDp(500f), 0f)
    }

    @Test
    fun leftColumn_midIsThirtyEightPercent() {
        assertEquals(380f, todayLeftColumnWidthDp(1000f), 0f)
    }

    @Test
    fun leftColumn_wideClampsToCap() {
        // 1500 * 0.38 = 570 -> 460 cap.
        assertEquals(460f, todayLeftColumnWidthDp(1500f), 0f)
    }

    // ---------- pager model: Today is page 0 when on ----------

    @Test
    fun pagerCount_todayAddsOnePage() {
        assertEquals(4, todayPagerPageCount(3, true))
        assertEquals(3, todayPagerPageCount(3, false))
        assertEquals(1, todayPagerPageCount(0, true))
    }

    @Test
    fun pagerIndex_hiddenPageMapsToNearestVisibleAtOrBefore() {
        val visible = listOf(0, 1, 3)
        assertEquals(1, pagerIndexForGrid(0, visible, true)) // Today page 0 + grid 0
        assertEquals(2, pagerIndexForGrid(1, visible, true))
        assertEquals(3, pagerIndexForGrid(3, visible, true))
        // Hidden grid page 2 -> nearest visible at-or-before (1) + Today offset.
        assertEquals(2, pagerIndexForGrid(2, visible, true))
        // Same mapping without Today: no offset.
        assertEquals(0, pagerIndexForGrid(0, visible, false))
        assertEquals(2, pagerIndexForGrid(3, visible, false))
        assertEquals(1, pagerIndexForGrid(2, visible, false))
    }

    @Test
    fun gridPageForPager_todayPageIsNull() {
        val visible = listOf(0, 1, 3)
        assertNull(gridPageForTodayPager(0, visible, true))
        assertEquals(0, gridPageForTodayPager(1, visible, true))
        assertEquals(1, gridPageForTodayPager(2, visible, true))
        assertEquals(3, gridPageForTodayPager(3, visible, true))
        assertNull(gridPageForTodayPager(99, visible, true))
        // Today off: pager pages are grid pages directly.
        assertEquals(0, gridPageForTodayPager(0, visible, false))
        assertEquals(1, gridPageForTodayPager(1, visible, false))
    }

    // ---------- glanceStyleFor: whole-wallpaper stats ----------

    @Test
    fun glance_nullStatsKeepLegacyLook() {
        val style = glanceStyleFor(null)
        assertEquals(LEGIBILITY_WHITE, style.textColor)
        assertFalse(style.needsPill)
        val empty = glanceStyleFor(emptyList())
        assertEquals(LEGIBILITY_WHITE, empty.textColor)
        assertFalse(empty.needsPill)
    }

    @Test
    fun glance_darkCalmIsWhiteNoPill() {
        val style = glanceStyleFor(listOf(CellStats(0.05f, 0f)))
        assertEquals(LEGIBILITY_WHITE, style.textColor)
        assertFalse(style.needsPill)
    }

    @Test
    fun glance_lightCalmIsNearBlackNoPill() {
        val style = glanceStyleFor(listOf(CellStats(0.8f, 0f)))
        assertEquals(LEGIBILITY_NEAR_BLACK, style.textColor)
        assertFalse(style.needsPill)
    }

    @Test
    fun glance_busyWallpaperNeedsPill() {
        // Mean 0.5 but one cell at max variance: the line cannot sit
        // on the wallpaper raw anywhere.
        val style = glanceStyleFor(listOf(CellStats(0f, 0.25f), CellStats(1f, 0f)))
        assertTrue(style.needsPill)
    }

    @Test
    fun glance_midZoneNeedsPill() {
        // Luminance 0.19: neither white (4.375:1) nor near-black
        // (4.317:1) reaches 4.5:1, so the pill is required.
        val style = glanceStyleFor(listOf(CellStats(0.19f, 0f)))
        assertTrue(style.needsPill)
    }

    // ---------- grid fit per size class (never clipped) ----------

    @Test
    fun gridFit_expandedFitsContentArea() {
        // iconDp 52 -> cell 108dp. 1000x700 fits at most 9 cols, 6 rows.
        val (cols, rows) = pagedGridCells(SettingsState(), false, HomeSizeClass.Expanded, 1000f, 700f, 52)
        assertTrue(cols in 3..9)
        assertTrue(rows in 3..6)
        assertTrue(cols * 108 <= 1000)
        assertTrue(rows * 108 <= 700)
    }

    @Test
    fun gridFit_compactFixedFiveBySix() {
        val (cols, rows) = pagedGridCells(SettingsState(), false, HomeSizeClass.Compact, 390f, 700f, 52)
        assertEquals(5, cols)
        assertEquals(6, rows)
    }

    @Test
    fun gridFit_mediumFixedSixBySix() {
        val (cols, rows) = pagedGridCells(SettingsState(), false, HomeSizeClass.Medium, 700f, 1000f, 52)
        assertEquals(6, cols)
        assertEquals(6, rows)
    }

    // ---------- label contrast routine on three wallpapers ----------

    @Test
    fun contrast_darkBusyWallpaperNeedsPill() {
        // Busy dark photo: alternating black/white samples.
        val stats = cellStatsFor(floatArrayOf(0f, 0f, 1f, 0f))
        val style = labelStyleFor(LabelMode.Auto, stats.luminance, stats.variance)
        assertTrue(stats.variance >= LEGIBILITY_HIGH_VARIANCE)
        assertTrue(style.needsPill)
        // Mean 0.25 is above the 0.19 switch, so text is near-black
        // (readable inside the pill, not raw on the photo).
        assertEquals(LEGIBILITY_NEAR_BLACK, style.textColor)
    }

    @Test
    fun contrast_lightCalmWallpaperNoPill() {
        val stats = cellStatsFor(floatArrayOf(0.9f, 0.9f, 0.9f))
        val style = labelStyleFor(LabelMode.Auto, stats.luminance, stats.variance)
        assertEquals(0f, stats.variance, 0f)
        assertFalse(style.needsPill)
        assertEquals(LEGIBILITY_NEAR_BLACK, style.textColor)
        // Near-black on a 0.9-luminance cell passes 4.5:1 comfortably.
        assertTrue(Legibility.contrastRatio(0.0056f, 0.9f) >= 4.5)
    }

    @Test
    fun contrast_midGreyWallpaperNeedsPill() {
        // Uniform mid-grey: the low-contrast trap — neither text
        // colour passes without a pill.
        val stats = cellStatsFor(floatArrayOf(0.19f, 0.19f))
        val style = labelStyleFor(LabelMode.Auto, stats.luminance, stats.variance)
        assertTrue(style.needsPill)
        assertTrue(Legibility.contrastRatio(1.0f, 0.19f) < 4.5)
        assertTrue(Legibility.contrastRatio(0.0056f, 0.19f) < 4.5)
    }

    @Test
    fun contrast_labelOffDrawsNothing() {
        val style = labelStyleFor(LabelMode.Off, 0.05f, 0.25f)
        assertNull(style.textColor)
        assertFalse(style.needsPill)
    }
}
