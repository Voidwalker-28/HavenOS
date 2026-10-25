package app.havenos.launcher.data

/**
 * Worker D (M9) — single source of truth, Doc 3 §3.1/§3.4.
 *
 * Pure, framework-free state layer. UI reads [LayoutState] and sends
 * events; only this layer (via [LayoutCoordinator], which persists it)
 * writes. Nothing here touches Compose, Room, or DataStore, so every
 * function below is unit-testable on the JVM.
 *
 * Model (Doc 3 §3.1):
 * - LayoutState: pages/items/spans (via HomeItem), folders, dock, Today
 *   cards, hidden apps. Positions per window class live in
 *   [LayoutProfiles] (LayoutProfiles.kt); the HomeItem page/col/row
 *   fields are the CANONICAL layout the profiles derive from.
 * - SettingsState: the existing DataStore state (Settings.kt).
 * - LookTokens: derived (LayoutCoordination.kt) — never stored.
 * - UiState: derived (metrics, label colors, insets) — never stored.
 *
 * Transaction rules (Doc 3 §3.4, Doc 1 §7):
 * - One drag/drop = one atomic write: every apply function returns a
 *   whole new LayoutState (or a rejection) — the coordinator persists
 *   it in a single Room transaction, so a crash mid-write can never
 *   leave a half-moved layout.
 * - A rejected transaction changes NOTHING and names the reason in a
 *   short toast string; it never corrupts the layout.
 * - No whole-home relayout for a single move: apply functions map over
 *   the item list and touch only the moved item (plus, for dock
 *   insert, the shifted dock entries).
 * - The write path stays off the composition path: the coordinator
 *   persists on Dispatchers.IO; these functions only compute.
 */

// ---------------------------------------------------------------- state

/** Today cards, in display order (Doc 3 §2.9: a flow, not grid cells). */
data class TodayCard(val id: String, val kind: Int)

/**
 * The layout half of the single source of truth. [recents] are app keys
 * in launch order (launcher's own history, Doc 3 §4 — no system usage
 * stats); the coordinator filters them against the live catalog at read
 * time, so a removed app vanishes from recents without a rewrite.
 */
data class LayoutState(
    val items: List<HomeItem> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val dock: List<String> = emptyList(),
    val todayCards: List<TodayCard> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
) {
    fun itemById(id: String): HomeItem? = items.firstOrNull { it.id == id }
    fun folderById(id: String): Folder? = folders.firstOrNull { it.id == id }
    fun pageCount(): Int = (items.maxOfOrNull { it.page } ?: -1) + 1
}

/** Result of one transaction attempt. Rejected changes nothing. */
sealed interface TxResult {
    /** Committed state + the pre-transaction snapshot for the undo stack. */
    data class Committed(val state: LayoutState, val undoSnapshot: LayoutState) : TxResult
    /** Nothing changed; [toast] is the short user-facing reason. */
    data class Rejected(val toast: String) : TxResult
}

/**
 * Shared 50-level undo stack (Doc 1 §3.7, Doc 3 §3.4): ONE object used
 * by edit mode AND Settings — Worker A adopts this class for the
 * toolbar Undo/Redo buttons (the UI-local 10-step list in Home.kt is
 * superseded by it; the coordinator owns the single instance).
 * Snapshots are whole LayoutStates (in-memory only; the stack clears
 * when the process ends — persisted snapshots in LayoutSnapshots.kt
 * handle longer recovery). Undo/redo never throws.
 */
class UndoStack(private val maxLevels: Int = 50) {
    private val undo = ArrayDeque<LayoutState>()
    private val redo = ArrayDeque<LayoutState>()

    /** Push the state BEFORE a committed transaction. Clears redo. */
    fun push(before: LayoutState) {
        undo.addLast(before)
        while (undo.size > maxLevels) undo.removeFirst()
        redo.clear()
    }

    /** Pop for undo: returns (stateToRestore, stateToPushOnRedo). */
    fun popUndo(current: LayoutState): Pair<LayoutState, LayoutState>? {
        val prev = undo.removeLastOrNull() ?: return null
        redo.addLast(current)
        while (redo.size > maxLevels) redo.removeFirst()
        return prev to current
    }

