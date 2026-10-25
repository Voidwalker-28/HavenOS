package app.havenos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import kotlin.math.roundToInt

/**
 * M9 edit-mode behavior (Doc 1 §3, Doc 2 §4.4, Doc 3 §3.4).
 *
 * Pure logic lives here and is unit-tested (EditModeTest): the 50-level
 * undo/redo stack, snap math, the §3.3 collision policy (including the
 * 400ms folder hold and the widget never-overlap rule), row-by-row
 * shifting, resize clamping, and first-free-spot search. The composables
 * below are the Doc 1 §3.10 toolbar, the context bar, the hint chip and
 * the Calm editor stub — the host (Home.kt) owns persistence and the
 * pager (PagedHome.kt) owns drag geometry; nothing here writes the DB
 * or the settings store directly.
 *
 * Adaptations (standing user directions, override the docs): no preset
 * picker/carousel anywhere — "preset" here means Minimal's tokens, so
 * "Reset to preset" in the overflow resets to Minimal defaults (the host
 * maps it to its existing section reset) and there is no preset UI.
 *
 * Known deviations, stated plainly:
 * - Snap-off steps are HALF-cell (1 unit), not quarter-cell: the stored
 *   grid is half-cell units (DB v2), so a quarter-cell position cannot
 *   be represented without a schema change. The toggle still labels
 *   itself "Snap to grid" and off still means a finer sub-grid.
 * - The context bar is rendered by the host directly under the toolbar
 *   (reserved space, never covering icons) rather than adjacent to the
 *   selected item: item geometry lives in PagedHome's pager and is not
 *   hoisted to the host. It appears only while something is selected.
 * - Corner handles on the selected item are visual only; resizing runs
 *   through the context-bar Resize entry (size chips + steppers),
 *   which is the same dialog the old toolbar Resize button opened.
 */

// ---------------------------------------------------------------------------
// Timing / limits (Doc 1 §3.3, §3.7, §3.10)
// ---------------------------------------------------------------------------

/** Long-press that starts a drag (also the in-edit-mode handle slop is separate). */
const val EDIT_LONG_PRESS_MS = 350L

/** Icon held on another icon this long creates a folder; released earlier shifts rows. */
const val FOLDER_HOLD_MS = 400L

/** Edge hold that turns the page (implemented in PagedHome). */
const val EDGE_TURN_MS = 600L

/** Undo/redo depth (Doc 1 §3.7). */
const val EDIT_UNDO_MAX = 50

/** Destructive-action snackbar lifetime (Doc 1 §3.7). */
const val EDIT_UNDO_SNACKBAR_MS = 5_000L

// ---------------------------------------------------------------------------
// Undo / redo — one stack object, shared by edit mode and the host.
// The host owns it (remember { EditUndoStack() }); the Settings panel
// keeps its existing Before/Changes path for settings values, and the
// host's layout snapshots are the shared undo currency for layout.
// Snapshots are opaque strings (snapshotJson); this class never parses.
// ---------------------------------------------------------------------------

class EditUndoStack {
    private val undo = ArrayDeque<String>()
    private val redo = ArrayDeque<String>()

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val depth: Int get() = undo.size

    /** Record the pre-change snapshot. Any new change clears the redo stack. */
    fun push(before: String) {
        undo.addLast(before)
        while (undo.size > EDIT_UNDO_MAX) undo.removeFirst()
        redo.clear()
    }

    /**
     * Undo: returns the snapshot to restore, pushing [current] onto the
     * redo stack. Null when there is nothing to undo.
     */
    fun undo(current: String): String? {
        val snap = undo.removeLastOrNull() ?: return null
        redo.addLast(current)
        while (redo.size > EDIT_UNDO_MAX) redo.removeFirst()
        return snap
    }

    /** Redo: the mirror of [undo]. Null when there is nothing to redo. */
    fun redo(current: String): String? {
        val snap = redo.removeLastOrNull() ?: return null
        undo.addLast(current)
        while (undo.size > EDIT_UNDO_MAX) undo.removeFirst()
        return snap
    }

    fun clear() {
        undo.clear()
        redo.clear()
    }
}

