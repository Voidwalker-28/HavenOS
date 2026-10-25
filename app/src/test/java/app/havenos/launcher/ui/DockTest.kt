package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 Wave 1 — slot calculation for [OneDock]. Pure functions only
 * ([computeDockLayout], [dockCapacity], [derivedIconBackdropArgb]):
 * no Compose runtime, no Android framework, so these run as plain JVM
 * unit tests.
 *
 * M9 scroll semantics: pinned apps are NEVER dropped — when they
 * exceed the visible app slots the pill scrolls ([DockLayout.pinnedScrolls])
 * and recents drop first (to zero while scrolling). Icons are never
 * shrunk: [DOCK_ICON_DP] is a constant 48dp and capacity comes from gap
 * compression (24dp nominal → 8dp minimum), never from a smaller icon.
 */
class DockTest {

    // ---------- capacity ----------

    @Test
    fun capacity_compact390_fitsSixItems() {
        // 32 padding + 6*48 + 5*8 = 360 <= 390; 7 items need 416.
        assertEquals(6, dockCapacity(390f))
    }

    @Test
    fun capacity_expanded1000_fitsSeventeenItems() {
        assertEquals(17, dockCapacity(1000f))
    }

    @Test
    fun capacity_zeroOrNegative_isZero() {
        assertEquals(0, dockCapacity(0f))
        assertEquals(0, dockCapacity(-50f))
    }

    // ---------- Compact width: 4-5 pinned, no recents ----------

