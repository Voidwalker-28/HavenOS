package app.havenos.launcher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M9 Worker B — dock side mapping, size table, and scroll-semantics
 * edges for [OneDock]. Pure functions only ([dockSideFromKey],
 * [dockThicknessFor], [dockMaxWidthFor], [computeDockLayout]): no
 * Compose runtime, no Android framework.
 *
 * Written, NOT run: the sandbox Gradle test executor is known-broken
 * (see docs/UNVERIFIED.md); the coordinator's build is the first run
 * of these tests.
 */
class DockM9Test {

    // ---------- side mapping (stored dockPos int -> DockSide) ----------

    @Test
    fun sideMapping_bottomIsDefault() {
        assertEquals(DockSide.Bottom, dockSideFromKey(0))
    }

    @Test
    fun sideMapping_leftAndRight() {
        assertEquals(DockSide.Left, dockSideFromKey(1))
        assertEquals(DockSide.Right, dockSideFromKey(2))
    }

    @Test
    fun sideMapping_unknownFallsBackToBottom() {
        // Stale-type-safe: a stored value from an older build (or a
        // corrupt row) must never crash the dock or hide it.
        assertEquals(DockSide.Bottom, dockSideFromKey(3))
        assertEquals(DockSide.Bottom, dockSideFromKey(-1))
    }

    // ---------- thickness table (Doc 2 §1) ----------

    @Test
    fun thickness_bottomDock() {
        assertEquals(76.dp, dockThicknessFor(HomeSizeClass.Expanded, DockSide.Bottom))
        assertEquals(72.dp, dockThicknessFor(HomeSizeClass.Medium, DockSide.Bottom))
        assertEquals(64.dp, dockThicknessFor(HomeSizeClass.Compact, DockSide.Bottom))
    }

    @Test
    fun thickness_sideDock() {
        assertEquals(80.dp, dockThicknessFor(HomeSizeClass.Expanded, DockSide.Left))
        assertEquals(80.dp, dockThicknessFor(HomeSizeClass.Expanded, DockSide.Right))
        assertEquals(76.dp, dockThicknessFor(HomeSizeClass.Medium, DockSide.Left))
        assertEquals(76.dp, dockThicknessFor(HomeSizeClass.Medium, DockSide.Right))
        assertEquals(64.dp, dockThicknessFor(HomeSizeClass.Compact, DockSide.Left))
        assertEquals(64.dp, dockThicknessFor(HomeSizeClass.Compact, DockSide.Right))
    }

    // ---------- max width (Doc 2 §1 slot counts fall out of this) ----------

    @Test
    fun maxWidth_expandedHoldsTwelveSlots() {
        // 720dp: capacity 12 = 10 app slots + 2 buttons.
        assertEquals(720.dp, dockMaxWidthFor(HomeSizeClass.Expanded))
        val layout = computeDockLayout(720f, pinnedCount = 10, recentsCount = 3, recentsOn = true)
        assertEquals(10, layout.visibleAppSlots)
        assertFalse(layout.pinnedScrolls)
    }

    @Test
    fun maxWidth_mediumHoldsTenSlots() {
        // 584dp: capacity 10 = 8 app slots + 2 buttons.
        assertEquals(584.dp, dockMaxWidthFor(HomeSizeClass.Medium))
        val layout = computeDockLayout(584f, pinnedCount = 8, recentsCount = 3, recentsOn = true)
        assertEquals(8, layout.visibleAppSlots)
        assertFalse(layout.pinnedScrolls)
    }

    @Test
    fun maxWidth_compactIsUncapped() {
        assertNull(dockMaxWidthFor(HomeSizeClass.Compact))
    }

    // ---------- scroll semantics edges ----------

    @Test
    fun scroll_recentsDroppedFirst_evenWhenRoomWouldFitThem() {
        // 6 pinned scroll at 390dp; recents are zero while scrolling
        // even though a shorter pinned list would leave room for them.
        val scrolling = computeDockLayout(390f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertTrue(scrolling.pinnedScrolls)
        assertEquals(0, scrolling.recentsShown)
        val fitting = computeDockLayout(390f, pinnedCount = 2, recentsCount = 3, recentsOn = true)
        assertFalse(fitting.pinnedScrolls)
        assertEquals(2, fitting.recentsShown)
    }

    @Test
    fun scroll_exactFitDoesNotScroll() {
        // 4 pinned at 390dp = exactly the 4 visible app slots.
        val layout = computeDockLayout(390f, pinnedCount = 4, recentsCount = 3, recentsOn = true)
        assertFalse(layout.pinnedScrolls)
        assertEquals(4, layout.pinnedShown)
        assertEquals(0, layout.recentsShown)
    }

    @Test
    fun scroll_recentsOffHidesRecentsEvenWithoutScroll() {
        val layout = computeDockLayout(1000f, pinnedCount = 6, recentsCount = 3, recentsOn = false)
        assertFalse(layout.pinnedScrolls)
        assertEquals(0, layout.recentsShown)
    }

    @Test
    fun scroll_negativeCountsClamped() {
        val layout = computeDockLayout(1000f, pinnedCount = -2, recentsCount = -1, recentsOn = true)
        assertEquals(0, layout.pinnedShown)
        assertEquals(0, layout.recentsShown)
        assertFalse(layout.pinnedScrolls)
    }

    @Test
    fun iconSize_is48dp_neverReduced() {
        assertEquals(48f, DOCK_ICON_DP, 0f)
        assertEquals(72f, DOCK_SLOT_PITCH_DP, 0f)
    }
}
