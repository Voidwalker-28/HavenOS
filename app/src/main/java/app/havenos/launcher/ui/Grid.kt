package app.havenos.launcher.ui

import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.gridCells
import app.havenos.launcher.data.parseMembers
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class Rect(val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int) {
    fun contains(c: Int, r: Int): Boolean =
        c in col until col + colSpan && r in row until row + rowSpan

    fun overlaps(o: Rect): Boolean =
        col < o.col + o.colSpan && o.col < col + colSpan &&
            row < o.row + o.rowSpan && o.row < row + rowSpan
}

fun rectOf(item: HomeItem): Rect = Rect(item.col, item.row, item.colSpan, item.rowSpan)

/** First position on [page] where a w×h rect fits without overlapping; null when none. */
fun findSpot(items: List<HomeItem>, page: Int, w: Int, h: Int, cols: Int, rows: Int): Pair<Int, Int>? {
    if (w > cols || h > rows) return null
    for (r in 0..rows - h) {
        for (c in 0..cols - w) {
            val rect = Rect(c, r, w, h)
            if (items.none { it.page == page && rectOf(it).overlaps(rect) }) {
                return c to r
            }
        }
    }
    return null
}

/** Nearest free position for a w×h rect to (targetCol, targetRow); null when nothing fits. */
fun findSpotNear(items: List<HomeItem>, page: Int, w: Int, h: Int, cols: Int, rows: Int, targetCol: Int, targetRow: Int): Pair<Int, Int>? {
    if (w > cols || h > rows) return null
    var best: Pair<Int, Int>? = null
    var bestDist = Int.MAX_VALUE
    for (r in 0..rows - h) {
        for (c in 0..cols - w) {
            val rect = Rect(c, r, w, h)
            if (items.none { it.page == page && rectOf(it).overlaps(rect) }) {
                val d = (c - targetCol) * (c - targetCol) + (r - targetRow) * (r - targetRow)
                if (d < bestDist) {
                    bestDist = d
                    best = c to r
                }
            }
        }
    }
    return best
}

/** Fraction-space rect (of the page) for overlap tests in free-canvas mode. */
data class FRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun overlaps(o: FRect): Boolean =
        x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h
}

/** An item's effective fraction rect: stored fractions, else derived from grid coords. */
fun fracOf(item: HomeItem, colsU: Int, rowsU: Int): FRect =
    if (item.fw > 0f) {
        FRect(item.fx, item.fy, item.fw, item.fh)
    } else {
        FRect(
            item.col.toFloat() / colsU, item.row.toFloat() / rowsU,
            item.colSpan.toFloat() / colsU, item.rowSpan.toFloat() / rowsU,
        )
    }

/**
 * Build a newly placed w×h (units) item on [page], honouring the placement
 * mode: grid mode scans grid rects; free mode scans in fraction space (so a
 * freely-moved item's stale grid coords can't cause an overlap) and stores
 * both fractions and the mirrored grid coords. Null when the page is full.
 */
fun placedItem(
    items: List<HomeItem>,
    id: String,
    kind: Int,
    page: Int,
    w: Int,
    h: Int,
    payload: String,
    colsU: Int,
    rowsU: Int,
    freeMode: Boolean,
): HomeItem? {
    if (!freeMode) {
        val spot = findSpot(items, page, w, h, colsU, rowsU) ?: return null
        return HomeItem(id, kind, page, spot.first, spot.second, w, h, payload)
    }
    val wf = w.toFloat() / colsU
    val hf = h.toFloat() / rowsU
    for (r in 0..rowsU - h) {
        for (c in 0..colsU - w) {
            val cand = FRect(c.toFloat() / colsU, r.toFloat() / rowsU, wf, hf)
            if (items.none { it.page == page && fracOf(it, colsU, rowsU).overlaps(cand) }) {
                return HomeItem(
                    id, kind, page, c, r, w, h, payload,
                    fx = cand.x, fy = cand.y, fw = wf, fh = hf,
                )
            }
        }
    }
    return null
}

