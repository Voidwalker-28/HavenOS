package app.havenos.launcher.ui

import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.gridCells
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.parseMembers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * All coordinates are in HALF-CELL units (DB v2): an app icon is 2x2 units,
 * the seeded clock is full-width x 4 units. The standard test grid is
 * 8 x 12 units (4 x 6 cells).
 */
class GridTest {

    private fun app(key: String, col: Int, row: Int, page: Int = 0) =
        HomeItem("app:$key", ItemKind.APP, page, col, row, 2, 2, key)

    private fun clock(colsU: Int) =
        HomeItem("clock", ItemKind.CLOCK, 0, 0, 0, colsU, 4, "serif")

    // ---------- seedDense ----------

    @Test
    fun seedDense_clockFirstSpanningAllCols() {
        val apps = (1..5).map { "key$it" to "com.example.app$it" }
        val (items, _) = seedDense(apps, cols = 8, rows = 12)

        val clock = items.first()
        assertEquals("clock", clock.id)
        assertEquals(ItemKind.CLOCK, clock.kind)
        assertEquals(0, clock.page)
        assertEquals(0, clock.col)
        assertEquals(0, clock.row)
        assertEquals(8, clock.colSpan)
        assertEquals(4, clock.rowSpan)

        assertEquals(5, items.count { it.kind == ItemKind.APP })
        assertTrue(items.filter { it.kind == ItemKind.APP }.all {
            it.id == "app:${it.payload}" && it.colSpan == 2 && it.rowSpan == 2
        })
    }

    @Test
    fun seedDense_noItemsOverlapOnAnyPage() {
        val apps = (1..40).map { "key$it" to "com.example.app$it" }
        val (items, _) = seedDense(apps, cols = 8, rows = 12)

        for (a in items) {
            for (b in items) {
                if (a.id != b.id && a.page == b.page) {
                    assertFalse(
                        "${a.id} overlaps ${b.id} on page ${a.page}",
                        rectOf(a).overlaps(rectOf(b)),
                    )
                }
            }
        }
        // Page 0: 8x12 units minus the 8x4 clock leaves room for 16 apps of 2x2.
        assertEquals(16, items.count { it.page == 0 && it.kind == ItemKind.APP })
        assertTrue(items.any { it.page > 0 })
    }

    @Test
    fun seedDense_dockPrefersDialerWhenPresent() {
        val apps = listOf(
            "calculator" to "com.android.calculator2",
            "dialer" to "com.android.dialer",
            "messages" to "com.google.android.apps.messaging",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
        )
        val (_, dock) = seedDense(apps, cols = 8, rows = 12)
        assertEquals("dialer", dock.first())
    }

    @Test
    fun seedDense_dockSizeFourAndFillsInInputOrder() {
        val apps = (1..6).map { "key$it" to "com.example.app$it" }
        val (_, dock) = seedDense(apps, cols = 8, rows = 12)
        assertEquals(4, dock.size)
        assertEquals(apps.take(4).map { it.first }, dock)
    }

    // ---------- resolveDrop ----------

    @Test
    fun resolveDrop_moveToEmptyCell() {
        val items = listOf(clock(8), app("a", 0, 4), app("b", 2, 4))
        val result = resolveDrop(items, emptyMap(), "app:a", 6, 10, 8, 12, "f1")

        assertNull(result.createdFolder)
        assertNull(result.updatedFolder)
        val moved = result.items.first { it.id == "app:a" }
        assertEquals(6, moved.col)
        assertEquals(10, moved.row)
        assertEquals(items.size, result.items.size)
    }

    @Test
    fun resolveDrop_appOntoClockSwaps() {
        // M2 rule: a widget occupant (incl. the clock) swaps like anything
        // else; only CLOCK_PAGE refuses drops.
        val items = listOf(clock(8), app("a", 0, 4))
        val result = resolveDrop(items, emptyMap(), "app:a", 2, 0, 8, 12, "f1")

        assertNull(result.createdFolder)
        val movedApp = result.items.first { it.id == "app:a" }
        assertEquals(0, movedApp.col)
        assertEquals(0, movedApp.row)
        val movedClock = result.items.first { it.id == "clock" }
        assertEquals(0, movedClock.col)
        assertEquals(4, movedClock.row)
    }

    @Test
    fun resolveDrop_appOntoAppCreatesFolder() {
        val items = listOf(clock(8), app("a", 0, 4), app("b", 4, 6))
        val result = resolveDrop(items, emptyMap(), "app:b", 0, 4, 8, 12, "f1")

        val folder = result.createdFolder
        assertNotNull(folder)
        assertEquals("f1", folder!!.id)
        assertEquals(listOf("a", "b"), parseMembers(folder.membersJson))

        val folderItem = result.items.firstOrNull { it.id == "folder:f1" }
        assertNotNull(folderItem)
        assertEquals(ItemKind.FOLDER, folderItem!!.kind)
        assertEquals(0, folderItem.col)
        assertEquals(4, folderItem.row)
        assertEquals(0, folderItem.page)
        assertFalse(result.items.any { it.id == "app:a" || it.id == "app:b" })
    }