    /** Pop for redo. */
    fun popRedo(current: LayoutState): LayoutState? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(current)
        while (undo.size > maxLevels) undo.removeFirst()
        return next
    }

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoDepth: Int get() = undo.size
    fun clear() {
        undo.clear()
        redo.clear()
    }
}

/**
 * Settings changes that arrive DURING a drag (Doc 3 §3.4): queued,
 * applied on drop, in order. The coordinator owns one instance; the
 * queue itself is pure so its ordering is testable.
 */
class PendingChangesQueue {
    private val queued = ArrayDeque<() -> Unit>()
    var dragging: Boolean = false
        private set

    fun beginDrag() {
        dragging = true
    }

    /** End of drag: returns the queued changes, in order, and clears. */
    fun endDrag(): List<() -> Unit> {
        dragging = false
        val out = queued.toList()
        queued.clear()
        return out
    }

    /** Run now, or queue when a drag is in progress. */
    fun submit(change: () -> Unit) {
        if (dragging) queued.addLast(change) else change()
    }

    val queuedCount: Int get() = queued.size
    fun clear() = queued.clear()
}

// ------------------------------------------------------------- validation

/**
 * Lock-layout enforcement (Doc 1 §5.3/§6, Doc 3 §3.6): the STATE layer
 * owns the flag check; Worker A owns the toast text shown. Any edit
 * attempt while locked is rejected before it touches state.
 */
fun checkEditAllowed(lockLayout: Boolean): String? =
    if (lockLayout) "Layout is locked" else null

/**
 * Validate a transaction against CURRENT state (Doc 3 §3.4 conflict
 * rule): stale references (item/page/folder gone since the gesture
 * started) reject with a toast instead of corrupting the layout.
 */
fun validateItemExists(state: LayoutState, itemId: String): String? =
    if (state.itemById(itemId) == null) "Item no longer exists" else null

// ------------------------------------------------------------------ moves

/** Move one item within/between pages. Caller passes the target cell rect in half-cell units. */
fun applyMoveItem(
    state: LayoutState,
    itemId: String,
    targetPage: Int,
    targetCol: Int,
    targetRow: Int,
    lockLayout: Boolean = false,
): TxResult {
    checkEditAllowed(lockLayout)?.let { return TxResult.Rejected(it) }
    validateItemExists(state, itemId)?.let { return TxResult.Rejected(it) }
    if (targetPage < 0 || targetCol < 0 || targetRow < 0) {
        return TxResult.Rejected("Invalid position")
    }
    val item = state.itemById(itemId)!!
    if (item.page == targetPage && item.col == targetCol && item.row == targetRow) {
        return TxResult.Committed(state, state) // no-op drop: still a clean transaction boundary
    }
    val moved = item.copy(page = targetPage, col = targetCol, row = targetRow)
    val items = state.items.map { if (it.id == itemId) moved else it }
    return TxResult.Committed(state.copy(items = items), state)
}

/**
 * Effective dock capacity (Doc 1 §5.3): a side dock on Compact is
 * capped at 5 slots no matter what dockMax says; otherwise dockMax.
 */
fun effectiveDockMax(dockPos: Int, windowClass: WindowClass, dockMax: Int): Int {
    val capped = dockMax.coerceIn(0, 12)
    return if (dockPos != 0 && windowClass == WindowClass.COMPACT) minOf(capped, 5) else capped
}

/**
 * Drag icon to dock (Doc 3 §3.3 flow 3): capacity check first, then
 * insert at [slot] shifting others right; a full dock refuses with
 * "Dock is full". Recents are untouched (the dragged app is a pinned
 * entry now, not a recent). Dropping an app already in the dock
 * reorders it instead of duplicating it.
 */
fun applyDragToDock(
    state: LayoutState,
    appKey: String,
    slot: Int,
    dockPos: Int,
    windowClass: WindowClass,
    dockMax: Int,
    lockLayout: Boolean = false,
): TxResult {
    checkEditAllowed(lockLayout)?.let { return TxResult.Rejected(it) }
    if (appKey.isBlank()) return TxResult.Rejected("Invalid app")
    val max = effectiveDockMax(dockPos, windowClass, dockMax)
    val without = state.dock.filter { it != appKey }
    if (appKey !in state.dock && without.size >= max) {
        return TxResult.Rejected("Dock is full")
    }
    val at = slot.coerceIn(0, without.size)
    val newDock = (without.take(at) + appKey + without.drop(at)).take(max)
    // Home copy (if any) stays: dragging to the dock pins, it does not
    // remove the home icon (removal is a separate Remove action).
    return TxResult.Committed(state.copy(dock = newDock), state)
}