data class DropResult(
    val items: List<HomeItem>,
    val createdFolder: Folder? = null,
    val updatedFolder: Folder? = null,
)

// shortcut: drops stay on the dragged item's page; cross-page drag lands with the pager rework.
fun resolveDrop(
    items: List<HomeItem>,
    folders: Map<String, Folder>,
    draggedId: String,
    targetCol: Int,
    targetRow: Int,
    cols: Int,
    rows: Int,
    newFolderId: String,
): DropResult {
    val dragged = items.firstOrNull { it.id == draggedId } ?: return DropResult(items)
    if (targetCol !in 0 until cols || targetRow !in 0 until rows) return DropResult(items)
    if (rectOf(dragged).contains(targetCol, targetRow)) return DropResult(items)

    fun nearFallback(): DropResult {
        val spot = findSpotNear(
            items.filter { it.id != draggedId }, dragged.page,
            dragged.colSpan, dragged.rowSpan, cols, rows, targetCol, targetRow,
        ) ?: return DropResult(items)
        return DropResult(items.map { if (it.id == draggedId) dragged.copy(col = spot.first, row = spot.second) else it })
    }

    val occupant = items.firstOrNull {
        it.id != draggedId && it.page == dragged.page && rectOf(it).contains(targetCol, targetRow)
    }

    if (occupant == null) {
        if (targetCol + dragged.colSpan > cols || targetRow + dragged.rowSpan > rows) {
            return nearFallback()
        }
        val nr = Rect(targetCol, targetRow, dragged.colSpan, dragged.rowSpan)
        if (items.any { it.id != draggedId && it.page == dragged.page && rectOf(it).overlaps(nr) }) {
            return nearFallback()
        }
        return DropResult(items.map { if (it.id == draggedId) dragged.copy(col = targetCol, row = targetRow) else it })
    }

    // A clock page fills its whole page; nothing may be dropped onto it.
    if (occupant.kind == ItemKind.CLOCK_PAGE) return DropResult(items)

    if (occupant.kind == ItemKind.APP && dragged.kind == ItemKind.APP) {
        val folder = Folder(newFolderId, "Folder", encodeMembers(listOf(occupant.payload, dragged.payload)))
        val newItems = items
            .filterNot { it.id == occupant.id || it.id == draggedId } +
            HomeItem(
                id = "folder:$newFolderId",
                kind = ItemKind.FOLDER,
                page = occupant.page,
                col = occupant.col,
                row = occupant.row,
                colSpan = 2,
                rowSpan = 2,
                payload = newFolderId,
            )
        return DropResult(newItems, createdFolder = folder)
    }

    if (occupant.kind == ItemKind.FOLDER && dragged.kind == ItemKind.APP) {
        val f = folders[occupant.payload] ?: return DropResult(items)
        val members = parseMembers(f.membersJson)
        val updated = if (dragged.payload in members) {
            f
        } else {
            f.copy(membersJson = encodeMembers(members + dragged.payload))
        }
        return DropResult(items.filterNot { it.id == draggedId }, updatedFolder = updated)
    }

    // Swap positions (folder onto app/folder, app onto folder being dragged, etc.).
    val dNew = dragged.copy(col = occupant.col, row = occupant.row)
    val oNew = occupant.copy(col = dragged.col, row = dragged.row)
    val dRect = rectOf(dNew)
    val oRect = rectOf(oNew)
    if (dRect.col + dRect.colSpan > cols || dRect.row + dRect.rowSpan > rows) return nearFallback()
    if (oRect.col + oRect.colSpan > cols || oRect.row + oRect.rowSpan > rows) return nearFallback()
    val third = items.filter { it.id != draggedId && it.id != occupant.id && it.page == dragged.page }
    if (third.any { rectOf(it).overlaps(dRect) || rectOf(it).overlaps(oRect) }) return nearFallback()
    return DropResult(items.map {
        when (it.id) {
            draggedId -> dNew
            occupant.id -> oNew
            else -> it
        }
    })
}

