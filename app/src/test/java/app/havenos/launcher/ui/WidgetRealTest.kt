package app.havenos.launcher.ui

import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M9 Worker C — widgets real: free-spot search, widget state machine,
 * weather unit formatting, plus stack/today/suggestion pure logic.
 * Written-not-run if the sandbox Gradle test executor crashes (known
 * pre-existing failure); the compile check is the gate that must pass.
 */
class WidgetRealTest {

    private fun item(id: String, kind: Int, page: Int, col: Int, row: Int, w: Int, h: Int, payload: String = "") =
        HomeItem(id, kind, page, col, row, w, h, payload)

    // ---------------------------------------------------------- free spot

    @Test
    fun freeSpot_rowMajor_firstFit() {
        val items = listOf(item("a", ItemKind.APP, 0, 0, 0, 2, 2))
        // 4x2-cell widget (8x4 units) on an 8x12-unit grid: row-major
        // scan tries (col 0, row 0) and (col 0, row 1) — both overlap the
        // 2x2-unit icon — then fits at (col 0, row 2).
        val found = findSpotForSizes(items, 0, listOf(8 to 4), 8, 12)
        assertNotNull(found)
        assertEquals(0, found!!.first)
        assertEquals(2, found.second)
        assertEquals(8 to 4, found.third)
    }

    @Test
    fun freeSpot_triesSmallerSizeWhenLargeDoesNotFit() {
        // Page 0 fully occupied except a 2x2-cell corner.
        val items = listOf(
            item("full", ItemKind.APP, 0, 0, 0, 8, 8),
            item("corner", ItemKind.APP, 0, 0, 8, 4, 4),
        )
        // 4x4 and 4x2 (units 8x8, 8x4) do not fit; 2x2 (4x4) fits at (4, 8).
        val found = findSpotForSizes(items, 0, listOf(8 to 8, 8 to 4, 4 to 4), 8, 12)
        assertNotNull(found)
        assertEquals(4 to 4, found!!.third)
        assertEquals(4, found.first)
        assertEquals(8, found.second)
    }

    @Test
    fun freeSpot_noneWhenPageFull() {
        val items = listOf(item("full", ItemKind.APP, 0, 0, 0, 8, 12))
        assertNull(findSpotForSizes(items, 0, listOf(4 to 4), 8, 12))
    }

    @Test
    fun placeAcrossPages_currentFirstThenOthersThenNewPage() {
        val full = item("full", ItemKind.APP, 1, 0, 0, 8, 12)
        val items = listOf(full)
        // Start page 1 is full; page 0 is free.
        val found = findSpotAcrossPagesSized(items, 1, 2, listOf(4 to 4), 8, 12)
        assertNotNull(found)
        assertEquals(0, found!!.first)
        // Host helper: every page full -> new page placement.
        val placed = buildPlacedWidget(
            ItemKind.WEATHER, 8, 4, "", listOf(full, item("full0", ItemKind.APP, 0, 0, 0, 8, 12)),
            0, 2, 8, 12, 5,
        )
        assertNotNull(placed)
        assertTrue(placed!!.needsNewPage)
        assertEquals(2, placed.item.page)
    }

    @Test
    fun buildPlacedWidget_nullWhenGridTooSmall() {
        // A 2x2-unit grid is smaller than even the smallest weather size
        // (2x2 cells = 4x4 units), so placement is impossible.
        assertNull(
            buildPlacedWidget(ItemKind.WEATHER, 8, 4, "", emptyList(), 0, 1, 2, 2, 0),
        )
    }

    // ------------------------------------------------------- state machine

    @Test
    fun stateMachine_precedence() {
        assertEquals(WidgetState.PROVIDER_REMOVED, resolveWidgetState(providerRemoved = true, permissionGranted = false))
        assertEquals(WidgetState.NEEDS_PERMISSION, resolveWidgetState(permissionGranted = false, loading = true))
        assertEquals(WidgetState.ERROR, resolveWidgetState(error = true, hasData = false))
        assertEquals(WidgetState.OFFLINE, resolveWidgetState(error = true, hasData = true, staleCache = true))
        assertEquals(WidgetState.LOADING, resolveWidgetState(loading = true))
        assertEquals(WidgetState.OFFLINE, resolveWidgetState(hasData = true, staleCache = true))
        assertEquals(WidgetState.EMPTY, resolveWidgetState(isEmpty = true))
        assertEquals(WidgetState.READY, resolveWidgetState(hasData = true))
    }

    // ------------------------------------------------------------ weather

    @Test
    fun weather_localityOnly_andUnits() {
        assertEquals("Delhi", localityOnly("Delhi, National Capital Territory, India"))
        assertEquals("Delhi", localityOnly("Delhi"))
        assertEquals("28°C", formatTempDisplay(28.4, 0))
        assertEquals("75°F", formatTempDisplay(75.1, 1))
        // Same value, same unit everywhere: formatting never converts.
        assertEquals("28°C", formatTempDisplay(28.0, 0))
    }

