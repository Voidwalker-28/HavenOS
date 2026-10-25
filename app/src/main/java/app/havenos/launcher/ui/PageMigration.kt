package app.havenos.launcher.ui

import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import org.json.JSONArray

// ---------------------------------------------------------------------------
// M8 page model + scroll-flow -> pages migration + page operations.
// Pure functions only (no Compose, no Android framework): unit-testable,
// and the host (PagedHome coordinator) is the only writer to the DB.
//
// PAGE MODEL (reuses the existing columns — NO Room change, NO version
// bump): HomeItem.page is the page index (0-based; display number is
// index + 1), HomeItem.col/row/colSpan/rowSpan are grid coordinates in
// HALF-CELL units (DB v2: 1 cell = 2 units), exactly the convention
// Grid.kt's findSpot/findSpotNear/rectOf already use. Page order is
// page-index order. There is no pages table: hidden pages live in the
// hiddenPagesJson settings key and the default page in defaultPage.
// Empty pages have no rows, so any page list derived from items alone
// cannot see them — the host must carry the true page count (items
// pages + empty added pages) and pass it where a function asks for it.
//
// MIGRATION ALGORITHM (migrateFlowToPages, vc5 scroll flow -> pages):
// Source of truth is flowOrder (the vc5 scroll order). Legacy pre-vc5
// page/col/row values are STALE and are ignored — every item is
// re-placed from scratch. Items with flowOrder < 0 (never migrated to
// the flow) sort last, in stored order, so they are never dropped.
// Placement is one sequential pass in three buckets, each bucket in
// flow order, placed row-major (row outer, col inner) with Grid.kt's
// findSpot on the half-cell grid (colsU = cols * 2, rowsU = rows * 2),
// so a widget/folder spanning multiple cells occupies exactly its
// stored colSpan x rowSpan:
//   1. Pinned apps (kind APP whose payload is in pinnedKeys, in
//      pinnedKeys order) -> page index 0.
//   2. Everything that is not an app (folders, widgets, clock cards,
//      stacks) in flow order -> page 0's remaining cells, then page 1+.
//   3. Remaining apps in flow order (the A-Z block) -> starting at
//      page index 1 when page 0 received anything (page 0 is the
//      favorites/widgets page), otherwise page 0 (no pinned/widgets
//      must not leave a permanently empty first page), spilling
//      forward page by page.
// CAPACITY RULE: a page holds cols x rows CELLS. An item is placed on
// the earliest page, from its bucket's start page upward, with a free
// rect for its span. An item whose span exceeds the whole page can fit
// nowhere — it is placed alone at (0, 0) on a fresh page, keeping its
// stored span, rather than being discarded or silently resized.
// NOTHING IS EVER DROPPED: every input item appears in the output,
// and as a final guard any item missing from the placed list (by id)
// is appended unchanged.
// IDEMPOTENCY: the host gates on the pagesMigrated settings key — it
// calls this only when the key is false, then sets it true. The
// alreadyMigrated parameter is that gate in function form: when true
// the input is returned unchanged, so a second run can never re-flow
// a layout the user has since edited. flowOrder values are preserved
// (not renumbered) as vc5 history; the paged home reads page/col/row.
// ---------------------------------------------------------------------------

/** One page for the overview / default-page picker. */
data class PageSummary(
    val page: Int,
    val itemCount: Int,
    val firstLabel: String,
    val hidden: Boolean = false,
    val isDefault: Boolean = false,
)

/** Parse hiddenPagesJson (JSON int array, "" = none). Never throws. */
fun parseHiddenPages(json: String): List<Int> {
    return try {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        val out = mutableListOf<Int>()
        for (i in 0 until arr.length()) {
            val v = arr.optInt(i, -1)
            if (v >= 0 && v !in out) out += v
        }
        out.sorted()
    } catch (t: Throwable) {
        emptyList()
    }
}

/** Encode a hidden-pages list ("" when none, matching the key default). */
fun encodeHiddenPages(pages: List<Int>): String {
    val clean = pages.filter { it >= 0 }.distinct().sorted()
    return if (clean.isEmpty()) "" else JSONArray(clean).toString()
}

/** Hide / unhide helpers: sorted, deduplicated, never negative. */
fun hidePage(hiddenPages: List<Int>, page: Int): List<Int> =
    if (page < 0) hiddenPages else (hiddenPages + page).distinct().sorted()

fun unhidePage(hiddenPages: List<Int>, page: Int): List<Int> =
    hiddenPages.filter { it != page }