/** Dock picks shared by seedDense and the Minimal first-run seed: dialer/messages/browser/camera first. */
fun dockPicks(apps: List<Pair<String, String>>, n: Int): List<String> {
    val candidates = listOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
        "com.android.camera",
    )
    val dock = mutableListOf<String>()
    for (pkg in candidates) {
        if (dock.size >= n) break
        apps.firstOrNull { it.second == pkg }?.let { (key, _) ->
            if (key !in dock) dock += key
        }
    }
    for ((key, _) in apps) {
        if (dock.size >= n) break
        if (key !in dock) dock += key
    }
    return dock
}

/**
 * Seed a dense home layout. [apps] are (key, pkg) pairs in catalog (label-sorted) order.
 * [cols]/[rows] are in half-cell units (DB v2). Returns items plus up to 4 dock keys.
 */
fun seedDense(apps: List<Pair<String, String>>, cols: Int, rows: Int): Pair<List<HomeItem>, List<String>> {
    val items = mutableListOf(
        HomeItem("clock", ItemKind.CLOCK, 0, 0, 0, cols, 4, "serif"),
    )
    var page = 0
    for ((key, _) in apps) {
        while (true) {
            val spot = findSpot(items, page, 2, 2, cols, rows)
            if (spot != null) {
                items += HomeItem("app:$key", ItemKind.APP, page, spot.first, spot.second, 2, 2, key)
                break
            }
            page++
        }
    }
    return items to dockPicks(apps, 4)
}

// ---------------------------------------------------------------------------
// Grid <-> free-canvas converters (half-cell units; fractions are of the page)
// ---------------------------------------------------------------------------

/** Grid coords -> free-canvas fractions. Grid coords are left untouched by callers. */
fun HomeItem.toFree(colsU: Int, rowsU: Int): HomeItem {
    if (colsU <= 0 || rowsU <= 0) return this
    return copy(
        fx = col.toFloat() / colsU,
        fy = row.toFloat() / rowsU,
        fw = colSpan.toFloat() / colsU,
        fh = rowSpan.toFloat() / rowsU,
    )
}

/** Free-canvas fractions -> grid coords; spans derive from fractions (min 2 units). */
fun HomeItem.toGrid(colsU: Int, rowsU: Int): HomeItem {
    if (colsU <= 0 || rowsU <= 0) return this
    val cs = (fw * colsU).roundToInt().coerceIn(2, colsU)
    val rs = (fh * rowsU).roundToInt().coerceIn(2, rowsU)
    val c = (fx * colsU).roundToInt().coerceIn(0, (colsU - cs).coerceAtLeast(0))
    val r = (fy * rowsU).roundToInt().coerceIn(0, (rowsU - rs).coerceAtLeast(0))
    return copy(col = c, row = r, colSpan = cs, rowSpan = rs)
}

// ---------------------------------------------------------------------------
// Scroll flow (vc5, pages removed). The home is one continuous list; a
// HomeItem's position in it is flowOrder. APP-kind items are not rendered
// as flow rows (the full A-Z catalog below the flow covers every app),
// but they still carry a flowOrder so migration never drops a row.
// ---------------------------------------------------------------------------

/**
 * One-time paged -> scroll-flow migration (vc5). MIGRATION CHOICE:
 * migrate in place, not a reset — a reset would silently delete the
 * user's folders and widgets, which are the only home content that is
 * not reconstructible from the app catalog. This function only numbers
 * items whose flowOrder is still -1 (unmigrated); items already >= 0 are
 * returned untouched, so re-running it after the user reorders can never
 * undo their reorder. Unmigrated items sort pinned-first (APP items
 * whose key is in [pinnedKeys], in dock order — the favorites the user
 * already chose stay on top), then by legacy (page, row, col), which is
 * exactly the page-then-position order they had. The caller writes the
 * result back only when it differs; with no -1 items the result is the
 * input list itself, so the write is naturally skipped.
 */
