package app.havenos.launcher.ui

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.DateFormat
import android.view.WindowManager
import app.havenos.launcher.data.SnapshotRecord
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.HavenDeviceAdminReceiver
import app.havenos.launcher.R
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.data.CalEvent
import app.havenos.launcher.data.CalendarRepo
import app.havenos.launcher.data.parseAllowedCalendars
import app.havenos.launcher.data.BadgeStore
import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.gridCells
import app.havenos.launcher.data.hiddenApps
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.parseGestures
import app.havenos.launcher.data.weatherInfo
import app.havenos.launcher.data.IconPacks
import app.havenos.launcher.data.encodeLabelOverrides
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.parseLabelOverrides
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.data.calmEditorStateFromKeys
import app.havenos.launcher.data.encode
import app.havenos.launcher.data.encodeFavorites
import app.havenos.launcher.data.encodeSchedule
import app.havenos.launcher.data.parseProfiles
import app.havenos.launcher.data.resolveDockFromIntents
import app.havenos.launcher.data.switchProfile
import app.havenos.launcher.ui.settings.shell.HubNavigator
import app.havenos.launcher.ui.settings.shell.LocalDraftLayer
import app.havenos.launcher.ui.settings.shell.TextFieldDialog
import app.havenos.launcher.ui.settings.shell.SettingsPanel
import app.havenos.launcher.ui.settings.shell.SpotlightRing
import app.havenos.launcher.ui.settings.shell.createHubRegistry
import app.havenos.launcher.ui.settings.shell.rememberDraftLayer
import app.havenos.launcher.ui.settings.shell.rememberPreviewSession
import app.havenos.launcher.ui.settings.shell.rememberSettingsPanelState
import app.havenos.launcher.ui.settings.shell.rememberSpotlightState
import app.havenos.launcher.ui.settings.shell.tabletPanelWidthDp
import app.havenos.launcher.ui.settings.shell.sessionDiffRecords
import app.havenos.launcher.ui.speedSpring
import app.havenos.launcher.ui.speedTween
import app.havenos.launcher.theme.GlassState
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.GlassTier
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalGlass
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import app.havenos.launcher.theme.LocalTokens
import app.havenos.launcher.theme.detectGlassTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private val LabelShadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 4f)

/**
 * Passive multi-pointer observer (never consumes): pinch-in = two pointers
 * closing to < 75% of their initial distance; two-finger-down = both
 * pointers travelling > 80dp downwards from where they touched.
 */
/**
 * Internal (not private) so the gestures hub test path reuses this exact
 * classifier instead of duplicating the pinch / two-finger thresholds.
 * Caller: HavenRoot.
 */
internal suspend fun PointerInputScope.observeMultiGestures(
    onPinchIn: () -> Unit,
    onTwoFingerDown: () -> Unit,
) {
    val thresholdPx = 80.dp.toPx()
    awaitPointerEventScope {
        var startDist: Float? = null
        var firedPinch = false
        var firedTwo = false
        val startPositions = mutableMapOf<PointerId, Offset>()
        while (true) {
            val event = awaitPointerEvent()
            for (change in event.changes) {
                if (change.pressed && !change.previousPressed) {
                    startPositions[change.id] = change.position
                }
                if (!change.pressed) {
                    startPositions.remove(change.id)
                }
            }
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                val a = pressed[0]
                val b = pressed[1]
                val dist = (a.position - b.position).getDistance()
                val start = startDist
                if (start == null) {
                    startDist = dist
                } else if (!firedPinch && start > 0f && dist < start * 0.75f) {
                    firedPinch = true
                    onPinchIn()
                }
                val sa = startPositions[a.id]
                val sb = startPositions[b.id]
                if (!firedTwo && sa != null && sb != null &&
                    a.position.y - sa.y > thresholdPx && b.position.y - sb.y > thresholdPx
                ) {
                    firedTwo = true
                    onTwoFingerDown()
                }
            }
            if (pressed.isEmpty()) {
                startDist = null
                firedPinch = false
                firedTwo = false
                startPositions.clear()
            }
        }
    }
}

/**
 * M7 Wave 2: the drawer section name -> Settings panel route map, shared
 * by the Library and the Search card (both receive section names from
 * SearchCard / LibraryOverlay). Identical to the inline map the drawer
 * used before; extracted so the two hosts cannot drift apart.
 */
private fun settingsRouteForDrawerSection(name: String): String = when (name) {
    "Style & Theme", "Wallpaper", "Icons" -> "look"
    "Home screen", "Dock", "Folders", "Widgets" -> "home-dock"
    "App drawer", "Search & Smart" -> "drawer-search"
    "Gestures", "Animations" -> "gestures-motion"
    "Privacy & Lock" -> "privacy"
    "Backup & Profiles", "System" -> "device-data"
    else -> "home"
}

