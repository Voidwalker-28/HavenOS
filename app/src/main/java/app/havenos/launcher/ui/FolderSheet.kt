package app.havenos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import kotlinx.coroutines.launch

/**
 * Folder panel, M7 Wave 2 (home-screen v2 prompt §5.5, adapted: no
 * pages, no styles carousel).
 *
 * - Open: a floating glass panel, centred, scaling in over 280ms (§9
 *   folder timing; 100ms fade only under reduced motion). A true
 *   scale-from-the-folder's-position needs the folder row's window
 *   rect passed in by the host; the host does not pass one, so the
 *   panel scales from its own centre — the timing and the centred
 *   floating panel are real, the origin point is not.
 * - Grid: fixed 4 columns (a 4 x 3 viewport, scrolling when more than
 *   12 apps). No internal pagination or dots: the grid scrolls
 *   instead. The M6 variable-width handle is gone with it:
 *   panel columns no longer derive from the folder item's colSpan,
 *   which the old sheet used to reflow 3..6 columns.
 * - Title: editable, saves on Done / focus loss.
 * - Rearrange: long-press an icon, drag it over another icon — order
 *   updates live, commits on release. Long-press without a move does
 *   nothing (the old long-press-to-remove was an accidental-removal
 *   trap).
 * - Remove: drag an icon to the "Remove from folder" strip and release.
 * - Drag OUT to home: drag an icon to the "Move to home" strip and
 *   release — the member leaves the folder and is pinned to Favorites
 *   (the top of the scroll flow), if the dock has room; if the dock
 *   is full the move is refused and the member stays in the folder.
 *   This is the panel half of drag-out: a true finger-continuous drag
 *   from the panel onto a home row needs host-owned cross-surface
 *   drag state (Home.kt), which this file cannot own; the strip is
 *   the honest, working form until the host wires that.
 * - Styles (grid / stack / fan) are NOT exposed: only the grid layout
 *   is implemented, so per the doc there is nothing to offer.
 * - Empty folder: the folder row and record are deleted and the panel
 *   closes. One member left: the folder dissolves into that app
 *   (same M6 rule, unchanged).
 *
 * Signature is unchanged from M6 (the host call in Home.kt still
 * compiles); [onMoveToHome] / [onToast] are optional host hooks. When
 * [onMoveToHome] is supplied, the strip delegates to it instead of
 * pinning internally, so the host can place the app at a drop position
 * of its own choosing.
 */