// ---------------------------------------------------------------------------
// Snap math (Doc 1 §3.3). Coordinates are half-cell units; cellW/cellH
// are pixels. Snap ON = nearest cell (even units); OFF = finer sub-grid
// (odd units allowed — half-cell steps, see the file header note).
// ---------------------------------------------------------------------------

/** Snapped horizontal unit for a finger x inside a grid of [colsU] units. */
fun snapUnit(posPx: Float, cellPx: Float, snapOn: Boolean, spanUnits: Int, maxUnits: Int): Int {
    if (cellPx <= 0f) return 0
    val maxStart = (maxUnits - spanUnits).coerceAtLeast(0)
    return if (snapOn) {
        ((posPx / cellPx).roundToInt() * 2).coerceIn(0, maxStart)
    } else {
        // Half-cell steps: posPx/cellPx cells -> *2 units, rounded.
        ((posPx / cellPx) * 2).roundToInt().coerceIn(0, maxStart)
    }
}

/** Snap ON must always land on an even unit (a whole cell). */
fun isCellAligned(unit: Int): Boolean = unit % 2 == 0

// ---------------------------------------------------------------------------
// Collision policy (Doc 1 §3.3), layered over Grid.kt's resolveDrop.
// ---------------------------------------------------------------------------

/**
 * Row-by-row shift: place [draggedId] at (targetCol, targetRow), then
 * walk the page's other items in row-major order; any item overlapping
 * an already-placed item moves to the next free spot scanning from its
 * own position. Returns null when some item has nowhere to go (the
 * caller then leaves the layout untouched).
 */
fun shiftRowByRow(
    items: List<HomeItem>,
    draggedId: String,
    targetCol: Int,
    targetRow: Int,
    cols: Int,
    rows: Int,
): List<HomeItem>? {
    val dragged = items.firstOrNull { it.id == draggedId } ?: return null
    val page = dragged.page
    val placed = mutableListOf<HomeItem>()
    val moved = dragged.copy(col = targetCol, row = targetRow)
    if (targetCol + dragged.colSpan > cols || targetRow + dragged.rowSpan > rows) return null
    placed += moved
    val others = items.filter { it.id != draggedId && it.page == page }
        .sortedWith(compareBy({ it.row }, { it.col }))
    for (item in others) {
        if (placed.none { rectOf(it).overlaps(rectOf(item)) }) {
            placed += item
        } else {
            val spot = findSpot(placed, page, item.colSpan, item.rowSpan, cols, rows)
                ?: return null
            placed += item.copy(col = spot.first, row = spot.second)
        }
    }
    val untouched = items.filter { it.page != page }
    return untouched + placed
}

/**
 * The §3.3 drop, given the resolved target cell and how long the dragged
 * item hovered its occupant ([holdMs], tracked by PagedHome):
 * - widget (any non-app, non-folder kind) on occupied cells: NEVER
 *   overlaps — nearest free fit, or the original layout when none.
 * - app on app, held >= [FOLDER_HOLD_MS]: folder (via resolveDrop).
 * - app on app, released earlier: neighbors shift row by row.
 * - app on folder: add to it (via resolveDrop).
 * - everything else: resolveDrop's own rules (empty cell = place).
 */
fun resolveDropEdit(
    items: List<HomeItem>,
    folders: Map<String, Folder>,
    draggedId: String,
    targetCol: Int,
    targetRow: Int,
    cols: Int,
    rows: Int,
    newFolderId: String,
    holdMs: Long,
): DropResult {
    val dragged = items.firstOrNull { it.id == draggedId } ?: return DropResult(items)
    val occupant = items.firstOrNull {
        it.id != draggedId && it.page == dragged.page && rectOf(it).contains(targetCol, targetRow)
    }
    if (occupant == null) {
        return resolveDrop(items, folders, draggedId, targetCol, targetRow, cols, rows, newFolderId)
    }
    val isWidgetDrag = dragged.kind != ItemKind.APP && dragged.kind != ItemKind.FOLDER
    if (isWidgetDrag) {
        val spot = findSpotNear(
            items.filter { it.id != draggedId }, dragged.page,
            dragged.colSpan, dragged.rowSpan, cols, rows, targetCol, targetRow,
        ) ?: return DropResult(items) // no fit: back where it was
        return DropResult(
            items.map {
                if (it.id == draggedId) dragged.copy(col = spot.first, row = spot.second) else it
            },
        )
    }
    if (occupant.kind == ItemKind.APP && dragged.kind == ItemKind.APP && holdMs < FOLDER_HOLD_MS) {
        val shifted = shiftRowByRow(items, draggedId, targetCol, targetRow, cols, rows)
            ?: return DropResult(items)
        return DropResult(shifted)
    }
    return resolveDrop(items, folders, draggedId, targetCol, targetRow, cols, rows, newFolderId)
}

