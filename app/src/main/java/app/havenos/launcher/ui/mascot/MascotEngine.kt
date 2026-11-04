package app.havenos.launcher.ui.mascot

import kotlin.math.abs
import kotlin.random.Random

/**
 * HavenOS 3.1 Phase E (E2/E3) — pixel mascot behavior engine, pure logic.
 *
 * No Compose, no Android framework: every rule here is unit-testable
 * on the JVM. The layer (MascotLayer.kt) only renders what this file
 * decides and owns the timers/coroutines around it.
 *
 * Coordinates are pixels in the layer's local space. A ledge's [y] is
 * where the sprite's feet rest; the sprite occupies [y - height, y].
 */

// ---------------------------------------------------------------------------
// State machine (E2): Hidden, Spawning, Idle, Walking, Jumping, Falling,
// Sitting, Sleeping, Reacting (tap), Carried (long-press drag),
// Pointing, Despawning. Hide always plays poof first (Despawning),
// never jumps straight to Hidden from a visible state.
// ---------------------------------------------------------------------------

enum class PixelMascotState {
    Hidden, Spawning, Idle, Walking, Jumping, Falling,
    Sitting, Sleeping, Reacting, Carried, Pointing, Despawning,
}

fun PixelMascotState.canTransitionTo(next: PixelMascotState): Boolean {
    if (this == next) return true
    return when (this) {
        PixelMascotState.Hidden -> next == PixelMascotState.Spawning
        PixelMascotState.Spawning -> next == PixelMascotState.Idle ||
            next == PixelMascotState.Despawning
        PixelMascotState.Idle -> next in setOf(
            PixelMascotState.Walking, PixelMascotState.Jumping,
            PixelMascotState.Sitting, PixelMascotState.Sleeping,
            PixelMascotState.Reacting, PixelMascotState.Carried,
            PixelMascotState.Pointing, PixelMascotState.Despawning,
        )
        PixelMascotState.Walking -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Jumping,
            PixelMascotState.Falling, PixelMascotState.Reacting,
            PixelMascotState.Carried, PixelMascotState.Despawning,
        )
        PixelMascotState.Jumping -> next in setOf(
            PixelMascotState.Falling, PixelMascotState.Idle,
            PixelMascotState.Walking, PixelMascotState.Reacting,
            PixelMascotState.Carried, PixelMascotState.Despawning,
        )
        PixelMascotState.Falling -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Walking,
            PixelMascotState.Reacting, PixelMascotState.Carried,
            PixelMascotState.Despawning,
        )
        PixelMascotState.Sitting -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Sleeping,
            PixelMascotState.Reacting, PixelMascotState.Carried,
            PixelMascotState.Pointing, PixelMascotState.Despawning,
        )
        PixelMascotState.Sleeping -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Reacting,
            PixelMascotState.Despawning,
        )
        PixelMascotState.Reacting -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Sitting,
            PixelMascotState.Sleeping, PixelMascotState.Pointing,
            PixelMascotState.Despawning,
        )
        PixelMascotState.Carried -> next == PixelMascotState.Idle ||
            next == PixelMascotState.Despawning
        PixelMascotState.Pointing -> next in setOf(
            PixelMascotState.Idle, PixelMascotState.Reacting,
            PixelMascotState.Despawning,
        )
        PixelMascotState.Despawning -> next == PixelMascotState.Hidden
    }
}

// ---------------------------------------------------------------------------
// Ledge graph (E2). A segment is a candidate top edge; [buildLedges]
// cuts it wherever the clearance band above it (one sprite tall)
// intersects an obstacle, and drops parts narrower than the sprite.
// ---------------------------------------------------------------------------

data class FloatRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersectsVertical(topY: Float, bottomY: Float): Boolean =
        top < bottomY && bottom > topY
}

enum class LedgeKind { DockTop, WidgetTop, RowTop, EmptyCell, RailTop, PageBottom }

