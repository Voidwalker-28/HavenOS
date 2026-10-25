package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Worker D (M9) tests: layout profiles, Doc 1 §5.10. All coordinates in half-cell units. */
class LayoutProfilesTest {

    private fun app(id: String, col: Int, row: Int, page: Int = 0) =
        HomeItem(id, ItemKind.APP, page, col, row, 2, 2, id)

    private val grid = { _: WindowClass -> 4 to 6 }

    @Test
    fun windowClassFor_breakpoints() {
        assertEquals(WindowClass.EXPANDED_LANDSCAPE, windowClassFor(900, true))
        assertEquals(WindowClass.MEDIUM, windowClassFor(900, false))
        assertEquals(WindowClass.MEDIUM, windowClassFor(600, false))
        assertEquals(WindowClass.COMPACT, windowClassFor(599, true))
    }

    @Test
    fun deriveProfile_reflowsInCanonicalOrderWithoutOverlap() {
        val clock = HomeItem("clock", ItemKind.CLOCK, 0, 0, 0, 8, 4, "serif")
        val items = listOf(clock, app("a", 0, 4), app("b", 2, 4), app("c", 4, 4))
        val canonical = canonicalOrderOf(items)
        val derived = deriveProfile(canonical, spansOf(items), 4, 6)
        assertEquals(ProfilePlacement(0, 0, 0, 8, 4), derived["clock"])
        // Apps reflow row-major under the full-width clock, no overlaps.
        val placed = derived.values.toList()
        for (i in placed.indices) {
            for (j in placed.indices) {
                if (i == j) continue
                val a = placed[i]
                val b = placed[j]
                if (a.page == b.page) {
                    val overlaps = a.col < b.col + b.colSpan && b.col < a.col + a.colSpan &&
                        a.row < b.row + b.rowSpan && b.row < a.row + a.rowSpan
                    assertFalse(overlaps)
                }
            }
        }
        assertEquals(4, derived.size)
    }

    @Test
    fun deriveProfile_emptyCanonicalYieldsEmptyProfile() {
        assertTrue(deriveProfile(emptyList(), emptyMap(), 4, 6).isEmpty())
    }