/** Clamp a default-page index to the existing pages (0 when none). */
fun clampDefaultPage(defaultPage: Int, pageCount: Int): Int =
    if (pageCount <= 0) 0 else defaultPage.coerceIn(0, pageCount - 1)

/**
 * Page list for the overview / picker: every page that holds items,
 * plus hidden pages (which may be empty), sorted by index. The first
 * label is the first APP item's label in (row, col) order, falling
 * back to the first item's label; empty pages read "Empty". The host
 * passes the real catalog labels via [labelFor]; [defaultItemLabel]
 * is the fallback when it has no catalog.
 */
fun pageSummaries(
    items: List<HomeItem>,
    hiddenPages: List<Int> = emptyList(),
    defaultPage: Int = -1,
    labelFor: (HomeItem) -> String = { defaultItemLabel(it) },
): List<PageSummary> {
    val pages = (items.map { it.page } + hiddenPages).filter { it >= 0 }.distinct().sorted()
    return pages.map { page ->
        val pageItems = items.filter { it.page == page }.sortedWith(compareBy({ it.row }, { it.col }))
        val firstApp = pageItems.firstOrNull { it.kind == ItemKind.APP }
        PageSummary(
            page = page,
            itemCount = pageItems.size,
            firstLabel = when {
                firstApp != null -> labelFor(firstApp)
                pageItems.isNotEmpty() -> labelFor(pageItems.first())
                else -> "Empty"
            },
            hidden = page in hiddenPages,
            isDefault = page == defaultPage,
        )
    }
}

/**
 * Fallback label for an item when the host has no app catalog handy.
 * APP payloads are catalog keys "$pkg/$cls#$serial": the readable part
 * is the last package segment. Folders use [folderNames]; widget kinds
 * use their kind name. The host should prefer real catalog labels.
 */
fun defaultItemLabel(item: HomeItem, folderNames: Map<String, String> = emptyMap()): String {
    return when (item.kind) {
        ItemKind.APP -> {
            val pkg = item.payload.substringBefore("/")
            val last = pkg.substringAfterLast(".")
            if (last.isBlank()) item.payload else last.replaceFirstChar { it.uppercaseChar() }
        }
        ItemKind.FOLDER -> folderNames[item.payload] ?: "Folder"
        ItemKind.CLOCK, ItemKind.CLOCK_PAGE -> "Clock"
        ItemKind.WEATHER -> "Weather"
        ItemKind.AGENDA -> "Agenda"
        ItemKind.NOW_PLAYING -> "Now playing"
        ItemKind.STORAGE -> "Storage"
        ItemKind.CALENDAR -> "Calendar"
        ItemKind.COUNTDOWN -> "Countdown"
        ItemKind.TASKS -> "Tasks"
        ItemKind.NOTES -> "Notes"
        ItemKind.GALLERY -> "Gallery"
        ItemKind.QUICK_TILES -> "Quick tiles"
        ItemKind.STACK -> "Stack"
        ItemKind.TP_WIDGET -> "Widget"
        else -> "Item"
    }
}

/**
 * Migrate the vc5 scroll flow to horizontal pages. [cols]/[rows] are
 * CELLS (see SettingsState.gridCells); spans convert via the half-cell
 * grid inside. See the file header for the algorithm + capacity rule.
 */