/** Remove an app key from the dock (drag out / context action). */
fun applyRemoveFromDock(state: LayoutState, appKey: String): LayoutState =
    state.copy(dock = state.dock.filter { it != appKey })

// ------------------------------------------------------------ free spots

private fun rectOverlaps(
    c: Int, r: Int, w: Int, h: Int,
    oCol: Int, oRow: Int, oW: Int, oH: Int,
): Boolean = c < oCol + oW && oCol < c + w && r < oRow + oH && oRow < r + h

/**
 * First free rect for a w×h (half-cell units) item on [page], row-major
 * scan. Null when the page is full. The coordinator tries pages in
 * order (current, then others, then a new page) for add-widget/pin flows.
 */
fun firstFreeSpot(
    items: List<HomeItem>,
    page: Int,
    w: Int,
    h: Int,
    colsU: Int,
    rowsU: Int,
): Pair<Int, Int>? {
    if (w <= 0 || h <= 0 || w > colsU || h > rowsU) return null
    for (r in 0..rowsU - h) {
        for (c in 0..colsU - w) {
            if (items.none { it.page == page && rectOverlaps(c, r, w, h, it.col, it.row, it.colSpan, it.rowSpan) }) {
                return c to r
            }
        }
    }
    return null
}

/**
 * Add-widget / pin-request placement (Doc 1 §3.5, Doc 3 §3.3 flow 4):
 * first free spot searching the current page, then other pages, then a
 * new page. Returns the placed item, or null when no page can hold it
 * (caller shows the "no room" state; nothing is added).
 */
fun placeOnFirstFreeSpot(
    state: LayoutState,
    id: String,
    kind: Int,
    payload: String,
    colSpan: Int,
    rowSpan: Int,
    colsU: Int,
    rowsU: Int,
    startPage: Int = 0,
): HomeItem? {
    val pageCount = maxOf(state.pageCount(), startPage + 1)
    val order = listOf(startPage) + (0 until pageCount).filter { it != startPage } + listOf(pageCount)
    for (page in order) {
        val spot = firstFreeSpot(state.items, page, colSpan, rowSpan, colsU, rowsU)
        if (spot != null) {
            return HomeItem(id, kind, page, spot.first, spot.second, colSpan, rowSpan, payload)
        }
    }
    return null
}

// ------------------------------------------------------- app lifecycle

/**
 * App installed (Doc 3 §3.3 flow 7): the Library always gains it (the
 * catalog is the source — this function only handles the home side).
 * When [autoAdd] is on and the app is not hidden and has no home item
 * yet, it is placed on the first free home slot; otherwise the layout
 * is unchanged. Returns (newState, wasPlacedOnHome).
 */
fun applyAppInstalled(
    state: LayoutState,
    appKey: String,
    autoAdd: Boolean,
    colsU: Int,
    rowsU: Int,
    startPage: Int = 0,
): Pair<LayoutState, Boolean> {
    if (!autoAdd || appKey.isBlank() || appKey in state.hidden) return state to false
    if (state.items.any { it.kind == ItemKind.APP && it.payload == appKey }) return state to false
    val placed = placeOnFirstFreeSpot(state, "app:$appKey", ItemKind.APP, appKey, 2, 2, colsU, rowsU, startPage)
        ?: return state to false
    return state.copy(items = state.items + placed) to true
}

/**
 * App uninstalled/disabled (Doc 3 §3.3 flow 8, §1 #10): ONE transaction,
 * NO undo (the app is gone — an undo entry pointing at a missing app
 * would resurrect a dead icon). Removes the app from layout, folders
 * (a folder left with 1 app dissolves to a plain icon; an empty folder
 * is deleted), dock, hidden list, and recents. Returns the new state.
 * The caller must ALSO drop the undo stack entry for this transaction
 * (there is none: this function never produces one).
 */