    @Test
    fun weather_staleAfter30Min() {
        val now = 1_000_000_000L
        assertFalse(isWeatherStale(now - 29 * 60_000L, now))
        assertTrue(isWeatherStale(now - 31 * 60_000L, now))
        assertTrue(isWeatherStale(0L, now))
        assertEquals("", formatUpdatedAt(0L))
        assertTrue(formatUpdatedAt(1_000_000_000L).startsWith("Updated "))
    }

    // -------------------------------------------------------------- stacks

    @Test
    fun stack_membersAndRotation() {
        assertEquals(listOf(2, 3), parseStackMembers("[2,3]"))
        assertEquals(emptyList<Int>(), parseStackMembers("not json"))
        assertEquals("[2,3]", stackMembersJson(listOf(2, 3)))
        assertEquals(2, stackNextIndex(1, 3, 1))
        assertEquals(0, stackNextIndex(2, 3, 1))
        assertEquals(2, stackNextIndex(0, 3, -1))
        // Morning -> weather, then agenda; evening -> suggested only.
        assertEquals(ItemKind.WEATHER, stackAutoMember(listOf(3, 4), 8))
        assertEquals(ItemKind.AGENDA, stackAutoMember(listOf(4), 8))
        assertEquals(ItemKind.SUGGESTED, stackAutoMember(listOf(3, 16), 20))
        assertNull(stackAutoMember(listOf(3, 4), 20))
        assertFalse(isStackRotationPaused(0L, 5000L))
        assertTrue(isStackRotationPaused(1000L, 1000L + 5_000L))
        assertFalse(isStackRotationPaused(1000L, 1000L + 11_000L))
        assertEquals(listOf(4, 3), stackReordered(listOf(3, 4), 0, 1))
    }

    @Test
    fun stack_unstackPlacements_fitAndOverflow() {
        val stack = item("stack", ItemKind.STACK, 0, 0, 0, 8, 4, "[3,4]")
        val placed = unstackPlacements(stack, listOf(3, 4), emptyList(), 8, 12)
        assertEquals(1, placed.size)
        assertEquals(ItemKind.AGENDA, placed[0].kind)
        // Full page: nothing fits, so the editor must disable Unstack.
        val full = listOf(item("full", ItemKind.APP, 0, 0, 0, 8, 12))
        assertTrue(unstackPlacements(stack, listOf(3, 4), full, 8, 12).isEmpty())
    }

    // ------------------------------------------------------- sizes / today

    @Test
    fun allowedSizes_respectProviderResizeMode() {
        // No resize: only the minimum itself.
        assertEquals(listOf(4 to 2), allowedCellSizes(4, 2, 0))
        // Horizontal only: wider sizes allowed, taller not.
        assertEquals(listOf(4 to 2), allowedCellSizes(4, 2, 1))
        // Both axes: the full chip set at/above the minimum.
        assertTrue(allowedCellSizes(2, 2, 3).containsAll(listOf(2 to 2, 4 to 2, 2 to 4, 4 to 4)))
        assertEquals(4, clampSpan(9, 2, 4))
        assertEquals(2, clampSpan(1, 2, 4))
    }

    @Test
    fun today_flowColumnsAndReorder() {
        assertEquals(2, todayFlowColumns(true))
        assertEquals(1, todayFlowColumns(false))
        val cards = listOf(
            item("a", ItemKind.CLOCK, 0, 0, 0, 8, 4),
            item("b", ItemKind.WEATHER, 0, 0, 0, 8, 4),
            item("c", ItemKind.AGENDA, 0, 0, 0, 8, 4),
        )
        assertEquals(listOf("b", "c", "a"), reorderTodayCards(cards, 0, 2).map { it.id })
        assertEquals(cards, reorderTodayCards(cards, 5, 0))
    }

    @Test
    fun dedupSeededAgendas_keepsFirstOnly() {
        val items = listOf(
            item("w1", ItemKind.WEATHER, 0, 0, 0, 8, 4),
            item("a1", ItemKind.AGENDA, 0, 0, 4, 8, 4),
            item("a2", ItemKind.AGENDA, 0, 0, 8, 8, 4),
            item("g1", ItemKind.AGENDA, 0, 0, 0, 8, 4, "glance"),
        )
        val out = deduplicateSeededAgendas(items)
        assertEquals(listOf("w1", "a1", "g1"), out.map { it.id })
        assertEquals(items, deduplicateSeededAgendas(listOf(items[0], items[1], items[3])))
    }

    @Test
    fun duplicatePlacement_firstFreeSpot() {
        val items = listOf(item("w", ItemKind.WEATHER, 0, 0, 0, 8, 4))
        val dup = duplicateWidgetPlacement(items[0], items, 1, 8, 12)
        assertNotNull(dup)
        assertEquals(0, dup!!.page)
        assertFalse(dup.id == items[0].id)
        // Full page -> null (host offers a new page instead).
        val full = listOf(item("full", ItemKind.APP, 0, 0, 0, 8, 12))
        assertNull(duplicateWidgetPlacement(items[0], full, 1, 8, 12))
    }
}
