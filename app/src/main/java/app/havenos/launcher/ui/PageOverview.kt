package app.havenos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.data.HomeItem
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Page overview (HOME-SCREEN-V2 §8.3, M8 paged home). A dialog/overlay;
// the coordinator wires it to PagedHome's onShowPageOverview and owns
// persistence. It renders ONLY what it is given — it never reads the
// DB — so the host's page list (items pages + empty added pages +
// hidden pages) is the single source of truth, shared with the
// HomeDockHub default-page picker via pageSummaries().
//
// WHAT EACH CONTROL DOES (host callbacks):
// - Thumbnail card: a mini icon layout of the page (positions from
//   col/row/colSpan/rowSpan in half-cell units, scaled to the card).
// - Move an icon: long-press-drag a mini icon onto another page card
//   (the card under the finger highlights; on release onMoveIcon fires
//   with the item id + target page — the host applies
//   moveItemToPage() and keeps the icon put when it returns null).
//   Tap fallback: tap a mini icon to select it, then tap "Move here"
//   on another card. Reorder: ◀ ▶ swap a page with its neighbour and
//   onReorderPages fires with the COMPLETE new order (old page
//   indices, visible pages in their new order followed by hidden
//   pages in theirs); the host applies reorderPages() and remaps
//   hidden/default via remapPageIndex().
// - ADD PAGE: onAddPage — the host computes addEmptyPage() and
//   persists it. The edit-mode toolbar's add-page button calls this
//   same operation (same helper, same host path).
// - HIDE / UNHIDE: onHidePage / onUnhidePage (host writes
//   hiddenPagesJson via hidePage()/unhidePage()); hidden pages render
//   in their own section with an Unhide button.
// - SET DEFAULT: onSetDefaultPage (host writes defaultPage).
// - DELETE: the last remaining page's delete control is disabled with
//   the reason "Cannot delete the last page". An empty page deletes
//   on tap with no confirm. A non-empty page shows a confirm dialog
//   naming the item count and the adjacent page its items move to;
//   on confirm onDeletePage fires and the host runs deletePage()
//   (adjacent-page move with spill, atomic refusal on impossible
//   moves). UNDO: after a delete the host passes the pre-delete
//   snapshot back as [lastDeletedSnapshot]; this dialog shows an
//   Undo bar for 5s — tapping Undo calls onUndoDeletePage(snapshot)
//   and the host restores it via restorePages(). After 5s (or on
//   dismiss) onUndoConsumed fires so the host clears the snapshot.
// - No presets, no network, no permissions: this file only lays out
//   pages the host already has.
// ---------------------------------------------------------------------------

/** One page as the overview renders it. Built by the host from pageSummaries() + items. */
data class OverviewPage(
    val page: Int,
    val items: List<HomeItem>,
    val hidden: Boolean = false,
    val isDefault: Boolean = false,
)

