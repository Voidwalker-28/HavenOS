package app.havenos.launcher.ui.settings.hubs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Coordination fix step 4: grid-overflow destinations — page entries in
 * page order, never collapsing the count line, collapsing only the
 * destination list past 8 entries with a "+M more" expansion.
 * Written before the production code (TDD).
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class CoordinationOverflowTest {

    @Test
    fun destinationEntries_sortedByPageWithPagerPositions() {
        val destinations = mapOf(2 to 1, 0 to 3, 1 to 2)
        // No Today page: grid page g is pager position g+1.
        assertEquals(
            listOf("page 1 (3)", "page 2 (2)", "page 3 (1)"),
            moveDestinationEntries(destinations, todayOn = false),
        )
        // Today occupies pager position 1: grid page g is position g+2.
        assertEquals(
            listOf("page 2 (3)", "page 3 (2)", "page 4 (1)"),
            moveDestinationEntries(destinations, todayOn = true),
        )
    }

    @Test
    fun visibleEntries_eightOrFewer_showsAllWithNoMore() {
        val entries = (1..8).map { "page $it (1)" }
        val (shown, more) = visibleDestinationEntries(entries, expanded = false)
        assertEquals(entries, shown)
        assertEquals(0, more)
    }

    @Test
    fun visibleEntries_moreThanEight_showsFirstEightPlusMore() {
        val entries = (1..11).map { "page $it (1)" }
        val (shown, more) = visibleDestinationEntries(entries, expanded = false)
        assertEquals(entries.take(8), shown)
        assertEquals(3, more)
    }

    @Test
    fun visibleEntries_expanded_showsAll() {
        val entries = (1..11).map { "page $it (1)" }
        val (shown, more) = visibleDestinationEntries(entries, expanded = true)
        assertEquals(entries, shown)
        assertEquals(0, more)
    }
}
