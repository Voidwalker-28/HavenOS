package app.havenos.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 Wave 1: legibility engine (Legibility.kt), size classes, grid
 * columns, label modes (HomeTokens.kt). Pure functions only — no
 * Android runtime, no Compose runtime needed beyond value types.
 */
class HomeTokensTest {

    // ---------- contrastRatio ----------

    @Test
    fun contrastRatio_whiteOnBlackIs21() {
        assertEquals(21.0, Legibility.contrastRatio(1.0f, 0.0f), 0.001)
    }

    @Test
    fun contrastRatio_isSymmetric() {
        assertEquals(
            Legibility.contrastRatio(0.2f, 0.8f),
            Legibility.contrastRatio(0.8f, 0.2f),
            0.001,
        )
    }

    @Test
    fun contrastRatio_sameLuminanceIs1() {
        assertEquals(1.0, Legibility.contrastRatio(0.5f, 0.5f), 0.001)
    }

    @Test
    fun contrastRatio_clampsOutOfRangeInputs() {
        assertEquals(21.0, Legibility.contrastRatio(-1.0f, 2.0f), 0.001)
    }

    // ---------- textColorFor: white vs near-black ----------

    @Test
    fun textColorFor_darkCellIsWhite() {
        assertEquals(Color.White, Legibility.textColorFor(0.0f))
    }

    @Test
    fun textColorFor_lightCellIsNearBlack() {
        assertEquals(Color(0xFF111111), Legibility.textColorFor(1.0f))
    }

    @Test
    fun textColorFor_switchesAroundMidLuminance() {
        // Below the switch point white wins; above it near-black wins.
        assertEquals(Color.White, Legibility.textColorFor(0.10f))
        assertEquals(Color(0xFF111111), Legibility.textColorFor(0.50f))
    }

    // ---------- needsPill: 4.5:1 threshold + busy cells ----------

    @Test
    fun needsPill_falseOnPlainDarkCell() {
        // White on black is 21:1 — no pill.
        assertFalse(Legibility.needsPill(0.0f, 0.0f))
    }

    @Test
    fun needsPill_falseOnPlainLightCell() {
        // Near-black on white is ~18.9:1 — no pill.
        assertFalse(Legibility.needsPill(1.0f, 0.0f))
    }

    @Test
    fun needsPill_trueWhenBestContrastBelow4Point5() {
        // Mid-grey (~0.19): best available contrast is ~4.4:1, under 4.5.
        assertTrue(Legibility.needsPill(0.19f, 0.0f))
    }

    @Test
    fun needsPill_falseJustAboveThreshold() {
        // At ~0.21 the near-black contrast clears 4.5:1.
        assertFalse(Legibility.needsPill(0.21f, 0.0f))
    }

    @Test
    fun needsPill_trueOnBusyCellEvenWithPassingMean() {
        // A light cell whose mean passes, but high variance = busy artwork.
        assertTrue(Legibility.needsPill(1.0f, 0.10f))
        assertTrue(Legibility.needsPill(0.0f, 0.10f))
    }

    @Test
    fun needsPill_falseOnCalmCellBelowVarianceThreshold() {
        assertFalse(Legibility.needsPill(1.0f, 0.01f))
    }

    // ---------- cellStatsFor ----------

    @Test
    fun cellStatsFor_uniformSamples() {
        val stats = cellStatsFor(floatArrayOf(0.5f, 0.5f, 0.5f))
        assertEquals(0.5f, stats.luminance, 0.001f)
        assertEquals(0.0f, stats.variance, 0.001f)
    }

    @Test
    fun cellStatsFor_halfBlackHalfWhite() {
        val stats = cellStatsFor(floatArrayOf(0.0f, 1.0f))
        assertEquals(0.5f, stats.luminance, 0.001f)
        assertEquals(0.25f, stats.variance, 0.001f)
    }

    @Test
    fun cellStatsFor_emptyIsSafeDefault() {
        val stats = cellStatsFor(floatArrayOf())
        assertEquals(0.0f, stats.luminance, 0.001f)
        assertEquals(0.0f, stats.variance, 0.001f)
    }

    // ---------- label modes ----------