fun applyAppRemoved(state: LayoutState, appKey: String): LayoutState {
    if (appKey.isBlank()) return state
    var items = state.items.filterNot { it.kind == ItemKind.APP && it.payload == appKey }
    var folders = state.folders
    // Folder dissolve: recompute membership after the removal.
    val dissolved = mutableListOf<HomeItem>()
    val keptFolders = mutableListOf<Folder>()
    for (folder in folders) {
        val members = parseMembers(folder.membersJson).filter { it != appKey }
        when {
            members.isEmpty() -> {
                // Empty folder deleted; its home item goes with it.
                items = items.filterNot { it.kind == ItemKind.FOLDER && it.payload == folder.id }
            }
            members.size == 1 -> {
                // Dissolves to a plain icon in the folder's place.
                val homeItem = items.firstOrNull { it.kind == ItemKind.FOLDER && it.payload == folder.id }
                items = items.filterNot { it.id == homeItem?.id }
                if (homeItem != null) {
                    dissolved += HomeItem(
                        id = "app:${members[0]}", kind = ItemKind.APP,
                        page = homeItem.page, col = homeItem.col, row = homeItem.row,
                        colSpan = 2, rowSpan = 2, payload = members[0],
                        flowOrder = homeItem.flowOrder,
                    )
                }
            }
            else -> keptFolders += folder.copy(membersJson = encodeMembers(members))
        }
    }
    folders = keptFolders
    items = items + dissolved
    return state.copy(
        items = items,
        folders = folders,
        dock = state.dock.filter { it != appKey },
        hidden = state.hidden - appKey,
        recents = state.recents.filter { it != appKey },
    )
}

/**
 * App updated: icons refresh from the catalog at read time (AppRepository
 * caches per key+pack), so an update needs NO layout write — this
 * function exists so the coordinator has one named place for the event,
 * and returns the state unchanged. Work-profile apps keep distinct keys
 * ("$pkg/$cls#$serial"), so an update in one profile never touches the
 * other's placements.
 */
fun applyAppUpdated(state: LayoutState, appKey: String): LayoutState = state

/** Hide an app (Privacy): removed from layout + dock + recents immediately, undoable. */
fun applyHideApp(state: LayoutState, appKey: String): LayoutState {
    val withoutLayout = applyAppRemoved(state, appKey)
    return withoutLayout.copy(hidden = state.hidden + appKey)
}

// ------------------------------------------------------------ grid change

/** One displaced item in a grid-change preview (Doc 1 §5.2). */
data class GridChangeMove(
    val itemId: String,
    val fromPage: Int,
    val toPage: Int,
    val shrunk: Boolean,
    val note: String,
)

/** Grid-change plan: nothing is committed until the host applies it on Apply. */
data class GridChangePlan(
    /** Items that fit the new grid untouched (same page, same rect). */
    val kept: List<HomeItem>,
    /** Items that must move/shrink, with a per-item note for the summary. */
    val moves: List<GridChangeMove>,
    /** The full item list after applying the plan (for Apply). */
    val resultItems: List<HomeItem>,
    /** "N items will move to page 2" — the ghost-overlay count line. */
    val overflowCount: Int,
    /** True when the change displaces anything (ghost + Apply/Cancel required). */
    val needsConfirm: Boolean,
) {
    /** "N items will move to page N" summary line, or null when nothing moves. */
    fun summaryLine(): String? {
        if (!needsConfirm) return null
        val movedPages = moves.map { it.toPage }.distinct().sorted()
        val pageText = if (movedPages.size == 1) "page ${movedPages[0] + 1}" else "other pages"
        return "${moves.size} items will move to $pageText"
    }
}

/**
 * Grid-change safety (Doc 1 §5.2): shrink the grid, keep what fits,
 * shrink widgets to the nearest valid size when they no longer fit,
 * otherwise move items to the next page in position order. Folders keep
 * their contents; positions reflow row by row. Pure plan — the host
 * shows the ghost + summary and commits only on Apply.
 */