// ---------------------------------------------------------------------------
// Resizing (Doc 1 §3.4)
// ---------------------------------------------------------------------------

/** Built-in widget size chips, in CELLS (converted to units by the caller: ×2). */
val WIDGET_SIZE_CHIPS: List<Pair<String, Pair<Int, Int>>> = listOf(
    "Small" to (2 to 2),
    "Wide" to (4 to 2),
    "Tall" to (2 to 4),
    "Large" to (4 to 4),
)

/** Clamp a requested span into [min..max] units per axis. */
fun resizeClamp(w: Int, h: Int, minW: Int, minH: Int, maxW: Int, maxH: Int): Pair<Int, Int> =
    w.coerceIn(minW, maxW) to h.coerceIn(minH, maxH)

/** True when a widget of [w]×[h] units fits on [page] without overlapping (excluding itself). */
fun widgetFits(items: List<HomeItem>, page: Int, w: Int, h: Int, col: Int, row: Int, selfId: String, cols: Int, rows: Int): Boolean {
    if (col < 0 || row < 0 || col + w > cols || row + h > rows) return false
    val rect = Rect(col, row, w, h)
    return items.none { it.id != selfId && it.page == page && rectOf(it).overlaps(rect) }
}

/** Widgets and large folders (≥2×2 cells) are resizable; apps, clock and small folders are not. */
fun isResizable(item: HomeItem): Boolean {
    if (item.kind == ItemKind.APP || item.kind == ItemKind.CLOCK || item.kind == ItemKind.CLOCK_PAGE) return false
    if (item.kind == ItemKind.FOLDER) return item.colSpan >= 4 && item.rowSpan >= 4
    return true // built-in widgets + third-party widgets
}

// ---------------------------------------------------------------------------
// Adding (Doc 1 §3.5): first free spot, current page first, then other
// pages in order; null means "offer a new page".
// ---------------------------------------------------------------------------

/** (page, col, row) of the first free [w]×[h] spot, or null when every page is full. */
fun firstFreeSpotAcrossPages(
    items: List<HomeItem>,
    pageOrder: List<Int>,
    w: Int,
    h: Int,
    cols: Int,
    rows: Int,
): Triple<Int, Int, Int>? {
    for (page in pageOrder) {
        val spot = findSpot(items, page, w, h, cols, rows)
        if (spot != null) return Triple(page, spot.first, spot.second)
    }
    return null
}

/** A duplicated widget: same kind/size/payload, fresh id, placed by the caller. */
fun duplicateWidget(item: HomeItem, newId: String): HomeItem =
    item.copy(id = newId)

// ---------------------------------------------------------------------------
// Toolbar (Doc 1 §3.10): ONE 56dp row in its own reserved space.
// Icons + short labels on Expanded, icons only on Compact. The host
// renders this inside the home column (above the pager), so the grid
// is pushed down, never overlapped. Every old toolbar action is in
// this map: Add widget/Page live in + Add, Wallpaper + Personalization
// both route to the Look hub (Wallpaper opens that hub's wallpaper
// screen), Snap is the labelled toggle, Resize/Remove live on the
// context bar, and Revert/Restore/Reset live in the overflow.
// ---------------------------------------------------------------------------