    @Test
    fun withAppAdded_updatesEveryExistingProfile() {
        val base = LayoutProfiles(
            canonical = listOf(CanonicalEntry("a", 0)),
            profiles = mapOf(
                WindowClass.COMPACT to mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2)),
                WindowClass.MEDIUM to mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2)),
            ),
        )
        val updated = base.withAppAdded("b", 2, 2, null, grid)
        assertTrue("b" in updated.canonical.map { it.id })
        assertTrue("b" in updated.profiles.getValue(WindowClass.COMPACT))
        assertTrue("b" in updated.profiles.getValue(WindowClass.MEDIUM))
        // The new item never overlaps the existing one in either profile.
        for ((_, map) in updated.profiles) {
            val a = map.getValue("a")
            val b = map.getValue("b")
            val overlaps = b.col < a.col + a.colSpan && a.col < b.col + b.colSpan &&
                b.row < a.row + a.rowSpan && a.row < b.row + b.rowSpan
            assertFalse(overlaps)
        }
    }

    @Test
    fun withAppAdded_absentProfileStaysAbsentUntilFirstUse() {
        val base = LayoutProfiles(
            canonical = listOf(CanonicalEntry("a", 0)),
            profiles = mapOf(WindowClass.COMPACT to mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2))),
        )
        val updated = base.withAppAdded("b", 2, 2, null, grid)
        assertFalse(WindowClass.MEDIUM in updated.profiles)
        // …but the updated canonical order includes it, so first-use
        // derivation of MEDIUM picks it up automatically.
        val derived = deriveProfile(updated.canonical, mapOf("a" to (2 to 2), "b" to (2 to 2)), 4, 6)
        assertTrue("b" in derived)
    }

    @Test
    fun withAppRemoved_removesFromCanonicalAndEveryProfile() {
        val base = LayoutProfiles(
            canonical = listOf(CanonicalEntry("a", 0), CanonicalEntry("b", 0)),
            profiles = mapOf(
                WindowClass.COMPACT to mapOf(
                    "a" to ProfilePlacement(0, 0, 0, 2, 2),
                    "b" to ProfilePlacement(0, 2, 0, 2, 2),
                ),
            ),
        )
        val updated = base.withAppRemoved("a")
        assertEquals(listOf("b"), updated.canonical.map { it.id })
        assertEquals(setOf("b"), updated.profiles.getValue(WindowClass.COMPACT).keys)
    }

    @Test
    fun syncLayouts_previewAndApplyRederiveFromSource() {
        // The source (COMPACT) profile was REORDERED by the user
        // (reversed), so its order differs from canonical order; syncing
        // from it must move items in EXPANDED, which was derived from
        // the original canonical order.
        val canonical = (0 until 6).map { CanonicalEntry("app$it", 0) }
        val spans = (0 until 6).associate { "app$it" to (2 to 2) }
        val compactReversed = mapOf(
            "app5" to ProfilePlacement(0, 0, 0, 2, 2),
            "app4" to ProfilePlacement(0, 2, 0, 2, 2),
            "app3" to ProfilePlacement(0, 4, 0, 2, 2),
            "app2" to ProfilePlacement(0, 6, 0, 2, 2),
            "app1" to ProfilePlacement(0, 0, 2, 2, 2),
            "app0" to ProfilePlacement(0, 2, 2, 2, 2),
        )
        val base = LayoutProfiles(
            canonical = canonical,
            profiles = mapOf(
                WindowClass.COMPACT to compactReversed,
                WindowClass.EXPANDED_LANDSCAPE to deriveProfile(canonical, spans, 6, 6),
            ),
        )
        val grids = { wc: WindowClass -> if (wc == WindowClass.COMPACT) 4 to 6 else 6 to 6 }
        val (preview, synced) = syncLayoutsPreview(base, WindowClass.COMPACT, grids)
        assertTrue(preview.isNotEmpty())
        assertTrue(preview.all { it.windowClass == WindowClass.EXPANDED_LANDSCAPE })
        // Applying the preview makes EXPANDED exactly the re-derivation
        // from the REVERSED source order.
        val applied = syncLayoutsFrom(base, WindowClass.COMPACT, grids)
        assertEquals(synced, applied)
        val reversedCanonical = canonical.reversed()
        assertEquals(
            deriveProfile(reversedCanonical, spans, 6, 6),
            applied.profiles.getValue(WindowClass.EXPANDED_LANDSCAPE),
        )
        // The source profile itself is untouched by the sync.
        assertEquals(
            base.profiles.getValue(WindowClass.COMPACT),
            applied.profiles.getValue(WindowClass.COMPACT),
        )
    }

    @Test
    fun profilesJson_roundTrip() {
        val profiles = LayoutProfiles(
            canonical = listOf(CanonicalEntry("a", 0), CanonicalEntry("b", 1)),
            profiles = mapOf(
                WindowClass.COMPACT to mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2)),
            ),
        )
        assertEquals(profiles, parseLayoutProfiles(profiles.toJson()))
    }

    @Test
    fun profilesJson_malformedYieldsEmpty() {
        assertEquals(LayoutProfiles(), parseLayoutProfiles("{not json"))
        assertEquals(LayoutProfiles(), parseLayoutProfiles(""))
    }

    @Test
    fun firstFreeSpotInProfile_findsGapAndReportsFull() {
        val placed = mapOf("a" to ProfilePlacement(0, 0, 0, 2, 2))
        // 4x2-unit page (2 cells wide): free spot is (2, 0).
        assertEquals(2 to 0, firstFreeSpotInProfile(placed, 0, 2, 2, 4, 4))
        // Oversize never fits.
        assertNull(firstFreeSpotInProfile(placed, 0, 6, 2, 4, 4))
    }
}