fun planGridChange(
    items: List<HomeItem>,
    newColsCells: Int,
    newRowsCells: Int,
): GridChangePlan {
    val colsU = newColsCells * 2
    val rowsU = newRowsCells * 2
    val ordered = items.sortedWith(compareBy({ it.page }, { it.row }, { it.col }))
    val result = mutableListOf<HomeItem>()
    val moves = mutableListOf<GridChangeMove>()
    val kept = mutableListOf<HomeItem>()
    for (item in ordered) {
        val w = item.colSpan.coerceAtMost(colsU)
        val h = item.rowSpan.coerceAtMost(rowsU)
        val shrunk = w != item.colSpan || h != item.rowSpan
        // Try the item's own page first (same rect, or shrunk rect).
        val samePageSpot = if (w > 0 && h > 0) {
            val rectFits = result.none {
                it.page == item.page && rectOverlaps(item.col, item.row, w, h, it.col, it.row, it.colSpan, it.rowSpan)
            } && item.col + w <= colsU && item.row + h <= rowsU
            rectFits
        } else false
        when {
            samePageSpot -> {
                val placed = if (shrunk) item.copy(colSpan = w, rowSpan = h) else item
                result += placed
                if (shrunk) {
                    moves += GridChangeMove(item.id, item.page, item.page, true, "Shrunk to fit the new grid")
                } else kept += placed
            }
            else -> {
                // Next page with a free rect, in position order; a fresh
                // page at the end is always available.
                var page = item.page
                var spot: Pair<Int, Int>? = null
                while (spot == null) {
                    page++
                    spot = firstFreeSpot(result, page, w, h, colsU, rowsU)
                    if (page > item.page + items.size + 2) break
                }
                if (spot == null) {
                    // Cannot happen (fresh page always fits a clamped
                    // span) — keep the item untouched rather than drop it.
                    result += item
                    kept += item
                } else {
                    val placed = item.copy(page = page, col = spot.first, row = spot.second, colSpan = w, rowSpan = h)
                    result += placed
                    moves += GridChangeMove(
                        item.id, item.page, page, shrunk,
                        if (shrunk) "Shrunk and moved to page ${page + 1}" else "Moved to page ${page + 1}",
                    )
                }
            }
        }
    }
    return GridChangePlan(
        kept = kept,
        moves = moves,
        resultItems = result,
        overflowCount = moves.count { it.toPage != it.fromPage },
        needsConfirm = moves.isNotEmpty(),
    )
}

// ------------------------------------------------------------ corruption

/** One skipped corrupt record, for the Diagnostics log (Doc 3 §3.5). */
data class CorruptRecord(val kind: String, val id: String, val reason: String)

/**
 * Corrupt-layout handling (Doc 3 §3.5): skip the bad record, keep the
 * rest, never crash on start. A record is corrupt when its id is blank,
 * its span is non-positive, its page is negative, or its id is
 * duplicated (first copy wins). Returns kept items/folders + the log.
 */
fun sanitizeLayout(
    items: List<HomeItem>,
    folders: List<Folder>,
): Triple<List<HomeItem>, List<Folder>, List<CorruptRecord>> {
    val log = mutableListOf<CorruptRecord>()
    val seen = HashSet<String>()
    val keptItems = mutableListOf<HomeItem>()
    for (item in items) {
        when {
            item.id.isBlank() -> log += CorruptRecord("item", item.id, "blank id")
            item.id in seen -> log += CorruptRecord("item", item.id, "duplicate id")
            item.colSpan <= 0 || item.rowSpan <= 0 -> log += CorruptRecord("item", item.id, "non-positive span")
            item.page < 0 -> log += CorruptRecord("item", item.id, "negative page")
            else -> {
                seen += item.id
                keptItems += item
            }
        }
    }
    // Orphan folders (no home item references them) are KEPT, not
    // dropped: deleting user data on a guess is worse than a stray row
    // the folder editor can surface. Only blank-id folders are skipped.
    val keptFolders = folders.filter { folder ->
        if (folder.id.isBlank()) {
            log += CorruptRecord("folder", folder.id, "blank id")
            false
        } else true
    }
    return Triple(keptItems, keptFolders, log)
}

// ------------------------------------------------------------- cold start

/**
 * Cold-start contract (Doc 3 §1 #16): the host draws [LayoutState]
 * from the Room cache (StateFlow) immediately; widgets fill in after.
 * This marker exists so the coordinator's cold-start path is one named
 * function: cache-first, never network, never a fresh seed over a
 * cached layout.
 */
fun isColdStartCacheUsable(items: List<HomeItem>): Boolean = true // any cached rows render; empty cache = first-run seed path
