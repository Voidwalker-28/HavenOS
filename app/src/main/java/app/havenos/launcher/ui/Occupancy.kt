package app.havenos.launcher.ui

import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.SettingsState

/**
 * M10 — One occupancy-aware layout engine (Phase 1 core).
 *
 * The audit hypothesized "multiple layout authorities / absolute
 * positioning without occupancy / no collision resolver". Verified in
 * code (2026-10-24): the paged grid DID have occupancy helpers
 * ([findSpot], [findSpotNear], [resolveDrop], [resolveDropEdit]), but
 * three gaps made grid settings lie:
 *  1. The host computed grid dims with [SettingsState.gridCells] while
 *     the pager rendered [pagedGridCells] — different formulas, so on
 *     phones (Compact 5x6 fixed) and at high density the host placed
 *     items on a grid the pager never rendered (BUG-030).
 *  2. A grid shrink promised "N icons will move" but nothing moved
 *     them: the commit only wrote the override, leaving stored cells
 *     out of bounds (BUG-031).
 *  3. Nothing ever validated a stored layout, so a stale layout
 *     (vc5 flow era, a failed migration, a corrupt write) could keep
 *     two items on one cell forever (BUG-005/006/009).
 *
 * This file closes all three WITHOUT forking the existing helpers:
 * [hostGridCells] is the one grid formula (it delegates to
 * [pagedGridCells]); [reflowForGrid] is the one repair/reflow used by
 * the host effect on every layout change; [validateLayout] is the
 * audit's "no two interactive surfaces share a cell" test, runnable
 * against any stored layout. All coordinates here are HALF-CELL UNITS
 * (1 cell = 2 units, DB v2), matching [rectOf] and the existing
 * helpers — never cells, never pixels.
 */

// ---------------------------------------------------------------------------
// Validation: the audit's acceptance test as a function
// ---------------------------------------------------------------------------

/** One pair of items sharing at least one cell on the same page. */
data class Overlap(val first: HomeItem, val second: HomeItem)

data class LayoutValidation(
    val overlaps: List<Overlap>,
    val outOfBounds: List<HomeItem>,
) {
    val isValid: Boolean get() = overlaps.isEmpty() && outOfBounds.isEmpty()
}

/** True when [item] lies fully inside a [colsU] x [rowsU] grid (units). */
fun isInBounds(item: HomeItem, colsU: Int, rowsU: Int): Boolean =
    item.col >= 0 && item.row >= 0 &&
        item.col + item.colSpan <= colsU && item.row + item.rowSpan <= rowsU

/**
 * Every same-page overlap (each pair once) plus every out-of-bounds
 * item. Pages below 0 are read as page 0, matching the pager's rule.
 */
fun validateLayout(items: List<HomeItem>, colsU: Int, rowsU: Int): LayoutValidation {
    val outOfBounds = items.filterNot { isInBounds(it, colsU, rowsU) }
    val overlaps = mutableListOf<Overlap>()
    val byPage = items.groupBy { it.page.coerceAtLeast(0) }
    for ((_, pageItems) in byPage) {
        for (i in pageItems.indices) {
            for (j in i + 1 until pageItems.size) {
                val a = pageItems[i]
                val b = pageItems[j]
                if (rectOf(a).overlaps(rectOf(b))) overlaps.add(Overlap(a, b))
            }
        }
    }
    return LayoutValidation(overlaps, outOfBounds)
}

// ---------------------------------------------------------------------------
// Placement: nearest free spot, resize preview
// ---------------------------------------------------------------------------

/**
 * Nearest free spot for a w x h rect (units) on [page] near
 * ([targetCol], [targetRow]), excluding [excludeId]; row-major first
 * fit as fallback. Null when the rect fits nowhere on the page.
 */