@Composable
fun FolderSheet(
    app: HavenApp,
    folder: Folder,
    items: List<HomeItem>,
    appsByKey: Map<String, AppEntry>,
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onMoveToHome: ((String) -> Unit)? = null,
    onToast: (String) -> Unit = {},
) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val members = parseMembers(folder.membersJson)

    // Refs for detector closures: the drag detectors are created once
    // per cell key and never restart, so they must not close over
    // composition-time values.
    val membersNow by rememberUpdatedState(members)
    val itemsNow by rememberUpdatedState(items)
    val folderNow by rememberUpdatedState(folder)
    val moveToHomeNow by rememberUpdatedState(onMoveToHome)
    val toastNow by rememberUpdatedState(onToast)

    // Live display order during a drag; reset whenever the stored
    // members change (i.e. after a commit lands from the database).
    var displayOrder by remember(members) { mutableStateOf(members) }
    val displayEntries = displayOrder.mapNotNull { appsByKey[it] }

    // Drag state: which icon is dragged, its cumulative offset, which
    // strip (if any) the pointer is over, and every cell's window rect
    // plus both strips' rects (for hover detection).
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var dragOver by remember { mutableStateOf<FolderStrip>(FolderStrip.None) }
    val cellRects = remember { mutableStateMapOf<String, Rect>() }
    var removeRect by remember { mutableStateOf<Rect?>(null) }
    var homeRect by remember { mutableStateOf<Rect?>(null) }

    fun saveName() {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed != folderNow.name) {
            scope.launch { app.db.folders().upsert(folderNow.copy(name = trimmed)) }
        }
    }

    /** Shared tail of member removal: empty -> delete, one left -> dissolve. */
    suspend fun applyRemovalWithout(key: String) {
        val newMembers = membersNow - key
        when {
            newMembers.isEmpty() -> {
                app.db.homeItems().replaceAll(
                    itemsNow.filterNot { it.kind == ItemKind.FOLDER && it.payload == folderNow.id },
                )
                app.db.folders().delete(folderNow.id)
            }
            newMembers.size == 1 -> {
                val remaining = newMembers.first()
                val newItems = itemsNow.map {
                    if (it.kind == ItemKind.FOLDER && it.payload == folderNow.id) {
                        HomeItem(
                            id = "app:$remaining",
                            kind = ItemKind.APP,
                            page = it.page,
                            col = it.col,
                            row = it.row,
                            colSpan = 2,
                            rowSpan = 2,
                            payload = remaining,
                        )
                    } else {
                        it
                    }
                }
                app.db.homeItems().replaceAll(newItems)
                app.db.folders().delete(folderNow.id)
            }
            else -> {
                app.db.folders().upsert(folderNow.copy(membersJson = encodeMembers(newMembers)))
            }
        }
    }

    fun removeMember(key: String) {
        scope.launch {
            applyRemovalWithout(key)
            if (membersNow.size <= 2) onClose()
        }
    }

    /**
     * Move a member out of the folder onto home. With a host hook, the
     * host places it (and owns the folder bookkeeping); without one,
     * this file pins it to Favorites itself — dock-full refuses before
     * anything is removed, so a refused move never strands the app.
     */
    fun moveMemberToHome(key: String) {
        val hostHook = moveToHomeNow
        if (hostHook != null) {
            hostHook(key)
            return
        }
        val dock = state.dock
        if (key in dock) {
            // Already a favorite: just leave the folder.
            scope.launch {
                applyRemovalWithout(key)
                if (membersNow.size <= 2) onClose()
            }
            return
        }
        if (dock.size >= state.dockMax) {
            toastNow("Dock is full — remove a favorite first")
            return
        }
        scope.launch {
            applyRemovalWithout(key)
            app.settings.setDock(dock + key)
            if (membersNow.size <= 2) onClose()
        }
    }

    fun commitOrder() {
        val order = displayOrder
        if (order != membersNow) {
            scope.launch {
                app.db.folders().upsert(folderNow.copy(membersJson = encodeMembers(order)))
            }
        }
    }

    /**
     * Long-press + drag for one member icon. Taps pass through to the
     * cell's clickable (the detector consumes nothing before the
     * long-press); after the long-press it consumes moves, so the grid
     * never scrolls under a drag. Hovering another icon reorders live;
     * hovering a strip arms that strip; releasing over a strip fires
     * it, releasing anywhere else commits the order (or reverts when
     * nothing changed).
     */
    fun Modifier.folderDragCell(key: String): Modifier {
        val isDragged = dragKey == key
        return this
            .onGloballyPositioned { cellRects[key] = it.boundsInWindow() }
            .graphicsLayer {
                translationX = if (isDragged) dragDelta.x else 0f
                translationY = if (isDragged) dragDelta.y else 0f
                val s = if (isDragged) 1.06f else 1f
                scaleX = s
                scaleY = s
                shadowElevation = if (isDragged) 12.dp.toPx() else 0f
                shape = RoundedCornerShape(14.dp)
                clip = false
            }
            .zIndex(if (isDragged) 1f else 0f)
            .pointerInput(key) {
                var startRect: Rect? = null
                var startOffset = Offset.Zero
                var total = Offset.Zero
                while (true) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            startRect = cellRects[key]
                            startOffset = offset
                            total = Offset.Zero
                            dragKey = key
                            dragDelta = Offset.Zero
                            dragOver = FolderStrip.None
                            displayOrder = membersNow
                        },
                        onDrag = { _, amount ->
                            total += amount
                            dragDelta = total
                            val rect = startRect ?: return@detectDragGesturesAfterLongPress
                            val pointerWindow = rect.topLeft + startOffset + total
                            dragOver = when {
                                homeRect?.contains(pointerWindow) == true -> FolderStrip.Home
                                removeRect?.contains(pointerWindow) == true -> FolderStrip.Remove
                                else -> FolderStrip.None
                            }
                            if (dragOver == FolderStrip.None) {
                                val hovered = cellRects.entries.firstOrNull { (k, r) ->
                                    k != key && r.contains(pointerWindow)
                                }?.key
                                if (hovered != null) {
                                    val without = displayOrder.filter { it != key }
                                    val idx = without.indexOf(hovered)
                                    if (idx >= 0) {
                                        val next = without.toMutableList().also { it.add(idx, key) }
                                        if (next != displayOrder) displayOrder = next
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val removed = dragKey
                            val over = dragOver
                            dragKey = null
                            dragDelta = Offset.Zero
                            dragOver = FolderStrip.None
                            when {
                                over == FolderStrip.Remove && removed != null -> removeMember(removed)
                                over == FolderStrip.Home && removed != null -> moveMemberToHome(removed)
                                else -> commitOrder()
                            }
                        },
                        onDragCancel = {
                            dragKey = null
                            dragDelta = Offset.Zero
                            dragOver = FolderStrip.None
                        },
                    )
                }
            }
    }

    Dialog(onDismissRequest = onClose) {
        AnimatedVisibility(
            visible = true,
            enter = if (state.reduceMotion) {
                fadeIn(tween(100))
            } else {
                scaleIn(tween(280), initialScale = 0.85f, transformOrigin = TransformOrigin.Center) +
                    fadeIn(tween(280))
            },
            exit = if (state.reduceMotion) fadeOut(tween(100)) else scaleOut(tween(280)) + fadeOut(tween(280)),
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            saveName()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) saveName() },
                    )
                    Spacer(Modifier.height(16.dp))
                    if (displayEntries.isEmpty()) {
                        Text("No apps in this folder", color = palette.muted)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(displayEntries, key = { it.key }) { entry ->
                                AppCell(
                                    entry = entry,
                                    appRepo = app.appRepo,
                                    onClick = { onLaunch(entry) },
                                    onLongClick = null,
                                    modifier = Modifier.height(92.dp).folderDragCell(entry.key),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap to open · long-press + drag to rearrange · drag to a bar below to move out or remove",
                            color = palette.muted,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FolderStripBox(
                                label = if (dragOver == FolderStrip.Home) "Release to move to home"
                                else "Move to home",
                                active = dragOver == FolderStrip.Home,
                                onPositioned = { homeRect = it },
                                modifier = Modifier.weight(1f),
                            )
                            FolderStripBox(
                                label = if (dragOver == FolderStrip.Remove) "Release to remove"
                                else "Remove from folder",
                                active = dragOver == FolderStrip.Remove,
                                onPositioned = { removeRect = it },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class FolderStrip { None, Home, Remove }

@Composable
private fun FolderStripBox(
    label: String,
    active: Boolean,
    onPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .onGloballyPositioned { onPositioned(it.boundsInWindow()) }
            .background(
                if (active) palette.accent.copy(alpha = 0.25f)
                else palette.glass.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp),
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) palette.accent else palette.muted,
            fontSize = 12.sp,
        )
    }
}