@Composable
fun PageOverviewDialog(
    pages: List<OverviewPage>,
    colsU: Int,
    rowsU: Int,
    labelFor: (HomeItem) -> String,
    onDismiss: () -> Unit,
    onMoveIcon: (itemId: String, targetPage: Int) -> Unit,
    onReorderPages: (newOrder: List<Int>) -> Unit,
    onAddPage: () -> Unit,
    onHidePage: (page: Int) -> Unit,
    onUnhidePage: (page: Int) -> Unit,
    onSetDefaultPage: (page: Int) -> Unit,
    onDeletePage: (page: Int) -> Unit,
    lastDeletedSnapshot: String? = null,
    onUndoDeletePage: (snapshot: String) -> Unit = {},
    onUndoConsumed: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val visible = pages.filter { !it.hidden }.sortedBy { it.page }
    val hidden = pages.filter { it.hidden }.sortedBy { it.page }
    val totalPages = pages.size
    val canDelete = totalPages > 1
    var pendingDelete by remember { mutableStateOf<OverviewPage?>(null) }
    var selectedIconId by remember { mutableStateOf<String?>(null) }

    // Long-press-drag state: card bounds + icon bounds in root coords.
    val cardBounds = remember { mutableStateMapOf<Int, Rect>() }
    val iconBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragCenter by remember { mutableStateOf<Offset?>(null) }
    var dragTarget by remember { mutableStateOf<Int?>(null) }

    // Undo bar lifetime: 5s from the snapshot arriving, then consumed.
    LaunchedEffect(lastDeletedSnapshot) {
        if (lastDeletedSnapshot != null) {
            delay(5_000)
            onUndoConsumed()
        }
    }

    fun fullOrderAfterSwap(section: List<OverviewPage>, a: Int, b: Int, other: List<OverviewPage>, sectionFirst: Boolean): List<Int> {
        val swapped = section.toMutableList()
        val tmp = swapped[a]
        swapped[a] = swapped[b]
        swapped[b] = tmp
        val swappedPages = swapped.map { it.page }
        val otherPages = other.map { it.page }
        return if (sectionFirst) swappedPages + otherPages else otherPages + swappedPages
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pages", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                Text(
                    "Long-press an icon and drag it onto another page, or tap an icon then tap Move here.",
                    fontSize = 13.sp,
                    color = scheme.onSurface.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onAddPage, modifier = Modifier.fillMaxWidth()) {
                    Text("Add page")
                }
                Spacer(Modifier.height(12.dp))

                if (lastDeletedSnapshot != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(scheme.primaryContainer).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Page deleted", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onUndoDeletePage(lastDeletedSnapshot) }) { Text("Undo") }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                PageCardGrid(
                    sectionPages = visible,
                    colsU = colsU,
                    rowsU = rowsU,
                    labelFor = labelFor,
                    canDelete = canDelete,
                    selectedIconId = selectedIconId,
                    draggedId = draggedId,
                    dragTarget = dragTarget,
                    cardBounds = cardBounds,
                    iconBounds = iconBounds,
                    onIconTap = { item ->
                        selectedIconId = if (selectedIconId == item.id) null else item.id
                    },
                    onDragStart = { item, center ->
                        draggedId = item.id
                        dragCenter = center
                        selectedIconId = null
                    },
                    onDragMove = { delta ->
                        dragCenter = dragCenter?.plus(delta)
                        dragTarget = dragCenter?.let { c ->
                            cardBounds.entries.firstOrNull { (_, r) -> r.contains(c) }?.key
                        }
                    },
                    onDragEnd = {
                        val target = dragTarget
                        val id = draggedId
                        draggedId = null
                        dragCenter = null
                        dragTarget = null
                        if (id != null && target != null) {
                            val source = pages.firstOrNull { p -> p.items.any { it.id == id } }?.page
                            if (source != null && source != target) onMoveIcon(id, target)
                        }
                    },
                    onMoveHere = { targetPage ->
                        val id = selectedIconId
                        if (id != null) {
                            selectedIconId = null
                            onMoveIcon(id, targetPage)
                        }
                    },
                    onReorder = { idx, dir ->
                        val next = idx + dir
                        if (idx in visible.indices && next in visible.indices) {
                            onReorderPages(fullOrderAfterSwap(visible, idx, next, hidden, true))
                        }
                    },
                    onHide = { onHidePage(it) },
                    onUnhide = null,
                    onSetDefault = { onSetDefaultPage(it) },
                    onDelete = { if (it.items.isEmpty()) onDeletePage(it.page) else pendingDelete = it },
                )

                if (hidden.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Hidden pages", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Hidden pages are skipped by the pager until unhidden.",
                        fontSize = 13.sp,
                        color = scheme.onSurface.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(8.dp))
                    PageCardGrid(
                        sectionPages = hidden,
                        colsU = colsU,
                        rowsU = rowsU,
                        labelFor = labelFor,
                        canDelete = canDelete,
                        selectedIconId = selectedIconId,
                        draggedId = draggedId,
                        dragTarget = dragTarget,
                        cardBounds = cardBounds,
                        iconBounds = iconBounds,
                        onIconTap = { item ->
                            selectedIconId = if (selectedIconId == item.id) null else item.id
                        },
                        onDragStart = { item, center ->
                            draggedId = item.id
                            dragCenter = center
                            selectedIconId = null
                        },
                        onDragMove = { delta ->
                            dragCenter = dragCenter?.plus(delta)
                            dragTarget = dragCenter?.let { c ->
                                cardBounds.entries.firstOrNull { (_, r) -> r.contains(c) }?.key
                            }
                        },
                        onDragEnd = {
                            val target = dragTarget
                            val id = draggedId
                            draggedId = null
                            dragCenter = null
                            dragTarget = null
                            if (id != null && target != null) {
                                val source = pages.firstOrNull { p -> p.items.any { it.id == id } }?.page
                                if (source != null && source != target) onMoveIcon(id, target)
                            }
                        },
                        onMoveHere = { targetPage ->
                            val id = selectedIconId
                            if (id != null) {
                                selectedIconId = null
                                onMoveIcon(id, targetPage)
                            }
                        },
                        onReorder = { idx, dir ->
                            val next = idx + dir
                            if (idx in hidden.indices && next in hidden.indices) {
                                onReorderPages(fullOrderAfterSwap(hidden, idx, next, visible, false))
                            }
                        },
                        onHide = null,
                        onUnhide = { onUnhidePage(it) },
                        onSetDefault = null,
                        onDelete = { if (it.items.isEmpty()) onDeletePage(it.page) else pendingDelete = it },
                    )
                }
            }
        }
    }

    pendingDelete?.let { page ->
        val adjacentDisplay = if (page.page > 0) page.page else page.page + 2
        AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete page ${page.page + 1}?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (page.items.size == 1) {
                            "1 item will move to page $adjacentDisplay."
                        } else {
                            "${page.items.size} items will move to page $adjacentDisplay."
                        },
                        fontSize = 14.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { val p = page.page; pendingDelete = null; onDeletePage(p) }) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
            )
        }
    }