fun pushForPlacement(
    items: List<HomeItem>,
    page: Int,
    wUnits: Int,
    hUnits: Int,
    colsU: Int,
    rowsU: Int,
    targetCol: Int,
    targetRow: Int,
    excludeId: String? = null,
): Pair<Int, Int>? {
    val rest = if (excludeId == null) items else items.filterNot { it.id == excludeId }
    return findSpotNear(rest, page, wUnits, hUnits, colsU, rowsU, targetCol, targetRow)
        ?: findSpot(rest, page, wUnits, hUnits, colsU, rowsU)
}

/** What a resize would claim, and whether it fits where the item sits. */
data class ResizePlan(
    /** True when the new size fits at the item's current position. */
    val valid: Boolean,
    /** New size in whole cells (units / 2, rounded up). */
    val cellsWide: Int,
    val cellsHigh: Int,
    /** Nearest free spot for the new size, or null when it fits nowhere. */
    val fallback: Pair<Int, Int>?,
)

fun resizePlacement(
    item: HomeItem,
    newSpanWUnits: Int,
    newSpanHUnits: Int,
    items: List<HomeItem>,
    colsU: Int,
    rowsU: Int,
): ResizePlan {
    val fitsGrid = newSpanWUnits <= colsU && newSpanHUnits <= rowsU &&
        item.col + newSpanWUnits <= colsU && item.row + newSpanHUnits <= rowsU
    val rest = items.filterNot { it.id == item.id }
    val candidate = Rect(item.col, item.row, newSpanWUnits, newSpanHUnits)
    val overlaps = rest.any {
        it.page.coerceAtLeast(0) == item.page.coerceAtLeast(0) && rectOf(it).overlaps(candidate)
    }
    val valid = fitsGrid && !overlaps
    val fallback = if (valid) null else pushForPlacement(
        items, item.page.coerceAtLeast(0), newSpanWUnits, newSpanHUnits,
        colsU, rowsU, item.col, item.row, excludeId = item.id,
    )
    return ResizePlan(
        valid = valid,
        cellsWide = (newSpanWUnits + 1) / 2,
        cellsHigh = (newSpanHUnits + 1) / 2,
        fallback = fallback,
    )
}

// ---------------------------------------------------------------------------
// Reflow: the one repair used after every layout change
// ---------------------------------------------------------------------------

/** One item whose placement changed under a reflow. */
data class ReflowMove(
    val itemId: String,
    val fromPage: Int,
    val toPage: Int,
    val fromCol: Int,
    val fromRow: Int,
    val toCol: Int,
    val toRow: Int,
    /** True when the item's span was clamped to fit the grid. */
    val shrunk: Boolean,
)

data class ReflowResult(val items: List<HomeItem>, val moved: List<ReflowMove>)

/**
 * Reflow every item into a [colsU] x [rowsU] grid (units).
 *
 * Rules, in order — an item that already fits and overlaps nothing
 * keeps its EXACT position (identity + position preserved, so a
 * reflow never disturbs a valid layout); anything else moves to the
 * nearest free spot on the same page, then to later pages in order,
 * then to a fresh page. CLOCK items never change page: their span is
 * clamped to the grid instead (the clock page is a single full-width
 * item by construction). Widgets whose span exceeds the grid shrink
 * to the grid before placement is attempted.
 *
 * Idempotent: reflowing an already-valid layout returns it unchanged
 * (empty [ReflowResult.moved]), so the host can run this after every
 * layout write as a safety net without fighting user edits.
 *
 * [visiblePages], when given, is the set of pages the pager actually
 * renders (hidden pages removed). A displaced item never lands on a
 * hidden page — that would make it vanish until unhide. Items that
 * already sit on a hidden page and fit stay there (hiding a page
 * hides its items by design; unhide restores them exactly).
 */