fun migrateFlowToPages(
    items: List<HomeItem>,
    pinnedKeys: List<String>,
    cols: Int,
    rows: Int,
    alreadyMigrated: Boolean = false,
): List<HomeItem> {
    if (alreadyMigrated) return items
    if (items.isEmpty() || cols <= 0 || rows <= 0) return items
    val colsU = cols * 2
    val rowsU = rows * 2
    // Flow order: migrated items first by flowOrder, unmigrated (-1)
    // stragglers last in stored order (flowSorted's rule, inlined so
    // this file stays dependency-light beyond Grid.kt's findSpot).
    val ordered = items.sortedWith(compareBy({ it.flowOrder < 0 }, { it.flowOrder }))
    val pinnedRank = pinnedKeys.withIndex().associate { it.value to it.index }
    val pinned = ordered
        .filter { it.kind == ItemKind.APP && it.payload in pinnedRank }
        .sortedBy { pinnedRank.getValue(it.payload) }
    val widgets = ordered.filter { it.kind != ItemKind.APP }
    val restApps = ordered.filter { it.kind == ItemKind.APP && it.payload !in pinnedRank }

    val placed = mutableListOf<HomeItem>()
    fun place(item: HomeItem, startPage: Int): HomeItem {
        // Oversize guard: fits nowhere, so it gets its own fresh page
        // at (0, 0) with its span untouched — never dropped, never resized.
        if (item.colSpan > colsU || item.rowSpan > rowsU || item.colSpan <= 0 || item.rowSpan <= 0) {
            val fresh = (placed.maxOfOrNull { it.page } ?: (startPage - 1)) + 1
            return item.copy(page = fresh.coerceAtLeast(startPage), col = 0, row = 0)
        }
        var page = startPage
        // Each item needs at most one fresh page; the cap only bounds
        // the loop so a corrupt grid can never spin forever.
        repeat(items.size + 2) {
            val spot = findSpot(placed, page, item.colSpan, item.rowSpan, colsU, rowsU)
            if (spot != null) return item.copy(page = page, col = spot.first, row = spot.second)
            page++
        }
        val fresh = (placed.maxOfOrNull { it.page } ?: (startPage - 1)) + 1
        return item.copy(page = fresh.coerceAtLeast(startPage), col = 0, row = 0)
    }

    for (item in pinned) placed += place(item, 0)
    for (item in widgets) placed += place(item, 0)
    // Page 0 is the favorites/widgets page when it has anything; the
    // A-Z apps start on the next page. When page 0 is empty (no pinned
    // apps, no widgets/folders), apps start on page 0 instead.
    val appStart = if (placed.any { it.page == 0 }) 1 else 0
    for (item in restApps) placed += place(item, appStart)

    // Final never-drop guard: anything missing (by id) rides along
    // unchanged rather than vanishing from the home.
    val placedIds = placed.map { it.id }.toSet()
    return placed + items.filter { it.id !in placedIds }
}

/** Pure add-page operation (also the edit-mode toolbar operation). */
fun addEmptyPage(items: List<HomeItem>, hiddenPages: List<Int> = emptyList()): Int {
    val maxPage = (items.map { it.page } + hiddenPages).filter { it >= 0 }.maxOrNull() ?: -1
    return maxPage + 1
}

/**
 * Move one icon to another page, recomputing its grid position near its
 * old position (findSpotNear, falling back to findSpot). Null when the
 * target page has no free rect for the item's span — the caller keeps
 * the item on its source page in that case.
 */
fun moveItemToPage(
    items: List<HomeItem>,
    itemId: String,
    targetPage: Int,
    colsU: Int,
    rowsU: Int,
): List<HomeItem>? {
    val item = items.firstOrNull { it.id == itemId } ?: return null
    if (item.page == targetPage) return items
    val others = items.filter { it.id != itemId }
    val spot = findSpotNear(others, targetPage, item.colSpan, item.rowSpan, colsU, rowsU, item.col, item.row)
        ?: findSpot(others, targetPage, item.colSpan, item.rowSpan, colsU, rowsU)
        ?: return null
    return items.map {
        if (it.id == itemId) it.copy(page = targetPage, col = spot.first, row = spot.second) else it
    }
}

/** Remap one page index through a reorder (pages absent from [newOrder] keep relative order after it). */
fun remapPageIndex(page: Int, newOrder: List<Int>, allPages: List<Int>): Int {
    val full = newOrder + allPages.filter { it !in newOrder }
    val idx = full.indexOf(page)
    return if (idx >= 0) idx else page
}

/**
 * Reorder pages: [newOrder] lists old page indices in their new display
 * order; pages absent from it keep their relative order after the
 * listed ones. Returns items with page indices renumbered.
 */
fun reorderPages(items: List<HomeItem>, newOrder: List<Int>): List<HomeItem> {
    val allPages = items.map { it.page }.distinct().sorted()
    return items.map { it.copy(page = remapPageIndex(it.page, newOrder, allPages)) }
}

// ---------------------------------------------------------------------------
// Remove page (the clarified rules, implemented exactly).
// The host owns persistence + the toast: it snapshots with
// snapshotPages() BEFORE calling deletePage, and on undo restores with
// restorePages(). Folders are untouched by a page delete, so the
// snapshot covers items only.
//   (a) LAST PAGE: when [totalPages] <= 1 the delete is refused as
//       LastPage — the overview disables the control with the reason
//       "Cannot delete the last page".
//   (b) EMPTY page: deletes freely, no confirm (the host skips its
//       confirm dialog when movedCount == 0).
//   (c) NON-EMPTY page: the host shows a confirm dialog naming
//       movedCount; on confirm every item moves to the ADJACENT page
//       (previous page, or next page when deleting page 0), placed via
//       findSpot in (row, col) order, spilling to further pages in
//       ascending index order, then to one fresh page — never discarded.
//   (d) UNDO: Success.snapshot is the pre-delete items snapshot; the
//       host shows its toast for 5s and restores it on undo.
//   (e) IMPOSSIBLE MOVE: an item whose span exceeds every page (so even
//       a fresh page cannot hold it) refuses the whole delete as
//       Refused, naming the blocking items by label; nothing moves.
// After a successful delete, page indices above the deleted page shift
// down by one (compaction), and hidden/default pages remap the same
// way; a default that pointed at the deleted page moves to the
// adjacent page. Hidden pages delete under the same rules.
// ---------------------------------------------------------------------------