@Composable
fun HavenRoot(
    app: HavenApp,
    onBackAction: ((() -> Boolean)?) -> Unit,
    quickPanelSignal: Int = 0,
    settingsSignal: Int = 0,
) {
    // M6 panel host state, owned here so the panel, the Before/draft
    // rendering, and the Changes diff all share one session.
    var showSettingsPanel by remember { mutableStateOf(false) }
    var panelStartRoute by remember { mutableStateOf("home") }
    var panelEpoch by remember { mutableIntStateOf(0) }
    val panelSession = rememberPreviewSession()
    val panelDraft = rememberDraftLayer()
    val panelSpotlight = rememberSpotlightState()
    val hubNavigator = remember { HubNavigator() }
    val liveState by app.settings.state.collectAsState()
    // What home renders: the panel-open snapshot while Before is held,
    // the preview draft while a grid/wallpaper preview is active, else
    // the live state. Writes still go to the store, never to these.
    val state = when {
        panelSession.beforeHeld && panelSession.openSnapshot != null -> panelSession.openSnapshot!!
        panelDraft.hasDraft -> panelDraft.draft!!
        else -> liveState
    }
    fun openSettingsPanel(route: String) {
        panelStartRoute = route
        panelEpoch++
        showSettingsPanel = true
    }
    val apps by app.appRepo.apps.collectAsState()
    val items by app.db.homeItems().observeAll().collectAsState(initial = emptyList())
    val folders by app.db.folders().observeAll().collectAsState(initial = emptyList())
    // M9 integration: drop duplicate seeded Agenda widgets once per process
    // (legacy layouts could seed two; fresh layouts seed none). Keeps the
    // first default-payload Agenda; glance/tomorrow variants are untouched.
    var agendaDedupDone by remember { mutableStateOf(false) }
    LaunchedEffect(items) {
        if (!agendaDedupDone && items.isNotEmpty()) {
            agendaDedupDone = true
            val deduped = deduplicateSeededAgendas(items)
            if (deduped.size != items.size) {
                app.db.homeItems().replaceAll(deduped)
            }
        }
    }
    val tokens = LocalTokens.current
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val foldersMap = remember(folders) { folders.associateBy { it.id } }
    val hidden = remember(state.hiddenJson) { state.hiddenApps() }
    val appsByKey = remember(apps, hidden) {
        apps.filter { it.key !in hidden }.associateBy { it.key }
    }

    // Grid geometry: settings speak in cells; the DB stores half-cell units.
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // M10: sizeClass + todayOn live here (not at their old spot below)
    // because the grid formula needs them: the host must use the SAME
    // grid the pager renders (hostGridCells -> pagedGridCells), never
    // the density-only gridCells the host used before (BUG-030: on
    // phones the host said 4x7 while the pager rendered Compact 5x6).
    val sizeClass = rememberHomeSizeClass()
    val todayOn = state.todayOn
    // M10 four-zone shell: on Expanded landscape the M9 Today pager
    // page is retired and Today becomes a rail beside the workspace;
    // on Medium it becomes a collapsible strip; on Compact the Today
    // page survives. todayForPager is what the pager mapping uses.
    val todayAsRail = sizeClass == HomeSizeClass.Expanded && landscape && todayOn
    val todayAsStrip = sizeClass == HomeSizeClass.Medium && todayOn
    val todayForPager = todayOn && !todayAsRail && !todayAsStrip
    val (colsCells, rowsCells) = remember(state, landscape, sizeClass, todayAsRail) {
        hostGridCells(
            state, landscape, sizeClass,
            configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat(),
            todayAsRail,
        )
    }
    val colsU = colsCells * 2
    val rowsU = rowsCells * 2

    // Glass pipeline: refresh the wallpaper store, then resolve the tier.
    val displayMetrics = context.resources.displayMetrics
    LaunchedEffect(
        state.wallpaperMode, state.wpKind, state.wpUri, state.wpTint, state.wpSolid,
    ) {
        app.wallpaper.refresh(state, displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    val wpBlurred by app.wallpaper.blurred.collectAsState()
    val sysReadable by app.wallpaper.systemReadable.collectAsState()
    val (glassTier, glassReason) = remember(state, sysReadable, tokens.blur) {
        detectGlassTier(context, state, sysReadable)
    }
    val glassState = remember(glassTier, glassReason, wpBlurred, state.reduceEffects) {
        val scale = if (wpBlurred != null && displayMetrics.widthPixels > 0) {
            wpBlurred!!.width.toFloat() / displayMetrics.widthPixels
        } else {
            1f
        }
        GlassState(
            tier = glassTier,
            reason = glassReason,
            blurred = if (glassTier == GlassTier.Full) wpBlurred else null,
            scaleX = scale,
            scaleY = scale,
            reduceEffects = state.reduceEffects,
        )
    }

    LaunchedEffect(state.iconPack) { app.appRepo.activePack = state.iconPack }

    var drawerOpen by remember { mutableStateOf(false) }
    var drawerFocusSearch by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var quickPanelOpen by remember { mutableStateOf(false) }
    var showWidgetSheet by remember { mutableStateOf(false) }
    // M10 Level 2 Replace: the widget id the sheet will replace
    // (null = the sheet adds). Cleared by the sheet's onDismiss,
    // which runs after every place or cancel.
    var replaceWidgetId by remember { mutableStateOf<String?>(null) }
    var showQuickCustomize by remember { mutableStateOf(false) }
    // M7 Wave 2 hosts: the standalone Search card, the Calm overlay, the
    // Library view a Calm "All apps" tap forces (null = the stored drawer
    // mode), and the dock icon-menu dialog + its label/icon sub-dialogs.
    var showSearchCard by remember { mutableStateOf(false) }
    var calmOpen by remember { mutableStateOf(false) }
    // M9 Worker D: the Calm editor screen (CalmEditorScreen.kt) shares
    // Worker A's showCalmEditor state in this scope (declared below by
    // Worker A); the editor block further down and the toolbar Calm
    // button both set it. Do not redeclare it here.
    var pendingLibraryView by remember { mutableStateOf<LibraryView?>(null) }
    var dockMenuEntry by remember { mutableStateOf<AppEntry?>(null) }
    var dockLabelEntry by remember { mutableStateOf<AppEntry?>(null) }
    var dockIconEntry by remember { mutableStateOf<AppEntry?>(null) }

    // M7 Wave 2 Calm hosting. CalmMode mutates NO home state: entering
    // only flips calmOpen, exiting only flips it back, so the home
    // underneath restores exactly (edit mode, selection, dock and flow
    // are all untouched by entry and exit).
    //
    // Entry points wired here: Glance long-press (AtAGlanceLine's
    // "Calm mode" row), the edit toolbar "Calm" action, the dock
    // Library long-press (OneDock's onLibraryLongPress), and the
    // performGestureAction("calm") case below — all via [enterCalm].
    // GESTURE_ACTIONS still has no "calm" entry, so no stored gesture
    // value can select it yet; the case exists for direct calls.
    //
    // Schedule (keys `calmScheduleJson` / `calmFavoritesJson`, by name —
    // coordinator adds them to Settings.kt): auto-enter runs only when
    // the parsed schedule is enabled AND active right now, only when no
    // overlay, panel, folder, widget sheet, search card or edit mode is
    // open, and never twice in one window after a manual exit — a manual
    // exit during an active window records that window as dismissed, so
    // the schedule cannot trap the user in Calm until the window changes.
    // Default is off: a blank/disabled schedule never auto-enters.
    val calmSchedule = remember(state.calmScheduleJson) {
        parseCalmSchedule(state.calmScheduleJson)
    }
    var calmAutoDismissedWindow by remember { mutableStateOf<String?>(null) }
    fun calmWindowKey(nowMs: Long): String {
        val (dayOfWeek, _) = calmNowFields(nowMs)
        return "$dayOfWeek:${calmSchedule.startMinutes}-${calmSchedule.endMinutes}"
    }
    val enterCalm: () -> Unit = { calmOpen = true }
    fun exitCalm() {
        val nowMs = System.currentTimeMillis()
        val (dayOfWeek, minuteOfDay) = calmNowFields(nowMs)
        if (calmSchedule.isActiveAt(dayOfWeek, minuteOfDay)) {
            calmAutoDismissedWindow = calmWindowKey(nowMs)
        }
        calmOpen = false
    }
    fun closeDrawer() {
        drawerOpen = false
        pendingLibraryView = null
    }
    LaunchedEffect(
        calmSchedule, calmOpen, calmAutoDismissedWindow,
        showSettingsPanel, drawerOpen, quickPanelOpen, openFolderId,
        showWidgetSheet, showSearchCard, editMode,
    ) {
        // Immediate check on every restart, then once a minute: a window
        // starting while the user is busy waits for a quiet moment.
        while (true) {
            val nowMs = System.currentTimeMillis()
            val (dayOfWeek, minuteOfDay) = calmNowFields(nowMs)
            val quiet = !showSettingsPanel && !drawerOpen && !quickPanelOpen &&
                openFolderId == null && !showWidgetSheet && !showSearchCard && !editMode
            if (quiet && !calmOpen &&
                calmSchedule.isActiveAt(dayOfWeek, minuteOfDay) &&
                calmAutoDismissedWindow != calmWindowKey(nowMs)
            ) {
                calmOpen = true
            }
            delay(60_000)
        }
    }
    // M6: long-press empty home enters edit mode directly (the M5 action
    // bar is gone — its options live in the edit toolbar below, which
    // opens the settings panel).
    var showLockedToast by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf<String?>(null) }
    var resizeItemId by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // M9: ONE 50-level undo/redo stack (Doc 1 §3.7), shared by edit mode
    // and every host mutation below. The Settings panel keeps its
    // existing Before/Changes path for settings values; this stack is
    // the shared undo currency for layout (see the M9 report note on
    // why settings values are not also snapshotted here).
    val editUndo = remember { EditUndoStack() }

    fun pushUndo() {
        editUndo.push(snapshotJson(items, folders))
    }

    fun restoreSnapshot(snap: String) {
        val (newItems, newFolders) = parseSnapshot(snap)
        scope.launch {
            app.db.homeItems().replaceAll(newItems)
            app.db.folders().replaceAll(newFolders)
        }
        selectedIds = emptySet()
    }

    fun doUndo() {
        val snap = editUndo.undo(snapshotJson(items, folders)) ?: return
        restoreSnapshot(snap)
    }

    fun doRedo() {
        val snap = editUndo.redo(snapshotJson(items, folders)) ?: return
        restoreSnapshot(snap)
    }

    // M9 edit-session state: snapshot at entry (overflow "Revert
    // session"), hint dismissal, 5s undo snackbar, and the pager /
    // dock / dialog plumbing the toolbar and PagedHome share.
    var sessionSnapshot by remember { mutableStateOf<String?>(null) }
    var hintDismissed by remember { mutableStateOf(false) }
    var undoSnackbar by remember { mutableStateOf<String?>(null) }
    var undoSnackbarAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var currentGridPage by remember { mutableStateOf(0) }
    var focusGridPage by remember { mutableStateOf<Int?>(null) }
    var dockDropHover by remember { mutableStateOf(false) }
    var selectMoreArmed by remember { mutableStateOf(false) }
    var showCalmEditor by remember { mutableStateOf(false) }
    var showRestoreLayout by remember { mutableStateOf(false) }
    var showResetPage by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var moveToPageItemIds by remember { mutableStateOf<List<String>?>(null) }

    fun showUndoSnackbar(message: String, action: (() -> Unit)? = null) {
        undoSnackbar = message
        undoSnackbarAction = action
    }

    LaunchedEffect(editMode) {
        if (editMode) {
            sessionSnapshot = snapshotJson(items, folders)
            hintDismissed = false
            if (state.editHintOpens < 3) {
                scope.launch { app.settings.setEditHintOpens(state.editHintOpens + 1) }
            }
        } else {
            selectedIds = emptySet()
            resizeItemId = null
            selectMoreArmed = false
        }
    }
    LaunchedEffect(undoSnackbar) {
        if (undoSnackbar != null) {
            delay(EDIT_UNDO_SNACKBAR_MS)
            undoSnackbar = null
            undoSnackbarAction = null
        }
    }
    LaunchedEffect(showLockedToast) {
        if (showLockedToast) {
            delay(2000)
            showLockedToast = false
        }
    }
    LaunchedEffect(toastText) {
        if (toastText != null) {
            delay(2000)
            toastText = null
        }
    }
    LaunchedEffect(quickPanelSignal) {
        if (quickPanelSignal > 0) quickPanelOpen = true
    }
    // System "launcher settings" intent (MainActivity, ACTION_APPLICATION_
    // PREFERENCES): open the full Settings hub, not the home.
    LaunchedEffect(settingsSignal) {
        if (settingsSignal > 0) openSettingsPanel("home")
    }

    // M10: the back handler is registered ONCE, late in this body.
    // An earlier duplicate registration sat here (dead: onBackAction
    // replaces, so the late block always won) and is deleted, not
    // kept as a second registration.
    // Window blur-behind (API 31+): always on for the Partial glass tier,
    // and while the drawer is open, for looks that want blur.
    val wantBlurBehind = tokens.blur && !state.reduceEffects &&
        (drawerOpen || glassTier == GlassTier.Partial)
    LaunchedEffect(wantBlurBehind, tokens.blur) {
        val win = (context as? Activity)?.window ?: return@LaunchedEffect
        try {
            if (wantBlurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val attrs = win.attributes
                attrs.blurBehindRadius = 60
                win.attributes = attrs
            } else {
                win.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        } catch (t: Throwable) {
            // blur-behind is best-effort
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    // First-run seed (vc5, Minimal, no picker): one clock card + the
    // dialer/messages/browser/camera dock picks. No app rows are seeded —
    // the paged home shows placed items only, and newly installed apps
    // appear in the Library for the user to add; seeding app rows would
    // only duplicate it. The items.isEmpty() guard is new: the old seed
    // would replaceAll a live layout if the seeded flag was ever lost;
    // a seed must never wipe an existing layout.
    var seeding by remember { mutableStateOf(false) }
    LaunchedEffect(apps, state.seeded, items) {
        if (!state.seeded && !seeding && apps.isNotEmpty() && items.isEmpty()) {
            seeding = true
            val clock = HomeItem(
                "clock", ItemKind.CLOCK, 0, 0, 0, colsU, 4, "serif",
                flowOrder = 0,
            )
            app.db.homeItems().replaceAll(listOf(clock))
            // M9 Worker D (Doc 3 §5): fresh-install dock from resolved
            // system intents, never hard-coded packages; the old
            // hard-coded dockPicks list is only the fallback when intent
            // resolution returns nothing on this device.
            val intentResolved = try {
                app.layoutStore.resolveDockPackages()
            } catch (t: Throwable) {
                emptyMap()
            }
            val installedPkgs = apps.map { it.pkg }.toSet()
            val intentKeys = resolveDockFromIntents(intentResolved, installedPkgs, 4)
                .mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg }?.key }
            app.settings.setDock(
                if (intentKeys.isNotEmpty()) intentKeys
                else dockPicks(apps.map { it.key to it.pkg }, 4),
            )
            app.settings.setSeeded(true)
        }
    }

    // Newly installed apps need no auto-add pass any more: the scroll
    // home's A-Z list shows the whole catalog, so a fresh install appears
    // on home by itself. (The old pass placed grid rows; keeping it would
    // only write APP rows the home never renders.) autoAddApps is now a
    // stored-only setting; deleting the field is the sibling's call.

    // One-time paged -> scroll-flow migration (vc5, see migrateToFlow):
    // runs exactly when unmigrated (-1) rows exist and never renumbers
    // migrated rows, so a user reorder survives any re-run. Idempotent:
    // with no -1 rows migrateToFlow returns the input unchanged and no
    // write happens, so this effect settles after one write.
    var migrating by remember { mutableStateOf(false) }
    LaunchedEffect(items, state.dock) {
        if (migrating || items.none { it.flowOrder < 0 }) return@LaunchedEffect
        migrating = true
        val migrated = migrateToFlow(items, state.dock)
        if (migrated != items) {
            app.db.homeItems().replaceAll(migrated)
        }
        migrating = false
    }

    // Dock data: pinned (capped at dockMax) + recents behind a divider.
    // M7 Wave 2: recents are gated by the NEW key `dockRecentsOn`
    // (Boolean, default true — coordinator adds it to Settings.kt); the
    // M6 `dockRecent` field is superseded and no longer read here.
    val stats by app.appRepo.stats.collectAsState()
    val pinned = remember(state.dock, state.dockMax, appsByKey) {
        state.dock.take(state.dockMax).mapNotNull { appsByKey[it] }
    }
    val recents = remember(stats, appsByKey, pinned, state.dockRecentsOn) {
        if (!state.dockRecentsOn) {
            emptyList()
        } else {
            app.appRepo.recents(3, pinned.map { it.key }.toSet()).filter { it.key in appsByKey }
        }
    }

    // Gestures (data/Gestures.kt): trigger -> action, executed here.
    val gestures = remember(state.gesturesJson) { parseGestures(state.gesturesJson) }

    fun expandShade(): Boolean = try {
        val sbm = context.getSystemService("statusbar") ?: return false
        val method = sbm.javaClass.getMethod("expandNotificationsPanel")
        method.invoke(sbm)
        true
    } catch (t: Throwable) {
        false
    }

    fun lockDevice() {
        try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(context, HavenDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isAdminActive(admin)) {
                dpm.lockNow()
            } else {
                context.startActivity(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin),
                )
            }
        } catch (t: Throwable) {
            // device-admin flow unavailable on this device
        }
    }

    fun performGestureAction(action: String) {
        when (action) {
            "drawer" -> { drawerFocusSearch = false; drawerOpen = true }
            // M7 Wave 2: the "search" action opens the standalone Search
            // card (the M7 search surface) instead of the Library with a
            // focused field. The Library's own field still searches inline
            // with the same grouped results, and search-first drawer mode
            // (drawerMode 3) still focuses the Library field on open.
            // Default swipe-down stays quickPanel: DEFAULT_GESTURES lives
            // in Settings.kt, which this worker may not edit, so a
            // swipe-down opens the Search card only when bound to search.
            "search" -> { showSearchCard = true }
            "quickPanel" -> quickPanelOpen = true
            // "settings" is a bindable gesture action (GesturesSettings
            // offers "Open settings"); it opens the full Settings hub.
            "settings" -> openSettingsPanel("home")
            "lock" -> lockDevice()
            "shade" -> if (!expandShade()) quickPanelOpen = true
            "calm" -> enterCalm()
            else -> Unit // "none"
        }
    }

    // Launcher icon in the drawer (§2.1): tapping HavenOS's own icon opens
    // full Settings instead of "launching" the home it already is.
    fun launchEntry(entry: AppEntry) {
        if (entry.pkg == context.packageName) {
            openSettingsPanel("home")
        } else {
            app.appRepo.launch(entry)
        }
    }

    // M7 Wave 2 Today data (keys `todayOn` / `smartStackAutoRotate`, by
    // name — coordinator adds them to Settings.kt; defaults true/false
    // per the TodayStack contract). No new network calls: weather is the
    // existing feed's last-known value (its own refresh, already gated
    // by city/staleness, is reused here exactly as Glance reuses it), and
    // "Updated HH:MM" is rendered inside TodayStack from fetchedAtMs.
    val todayAutoRotate = state.smartStackAutoRotate
    val todayWeatherData by app.weather.data.collectAsState()
    LaunchedEffect(todayOn, state.weatherCity, state.weatherUnits) {
        if (todayOn) app.weather.refresh(state)
    }
    val todayWeather = remember(todayWeatherData) {
        todayWeatherData?.let { w ->
            val (label, _) = weatherInfo(w.code)
            TodayWeather(
                tempText = formatTempDisplay(w.temp, state.weatherUnits),
                condition = label,
                city = localityOnly(w.cityName),
                fetchedAtMs = w.fetchedAtMs,
            )
        }
    }
    val todayWeatherOff = !state.weatherNetwork
    // Next events: local calendar query, off the main thread, refreshed
    // every minute; permission-gated (no permission = empty list, and
    // TodayStack shows its prompt instead of an empty list).
    val todayCalendarGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED
    var todayEvents by remember { mutableStateOf<List<TodayEvent>>(emptyList()) }
    LaunchedEffect(todayOn, state.calendarsJson, todayCalendarGranted) {
        if (!todayOn) return@LaunchedEffect
        while (true) {
            todayEvents = if (todayCalendarGranted) {
                withContext(Dispatchers.IO) {
                    CalendarRepo.upcoming(
                        context, 5,
                        parseAllowedCalendars(state.calendarsJson), null,
                    ).map { ev ->
                        TodayEvent(
                            title = ev.title,
                            timeLabel = android.text.format.DateFormat
                                .getTimeFormat(context).format(Date(ev.beginMs)),
                        )
                    }
                }
            } else {
                emptyList()
            }
            delay(60_000)
        }
    }
    fun requestCalendarPermission() {
        // No in-app permission launcher exists on this host; the system
        // app-details page is where the user grants calendar access.
        try {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        } catch (t: Throwable) {
            // system activity unavailable
        }
    }
    // Suggested: local launch history only (count desc, recency desc).
    val todaySuggested = remember(stats, appsByKey) {
        stats.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Pair<Int, Long>>> { it.value.first }
                    .thenByDescending { it.value.second },
            )
            .mapNotNull { appsByKey[it.key] }
            .take(8)
    }
    // Calm favorites: the user's Calm list when set, else the dock
    // favorites (the "existing favorites data" fallback), capped at 8.
    // CalmMode caps again internally; passing more is harmless.
    val calmFavoriteEntries = remember(state.calmFavoritesJson, appsByKey, state.dock) {
        val custom = calmFavoritesCapped(parseCalmFavorites(state.calmFavoritesJson))
        val keys = if (custom.isNotEmpty()) custom else calmFavoritesCapped(state.dock)
        keys.mapNotNull { appsByKey[it] }
    }

    // Panel registry + state. The panel state is stable (constant
    // start route); re-targeting on open resets its stack in the open
    // effect below, so a stale stack never survives a reopen.
    val panelRegistry = remember(hubNavigator) {
        createHubRegistry(app, panelSession, hubNavigator) { action ->
            performGestureAction(action)
        }
    }
    val panelState = rememberSettingsPanelState(panelRegistry, "home", panelSession)
    LaunchedEffect(panelState) { hubNavigator.push = { route -> panelState.push(route) } }
    // Opening (or re-targeting) the panel: fresh snapshot, no stale
    // draft, stack reset to the requested route.
    LaunchedEffect(showSettingsPanel, panelEpoch) {
        if (showSettingsPanel) {
            panelSession.begin(liveState)
            panelDraft.discard()
            panelState.toRoot()
            if (panelStartRoute != "home") panelState.push(panelStartRoute)
        }
    }
    // Session diff: rebuild the Changes list from the panel-open
    // snapshot whenever the live state changes while the panel is open.
    // Hubs never record changes themselves; this is what makes the
    // panel's Changes chip, footer count, per-item Revert, and Revert
    // session real instead of permanently empty.
    LaunchedEffect(liveState) {
        if (showSettingsPanel) {
            val snapshot = panelSession.openSnapshot
            if (snapshot != null) {
                panelSession.replaceChanges(sessionDiffRecords(snapshot, liveState, app.settings))
            }
        }
    }
    // Spotlight: panel spotlight keys pulse the matching home ring.
    LaunchedEffect(panelState.spotlightKey, panelState.spotlightNonce) {
        val key = panelState.spotlightKey
        if (key != null) panelSpotlight.pulse(scope, key)
    }

    // Profile schedule: switch to the scheduled profile inside its window.
    LaunchedEffect(
        state.profileSchedule, state.schedStart, state.schedEnd,
        state.schedProfile, state.activeProfile, state.profilesJson,
    ) {
        if (!state.profileSchedule || state.schedProfile.isBlank()) return@LaunchedEffect
        while (true) {
            val cal = java.util.Calendar.getInstance()
            val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                cal.get(java.util.Calendar.MINUTE)
            val inWindow = if (state.schedStart <= state.schedEnd) {
                nowMin in state.schedStart until state.schedEnd
            } else {
                nowMin >= state.schedStart || nowMin < state.schedEnd
            }
            if (inWindow && state.schedProfile != state.activeProfile &&
                parseProfiles(state.profilesJson).any { it.name == state.schedProfile }
            ) {
                switchProfile(app, state, state.schedProfile)
            }
            delay(60_000)
        }
    }

    // Dock auto-hide: visible again on any interaction, fades after 3s idle.
    var dockTick by remember { mutableIntStateOf(0) }
    var dockVisible by remember { mutableStateOf(true) }
    LaunchedEffect(state.dockAutoHide, dockTick) {
        if (!state.dockAutoHide) {
            dockVisible = true
            return@LaunchedEffect
        }
        dockVisible = true
        delay(3000)
        dockVisible = false
    }
    val dockAlpha by animateFloatAsState(
        targetValue = if (dockVisible) 1f else 0.15f,
        // Reduce Motion: the auto-hide fade becomes instant.
        animationSpec = speedTween(300, state.animSpeed, state.reduceMotion),
        label = "dockAlpha",
    )

    // Edit-mode shrink (BUG 3): the whole home content scales to 0.87,
    // centred, so wallpaper and the edit controls breathe around it.
    // Critically damped spring, ~300ms; Reduce Motion: instant.
    val editScale by animateFloatAsState(
        targetValue = if (editMode) 0.87f else 1f,
        animationSpec = speedSpring(state.animSpeed, state.reduceMotion),
        label = "editScale",
    )

    // Shared by the drawer ("Add to home") and PagedHome's app menu.
    // The scroll home IS favorites + A-Z, so "add to home" means pin to
    // favorites (the dock list): the app moves to the top section. A
    // full dock refuses loudly instead of silently doing nothing.
    fun addEntryToHome(entry: AppEntry) {
        if (entry.key in state.dock) {
            toastText = "Already on home"
            return
        }
        if (state.dock.size >= state.dockMax) {
            toastText = "Dock is full — remove a favorite first"
            return
        }
        scope.launch { app.settings.setDock(state.dock + entry.key) }
        toastText = "Added to favorites"
    }

    // M9 lock guard (Doc 1 §3.11/§6): every edit path refuses with the
    // one-line toast while the layout is locked.
    fun requireUnlocked(): Boolean {
        if (state.lockLayout) {
            showLockedToast = true
            return false
        }
        return true
    }

    fun exitEditOrClearSelection() {
        if (selectedIds.isNotEmpty()) selectedIds = emptySet() else editMode = false
    }

    fun isUserAppPkg(pkg: String): Boolean = try {
        val flags = context.packageManager.getApplicationInfo(pkg, 0).flags
        flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 ||
            flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    } catch (t: Throwable) {
        true
    }

    // Selection spans two stores: "fav:<key>" ids are dock favorites
    // (removed from the dock list), plain ids are flow rows (removed from
    // the layout store, deleting hosted widget ids and folder rows too).
    fun removeSelected() {
        if (!requireUnlocked()) return
        if (selectedIds.isEmpty()) return
        val favKeys = selectedIds.filter { it.startsWith("fav:") }.map { it.removePrefix("fav:") }
        val selected = items.filter { it.id in selectedIds }
        if (favKeys.isEmpty() && selected.isEmpty()) {
            selectedIds = emptySet()
            return
        }
        pushUndo()
        for (item in selected) {
            if (item.kind == ItemKind.TP_WIDGET) {
                item.payload.toIntOrNull()?.let { wid ->
                    try {
                        app.widgetHost.deleteAppWidgetId(wid)
                    } catch (t: Throwable) {
                        // already gone
                    }
                }
            }
        }
        val folderIds = selected.filter { it.kind == ItemKind.FOLDER }.map { it.payload }
        val remaining = items.filterNot { it.id in selectedIds }
        scope.launch {
            if (favKeys.isNotEmpty()) {
                app.settings.setDock(state.dock - favKeys)
            }
            if (selected.isNotEmpty()) {
                app.db.homeItems().replaceAll(remaining)
                for (fid in folderIds) app.db.folders().delete(fid)
            }
        }
        selectedIds = emptySet()
        showUndoSnackbar("Removed")
    }

    fun enterEditMode() {
        if (state.lockLayout) showLockedToast = true else editMode = true
    }

    // M8 host page wiring. Grid-page indices are the model everywhere:
    // HomeItem.page, the overview, and the settings rows all speak grid
    // pages; PagedHome maps them to pager pages (Today offset on
    // Compact/Medium). totalPages is a floor: item pages, plus the
    // persisted pageCount for added-but-still-empty pages, which leave
    // no HomeItem rows to derive from.
    // M10: the host grid IS the pager grid (colsCells above, via
    // hostGridCells). The old call passed sizeClass==Expanded as the
    // landscape flag — wrong on phones in landscape and tablets in
    // portrait — and disagreed with the pager on Compact/Medium.
    val gridCellsHost = colsCells to rowsCells
    val colsUHost = colsU
    val rowsUHost = rowsU
    val itemMaxPage = items.maxOfOrNull { it.page } ?: -1
    val totalPages = maxOf(itemMaxPage + 1, state.pageCount, 1)
    val clampedDefault = clampDefaultPage(state.defaultPage, totalPages)
    val hiddenPages = parseHiddenPages(state.hiddenPagesJson)
    // M10 (was M9 Worker B): ONE host pager state shared by the host
    // indicator row + glance visibility and the pager, so the two can
    // never show different pages mid-swipe. Same inputs as PagedHome's
    // internal model (Grid.kt helpers), so host and pager cannot
    // disagree about count or mapping. M10: the mapping uses
    // todayForPager (Today is a pager page only on Compact now).
    val visibleGridPagesHost = (0 until totalPages).filter { it !in hiddenPages }
        .ifEmpty { (0 until totalPages).toList() }
    val todayPagerCountHost = todayPagerPageCount(visibleGridPagesHost.size, todayForPager)
    val pagerStateHost = rememberPagerState(
        initialPage = pagerIndexForGrid(clampedDefault, visibleGridPagesHost, todayForPager)
            .coerceIn(0, (todayPagerCountHost - 1).coerceAtLeast(0)),
        pageCount = { todayPagerCountHost },
    )
    // Grid page under the current pager page, or null on the Today
    // page. Read in composition (currentPage is snapshot state), so
    // host rows recompose on page change.
    val hostCurrentGridPage = gridPageForTodayPager(pagerStateHost.currentPage, visibleGridPagesHost, todayForPager)
    val labelForItem: (HomeItem) -> String = { item ->
        if (item.kind == ItemKind.APP) appsByKey[item.payload]?.label ?: defaultItemLabel(item)
        else defaultItemLabel(item)
    }
    var showPageOverview by remember { mutableStateOf(false) }
    var lastDeletedSnapshot by remember { mutableStateOf<String?>(null) }
    var deleteRefusedLabels by remember { mutableStateOf<List<String>?>(null) }
    val overviewPages = remember(items, hiddenPages, clampedDefault, totalPages) {
        val byPage = items.groupBy { it.page }
        (0 until totalPages).map { p ->
            OverviewPage(
                page = p,
                items = byPage[p].orEmpty(),
                hidden = p in hiddenPages,
                isDefault = p == clampedDefault,
            )
        }
    }
    val onAddPage: () -> Unit = {
        // Next fresh grid page: past both item pages and the floor, so
        // adding never collides with an existing empty page.
        val newPage = maxOf(addEmptyPage(items, hiddenPages), totalPages)
        scope.launch { app.settings.setPageCount(newPage + 1) }
        // M9 §3.5: appending a page also scrolls to it (PagedHome
        // consumes focusGridPage once).
        focusGridPage = newPage
        toastText = "Page ${newPage + 1} added"
    }
    val onMoveIconToPage: (String, Int) -> Unit = { itemId, targetPage ->
        val item = items.firstOrNull { it.id == itemId }
        if (item != null && item.page != targetPage) {
            val spot = findSpotNear(items, targetPage, item.colSpan, item.rowSpan, colsUHost, rowsUHost, item.col, item.row)
                ?: findSpot(items, targetPage, item.colSpan, item.rowSpan, colsUHost, rowsUHost)
            if (spot != null) {
                pushUndo()
                scope.launch {
                    app.db.homeItems().upsertAll(
                        listOf(item.copy(page = targetPage, col = spot.first, row = spot.second)),
                    )
                }
            } else {
                toastText = "No room on that page"
            }
        }
    }
    val onReorderPagesHost: (List<Int>) -> Unit = { newOrder ->
        val allPages = (0 until totalPages).toList()
        val reordered = reorderPages(items, newOrder)
        val newHidden = hiddenPages.map { remapPageIndex(it, newOrder, allPages) }.distinct().sorted()
        val newDefault = remapPageIndex(state.defaultPage, newOrder, allPages)
        pushUndo()
        scope.launch {
            app.db.homeItems().replaceAll(reordered)
            app.settings.setHiddenPagesJson(encodeHiddenPages(newHidden))
            app.settings.setDefaultPage(newDefault)
        }
    }
    val onHidePageHost: (Int) -> Unit = { page ->
        val visibleCount = (0 until totalPages).count { it !in hiddenPages }
        if (visibleCount <= 1) {
            toastText = "Cannot hide the last visible page"
        } else {
            scope.launch {
                app.settings.setHiddenPagesJson(encodeHiddenPages(hidePage(hiddenPages, page)))
            }
        }
    }
    val onUnhidePageHost: (Int) -> Unit = { page ->
        scope.launch {
            app.settings.setHiddenPagesJson(encodeHiddenPages(unhidePage(hiddenPages, page)))
        }
    }
    val onSetDefaultPageHost: (Int) -> Unit = { page ->
        scope.launch { app.settings.setDefaultPage(page) }
    }
    val onDeletePageHost: (Int) -> Unit = { page ->
        when (val result = deletePage(items, page, colsUHost, rowsUHost, totalPages, hiddenPages, state.defaultPage, labelForItem)) {
            is PageDeleteResult.LastPage -> toastText = "Cannot delete the last page"
            is PageDeleteResult.Refused -> deleteRefusedLabels = result.blockingLabels
            is PageDeleteResult.Success -> {
                pushUndo()
                scope.launch {
                    app.db.homeItems().replaceAll(result.items)
                    app.settings.setHiddenPagesJson(encodeHiddenPages(result.hiddenPages))
                    app.settings.setDefaultPage(result.defaultPage)
                    app.settings.setPageCount(maxOf(0, totalPages - 1))
                }
                lastDeletedSnapshot = result.snapshot
                toastText = if (result.movedCount > 0) {
                    "${result.movedCount} items moved to the adjacent page"
                } else {
                    "Page deleted"
                }
            }
        }
    }
    val onUndoDeletePageHost: (String) -> Unit = { snapshot ->
        val restored = restorePages(snapshot)
        if (restored.isNotEmpty()) {
            scope.launch { app.db.homeItems().replaceAll(restored) }
            toastText = "Page restored"
        }
        lastDeletedSnapshot = null
    }

    // ---- M9 edit-mode host mutations (Doc 1 §3.5, §3.6, §3.11) ----
    // All are lock-guarded and undoable via the shared editUndo stack.

    /** Dock capacity, via the dock component's own math (Dock.kt). */
    fun dockWouldOverflow(newPinnedCount: Int): Boolean {
        if (state.dock.size >= state.dockMax) return true
        val layout = computeDockLayout(
            configuration.screenWidthDp.toFloat(), newPinnedCount, 0, false,
        )
        return layout.pinnedShown < newPinnedCount
    }

    /** Pin an app key to the dock without removing it from the grid. */
    fun addToDockByKey(key: String) {
        if (key in state.dock) {
            toastText = "Already in dock"
            return
        }
        if (dockWouldOverflow(state.dock.size + 1)) {
            toastText = "Dock is full"
            return
        }
        scope.launch { app.settings.setDock(state.dock + key) }
        toastText = "Added to dock"
    }

    /** Pager drag drop onto the dock: the app MOVES from grid to dock. */
    fun dropAppToDock(key: String) {
        if (key in state.dock) {
            toastText = "Already in dock"
            return
        }
        if (dockWouldOverflow(state.dock.size + 1)) {
            toastText = "Dock is full"
            return
        }
        val gridItem = items.firstOrNull { it.kind == ItemKind.APP && it.payload == key }
        pushUndo()
        scope.launch {
            if (gridItem != null) {
                app.db.homeItems().replaceAll(items.filterNot { it.id == gridItem.id })
            }
            app.settings.setDock(state.dock + key)
        }
        toastText = "Added to dock"
    }

    /** Move grid items (by id) to [targetPage], nearest free spot each. */
    fun moveItemsToPage(ids: List<String>, targetPage: Int) {
        if (!requireUnlocked()) return
        val sel = items.filter { it.id in ids }
        if (sel.isEmpty()) return
        pushUndo()
        var acc = items
        val moved = mutableListOf<HomeItem>()
        for (item in sel) {
            val spot = findSpotNear(
                acc, targetPage, item.colSpan, item.rowSpan, colsUHost, rowsUHost,
                item.col, item.row,
            ) ?: findSpot(acc, targetPage, item.colSpan, item.rowSpan, colsUHost, rowsUHost)
            if (spot != null) {
                val next = item.copy(page = targetPage, col = spot.first, row = spot.second)
                acc = acc.map { if (it.id == item.id) next else it }
                moved += next
            }
        }
        if (moved.isNotEmpty()) {
            scope.launch { app.db.homeItems().upsertAll(moved) }
            toastText = "Moved to page ${targetPage + 1}"
        } else {
            toastText = "No room on that page"
        }
        selectedIds = emptySet()
    }

    /** TalkBack/keyboard step move: one cell per step, no overlap. */
    fun moveItemBy(itemId: String, dxCells: Int, dyCells: Int) {
        if (!requireUnlocked()) return
        val item = items.firstOrNull { it.id == itemId } ?: return
        val newCol = (item.col + dxCells * 2).coerceIn(0, (colsUHost - item.colSpan).coerceAtLeast(0))
        val newRow = (item.row + dyCells * 2).coerceIn(0, (rowsUHost - item.rowSpan).coerceAtLeast(0))
        if (newCol == item.col && newRow == item.row) {
            toastText = "At the edge"
            return
        }
        val rect = Rect(newCol, newRow, item.colSpan, item.rowSpan)
        val blocked = items.any {
            it.id != itemId && it.page == item.page && rectOf(it).overlaps(rect)
        }
        if (blocked) {
            toastText = "No room there"
            return
        }
        pushUndo()
        scope.launch {
            app.db.homeItems().upsertAll(listOf(item.copy(col = newCol, row = newRow)))
        }
    }

    /** Hide grid items' apps (Doc 1 §3.6): layout + search + dock. */
    fun hideItems(ids: List<String>) {
        if (!requireUnlocked()) return
        val sel = items.filter { it.id in ids && it.kind == ItemKind.APP }
        if (sel.isEmpty()) return
        val keys = sel.map { it.payload }
        val oldHidden = state.hiddenApps().toList()
        pushUndo()
        scope.launch {
            app.settings.setHiddenJson(encodeMembers((oldHidden + keys).distinct()))
            app.db.homeItems().replaceAll(items.filterNot { it.id in ids })
            if (keys.any { it in state.dock }) {
                app.settings.setDock(state.dock - keys.toSet())
            }
        }
        selectedIds = emptySet()
        // Undo must restore BOTH the layout snapshot and the hidden list;
        // the generic doUndo restores layout only, so Hide carries its own.
        showUndoSnackbar("Hidden") {
            val snap = editUndo.undo(snapshotJson(items, folders))
            if (snap != null) restoreSnapshot(snap)
            scope.launch { app.settings.setHiddenJson(encodeMembers(oldHidden)) }
        }
    }

    /** Duplicate a single built-in widget onto the first free spot. */
    fun duplicateSelected() {
        if (!requireUnlocked()) return
        val single = items.filter { it.id in selectedIds }.singleOrNull() ?: return
        if (single.kind == ItemKind.APP || single.kind == ItemKind.FOLDER ||
            single.kind == ItemKind.CLOCK || single.kind == ItemKind.CLOCK_PAGE ||
            single.kind == ItemKind.TP_WIDGET
        ) {
            return
        }
        val pageOrder = listOf(currentGridPage) +
            (0 until totalPages).filter { it != currentGridPage }
        val spot = firstFreeSpotAcrossPages(
            items, pageOrder, single.colSpan, single.rowSpan, colsUHost, rowsUHost,
        )
        pushUndo()
        if (spot != null) {
            val copy = duplicateWidget(single, "w:${single.kind}:${UUID.randomUUID()}")
                .copy(page = spot.first, col = spot.second, row = spot.third)
            scope.launch { app.db.homeItems().upsertAll(listOf(copy)) }
            toastText = "Widget duplicated"
        } else {
            toastText = "No room for a duplicate"
        }
        selectedIds = emptySet()
    }

    /** Context-bar + Add: folder from the selected apps, at the first app's cell. */
    fun createFolderFromSelection() {
        if (!requireUnlocked()) return
        val sel = items.filter { it.id in selectedIds && it.kind == ItemKind.APP }
        if (sel.size < 2) {
            toastText = "Select 2 or more apps first"
            return
        }
        val first = sel.minWithOrNull(compareBy({ it.page }, { it.row }, { it.col })) ?: sel.first()
        val folderId = UUID.randomUUID().toString()
        pushUndo()
        scope.launch {
            app.db.folders().upsert(
                Folder(folderId, "Folder", encodeMembers(sel.map { it.payload })),
            )
            app.db.homeItems().replaceAll(
                items.filterNot { it.id in sel.map { s -> s.id }.toSet() } +
                    HomeItem(
                        "folder:$folderId", ItemKind.FOLDER, first.page,
                        first.col, first.row, 2, 2, folderId,
                    ),
            )
            val dockMembers = sel.map { it.payload }.filter { it in state.dock }
            if (dockMembers.isNotEmpty()) {
                app.settings.setDock(state.dock - dockMembers.toSet())
            }
        }
        selectedIds = emptySet()
        toastText = "Folder created"
    }

    /** Uninstall (user apps only) via the system confirm (Doc 1 §3.6). */
    fun uninstallApp(entry: AppEntry) {
        if (!isUserAppPkg(entry.pkg)) {
            toastText = "System apps can't be uninstalled"
            return
        }
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    Uri.parse("package:" + entry.pkg),
                ),
            )
        } catch (t: Throwable) {
            // system activity unavailable
        }
    }

    /** M9 §3.5: a tapped widget lands on the first free spot — current
     * page first, then other pages, then a brand-new page. This
     * replaces the old placeWidget, which pinned every widget to page
     * 0, cell (0, 0) on top of the clock. */
    fun addWidgetAtFirstFree(kind: Int, colSpan: Int, rowSpan: Int, payload: String) {
        val id = if (kind == ItemKind.TP_WIDGET) "tp:$payload" else "w:$kind:${UUID.randomUUID()}"
        val pageOrder = listOf(currentGridPage) +
            (0 until totalPages).filter { it != currentGridPage }
        val spot = firstFreeSpotAcrossPages(items, pageOrder, colSpan, rowSpan, colsUHost, rowsUHost)
        pushUndo()
        if (spot != null) {
            val item = HomeItem(id, kind, spot.first, spot.second, spot.third, colSpan, rowSpan, payload)
            scope.launch { app.db.homeItems().upsertAll(listOf(item)) }
            toastText = "Widget added"
        } else {
            val newPage = totalPages
            val item = HomeItem(id, kind, newPage, 0, 0, colSpan, rowSpan, payload)
            scope.launch {
                app.settings.setPageCount(newPage + 1)
                app.db.homeItems().upsertAll(listOf(item))
            }
            focusGridPage = newPage
            toastText = "Widget added to a new page"
        }
    }

    // M10 Level 2 Replace: swap a widget's kind/payload in place.
    // Same position and the sheet's chosen size when the new widget
    // fits there, nearest free spot on the same page otherwise, then
    // any visible page; if nothing fits, the old widget stays and the
    // user is told (never a silent drop). Third-party widget ids
    // encode their payload ("tp:..."), so replacing one renames the
    // id — unless that id is already placed, which is refused.
    fun replaceWidget(itemId: String, kind: Int, colSpan: Int, rowSpan: Int, payload: String) {
        val old = items.firstOrNull { it.id == itemId } ?: return
        val newId = if (kind == ItemKind.TP_WIDGET) "tp:$payload" else old.id
        if (newId != old.id && items.any { it.id == newId }) {
            toastText = "That widget is already on your home"
            return
        }
        val rest = items.filterNot { it.id == itemId }
        val fitsHere = widgetFits(items, old.page, colSpan, rowSpan, old.col, old.row, old.id, colsU, rowsU)
        val target: Triple<Int, Int, Int>? = if (fitsHere) {
            Triple(old.page, old.col, old.row)
        } else {
            val samePage = findSpotNear(rest, old.page, colSpan, rowSpan, colsU, rowsU, old.col, old.row)
            if (samePage != null) {
                Triple(old.page, samePage.first, samePage.second)
            } else {
                visibleGridPagesHost.firstNotNullOfOrNull { p ->
                    findSpot(rest, p, colSpan, rowSpan, colsU, rowsU)
                        ?.let { Triple(p, it.first, it.second) }
                }
            }
        }
        if (target == null) {
            toastText = "No room for that widget — kept the old one"
            return
        }
        val updated = old.copy(
            id = newId, kind = kind, page = target.first, col = target.second, row = target.third,
            colSpan = colSpan, rowSpan = rowSpan, payload = payload,
        )
        pushUndo()
        scope.launch {
            app.db.homeItems().replaceAll(items.map { if (it.id == itemId) updated else it })
        }
        if (newId != old.id && old.id in selectedIds) {
            selectedIds = (selectedIds - old.id) + newId
        }
        toastText = "Widget replaced"
    }

    // One-time scroll-flow -> pages migration (M8, see migrateFlowToPages):
    // runs once while pagesMigrated is false and no unmigrated (-1) rows
    // remain (the vc5 effect above settles those first), then sets the
    // gate so a later user edit is never re-derived from flowOrder.
    // Grid dims are the current orientation's — pages keep their stored
    // cells afterwards, so a later rotation or density change does not
    // re-pack them.
    var pagesMigrating by remember { mutableStateOf(false) }
    LaunchedEffect(items, state.pagesMigrated) {
        if (pagesMigrating || state.pagesMigrated || items.isEmpty() ||
            items.any { it.flowOrder < 0 }
        ) return@LaunchedEffect
        pagesMigrating = true
        val (migCols, migRows) = colsCells to rowsCells
        val migrated = migrateFlowToPages(items, state.dock, migCols, migRows, alreadyMigrated = false)
        if (migrated != items) {
            app.db.homeItems().replaceAll(migrated)
        }
        app.settings.setPagesMigrated(true)
        pagesMigrating = false
    }

    // M10 occupancy safety net (Phase 1): after every layout write,
    // reflow the stored layout into the rendered grid. On a valid
    // layout this is a no-op (reflow preserves fitting items exactly,
    // so it never fights drags, undo, or page edits); on a shrunk
    // grid, a stale layout, or a corrupt write it moves only the
    // offending items to the nearest free spot (same page, then later
    // pages) and records one undo step — so the grid setting's "N
    // icons will move" promise is actually kept, and no two items can
    // share a cell. Gated on pagesMigrated so it never touches
    // pre-migration flow rows.
    LaunchedEffect(items, colsU, rowsU, state.pagesMigrated) {
        if (!state.pagesMigrated || items.isEmpty()) return@LaunchedEffect
        val result = reflowForGrid(items, colsU, rowsU, visibleGridPagesHost)
        if (result.moved.isNotEmpty()) {
            pushUndo()
            app.db.homeItems().replaceAll(result.items)
        }
    }

    CompositionLocalProvider(LocalGlass provides glassState) {
    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(app, state)

        // Home-level gestures (vc5): the passive observers and the
        // double-tap detector sit on the whole home — none of them
        // consumes, so rows, scrolling and drags are unaffected. The
        // vertical swipe detector deliberately does NOT wrap the
        // pager (it would fight page swipes):
        // swipe-up / swipe-down start on the dock instead (DockArea
        // below). Everything here is off in edit mode, so a drag can
        // never open the drawer or the quick panel mid-reorder.
        val homeGestures = if (!editMode) {
            Modifier
                .pointerInput(gestures) {
                    observeMultiGestures(
                        onPinchIn = { performGestureAction(gestures["pinchIn"] ?: "none") },
                        onTwoFingerDown = { performGestureAction(gestures["twoFingerDown"] ?: "search") },
                    )
                }
                .pointerInput(Unit) {
                    // Passive tap observer: resets the dock auto-hide timer.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.changedToUp() }) dockTick++
                        }
                    }
                }
                .pointerInput(gestures) {
                    detectTapGestures(
                        onDoubleTap = { performGestureAction(gestures["doubleTap"] ?: "none") },
                    )
                }
        } else {
            Modifier
        }

        // M9 Worker B: wallpaper legibility cells, hoisted out of
        // ScrollArea so the host glance line shares the pager's
        // stats (same computation, computed once per tree).
        val wallpaperSourceHost by app.wallpaper.source.collectAsState()
        val wallpaperBitmapHost = if (state.wallpaperMode == 1) wallpaperSourceHost else null
        val wallpaperKeyHost = "${state.wallpaperMode}:${state.wpKind}:${state.wpUri}:${state.wpTint}:${state.wpSolid}"
        val labelCellsHost = rememberLegibilityCells(
            wallpaperKeyHost, wallpaperBitmapHost, gridCellsHost.first, gridCellsHost.second,
        )
        // M9 Worker B: this tree's dock side (OneDock renders the
        // vertical variant for Left/Right; the host layout below
        // branches on it too).
        val dockSide = dockSideFromKey(state.dockPos)

        @Composable
        fun ScrollArea(modifier: Modifier) {
            SpotlightRing(
                visible = panelSpotlight.visible &&
                    panelSpotlight.target in setOf("home", "grid", "icons", "labels", "widgets", "weather", "wallpaper"),
                reducedMotion = state.reduceMotion,
            ) {
            Box(modifier.then(homeGestures)) {
            Column(Modifier.fillMaxSize()) {
                // M8: the home surface is the horizontal pager.
                // PagedHome owns Today placement itself — a dedicated
                // Today page (M9, Doc 2 §4.3) on every size class, grid
                // pages after it — so the host no longer pins a
                // TodaySection above the home, and the outer layout
                // below no longer branches on size class either.
                //
                // Legibility (M8, Worker C hook): real per-cell
                // wallpaper stats, computed once above the pager —
                // never per icon, never during scrolling. System mode
                // passes null (the OS bitmap is unreadable on API 33+),
                // so Auto degrades to the mid-grey default there; the
                // key covers mode/kind/uri/tint/solid so a wallpaper
                // change always resamples. The grid here is the same
                // gridCells the pager uses, so row*cols+col lines up.
                // M9 Worker B: hoisted above ScrollArea (shared with
                // the host glance line).
                val labelCells = labelCellsHost
                PagedHome(
                    app = app,
                    stateOverride = state,
                    pinned = pinned,
                    apps = appsByKey.values.toList(),
                    onLaunch = { entry -> launchEntry(entry) },
                    onAddToHome = { entry -> addEntryToHome(entry) },
                    modifier = Modifier.weight(1f),
                    labelModeValue = state.labelMode,
                    flowItems = items,
                    foldersMap = foldersMap,
                    dockKeys = state.dock,
                    editMode = editMode,
                    selectedIds = selectedIds,
                    // M9: "Select more" arms add-only selection;
                    // otherwise a tap toggles (Doc 1 §3.2).
                    onSelectToggle = { id ->
                        selectedIds = if (selectMoreArmed) {
                            selectedIds + id
                        } else if (id in selectedIds) {
                            selectedIds - id
                        } else {
                            selectedIds + id
                        }
                    },
                    onOpenFolder = { openFolderId = it },
                    onEditHome = { enterEditMode() },
                    onSwipeAction = { performGestureAction(it) },
                    // M10 Level 1 quick customize: long-press empty
                    // space opens the compact customize bar (Edit home
                    // inside it still enters edit mode, so the old
                    // entry path is preserved, not removed).
                    onEmptyLongPress = { showQuickCustomize = true },
                    onReorderFavorites = { next ->
                        scope.launch { app.settings.setDock(next) }
                    },
                    // M8 rename: onReorderFlow -> onCommitItems. The
                    // host contract is unchanged: pushUndo, then write
                    // the whole committed list.
                    onCommitItems = { reordered ->
                        pushUndo()
                        scope.launch { app.db.homeItems().upsertAll(reordered) }
                    },
                    onToast = { toastText = it },
                    // Folder drops from PagedHome: the host contract
                    // takes member keys only, so the folder lands on
                    // the first free cell of grid page 0 (spilling to
                    // later pages when page 0 is full); members pinned
                    // in the dock leave it (they now live in the folder).
                    onCreateFolder = { members ->
                        pushUndo()
                        val folderId = UUID.randomUUID().toString()
                        val folder = Folder(folderId, "Folder", encodeMembers(members))
                        val nextOrder = (items.maxOfOrNull { it.flowOrder } ?: -1) + 1
                        var folderPage = 0
                        var folderSpot = findSpot(items, 0, 2, 2, colsUHost, rowsUHost)
                        if (folderSpot == null) {
                            for (p in 1..(itemMaxPage + 1)) {
                                val s = findSpot(items, p, 2, 2, colsUHost, rowsUHost)
                                if (s != null) { folderPage = p; folderSpot = s; break }
                            }
                        }
                        val (fc, fr) = folderSpot ?: (0 to 0)
                        val item = HomeItem(
                            "folder:$folderId", ItemKind.FOLDER, folderPage, fc, fr, 2, 2,
                            folderId, flowOrder = nextOrder,
                        )
                        scope.launch {
                            app.db.folders().upsert(folder)
                            app.db.homeItems().upsertAll(listOf(item))
                            if (members.any { it in state.dock }) {
                                app.settings.setDock(state.dock - members.toSet())
                            }
                        }
                        toastText = "Folder created"
                    },
                    onAddToFolder = { folderId, appKey ->
                        val folder = foldersMap[folderId]
                        if (folder == null) {
                            toastText = "Folder unavailable"
                        } else {
                            val members = parseMembers(folder.membersJson)
                            if (appKey in members) {
                                toastText = "Already in this folder"
                            } else {
                                pushUndo()
                                scope.launch {
                                    app.db.folders().upsert(
                                        folder.copy(
                                            membersJson = encodeMembers(members + appKey),
                                        ),
                                    )
                                    if (appKey in state.dock) {
                                        app.settings.setDock(state.dock - appKey)
                                    }
                                }
                                toastText = "Added to folder"
                            }
                        }
                    },
                    // Widget resize commit (whole-cell steps, already
                    // stepped by the caller).
                    onResizeItem = { updated ->
                        pushUndo()
                        scope.launch { app.db.homeItems().upsertAll(listOf(updated)) }
                    },
                    // M8 pager params, wired to the M8 settings keys.
                    todayOn = todayForPager,
                    parallaxOn = state.parallaxOn,
                    indicatorOn = state.pageIndicatorOn,
                    hiddenPages = hiddenPages,
                    defaultPage = clampedDefault,
                    onShowPageOverview = { showPageOverview = true },
                    onAddPage = onAddPage,
                    labelCells = labelCells,
                    weather = todayWeather,
                    weatherOff = todayWeatherOff,
                    events = todayEvents,
                    calendarGranted = todayCalendarGranted,
                    onRequestCalendarPermission = { requestCalendarPermission() },
                    suggested = todaySuggested,
                    autoRotate = todayAutoRotate,
                    snapToGrid = state.snapToGrid,
                    minPages = totalPages,
                    // M9 edit-mode wiring (Doc 1 §3.8, §3.11).
                    dockFull = state.dock.size >= state.dockMax ||
                        computeDockLayout(
                            configuration.screenWidthDp.toFloat(), state.dock.size + 1, 0, false,
                        ).pinnedShown < state.dock.size + 1,
                    onDropToDock = { key -> dropAppToDock(key) },
                    onDockHover = { v -> if (dockDropHover != v) dockDropHover = v },
                    // v10 dock drag-out: a successful grid drop
                    // removes the app from the dock (the grid add
                    // itself already pushed undo via onCommitItems).
                    onDockDragDrop = { entry ->
                        scope.launch { app.settings.setDock(state.dock - entry.key) }
                    },
                    onCurrentGridPage = { pg -> currentGridPage = pg },
                    onOpenSearch = { showSearchCard = true },
                    focusGridPage = focusGridPage,
                    onFocusConsumed = { focusGridPage = null },
                    onA11yMove = { id, dx, dy -> moveItemBy(id, dx, dy) },
                    onA11yMoveToPage = { id -> moveToPageItemIds = listOf(id) },
                    onA11yAddToDock = { id ->
                        items.firstOrNull { it.id == id }
                            ?.takeIf { it.kind == ItemKind.APP }
                            ?.let { addToDockByKey(it.payload) }
                    },
                    onA11yHide = { id -> hideItems(listOf(id)) },
                    onA11yResize = { id ->
                        if (requireUnlocked()) resizeItemId = id
                    },
                    onA11yRemove = { id ->
                        if (requireUnlocked()) {
                            selectedIds = setOf(id)
                            showRemoveConfirm = true
                        }
                    },
                    onExitEdit = { exitEditOrClearSelection() },
                    hostPagerState = pagerStateHost,
                    indicatorHosted = true,
                )
            } // Column: PagedHome (no vertical scroll surface remains)
            }
            }
        }

        @Composable
        fun DockArea() {
            // Swipe-up / swipe-down start on the dock (see homeGestures):
            // the dock never scrolls, so this detector competes with
            // nothing, and it is off in edit mode like all home gestures.
            // M9 Worker B: dock swipe gestures are bottom-dock
            // only — a vertical pill must not fight its own drags.
            val dockGestures = if (!editMode && dockSide == DockSide.Bottom) {
                Modifier.pointerInput(gestures) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { _, amount -> total += amount },
                        onDragEnd = {
                            if (total < -120.dp.toPx()) {
                                // swipeUpDrawer=false means a dock swipe-up
                                // does nothing; true runs the mapped action
                                // (drawer by default).
                                if (state.swipeUpDrawer) {
                                    performGestureAction(gestures["swipeUp"] ?: "drawer")
                                }
                            } else if (total > 120.dp.toPx()) {
                                performGestureAction(gestures["swipeDown"] ?: "quickPanel")
                            }
                        },
                    )
                }
            } else {
                Modifier
            }
            SpotlightRing(
                visible = panelSpotlight.visible &&
                    panelSpotlight.target in setOf("dock", "search"),
                reducedMotion = state.reduceMotion,
            ) {
            // M7 Wave 2: the ONE dock. Identical on home and in edit mode
            // (same DockArea both times); the Library and Search surfaces
            // have no dock of their own, so there is no second dock to
            // keep in sync. Long-press opens the icon menu (below), not
            // the old instant-remove: removal is one tap inside the menu.
            // The old dock's Settings chip is gone (M6 note): Settings
            // still opens from the quick panel, the edit toolbar, and
            // Glance long-press. The old dockSearch / dockDrawerBtn
            // toggles no longer hide buttons — OneDock always shows
            // Search + Library (its contract); retiring those stored
            // toggles is the coordinator's call.
            //
            // Icon provider: the repository resolver (icon packs +
            // per-app overrides), held stable via remember; key() below
            // remounts the dock when the pack or per-app overrides
            // change, so icons refresh instead of going stale.
            //
            // Library drop target: accepts the Library drag-out MIME and
            // adds the app to the dock. NOTE the deviation, stated
            // plainly: drops APPEND to the dock instead of landing at
            // the drop position — the platform DragAndDropEvent exposes
            // no drop coordinates to the target, and OneDock exposes no
            // slot geometry, so an exact slot cannot be computed here.
            // (LibraryOverlay dismisses itself via onDragOut when the
            // drag starts, so this target is live when the drop lands.)
            val dockIconProvider = remember(app.appRepo) {
                { entry: AppEntry -> app.appRepo.icon(entry) }
            }
            val dockNow by rememberUpdatedState(state.dock)
            val dockMaxNow by rememberUpdatedState(state.dockMax)
            Box(
                Modifier
                    .then(dockGestures)
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            event.mimeTypes().contains(LIBRARY_DRAG_MIME)
                        },
                        target = object : DragAndDropTarget {
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                // Same Compose 1.11 pattern as PagedHome's
                                // library drop target: the Compose event has
                                // no clipData/clipDescription — the Android
                                // DragEvent carries the payload.
                                val dragEvent = event.toAndroidDragEvent()
                                val text = try {
                                    dragEvent.clipData?.getItemAt(0)?.text?.toString()
                                } catch (t: Throwable) {
                                    null
                                } ?: return false
                                val payload = decodeLibraryDragPayload(text)
                                    ?: return false
                                val entry = appsByKey[payload.appKey]
                                    ?: appsByKey.values.firstOrNull {
                                        it.pkg == payload.pkg && it.cls == payload.cls
                                    }
                                    ?: return false
                                scope.launch {
                                    when {
                                        entry.key in dockNow -> {
                                            toastText = "Already in dock"
                                        }
                                        dockNow.size >= dockMaxNow -> {
                                            toastText = "Dock is full — remove a favorite first"
                                        }
                                        else -> {
                                            app.settings.setDock(dockNow + entry.key)
                                            toastText = "Added to dock"
                                        }
                                    }
                                }
                                return true
                            }
                        },
                    )
                    // M9 Worker B: bottom dock sits 16dp clear
                    // above the gesture-nav inset (plus the Column's
                    // navigationBarsPadding); side dock pads its
                    // outer edge 12dp, inner edge 8dp.
                    .padding(
                        start = if (dockSide == DockSide.Left) 12.dp else if (dockSide == DockSide.Right) 8.dp else 12.dp,
                        end = if (dockSide == DockSide.Right) 12.dp else if (dockSide == DockSide.Left) 8.dp else 12.dp,
                        top = if (dockSide == DockSide.Bottom) 0.dp else 12.dp,
                        bottom = if (dockSide == DockSide.Bottom) 16.dp else 12.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // M9 Worker B (Doc 2 §7): 240ms fade on dock
                // position change (100ms fade under reduced motion,
                // instant at speed 0). First composition fades in
                // once — harmless.
                val dockFade = remember { Animatable(1f) }
                LaunchedEffect(dockSide) {
                    dockFade.snapTo(0.55f)
                    dockFade.animateTo(1f, homeFadeSpec(240, state.animSpeed, state.reduceMotion))
                }
                Box(Modifier.graphicsLayer { alpha = dockAlpha * dockFade.value }) {
                    key(state.iconPack, state.perAppIconJson) {
                        OneDock(
                            pinned = pinned,
                            recents = recents,
                            recentsOn = state.dockRecentsOn,
                            onAppClick = { entry ->
                                dockTick++
                                launchEntry(entry)
                            },
                            onAppLongPress = { entry ->
                                dockTick++
                                dockMenuEntry = entry
                            },
                            onSearchClick = {
                                dockTick++
                                showSearchCard = true
                            },
                            onLibraryClick = {
                                dockTick++
                                drawerFocusSearch = false
                                drawerOpen = true
                            },
                            onLibraryLongPress = {
                                dockTick++
                                enterCalm()
                            },
                            iconProvider = dockIconProvider,
                            side = dockSide,
                            sizeClass = sizeClass,
                            animSpeed = state.animSpeed,
                            reduceMotion = state.reduceMotion,
                            hapticsOn = hapticsEnabled(state.pressFeedback, state.hapticStrength),
                            // v10: dock drag-out is edit-mode-only
                            // (normal-mode long-press keeps the menu).
                            dragOutEnabled = editMode,
                        )
                    }
                }
                if (state.dockAutoHide && !dockVisible) {
                    Box(
                        Modifier
                            .width(72.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.65f))
                            .clickable { dockTick++ },
                    )
                }
            }
            }
        }

        // The shrink wraps the pager + glance + dock, but not the
        // wallpaper (drawn above): the home content shrinks, its
        // frame stays full-size. The edit toolbar is a reserved row
        // at the top of the layout below (M9 Worker A), never an
        // overlay, so the grid below is pushed down, never covered.
        //
        // M9 Worker B layout (Doc 2 §1/§4.5): the host branches on
        // dock side. Bottom = Column: toolbar, pager, glance,
        // indicator row, dock. Left/Right = Row: vertical pill
        // centred on that edge + content Column (toolbar, pager,
        // glance, indicator at the bottom-centre). The indicator is
        // a layout row, never an overlay, so it cannot sit on top
        // of grid content. navigationBarsPadding keeps the bottom
        // dock 16dp clear of the gesture-nav inset.
        // M9: dock capacity for drop refusal — the dock component's
        // own math (Dock.kt), not a reimplementation.
        val dockFullNow = state.dock.size >= state.dockMax ||
            computeDockLayout(
                configuration.screenWidthDp.toFloat(), state.dock.size + 1, 0, false,
            ).pinnedShown < state.dock.size + 1
        // M9 Worker B: the edit toolbar block, shared by the
        // bottom-dock Column and side-dock Row layouts below
        // (extracted so the ~100 lines are not duplicated).
        @Composable
        fun EditToolbarBlock() {
            // M9 toolbar (Doc 1 §3.10): ONE 56dp row in its OWN
            // reserved space at the top of this column — the grid
            // below is pushed down (and scaled by editScale), never
            // overlapped, unlike the old overlay toolbar it replaces.
            if (editMode) {
                EditToolbar(
                    expanded = sizeClass == HomeSizeClass.Expanded,
                    canUndo = editUndo.canUndo,
                    canRedo = editUndo.canRedo,
                    snapOn = state.snapToGrid,
                    onUndo = { doUndo() },
                    onRedo = { doRedo() },
                    onSnapToggle = {
                        scope.launch {
                            app.settings.setPlacementMode(if (state.snapToGrid) 1 else 0)
                        }
                    },
                    onAddWidget = { showWidgetSheet = true },
                    onAddPage = onAddPage,
                    onAddFolder = { createFolderFromSelection() },
                    onPages = { showPageOverview = true },
                    onWallpaper = { openSettingsPanel("look") },
                    onLook = { openSettingsPanel("look") },
                    onCalmEditor = { showCalmEditor = true },
                    onSettings = { openSettingsPanel("home") },
                    onDone = {
                        editMode = false
                        scope.launch { app.layoutStore.autoSnapshotOnEditExit() }
                    },
                    onRevertSession = {
                        val snap = sessionSnapshot
                        if (snap != null) {
                            pushUndo()
                            restoreSnapshot(snap)
                            toastText = "Session reverted"
                        } else {
                            toastText = "Nothing to revert"
                        }
                    },
                    onRestoreLayout = {
                        if (state.undoJson.isBlank()) {
                            toastText = "No saved layout snapshot"
                        } else {
                            showRestoreLayout = true
                        }
                    },
                    onResetPage = { showResetPage = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                if (state.editHintOpens < 3 && !hintDismissed) {
                    EditHintChip(
                        onDismiss = { hintDismissed = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                if (selectedIds.isNotEmpty()) {
                    val selectedItems = items.filter { it.id in selectedIds }
                    val single = selectedItems.singleOrNull()
                    val isWidgetSel = single != null &&
                        single.kind != ItemKind.APP &&
                        single.kind != ItemKind.FOLDER &&
                        single.kind != ItemKind.CLOCK &&
                        single.kind != ItemKind.CLOCK_PAGE &&
                        single.kind != ItemKind.TP_WIDGET
                    val isFolderSel = single?.kind == ItemKind.FOLDER
                    val singleApp = if (single?.kind == ItemKind.APP) {
                        appsByKey[single.payload]
                    } else {
                        null
                    }
                    EditContextBar(
                        selectedCount = selectedIds.size,
                        canResize = single != null && isResizable(single),
                        isWidget = isWidgetSel,
                        isFolder = isFolderSel,
                        isSingleApp = singleApp != null,
                        isUserApp = singleApp?.let { isUserAppPkg(it.pkg) } ?: false,
                        selectedName = single?.let { labelForItem(it) }
                            ?: if (selectedIds.size > 1) "${selectedIds.size} items" else null,
                        canReplace = isWidgetSel,
                        onReplace = {
                            if (single != null && requireUnlocked()) {
                                replaceWidgetId = single.id
                                showWidgetSheet = true
                            }
                        },
                        onResize = {
                            if (single != null && requireUnlocked()) resizeItemId = single.id
                        },
                        onRemove = {
                            if (requireUnlocked()) showRemoveConfirm = true
                        },
                        onMoveToPage = { moveToPageItemIds = selectedIds.toList() },
                        onDuplicate = { duplicateSelected() },
                        onItemSettings = {
                            if (single == null) {
                                // no-op: bar only shows when selected
                            } else if (isWidgetSel) {
                                if (requireUnlocked()) resizeItemId = single.id
                            } else if (isFolderSel) {
                                openFolderId = single.payload
                            }
                        },
                        onSelectMore = {
                            selectMoreArmed = !selectMoreArmed
                            toastText = if (selectMoreArmed) {
                                "Tap more items to add them"
                            } else {
                                "Select more off"
                            }
                        },
                        onAddToDock = {
                            singleApp?.let { addToDockByKey(it.key) }
                        },
                        onHide = { hideItems(selectedIds.toList()) },
                        onUninstall = {
                            singleApp?.let { uninstallApp(it) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
        }

        // M10: when the Settings panel is open on a tablet, home
        // re-lays out in the free area left of the panel (BUG-026/027:
        // the dock no longer sits under the panel). The width matches
        // SettingsPanel's docked width + its 12dp inset on each side.
        // On phones the sheet overlays the bottom as before (M6).
        val panelEndPadding = if (showSettingsPanel && configuration.screenWidthDp >= 840) {
            (tabletPanelWidthDp(configuration.screenWidthDp.toFloat()) + 24f).dp
        } else {
            0.dp
        }
        var mediumTodayExpanded by remember { mutableStateOf(false) }
        // v10 dock correction — ONE dock host, shared content slot.
        // The three old branches (todayAsRail / bottom / side) each
        // composed DockArea separately, so the dock's presence depended
        // on which branch ran; the content (toolbar, pager, glance,
        // indicator) was also triplicated. Now there is one placement
        // decision, driven only by dockSide, and one content slot:
        // the pager (ScrollArea weight 1f) lives inside ShellContent,
        // the dock lives OUTSIDE it as a sibling, so the pager can
        // never extend into the dock zone and no grid row can touch
        // the screen edge below the dock. The DockHost calls stay
        // INSIDE the scaled Box below, so edit-mode 0.87 shrink and
        // tablet panelEndPadding behaviour are exactly preserved. The one
        // visible change vs the old rail layout: a bottom dock used
        // to sit under the 70% workspace column only; it now centres
        // under the full shell width (the pill itself is capped at
        // 720dp by OneDock, so the pill does not grow).
        @Composable
        fun DockHost() {
            // Explicit space reservation: the zone is exactly the
            // dock's measured size — pill thickness + its own outer
            // padding (bottom: 16dp below the pill; side: 12dp +
            // 8dp across). Content is weight(1f) against this fixed
            // sibling, so the reservation is structural, not a hope.
            val zoneMod = if (dockSide == DockSide.Bottom) {
                Modifier.fillMaxWidth()
                    .height(dockThicknessFor(sizeClass, DockSide.Bottom) + 16.dp)
            } else {
                Modifier.width(dockThicknessFor(sizeClass, dockSide) + 20.dp)
                    .fillMaxHeight()
            }
            Box(zoneMod, contentAlignment = Alignment.Center) {
                // Dock drop highlight (Doc 1 §3.8): accent while a
                // drag hovers the dock, red when the dock is full.
                // The dock itself is untouched — host-side border.
                Box(
                    Modifier.then(
                        if (dockDropHover) {
                            Modifier.border(
                                2.dp,
                                if (dockFullNow) androidx.compose.ui.graphics.Color.Red else palette.accent,
                                RoundedCornerShape(50),
                            )
                        } else {
                            Modifier
                        },
                    ),
                ) {
                    DockArea()
                }
            }
        }

        @Composable
        fun ShellContent(modifier: Modifier) {
            if (todayAsRail) {
                Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                    TodayRail(
                        weather = todayWeather,
                        weatherOff = todayWeatherOff,
                        events = todayEvents,
                        calendarGranted = todayCalendarGranted,
                        onRequestCalendarPermission = { requestCalendarPermission() },
                        suggested = todaySuggested,
                        appRepo = app.appRepo,
                        onLaunch = { entry -> launchEntry(entry) },
                        onSearch = { showSearchCard = true },
                        onAddWidget = { showWidgetSheet = true },
                        onEnterCalm = enterCalm,
                        modifier = Modifier.weight(0.30f).fillMaxHeight(),
                    )
                    Column(Modifier.weight(0.70f).fillMaxHeight()) {
                        EditToolbarBlock()
                        ScrollArea(Modifier.weight(1f))
                        if (state.pageIndicatorOn) {
                            HomePageIndicator(
                                currentPage = pagerStateHost.currentPage,
                                pageCount = todayPagerCountHost,
                                todayOn = todayForPager,
                                editMode = editMode,
                                onPageSelected = { page ->
                                    scope.launch {
                                        if (state.reduceMotion) pagerStateHost.scrollToPage(page)
                                        else pagerStateHost.animateScrollToPage(page, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
                                    }
                                },
                                onLongPressOverview = { showPageOverview = true },
                                onAddPage = onAddPage,
                            )
                        }
                    }
                }
            } else {
                Column(modifier) {
                    EditToolbarBlock()
                    if (todayAsStrip) {
                        MediumTodayStrip(
                            weather = todayWeather,
                            weatherOff = todayWeatherOff,
                            events = todayEvents,
                            calendarGranted = todayCalendarGranted,
                            onRequestCalendarPermission = { requestCalendarPermission() },
                            suggested = todaySuggested,
                            appRepo = app.appRepo,
                            onLaunch = { entry -> launchEntry(entry) },
                            onSearch = { showSearchCard = true },
                            onAddWidget = { showWidgetSheet = true },
                            onEnterCalm = enterCalm,
                            expanded = mediumTodayExpanded,
                            onToggle = { mediumTodayExpanded = !mediumTodayExpanded },
                        )
                    }
                    ScrollArea(Modifier.weight(1f))
                    // One glance line: PagedHome draws its own top Glance
                    // row when Today is off, so the host line below the
                    // pager shows only on grid pages while Today is on —
                    // never both, never on the Today page itself. The
                    // rail path omits it (the rail already shows
                    // date/weather), as before.
                    if (todayForPager && hostCurrentGridPage != null) {
                        AtAGlanceLine(
                            app, state,
                            onOpenSettings = { openSettingsPanel("home") },
                            onEnterCalm = enterCalm,
                            labelCells = labelCellsHost,
                        )
                    }
                    // Indicator is a layout row at the bottom of the
                    // content area: above the dock for a bottom dock,
                    // bottom-centre of the content area for a side
                    // dock — never an overlay, never over content.
                    if (state.pageIndicatorOn) {
                        HomePageIndicator(
                            currentPage = pagerStateHost.currentPage,
                            pageCount = todayPagerCountHost,
                            todayOn = todayForPager,
                            editMode = editMode,
                            onPageSelected = { page ->
                                scope.launch {
                                    if (state.reduceMotion) pagerStateHost.scrollToPage(page)
                                    else pagerStateHost.animateScrollToPage(page, animationSpec = homePageSpec(state.animSpeed, state.reduceMotion))
                                }
                            },
                            onLongPressOverview = { showPageOverview = true },
                            onAddPage = onAddPage,
                        )
                    }
                }
            }
        }

        Box(
            Modifier.fillMaxSize().graphicsLayer { scaleX = editScale; scaleY = editScale }
                .padding(end = panelEndPadding),
        ) {
            // M10 four-zone shell (was M9 Worker B, Doc 2 §1/§4.5),
            // v10: placement branches ONLY on dock side now. Bottom
            // = Column: content (weight 1f), then the dock host.
            // Left/Right = Row: dock host on the edge, content
            // (weight 1f). The dock stays in the home layout on
            // every page and in edit mode, OUTSIDE the pager, so it
            // can never sit under page content (BUG-022/025).
            if (dockSide == DockSide.Bottom) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    ShellContent(Modifier.weight(1f).fillMaxWidth())
                    DockHost()
                }
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    if (dockSide == DockSide.Left) DockHost()
                    ShellContent(Modifier.weight(1f).fillMaxHeight().statusBarsPadding().navigationBarsPadding())
                    if (dockSide == DockSide.Right) DockHost()
                }
            }
        }

    LaunchedEffect(showLockedToast) {
        if (showLockedToast) {
            delay(2000)
            showLockedToast = false
        }
    }
    LaunchedEffect(toastText) {
        if (toastText != null) {
            delay(2000)
            toastText = null
        }
    }
    LaunchedEffect(quickPanelSignal) {
        if (quickPanelSignal > 0) quickPanelOpen = true
    }
    // System "launcher settings" intent (MainActivity, ACTION_APPLICATION_
    // PREFERENCES): open the full Settings hub, not the home.
    LaunchedEffect(settingsSignal) {
        if (settingsSignal > 0) openSettingsPanel("home")
    }

    DisposableEffect(
        drawerOpen, openFolderId, editMode, quickPanelOpen, showWidgetSheet,
        calmOpen, showSearchCard, selectedIds, showQuickCustomize,
    ) {
        onBackAction {
            when {
                // Topmost overlays close first (Calm is composed last).
                calmOpen -> { exitCalm(); true }
                showSearchCard -> { showSearchCard = false; true }
                openFolderId != null -> { openFolderId = null; true }
                showWidgetSheet -> { showWidgetSheet = false; true }
                quickPanelOpen -> { quickPanelOpen = false; true }
                showQuickCustomize -> { showQuickCustomize = false; true }
                drawerOpen -> { closeDrawer(); true }
                // Doc 1 §3.1: first Back clears the selection, second exits.
                editMode -> {
                    if (selectedIds.isNotEmpty()) {
                        selectedIds = emptySet()
                        true
                    } else {
                        editMode = false
                        scope.launch { app.layoutStore.autoSnapshotOnEditExit() }
                        true
                    }
                }
                else -> false
            }
        }
        onDispose { onBackAction(null) }
    }

    // Window blur-behind (API 31+): always on for the Partial glass tier,
    // and while the drawer is open, for looks that want blur.
    val wantBlurBehind = tokens.blur && !state.reduceEffects &&
        (drawerOpen || glassTier == GlassTier.Partial)
    LaunchedEffect(wantBlurBehind, tokens.blur) {
        val win = (context as? Activity)?.window ?: return@LaunchedEffect
        try {
            if (wantBlurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val attrs = win.attributes
                attrs.blurBehindRadius = 60
                win.attributes = attrs
            } else {
                win.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        } catch (t: Throwable) {
            // blur-behind is best-effort
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

    // First-run seed (vc5, Minimal, no picker): one clock card + the
    // dialer/messages/browser/camera dock picks. No app rows are seeded —
    // the paged home shows placed items only, and newly installed apps
    // appear in the Library for the user to add; seeding app rows would
    // only duplicate it. The items.isEmpty() guard is new: the old seed
    // would replaceAll a live layout if the seeded flag was ever lost;
    // a seed must never wipe an existing layout.
    var seeding by remember { mutableStateOf(false) }
    LaunchedEffect(apps, state.seeded, items) {
        if (!state.seeded && !seeding && apps.isNotEmpty() && items.isEmpty()) {
            seeding = true
            val clock = HomeItem(
                "clock", ItemKind.CLOCK, 0, 0, 0, colsU, 4, "serif",
                flowOrder = 0,
            )
            app.db.homeItems().replaceAll(listOf(clock))
            // M9 Worker D (Doc 3 §5): fresh-install dock from resolved
            // system intents, never hard-coded packages; the old
            // hard-coded dockPicks list is only the fallback when intent
            // resolution returns nothing on this device.
            val intentResolved = try {
                app.layoutStore.resolveDockPackages()
            } catch (t: Throwable) {
                emptyMap()
            }
            val installedPkgs = apps.map { it.pkg }.toSet()
            val intentKeys = resolveDockFromIntents(intentResolved, installedPkgs, 4)
                .mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg }?.key }
            app.settings.setDock(
                if (intentKeys.isNotEmpty()) intentKeys
                else dockPicks(apps.map { it.key to it.pkg }, 4),
            )
            app.settings.setSeeded(true)
        }
    }

    // Newly installed apps need no auto-add pass any more: the scroll
    // home's A-Z list shows the whole catalog, so a fresh install appears
    // on home by itself. (The old pass placed grid rows; keeping it would
    // only write APP rows the home never renders.) autoAddApps is now a
    // stored-only setting; deleting the field is the sibling's call.

    // One-time paged -> scroll-flow migration (vc5, see migrateToFlow):
    // runs exactly when unmigrated (-1) rows exist and never renumbers
    // migrated rows, so a user reorder survives any re-run. Idempotent:
    // with no -1 rows migrateToFlow returns the input unchanged and no
    // write happens, so this effect settles after one write.
    var migrating by remember { mutableStateOf(false) }
    LaunchedEffect(items, state.dock) {
        if (migrating || items.none { it.flowOrder < 0 }) return@LaunchedEffect
        migrating = true
        val migrated = migrateToFlow(items, state.dock)
        if (migrated != items) {
            app.db.homeItems().replaceAll(migrated)
        }
        migrating = false
    }

    // Dock data: pinned (capped at dockMax) + recents behind a divider.
    // M7 Wave 2: recents are gated by the NEW key `dockRecentsOn`
    // (Boolean, default true — coordinator adds it to Settings.kt); the
    // M6 `dockRecent` field is superseded and no longer read here.
    val stats by app.appRepo.stats.collectAsState()
    val pinned = remember(state.dock, state.dockMax, appsByKey) {
        state.dock.take(state.dockMax).mapNotNull { appsByKey[it] }
    }
    val recents = remember(stats, appsByKey, pinned, state.dockRecentsOn) {
        if (!state.dockRecentsOn) {
            emptyList()
        } else {
            app.appRepo.recents(3, pinned.map { it.key }.toSet()).filter { it.key in appsByKey }
        }
    }

    // Gestures (data/Gestures.kt): trigger -> action, executed here.
    val gestures = remember(state.gesturesJson) { parseGestures(state.gesturesJson) }


        if (showLockedToast) {
            Box(
                Modifier.fillMaxSize().padding(bottom = 110.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                GlassSurface(shape = RoundedCornerShape(50)) {
                    Text(
                        "Layout is locked",
                        color = palette.onSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        if (toastText != null) {
            Box(
                Modifier.fillMaxSize().padding(bottom = 110.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                GlassSurface(shape = RoundedCornerShape(50)) {
                    Text(
                        toastText!!,
                        color = palette.onSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // M8 page overview: thumbnails, drag icons between pages,
        // reorder, add, hide, default, delete (with the clarified
        // remove-page rules). Dismissing clears a pending undo snapshot
        // so a stale bar never reappears on the next open.
        if (showPageOverview) {
            PageOverviewDialog(
                pages = overviewPages,
                colsU = colsUHost,
                rowsU = rowsUHost,
                labelFor = labelForItem,
                onDismiss = { showPageOverview = false; lastDeletedSnapshot = null },
                onMoveIcon = onMoveIconToPage,
                onReorderPages = onReorderPagesHost,
                onAddPage = onAddPage,
                onHidePage = onHidePageHost,
                onUnhidePage = onUnhidePageHost,
                onSetDefaultPage = onSetDefaultPageHost,
                onDeletePage = onDeletePageHost,
                lastDeletedSnapshot = lastDeletedSnapshot,
                onUndoDeletePage = onUndoDeletePageHost,
                onUndoConsumed = { lastDeletedSnapshot = null },
            )
        }

        if (deleteRefusedLabels != null) {
            AlertDialog(
                onDismissRequest = { deleteRefusedLabels = null },
                title = { Text("Cannot delete this page") },
                text = {
                    Text(
                        "These items do not fit on any other page: " +
                            deleteRefusedLabels!!.joinToString(", "),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { deleteRefusedLabels = null }) { Text("OK") }
                },
            )
        }

        // M9 edit-mode dialogs (Doc 1 §3.6, §3.9, §3.10): remove confirm,
        // move-to-page picker, restore-layout confirm, reset-page
        // confirm, Calm editor, and the 5s undo snackbar (above the toast).
        if (showRemoveConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirm = false },
                title = { Text("Remove from home") },
                text = {
                    Text(
                        "Remove ${selectedIds.size} item(s) from home? " +
                            "The apps stay in the Library.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRemoveConfirm = false
                        removeSelected()
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
                },
            )
        }

        val moveIds = moveToPageItemIds
        if (moveIds != null) {
            AlertDialog(
                onDismissRequest = { moveToPageItemIds = null },
                title = { Text("Move to page") },
                text = {
                    Column {
                        for (pg in overviewPages) {
                            TextButton(onClick = {
                                moveToPageItemIds = null
                                moveItemsToPage(moveIds, pg.page)
                            }) {
                                Text(
                                    "Page ${pg.page + 1}" +
                                        (if (pg.isDefault) " (default)" else "") +
                                        (if (pg.hidden) " (hidden)" else ""),
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { moveToPageItemIds = null }) { Text("Cancel") }
                },
            )
        }

        if (showRestoreLayout) {
            // M9 integration: list Worker D's auto-snapshots (date + page /
            // item / folder counts) instead of restoring a single stale blob.
            // Visual home preview before applying is still open (see report).
            var restoreList by remember { mutableStateOf<List<SnapshotRecord>?>(null) }
            LaunchedEffect(Unit) {
                restoreList = app.layoutStore.snapshots()
            }
            AlertDialog(
                onDismissRequest = { showRestoreLayout = false },
                title = { Text("Restore layout") },
                text = {
                    val list = restoreList
                    if (list == null) {
                        Text("Loading snapshots…")
                    } else if (list.isEmpty()) {
                        Text(
                            "No layout snapshots yet. A snapshot is saved " +
                                "automatically when you leave edit mode.",
                        )
                    } else {
                        LazyColumn(Modifier.heightIn(max = 360.dp)) {
                            items(list, key = { it.id }) { rec ->
                                val date = remember(rec.createdAtMs) {
                                    SimpleDateFormat(
                                        "d MMM yyyy, HH:mm",
                                        Locale.getDefault(),
                                    ).format(Date(rec.createdAtMs))
                                }
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            showRestoreLayout = false
                                            pushUndo()
                                            scope.launch {
                                                if (app.layoutStore.restoreSnapshot(rec)) {
                                                    toastText = "Layout restored"
                                                } else {
                                                    toastText =
                                                        "Couldn't restore that snapshot"
                                                }
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                ) {
                                    Text(date)
                                    Text(
                                        "${rec.pageCount} pages · " +
                                            "${rec.itemCount} items · " +
                                            "${rec.folderCount} folders",
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRestoreLayout = false }) { Text("Cancel") }
                },
            )
        }

        if (showResetPage) {
            AlertDialog(
                onDismissRequest = { showResetPage = false },
                title = { Text("Reset page") },
                text = {
                    Text(
                        "Remove all items from page ${currentGridPage + 1}? " +
                            "This is undoable.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showResetPage = false
                        if (requireUnlocked()) {
                            pushUndo()
                            scope.launch {
                                app.db.homeItems().replaceAll(
                                    items.filterNot { it.page == currentGridPage },
                                )
                            }
                            selectedIds = emptySet()
                            toastText = "Page reset"
                        }
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetPage = false }) { Text("Cancel") }
                },
            )
        }

        if (undoSnackbar != null) {
            Box(
                Modifier.fillMaxSize().padding(bottom = 170.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                EditUndoSnackbar(
                    message = undoSnackbar!!,
                    onUndo = {
                        val action = undoSnackbarAction
                        undoSnackbar = null
                        undoSnackbarAction = null
                        if (action != null) action() else doUndo()
                    },
                    onDismiss = {
                        undoSnackbar = null
                        undoSnackbarAction = null
                    },
                )
            }
        }

        // M7 Wave 2 Library hosting: LibraryOverlay directly (the M7
        // entry point for new hosts; the legacy DrawerOverlay wrapper is
        // left intact for its other callers but no longer used here).
        // The mapping below replicates DrawerOverlay exactly: initial
        // view from the stored drawer mode (unless a Calm "All apps" tap
        // forced List for this open), view changes written back to the
        // drawer mode, search-first mode still focuses the field.
        //
        // Categories tab plug (the slot already existed on LibraryOverlay
        // — no Drawer.kt edit was needed): visible apps (hidden already
        // filtered out of appsByKey), overrides from the NEW key
        // `categoryOverridesJson` by name, expanded at width >= 840dp.
        // Moves/renames compute the new JSON with the Wave 1 pure
        // helpers and persist it via the coordinator-owned setter
        // `setCategoryOverridesJson` (does not exist yet — coordinator
        // adds it to Settings.kt alongside the field; until then this
        // block does not compile, like the other new-key reads here).
        if (drawerOpen) {
            LibraryOverlay(
                app = app,
                state = state,
                initialView = pendingLibraryView ?: LibraryView.fromKey(state.libraryDefaultView),
                categoriesContent = {
                    CategoriesView(
                        apps = appsByKey.values.toList(),
                        appRepo = app.appRepo,
                        overridesJson = state.categoryOverridesJson,
                        expanded = configuration.screenWidthDp >= 840,
                        onLaunch = { entry ->
                            closeDrawer()
                            launchEntry(entry)
                        },
                        onMoveApp = { appKey, categoryId ->
                            scope.launch {
                                app.settings.setCategoryOverridesJson(
                                    categoryOverridesWithMove(
                                        state.categoryOverridesJson, appKey, categoryId,
                                    ),
                                )
                            }
                        },
                        onRenameCategory = { categoryId, name ->
                            scope.launch {
                                app.settings.setCategoryOverridesJson(
                                    categoryOverridesWithRename(
                                        state.categoryOverridesJson, categoryId, name,
                                    ),
                                )
                            }
                        },
                    )
                },
                focusSearch = drawerFocusSearch || state.drawerMode == 3,
                onClose = { closeDrawer() },
                onLaunch = { entry ->
                    closeDrawer()
                    launchEntry(entry)
                },
                onAddToHome = { entry ->
                    addEntryToHome(entry)
                    closeDrawer()
                },
                onOpenSettings = { name ->
                    closeDrawer()
                    openSettingsPanel(settingsRouteForDrawerSection(name))
                },
                // Drag-out: dismiss the Library at drag start so the
                // home/dock drop target can receive the platform drag
                // (the drag itself keeps running across the dismissal).
                onDragOut = { closeDrawer() },
                onViewChanged = { view ->
                    scope.launch {
                        app.settings.setLibraryDefaultView(view.key)
                    }
                },
            )
        }

        // M7 Wave 2 Search card: the standalone search surface for the
        // dock Search button and the "search" gesture action. (The
        // Library's own field is NOT replaced by this card: it renders
        // the same grouped results inline, and lives in Drawer.kt, which
        // this worker may only touch for the Categories plug above —
        // so there is no Search-card trigger from inside the Library.
        // The card and the Library field share SearchResultsList, so
        // results are identical from either entry point.)
        if (showSearchCard) {
            SearchCard(
                app = app,
                state = state,
                onClose = { showSearchCard = false },
                onLaunch = { entry ->
                    showSearchCard = false
                    launchEntry(entry)
                },
                onOpenSettings = { name ->
                    showSearchCard = false
                    openSettingsPanel(settingsRouteForDrawerSection(name))
                },
            )
        }


/**
 * M7 Wave 2 dock menu row. Same shape as the drawer's MenuRow, defined
 * here because that one is private to Drawer.kt and this worker may not
 * widen Drawer.kt beyond the Categories plug (which needed no edit).
 */
@Composable
fun DockMenuRow(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = palette.onSurface, fontSize = 15.sp)
    }
}

        // M7 Wave 2 dock icon menu: OneDock's onAppLongPress contract.
        // Same rows as the Library menu, adapted to the dock: pinned
        // apps offer removal, recents offer adding (when room).
        val dockEntry = dockMenuEntry
        if (dockEntry != null) {
            AlertDialog(
                onDismissRequest = { dockMenuEntry = null },
                title = { Text(dockEntry.label) },
                text = {
                    Column {
                        DockMenuRow("Launch") {
                            dockMenuEntry = null
                            launchEntry(dockEntry)
                        }
                        if (dockEntry.key in state.dock) {
                            DockMenuRow("Remove from dock") {
                                dockMenuEntry = null
                                scope.launch {
                                    app.settings.setDock(state.dock - dockEntry.key)
                                }
                            }
                            // M9 Worker B: dock reorder / move-out
                            // (TalkBack/keyboard-safe). Pointer drag
                            // reorder needs a shared grid↔dock drag
                            // geometry that does not exist yet — the
                            // menu rows are the shipped path.
                            val dockIdx = state.dock.indexOf(dockEntry.key)
                            if (dockIdx > 0) {
                                DockMenuRow("Move left") {
                                    dockMenuEntry = null
                                    val next = state.dock.toMutableList().also {
                                        it[dockIdx] = it[dockIdx - 1]
                                        it[dockIdx - 1] = dockEntry.key
                                    }
                                    scope.launch { app.settings.setDock(next) }
                                }
                            }
                            if (dockIdx < state.dock.size - 1) {
                                DockMenuRow("Move right") {
                                    dockMenuEntry = null
                                    val next = state.dock.toMutableList().also {
                                        it[dockIdx] = it[dockIdx + 1]
                                        it[dockIdx + 1] = dockEntry.key
                                    }
                                    scope.launch { app.settings.setDock(next) }
                                }
                            }
                            DockMenuRow("Move to home") {
                                dockMenuEntry = null
                                val targetPage = hostCurrentGridPage ?: 0
                                val spot = findSpot(items, targetPage, 2, 2, colsUHost, rowsUHost)
                                if (spot == null) {
                                    toastText = "No room on that page"
                                } else {
                                    pushUndo()
                                    val item = HomeItem(
                                        "app:${dockEntry.key}", ItemKind.APP, targetPage,
                                        spot.first, spot.second, 2, 2, dockEntry.key,
                                    )
                                    scope.launch {
                                        app.db.homeItems().upsertAll(listOf(item))
                                        app.settings.setDock(state.dock - dockEntry.key)
                                    }
                                }
                            }
                        } else if (state.dock.size < state.dockMax) {
                            DockMenuRow("Add to dock") {
                                dockMenuEntry = null
                                scope.launch {
                                    app.settings.setDock(state.dock + dockEntry.key)
                                }
                            }
                        } else {
                            DockMenuRow("Dock is full") { dockMenuEntry = null }
                        }
                        DockMenuRow("Edit label") {
                            dockMenuEntry = null
                            dockLabelEntry = dockEntry
                        }
                        DockMenuRow("Edit icon") {
                            dockMenuEntry = null
                            dockIconEntry = dockEntry
                        }
                        DockMenuRow("Hide app") {
                            dockMenuEntry = null
                            scope.launch {
                                app.settings.setHiddenJson(
                                    encodeMembers((hidden + dockEntry.key).toList()),
                                )
                            }
                        }
                        DockMenuRow("App info") {
                            dockMenuEntry = null
                            try {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + dockEntry.pkg),
                                    ),
                                )
                            } catch (t: Throwable) {
                                // system activity unavailable
                            }
                        }
                        DockMenuRow("Uninstall") {
                            dockMenuEntry = null
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.parse("package:" + dockEntry.pkg),
                                    ),
                                )
                            } catch (t: Throwable) {
                                // system activity unavailable
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { dockMenuEntry = null }) { Text("Cancel") } },
            )
        }

        val dockLabel = dockLabelEntry
        if (dockLabel != null) {
            val overrides = remember(state.labelOverridesJson) {
                parseLabelOverrides(state.labelOverridesJson)
            }
            TextFieldDialog(
                title = "Edit label",
                label = "Label",
                initial = overrides[dockLabel.key] ?: dockLabel.label,
                confirmLabel = "Save",
                onDismiss = { dockLabelEntry = null },
                onConfirm = { raw ->
                    val next = overrides.toMutableMap()
                    val trimmed = raw.trim()
                    if (trimmed.isBlank()) next.remove(dockLabel.key)
                    else next[dockLabel.key] = trimmed
                    dockLabelEntry = null
                    scope.launch {
                        app.settings.setLabelOverridesJson(encodeLabelOverrides(next))
                        app.appRepo.refresh()
                    }
                },
            )
        }

        val dockIcon = dockIconEntry
        if (dockIcon != null) {
            AppIconPickerDialog(app, dockIcon, state) { dockIconEntry = null }
        }

        val folderId = openFolderId
        if (folderId != null) {
            foldersMap[folderId]?.let { folder ->
                SpotlightRing(
                    visible = panelSpotlight.visible && panelSpotlight.target == "folder",
                    reducedMotion = state.reduceMotion,
                ) {
                FolderSheet(
                    app = app,
                    folder = folder,
                    items = items,
                    appsByKey = appsByKey,
                    onClose = { openFolderId = null },
                    onLaunch = { entry ->
                        app.appRepo.launch(entry)
                        openFolderId = null
                    },
                )
                }
            }
        }

        if (showWidgetSheet) {
            val cellDp = if (colsCells > 0) configuration.screenWidthDp / colsCells else 72
            AddWidgetSheet(
                app = app,
                cellDp = cellDp,
                colsU = colsU,
                rowsU = rowsU,
                onDismiss = {
                    showWidgetSheet = false
                    replaceWidgetId = null
                },
                onPlace = { kind, w, h, payload ->
                    val replaceId = replaceWidgetId
                    if (replaceId != null) {
                        replaceWidget(replaceId, kind, w, h, payload)
                    } else {
                        addWidgetAtFirstFree(kind, w, h, payload)
                    }
                },
            )
        }

        val resizingId = resizeItemId
        if (resizingId != null) {
            items.firstOrNull { it.id == resizingId }?.let { item ->
                ResizeDialog(
                    item = item,
                    colsU = colsU,
                    rowsU = rowsU,
                    step = if (item.kind == ItemKind.FOLDER) 2 else 1,
                    items = items,
                    visiblePages = visibleGridPagesHost,
                    onDismiss = { resizeItemId = null },
                    onSave = { wUnits, hUnits ->
                        if (!requireUnlocked()) {
                            resizeItemId = null
                        } else if (widgetFits(
                                items, item.page, wUnits, hUnits,
                                item.col, item.row, item.id, colsU, rowsU,
                            )
                        ) {
                            pushUndo()
                            val updated = item.copy(colSpan = wUnits, rowSpan = hUnits)
                            scope.launch { app.db.homeItems().upsertAll(listOf(updated)) }
                            resizeItemId = null
                        } else {
                            val spot = findSpotNear(
                                items.filter { it.id != item.id }, item.page,
                                wUnits, hUnits, colsU, rowsU, item.col, item.row,
                            )
                            if (spot != null) {
                                pushUndo()
                                val updated = item.copy(
                                    col = spot.first, row = spot.second,
                                    colSpan = wUnits, rowSpan = hUnits,
                                )
                                scope.launch { app.db.homeItems().upsertAll(listOf(updated)) }
                                resizeItemId = null
                            } else {
                                // M10: a resize with no room here is
                                // offered another page before it is
                                // refused — never just "No room".
                                val elsewhere = visibleGridPagesHost.firstNotNullOfOrNull { p ->
                                    findSpot(
                                        items.filter { it.id != item.id }, p,
                                        wUnits, hUnits, colsU, rowsU,
                                    )?.let { p to it }
                                }
                                if (elsewhere != null) {
                                    pushUndo()
                                    val updated = item.copy(
                                        page = elsewhere.first,
                                        col = elsewhere.second.first,
                                        row = elsewhere.second.second,
                                        colSpan = wUnits, rowSpan = hUnits,
                                    )
                                    scope.launch { app.db.homeItems().upsertAll(listOf(updated)) }
                                    resizeItemId = null
                                    val pos = elsewhere.first + if (todayOn) 2 else 1
                                    toastText = "Moved to page $pos"
                                } else {
                                    toastText = "No room for that size"
                                }
                            }
                        }
                    },
                )
            }
        }

        if (quickPanelOpen) {
            QuickPanel(
                app = app,
                state = state,
                onClose = { quickPanelOpen = false },
                onOpenSettings = { section ->
                    quickPanelOpen = false
                    openSettingsPanel(if (section == 3) "look" else "home")
                },
                onEditHome = {
                    quickPanelOpen = false
                    if (state.lockLayout) showLockedToast = true else editMode = true
                },
            )
        }

        // M10 Level 1 quick customize (Phase 3): long-press empty
        // space opens this compact bar over the live home (home stays
        // visible; the bar floats above the dock, never over it).
        // Wallpaper / Layout / Widgets / Dock / Style / Home settings
        // + Edit home (the previous long-press action, kept so the
        // edit-mode entry path is preserved). Tapping outside or
        // Back dismisses; every action closes the bar first.
        if (showQuickCustomize) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showQuickCustomize = false }
                        },
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            bottom = if (dockSide == DockSide.Bottom) 104.dp else 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                        ),
                ) {
                    GlassSurface(shape = RoundedCornerShape(28.dp)) {
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for ((label, action) in listOf(
                                "Wallpaper" to { openSettingsPanel("look") },
                                "Layout" to { openSettingsPanel("home-dock") },
                                "Widgets" to { showWidgetSheet = true },
                                "Dock" to { openSettingsPanel("home-dock") },
                                "Style" to { openSettingsPanel("look") },
                                "Home settings" to { openSettingsPanel("home") },
                                "Edit home" to { enterEditMode() },
                            )) {
                                TextButton(onClick = {
                                    showQuickCustomize = false
                                    action()
                                }) {
                                    Text(label, color = palette.onSurface, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // M6 panel over the live home: homeContent is empty because
        // home is already composed below this overlay; the panel draws
        // its scrim + sheet/docked panel on top of it. DraftLayer is
        // provided here so hub previews (grid, wallpaper) reach home.
        // UNCHANGED by M7 Wave 2 (this worker must not regress it): the
        // panel still sits over the live home below it.
        if (showSettingsPanel) {
            CompositionLocalProvider(LocalDraftLayer provides panelDraft) {
                SettingsPanel(
                    state = panelState,
                    homeContent = {},
                    onClose = {
                        panelDraft.discard()
                        showSettingsPanel = false
                    },
                )
            }
        }

        // M7 Wave 2 Calm overlay, composed LAST so it is topmost. It
        // draws over the unchanged home and removes itself on exit, so
        // the home restores exactly. Favorites: the Calm list when set,
        // else the dock favorites, capped at 8. "All apps" closes Calm
        // and opens the Library forced to the List view for this open
        // (the stored drawer mode is untouched). "Choose apps" only
        // appears when both lists are empty; since M9 it opens the real
        // Calm editor (CalmEditorScreen.kt), which persists
        // calmFavoritesJson / calmScheduleJson / calmEntryJson.
        // KNOWN WAVE 1 CONCERN, not fixed here (CalmMode.kt is outside
        // this worker's owned files): CalmMode's background has no
        // click/tap consumer, so taps on its empty background may pass
        // through to home rows underneath — worth a Pad check.
        if (calmOpen) {
            CalmMode(
                onExit = { exitCalm() },
                favorites = calmFavoriteEntries,
                onLaunch = { entry -> launchEntry(entry) },
                onOpenAllApps = {
                    calmOpen = false
                    pendingLibraryView = LibraryView.LIST
                    drawerOpen = true
                },
                onChooseFavorites = {
                    calmOpen = false
                    showCalmEditor = true
                },
                expanded = sizeClass == HomeSizeClass.Expanded,
            )
        }
        // M9 Worker D: Calm editor screen. Saving writes only the three
        // Calm settings keys; Calm never changes the layout.
        if (showCalmEditor) {
            val editorInitial = remember(state.calmFavoritesJson, state.calmScheduleJson, state.calmEntryJson) {
                calmEditorStateFromKeys(
                    state.calmFavoritesJson,
                    state.calmScheduleJson,
                    state.calmEntryJson,
                )
            }
            CalmEditorScreen(
                apps = apps,
                initial = editorInitial,
                onSave = { edited ->
                    scope.launch {
                        app.settings.setCalmFavoritesJson(edited.encodeFavorites())
                        app.settings.setCalmScheduleJson(edited.encodeSchedule())
                        app.settings.setCalmEntryJson(edited.entry.encode())
                    }
                    showCalmEditor = false
                },
                onClose = { showCalmEditor = false },
            )
        }

    }
    }
}

/**
 * At-a-glance line (M7 Wave 1 Glance cleanup, Home v2 prompt §5.7):
 * ONE line with ONE clear role — date + weather + one event chip —
 * rendered by this single composable above the dock. It has exactly
 * one call site in HavenRoot (M7 Wave 2 unified layout), so it cannot
 * float above the dock in some states only.
 * It also survives edit mode (it sits inside the shrink box, like the
 * rest of home content).
 *
 * - Tap the date/weather text opens the clock app; tap the event chip
 *   opens the calendar app. Both are best-effort intents: if no app
 *   handles one, nothing happens (no crash, no dead button state).
 * - Long-press opens Glance options: date only / date + weather / off,
 *   plus a jump to Settings. No permanent hint text lives here — the
 *   long-press hint lives in the tutorial only.
 * - The event chip appears only when READ_CALENDAR is granted and a
 *   next event exists; the query runs off the main thread and refreshes
 *   every minute. No new network calls: weather still comes from the
 *   existing weather store.
 * - Type is the HomeTokens glance size (16sp medium), with the shared
 *   label shadow so it stays legible on wallpaper.
 */
@Composable
private fun AtAGlanceLine(
    app: HavenApp,
    state: SettingsState,
    onOpenSettings: () -> Unit = {},
    // M7 Wave 2: Glance long-press Calm entry (a row in the options
    // dialog below). Entering Calm mutates no home state.
    onEnterCalm: () -> Unit = {},
    labelCells: List<CellStats>? = null,
) {
    if (state.atAGlance == 0) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val weather by app.weather.data.collectAsState()
    LaunchedEffect(state.atAGlance, state.weatherCity, state.weatherUnits) {
        if (state.atAGlance == 2) app.weather.refresh(state)
    }
    // Next event: local calendar query, off the main thread, refreshed
    // every minute. Without the permission (or on any query failure,
    // which CalendarRepo already swallows) there is simply no chip.
    var nextEvent by remember { mutableStateOf<CalEvent?>(null) }
    LaunchedEffect(state.calendarsJson) {
        while (true) {
            nextEvent = if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED
            ) {
                withContext(Dispatchers.IO) {
                    CalendarRepo.upcoming(
                        context, 1,
                        parseAllowedCalendars(state.calendarsJson), null,
                    ).firstOrNull()
                }
            } else {
                null
            }
            delay(60_000)
        }
    }
    val text = remember(tick, weather, state.atAGlance) {
        val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val w = weather
        if (state.atAGlance == 2 && w != null && w.cityName == state.weatherCity) {
            val (label, symbol) = weatherInfo(w.code)
            "$date  ·  $symbol ${w.temp.roundToInt()}° $label"
        } else {
            date
        }
    }
    val event = nextEvent
    val eventText = remember(event) {
        event?.let {
            val time = android.text.format.DateFormat.getTimeFormat(context).format(Date(it.beginMs))
            "• $time ${it.title}"
        }
    }
    var showOptions by remember { mutableStateOf(false) }
    fun openClock() {
        try {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        } catch (t: Throwable) {
            // No clock app handles the intent; stay on home.
        }
    }
    fun openCalendar() {
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
    // M9 Worker B: the glance line follows the legibility engine
    // (whole-wallpaper stats, shared with the pager); the event chip
    // keeps its white-on-black pill, readable on any wallpaper.
    val glanceStyle = glanceStyleFor(labelCells)
    val glanceColor = glanceStyle.textColor ?: Color.White
    val glancePillBg = if (glanceColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(
                onClick = { openClock() },
                onLongClick = { showOptions = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (glanceStyle.needsPill) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glancePillBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text,
                        color = glanceColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text,
                    color = glanceColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(shadow = LabelShadow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (eventText != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { openCalendar() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        eventText,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("At a glance") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            scope.launch { app.settings.setAtAGlance(1) }
                            showOptions = false
                        },
                    ) { Text("Date only") }
                    TextButton(
                        onClick = {
                            scope.launch { app.settings.setAtAGlance(2) }
                            showOptions = false
                        },
                    ) { Text("Date + weather") }
                    TextButton(
                        onClick = {
                            scope.launch { app.settings.setAtAGlance(0) }
                            showOptions = false
                        },
                    ) { Text("Off") }
                    TextButton(
                        onClick = {
                            showOptions = false
                            onEnterCalm()
                        },
                    ) { Text("Calm mode") }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOptions = false
                        onOpenSettings()
                    },
                ) { Text("Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showOptions = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun ResizeDialog(
    item: HomeItem,
    colsU: Int,
    rowsU: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    step: Int = 1,
    items: List<HomeItem> = emptyList(),
    visiblePages: List<Int> = emptyList(),
) {
    val palette = LocalPalette.current
    // Flow rows size from colSpan/rowSpan only (rowSpan sets the height).
    val initW = item.colSpan.coerceIn(2, colsU)
    val initH = item.rowSpan.coerceIn(2, rowsU)
    // Folders step by 2: snap a legacy odd span to the nearest even on open.
    val startW = if (step == 2) (initW + 1) / 2 * 2 else initW
    val startH = if (step == 2) (initH + 1) / 2 * 2 else initH
    var w by remember(item.id) { mutableIntStateOf(startW) }
    var h by remember(item.id) { mutableIntStateOf(startH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resize") },
        text = {
            Column {
                StepperRow("Width", w, colsU, step) { w = it }
                StepperRow("Height", h, rowsU, step) { h = it }
                // M9 §3.4: built-in widgets offer size chips (Small 2×2,
                // Wide 4×2, Tall 2×4, Large 4×4) plus free resize inside
                // limits; folders keep free resize only.
                if (item.kind != ItemKind.FOLDER) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for ((label, cells) in WIDGET_SIZE_CHIPS) {
                            TextButton(onClick = {
                                val (cu, ru) = widgetChipUnits(cells, colsU, rowsU)
                                w = cu
                                h = ru
                            }) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Size: ${fmtUnits(w)} × ${fmtUnits(h)} cells",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
                // M10: the resize shows the cells it will claim AND
                // whether the new size fits here, moves, or has no
                // room — before commit, not as a post-commit toast.
                val plan = resizePlacement(item, w, h, items, colsU, rowsU)
                val otherPageFits = !plan.valid && plan.fallback == null &&
                    visiblePages.any { p ->
                        p != item.page &&
                            findSpot(items.filterNot { it.id == item.id }, p, w, h, colsU, rowsU) != null
                    }
                Text(
                    when {
                        plan.valid -> "Fits here — claims ${plan.cellsWide} × ${plan.cellsHigh} cells"
                        plan.fallback != null -> "No room here — will move to the nearest free spot"
                        otherPageFits -> "No room on this page — will move to another page"
                        else -> "No room anywhere for that size — try a smaller size"
                    },
                    color = palette.muted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(w, h) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StepperRow(label: String, value: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = palette.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange((value - step).coerceIn(2, max)) }) { Text("−") }
        Text(fmtUnits(value), color = palette.onSurface, fontSize = 14.sp)
        TextButton(onClick = { onChange((value + step).coerceIn(2, max)) }) { Text("+") }
    }
}

/**
 * The wallpaper layer behind everything. System mode leaves the window
 * transparent (the OS draws the system wallpaper); HavenOS mode draws its
 * own bitmap with crop zoom/offset, dim and tint. Pager parallax died
 * with pages (there is no page fraction to shift against any more).
 */
@Composable
private fun WallpaperLayer(
    app: HavenApp,
    state: app.havenos.launcher.data.SettingsState,
) {
    if (state.wallpaperMode != 1) return
    val source by app.wallpaper.source.collectAsState()
    val bmp = source ?: return
    val tokens = LocalTokens.current
    Box(Modifier.fillMaxSize()) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val zoom = state.wpZoom / 100f
                    scaleX = zoom
                    scaleY = zoom
                    translationX = state.wpOffX * size.width * 0.25f
                    translationY = state.wpOffY * size.height * 0.25f
                },
        )
        if (state.wpDim > 0) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = state.wpDim / 100f)))
        }
        if (state.wpTint != 0) {
            Box(Modifier.fillMaxSize().background(Color(state.wpTint)))
        }
        @Suppress("UNUSED_EXPRESSION") tokens
    }
}

/**
 * Async icon pipeline (M1 should-fix): bitmaps load on Dispatchers.Default
 * into the repository's LruCache; composition only reads the cache.
 */
@Composable
fun rememberAppIcon(appRepo: AppRepository, entry: AppEntry): ImageBitmap? {
    val pack = appRepo.activePack
    var bmp by remember(entry.key, pack) {
        mutableStateOf(appRepo.cachedBitmap(entry.key)?.asImageBitmap())
    }
    LaunchedEffect(entry.key, pack) {
        if (bmp == null) {
            bmp = withContext(Dispatchers.Default) { appRepo.iconBitmap(entry)?.asImageBitmap() }
        }
    }
    return bmp
}

private val MonoFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** App icon rendered with the user's shape mask, size and monochrome choice. */
@Composable
fun AppIconImage(entry: AppEntry, appRepo: AppRepository, size: androidx.compose.ui.unit.Dp) {
    val state = LocalState.current
    val palette = LocalPalette.current
    val bmp = rememberAppIcon(appRepo, entry)
    val shape = when (state.iconShape) {
        0 -> RoundedCornerShape(2.dp)
        1 -> RoundedCornerShape(size * 0.28f)
        2 -> RoundedCornerShape(size * 0.42f)
        else -> CircleShape
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = entry.label,
                modifier = Modifier.size(size).clip(shape),
                colorFilter = if (state.monoIcons) MonoFilter else null,
            )
        } else {
            // M9 Worker B (gold overuse): tonal tile + on-surface
            // letter, never a full accent block.
            Box(
                Modifier.size(size).clip(shape).background(palette.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    entry.label.firstOrNull()?.toString() ?: "?",
                    color = palette.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Notification badge (dot or count) for a package; hidden unless access is granted. */
@Composable
fun BadgeOverlay(pkg: String, extraCount: Int = -1) {
    val state = LocalState.current
    if (state.badgeMode == 0) return
    // Per-app dot silencing (Settings -> Icons): a silenced app shows no
    // dot or count, whatever the listener reports.
    if (pkg in parseMembers(state.badgeHiddenJson)) return
    val counts by BadgeStore.counts.collectAsState()
    val connected by BadgeStore.connected.collectAsState()
    if (!connected) return
    val count = if (extraCount >= 0) extraCount else (counts[pkg] ?: 0)
    if (count <= 0) return
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (state.badgeMode == 1) {
            Box(
                Modifier
                    .padding(2.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.5.dp, Color.White, CircleShape),
            )
        } else {
            Box(
                Modifier
                    .padding(1.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE53935))
                    .border(1.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    if (count > 99) "99+" else count.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(
    entry: AppEntry,
    appRepo: AppRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    // Draft override for the settings panel (DraftLayer): the
    // cell's own fields (icon size, labels) come from here. Leaf children
    // (AppIconImage, BadgeOverlay) still read LocalState, so mini callers
    // wrap in HavenTheme(draft) — MiniHome does — and the two agree.
    stateOverride: SettingsState? = null,
) {
    val state = stateOverride ?: LocalState.current
    val clickMod = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Column(
        modifier = modifier.then(clickMod).padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            AppIconImage(entry, appRepo, state.iconDp().dp)
            BadgeOverlay(entry.pkg)
        }
        if (state.labelsOn) {
            Text(
                text = entry.label,
                color = LocalPalette.current.onSurface,
                fontFamily = LocalFont.current,
                fontSize = state.labelSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = LabelShadow),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// M6: the M5 action bar is deleted — long-press empty home now enters
// edit mode directly, and its options (Add Widget, Wallpaper,
// Personalization, Settings, Done) live in the edit toolbar in HavenRoot.
// The long-press icon menu itself lives in PagedHome.kt (Edit label/icon
// · Hide · App info · Uninstall, plus Launch / Add to home / dock rows).
// Every row does what it says: label/icon writes persist through the
// Settings store and are read back by the catalog/icon pipeline; Hide uses
// the same hidden list as the drawer; App info / Uninstall hand off to the
// system, which confirms the uninstall itself.
// ---------------------------------------------------------------------------

/**
 * Shared per-app icon picker (grid home menu, drawer menu, scroll-home
 * menu): "Follow global pack" clears the override, "System icon" pins the
 * app's own icon, a pack pins that pack's icon for this app only. The
 * picked value is stored in perAppIconJson and read by AppRepository.icon.
 */
@Composable
fun AppIconPickerDialog(
    app: HavenApp,
    entry: AppEntry,
    state: SettingsState,
    onClose: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPack = remember(state.perAppIconJson, entry.key) {
        app.appRepo.perAppPack(entry.key)
    }
    // Pack discovery is a PackageManager query: off the main thread.
    var packs by remember {
        mutableStateOf<List<app.havenos.launcher.data.IconPack>>(emptyList())
    }
    LaunchedEffect(entry.key) {
        packs = withContext(Dispatchers.IO) { IconPacks.discover(context) }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Icon for ${entry.label}") },
        text = {
            Column {
                // "" = follow the global pack; "system" = this app's own icon.
                val rows = listOf("" to "Follow global pack") +
                    listOf("system" to "System icon") +
                    packs.map { it.pkg to it.label }
                for ((value, label) in rows) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val map = parseLabelOverrides(state.perAppIconJson).toMutableMap()
                                    if (value.isBlank()) map.remove(entry.key)
                                    else map[entry.key] = value
                                    app.settings.setPerAppIconJson(encodeLabelOverrides(map))
                                    app.appRepo.refresh()
                                }
                                onClose()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            color = palette.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (value == currentPack) {
                            Text("✓", color = palette.accent, fontSize = 15.sp)
                        }
                    }
                }
                if (packs.isEmpty()) {
                    Text(
                        "No icon packs installed — install one and it will appear here.",
                        color = palette.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