@Composable
fun EditToolbar(
    expanded: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    snapOn: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSnapToggle: () -> Unit,
    onAddWidget: () -> Unit,
    onAddPage: () -> Unit,
    onAddFolder: () -> Unit,
    onPages: () -> Unit,
    onWallpaper: () -> Unit,
    onLook: () -> Unit,
    onCalmEditor: () -> Unit,
    onSettings: () -> Unit,
    onDone: () -> Unit,
    onRevertSession: () -> Unit,
    onRestoreLayout: () -> Unit,
    onResetPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var overflowOpen by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    GlassSurface(shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier
                .height(56.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EditToolButton("↩", "Undo", expanded, enabled = canUndo, onClick = onUndo)
            EditToolButton("↪", "Redo", expanded, enabled = canRedo, onClick = onRedo)
            // Snap: labelled "Snap to grid" with icon + tick state (Doc 1 §3.3).
            TextButton(
                onClick = onSnapToggle,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(
                    if (expanded) "Snap to grid ${if (snapOn) "✓" else "○"}" else if (snapOn) "⊞✓" else "⊞",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (snapOn) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Box {
                TextButton(
                    onClick = { addOpen = true },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(
                        if (expanded) "+ Add" else "+",
                        color = palette.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                    DropdownMenuItem(text = { Text("Widget") }, onClick = { addOpen = false; onAddWidget() })
                    DropdownMenuItem(text = { Text("Page") }, onClick = { addOpen = false; onAddPage() })
                    DropdownMenuItem(text = { Text("Folder") }, onClick = { addOpen = false; onAddFolder() })
                }
            }
            EditToolButton("▦", "Pages", expanded, onClick = onPages)
            EditToolButton("▣", "Wallpaper", expanded, onClick = onWallpaper)
            EditToolButton("◐", "Look", expanded, onClick = onLook)
            EditToolButton("☾", "Calm", expanded, onClick = onCalmEditor)
            EditToolButton("⚙", "Settings", expanded, onClick = onSettings)
            EditToolButton("✓", "Done", expanded, onClick = onDone)
            Box {
                TextButton(
                    onClick = { overflowOpen = true },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text("⋮", color = palette.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(text = { Text("Revert session") }, onClick = { overflowOpen = false; onRevertSession() })
                    DropdownMenuItem(text = { Text("Restore layout…") }, onClick = { overflowOpen = false; onRestoreLayout() })
                    DropdownMenuItem(text = { Text("Reset page") }, onClick = { overflowOpen = false; onResetPage() })
                }
            }
        }
    }
}

@Composable
private fun EditToolButton(
    icon: String,
    label: String,
    expanded: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Text(
            if (expanded) "$icon $label" else icon,
            color = palette.onSurface,
            fontSize = 13.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Context bar (Doc 1 §3.10): floating bar, ONLY when something is
// selected. "Remove (N)" shows the count only when N > 1. Resize only
// for resizable singles; Duplicate only for widgets; Item settings only
// for widgets (configure = resize) and folders (open the folder sheet).
// Add to dock / Hide / Uninstall are the Doc 1 §3.6 companions, shown
// only where they apply (single app; Uninstall only for user apps).
// ---------------------------------------------------------------------------

@Composable
fun EditContextBar(
    selectedCount: Int,
    canResize: Boolean,
    isWidget: Boolean,
    isFolder: Boolean,
    isSingleApp: Boolean,
    isUserApp: Boolean,
    onResize: () -> Unit,
    onRemove: () -> Unit,
    onMoveToPage: () -> Unit,
    onDuplicate: () -> Unit,
    onItemSettings: () -> Unit,
    onSelectMore: () -> Unit,
    onAddToDock: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    // M10 Level 2 (BUG-016): the selection's NAME, shown first so
    // the action scope is always obvious ("what am I editing?").
    selectedName: String? = null,
    canReplace: Boolean = false,
    onReplace: () -> Unit = {},
) {
    val palette = LocalPalette.current
    GlassSurface(shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier
                .height(56.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (selectedName != null) {
                Text(
                    selectedName,
                    color = palette.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (canResize) {
                TextButton(
                    onClick = onResize,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Resize", color = palette.onSurface, fontSize = 13.sp) }
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(
                    if (selectedCount > 1) "Remove ($selectedCount)" else "Remove",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                )
            }
            TextButton(
                onClick = onMoveToPage,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) { Text("Move to page", color = palette.onSurface, fontSize = 13.sp) }
            if (isWidget) {
                TextButton(
                    onClick = onDuplicate,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Duplicate", color = palette.onSurface, fontSize = 13.sp) }
            }
            // M10 Level 2: Replace (widgets only) opens the widget
            // sheet in replace mode — same position/size when the new
            // widget fits, nearest free spot otherwise, never a
            // silent drop (see the host's replaceWidget).
            if (canReplace) {
                TextButton(
                    onClick = onReplace,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Replace", color = palette.onSurface, fontSize = 13.sp) }
            }
            if (isWidget || isFolder) {
                TextButton(
                    onClick = onItemSettings,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Item settings", color = palette.onSurface, fontSize = 13.sp) }
            }
            TextButton(
                onClick = onSelectMore,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) { Text("Select more", color = palette.onSurface, fontSize = 13.sp) }
            if (isSingleApp) {
                TextButton(
                    onClick = onAddToDock,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Add to dock", color = palette.onSurface, fontSize = 13.sp) }
                TextButton(
                    onClick = onHide,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) { Text("Hide", color = palette.onSurface, fontSize = 13.sp) }
                if (isUserApp) {
                    TextButton(
                        onClick = onUninstall,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text("Uninstall", color = palette.onSurface, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hint chip (Doc 1 §3.10, Doc 2 §4.4): single dismissible chip, first
// three edit-mode opens only, rendered under the toolbar row — never
// over icons. The host owns the open count (settings key
// editHintOpens) and the per-session dismissal.
// ---------------------------------------------------------------------------

@Composable
fun EditHintChip(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    GlassSurface(shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Drag to move · Tap to select",
                color = palette.onSurface,
                fontSize = 12.sp,
            )
            Text(
                "✕",
                color = palette.onSurface,
                fontSize = 12.sp,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable { onDismiss() }
                    .padding(8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5-second destructive-action snackbar with Undo (Doc 1 §3.7). The host
// owns the 5s timer; this is only the row.
// ---------------------------------------------------------------------------

@Composable
fun EditUndoSnackbar(message: String, onUndo: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    GlassSurface(shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(message, color = palette.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
            TextButton(onClick = onUndo, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Text("Undo", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Text("✕", color = palette.onSurface, fontSize = 13.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Calm editor (Doc 1 §3.10, §5.9): the toolbar Calm action opens THIS,
// it does not enter Calm. Minimum viable editor: pick up to 8 favorites
// (order = selection order), saved to calmFavoritesJson by the host.
// Schedule + entry options stay in the Calm settings surface; this stub
// deliberately does not duplicate them. If Worker D ships a fuller
// editor, swap this call site for it — the host passes the same
// (currentKeys, onSave, onDismiss) contract.
// ---------------------------------------------------------------------------

@Composable
fun CalmEditorDialog(
    apps: List<AppEntry>,
    currentKeys: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var picked by remember(currentKeys) { mutableStateOf(currentKeys) }
    val ordered = remember(apps, picked) {
        val pickedFirst = picked.mapNotNull { key -> apps.firstOrNull { it.key == key } }
        val rest = apps.filter { it.key !in picked }.sortedBy { it.label }
        pickedFirst + rest
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calm favorites") },
        text = {
            Column {
                Text(
                    "Pick up to 8 favorites for Calm. This edits Calm only — it never changes the home layout.",
                    color = palette.muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(ordered, key = { it.key }) { entry ->
                        val selected = entry.key in picked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    picked = when {
                                        selected -> picked - entry.key
                                        picked.size >= 8 -> picked
                                        else -> picked + entry.key
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                if (selected) "☑" else "☐",
                                color = palette.accent,
                                fontSize = 16.sp,
                            )
                            Text(entry.label, color = palette.onSurface, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// NOTE: Calm favorites encoding lives in CalmMode.kt
// (encodeCalmFavorites); this file deliberately does not redefine it.

// ---------------------------------------------------------------------------
// Widget size chips for the host's ResizeDialog: chip cells -> units,
// clamped to the grid.
// ---------------------------------------------------------------------------

/** Chip (cells) -> units, clamped so the chip never exceeds the grid. */
fun widgetChipUnits(chip: Pair<Int, Int>, colsU: Int, rowsU: Int): Pair<Int, Int> =
    (chip.first * 2).coerceIn(2, colsU) to (chip.second * 2).coerceIn(2, rowsU)
