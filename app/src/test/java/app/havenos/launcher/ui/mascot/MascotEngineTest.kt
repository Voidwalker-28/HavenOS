package app.havenos.launcher.ui.mascot

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MascotEngineTest {

    // State machine ---------------------------------------------------------

    @Test fun spawnPath() {
        assertTrue(PixelMascotState.Hidden.canTransitionTo(PixelMascotState.Spawning))
        assertTrue(PixelMascotState.Spawning.canTransitionTo(PixelMascotState.Idle))
        assertTrue(PixelMascotState.Idle.canTransitionTo(PixelMascotState.Walking))
        assertTrue(PixelMascotState.Walking.canTransitionTo(PixelMascotState.Idle))
    }

    @Test fun hideAlwaysPoofsFirst() {
        assertFalse(PixelMascotState.Idle.canTransitionTo(PixelMascotState.Hidden))
        assertTrue(PixelMascotState.Idle.canTransitionTo(PixelMascotState.Despawning))
        assertTrue(PixelMascotState.Despawning.canTransitionTo(PixelMascotState.Hidden))
        assertFalse(PixelMascotState.Despawning.canTransitionTo(PixelMascotState.Idle))
    }

    @Test fun jumpFallCycle() {
        assertTrue(PixelMascotState.Walking.canTransitionTo(PixelMascotState.Jumping))
        assertTrue(PixelMascotState.Jumping.canTransitionTo(PixelMascotState.Falling))
        assertTrue(PixelMascotState.Falling.canTransitionTo(PixelMascotState.Idle))
        assertTrue(PixelMascotState.Idle.canTransitionTo(PixelMascotState.Carried))
        assertTrue(PixelMascotState.Carried.canTransitionTo(PixelMascotState.Idle))
        assertTrue(PixelMascotState.Idle.canTransitionTo(PixelMascotState.Pointing))
        assertTrue(PixelMascotState.Pointing.canTransitionTo(PixelMascotState.Idle))
    }

    @Test fun illegalTransitionsRejected() {
        assertFalse(PixelMascotState.Hidden.canTransitionTo(PixelMascotState.Idle))
        assertFalse(PixelMascotState.Sleeping.canTransitionTo(PixelMascotState.Walking))
        assertFalse(PixelMascotState.Carried.canTransitionTo(PixelMascotState.Walking))
        assertFalse(PixelMascotState.Sitting.canTransitionTo(PixelMascotState.Walking))
    }

    // Ledge graph -----------------------------------------------------------

    @Test fun clearanceCutsBlockedParts() {
        // Row-top segment at y=300, sprite 100 tall: band [200,300].
        val segments = listOf(LedgeSegment(0f, 1000f, 300f, LedgeKind.RowTop))
        // Obstacle overlapping the band over x 100..200 only.
        val obstacles = listOf(FloatRect(100f, 150f, 200f, 250f))
        val ledges = buildLedges(segments, obstacles, 100f, 50f)
        assertEquals(2, ledges.size)
        assertEquals(0f, ledges[0].left)
        assertEquals(100f, ledges[0].right)
        assertEquals(200f, ledges[1].left)
        assertEquals(1000f, ledges[1].right)
    }

    @Test fun exactFitBandDoesNotBlock() {
        // Obstacle bottom exactly at band top: no overlap, no cut.
        val segments = listOf(LedgeSegment(0f, 1000f, 300f, LedgeKind.RowTop))
        val obstacles = listOf(FloatRect(100f, 100f, 200f, 200f))
        val ledges = buildLedges(segments, obstacles, 100f, 50f)
        assertEquals(1, ledges.size)
        assertEquals(0f, ledges[0].left)
        assertEquals(1000f, ledges[0].right)
    }

    @Test fun narrowPartsDroppedAndEmptyLayoutHasNoLedge() {
        // Segment narrower than the sprite: dropped.
        val narrow = buildLedges(
            listOf(LedgeSegment(0f, 40f, 300f, LedgeKind.RowTop)),
            emptyList(), 100f, 50f,
        )
        assertTrue(narrow.isEmpty())
        // Fully blocked band: no ledge at all.
        val blocked = buildLedges(
            listOf(LedgeSegment(0f, 100f, 300f, LedgeKind.RowTop)),
            listOf(FloatRect(0f, 150f, 100f, 250f)), 100f, 50f,
        )
        assertTrue(blocked.isEmpty())
    }

    @Test fun widgetTopAndDockSegmentsSurvive() {
        val segments = listOf(
            LedgeSegment(0f, 400f, 400f, LedgeKind.WidgetTop),
            LedgeSegment(0f, 1000f, 900f, LedgeKind.DockTop),
        )
        val ledges = buildLedges(segments, emptyList(), 100f, 50f)
        assertEquals(2, ledges.size)
        assertEquals(LedgeKind.WidgetTop, ledges[0].kind)
        assertEquals(LedgeKind.DockTop, ledges[1].kind)
    }

    // Segment derivation ------------------------------------------------------

    @Test fun segmentsFromRowTopsWithEmptyRows() {
        val rects = listOf(
            FloatRect(0f, 100f, 100f, 200f),
            FloatRect(200f, 100f, 300f, 200f),
            FloatRect(0f, 300f, 100f, 400f),
        )
        val segs = buildSegments(rects, 1000f, 1000f, 50f, null, null)
        // Two row tops; the empty row above would sit at y = -100, so it
        // is dropped, while the empty row below (y = 500) is kept.
        assertTrue(segs.any { it.y == 100f && it.kind == LedgeKind.RowTop })
        assertTrue(segs.any { it.y == 300f && it.kind == LedgeKind.RowTop })
        assertTrue(segs.any { it.y == 500f && it.kind == LedgeKind.EmptyCell })
        assertFalse(segs.any { it.kind == LedgeKind.EmptyCell && it.y < 0f })
    }

    @Test fun segmentsIncludeDockAndRail() {
        val segs = buildSegments(
            listOf(FloatRect(0f, 100f, 100f, 200f)),
            1000f, 1000f, 50f, 900f,
            FloatRect(700f, 0f, 1000f, 1000f),
        )
        assertTrue(segs.any { it.y == 900f && it.kind == LedgeKind.DockTop })
        assertTrue(segs.any { it.y == 0f && it.kind == LedgeKind.RailTop })
    }

    @Test fun nearestLedgePointClamps() {
        val ledges = listOf(Ledge(0, 100f, 200f, 300f, LedgeKind.RowTop))
        val p = nearestLedgePoint(ledges, 500f, 290f)
        assertNotNull(p)
        assertEquals(200f, p.x)
        assertEquals(300f, p.y)
        assertNull(nearestLedgePoint(emptyList(), 0f, 0f))
    }

    // Routing ----------------------------------------------------------------

    private fun twoLedges(): List<Ledge> = listOf(
        Ledge(0, 0f, 500f, 500f, LedgeKind.RowTop),
        Ledge(1, 100f, 600f, 300f, LedgeKind.RowTop),
    )

    @Test fun sameLedgeRouteIsWalk() {
        val ledges = twoLedges()
        val route = planRoute(
            LedgePoint(0, 100f, 500f), LedgePoint(0, 400f, 500f),
            ledges, 200f, 50f,
        )
        assertEquals(listOf(RouteStep.WalkTo(400f)), route)
    }

    @Test fun jumpRouteUp() {
        val ledges = twoLedges()
        val route = planRoute(
            LedgePoint(0, 150f, 500f), LedgePoint(1, 200f, 300f),
            ledges, 250f, 50f,
        )
        assertNotNull(route)
        assertTrue(route.any { it is RouteStep.JumpTo && it.ledgeId == 1 })
    }

    @Test fun dropRouteDown() {
        val ledges = twoLedges()
        val route = planRoute(
            LedgePoint(1, 200f, 300f), LedgePoint(0, 200f, 500f),
            ledges, 250f, 50f,
        )
        assertNotNull(route)
        assertTrue(route.any { it is RouteStep.DropTo && it.ledgeId == 0 })
    }

    @Test fun noRouteWhenRiseTooHigh() {
        val ledges = listOf(
            Ledge(0, 0f, 500f, 500f, LedgeKind.RowTop),
            Ledge(1, 100f, 600f, 100f, LedgeKind.RowTop),
        )
        val route = planRoute(
            LedgePoint(0, 150f, 500f), LedgePoint(1, 200f, 100f),
            ledges, 200f, 50f,
        )
        assertNull(route)
    }

    @Test fun snapDropValidAndInvalid() {
        val ledges = twoLedges()
        val hit = snapDropToLedge(ledges, 200f, 505f, 48f)
        assertNotNull(hit)
        assertEquals(0, hit.ledgeId)
        assertNull(snapDropToLedge(ledges, 900f, 505f, 48f))
        assertNull(snapDropToLedge(ledges, 200f, 600f, 48f))
    }

    // Planner ---------------------------------------------------------------

    @Test fun plannerDeterministicWithSeed() {
        val ledges = listOf(
            Ledge(0, 0f, 500f, 500f, LedgeKind.RowTop),
            Ledge(1, 0f, 500f, 900f, LedgeKind.DockTop),
        )
        val a = pickTargetLedgePoint(ledges, Random(42))
        val b = pickTargetLedgePoint(ledges, Random(42))
        assertEquals(a, b)
        assertNotNull(a)
        assertTrue(a!!.x in 0f..500f)
    }

    @Test fun dwellSitSleepRanges() {
        val r = Random(7)
        repeat(20) {
            val dwell = dwellMs(r)
            assertTrue(dwell in 3000L..12000L)
        }
        repeat(20) {
            val sit = sitDelayMs(r)
            assertTrue(sit in 60_000L..120_000L)
        }
        assertFalse(shouldSleep(299_999L))
        assertTrue(shouldSleep(300_000L))
        val speed = tripSpeedDpPerSec(Random(3))
        assertTrue(speed in 40f..60f)
    }

    // Invalidation ------------------------------------------------------------

    private fun baseInputs() = GraphInputs(
        layoutHash = 1, page = 0, sizeClass = 2, dockSide = 0,
        densityBucket = 2, keyboardVisible = false, settingsOpen = false,
        editMode = false, iconSizeDp = 56f, gridCols = 6, gridRows = 4,
    )

    @Test fun invalidationTriggers() {
        val base = baseInputs()
        assertFalse(graphInputsChanged(base, base))
        assertTrue(graphInputsChanged(null, base))
        assertTrue(graphInputsChanged(base, base.copy(page = 1)))
        assertTrue(graphInputsChanged(base, base.copy(layoutHash = 2)))
        assertTrue(graphInputsChanged(base, base.copy(settingsOpen = true)))
        assertTrue(graphInputsChanged(base, base.copy(keyboardVisible = true)))
        assertTrue(graphInputsChanged(base, base.copy(editMode = true)))
        assertTrue(graphInputsChanged(base, base.copy(iconSizeDp = 64f)))
        assertTrue(graphInputsChanged(base, base.copy(dockSide = 1)))
    }

    // Find flow ----------------------------------------------------------------

    @Test fun findHappyPath() {
        var s = FindFlowState.Idle
        s = nextFindFlowState(s, FindFlowEvent.Start)
        assertEquals(FindFlowState.Asking, s)
        s = nextFindFlowState(s, FindFlowEvent.QuerySubmitted)
        assertEquals(FindFlowState.Searching, s)
        s = nextFindFlowState(s, FindFlowEvent.Results(FindDecision.NoMatch))
        assertEquals(FindFlowState.Done, s)
        s = nextFindFlowState(s, FindFlowEvent.Start)
        assertEquals(FindFlowState.Asking, s)
    }

    @Test fun findChoicesThenPointing() {
        var s = FindFlowState.Searching
        s = nextFindFlowState(s, FindFlowEvent.Done)
        assertEquals(FindFlowState.Done, s)
        s = FindFlowState.Choosing
        s = nextFindFlowState(s, FindFlowEvent.Done)
        assertEquals(FindFlowState.Done, s)
        s = FindFlowState.Navigating
        s = nextFindFlowState(s, FindFlowEvent.Done)
        assertEquals(FindFlowState.Done, s)
        s = FindFlowState.Searching
        s = nextFindFlowState(s, FindFlowEvent.Results(FindDecision.Choices(emptyList())))
        assertEquals(FindFlowState.Choosing, s)
        s = nextFindFlowState(s, FindFlowEvent.CandidatePicked)
        assertEquals(FindFlowState.Navigating, s)
        s = nextFindFlowState(s, FindFlowEvent.Arrived)
        assertEquals(FindFlowState.Pointing, s)
        s = nextFindFlowState(s, FindFlowEvent.Done)
        assertEquals(FindFlowState.Done, s)
    }

    @Test fun findCancelWins() {
        assertEquals(
            FindFlowState.Cancelled,
            nextFindFlowState(FindFlowState.Searching, FindFlowEvent.Cancel),
        )
        assertEquals(
            FindFlowState.Cancelled,
            nextFindFlowState(FindFlowState.Navigating, FindFlowEvent.Timeout),
        )
        assertEquals(
            FindFlowState.Cancelled,
            nextFindFlowState(FindFlowState.Pointing, FindFlowEvent.ModeChanged),
        )
        assertEquals(20_000L, FIND_PROGRESS_TIMEOUT_MS)
    }

    // Find target resolution (each kind + no-match + uncertain) ---------------

    @Test fun findResolutionKinds() {
        val apps = listOf(
            app.havenos.launcher.ui.LibraryApp("k1", "Alpha", "pkg.a"),
            app.havenos.launcher.ui.LibraryApp("k2", "Beta", "pkg.b"),
        )
        val ranked = rankFind(apps, "alpha", emptyMap())
        assertEquals(1, ranked.size)
        assertTrue(ranked[0].target is FindTarget.App)
        assertEquals(FindBand.EXACT, ranked[0].band)
        // Uncertain: two apps sharing a prefix band -> Choices.
        val both = rankFind(apps, "a", emptyMap())
        assertTrue(both.isNotEmpty())
        // No match at all.
        assertTrue(rankFind(apps, "zzznope", emptyMap()).isEmpty())
        assertEquals(
            FindDecision.NoMatch,
            decideFind(rankFind(apps, "zzznope", emptyMap())),
        )
        // Settings + action targets resolve through the same ranking.
        val setting = rankFind(emptyList(), "accent", emptyMap())
        assertTrue(setting.any { it.target is FindTarget.Setting })
        val action = rankFind(emptyList(), "add widget", emptyMap())
        assertTrue(action.any { it.target is FindTarget.Action })
    }
}
