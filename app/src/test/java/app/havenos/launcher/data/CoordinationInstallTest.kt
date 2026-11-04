package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordination fix step 2: install membership — canonical entry always,
 * profile placement only in the CURRENT profile and only when auto-add
 * is on (doc §1). Never places in every profile (that was the old
 * withAppAdded behaviour, kept verbatim for its other callers).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationInstallTest {

    private val grid = { _: WindowClass -> 4 to 6 }
    private val existing = mapOf(
        "a" to ProfilePlacement(0, 0, 0, 2, 2),
    )
    private val base = LayoutProfiles(
        canonical = listOf(CanonicalEntry("a", 0)),
        profiles = mapOf(
            WindowClass.COMPACT to existing,
            WindowClass.MEDIUM to existing,
        ),
    )

    @Test
    fun installed_autoAddOn_placesOnlyInCurrentProfile() {
        val out = base.withAppInstalled("b", 2, 2, autoAdd = true, current = WindowClass.MEDIUM, gridCells = grid)
        assertEquals(listOf(CanonicalEntry("a", 0), CanonicalEntry("b", 0)), out.canonical)
        assertEquals(ProfilePlacement(0, 2, 0, 2, 2), out.profiles[WindowClass.MEDIUM]?.get("b"))
        assertNull(out.profiles[WindowClass.COMPACT]?.get("b"))
    }

    @Test
    fun installed_autoAddOff_addsCanonicalOnly() {
        val out = base.withAppInstalled("b", 2, 2, autoAdd = false, current = WindowClass.MEDIUM, gridCells = grid)
        assertEquals(listOf(CanonicalEntry("a", 0), CanonicalEntry("b", 0)), out.canonical)
        assertNull(out.profiles[WindowClass.MEDIUM]?.get("b"))
        assertNull(out.profiles[WindowClass.COMPACT]?.get("b"))
    }

    @Test
    fun installed_currentProfileAbsent_addsCanonicalOnly() {
        val noMedium = base.copy(profiles = mapOf(WindowClass.COMPACT to existing))
        val out = noMedium.withAppInstalled("b", 2, 2, autoAdd = true, current = WindowClass.MEDIUM, gridCells = grid)
        assertEquals(2, out.canonical.size)
        assertNull(out.profiles[WindowClass.MEDIUM])
    }

    @Test
    fun installed_existingOrBlank_isUnchanged() {
        assertEquals(base, base.withAppInstalled("a", 2, 2, autoAdd = true, current = WindowClass.MEDIUM, gridCells = grid))
        assertEquals(base, base.withAppInstalled("", 2, 2, autoAdd = true, current = WindowClass.MEDIUM, gridCells = grid))
    }

    @Test
    fun installed_fullTargetPage_spillsForward() {
        // 4-col grid (8 units): page 0 holds 4 icons per row; fill page 0's
        // first row completely so the new icon spills to the next free spot.
        val fullRow = (0..3).associate { i -> "r$i" to ProfilePlacement(0, i * 2, 0, 2, 2) }
        val profiles = LayoutProfiles(
            canonical = (0..3).map { CanonicalEntry("r$it", 0) },
            profiles = mapOf(WindowClass.MEDIUM to fullRow),
        )
        val out = profiles.withAppInstalled("new", 2, 2, autoAdd = true, current = WindowClass.MEDIUM, gridCells = grid)
        val placed = out.profiles[WindowClass.MEDIUM]?.get("new")
        assertTrue(placed != null)
        // Row 0 full: lands on row 1 (row units 2) of page 0, not page 1.
        assertEquals(0, placed!!.page)
        assertEquals(2, placed.row)
    }
}