fun migrateToFlow(items: List<HomeItem>, pinnedKeys: List<String>): List<HomeItem> {
    if (items.none { it.flowOrder < 0 }) return items
    val pinnedRank = pinnedKeys.withIndex().associate { it.value to it.index }
    val kept = items.filter { it.flowOrder >= 0 }
    var next = (kept.maxOfOrNull { it.flowOrder } ?: -1) + 1
    val unmigrated = items.filter { it.flowOrder < 0 }.sortedWith(
        compareBy(
            { item ->
                if (item.kind == ItemKind.APP) pinnedRank[item.payload] ?: Int.MAX_VALUE
                else Int.MAX_VALUE
            },
            { it.page },
            { it.row },
            { it.col },
        ),
    )
    val numbered = unmigrated.map { it.copy(flowOrder = next++) }
    return kept + numbered
}

/** Flow rows in display order: migrated items first, any unmigrated stragglers appended in stored order. */
fun flowSorted(items: List<HomeItem>): List<HomeItem> =
    items.sortedWith(compareBy({ it.flowOrder < 0 }, { it.flowOrder }))

// ---------------------------------------------------------------------------
// Scroll-flow drag helpers (M6, stock-Android model). NOTE: resolveDrop /
// placedItem / findSpot above are paged-grid math (page/col/row) and
// are the drop math for the paged home: a drop resolves to a page +
// cell (or onto a folder), in ui/PagedHome.kt's drag controller.
// ---------------------------------------------------------------------------

/**
 * M6 snap mapping for the existing placementMode store field (kept, not
 * replaced — the settings worker reads the same field):
 * - 0 (grid) = Smart Grid, snap ON — the default.
 * - 1 (free canvas) = snap OFF.
 * - 2 (legacy scroll-list, a page-era remnant) = snap ON: it predates the
 *   snap toggle and never meant free placement, so it reads as snap-on
 *   rather than inventing a third mode. The edit-toolbar toggle writes
 *   only 0/1, which normalizes a legacy 2 on first flip.
 * In a one-dimensional scroll flow there are no 2-D cells to snap to, so
 * "snap" here means: snap ON latches the drop slot (midpoint rule) and
 * shows the accent insertion bar; snap OFF shows no slot bar and the
 * landing spot tracks the finger continuously, including gaps and headers.
 * The home stays ONE continuous scroll in both modes — this toggles
 * snapping only, never pages.
 */
fun snapOn(placementMode: Int): Boolean = placementMode != 1

/** Drop zone within a target row: the middle 60% is ONTO (folder ops for
 * app drags), the top/bottom 20% are INSERT edges (reorder/pin). */
enum class DropZone { ONTO, INSERT_BEFORE, INSERT_AFTER }

fun dropZoneFor(center: Float, offset: Float, size: Float): DropZone {
    if (size <= 0f) return DropZone.INSERT_AFTER
    val frac = (center - offset) / size
    return when {
        frac < 0.2f -> DropZone.INSERT_BEFORE
        frac > 0.8f -> DropZone.INSERT_AFTER
        else -> DropZone.ONTO
    }
}

// ---------------------------------------------------------------------------
// Layout snapshots (undo). JSON via org.json; parsing never throws.
// ---------------------------------------------------------------------------

