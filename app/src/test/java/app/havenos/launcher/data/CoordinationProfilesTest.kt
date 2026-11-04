package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordination fix step 1: profile scope — materialize, first-use gate,
 * dirty repair, dirty JSON round-trip. Pure logic; written before the
 * production code (TDD). Coordinates in half-cell units.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationProfilesTest {

    private fun app(id: String, col: Int, row: Int, page: Int = 0, colSpan: Int = 2, rowSpan: Int = 2) =
        HomeItem(id, ItemKind.APP, page, col, row, colSpan, rowSpan, id)

    @Test
    fun materializeProfile_overlaysPlacementsAndKeepsUnplacedCoords() {
        val items = listOf(
            app("a", 0, 0),
            app("b", 2, 0),
        )
        val placements = mapOf("a" to ProfilePlacement(page = 1, col = 4, row = 6, colSpan = 2, rowSpan = 2))
        val out = materializeProfile(items, placements)
        val a = out.first { it.id == "a" }
        assertEquals(1, a.page)
        assertEquals(4, a.col)
        assertEquals(6, a.row)
        assertEquals("a", a.payload)
        val b = out.first { it.id == "b" }
        assertEquals(0, b.page)
        assertEquals(2, b.col)
        assertEquals(0, b.row)
    }

    @Test
    fun dirtyJson_roundTripPreservesDirtySet() {
        val profiles = LayoutProfiles(
            canonical = listOf(CanonicalEntry("a", 0)),
            profiles = mapOf(WindowClass.MEDIUM to mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2))),
            dirty = setOf(WindowClass.COMPACT, WindowClass.EXPANDED_LANDSCAPE),
        )
        val parsed = parseLayoutProfiles(profiles.toJson())
        assertEquals(setOf(WindowClass.COMPACT, WindowClass.EXPANDED_LANDSCAPE), parsed.dirty)
        assertEquals(profiles.canonical, parsed.canonical)
        assertEquals(profiles.profiles, parsed.profiles)
    }

    @Test
    fun dirtyJson_oldBackupWithoutDirtyParsesToEmptyDirty() {
        val old = """{"canonical":[{"id":"a","page":0}],"profiles":{"MEDIUM":{"a":{"page":0,"col":0,"row":0,"colSpan":2,"rowSpan":2}}}}"""
        assertEquals(emptySet<WindowClass>(), parseLayoutProfiles(old).dirty)
    }

    @Test
    fun firstUse_oversizeSingleKeepsSpanIntact() {
        // 4-col grid = 8 units wide; widget span 20 units is oversize.
        val items = listOf(app("wide", 0, 0, colSpan = 20, rowSpan = 2), app("a", 0, 0))
        val out = firstUseProfileItems(items, colsCells = 4, rowsCells = 6)
        val wide = out.first { it.id == "wide" }
        assertEquals(20, wide.colSpan)
        assertEquals(0, wide.col)
        assertEquals(0, wide.row)
    }

    @Test
    fun firstUse_fittingLayoutKeepsDerivedPositions() {
        val items = listOf(app("a", 0, 0), app("b", 2, 0), app("c", 0, 2))
        val out = firstUseProfileItems(items, colsCells = 4, rowsCells = 6)
        assertEquals(3, out.size)
        assertTrue(out.none { it.id == "a" && (it.col != 0 || it.row != 0) })
    }

    @Test
    fun repair_movesOverlappingItemsAndKeepsValidOnesExact() {
        // Two icons stored on the same cell; a third is valid and must not move.
        val items = listOf(app("a", 0, 0), app("b", 0, 0), app("c", 4, 0))
        val (repaired, gateOk) = repairProfileItems(items, colsCells = 4, rowsCells = 6, visiblePages = null)
        assertTrue(gateOk)
        val c = repaired.first { it.id == "c" }
        assertEquals(4, c.col)
        assertEquals(0, c.row)
        val a = repaired.first { it.id == "a" }
        val b = repaired.first { it.id == "b" }
        assertTrue(a.col != b.col || a.row != b.row || a.page != b.page)
    }

    @Test
    fun repair_validLayoutIsIdentity() {
        val items = listOf(app("a", 0, 0), app("b", 2, 0))
        val (repaired, gateOk) = repairProfileItems(items, colsCells = 4, rowsCells = 6, visiblePages = null)
        assertTrue(gateOk)
        assertEquals(items, repaired)
    }
}