fun reflowForGrid(
    items: List<HomeItem>,
    colsU: Int,
    rowsU: Int,
    visiblePages: List<Int>? = null,
): ReflowResult {
    if (items.isEmpty()) return ReflowResult(items, emptyList())
    val kept = mutableListOf<HomeItem>()
    val moved = mutableListOf<ReflowMove>()
    val pages = items.map { it.page.coerceAtLeast(0) }.distinct().sorted()
    val targets = visiblePages?.distinct()?.sorted()?.ifEmpty { null } ?: pages
    var nextPage = maxOf(pages.maxOrNull() ?: 0, targets.maxOrNull() ?: 0) + 1
    // Deterministic order: page by page, top-to-bottom, left-to-right,
    // so the first item on a contested cell always wins its position.
    val ordered = items.sortedWith(
        compareBy({ it.page.coerceAtLeast(0) }, { it.row }, { it.col }),
    )
    for (item in ordered) {
        val page = item.page.coerceAtLeast(0)
        var spanW = item.colSpan.coerceAtLeast(2)
        var spanH = item.rowSpan.coerceAtLeast(2)
        var shrunk = false
        if (spanW > colsU) {
            spanW = colsU
            shrunk = true
        }
        if (spanH > rowsU) {
            spanH = rowsU
            shrunk = true
        }
        val clampedCol = item.col.coerceIn(0, (colsU - spanW).coerceAtLeast(0))
        val clampedRow = item.row.coerceIn(0, (rowsU - spanH).coerceAtLeast(0))
        val candidate = Rect(clampedCol, clampedRow, spanW, spanH)
        val overlapsKept = kept.any {
            it.page.coerceAtLeast(0) == page && rectOf(it).overlaps(candidate)
        }
        val fitsAsIs = clampedCol == item.col && clampedRow == item.row &&
            spanW == item.colSpan && spanH == item.rowSpan && !overlapsKept
        if (fitsAsIs) {
            kept.add(item)
            continue
        }
        if (item.kind == ItemKind.CLOCK) {
            // Clock never changes page: clamp in place even if the page
            // is contested (the clock page holds only the clock).
            val placed = item.copy(
                page = page, col = clampedCol, row = clampedRow,
                colSpan = spanW, rowSpan = spanH,
            )
            kept.add(placed)
            // Record only an ACTUAL change: recording an unchanged
            // item as "moved" would make the host reflow effect write
            // on every run forever (see the fallback below).
            if (placed != item) {
                moved.add(
                    ReflowMove(
                        itemId = item.id, fromPage = item.page, toPage = page,
                        fromCol = item.col, fromRow = item.row,
                        toCol = clampedCol, toRow = clampedRow, shrunk = shrunk,
                    ),
                )
            }
            continue
        }
        // Same page first (nearest to where the item was) — but never
        // within a hidden page, where the item would stay invisible.
        // Then later visible pages in order, then any visible page
        // with room, then a fresh page.
        val ownPageVisible = visiblePages == null || page in targets
        val samePage = if (ownPageVisible) {
            findSpotNear(kept, page, spanW, spanH, colsU, rowsU, item.col, item.row)
        } else {
            null
        }
        val placedSpot: Pair<Int, Int>?
        val placedPage: Int
        if (samePage != null) {
            placedSpot = samePage
            placedPage = page
        } else {
            var found: Pair<Int, Int>? = null
            var foundPage = page
            for (p in targets) {
                if (p <= page) continue
                found = findSpot(kept, p, spanW, spanH, colsU, rowsU)
                if (found != null) {
                    foundPage = p
                    break
                }
            }
            if (found == null) {
                for (p in targets) {
                    found = findSpot(kept, p, spanW, spanH, colsU, rowsU)
                    if (found != null) {
                        foundPage = p
                        break
                    }
                }
            }
            if (found == null) {
                found = findSpot(kept, nextPage, spanW, spanH, colsU, rowsU)
                foundPage = nextPage
                nextPage++
            }
            placedSpot = found
            placedPage = foundPage
        }
        if (placedSpot == null) {
            // No page has room (grids full): keep the clamped position
            // rather than dropping the item — validation will still
            // flag the overlap instead of silently losing an icon.
            // Only record a move when something actually changed, or
            // the host effect would rewrite on every run forever.
            val placed = item.copy(
                page = page, col = clampedCol, row = clampedRow,
                colSpan = spanW, rowSpan = spanH,
            )
            kept.add(placed)
            if (placed != item) {
                moved.add(
                    ReflowMove(
                        itemId = item.id, fromPage = item.page, toPage = page,
                        fromCol = item.col, fromRow = item.row,
                        toCol = clampedCol, toRow = clampedRow, shrunk = shrunk,
                    ),
                )
            }
        } else {
            val placed = item.copy(
                page = placedPage, col = placedSpot.first, row = placedSpot.second,
                colSpan = spanW, rowSpan = spanH,
            )
            kept.add(placed)
            moved.add(
                ReflowMove(
                    itemId = item.id, fromPage = item.page, toPage = placedPage,
                    fromCol = item.col, fromRow = item.row,
                    toCol = placedSpot.first, toRow = placedSpot.second,
                    shrunk = shrunk,
                ),
            )
        }
    }
    // Preserve the caller's item order (reflow must not reorder lists).
    val byId = kept.associateBy { it.id }
    return ReflowResult(items.map { byId[it.id] ?: it }, moved)
}