@Composable
private fun PageCardGrid(
    sectionPages: List<OverviewPage>,
    colsU: Int,
    rowsU: Int,
    labelFor: (HomeItem) -> String,
    canDelete: Boolean,
    selectedIconId: String?,
    draggedId: String?,
    dragTarget: Int?,
    cardBounds: MutableMap<Int, Rect>,
    iconBounds: MutableMap<String, Rect>,
    onIconTap: (HomeItem) -> Unit,
    onDragStart: (HomeItem, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onMoveHere: (Int) -> Unit,
    onReorder: (index: Int, dir: Int) -> Unit,
    onHide: ((Int) -> Unit)?,
    onUnhide: ((Int) -> Unit)?,
    onSetDefault: ((Int) -> Unit)?,
    onDelete: (OverviewPage) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in sectionPages.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (page in row) {
                    val idx = sectionPages.indexOf(page)
                    val isTarget = dragTarget == page.page
                    Column(
                        Modifier.weight(1f)
                            .onGloballyPositioned { cardBounds[page.page] = it.boundsInRoot() },
                    ) {
                        PageThumbnail(
                            page = page,
                            colsU = colsU,
                            rowsU = rowsU,
                            labelFor = labelFor,
                            highlighted = isTarget,
                            selectedIconId = selectedIconId,
                            draggedId = draggedId,
                            iconBounds = iconBounds,
                            onIconTap = onIconTap,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Page ${page.page + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            if (page.isDefault) {
                                Text(
                                    "Default",
                                    fontSize = 11.sp,
                                    color = scheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            if (page.items.size == 1) "1 item" else "${page.items.size} items",
                            fontSize = 12.sp,
                            color = scheme.onSurface.copy(alpha = 0.6f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                enabled = idx > 0,
                                onClick = { onReorder(idx, -1) },
                            ) { Text("◀", fontSize = 13.sp) }
                            TextButton(
                                enabled = idx < sectionPages.lastIndex,
                                onClick = { onReorder(idx, 1) },
                            ) { Text("▶", fontSize = 13.sp) }
                        }
                        if (selectedIconId != null &&
                            page.items.none { it.id == selectedIconId }
                        ) {
                            OutlinedButton(
                                onClick = { onMoveHere(page.page) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Move here", fontSize = 13.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (onSetDefault != null && !page.isDefault) {
                                TextButton(onClick = { onSetDefault(page.page) }) {
                                    Text("Set default", fontSize = 13.sp)
                                }
                            }
                            if (onHide != null) {
                                TextButton(onClick = { onHide(page.page) }) {
                                    Text("Hide", fontSize = 13.sp)
                                }
                            }
                            if (onUnhide != null) {
                                TextButton(onClick = { onUnhide(page.page) }) {
                                    Text("Unhide", fontSize = 13.sp)
                                }
                            }
                        }
                        if (canDelete) {
                            TextButton(onClick = { onDelete(page) }) {
                                Text("Delete page", fontSize = 13.sp, color = scheme.error)
                            }
                        } else {
                            Text(
                                "Cannot delete the last page",
                                fontSize = 12.sp,
                                color = scheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                        // Keep row alignment stable when a card is alone.
                        // (The trailing spacer below the Row handles it.)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    page: OverviewPage,
    colsU: Int,
    rowsU: Int,
    labelFor: (HomeItem) -> String,
    highlighted: Boolean,
    selectedIconId: String?,
    draggedId: String?,
    iconBounds: MutableMap<String, Rect>,
    onIconTap: (HomeItem) -> Unit,
    onDragStart: (HomeItem, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val safeCols = colsU.coerceAtLeast(1)
    val safeRows = rowsU.coerceAtLeast(1)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = if (highlighted || page.isDefault) 2.dp else 1.dp,
                color = when {
                    highlighted -> scheme.primary
                    page.isDefault -> scheme.primary.copy(alpha = 0.6f)
                    else -> scheme.onSurface.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        val shown = page.items.sortedWith(compareBy({ it.row }, { it.col })).take(12)
        for (item in shown) {
            val w = maxWidth * (item.colSpan.coerceIn(1, safeCols).toFloat() / safeCols)
            val h = maxHeight * (item.rowSpan.coerceIn(1, safeRows).toFloat() / safeRows)
            val x = maxWidth * (item.col.coerceIn(0, safeCols - 1).toFloat() / safeCols)
            val y = maxHeight * (item.row.coerceIn(0, safeRows - 1).toFloat() / safeRows)
            val selected = selectedIconId == item.id
            val dragging = draggedId == item.id
            Box(
                Modifier.offset(x = x, y = y).size(w, h).padding(1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            dragging -> scheme.primary.copy(alpha = 0.35f)
                            selected -> scheme.primary.copy(alpha = 0.5f)
                            else -> scheme.primary.copy(alpha = 0.22f)
                        },
                    )
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = scheme.primary,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .onGloballyPositioned { iconBounds[item.id] = it.boundsInRoot() }
                    .clickable { onIconTap(item) }
                    .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                onDragStart(item, iconBounds[item.id]?.center ?: Offset.Zero)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragMove(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val label = labelFor(item)
                Text(
                    label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = scheme.onSurface,
                )
            }
        }
        if (page.items.size > shown.size) {
            Text(
                "+${page.items.size - shown.size} more",
                fontSize = 11.sp,
                color = scheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            )
        }
        if (page.items.isEmpty()) {
            Text(
                "Empty",
                fontSize = 12.sp,
                color = scheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
