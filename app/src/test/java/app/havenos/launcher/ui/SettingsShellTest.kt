package app.havenos.launcher.ui

import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.ui.settings.shell.allHubSearchEntries
import app.havenos.launcher.ui.settings.shell.gridShrinkMessage
import app.havenos.launcher.ui.settings.shell.gridShrinkReport
import app.havenos.launcher.ui.settings.shell.hubRouteForEntry
import app.havenos.launcher.ui.settings.shell.searchSettingsEntries
import app.havenos.launcher.ui.settings.shell.sessionChanged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M6 shell tests. Pure logic only — no Compose, no DataStore — so
 * these run on the JVM. Written, NOT run: the sandbox test executor
 * is known to crash (pre-existing).
 *
 * M6 rewrite: the M5 14-category registry tests are gone with the old
 * shell (no SETTINGS_CATEGORIES, no category-level search index). The
 * grid-shrink and session-snapshot cases survive against the moved
 * shell functions; the search cases now run against the aggregated
 * six-hub index.
 */
class SettingsShellTest {

    // Coordinates are HALF-CELL units (DB v2): an app icon is 2x2 units.
    private fun app(id: String, col: Int, row: Int, page: Int = 0) =
        HomeItem("app:$id", ItemKind.APP, page, col, row, 2, 2, id)

    private fun folder(id: String, col: Int, row: Int) =
        HomeItem("folder:$id", ItemKind.FOLDER, 0, col, row, 2, 2, id)

    private fun widget(col: Int, row: Int) =
        HomeItem("w:weather", ItemKind.WEATHER, 0, col, row, 4, 4, "weather")

    // ---------- gridShrinkReport ----------

    @Test
    fun shrink_noOverflowWhenGridUnchanged() {
        val items = listOf(app("a", 0, 0), app("b", 6, 10))
        val report = gridShrinkReport(items, newColsCells = 4, newRowsCells = 6)
        assertEquals(0, report.movedCount)
        assertTrue(report.movedIds.isEmpty())
    }

    @Test
    fun shrink_columnOverflowCountsOnlyIconsBeyondNewEdge() {
        // 4-col grid (8 units) -> 3-col grid (6 units): the icon at col 6
        // ends at 8 > 6, so it moves; the icon at col 0 stays.
        val items = listOf(app("stay", 0, 0), app("move", 6, 0))
        val report = gridShrinkReport(items, newColsCells = 3, newRowsCells = 6)
        assertEquals(1, report.movedCount)
        assertEquals(listOf("app:move"), report.movedIds)
    }

    @Test
    fun shrink_rowOverflowCountsAndBoundaryIsInclusive() {
        // 6-row grid (12 units) -> 5-row grid (10 units). Icon ending
        // exactly at 10 stays (col+span == max is NOT overflow).
        val items = listOf(
            app("edge", 0, 8), // ends at row 10 == max: stays
            app("over", 0, 10), // ends at 12 > 10: moves
            folder("f", 4, 10), // folders count as icons: moves
        )
        val report = gridShrinkReport(items, newColsCells = 4, newRowsCells = 5)
        assertEquals(2, report.movedCount)
        assertTrue(report.movedIds.contains("app:over"))
        assertTrue(report.movedIds.contains("folder:f"))
    }

    @Test
    fun shrink_widgetsAndClocksAreNeverReportedAsIcons() {
        val items = listOf(widget(6, 10))
        val report = gridShrinkReport(items, newColsCells = 3, newRowsCells = 5)
        assertEquals(0, report.movedCount)
    }

    @Test
    fun shrink_messageSingularAndPlural() {
        assertEquals(
            "1 icon will move to the next page",
            gridShrinkMessage(gridShrinkReport(listOf(app("a", 6, 0)), 3, 6)),
        )
        assertEquals(
            "2 icons will move to the next page",
            gridShrinkMessage(gridShrinkReport(listOf(app("a", 6, 0), app("b", 6, 2)), 3, 6)),
        )
    }

    // ---------- session snapshot / revert ----------

    @Test
    fun session_unchangedWhenStateEqualsSnapshot() {
        val snapshot = SettingsState()
        assertFalse(sessionChanged(snapshot, SettingsState()))
    }

    @Test
    fun session_changedWhenAnyFieldDiffers() {
        val snapshot = SettingsState()
        assertTrue(sessionChanged(snapshot, snapshot.copy(density = 41)))
        assertTrue(sessionChanged(snapshot, snapshot.copy(darkMode = 2)))
        assertTrue(sessionChanged(snapshot, snapshot.copy(hiddenJson = "[\"x\"]")))
    }

    // ---------- six-hub search index ----------

    @Test
    fun search_biggerIconsRanksIconSizeFirst() {
        val hits = searchSettingsEntries("bigger icons", allHubSearchEntries)
        assertTrue(hits.isNotEmpty())
        assertEquals("Icon size", hits.first().title)
        assertEquals("home-dock", hubRouteForEntry(hits.first()))
    }

    @Test
    fun search_wallpaperHitsLandInLookHub() {
        val hits = searchSettingsEntries("wallpaper", allHubSearchEntries)
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.hub == "Look" })
    }

    @Test
    fun search_everyTokenMustMatch() {
        // "wallpaper" + "zebra": no entry matches both tokens.
        assertTrue(searchSettingsEntries("wallpaper zebra", allHubSearchEntries).isEmpty())
    }

    @Test
    fun search_blankQueryReturnsNothing() {
        assertTrue(searchSettingsEntries("   ", allHubSearchEntries).isEmpty())
    }

    @Test
    fun search_hubSlugsAreDisplayNames() {
        // The unification: no slug-style hub names survive in the index.
        assertTrue(allHubSearchEntries.none { it.hub == "device-data" || it.hub == "privacy" })
        assertTrue(allHubSearchEntries.any { it.hub == "Device & Data" })
        assertTrue(allHubSearchEntries.any { it.hub == "Privacy" })
    }
}