data class LedgeSegment(
    val left: Float,
    val right: Float,
    val y: Float,
    val kind: LedgeKind,
)

data class Ledge(
    val id: Int,
    val left: Float,
    val right: Float,
    val y: Float,
    val kind: LedgeKind,
)

fun buildLedges(
    segments: List<LedgeSegment>,
    obstacles: List<FloatRect>,
    spriteHeightPx: Float,
    spriteWidthPx: Float,
): List<Ledge> {
    val out = mutableListOf<Ledge>()
    var nextId = 0
    for (seg in segments) {
        if (seg.right - seg.left < spriteWidthPx) continue
        val bandTop = seg.y - spriteHeightPx
        // Blocked x-intervals: obstacles overlapping the clearance band.
        val blocked = obstacles
            .filter { it.intersectsVertical(bandTop, seg.y) }
            .mapNotNull { ob ->
                val l = maxOf(seg.left, ob.left)
                val r = minOf(seg.right, ob.right)
                if (r > l) l to r else null
            }
            .sortedBy { it.first }
        // Subtract blocked intervals from the segment range.
        var cursor = seg.left
        val parts = mutableListOf<Pair<Float, Float>>()
        for ((bl, br) in blocked) {
            if (bl > cursor) parts.add(cursor to bl)
            if (br > cursor) cursor = br
        }
        if (seg.right > cursor) parts.add(cursor to seg.right)
        for ((l, r) in parts) {
            if (r - l >= spriteWidthPx) {
                out.add(Ledge(nextId++, l, r, seg.y, seg.kind))
            }
        }
    }
    return out
}

/**
 * Candidate segments from live rects (pure). Row tops span the full
 * layer width — [buildLedges] cuts them at obstacles, which is what
 * yields perches in empty cells and gutters without grid geometry.
 * Empty-row segments above/below the row band use the row pitch.
 * The dock top and rail top are added when the host knows them.
 */
fun buildSegments(
    itemRects: List<FloatRect>,
    layerWidth: Float,
    layerHeight: Float,
    spriteHeightPx: Float,
    dockTopY: Float?,
    railRect: FloatRect?,
): List<LedgeSegment> {
    val segments = mutableListOf<LedgeSegment>()
    val tops = itemRects.map { it.top }.sorted()
    val groups = mutableListOf<Float>()
    for (top in tops) {
        if (groups.isEmpty() || top - groups.last() > 1f) groups.add(top)
    }
    for (top in groups) {
        segments.add(LedgeSegment(0f, layerWidth, top, LedgeKind.RowTop))
    }
    if (groups.size >= 2) {
        val diffs = groups.zipWithNext { a, b -> b - a }.sorted()
        val pitch = diffs[diffs.size / 2]
        if (pitch > 0f) {
            val above = groups.first() - pitch
            if (above >= spriteHeightPx) {
                segments.add(LedgeSegment(0f, layerWidth, above, LedgeKind.EmptyCell))
            }
            val below = groups.last() + pitch
            if (below <= layerHeight) {
                segments.add(LedgeSegment(0f, layerWidth, below, LedgeKind.EmptyCell))
            }
        }
    }
    if (dockTopY != null) {
        segments.add(LedgeSegment(0f, layerWidth, dockTopY, LedgeKind.DockTop))
    }
    if (railRect != null) {
        segments.add(
            LedgeSegment(railRect.left, railRect.right, railRect.top, LedgeKind.RailTop),
        )
    }
    return segments
}

/** A point on a ledge: where the sprite's feet rest. */
data class LedgePoint(val ledgeId: Int, val x: Float, val y: Float)

/** Nearest point on any ledge to (x, y), or null when no ledges. */
fun nearestLedgePoint(ledges: List<Ledge>, x: Float, y: Float): LedgePoint? {
    var best: LedgePoint? = null
    var bestDist = Float.MAX_VALUE
    for (ledge in ledges) {
        val px = x.coerceIn(ledge.left, ledge.right)
        val dx = px - x
        val dy = ledge.y - y
        val dist = dx * dx + dy * dy
        if (dist < bestDist) {
            bestDist = dist
            best = LedgePoint(ledge.id, px, ledge.y)
        }
    }
    return best
}

