package app.havenos.launcher.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
// M9: `key`/`type` are extension properties on the KeyEvent value
// class (KeyEvent.android.kt) — they need explicit imports, unlike
// members. HomeA11y.kt already imports them; PagedHome did not, which
// broke every onKeyEvent/onPreviewKeyEvent lambda in this file
// (including Worker B's cell + pager handlers, repaired by this import).
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.encodeLabelOverrides
import app.havenos.launcher.data.gridCells
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.parseLabelOverrides
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.data.swipeActionFor
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import app.havenos.launcher.ui.settings.shell.TextFieldDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * M8 horizontal paged home — the replacement for ScrollHome's vertical
 * continuous-scroll surface (spec: HOME-SCREEN-V2-BUILDER-PROMPT §4.1,
 * §5.4, §8.3; FULL-PROMPT "Horizontal page swiping only").
 *
 * Minimal-only: no presets, no Look switching, no new network calls,
 * no new permissions. Accent recolours buttons/highlights only.
 *
 * What this file owns: the [HorizontalPager], the page indicator, the
 * per-page grids (coords from HomeItem.page/col/row in HALF-CELL units
 * — 1 cell = 2 units — via [rectOf]/[findSpot]/[resolveDrop]), the
 * Glance row, edit-mode drag (edge-turn, folder hover, drop zones),
 * the Library drag-in target, and the icon menu. What it does NOT own:
 * persistence. Every mutation goes through the host callbacks
 * ([onCommitItems], [onCreateFolder], [onAddToFolder], [onResizeItem])
 * so the host's 5s-undo (pushUndo) stays intact — nothing here writes
 * the database or the settings store, except the icon menu's Edit
 * label/icon rows, which write exactly what ScrollHome's menu wrote.
 *
 * The dock is host-owned (OneDock below the pager in Home.kt) and is
 * NOT rendered here; [pinned]/[dockKeys]/[onReorderFavorites] are kept
 * only so the host call site mirrors ScrollHome's.
 *
 * Parallax note: the wallpaper layer is host-owned (WallpaperLayer in
 * Home.kt draws behind this pager), so [parallaxOn] applies the ≤8%
 * pager-side parallax to page content (adjacent pages lag the swipe
 * by up to 8% of page width, settling at 0). True wallpaper-layer
 * parallax needs the coordinator to drive WallpaperLayer from the
 * pager offset — the offset lives in this file's pagerState, which is
 * intentionally not hoisted until that wiring exists.
 *
 * Folder-creation note: [onCreateFolder] takes member keys only (the
 * host's existing contract), so a folder created by an app-onto-app
 * drop is recorded by the host and its folder item lands where the
 * host puts it (page 0 today), while the two apps leave the grid via
 * [onCommitItems]. Placing that folder item at the occupant's cell
 * needs a host-side change to the onCreateFolder lambda (Worker B /
 * coordinator follow-up) — this file deliberately does not invent a
 * second folder record to work around it.
 *
 * Label stats: [labelCells] is the Worker C list (index
 * row * cols + col within the page grid, same list for every page —
 * the Legibility parallax note says ≤8% shift is under half a cell,
 * so no per-page resampling is needed). Null means "not analysed
 * yet": labels fall back to the mid-grey default (luminance 0.5,
 * variance 0 → near-black text, no pill), never a crash.
 */

private enum class PagerDropZone { NONE, REMOVE, APP_INFO, DOCK }

/** Recorded geometry of one pager page's grid area, in pixels. */
private data class GridGeom(
    val cols: Int,
    val rows: Int,
    val cellWPx: Float,
    val cellHPx: Float,
)

/** Live drag state, hoisted so cells, zones and overlays all read it. */
private data class DragState(
    val item: HomeItem,
    val fromGridPage: Int,
    val targetGridPage: Int,
    /** Finger position in page (viewport) coordinates, pixels. */
    val fingerPage: Offset,
    val moved: Boolean,
    val overCell: Pair<Int, Int>?,
    /** Drop units (half-cell): snapped to a cell, or raw when snap is off. */
    val overUnits: Pair<Int, Int>?,
    /** Folder payload hovered, or null. */
    val overFolderId: String?,
    val overZone: PagerDropZone,
    /** Item id under the finger (for the §3.3 400ms folder hold), or null. */
    val occupantId: String? = null,
    /** Uptime-ms when [occupantId] was first hovered. */
    val occupantSinceMs: Long = 0L,
)

