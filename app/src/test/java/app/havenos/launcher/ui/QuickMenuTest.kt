package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase A10/A11 flow 13: every long-press menu action maps to
 * exactly one destination with its label.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class QuickMenuTest {

    @Test
    fun menu_hasSevenItemsInOrder() {
        val menu = quickCustomizeMenu()
        assertEquals(
            listOf(
                "Wallpaper", "Layout", "Widgets", "Dock",
                "Look", "Home settings", "Edit home",
            ),
            menu.map { it.label },
        )
    }

    @Test
    fun menu_destinationsAreUniqueAndComplete() {
        val destinations = quickCustomizeMenu().map { it.destination }
        assertEquals(7, destinations.size)
        assertEquals(QuickMenuDestination.entries.toSet(), destinations.toSet())
    }

    @Test
    fun menu_styleRenamedToLook() {
        // The old "Style" label must not survive: it opens the Look hub.
        val menu = quickCustomizeMenu()
        assertTrue(menu.none { it.label == "Style" })
        assertEquals(
            QuickMenuDestination.LOOK,
            menu.first { it.label == "Look" }.destination,
        )
    }

    @Test
    fun menu_mappingMatchesPrompt() {
        val byLabel = quickCustomizeMenu().associate { it.label to it.destination }
        assertEquals(QuickMenuDestination.WALLPAPER, byLabel["Wallpaper"])
        assertEquals(QuickMenuDestination.GRID, byLabel["Layout"])
        assertEquals(QuickMenuDestination.WIDGET_SHEET, byLabel["Widgets"])
        assertEquals(QuickMenuDestination.DOCK, byLabel["Dock"])
        assertEquals(QuickMenuDestination.HOME_DOCK, byLabel["Home settings"])
        assertEquals(QuickMenuDestination.EDIT_HOME, byLabel["Edit home"])
    }
}