sealed interface PageDeleteResult {
    data class Success(
        val items: List<HomeItem>,
        val movedCount: Int,
        val snapshot: String,
        val hiddenPages: List<Int>,
        val defaultPage: Int,
    ) : PageDeleteResult

    data class Refused(val blockingLabels: List<String>) : PageDeleteResult
    data object LastPage : PageDeleteResult
}

fun snapshotPages(items: List<HomeItem>): String = snapshotJson(items, emptyList())

/** Restore a snapshot from snapshotPages(); empty list on bad input (host must check before restoring). */
fun restorePages(snapshot: String): List<HomeItem> = parseSnapshot(snapshot).first

fun deletePage(
    items: List<HomeItem>,
    pageToDelete: Int,
    colsU: Int,
    rowsU: Int,
    totalPages: Int = -1,
    hiddenPages: List<Int> = emptyList(),
    defaultPage: Int = 0,
    labelFor: (HomeItem) -> String = { defaultItemLabel(it) },
): PageDeleteResult {
    val derivedTotal = (items.map { it.page }.maxOrNull() ?: -1) + 1
    val total = if (totalPages > 0) totalPages else derivedTotal.coerceAtLeast(1)
    if (total <= 1) return PageDeleteResult.LastPage
    if (pageToDelete !in 0 until total) {
        return PageDeleteResult.Refused(listOf("Page ${pageToDelete + 1} does not exist"))
    }
    val snapshot = snapshotPages(items)
    val toMove = items.filter { it.page == pageToDelete }.sortedWith(compareBy({ it.row }, { it.col }))
    val adjacent = if (pageToDelete > 0) pageToDelete - 1 else pageToDelete + 1
    // Compact helper: indices above the deleted page shift down by one.
    fun compact(page: Int): Int = when {
        page < pageToDelete -> page
        page > pageToDelete -> page - 1
        else -> page // unreachable for surviving items; kept for totality
    }
    val newHidden = hiddenPages.filter { it != pageToDelete }.map { compact(it) }.distinct().sorted()
    val newDefault = when {
        defaultPage == pageToDelete -> compact(adjacent)
        defaultPage > pageToDelete -> defaultPage - 1
        else -> defaultPage
    }.coerceIn(0, (total - 2).coerceAtLeast(0))

    if (toMove.isEmpty()) {
        // Empty page: free delete, no confirm, no items moved.
        val compacted = items.map { it.copy(page = compact(it.page)) }
        return PageDeleteResult.Success(compacted, 0, snapshot, newHidden, newDefault)
    }

    // Spill order: adjacent first, then every other page ascending
    // (excluding the deleted page), then one fresh page at the end.
    val candidates = listOf(adjacent) +
        (0 until total).filter { it != pageToDelete && it != adjacent } +
        listOf(total)
    val working = items.filter { it.page != pageToDelete }.toMutableList()
    val blocking = mutableListOf<String>()
    for (item in toMove) {
        if (item.colSpan > colsU || item.rowSpan > rowsU || item.colSpan <= 0 || item.rowSpan <= 0) {
            blocking += labelFor(item)
            continue
        }
        var placedItem: HomeItem? = null
        for (cand in candidates) {
            val spot = findSpot(working, cand, item.colSpan, item.rowSpan, colsU, rowsU)
            if (spot != null) {
                placedItem = item.copy(page = cand, col = spot.first, row = spot.second)
                break
            }
        }
        if (placedItem == null) blocking += labelFor(item) else working += placedItem
    }
    // Atomic: any blocking item refuses the whole delete; nothing moves.
    if (blocking.isNotEmpty()) return PageDeleteResult.Refused(blocking)
    val compacted = working.map { it.copy(page = compact(it.page)) }
    return PageDeleteResult.Success(compacted, toMove.size, snapshot, newHidden, newDefault)
}
