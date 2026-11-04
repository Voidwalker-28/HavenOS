package app.havenos.launcher.ui.mascot

import app.havenos.launcher.ui.LibraryApp
import app.havenos.launcher.ui.glass.AccentSelection
import app.havenos.launcher.ui.glass.AccentSource
import app.havenos.launcher.ui.glass.resolveAccentSeed
import app.havenos.launcher.ui.rankLibraryApps
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1.1 (item 1): empty-input crash sweep, JUnit4 against current APIs.
 * Written, not run (sandbox test executor broken + test tree does not
 * compile, pre-existing — see the hotfix report for the exact status).
 *
 * NOTE on placementOf: the firstNotNullOf site fixed in this hotfix
 * lives in a lambda inside the MascotLayer composable, so it cannot be
 * unit-tested directly. These tests cover the testable equivalents:
 * every engine-level null path the host relies on (planRoute,
 * pickTargetLedgePoint, nearestLedgePoint return null, never throw,
 * on empty input), plus the Find/rank and accent paths. The
 * placementOf fallback itself (folderHit null -> dock/empty branches)
 * is a Pad check, listed in DEVICE_CHECKLIST.md.
 */
class HotfixCrashSweepTest {

    private val ledgeA = Ledge(0, 0f, 100f, 0f, LedgeKind.DockTop)
    private val ledgeB = Ledge(1, 1000f, 1100f, 0f, LedgeKind.DockTop)

    @Test
    fun planRoute_emptyLedges_returnsNull() {
        assertNull(
            planRoute(
                LedgePoint(0, 50f, 0f), LedgePoint(1, 1050f, 0f),
                emptyList(), 100f, 100f,
            ),
        )
    }

    @Test
    fun planRoute_unreachable_returnsNull() {
        // Same height, ranges 900px apart, max gap 100px: no jump
        // edge, no drop edge (ranges do not overlap).
        assertNull(
            planRoute(
                LedgePoint(0, 50f, 0f), LedgePoint(1, 1050f, 0f),
                listOf(ledgeA, ledgeB), 100f, 100f,
            ),
        )
    }

    @Test
    fun planRoute_queueHeadNotFirstLedge_doesNotThrow() {
        // vc14 crash (2026-10-31, versionCode 14): the BFS step
        // used ledges.firstOrNull { it.id == queue.removeFirst() },
        // which called removeFirst() once per ledge tested. With
        // the queue head (id 0) NOT first in the ledge list, the
        // second predicate call removed from an empty queue and
        // threw NoSuchElementException (ArrayDeque is empty).
        // Unreachable here (same height, 900px apart), but it must
        // return null, never throw.
        assertNull(
            planRoute(
                LedgePoint(0, 50f, 0f), LedgePoint(1, 1050f, 0f),
                listOf(ledgeB, ledgeA), 100f, 100f,
            ),
        )
    }

    @Test
    fun planRoute_sameLedgeWalk_returnsWalkStep() {
        val steps = planRoute(
            LedgePoint(0, 50f, 0f), LedgePoint(0, 80f, 0f),
            listOf(ledgeA), 100f, 100f,
        )
        assertEquals(listOf(RouteStep.WalkTo(80f)), steps)
    }

    @Test
    fun pickTargetLedgePoint_empty_returnsNull() {
        assertNull(pickTargetLedgePoint(emptyList(), Random(0)))
    }

    @Test
    fun nearestLedgePoint_empty_returnsNull() {
        assertNull(nearestLedgePoint(emptyList(), 50f, 50f))
    }

    @Test
    fun buildLedges_emptySegments_returnsEmpty() {
        assertEquals(
            emptyList<Ledge>(),
            buildLedges(emptyList(), emptyList(), 48f, 48f),
        )
    }

    @Test
    fun spriteHitRect_equalsDrawnRect() {
        // (x, y) is the animated top-left, sizePx the sprite size:
        // the hitbox rect IS the drawn sprite rect, by construction.
        val rect = spriteHitRect(10f, 20f, 48f)
        assertEquals(10f, rect.left, 0f)
        assertEquals(20f, rect.top, 0f)
        assertEquals(58f, rect.right, 0f)
        assertEquals(68f, rect.bottom, 0f)
    }

    @Test
    fun spriteHitRect_edgeCases() {
        val zero = spriteHitRect(5f, 5f, 0f)
        assertEquals(5f, zero.left, 0f)
        assertEquals(5f, zero.right, 0f)
        val neg = spriteHitRect(-10f, -20f, 48f)
        assertEquals(-10f, neg.left, 0f)
        assertEquals(38f, neg.right, 0f)
    }

    @Test
    fun steppedCornerPoints_zeroRadius_isEmpty() {
        assertTrue(steppedCornerPoints(0f, 4f).isEmpty())
    }

    @Test
    fun steppedCornerPoints_isMonotoneStaircaseOnStepGrid() {
        val pts = steppedCornerPoints(40f, 4f)
        assertTrue("expected staircase points", pts.size >= 2)
        // First point on the top edge, last point on the left edge.
        assertEquals(0f, pts.first().y, 0f)
        assertEquals(0f, pts.last().x, 0f)
        for (i in pts.indices) {
            val p = pts[i]
            // Step-grid quantization (radius 40, step 4 divide evenly).
            assertEquals(0f, p.x % 4f, 0f)
            assertEquals(0f, p.y % 4f, 0f)
            if (i > 0) {
                val prev = pts[i - 1]
                // Monotone: x never grows, y never shrinks.
                assertTrue("x monotone at $i", p.x <= prev.x)
                assertTrue("y monotone at $i", p.y >= prev.y)
                // True staircase: at most one axis moves per step.
                val dx = p.x != prev.x
                val dy = p.y != prev.y
                assertTrue("single-axis step at $i", !(dx && dy))
            }
        }
    }

    @Test
    fun rankLibraryApps_emptyApps_returnsEmpty() {
        assertTrue(
            rankLibraryApps(emptyList(), "calc", emptyMap()).isEmpty(),
        )
    }

    @Test
    fun rankLibraryApps_emptyQuery_returnsEmpty() {
        assertTrue(
            rankLibraryApps(
                listOf(LibraryApp("k", "Calculator", "com.calc")),
                "",
                emptyMap(),
            ).isEmpty(),
        )
    }

    @Test
    fun rankLibraryApps_prefixMatch_found() {
        val found = rankLibraryApps(
            listOf(LibraryApp("k", "Calculator", "com.calc")),
            "calc",
            emptyMap(),
        )
        assertEquals(1, found.size)
        assertEquals("Calculator", found[0].label)
    }

    @Test
    fun suggestPinCandidate_emptyApps_returnsNull() {
        assertNull(suggestPinCandidate(emptyList(), emptyMap(), emptyList(), 5))
    }

    @Test
    fun resolveAccentSeed_wallpaperWithNoSwatches_returnsNull() {
        // No wallpaper swatches: the wallpaper accent falls back to
        // the default instead of throwing.
        assertNull(
            resolveAccentSeed(AccentSelection(AccentSource.WALLPAPER), null, null),
        )
    }
}