private fun isUserApp(context: android.content.Context, pkg: String): Boolean = try {
    val flags = context.packageManager.getApplicationInfo(pkg, 0).flags
    flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
        flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
} catch (t: Throwable) {
    true
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagedHome(
    app: HavenApp,
    stateOverride: SettingsState? = null,
    pinned: List<AppEntry>,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onAddToHome: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    labelModeValue: String = "auto",
    flowItems: List<HomeItem> = emptyList(),
    foldersMap: Map<String, Folder> = emptyMap(),
    dockKeys: List<String> = pinned.map { it.key },
    editMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onSelectToggle: (String) -> Unit = {},
    onOpenFolder: (String) -> Unit = {},
    onEditHome: () -> Unit = {},
    onSwipeAction: (String) -> Unit = {},
    onEmptyLongPress: () -> Unit = {},
    onReorderFavorites: (List<String>) -> Unit = {},
    onCommitItems: (List<HomeItem>) -> Unit = {},
    onToast: (String) -> Unit = {},
    onCreateFolder: (List<String>) -> Unit = {},
    onAddToFolder: (String, String) -> Unit = { _, _ -> },
    onResizeItem: (HomeItem) -> Unit = {},
    // M8 additions (all defaulted; the M7 host call compiles unchanged
    // apart from the renamed onReorderFlow -> onCommitItems).
    todayOn: Boolean = true,
    parallaxOn: Boolean = true,
    indicatorOn: Boolean = true,
    hiddenPages: List<Int> = emptyList(),
    defaultPage: Int = 0,
    onShowPageOverview: () -> Unit = {},
    onAddPage: () -> Unit = {},
    labelCells: List<CellStats>? = null,
    // Today plumbing (host passes its existing values through).
    weather: TodayWeather? = null,
    weatherOff: Boolean = false,
    events: List<TodayEvent> = emptyList(),
    calendarGranted: Boolean = false,
    onRequestCalendarPermission: () -> Unit = {},
    suggested: List<AppEntry> = emptyList(),
    autoRotate: Boolean = false,
    // Edit-mode snap (the existing snapToGrid key, read by the host).
    snapToGrid: Boolean = true,
    // Floor for the page count: empty trailing pages have no items, so
    // the host passes the total page count (incl. empty pages) here.
    minPages: Int = 1,
    // M9 edit-mode host hooks (all defaulted; older call sites compile).
    // Dock drop: the host owns dock capacity (Dock.kt); PagedHome only
    // reports the hover/drop and refuses when [dockFull] is true.
    dockFull: Boolean = false,
    onDropToDock: (String) -> Unit = {},
    onDockHover: (Boolean) -> Unit = {},
    // v10 dock drag-out: the host removes the dropped app from the
    // dock. Only called on a successful grid add (never on
    // "Already on this page" / "No room"), so a failed drop leaves
    // the dock untouched. Default {} keeps older call sites compiling.
    onDockDragDrop: (AppEntry) -> Unit = {},
    onCurrentGridPage: (Int) -> Unit = {},
    // M9 Worker C: Quick search widget opens the host Search card.
    onOpenSearch: () -> Unit = {},
    // Add-page scroll: the host sets a grid page, PagedHome scrolls to
    // it, then reports consumption so the request does not repeat.
    focusGridPage: Int? = null,
    onFocusConsumed: () -> Unit = {},
    // Edit-mode TalkBack/keyboard actions (Doc 1 §3.11); host-owned.
    onA11yMove: (String, Int, Int) -> Unit = { _, _, _ -> },
    onA11yMoveToPage: (String) -> Unit = {},
    onA11yAddToDock: (String) -> Unit = {},
    onA11yHide: (String) -> Unit = {},
    onA11yResize: (String) -> Unit = {},
    onA11yRemove: (String) -> Unit = {},
    onExitEdit: () -> Unit = {},
    // M9 Worker B: host-hoisted pager state + hosted indicator.
    // The host passes its own PagerState so host layout (indicator
    // row, glance visibility, dock) and pager never disagree about
    // the current page; null keeps the internal fallback for older
    // call sites. indicatorHosted moves the indicator out of the
    // pager overlay into the host's layout flow (12dp above the
    // dock, never over content).
    hostPagerState: PagerState? = null,
    indicatorHosted: Boolean = false,
) {
    val state = stateOverride ?: LocalState.current
    val palette = LocalPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val landscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val sizeClass = rememberHomeSizeClass()
    val items = flowItems
    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val labelOverrides = remember(state.labelOverridesJson) {
        parseLabelOverrides(state.labelOverridesJson)
    }

    // Latest-value refs: gesture detectors are created once per cell
    // key and must never close over stale lists or callbacks.
    val itemsNow by rememberUpdatedState(items)
    val foldersNow by rememberUpdatedState(foldersMap)
    val appsByKeyNow by rememberUpdatedState(appsByKey)
    val stateNow by rememberUpdatedState(state)
    val commitNow by rememberUpdatedState(onCommitItems)
    val createFolderNow by rememberUpdatedState(onCreateFolder)
    val addToFolderNow by rememberUpdatedState(onAddToFolder)
    val toastNow by rememberUpdatedState(onToast)
    val swipeActionNow by rememberUpdatedState(onSwipeAction)
    val emptyLongPressNow by rememberUpdatedState(onEmptyLongPress)

    // Page model (M9 Worker B, Doc 2 §4.3): Today is a DEDICATED
    // pager page (index 0 when on) on EVERY size class, replacing
    // M8's Expanded Today-column-beside-grid on page 1. Grid pages
    // follow it. The shared helpers in Grid.kt are the single
    // definition — the host computes the same values from the same
    // inputs, so host and pager cannot disagree about the page count
    // or the mapping. Hidden grid pages are skipped in the pager
    // entirely (M8 integration): hiding a page removes it from
    // swiping instead of leaving a blank page. The overview refuses
    // to hide the last visible page, so the visible list is only ever
    // empty for a corrupt hiddenPagesJson — in which case hiding is
    // ignored rather than rendering a zero-page pager. A hidden
    // default page maps to the nearest visible page at or before it
    // (else the first visible page).
    val gridCount = pagedPageCount(items, minPages)
    val visibleGridPages = (0 until gridCount).filter { it !in hiddenPages }
    val pagerGridPages = visibleGridPages.ifEmpty { (0 until gridCount).toList() }
    val pagerPageCount = todayPagerPageCount(pagerGridPages.size, todayOn)
    fun gridPageForPager(pagerPage: Int): Int? =
        gridPageForTodayPager(pagerPage, pagerGridPages, todayOn)
    fun pagerPageForGrid(gridPage: Int): Int =
        pagerIndexForGrid(gridPage, pagerGridPages, todayOn)

    // Host-hoisted state wins when the host passes one (M9); the
    // internal state is the fallback for older call sites. No
    // conditional composable call: rememberPagerState always runs.
    val fallbackPagerState = rememberPagerState(
        initialPage = pagerPageForGrid(defaultPage).coerceIn(0, (pagerPageCount - 1).coerceAtLeast(0)),
        pageCount = { pagerPageCount },
    )
    val pagerState = hostPagerState ?: fallbackPagerState
    LaunchedEffect(defaultPage, pagerPageCount) {
        val target = pagerPageForGrid(defaultPage).coerceIn(0, (pagerPageCount - 1).coerceAtLeast(0))
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerPageCount) {
        if (pagerState.currentPage >= pagerPageCount && pagerPageCount > 0) {
            pagerState.scrollToPage(pagerPageCount - 1)
        }
    }

    // Recorded geometry, keyed by pager page. Written from
    // onGloballyPositioned (post-layout), read by drag hit-testing.
    val pageWindowRects = remember { mutableStateMapOf<Int, Rect>() }
    val gridWindowRects = remember { mutableStateMapOf<Int, Rect>() }
    val cellWindowRects = remember { mutableStateMapOf<String, Rect>() }
    val gridGeoms = remember { mutableStateMapOf<Int, GridGeom>() }
    fun geomForGrid(gridPage: Int): GridGeom {
        gridGeoms[pagerPageForGrid(gridPage)]?.let { return it }
        val (cols, rows) = state.gridCells(landscape)
        return GridGeom(cols, rows, 108f, 108f)
    }

    var drag by remember { mutableStateOf<DragState?>(null) }
    // M9 haptics (Doc 1 §3.3): tick on pickup + each snap. Captured by
    // the gesture lambdas below (never read inside pointerInput scope).
    val homeHaptics = rememberHomeHaptics(state.pressFeedback, state.hapticStrength)
    val dockHoverNow by rememberUpdatedState(onDockHover)
    val dropToDockNow by rememberUpdatedState(onDropToDock)
    val dockDragDropNow by rememberUpdatedState(onDockDragDrop)
    val currentGridPageNow by rememberUpdatedState(onCurrentGridPage)
    val focusConsumedNow by rememberUpdatedState(onFocusConsumed)
    var menuEntry by remember { mutableStateOf<AppEntry?>(null) }
    var labelEntry by remember { mutableStateOf<AppEntry?>(null) }
    var iconEntry by remember { mutableStateOf<AppEntry?>(null) }
    var libraryDropActive by remember { mutableStateOf(false) }

    val zoneBandPx = with(density) { 80.dp.toPx() }
    val edgePx = with(density) { 48.dp.toPx() }

    fun pageOffsetFor(windowOffset: Offset): Offset {
        val pageRect = pageWindowRects[pagerState.currentPage] ?: return windowOffset
        return Offset(windowOffset.x - pageRect.left, windowOffset.y - pageRect.top)
    }

    fun startDrag(item: HomeItem, gridPage: Int, fingerPage: Offset) {
        if (stateNow.lockLayout) {
            toastNow("Layout is locked")
            return
        }
        homeHaptics.gridSnapTick()
        drag = DragState(
            item = item,
            fromGridPage = gridPage,
            targetGridPage = gridPage,
            fingerPage = fingerPage,
            moved = false,
            overCell = null,
            overUnits = null,
            overFolderId = null,
            overZone = PagerDropZone.NONE,
            occupantId = null,
            occupantSinceMs = 0L,
        )
    }

    fun updateDrag(fingerPage: Offset) {
        val d = drag ?: return
        val pagerPage = pagerState.currentPage
        val targetGrid = gridPageForPager(pagerPage) ?: d.targetGridPage
        val geom = gridGeoms[pagerPage] ?: gridGeoms[pagerPageForGrid(d.targetGridPage)]
        val gridRect = gridWindowRects[pagerPage] ?: gridWindowRects[pagerPageForGrid(d.targetGridPage)]
        val pageRect = pageWindowRects[pagerPage]
        var overCell: Pair<Int, Int>? = null
        var overUnits: Pair<Int, Int>? = null
        var overFolder: String? = null
        var occupantId: String? = null
        if (geom != null && gridRect != null && pageRect != null) {
            val gx = fingerPage.x - (gridRect.left - pageRect.left)
            val gy = fingerPage.y - (gridRect.top - pageRect.top)
            overCell = cellFromOffset(gx, gy, geom.cellWPx, geom.cellHPx, geom.cols, geom.rows)
            if (overCell != null) {
                overUnits = if (snapToGrid) {
                    overCell.first * 2 to overCell.second * 2
                } else {
                    val maxCol = (geom.cols * 2 - d.item.colSpan).coerceAtLeast(0)
                    val maxRow = (geom.rows * 2 - d.item.rowSpan).coerceAtLeast(0)
                    (gx / geom.cellWPx * 2).roundToInt().coerceIn(0, maxCol) to
                        (gy / geom.cellHPx * 2).roundToInt().coerceIn(0, maxRow)
                }
                val cu = overCell.first * 2
                val ru = overCell.second * 2
                val occupant = itemsNow.firstOrNull {
                    it.id != d.item.id && it.page.coerceAtLeast(0) == targetGrid &&
                        rectOf(it).contains(cu, ru)
                }
                occupantId = occupant?.id
                if (occupant?.kind == ItemKind.FOLDER) overFolder = occupant.payload
            }
        }
        val pageWidth = pageRect?.width ?: 0f
        val pageHeight = pageRect?.height ?: 0f
        val zone = when {
            fingerPage.y < zoneBandPx ->
                if (fingerPage.x < pageWidth / 2f) PagerDropZone.REMOVE
                else if (d.item.kind == ItemKind.APP) PagerDropZone.APP_INFO
                else PagerDropZone.NONE
            // Below the pager lies the host dock: a drop target (§3.8).
            pageHeight > 0f && fingerPage.y >= pageHeight -> PagerDropZone.DOCK
            else -> PagerDropZone.NONE
        }
        if (overCell != null && overCell != d.overCell) homeHaptics.gridSnapTick()
        val nowMs = System.currentTimeMillis()
        val occupantSince = if (occupantId != null && occupantId == d.occupantId) {
            d.occupantSinceMs
        } else if (occupantId != null) {
            nowMs
        } else {
            0L
        }
        if (zone == PagerDropZone.DOCK != (d.overZone == PagerDropZone.DOCK)) {
            dockHoverNow(zone == PagerDropZone.DOCK)
        }
        drag = d.copy(
            targetGridPage = targetGrid,
            fingerPage = fingerPage,
            moved = true,
            overCell = overCell,
            overUnits = overUnits,
            overFolderId = overFolder,
            overZone = zone,
            occupantId = occupantId,
            occupantSinceMs = occupantSince,
        )
    }

    fun cancelDrag() {
        if (drag?.overZone == PagerDropZone.DOCK) dockHoverNow(false)
        drag = null
    }

    fun endDrag() {
        val d = drag ?: return
        drag = null
        if (d.overZone == PagerDropZone.DOCK) dockHoverNow(false)
        when (d.overZone) {
            PagerDropZone.DOCK -> {
                if (d.item.kind != ItemKind.APP) {
                    toastNow("Only apps go in the dock")
                } else if (dockFull) {
                    toastNow("Dock is full")
                } else {
                    dropToDockNow(d.item.payload)
                }
                return
            }
            PagerDropZone.REMOVE -> {
                commitNow(itemsNow.filterNot { it.id == d.item.id })
                toastNow("Removed")
                return
            }
            PagerDropZone.APP_INFO -> {
                val entry = appsByKeyNow[d.item.payload]
                if (entry != null) {
                    try {
                        if (isUserApp(context, entry.pkg)) {
                            context.startActivity(
                                Intent(Intent.ACTION_DELETE, Uri.parse("package:" + entry.pkg)),
                            )
                        } else {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + entry.pkg),
                                ),
                            )
                        }
                    } catch (t: Throwable) {
                        // system activity unavailable
                    }
                }
                return
            }
            PagerDropZone.NONE -> Unit
        }
        val units = d.overUnits ?: return // dropped outside any grid: cancel, item stays
        val targetGrid = d.targetGridPage
        val geom = geomForGrid(targetGrid)
        val colsU = geom.cols * 2
        val rowsU = geom.rows * 2
        val occupant = itemsNow.firstOrNull {
            it.id != d.item.id && it.page.coerceAtLeast(0) == targetGrid &&
                rectOf(it).contains(units.first, units.second)
        }
        if (occupant?.kind == ItemKind.CLOCK_PAGE) {
            toastNow("Can't drop on the clock")
            return
        }
        val moved = itemsNow.map {
            if (it.id == d.item.id) it.copy(page = targetGrid) else it
        }
        // M9 §3.3 policy: hold time decides folder vs row shift for
        // app-on-app; widgets never overlap (nearest free fit or back).
        val holdMs = if (d.occupantId != null && d.occupantId == occupant?.id) {
            System.currentTimeMillis() - d.occupantSinceMs
        } else {
            0L
        }
        val result = resolveDropEdit(
            moved, foldersNow, d.item.id, units.first, units.second,
            colsU, rowsU, UUID.randomUUID().toString(), holdMs,
        )
        when {
            result.createdFolder != null -> {
                // App onto app: the host records the folder (its
                // contract takes member keys only); the two apps leave
                // the grid here. See the file header for the placement
                // follow-up on the host-created folder item.
                if (occupant != null && occupant.kind == ItemKind.APP && d.item.kind == ItemKind.APP) {
                    commitNow(itemsNow.filterNot { it.id == d.item.id || it.id == occupant.id })
                    createFolderNow(listOf(occupant.payload, d.item.payload))
                    toastNow("Folder created")
                }
            }
            result.updatedFolder != null -> {
                commitNow(result.items)
                addToFolderNow(result.updatedFolder.id, d.item.payload)
                toastNow("Added to folder")
            }
            result.items != itemsNow -> commitNow(result.items)
            else -> Unit // dropped on itself / no room: nothing changes
        }
    }

    // Edge-turn (§8): dragging near the left/right edge for 600ms turns
    // the page (grid pages only — the Today page is skipped, a drag
    // can never land on it). The page-change effect below re-runs
    // updateDrag so the target grid follows the new page.
    val dragEdge = run {
        val d = drag ?: return@run 0
        val pageWidth = pageWindowRects[pagerState.currentPage]?.width ?: 0f
        when {
            pageWidth <= 0f -> 0
            d.fingerPage.x < edgePx -> -1
            d.fingerPage.x > pageWidth - edgePx -> 1
            else -> 0
        }
    }
    LaunchedEffect(drag?.item?.id, dragEdge) {
        if (drag != null && dragEdge != 0) {
            delay(600)
            var next = pagerState.currentPage + dragEdge
            while (next in 0 until pagerPageCount && gridPageForPager(next) == null) {
                next += dragEdge
            }
            if (next in 0 until pagerPageCount && next != pagerState.currentPage) {
                pagerState.animateScrollToPage(next, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        val d = drag
        if (d != null) updateDrag(d.fingerPage)
        gridPageForPager(pagerState.currentPage)?.let { currentGridPageNow(it) }
    }
    // M9 Worker B (Doc 2 §7): one tick per page settle, never per
    // scroll delta. The first composition (initial page) is silent.
    var prevPagerPage by remember { mutableStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != prevPagerPage) {
            homeHaptics.pageTurnTick()
            prevPagerPage = pagerState.currentPage
        }
    }
    // M9: Add-page scroll — the host requests a grid page, we scroll to
    // it once and report consumption.
    LaunchedEffect(focusGridPage) {
        val target = focusGridPage
        if (target != null) {
            val pagerPage = pagerPageForGrid(target)
            if (pagerPage in 0 until pagerPageCount) {
                pagerState.animateScrollToPage(pagerPage, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
            }
            focusConsumedNow()
        }
    }
    // M9: the old 400ms folder-hover-open is REMOVED. Doc 1 §3.3 says a
    // drop on a folder adds to it, and the 400ms hold is the app-on-app
    // folder-creation hold (tracked in DragState.occupantSinceMs) — a
    // hover-open would steal slow folder drops before they land.

    // ---------------------------------------------------------------
    // Gesture detectors. Single-owner rule, copied from ScrollHome's
    // M6 model: in edit mode ONLY the ≡ handle drags (taps select);
    // outside edit mode ONE long-press detector per cell owns tap +
    // long-press + drag, and the menu opens only on release without
    // a move — never underneath an active drag.
    // ---------------------------------------------------------------

    fun cellPointerModifier(
        item: HomeItem,
        gridPage: Int,
        entry: AppEntry?,
        onTap: () -> Unit,
        onTapAt: ((Offset) -> Unit)?,
        onLongPressNoMove: () -> Unit,
    ): Modifier = Modifier.pointerInput(item.id, editMode) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown()
                val slop = viewConfiguration.touchSlop
                // Doc 1 §3.3: long-press 350ms starts a drag (the system
                // default is longer and varies by device).
                val timeout = EDIT_LONG_PRESS_MS
                val swipeAction = entry?.let { swipeActionFor(stateNow, it.pkg) }
                val sidePx = 32.dp.toPx()
                // Phase 1: up / consumed / slop-cross, or timeout.
                val phase1 = withTimeoutOrNull(timeout) {
                    var result: Int? = null // 0 = tap, 1 = abort
                    while (result == null) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: continue
                        if (!change.pressed) {
                            result = 0
                        } else if (change.isConsumed) {
                            result = 1
                        } else {
                            val delta = change.position - down.position
                            if (delta.getDistance() > slop) {
                                // Per-icon swipe-up flick (opt-in only:
                                // swipeActionFor returns null unless the
                                // user configured one for this package).
                                // A fast, mostly-vertical upward move
                                // before the timeout is watched as a
                                // flick; anything else aborts to the
                                // pager, exactly like ScrollHome.
                                if (swipeAction != null && delta.y < -slop &&
                                    kotlin.math.abs(delta.x) <= sidePx
                                ) {
                                    var totalY = delta.y
                                    var totalX = delta.x
                                    var fired = false
                                    var flickDone = false
                                    val startMs = down.uptimeMillis
                                    while (!flickDone) {
                                        val ev2 = awaitPointerEvent()
                                        val ch2 = ev2.changes.firstOrNull { it.id == down.id }
                                            ?: continue
                                        if (!ch2.pressed) {
                                            val dt = ch2.uptimeMillis - startMs
                                            if (dt <= 300L && totalY <= -56.dp.toPx() &&
                                                totalY >= -200.dp.toPx() &&
                                                kotlin.math.abs(totalX) <= sidePx
                                            ) {
                                                fired = true
                                            }
                                            flickDone = true
                                        } else if (ch2.isConsumed) {
                                            flickDone = true
                                        } else {
                                            totalY += ch2.position.y - ch2.previousPosition.y
                                            totalX += ch2.position.x - ch2.previousPosition.x
                                            if (ch2.uptimeMillis - startMs > 300L ||
                                                totalY > 8.dp.toPx() ||
                                                kotlin.math.abs(totalX) > sidePx ||
                                                totalY < -200.dp.toPx()
                                            ) {
                                                flickDone = true
                                            } else {
                                                ch2.consume()
                                            }
                                        }
                                    }
                                    if (fired) swipeActionNow(swipeAction)
                                }
                                result = 1
                            }
                        }
                    }
                    result
                }
                if (phase1 == 0) {
                    if (onTapAt != null) onTapAt(down.position) else onTap()
                    continue
                }
                if (phase1 == 1) continue
                // Timeout: finger still down. Lift, unless locked.
                if (stateNow.lockLayout) {
                    toastNow("Layout is locked")
                    continue
                }
                // Pickup tick fires inside startDrag (one tick, not two).
                val cellRect = cellWindowRects[item.id]
                val pageRect = pageWindowRects[pagerState.currentPage]
                val startFingerWindow = if (cellRect != null) {
                    Offset(cellRect.left + down.position.x, cellRect.top + down.position.y)
                } else {
                    down.position
                }
                val startFingerPage = if (pageRect != null) {
                    Offset(startFingerWindow.x - pageRect.left, startFingerWindow.y - pageRect.top)
                } else {
                    down.position
                }
                startDrag(item, gridPage, startFingerPage)
                if (drag == null) continue
                var fingerWindow = startFingerWindow
                var dragged = false
                var aborted = false
                var done = false
                while (!done) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: continue
                    if (!change.pressed) {
                        done = true
                    } else if (change.isConsumed) {
                        aborted = true
                        done = true
                    } else {
                        val dlt = change.position - change.previousPosition
                        if (dlt != Offset.Zero) {
                            change.consume()
                            dragged = true
                            fingerWindow += dlt
                            updateDrag(pageOffsetFor(fingerWindow))
                        }
                    }
                }
                when {
                    aborted -> cancelDrag()
                    dragged -> endDrag()
                    else -> {
                        cancelDrag()
                        onLongPressNoMove()
                    }
                }
            }
        }
    }

    fun handlePointerModifier(item: HomeItem, gridPage: Int): Modifier =
        Modifier.pointerInput(item.id) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown()
                    val slop = viewConfiguration.touchSlop
                    var started = false
                    var fingerWindow: Offset? = null
                    while (!started) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: continue
                        if (!change.pressed) break
                        if (change.isConsumed) break
                        if ((change.position - down.position).getDistance() > slop) {
                            change.consume()
                            val cellRect = cellWindowRects[item.id]
                            fingerWindow = if (cellRect != null) {
                                Offset(cellRect.left + change.position.x, cellRect.top + change.position.y)
                            } else {
                                change.position
                            }
                            started = true
                            startDrag(item, gridPage, pageOffsetFor(fingerWindow!!))
                        }
                    }
                    if (!started) continue
                    var aborted = false
                    var done = false
                    while (!done) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: continue
                        if (!change.pressed) {
                            done = true
                        } else if (change.isConsumed) {
                            aborted = true
                            done = true
                        } else {
                            val dlt = change.position - change.previousPosition
                            if (dlt != Offset.Zero) {
                                change.consume()
                                fingerWindow = (fingerWindow ?: Offset.Zero) + dlt
                                updateDrag(pageOffsetFor(fingerWindow!!))
                            }
                        }
                    }
                    if (aborted) cancelDrag() else endDrag()
                }
            }
        }

    // ---------------------------------------------------------------
    // Labels (legibility engine): mode + per-cell stats -> what to draw.
    // M10 (BUG-012): missing stats mean the wallpaper is UNKNOWN
    // (system mode: the OS bitmap is unreadable), so the label never
    // guesses a bare text colour against it — it forces the pill,
    // which is dark behind white text and reads on any wallpaper.
    // ---------------------------------------------------------------

    @Composable
    fun PagerLabel(text: String, cellCol: Int, cellRow: Int, cols: Int) {
        if (!state.labelsOn) return
        val idx = cellRow * cols + cellCol
        val cellStats = labelCells?.getOrNull(idx)
        val stats = cellStats ?: CellStats(0.5f, 0f)
        val baseStyle = labelStyleFor(
            labelModeFromKey(labelModeValue),
            stats.luminance,
            stats.variance,
        )
        val style = if (cellStats == null) baseStyle.copy(needsPill = true) else baseStyle
        val color = style.textColor ?: return
        if (style.needsPill) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    // M9 Worker B: a black pill under near-black text
                    // is invisible on a busy light cell; the pill
                    // follows the text colour instead.
                    .background(if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text,
                    color = color,
                    fontFamily = LocalFont.current,
                    fontSize = state.labelSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                text,
                color = color,
                fontFamily = LocalFont.current,
                fontSize = state.labelSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = LEGIBILITY_SHADOW),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }

    @Composable
    fun GridCell(
        item: HomeItem,
        gridPage: Int,
        cols: Int,
        rows: Int,
        cellW: androidx.compose.ui.unit.Dp,
        cellH: androidx.compose.ui.unit.Dp,
        pageItems: List<HomeItem> = emptyList(),
    ) {
        val cellCol = item.col / 2
        val cellRow = item.row / 2
        val xDp = cellW * (item.col / 2f)
        val yDp = cellH * (item.row / 2f)
        val wDp = cellW * (item.colSpan / 2f)
        val hDp = cellH * (item.rowSpan / 2f)
        val dragNow = drag
        val isDragged = dragNow?.item?.id == item.id
        val isTarget = dragNow?.targetGridPage == gridPage && dragNow.overCell == (cellCol to cellRow)
        val isSelected = item.id in selectedIds
        val appEntry: AppEntry? = if (item.kind == ItemKind.APP) appsByKey[item.payload] else null
        if (item.kind == ItemKind.APP && appEntry == null) return // uninstalled: never crash
        val folder: Folder? = if (item.kind == ItemKind.FOLDER) foldersMap[item.payload] else null
        if (item.kind == ItemKind.FOLDER && folder == null) return
        val members = remember(folder, appsByKey) {
            if (folder == null) emptyList()
            else parseMembers(folder.membersJson).mapNotNull { appsByKey[it] }
        }
        val isLargeFolder = item.colSpan >= 4 && item.rowSpan >= 4
        // M9 Worker B hover label (Doc 2 §2.9): app label or folder
        // name; widgets get no tooltip (empty label hides it).
        val hoverLabel = when (item.kind) {
            ItemKind.APP -> labelOverrides[appEntry?.key] ?: appEntry?.label ?: ""
            ItemKind.FOLDER -> folder?.name ?: ""
            else -> ""
        }
        val tapAction: () -> Unit = when (item.kind) {
            ItemKind.APP -> ({ appEntry?.let { onLaunch(it) } })
            ItemKind.FOLDER -> ({ folder?.let { onOpenFolder(it.id) } })
            ItemKind.CLOCK -> ({}) // ClockWidget handles its own tap
            else -> ({})
        }
        // Large folders (2x2 cells): tapping a quadrant launches that
        // member directly; tapping elsewhere opens the folder.
        val tapAtAction: ((Offset) -> Unit)? = if (item.kind == ItemKind.FOLDER && isLargeFolder) {
            { pos ->
                val rect = cellWindowRects[item.id]
                if (rect != null && rect.width > 0f && rect.height > 0f) {
                    val qCol = (pos.x / (rect.width / 2f)).toInt().coerceIn(0, 1)
                    val qRow = (pos.y / (rect.height / 2f)).toInt().coerceIn(0, 1)
                    val member = members.getOrNull(qRow * 2 + qCol)
                    if (member != null) onLaunch(member) else folder?.let { onOpenFolder(it.id) }
                } else {
                    folder?.let { onOpenFolder(it.id) }
                }
            }
        } else {
            null
        }
        val longPressAction: () -> Unit = when (item.kind) {
            ItemKind.APP -> ({ appEntry?.let { menuEntry = it } })
            ItemKind.FOLDER -> onEditHome
            else -> ({ onEditHome(); onSelectToggle(item.id) })
        }
        val gesture = if (editMode) {
            // Doc 1 §1.8/§3.3: in edit mode a tap SELECTS, never launches.
            Modifier.clickable { onSelectToggle(item.id) }
        } else {
            cellPointerModifier(item, gridPage, appEntry, tapAction, tapAtAction, longPressAction)
        }
        val borderMod = when {
            isTarget -> Modifier.border(2.dp, palette.accent, RoundedCornerShape(12.dp))
            editMode && isSelected -> Modifier.border(2.dp, palette.accent, RoundedCornerShape(12.dp))
            // M9 Worker B: the old bare comparison was true for
            // EVERY app icon with no drag active (null == null),
            // painting a gold ring on all cells always. A drag must
            // actually be active and hovering this folder.
            dragNow != null && dragNow.overFolderId != null && dragNow.overFolderId == folder?.id -> Modifier.border(2.dp, palette.accent, RoundedCornerShape(12.dp))
            else -> Modifier
        }
        // M9 edit-mode a11y (Doc 1 §3.11): TalkBack actions + keyboard.
        // Space selects via the clickable above; Delete removes (undoable);
        // Esc clears selection first, then exits (host onExitEdit).
        val a11yMod: Modifier = if (editMode) {
            val a11yLabel = when (item.kind) {
                ItemKind.APP -> appEntry?.label
                ItemKind.FOLDER -> folder?.name
                else -> null
            }
            Modifier
                .homeIconActions(
                    a11yLabel,
                    HomeIconActions(
                        onMove = { dx, dy -> onA11yMove(item.id, dx, dy) },
                        onMoveToPage = { onA11yMoveToPage(item.id) },
                        onResize = if (isResizable(item)) ({ onA11yResize(item.id) }) else null,
                        onAddToDock = if (item.kind == ItemKind.APP) ({ onA11yAddToDock(item.id) }) else null,
                        onRemove = { onA11yRemove(item.id) },
                        onEditLabel = if (item.kind == ItemKind.APP && appEntry != null) {
                            { labelEntry = appEntry }
                        } else {
                            null
                        },
                        onHide = if (item.kind == ItemKind.APP) ({ onA11yHide(item.id) }) else null,
                    ),
                )
                .homeIconKeys(
                    onOpen = { onSelectToggle(item.id) },
                    onOpenMenu = longPressAction,
                    onCloseOverlay = onExitEdit,
                    onMoveInEditMode = { dx, dy -> onA11yMove(item.id, dx, dy) },
                    editMode = true,
                )
                .onPreviewKeyEvent { event: KeyEvent ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Delete || event.key == Key.Backspace)
                    ) {
                        onA11yRemove(item.id)
                        true
                    } else {
                        false
                    }
                }
        } else {
            Modifier
        }
        // M9 Worker B (Doc 2 §2.9/§7): keyboard + mouse for grid
        // cells outside edit mode (edit mode already has
        // homeIconActions/homeIconKeys above). Right-click sits
        // BEFORE the gesture detector in the chain, as its doc
        // requires. Press scale is NOT wired here: press handling
        // lives inside the custom gesture detector (drag-adjacent;
        // do not touch), so the 0.94 press scale stays dock-only
        // for now (see final report).
        val cellInteraction = remember { MutableInteractionSource() }
        val cellHovered by cellInteraction.collectIsHoveredAsState()
        val cellExtraMod: Modifier = if (!editMode) {
            Modifier
                .hoverable(cellInteraction)
                .homeRightClickMenu(longPressAction)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        tapAction()
                        true
                    } else {
                        false
                    }
                }
        } else {
            Modifier
        }
        Box(
            Modifier
                .then(a11yMod)
                .offset(x = xDp, y = yDp)
                .size(wDp, hDp)
                .onGloballyPositioned { cellWindowRects[item.id] = it.boundsInWindow() }
                .graphicsLayer {
                    alpha = if (isDragged) 0.4f else 1f
                    // Doc 2 §4.4: idle edit items are lifted (+2dp
                    // shadow), with NO outlines — only the selected
                    // item gets the accent ring (borderMod above).
                    shadowElevation = if (editMode) with(density) { 2.dp.toPx() } else 0f
                }
                .then(borderMod)
                .then(cellExtraMod)
                .then(gesture),
        ) {
            when (item.kind) {
                ItemKind.APP -> {
                    val entry = appEntry!!
                    Column(
                        Modifier.fillMaxSize().padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box {
                            AppIconImage(entry, app.appRepo, state.iconDp().dp)
                            BadgeOverlay(entry.pkg)
                        }
                        PagerLabel(
                            labelOverrides[entry.key] ?: entry.label,
                            cellCol, cellRow, cols,
                        )
                    }
                }
                ItemKind.FOLDER -> {
                    val f = folder!!
                    Column(
                        Modifier.fillMaxSize().padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f, fill = true).fillMaxWidth(),
                        ) {
                            if (isLargeFolder) {
                                Column(
                                    Modifier.fillMaxSize().padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    for (rowMembers in members.take(4).chunked(2)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            for (member in rowMembers) {
                                                AppIconImage(member, app.appRepo, 48.dp)
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                Column(
                                    Modifier.fillMaxSize().padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    for (rowMembers in members.take(9).chunked(3)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            for (member in rowMembers) {
                                                AppIconImage(member, app.appRepo, 22.dp)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                        PagerLabel(f.name, cellCol, cellRow, cols)
                    }
                }
                ItemKind.CLOCK -> {
                    WidgetContent(
                        app, item, appsByKey, onLaunch,
                        Modifier.fillMaxSize().padding(4.dp), editMode,
                        onOpenSearch, { menuEntry = it }, pageItems, cols * 2, rows * 2,
                    )
                }
                ItemKind.TP_WIDGET -> {
                    TpWidgetView(app, item, Modifier.fillMaxSize())
                }
                else -> {
                    // M9 Worker C: built-ins render live content (the
                    // old branch rendered a labelled card — the "blank
                    // dark rectangles named Weather/Agenda" defect).
                    // In edit mode they keep rendering live content
                    // under the selection ring, never name labels.
                    WidgetContent(
                        app, item, appsByKey, onLaunch,
                        Modifier.fillMaxSize().padding(4.dp), editMode,
                        onOpenSearch, { menuEntry = it }, pageItems, cols * 2, rows * 2,
                    )
                }
            }
            // M9 Worker B: hover tooltip (mouse only; hidden when
            // the label is empty, i.e. for widgets).
            if (!editMode && hoverLabel.isNotEmpty()) {
                HomeHoverTooltip(label = hoverLabel, visible = cellHovered)
            }
            // M9 Worker B (Doc 2 §4.4): the handle belongs ONLY to
            // the selected item — tap to select first, then drag the
            // handle. A handle on every icon was the second half of
            // the "gold dots everywhere" report. Drag mechanics
            // (handlePointerModifier) are untouched.
            if (editMode && isSelected) {
                // M5/M6 drag handle: drags start ONLY here in edit mode.
                // 48dp touch target (Doc 1 §3.11); the 24dp pill is visual.
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp)
                        .then(handlePointerModifier(item, gridPage)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("≡", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // One grid page's cells.
    // ---------------------------------------------------------------

    @Composable
    fun GridArea(pagerPage: Int, gridPage: Int, areaModifier: Modifier) {
        // 28dp bottom reserve ONLY when the page indicator overlays
        // the bottom of the pager (legacy callers). When the host
        // lays the indicator out as its own row (indicatorHosted),
        // the grid takes the full safe area — the reserve would
        // otherwise shrink every page for nothing.
        BoxWithConstraints(areaModifier.padding(bottom = if (indicatorHosted) 0.dp else 28.dp)) {
            val contentW = maxWidth.value
            val contentH = maxHeight.value
            val (cols, rows) = remember(state, landscape, sizeClass, contentW, contentH) {
                pagedGridCells(state, landscape, sizeClass, contentW, contentH, state.iconDp())
            }
            val cellW = maxWidth / cols
            val cellH = maxHeight / rows
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        gridWindowRects[pagerPage] = coords.boundsInWindow()
                        gridGeoms[pagerPage] = GridGeom(
                            cols, rows,
                            with(density) { cellW.toPx() },
                            with(density) { cellH.toPx() },
                        )
                    }
                    .pointerInput(gridPage) {
                        // Empty-space long-press: fires ONLY for cells
                        // no item occupies. A press on a cell is owned
                        // by that cell's detector (lift/drag/menu),
                        // never by this background detector. Geometry
                        // comes from the recorded grid geom (updated on
                        // every layout), never from composition-time
                        // captures, so rotation can't leave it stale.
                        detectTapGestures(
                            onLongPress = { offset ->
                                val geom = gridGeoms[pagerPage]
                                val hit = geom?.let {
                                    cellFromOffset(
                                        offset.x, offset.y,
                                        it.cellWPx, it.cellHPx, it.cols, it.rows,
                                    )
                                }
                                val occupied = hit != null && itemsNow.any { item ->
                                    item.page.coerceAtLeast(0) == gridPage &&
                                        rectOf(item).contains(hit.first * 2, hit.second * 2)
                                }
                                if (!occupied) emptyLongPressNow()
                            },
                        )
                    },
            ) {
                val pageItems = items.filter { it.page.coerceAtLeast(0) == gridPage }
                for (item in pageItems) {
                    GridCell(item, gridPage, cols, rows, cellW, cellH, pageItems)
                }
                if (drag != null) {
                    // Doc 2 §4.4: soft grid dots — plus-marks at cell
                    // corners, 30% opacity — while dragging.
                    Canvas(Modifier.fillMaxSize()) {
                        val cw = size.width / cols
                        val ch = size.height / rows
                        val dot = palette.accent.copy(alpha = 0.3f)
                        val half = 4.dp.toPx()
                        val stroke = 1.dp.toPx()
                        for (c in 0..cols) {
                            for (r in 0..rows) {
                                val x = c * cw
                                val y = r * ch
                                drawLine(dot, Offset(x - half, y), Offset(x + half, y), strokeWidth = stroke)
                                drawLine(dot, Offset(x, y - half), Offset(x, y + half), strokeWidth = stroke)
                            }
                        }
                    }
                }
            }
        }
    }


    // ---------------------------------------------------------------
    // Pager tree. The dock lives in the host below this pager
    // (dock zone: bottom inset + 16dp, 72–80dp — Dock.kt untouched).
    // ---------------------------------------------------------------

    BoxWithConstraints(modifier = modifier) {
        val pageWidthPx = with(density) { maxWidth.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                // M9 Worker B (Doc 2 §2.9): the pager announces
                // "Page N of M" (live region, polite) and arrow keys
                // move between pages (instant under reduced motion).
                // Tab moves focus between cells; Enter on a focused
                // cell opens it (see GridCell).
                .semantics(mergeDescendants = false) {
                    contentDescription = "Page ${pagerState.currentPage + 1} of $pagerPageCount"
                    liveRegion = LiveRegionMode.Polite
                }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val target = when (event.key) {
                        Key.DirectionLeft -> (pagerState.currentPage - 1).coerceAtLeast(0)
                        Key.DirectionRight -> (pagerState.currentPage + 1).coerceAtMost((pagerPageCount - 1).coerceAtLeast(0))
                        else -> return@onKeyEvent false
                    }
                    if (target == pagerState.currentPage) return@onKeyEvent false
                    scope.launch {
                        if (state.reduceMotion) pagerState.scrollToPage(target)
                        else pagerState.animateScrollToPage(target, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
                    }
                    true
                }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        // v10: a dock drag-out carries DOCK_DRAG_MIME
                        // (same payload format as the Library MIME);
                        // a Library drag carries LIBRARY_DRAG_MIME.
                        event.mimeTypes().contains(LIBRARY_DRAG_MIME) ||
                            event.mimeTypes().contains(DOCK_DRAG_MIME)
                    },
                    target = object : DragAndDropTarget {
                        override fun onStarted(event: DragAndDropEvent) {
                            libraryDropActive = true
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            libraryDropActive = false
                        }

                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val dragEvent = event.toAndroidDragEvent()
                            val isDockDrag = dragEvent.clipDescription
                                ?.hasMimeType(DOCK_DRAG_MIME) == true
                            val text = try {
                                dragEvent.clipData?.getItemAt(0)?.text?.toString()
                            } catch (t: Throwable) {
                                null
                            } ?: return false
                            val payload = decodeLibraryDragPayload(text) ?: return false
                            val entry = appsByKeyNow[payload.appKey]
                                ?: apps.firstOrNull { it.pkg == payload.pkg && it.cls == payload.cls }
                                ?: run { toastNow("App not found"); return false }
                            // A drop on the Today page lands on the
                            // first home page (a drag can never land
                            // on Today itself).
                            val gridPage = gridPageForPager(pagerState.currentPage) ?: 0
                            val geom = geomForGrid(gridPage)
                            val colsU = geom.cols * 2
                            val rowsU = geom.rows * 2
                            if (itemsNow.any {
                                    it.kind == ItemKind.APP &&
                                        it.page.coerceAtLeast(0) == gridPage &&
                                        it.payload == entry.key
                                }
                            ) {
                                toastNow("Already on this page")
                                return true
                            }
                            val spot = findSpot(itemsNow, gridPage, 2, 2, colsU, rowsU)
                                ?: run { toastNow("No room on this page"); return true }
                            commitNow(
                                itemsNow + HomeItem(
                                    "app:${entry.key}", ItemKind.APP, gridPage,
                                    spot.first, spot.second, 2, 2, entry.key,
                                ),
                            )
                            // v10 dock move: the grid add above goes
                            // through the host's onCommitItems (which
                            // pushes undo), then the host removes the
                            // app from the dock. Slot-aware insert is
                            // NOT possible here — the platform drag
                            // event exposes no drop coordinates, so
                            // first-free-cell is the existing Library
                            // drop behaviour, reused unchanged.
                            if (isDockDrag) {
                                dockDragDropNow(entry)
                                toastNow("Moved to home")
                            } else {
                                toastNow("Added to home")
                            }
                            return true
                        }
                    },
                ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { pagerPage ->
                val pageOffset =
                    (pagerState.currentPage - pagerPage) + pagerState.currentPageOffsetFraction
                val parallaxX =
                    if (parallaxOn && !state.reduceMotion) pageOffset * pageWidthPx * 0.08f else 0f
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = parallaxX }
                        .onGloballyPositioned { pageWindowRects[pagerPage] = it.boundsInWindow() },
                ) {
                    val gridPage = gridPageForPager(pagerPage)
                    if (gridPage == null) {
                        // Today page (M9, Doc 2 §4.3): a dedicated page
                        // on every size class, with the dock visible
                        // below it — the host lays the dock out under
                        // this pager on all pages, Today included.
                        TodayPage(
                            appRepo = app.appRepo,
                            onLaunch = onLaunch,
                            weather = weather,
                            weatherOff = weatherOff,
                            events = events,
                            calendarGranted = calendarGranted,
                            onRequestCalendarPermission = onRequestCalendarPermission,
                            suggested = suggested,
                            sizeClass = sizeClass,
                            onEmptyLongPress = emptyLongPressNow,
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (!todayOn) {
                                GlanceRow(weather = weather, events = events, labelCells = labelCells)
                            }
                            // Grid pages (M9): Expanded centres the
                            // grid at max ~880dp (side margins ≥ 5%);
                            // other sizes take the full width. The
                            // M8 Today-column split is gone — Today
                            // is its own page now (branch above).
                            if (sizeClass == HomeSizeClass.Expanded) {
                                Box(
                                    Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(0.9f)
                                            .widthIn(max = 880.dp)
                                            .fillMaxHeight(),
                                    ) {
                                        GridArea(pagerPage, gridPage, Modifier.fillMaxSize())
                                    }
                                }
                            } else {
                                GridArea(pagerPage, gridPage, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }

            // Page indicator (M9, Doc 2 §2.9): when the host lays it
            // out as its own row (indicatorHosted), the overlay form
            // is skipped entirely — an overlay indicator would sit
            // on top of grid content. The fallback below keeps older
            // call sites working (12dp above the dock zone).
            if (indicatorOn && !indicatorHosted) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                ) {
                    HomePageIndicator(
                        currentPage = pagerState.currentPage,
                        pageCount = pagerPageCount,
                        todayOn = todayOn,
                        editMode = editMode,
                        onPageSelected = { page ->
                            scope.launch {
                                if (state.reduceMotion) pagerState.scrollToPage(page)
                                else pagerState.animateScrollToPage(page, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
                            }
                        },
                        onLongPressOverview = onShowPageOverview,
                        onAddPage = onAddPage,
                    )
                }
            }

            // Library drag hover highlight.
            if (libraryDropActive) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(palette.accent),
                )
            }

            // Drop zones (§8): Remove (left) + App info / Uninstall
            // (right, app drags only) at the top while dragging.
            val dragNow = drag
            if (dragNow != null) {
                val zoneEntry = if (dragNow.item.kind == ItemKind.APP) {
                    appsByKeyNow[dragNow.item.payload]
                } else {
                    null
                }
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PagerDropZoneLabel(
                        "Remove",
                        dragNow.overZone == PagerDropZone.REMOVE,
                        Modifier.weight(1f),
                    )
                    if (zoneEntry != null) {
                        PagerDropZoneLabel(
                            if (isUserApp(context, zoneEntry.pkg)) "Uninstall" else "App info",
                            dragNow.overZone == PagerDropZone.APP_INFO,
                            Modifier.weight(1f),
                        )
                    }
                }
            }

            // Landing ghost at the snapped target cell (Doc 2 §4.4):
            // accent when the landing is valid, red when a widget would
            // overlap (widgets never land on occupied cells — §3.3).
            if (dragNow != null && dragNow.overCell != null &&
                dragNow.overZone == PagerDropZone.NONE
            ) {
                val targetPagerPage = pagerPageForGrid(dragNow.targetGridPage)
                val ghostGeom = gridGeoms[pagerState.currentPage]
                val ghostGridRect = gridWindowRects[pagerState.currentPage]
                val ghostPageRect = pageWindowRects[pagerState.currentPage]
                if (targetPagerPage == pagerState.currentPage && ghostGeom != null &&
                    ghostGridRect != null && ghostPageRect != null
                ) {
                    val gx = ghostGridRect.left - ghostPageRect.left +
                        dragNow.overCell!!.first * ghostGeom.cellWPx
                    val gy = ghostGridRect.top - ghostPageRect.top +
                        dragNow.overCell!!.second * ghostGeom.cellHPx
                    val gw = dragNow.item.colSpan / 2f * ghostGeom.cellWPx
                    val gh = dragNow.item.rowSpan / 2f * ghostGeom.cellHPx
                    val isWidgetDrag = dragNow.item.kind != ItemKind.APP &&
                        dragNow.item.kind != ItemKind.FOLDER
                    val ghostOverlaps = if (dragNow.overUnits != null) {
                        val rect = Rect(
                            dragNow.overUnits!!.first, dragNow.overUnits!!.second,
                            dragNow.item.colSpan, dragNow.item.rowSpan,
                        )
                        itemsNow.any {
                            it.id != dragNow.item.id &&
                                it.page.coerceAtLeast(0) == dragNow.targetGridPage &&
                                rectOf(it).overlaps(rect)
                        }
                    } else {
                        false
                    }
                    val ghostColor = if (isWidgetDrag && ghostOverlaps) Color.Red else palette.accent
                    Box(
                        Modifier
                            .offset { IntOffset(gx.roundToInt(), gy.roundToInt()) }
                            .size(
                                with(density) { gw.toDp() },
                                with(density) { gh.toDp() },
                            )
                            .border(2.dp, ghostColor, RoundedCornerShape(12.dp))
                            .background(ghostColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    )
                }
            }

            // Lifted drag ghost, following the finger.
            if (dragNow != null) {
                val iconDpVal = state.iconDp().dp
                val iconPx = with(density) { iconDpVal.toPx() }
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (dragNow.fingerPage.x - iconPx / 2f).roundToInt(),
                                (dragNow.fingerPage.y - iconPx / 2f).roundToInt(),
                            )
                        }
                        .size(iconDpVal)
                        .graphicsLayer {
                            scaleX = 1.1f
                            scaleY = 1.1f
                            shadowElevation = with(density) { 8.dp.toPx() }
                        },
                ) {
                    val entry = if (dragNow.item.kind == ItemKind.APP) {
                        appsByKeyNow[dragNow.item.payload]
                    } else {
                        null
                    }
                    if (entry != null) {
                        Box {
                            AppIconImage(entry, app.appRepo, iconDpVal)
                            BadgeOverlay(entry.pkg)
                        }
                    } else {
                        GlassSurface(shape = RoundedCornerShape(12.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    dragNow.item.payload.take(12),
                                    color = palette.onSurface,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Icon menu (M6 behaviour: release-without-move after long-press).
    // Rows: Edit · Edit label · Edit icon · Remove · App info ·
    // Uninstall (user apps only). Shortcuts are not carried here —
    // the Library menu still offers them.
    // ---------------------------------------------------------------

    val mEntry = menuEntry
    if (mEntry != null) {
        val gridItem = items.firstOrNull { it.kind == ItemKind.APP && it.payload == mEntry.key }
        val mIsUser = remember(mEntry) { isUserApp(context, mEntry.pkg) }
        AlertDialog(
            onDismissRequest = { menuEntry = null },
            title = { Text(mEntry.label) },
            text = {
                Column {
                    PagerMenuRow("Edit") {
                        menuEntry = null
                        onEditHome()
                    }
                    PagerMenuRow("Edit label") {
                        menuEntry = null
                        labelEntry = mEntry
                    }
                    PagerMenuRow("Edit icon") {
                        menuEntry = null
                        iconEntry = mEntry
                    }
                    PagerMenuRow("Remove") {
                        menuEntry = null
                        if (gridItem != null) {
                            commitNow(items.filterNot { it.id == gridItem.id })
                            toastNow("Removed")
                        } else {
                            toastNow("Not on home")
                        }
                    }
                    PagerMenuRow("App info") {
                        menuEntry = null
                        try {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + mEntry.pkg),
                                ),
                            )
                        } catch (t: Throwable) {
                            // system activity unavailable
                        }
                    }
                    if (mIsUser) {
                        PagerMenuRow("Uninstall") {
                            menuEntry = null
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_DELETE, Uri.parse("package:" + mEntry.pkg)),
                                )
                            } catch (t: Throwable) {
                                // system activity unavailable
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuEntry = null }) { Text("Cancel") } },
        )
    }

    val lEntry = labelEntry
    if (lEntry != null) {
        val overrides = remember(state.labelOverridesJson) {
            parseLabelOverrides(state.labelOverridesJson)
        }
        TextFieldDialog(
            title = "Edit label",
            label = "Label",
            initial = overrides[lEntry.key] ?: lEntry.label,
            confirmLabel = "Save",
            onDismiss = { labelEntry = null },
            onConfirm = { raw ->
                val next = overrides.toMutableMap()
                val trimmed = raw.trim()
                if (trimmed.isBlank()) next.remove(lEntry.key) else next[lEntry.key] = trimmed
                labelEntry = null
                scope.launch {
                    app.settings.setLabelOverridesJson(encodeLabelOverrides(next))
                    app.appRepo.refresh()
                }
            },
        )
    }

    val iEntry = iconEntry
    if (iEntry != null) {
        AppIconPickerDialog(app, iEntry, state) { iconEntry = null }
    }
}

@Composable
private fun PagerMenuRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = LocalPalette.current.onSurface, fontSize = 15.sp)
    }
}