    @Test
    fun compact_390_sixPinnedScroll_noRecents() {
        // M9: capacity 6 = 4 app slots + 2 buttons; 6 pinned exceed the
        // 4 visible slots, so the pill scrolls instead of dropping any.
        val layout = computeDockLayout(390f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(layout.pinnedScrolls)
        assertEquals(4, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
        assertTrue(layout.showSearch)
        assertTrue(layout.showLibrary)
    }

    @Test
    fun compact_430_sixPinnedScroll_noRecents() {
        // Capacity 7 = 5 app slots; 6 pinned still scroll.
        val layout = computeDockLayout(430f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(layout.pinnedScrolls)
        assertEquals(5, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
    }

    // ---------- recents dropped first, then pinned ----------

    @Test
    fun medium_500_recentsDroppedFirst_pinnedIntact() {
        // Capacity 8 = 6 app slots + 2 buttons; 6 pinned fit exactly,
        // so no scroll and recents get nothing.
        val layout = computeDockLayout(500f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(6, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
    }

    @Test
    fun medium_560_partialRecents_afterFullPinned() {
        // Capacity 9 = 6 pinned + 1 recent + 2 buttons.
        val layout = computeDockLayout(560f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(1, layout.recentsShown)
    }

    @Test
    fun narrow_300_pinnedScroll_recentsGone() {
        // M9: capacity 4 = 2 app slots + 2 buttons; 6 pinned scroll
        // instead of dropping to 2.
        val layout = computeDockLayout(300f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(layout.pinnedScrolls)
        assertEquals(2, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
    }

    // ---------- Expanded width ----------

    @Test
    fun expanded_1000_showsAllPinnedAndRecents() {
        val layout = computeDockLayout(1000f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(3, layout.recentsShown)
    }

    @Test
    fun expanded_recentsCappedAtThree() {
        val layout = computeDockLayout(1200f, pinnedCount = 4, recentsCount = 8, recentsOn = true)
        assertEquals(4, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(3, layout.recentsShown)
    }

    // ---------- recents toggle / empty states ----------

    @Test
    fun recentsOff_hidesRecentsEvenWithRoom() {
        val layout = computeDockLayout(1200f, pinnedCount = 4, recentsCount = 3, recentsOn = false)
        assertEquals(4, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(0, layout.recentsShown)
    }

    @Test
    fun emptyLists_buttonsStillShown() {
        val layout = computeDockLayout(390f, pinnedCount = 0, recentsCount = 0, recentsOn = true)
        assertEquals(0, layout.pinnedShown)
        assertTrue(!layout.pinnedScrolls)
        assertEquals(4, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
        assertTrue(layout.showSearch)
        assertTrue(layout.showLibrary)
    }

    @Test
    fun tinyWidth_buttonsStillShown_pinnedScroll() {
        // M9: capacity 1 < 2 buttons, so app slots clamp to 0 — but
        // pinned apps still scroll (never dropped); buttons stay.
        val layout = computeDockLayout(120f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, layout.pinnedShown)
        assertTrue(layout.pinnedScrolls)
        assertEquals(0, layout.visibleAppSlots)
        assertEquals(0, layout.recentsShown)
        assertTrue(layout.showSearch)
        assertTrue(layout.showLibrary)
    }

    // ---------- icons never shrink ----------

    @Test
    fun iconSize_is48dp_neverReduced() {
        assertEquals(48f, DOCK_ICON_DP, 0f)
        // The layout carries counts only — there is no icon-size output
        // to shrink, at any width.
        val tiny = computeDockLayout(120f, pinnedCount = 6, recentsCount = 3, recentsOn = true)
        assertEquals(6, tiny.pinnedShown)
        assertTrue(tiny.pinnedScrolls)
        assertEquals(48f, DOCK_ICON_DP, 0f)
    }

    // ---------- derived icon backdrop ----------

    @Test
    fun backdrop_lightTheme_isNotWhite() {
        // Minimal light: surface white, on-surface near-black.
        val backdrop = derivedIconBackdropArgb(0xFFFFFFFF, 0xFF111111)
        assertTrue(backdrop != 0xFFFFFFFF)
        // 80% white + 20% near-black = 0xCFCFCF-ish grey: visibly bounded.
        val r = ((backdrop shr 16) and 0xFF).toInt()
        assertTrue(r in 190..220)
    }

    @Test
    fun backdrop_darkTheme_isNotBlack() {
        // Minimal dark: surface near-black, on-surface near-white.
        val backdrop = derivedIconBackdropArgb(0xFF0D0D0D, 0xFFF5F5F5)
        assertTrue(backdrop != 0xFF0D0D0D)
        val r = ((backdrop shr 16) and 0xFF).toInt()
        assertTrue(r in 40..80)
    }

    @Test
    fun backdrop_preservesAlpha() {
        val backdrop = derivedIconBackdropArgb(0xFFFFFFFF, 0xFF111111)
        assertEquals(0xFF, ((backdrop shr 24) and 0xFF).toInt())
    }

    // ---------- v10 adaptive-icon contrast ----------

    @Test
    fun adaptive_whiteIconOnLightSurface_needsBackdrop() {
        assertTrue(adaptiveIconNeedsBackdrop(0.95f, 0.90f))
    }

    @Test
    fun adaptive_blackIconOnDarkSurface_needsBackdrop() {
        assertTrue(adaptiveIconNeedsBackdrop(0.05f, 0.10f))
    }

    @Test
    fun adaptive_darkIconOnLightSurface_keepsFullBleed() {
        assertTrue(!adaptiveIconNeedsBackdrop(0.05f, 0.90f))
    }

    @Test
    fun adaptive_whiteIconOnDarkSurface_keepsFullBleed() {
        assertTrue(!adaptiveIconNeedsBackdrop(0.95f, 0.10f))
    }

    @Test
    fun adaptive_gapBoundary_isExclusive() {
        // Exactly at the 0.22 gap: not "too close", no backdrop.
        assertTrue(!adaptiveIconNeedsBackdrop(0.50f, 0.50f + ADAPTIVE_BACKDROP_LUMINANCE_GAP))
    }

    // ---------- v10 dock correction: sRGB linearisation ----------

    @Test
    fun srgbChannelToLinear_darkGreyCentreOnNearBlackDock_needsBackdrop() {
        // The failure case: a dark-grey icon centre (sRGB 0.30) on a
        // near-black dock (linear 0.004). Raw, the gap reads 0.30 —
        // above threshold, no backdrop, icon nearly invisible.
        // Linearised the centre is ~0.073, a 0.07 gap, so the
        // backdrop is required.
        val linear = srgbChannelToLinear(0.30f)
        assertEquals(0.073f, linear, 0.005f)
        assertTrue(!adaptiveIconNeedsBackdrop(0.30f, 0.004f))
        assertTrue(adaptiveIconNeedsBackdrop(linear, 0.004f))
        // Linear branch of the formula, and the endpoints.
        assertEquals(0.00313f, srgbChannelToLinear(0.04045f), 0.0005f)
        assertEquals(0.0f, srgbChannelToLinear(0.0f), 0.0f)
        assertEquals(1.0f, srgbChannelToLinear(1.0f), 0.001f)
    }

    @Test
    fun srgbChannelToLinear_darkGreyCentreOnLightDock_keepsFullBleed() {
        // Light-dock sanity: the same linearised icon is far from a
        // light dock, so it keeps drawing full-bleed.
        assertTrue(!adaptiveIconNeedsBackdrop(srgbChannelToLinear(0.30f), 0.95f))
    }
}
