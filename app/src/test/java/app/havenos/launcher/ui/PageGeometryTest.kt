package app.havenos.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 3.1 Phase A: page geometry (A4), glance rule (A5), rail inset (A6).
 * Pure functions only — Compose modifier clipping (A4's clipToBounds)
 * and the dock tile restyle (A7) cannot be asserted in a JVM test and
 * are covered by the Phase G tablet checklist instead.
 */
class PageGeometryTest {

    // A4: parallax shift -------------------------------------------------

    @Test
    fun parallax_currentPage_zero() {
        assertEquals(0f, pageParallaxX(0f, 1000f, true, false), 0.001f)
    }

    @Test
    fun parallax_neighbourPage_shiftsEightPercent() {
        // The neighbour at rest sits at offset ±1: its content shifts
        // 8% of a page width toward the viewport — the sliver the
        // clipToBounds on the page Box now contains.
        assertEquals(80f, pageParallaxX(1f, 1000f, true, false), 0.001f)
        assertEquals(-80f, pageParallaxX(-1f, 1000f, true, false), 0.001f)
    }

    @Test
    fun parallax_midSwipe_scalesWithOffset() {
        assertEquals(40f, pageParallaxX(0.5f, 1000f, true, false), 0.001f)
    }

    @Test
    fun parallax_off_orReducedMotion_zero() {
        assertEquals(0f, pageParallaxX(1f, 1000f, false, false), 0.001f)
        assertEquals(0f, pageParallaxX(1f, 1000f, true, true), 0.001f)
    }

    // A5: glance only when Today is not visible anywhere --------------

    @Test
    fun glance_shown_onlyWhenTodayOff() {
        assertEquals(true, glanceVisible(false))
        assertEquals(false, glanceVisible(true))
    }

    @Test
    fun glance_hidden_forEveryTodayForm() {
        // Rail (Expanded landscape), strip (Medium) and pager page
        // (Compact) all count as Today visible — the old pager-flag
        // check re-showed the glance beside the rail and the strip.
        assertEquals(false, glanceVisible(true)) // rail
        assertEquals(false, glanceVisible(true)) // strip
        assertEquals(false, glanceVisible(true)) // pager page
    }

    // A6: rail bottom inset --------------------------------------------

    @Test
    fun railInset_bottomDock_heightPlus16() {
        assertEquals(72f, railBottomInsetDp(56f, true), 0.001f)
        assertEquals(16f, railBottomInsetDp(0f, true), 0.001f)
    }

    @Test
    fun railInset_sideDock_zero() {
        assertEquals(0f, railBottomInsetDp(56f, false), 0.001f)
    }
}