    @Test
    fun labelModeFromKey_parsesAllValuesAndDefaultsToAuto() {
        assertEquals(LabelMode.Auto, labelModeFromKey("auto"))
        assertEquals(LabelMode.Pill, labelModeFromKey("pill"))
        assertEquals(LabelMode.Off, labelModeFromKey("off"))
        assertEquals(LabelMode.Auto, labelModeFromKey(null))
        assertEquals(LabelMode.Auto, labelModeFromKey(""))
        assertEquals(LabelMode.Auto, labelModeFromKey("bogus"))
    }

    @Test
    fun labelStyleFor_offHidesLabel() {
        val style = labelStyleFor(LabelMode.Off, 0.0f, 0.0f)
        assertNull(style.textColor)
        assertFalse(style.needsPill)
    }

    @Test
    fun labelStyleFor_pillAlwaysPills() {
        val style = labelStyleFor(LabelMode.Pill, 1.0f, 0.0f)
        assertTrue(style.needsPill)
        assertEquals(Color(0xFF111111), style.textColor)
    }

    @Test
    fun labelStyleFor_autoFollowsEngine() {
        val dark = labelStyleFor(LabelMode.Auto, 0.0f, 0.0f)
        assertEquals(Color.White, dark.textColor)
        assertFalse(dark.needsPill)
        val busy = labelStyleFor(LabelMode.Auto, 1.0f, 0.10f)
        assertTrue(busy.needsPill)
    }

    // ---------- size classes: boundaries ----------

    @Test
    fun sizeClass_boundaries() {
        assertEquals(HomeSizeClass.Compact, sizeClassFor(599))
        assertEquals(HomeSizeClass.Medium, sizeClassFor(600))
        assertEquals(HomeSizeClass.Medium, sizeClassFor(839))
        assertEquals(HomeSizeClass.Expanded, sizeClassFor(840))
        assertEquals(HomeSizeClass.Compact, sizeClassFor(0))
        assertEquals(HomeSizeClass.Expanded, sizeClassFor(1200))
    }

    // ---------- type scale by size class ----------

    @Test
    fun homeTypeFor_labelSizeBySizeClass() {
        assertEquals(12.sp, homeTypeFor(HomeSizeClass.Compact).label.fontSize)
        assertEquals(12.sp, homeTypeFor(HomeSizeClass.Medium).label.fontSize)
        assertEquals(13.sp, homeTypeFor(HomeSizeClass.Expanded).label.fontSize)
    }

    @Test
    fun homeTypeFor_fixedSizes() {
        val type = homeTypeFor(HomeSizeClass.Expanded)
        assertEquals(16.sp, type.glance.fontSize)
        assertEquals(20.sp, type.todayTitle.fontSize)
        assertEquals(12.sp, type.sectionHeader.fontSize)
        assertEquals(17.sp, type.searchField.fontSize)
        assertEquals(34.sp, type.calmList.fontSize)
        assertEquals(28.sp, type.calmListCompact.fontSize)
    }

    // ---------- grid columns ----------

    @Test
    fun gridColumns_floorsWidthOverCell() {
        // 840dp / 112dp cell = 7.5 -> 7 columns.
        assertEquals(7, gridColumns(840f, 112f))
        // Exact fit: 672 / 112 = 6.
        assertEquals(6, gridColumns(672f, 112f))
    }

    @Test
    fun gridColumns_clampsToMinAndMax() {
        // Too narrow for even one cell -> min (1).
        assertEquals(1, gridColumns(50f, 112f))
        // Very wide -> default max (12).
        assertEquals(12, gridColumns(2000f, 100f))
        // Caller-supplied clamps win.
        assertEquals(4, gridColumns(500f, 100f, minColumns = 1, maxColumns = 4))
        assertEquals(6, gridColumns(500f, 100f, minColumns = 6, maxColumns = 12))
    }

    @Test
    fun gridColumns_rejectsDegenerateInputs() {
        assertEquals(1, gridColumns(0f, 112f))
        assertEquals(1, gridColumns(840f, 0f))
    }

    @Test
    fun gridCellWidthDp_isIconPlus56() {
        assertEquals(128f, gridCellWidthDp(72f), 0.001f)
    }
}