    @Test
    fun resolveDrop_appOntoFolderAppendsMember() {
        val folder = Folder("f1", "Games", encodeMembers(listOf("x")))
        val folderItem = HomeItem("folder:f1", ItemKind.FOLDER, 0, 2, 4, 2, 2, "f1")
        val items = listOf(clock(8), app("a", 0, 4), folderItem)
        val result = resolveDrop(items, mapOf("f1" to folder), "app:a", 2, 4, 8, 12, "f2")

        assertNull(result.createdFolder)
        assertNotNull(result.updatedFolder)
        assertEquals(listOf("x", "a"), parseMembers(result.updatedFolder!!.membersJson))
        assertFalse(result.items.any { it.id == "app:a" })
        assertTrue(result.items.any { it.id == "folder:f1" })
    }

    @Test
    fun resolveDrop_folderOntoAppSwapsPositions() {
        val folder = Folder("f1", "Games", encodeMembers(listOf("x")))
        val folderItem = HomeItem("folder:f1", ItemKind.FOLDER, 0, 2, 4, 2, 2, "f1")
        val items = listOf(clock(8), app("a", 0, 4), folderItem)
        val result = resolveDrop(items, mapOf("f1" to folder), "folder:f1", 0, 4, 8, 12, "f2")

        assertNull(result.createdFolder)
        assertNull(result.updatedFolder)
        val movedFolder = result.items.first { it.id == "folder:f1" }
        assertEquals(0, movedFolder.col)
        assertEquals(4, movedFolder.row)
        val movedApp = result.items.first { it.id == "app:a" }
        assertEquals(2, movedApp.col)
        assertEquals(4, movedApp.row)
    }

    @Test
    fun resolveDrop_outOfBoundsTargetIsUnchanged() {
        val items = listOf(clock(8), app("a", 0, 4))
        assertEquals(
            items,
            resolveDrop(items, emptyMap(), "app:a", 8, 4, 8, 12, "f1").items,
        )
        assertEquals(
            items,
            resolveDrop(items, emptyMap(), "app:a", 0, -1, 8, 12, "f1").items,
        )
    }

    @Test
    fun resolveDrop_ontoOwnCellIsUnchanged() {
        val items = listOf(clock(8), app("a", 4, 6))
        val result = resolveDrop(items, emptyMap(), "app:a", 4, 6, 8, 12, "f1")
        assertEquals(items, result.items)
    }

    @Test
    fun resolveDrop_blockedMoveFallsBackToAdjacentFreeSpot() {
        // Target (3,6) is inside no item, but the 2x2 rect there overlaps
        // app b at (4,6), so the plain move is blocked; nearest free spot
        // to (3,6) for the dragged item is (2,6).
        val items = listOf(app("a", 0, 0), app("b", 4, 6))
        val result = resolveDrop(items, emptyMap(), "app:a", 3, 6, 8, 12, "f1")

        assertNull(result.createdFolder)
        val moved = result.items.first { it.id == "app:a" }
        assertEquals(2, moved.col)
        assertEquals(6, moved.row)
    }

    @Test
    fun resolveDrop_appOntoAppStillCreatesFolderNotFallback() {
        val items = listOf(clock(8), app("a", 0, 4), app("b", 4, 6))
        val result = resolveDrop(items, emptyMap(), "app:b", 0, 4, 8, 12, "f1")

        assertNotNull(result.createdFolder)
        assertEquals("f1", result.createdFolder!!.id)
        assertTrue(result.items.any { it.id == "folder:f1" })
        assertFalse(result.items.any { it.id == "app:b" && it.col == 0 && it.row == 4 })
    }

    // ---------- findSpot / density ----------

    @Test
    fun findSpotNear_picksNearestFreeSpot() {
        val items = listOf(app("a", 0, 0))
        // (1,0) overlaps the app; the unique nearest free 2x2 spot is (2,0).
        assertEquals(2 to 0, findSpotNear(items, 0, 2, 2, 8, 12, 1, 0))
    }

    @Test
    fun findSpotNear_returnsNullOnFullPage() {
        val big = HomeItem("big", ItemKind.CLOCK, 0, 0, 0, 8, 12, "serif")
        assertNull(findSpotNear(listOf(big), 0, 2, 2, 8, 12, 0, 0))
    }


    @Test
    fun findSpot_skipsOccupiedArea() {
        val items = listOf(clock(8), app("a", 0, 4))
        // First 2x2 hole is right of the app on the clock's first free row.
        assertEquals(2 to 4, findSpot(items, 0, 2, 2, 8, 12))
        // A full-width widget only fits below everything placed so far.
        assertEquals(0 to 6, findSpot(items, 0, 8, 2, 8, 12))
        // Nothing fits a grid that's too small.
        assertNull(findSpot(items, 0, 10, 2, 8, 12))
    }

    @Test
    fun gridCells_defaultDensityMatchesM1Defaults() {
        val state = SettingsState()
        assertEquals(6 to 5, state.gridCells(landscape = true))
        assertEquals(52, state.iconDp())
    }
}
