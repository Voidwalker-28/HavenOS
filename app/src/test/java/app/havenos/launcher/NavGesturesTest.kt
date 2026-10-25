package app.havenos.launcher

import app.havenos.launcher.data.DEFAULT_NAV_MAP
import app.havenos.launcher.data.NavEdge
import app.havenos.launcher.data.classifySwipe
import app.havenos.launcher.data.navThresholdPx
import app.havenos.launcher.data.parseNavMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavGesturesTest {

    // ---------- classifySwipe: BOTTOM (swipe up) ----------

    @Test
    fun classifySwipe_bottomUpFires() {
        assertEquals("up", classifySwipe(NavEdge.BOTTOM, 0f, -100f, 50f))
        assertEquals("up", classifySwipe(NavEdge.BOTTOM, 40f, -100f, 50f))
    }

    @Test
    fun classifySwipe_bottomBelowThresholdIsNull() {
        assertNull(classifySwipe(NavEdge.BOTTOM, 0f, -30f, 50f))
        assertNull(classifySwipe(NavEdge.BOTTOM, 0f, -50f, 50f)) // at threshold, not past it
        assertNull(classifySwipe(NavEdge.BOTTOM, 0f, 100f, 50f)) // wrong direction
    }

    @Test
    fun classifySwipe_bottomDiagonalRejected() {
        assertNull(classifySwipe(NavEdge.BOTTOM, 80f, -100f, 50f))
        assertNull(classifySwipe(NavEdge.BOTTOM, -80f, -100f, 50f))
    }

    // ---------- classifySwipe: LEFT edge (swipe right, inward) ----------

    @Test
    fun classifySwipe_leftEdgeInwardFires() {
        assertEquals("left", classifySwipe(NavEdge.LEFT, 100f, 0f, 50f))
        assertEquals("left", classifySwipe(NavEdge.LEFT, 100f, 40f, 50f))
    }

    @Test
    fun classifySwipe_leftEdgeBelowThresholdIsNull() {
        assertNull(classifySwipe(NavEdge.LEFT, 30f, 0f, 50f))
        assertNull(classifySwipe(NavEdge.LEFT, -100f, 0f, 50f)) // wrong direction
    }

    @Test
    fun classifySwipe_leftEdgeDiagonalRejected() {
        assertNull(classifySwipe(NavEdge.LEFT, 100f, 80f, 50f))
        assertNull(classifySwipe(NavEdge.LEFT, 100f, -80f, 50f))
    }

    // ---------- classifySwipe: RIGHT edge (swipe left, inward) ----------

    @Test
    fun classifySwipe_rightEdgeInwardFires() {
        assertEquals("right", classifySwipe(NavEdge.RIGHT, -100f, 0f, 50f))
        assertEquals("right", classifySwipe(NavEdge.RIGHT, -100f, -40f, 50f))
    }

    @Test
    fun classifySwipe_rightEdgeBelowThresholdIsNull() {
        assertNull(classifySwipe(NavEdge.RIGHT, -30f, 0f, 50f))
        assertNull(classifySwipe(NavEdge.RIGHT, 100f, 0f, 50f)) // wrong direction
    }

    @Test
    fun classifySwipe_rightEdgeDiagonalRejected() {
        assertNull(classifySwipe(NavEdge.RIGHT, -100f, 80f, 50f))
        assertNull(classifySwipe(NavEdge.RIGHT, -100f, -80f, 50f))
    }

    // ---------- navThresholdPx ----------

    @Test
    fun navThresholdPx_endpoints() {
        assertEquals(56.0, navThresholdPx(0, 1f).toDouble(), 0.001)
        assertEquals(20.0, navThresholdPx(100, 1f).toDouble(), 0.001)
        assertEquals(38.0, navThresholdPx(50, 1f).toDouble(), 0.001)
    }

    @Test
    fun navThresholdPx_scalesWithDensity() {
        assertEquals(112.0, navThresholdPx(0, 2f).toDouble(), 0.001)
        assertEquals(40.0, navThresholdPx(100, 2f).toDouble(), 0.001)
    }

    @Test
    fun navThresholdPx_monotonicDecrease() {
        val values = (0..100 step 10).map { navThresholdPx(it, 1f) }
        assertTrue(values.zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun navThresholdPx_outOfRangeSensitivityClamps() {
        assertEquals(56.0, navThresholdPx(-10, 1f).toDouble(), 0.001)
        assertEquals(20.0, navThresholdPx(150, 1f).toDouble(), 0.001)
    }

    // ---------- parseNavMap ----------

    @Test
    fun parseNavMap_validCustomMap() {
        val map = parseNavMap("""{"up":"back","upHold":"quickPanel","left":"home","right":"none"}""")
        assertEquals(
            mapOf("up" to "back", "upHold" to "quickPanel", "left" to "home", "right" to "none"),
            map,
        )
    }

    @Test
    fun parseNavMap_defaultMapRoundTrips() {
        assertEquals(
            mapOf("up" to "home", "upHold" to "recents", "left" to "back", "right" to "back"),
            parseNavMap(DEFAULT_NAV_MAP),
        )
    }

    @Test
    fun parseNavMap_garbageJsonFallsBackToDefaults() {
        val defaults = parseNavMap(DEFAULT_NAV_MAP)
        assertEquals(defaults, parseNavMap("not json"))
        assertEquals(defaults, parseNavMap(""))
        assertEquals(defaults, parseNavMap("{}"))
    }

    @Test
    fun parseNavMap_unknownActionFallsBackPerKey() {
        val map = parseNavMap("""{"up":"explode","left":"home"}""")
        assertEquals("home", map["up"]) // unknown action -> default for that key only
        assertEquals("home", map["left"]) // valid custom value kept
        assertEquals("recents", map["upHold"])
        assertEquals("back", map["right"])
    }

    @Test
    fun parseNavMap_missingKeyFallsBackPerKey() {
        val map = parseNavMap("""{"right":"quickPanel"}""")
        assertEquals("quickPanel", map["right"])
        assertEquals("home", map["up"])
        assertEquals("recents", map["upHold"])
        assertEquals("back", map["left"])
    }
}