@Composable
private fun PagerDropZoneLabel(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            // M9 Worker B (gold overuse): tonal surface; accent
            // appears only as the active drop target's border.
            .background(palette.surface.copy(alpha = if (active) 0.9f else 0.5f))
            .border(
                1.dp,
                if (active) palette.accent else palette.onSurface.copy(alpha = 0.3f),
                RoundedCornerShape(50),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Glance row (§4.1): top-left, 48dp high, only when Today is not visible. */
@Composable
private fun GlanceRow(
    weather: TodayWeather?,
    events: List<TodayEvent>,
    labelCells: List<CellStats>? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val dateText = remember(tick) {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
    }
    val weatherText = weather?.let { " · ${it.tempText} ${it.condition}" } ?: ""
    val firstEvent = events.firstOrNull()
    Box(
        modifier.fillMaxWidth().height(48.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // M9 Worker B: the date/weather line follows the
            // legibility engine (whole-wallpaper stats); the event
            // chip keeps its white-on-black pill, readable anywhere.
            val glanceStyle = glanceStyleFor(labelCells)
            val glanceColor = glanceStyle.textColor ?: Color.White
            val glancePillBg = if (glanceColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)
            if (glanceStyle.needsPill) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glancePillBg)
                        .clickable { openClockApp(context) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        dateText + weatherText,
                        color = glanceColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    dateText + weatherText,
                    color = glanceColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(shadow = LEGIBILITY_SHADOW),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { openClockApp(context) },
                )
            }
            if (firstEvent != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { openCalendarApp(context) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "• ${firstEvent.timeLabel} ${firstEvent.title}",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun openClockApp(context: android.content.Context) {
    try {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    } catch (t: Throwable) {
        // No clock app handles the intent; stay on home.
    }
}

private fun openCalendarApp(context: android.content.Context) {
    try {
        val uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (t: Throwable) {
        // No calendar app handles the intent; stay on home.
    }
}
