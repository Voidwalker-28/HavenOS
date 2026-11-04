package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Coordination fix step 3: the density-only grid (overrides ignored)
 * that Follow-density commits through GridChange. Written before the
 * production code (TDD).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationGridTest {

    @Test
    fun densityGridCells_ignoresOverrides() {
        val state = SettingsState(density = 50, colsPort = 9, rowsPort = 9, colsLand = 11, rowsLand = 10)
        // Portrait density 50: cols 3 + (150+50)/100 = 5, rows 5 + (200+50)/100 = 7.
        assertEquals(5 to 7, state.densityGridCells(false))
        // Landscape density 50: cols 4 + (300+50)/100 = 7, rows 3 + (250+50)/100 = 6.
        assertEquals(7 to 6, state.densityGridCells(true))
        // Overrides still honored by the effective grid.
        assertEquals(9 to 9, state.gridCells(false))
        assertEquals(11 to 10, state.gridCells(true))
    }

    @Test
    fun densityGridCells_clampsToPickerRange() {
        val state = SettingsState(density = 100)
        // Portrait density 100: cols 3 + 3 = 6, rows 5 + 4 = 9 (in range).
        assertEquals(6 to 9, state.densityGridCells(false))
        val zero = SettingsState(density = 0)
        // Portrait density 0: cols 3 + 0 = 3, rows 5 + 0 = 5.
        assertEquals(3 to 5, zero.densityGridCells(false))
    }
}
