package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 3.1 Phase A12: per-cell aggregation over a cached luminance map.
 * Pure — no Bitmap, no dispatcher. Written, not run (sandbox test
 * executor broken, pre-existing).
 */
class LegibilityA12Test {

    @Test
    fun aggregate_emptyMap_midGreyCells() {
        val cells = aggregateCells(LuminanceMap(0, 0, FloatArray(0)), 3, 2)
        assertEquals(6, cells.size)
        for (cell in cells) {
            assertEquals(0.5f, cell.luminance, 0.001f)
            assertEquals(0f, cell.variance, 0.001f)
        }
    }

    @Test
    fun aggregate_wholeMap_oneCell() {
        // 2x2 map, half dark half light: mean 0.5, variance 0.25.
        val map = LuminanceMap(2, 2, floatArrayOf(0f, 0f, 1f, 1f))
        val cells = aggregateCells(map, 1, 1)
        assertEquals(1, cells.size)
        assertEquals(0.5f, cells[0].luminance, 0.001f)
        assertEquals(0.25f, cells[0].variance, 0.001f)
    }

    @Test
    fun aggregate_columns_splitVertically() {
        // Left column dark, right column light.
        val map = LuminanceMap(2, 2, floatArrayOf(0f, 1f, 0f, 1f))
        val cells = aggregateCells(map, 2, 1)
        assertEquals(2, cells.size)
        assertEquals(0f, cells[0].luminance, 0.001f)
        assertEquals(0f, cells[0].variance, 0.001f)
        assertEquals(1f, cells[1].luminance, 0.001f)
        assertEquals(0f, cells[1].variance, 0.001f)
    }

    @Test
    fun aggregate_sameMap_regirdsWithoutResampling() {
        // The A12 point: one map serves every grid — the aggregation
        // alone changes when the grid changes.
        val map = LuminanceMap(2, 2, floatArrayOf(0f, 1f, 0f, 1f))
        assertEquals(1, aggregateCells(map, 1, 1).size)
        assertEquals(4, aggregateCells(map, 2, 2).size)
        assertEquals(2, aggregateCells(map, 1, 2).size)
    }
}