/** Nearest valid ledge below (x, y), or null. Drop keeps x. */
fun dropTarget(ledges: List<Ledge>, x: Float, y: Float): Ledge? {
    return ledges
        .filter { it.y > y && x >= it.left && x <= it.right }
        .minByOrNull { it.y }
}

/** Ledges reachable by a jump from [from]: rise within [maxRisePx]
 * and horizontal ranges overlapping or within [maxGapPx]. */
fun jumpTargets(
    from: Ledge,
    ledges: List<Ledge>,
    maxRisePx: Float,
    maxGapPx: Float,
): List<Ledge> {
    return ledges.filter { to ->
        to.id != from.id && to.y < from.y && (from.y - to.y) <= maxRisePx &&
            (rangesOverlapOrNear(from.left, from.right, to.left, to.right, maxGapPx))
    }
}

private fun rangesOverlapOrNear(
    a0: Float, a1: Float, b0: Float, b1: Float, maxGap: Float,
): Boolean {
    if (a1 >= b0 && b1 >= a0) return true
    val gap = if (a1 < b0) b0 - a1 else a0 - b1
    return gap <= maxGap
}

sealed class RouteStep {
    data class WalkTo(val x: Float) : RouteStep()
    data class JumpTo(val ledgeId: Int, val x: Float) : RouteStep()
    data class DropTo(val ledgeId: Int, val x: Float) : RouteStep()
}

/**
 * Route from [from] to [to] over the ledge graph: walk along ledges,
 * jump up (<= maxRisePx, ranges overlap or within maxGapPx), drop down
 * (any lower ledge containing the drop x). Breadth-first over ledges,
 * depth capped at 2 hops; null when no route exists.
 */
fun planRoute(
    from: LedgePoint,
    to: LedgePoint,
    ledges: List<Ledge>,
    maxRisePx: Float,
    maxGapPx: Float,
): List<RouteStep>? {
    val fromLedge = ledges.firstOrNull { it.id == from.ledgeId } ?: return null
    val toLedge = ledges.firstOrNull { it.id == to.ledgeId } ?: return null
    if (fromLedge.id == toLedge.id) {
        return if (from.x == to.x) emptyList() else listOf(RouteStep.WalkTo(to.x))
    }
    // BFS over ledges: jump edges up, drop edges down (ranges overlap).
    val prev = mutableMapOf<Int, Int>()
    val queue = ArrayDeque<Int>()
    queue.add(fromLedge.id)
    prev[fromLedge.id] = -1
    var found = false
    while (queue.isNotEmpty() && !found) {
        // vc14 fix: the queue head MUST be removed once, outside
        // the lookup. The 3.1.1 form
        // (ledges.firstOrNull { it.id == queue.removeFirst() })
        // called removeFirst() once per ledge tested, so a queue
        // head that did not match the first ledge tested threw
        // NoSuchElementException (ArrayDeque is empty) — the vc14
        // crash of 2026-10-31 — and also ate extra queue entries.
        // A queue id with no ledge still fails the route (host
        // stays put) instead of throwing.
        val curId = queue.removeFirstOrNull() ?: return null
        val cur = ledges.firstOrNull { it.id == curId } ?: return null
        val neighbors = mutableListOf<Ledge>()
        neighbors += jumpTargets(cur, ledges, maxRisePx, maxGapPx)
        // Drop: any lower ledge whose range overlaps this ledge's range.
        neighbors += ledges.filter { it.y > cur.y &&
            rangesOverlapOrNear(cur.left, cur.right, it.left, it.right, 0f) }
        for (n in neighbors) {
            if (n.id !in prev) {
                prev[n.id] = cur.id
                if (n.id == toLedge.id) {
                    found = true
                    break
                }
                queue.add(n.id)
            }
        }
    }
    if (toLedge.id !in prev) return null
    // Reconstruct ledge path, then expand to steps.
    val path = mutableListOf<Ledge>()
    var cur = toLedge.id
    while (cur != -1) {
        // 3.1.1: OrNull + return-null — a broken prev link fails
        // the route (host stays put) instead of throwing.
        path.add(0, ledges.firstOrNull { it.id == cur } ?: return null)
        cur = prev[cur] ?: return null
    }
    val steps = mutableListOf<RouteStep>()
    var x = from.x
    for (i in 0 until path.size - 1) {
        val a = path[i]
        val b = path[i + 1]
        if (b.y < a.y) {
            // Jump: walk to the point on a nearest b, then jump.
            val depX = x.coerceIn(a.left, a.right).let { cx ->
                when {
                    cx < b.left -> minOf(b.left, a.right)
                    cx > b.right -> maxOf(b.right, a.left)
                    else -> cx
                }
            }
            if (depX != x) steps.add(RouteStep.WalkTo(depX))
            val landX = depX.coerceIn(b.left, b.right)
            steps.add(RouteStep.JumpTo(b.id, landX))
            x = landX
        } else {
            // Drop: x must land inside b's range.
            val overlapL = maxOf(a.left, b.left)
            val overlapR = minOf(a.right, b.right)
            val depX = x.coerceIn(overlapL, overlapR)
            if (depX != x) steps.add(RouteStep.WalkTo(depX))
            steps.add(RouteStep.DropTo(b.id, depX))
            x = depX
        }
    }
    if (x != to.x) steps.add(RouteStep.WalkTo(to.x))
    return steps
}