fun snapshotJson(items: List<HomeItem>, folders: List<Folder>): String {
    val root = JSONObject()
    val itemsArr = JSONArray()
    for (it in items) {
        itemsArr.put(
            JSONObject()
                .put("id", it.id)
                .put("kind", it.kind)
                .put("page", it.page)
                .put("col", it.col)
                .put("row", it.row)
                .put("colSpan", it.colSpan)
                .put("rowSpan", it.rowSpan)
                .put("payload", it.payload)
                .put("fx", it.fx.toDouble())
                .put("fy", it.fy.toDouble())
                .put("fw", it.fw.toDouble())
                .put("fh", it.fh.toDouble())
                .put("flowOrder", it.flowOrder),
        )
    }
    val foldersArr = JSONArray()
    for (f in folders) {
        foldersArr.put(
            JSONObject()
                .put("id", f.id)
                .put("name", f.name)
                .put("membersJson", f.membersJson),
        )
    }
    root.put("items", itemsArr)
    root.put("folders", foldersArr)
    return root.toString()
}

fun parseSnapshot(json: String): Pair<List<HomeItem>, List<Folder>> {
    return try {
        val root = JSONObject(json)
        val items = mutableListOf<HomeItem>()
        val itemsArr = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemsArr.length()) {
            val o = itemsArr.optJSONObject(i) ?: continue
            items += HomeItem(
                id = o.optString("id", ""),
                kind = o.optInt("kind", ItemKind.APP),
                page = o.optInt("page", 0),
                col = o.optInt("col", 0),
                row = o.optInt("row", 0),
                colSpan = o.optInt("colSpan", 2),
                rowSpan = o.optInt("rowSpan", 2),
                payload = o.optString("payload", ""),
                fx = o.optDouble("fx", 0.0).toFloat(),
                fy = o.optDouble("fy", 0.0).toFloat(),
                fw = o.optDouble("fw", 0.0).toFloat(),
                fh = o.optDouble("fh", 0.0).toFloat(),
                // Pre-vc5 snapshots have no flowOrder (-1): the migration
                // re-numbers them instead of dropping them.
                flowOrder = o.optInt("flowOrder", -1),
            )
        }
        val folders = mutableListOf<Folder>()
        val foldersArr = root.optJSONArray("folders") ?: JSONArray()
        for (i in 0 until foldersArr.length()) {
            val o = foldersArr.optJSONObject(i) ?: continue
            folders += Folder(
                id = o.optString("id", ""),
                name = o.optString("name", "Folder"),
                membersJson = o.optString("membersJson", "[]"),
            )
        }
        items.filter { it.id.isNotEmpty() } to folders.filter { it.id.isNotEmpty() }
    } catch (t: Throwable) {
        emptyList<HomeItem>() to emptyList<Folder>()
    }
}

// ---------------------------------------------------------------------------
// M8 paged-home grid (PagedHome.kt). Pure helpers only; the paged-grid
// math above (rectOf / findSpot / resolveDrop, half-cell units) is
// untouched, and so are migrateToFlow / flowSorted / snapshot code.
// ---------------------------------------------------------------------------

/**
 * Grid size (in CELLS) for one pager page, honouring the stored grid
 * settings exactly as [gridCells] does: an explicit cols/rows override
 * (> 0) wins per axis, clamped to the supported 3..12; -1 means
 * "derived from density 0..100" via that same derivation.
 *
 * Un-overridden defaults, per the home spec: Compact 5x6, Medium 6x6
 * (fixed — a phone grid is a fixed grid, not a content fit). Expanded
 * (tablet) is the computed grid of spec §4.1: columns =
 * floor(contentWidth / cellWidth), rows = floor(contentHeight /
 * cellHeight), cell ~ icon + 56dp, clamped to 3..12 — intersected with
 * the density derivation, so a dense setting can shrink the grid but
 * never grow it past what the page fits. That intersection is what
 * yields the spec's defaults on a tablet: Today visible narrows the
 * grid area to ~4 columns, Today off (full-width, ~880dp cap) to ~6.
 */