// ---------------------------------------------------------------------------
// Grid-change plan: what the picker preview promises
// ---------------------------------------------------------------------------

/** One item the new grid displaces, and the grid page it lands on. */
data class GridChangeMove(val itemId: String, val destPage: Int)

data class GridChangePlan(
    val movedCount: Int,
    val moves: List<GridChangeMove>,
    /** Destination grid page -> number of icons landing there. */
    val destinations: Map<Int, Int>,
)

/**
 * The full impact of changing the grid to [newColsCells] x
 * [newRowsCells] (cells): the same [reflowForGrid] the host applies
 * on commit, run as a preview. [movedCount] is what the picker shows;
 * [destinations] is the per-page breakdown ("page 2: 2 icons").
 */
fun gridChangePlan(
    items: List<HomeItem>,
    newColsCells: Int,
    newRowsCells: Int,
    visiblePages: List<Int>? = null,
): GridChangePlan {
    val result = reflowForGrid(items, newColsCells * 2, newRowsCells * 2, visiblePages)
    // Display only relocations: an item whose span merely clamps in
    // place (the full-width clock on a shrunk grid) has not "moved".
    val moves = result.moved
        .filter {
            it.toPage != it.fromPage || it.toCol != it.fromCol || it.toRow != it.fromRow
        }
        .map { GridChangeMove(it.itemId, it.toPage) }
    return GridChangePlan(
        movedCount = moves.size,
        moves = moves,
        destinations = moves.groupingBy { it.destPage }.eachCount(),
    )
}

// ---------------------------------------------------------------------------
// Host grid dims: the ONE formula (the pager renders pagedGridCells)
// ---------------------------------------------------------------------------

/**
 * Grid dims for host-side math (migration, resize bounds, dialog
 * geometry, reflow). This delegates to [pagedGridCells] — the exact
 * formula the pager renders — with the workspace size estimated from
 * the screen and the shell chrome. Compact/Medium grids are fixed, so
 * the estimate only matters on Expanded, where the workspace is the
 * full width minus 32dp of margins, or ~70% of it when the Today rail
 * is visible (rail ~30% + margins). A small estimate error can flip
 * a fit-boundary col on Expanded; the reflow safety net repairs any
 * item that lands outside the rendered grid, so the estimate can
 * never strand an icon off-grid.
 */
fun hostGridCells(
    state: SettingsState,
    landscape: Boolean,
    sizeClass: HomeSizeClass,
    screenWidthDp: Float,
    screenHeightDp: Float,
    railVisible: Boolean,
): Pair<Int, Int> {
    val contentW = if (sizeClass == HomeSizeClass.Expanded && landscape && railVisible) {
        screenWidthDp * 0.70f - 32f
    } else {
        screenWidthDp - 32f
    }
    // Status bar + nav bar + dock + toolbar + indicator, estimated.
    val contentH = screenHeightDp - 220f
    return pagedGridCells(state, landscape, sizeClass, contentW, contentH, state.iconDp())
}