/** Snap a drop (sprite bottom at [dropBottomY], center x [dropX]) to
 * the nearest ledge within [tolerancePx] vertically whose range
 * contains dropX; null when no such ledge (invalid drop). */
fun snapDropToLedge(
    ledges: List<Ledge>,
    dropX: Float,
    dropBottomY: Float,
    tolerancePx: Float,
): LedgePoint? {
    return ledges
        .filter { dropX >= it.left && dropX <= it.right &&
            abs(it.y - dropBottomY) <= tolerancePx }
        .minByOrNull { abs(it.y - dropBottomY) }
        ?.let { LedgePoint(it.id, dropX.coerceIn(it.left, it.right), it.y) }
}

// ---------------------------------------------------------------------------
// Planner (E2): weighted target pick, dwell/sit/sleep timing. The
// Random is injected so tests seed it for determinism.
// ---------------------------------------------------------------------------

/** Weighted pick: dock top 3x, empty/free ledges 2x, others 1x. */
fun pickTargetLedgePoint(
    ledges: List<Ledge>,
    random: Random,
): LedgePoint? {
    if (ledges.isEmpty()) return null
    val weights = ledges.map { ledge ->
        when (ledge.kind) {
            LedgeKind.DockTop -> 3f
            LedgeKind.EmptyCell, LedgeKind.PageBottom -> 2f
            else -> 1f
        }
    }
    val total = weights.sum()
    var roll = random.nextFloat() * total
    for (i in ledges.indices) {
        roll -= weights[i]
        if (roll <= 0f) {
            val ledge = ledges[i]
            val x = ledge.left + random.nextFloat() * (ledge.right - ledge.left)
            return LedgePoint(ledge.id, x, ledge.y)
        }
    }
    // 3.1.1: trailing fallback, unreachable when ledges is non-empty
    // (roll < total always returns inside the loop) but never throwing.
    val last = ledges.lastOrNull() ?: return null
    return LedgePoint(last.id, (last.left + last.right) / 2f, last.y)
}

/** Dwell on a perch: 3–12s. */
fun dwellMs(random: Random): Long = 3000L + (random.nextFloat() * 9000f).toLong()