fun pagedGridCells(
    state: SettingsState,
    landscape: Boolean,
    sizeClass: HomeSizeClass,
    contentWidthDp: Float,
    contentHeightDp: Float,
    iconDp: Int,
): Pair<Int, Int> {
    val overrideCols = if (landscape) state.colsLand else state.colsPort
    val overrideRows = if (landscape) state.rowsLand else state.rowsPort
    val derived = state.gridCells(landscape)
    val cell = (iconDp + 56).toFloat().coerceAtLeast(1f)
    val fitCols = (contentWidthDp / cell).toInt().coerceIn(3, 12)
    val fitRows = (contentHeightDp / cell).toInt().coerceIn(3, 12)
    val cols = when {
        overrideCols > 0 -> overrideCols.coerceIn(3, 12)
        sizeClass == HomeSizeClass.Compact -> 5
        sizeClass == HomeSizeClass.Medium -> 6
        else -> minOf(derived.first, fitCols)
    }
    val rows = when {
        overrideRows > 0 -> overrideRows.coerceIn(3, 12)
        sizeClass == HomeSizeClass.Compact -> 6
        sizeClass == HomeSizeClass.Medium -> 6
        else -> minOf(derived.second, fitRows)
    }
    return cols to rows
}

/**
 * M9 pager model: Today is a DEDICATED pager page (Doc 2 §4.3), never
 * a column beside a grid page. When Today is on it is always pager
 * page 0 and grid pages follow it (pager index = grid index + 1);
 * when off, pager pages are exactly the visible grid pages. The host
 * (HavenRoot, which owns the pager state) and the pager (PagedHome)
 * share these helpers so the two can never disagree about the mapping.
 * [visibleGridPages] is the visible grid-page list (hidden pages
 * removed, falling back to all pages when hiding would leave none —
 * the same rule PagedHome applies).
 */

/** Pager page count: visible grid pages, plus one for Today when on. */
fun todayPagerPageCount(visibleGridCount: Int, todayOn: Boolean): Int =
    visibleGridCount + if (todayOn) 1 else 0

/** Pager index of a grid page. A hidden grid page maps to the nearest
 * visible page at or before it (else the first visible page), exactly
 * as PagedHome did before this helper existed. */
fun pagerIndexForGrid(
    gridPage: Int,
    visibleGridPages: List<Int>,
    todayOn: Boolean,
): Int {
    val visibleIndex = visibleGridPages.indexOf(gridPage)
    val base = when {
        visibleIndex >= 0 -> visibleIndex
        else -> visibleGridPages.indexOfLast { it < gridPage }.takeIf { it >= 0 } ?: 0
    }
    return base + if (todayOn) 1 else 0
}

/** Grid page of a pager page, or null for the Today page (pager 0 when on). */
fun gridPageForTodayPager(
    pagerPage: Int,
    visibleGridPages: List<Int>,
    todayOn: Boolean,
): Int? {
    if (todayOn && pagerPage == 0) return null
    return visibleGridPages.getOrNull(pagerPage - if (todayOn) 1 else 0)
}

/**
 * Number of grid pages in [items]: one past the highest stored page
 * (pages are 0-based), at least [minPages]. Items with a negative page
 * (unmigrated leftovers) count as page 0. Empty trailing pages have
 * no items by definition, so the host passes them via [minPages].
 */
fun pagedPageCount(items: List<HomeItem>, minPages: Int = 1): Int {
    val highest = items.maxOfOrNull { it.page.coerceAtLeast(0) } ?: -1
    return maxOf(minPages.coerceAtLeast(1), highest + 1)
}

/**
 * Cell (col, row) hit by a point inside a grid area, or null when the
 * point is outside the grid. Coordinates and cell size share a unit
 * (pixels here; the caller converts dp once).
 */
fun cellFromOffset(
    x: Float,
    y: Float,
    cellW: Float,
    cellH: Float,
    cols: Int,
    rows: Int,
): Pair<Int, Int>? {
    if (cellW <= 0f || cellH <= 0f) return null
    val c = (x / cellW).toInt()
    val r = (y / cellH).toInt()
    if (c !in 0 until cols || r !in 0 until rows) return null
    return c to r
}