/** Idle before sitting: 60–120s. */
fun sitDelayMs(random: Random): Long = 60_000L + (random.nextFloat() * 60_000f).toLong()

/** Sleep after 5 minutes idle. */
fun shouldSleep(idleMs: Long): Boolean = idleMs >= 300_000L

/** Move speed for one trip: 40–60 dp/s. */
fun tripSpeedDpPerSec(random: Random): Float = 40f + random.nextFloat() * 20f

// ---------------------------------------------------------------------------
// Graph invalidation (E2): any field change rebuilds the graph.
// [layoutHash] is the host layout's content hash; the layer also keys
// on the live cell-rect snapshot, so a hash collision alone cannot
// leave a stale graph past the next rect change.
// ---------------------------------------------------------------------------

data class GraphInputs(
    val layoutHash: Int,
    val page: Int,
    val sizeClass: Int,
    val dockSide: Int,
    val densityBucket: Int,
    val keyboardVisible: Boolean,
    val settingsOpen: Boolean,
    val editMode: Boolean,
    val iconSizeDp: Float,
    val gridCols: Int,
    val gridRows: Int,
)

fun graphInputsChanged(old: GraphInputs?, new: GraphInputs): Boolean = old != new

// ---------------------------------------------------------------------------
// Find flow (E3, event flow 14): Idle, Asking, Searching, Choosing,
// Navigating, Pointing, Done, Cancelled. Cancel wins from any live
// state (touch outside, back, mode change, 20s without progress).
// ---------------------------------------------------------------------------

enum class FindFlowState {
    Idle, Asking, Searching, Choosing, Navigating, Pointing, Done, Cancelled,
}

sealed class FindFlowEvent {
    object Start : FindFlowEvent()
    object QuerySubmitted : FindFlowEvent()
    data class Results(val decision: FindDecision) : FindFlowEvent()
    object CandidatePicked : FindFlowEvent()
    object Arrived : FindFlowEvent()
    object Pointed : FindFlowEvent()
    object Done : FindFlowEvent()
    object Cancel : FindFlowEvent()
    object Timeout : FindFlowEvent()
    object ModeChanged : FindFlowEvent()
}

const val FIND_PROGRESS_TIMEOUT_MS = 20_000L

fun nextFindFlowState(state: FindFlowState, event: FindFlowEvent): FindFlowState {
    if (state == FindFlowState.Done || state == FindFlowState.Cancelled) {
        return if (event == FindFlowEvent.Start) FindFlowState.Asking else state
    }
    if (event == FindFlowEvent.Cancel || event == FindFlowEvent.Timeout ||
        event == FindFlowEvent.ModeChanged
    ) {
        return FindFlowState.Cancelled
    }
    return when (state) {
        FindFlowState.Idle -> if (event == FindFlowEvent.Start) FindFlowState.Asking else state
        FindFlowState.Asking -> if (event == FindFlowEvent.QuerySubmitted) {
            FindFlowState.Searching
        } else state
        FindFlowState.Searching -> when (event) {
            is FindFlowEvent.Results -> when (event.decision) {
                is FindDecision.Act -> FindFlowState.Navigating
                is FindDecision.Choices -> FindFlowState.Choosing
                is FindDecision.NoMatch -> FindFlowState.Done
            }
            FindFlowEvent.Done -> FindFlowState.Done
            else -> state
        }
        FindFlowState.Choosing -> when (event) {
            FindFlowEvent.CandidatePicked -> FindFlowState.Navigating
            FindFlowEvent.Done -> FindFlowState.Done
            else -> state
        }
        FindFlowState.Navigating -> when (event) {
            FindFlowEvent.Arrived -> FindFlowState.Pointing
            FindFlowEvent.Done -> FindFlowState.Done
            else -> state
        }
        FindFlowState.Pointing -> if (event == FindFlowEvent.Done) {
            FindFlowState.Done
        } else state
        else -> state
    }
}
