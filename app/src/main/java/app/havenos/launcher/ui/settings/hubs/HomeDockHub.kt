package app.havenos.launcher.ui.settings.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.WeatherNet
import app.havenos.launcher.data.gridCells
import app.havenos.launcher.data.LayoutMutation
import app.havenos.launcher.data.WindowClass
import app.havenos.launcher.data.densityGridCells
import app.havenos.launcher.ui.glass.GlassLevel
import app.havenos.launcher.ui.glass.GroupedSection
import app.havenos.launcher.ui.glass.HavenGlassDialog
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconImage
import app.havenos.launcher.ui.glass.HavenRadii
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.ui.glass.glass
import app.havenos.launcher.ui.gridChangePlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import app.havenos.launcher.ui.settings.shell.DeepLinkAnchor
import app.havenos.launcher.ui.settings.shell.LocalDraftLayer
import app.havenos.launcher.ui.settings.shell.SearchEntry
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.ui.parseCalmFavorites
import app.havenos.launcher.ui.parseCalmSchedule
import app.havenos.launcher.ui.clampDefaultPage
import app.havenos.launcher.ui.defaultItemLabel
import app.havenos.launcher.ui.pageSummaries
import app.havenos.launcher.ui.parseHiddenPages

val HomeDockSearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Grid", listOf("grid", "rows", "columns", "layout", "size"), "Home & Dock", "home-dock"),
    SearchEntry("Icon size", listOf("bigger", "icons", "scale", "size"), "Home & Dock", "home-dock"),
    SearchEntry("Dock labels", listOf("labels", "dock", "icon", "labels", "text"), "Home & Dock", "home-dock"),
    SearchEntry("Label size", listOf("labels", "text", "size", "font"), "Home & Dock", "home-dock"),
    SearchEntry("Lock layout", listOf("lock", "layout", "edit", "move", "drag"), "Home & Dock", "home-dock"),
    SearchEntry("Dock position", listOf("dock", "position", "bottom", "side", "rail"), "Home & Dock", "home-dock"),
    SearchEntry("Dock slots", listOf("dock", "slots", "icons", "count"), "Home & Dock", "home-dock"),
    SearchEntry("Recent apps", listOf("dock", "recent", "apps"), "Home & Dock", "home-dock"),
    SearchEntry("Auto-hide", listOf("dock", "auto", "hide"), "Home & Dock", "home-dock"),
    SearchEntry("Search pill", listOf("dock", "search", "pill"), "Home & Dock", "home-dock"),
    SearchEntry("Drawer button", listOf("dock", "drawer", "button", "apps"), "Home & Dock", "home-dock"),
    SearchEntry("Cell spacing", listOf("spacing", "padding", "gap", "grid"), "Home & Dock", "home-dock"),
    SearchEntry("Widget frame", listOf("widgets", "frame", "radius", "background", "transparency"), "Home & Dock", "home-dock"),
    SearchEntry("At a glance", listOf("clock", "date", "weather", "glance"), "Home & Dock", "home-dock"),
    SearchEntry("Snap to grid", listOf("snap", "grid", "drag", "drop"), "Home & Dock", "home-dock"),
    SearchEntry("Page indicator", listOf("page", "indicator", "dots", "pager"), "Home & Dock", "home-dock"),
    SearchEntry("Parallax", listOf("page", "parallax", "wallpaper", "paging"), "Home & Dock", "home-dock"),
    SearchEntry("Default page", listOf("page", "default", "first", "home"), "Home & Dock", "home-dock"),
    SearchEntry("Today section", listOf("today", "stack", "cards", "smart"), "Home & Dock", "home-dock"),
    SearchEntry("Auto-rotate", listOf("today", "stack", "rotate", "auto", "cards"), "Home & Dock", "home-dock"),
    SearchEntry("Calm favorites", listOf("calm", "favorites", "focus", "minimal"), "Home & Dock", "home-dock"),
    SearchEntry("Calm schedule", listOf("calm", "schedule", "focus", "time", "days"), "Home & Dock", "home-dock"),
    SearchEntry("Weather city", listOf("weather", "city", "location"), "Home & Dock", "home-dock"),
    SearchEntry("Weather units", listOf("weather", "celsius", "fahrenheit", "units"), "Home & Dock", "home-dock"),
)

// ---------------------------------------------------------------------------
// Home & Dock hub. Only controls whose writes visibly render. Deliberately
// absent (all Not-wired in AUDIT-M6, zero live consumers): search-bar
// position/style/height, status-bar mode, Today page, margins, cell
// spacing, label lines/colour, tablet grid (gridCells() has no tablet
// branch), dock rows/material/opacity/magnify/labels/floating, folder
// preview/anim/size/background/title, and the Attached/Floating dock
// switch (dockFloating is unread; the dock is always the floating pill
// — position is the live control).
// M7 Wave 2: Snap-to-grid is now a stored key (snapToGrid) with its own
// row below. The row also writes placementMode, because PagedHome /
// Home read snap from state.snapToGrid while setPlacementMode keeps
// both keys in sync (see the M7 integration notes); the toggle is
// live under both keys.
// ---------------------------------------------------------------------------

@Composable
fun HomeDockHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    // Page list for the default-page picker: the same source the page
    // overview uses (pageSummaries over live items + hidden pages),
    // with real catalog labels where the catalog has the app.
    val homeItems by app.db.homeItems().observeAll().collectAsState(initial = emptyList())
    val folders by app.db.folders().observeAll().collectAsState(initial = emptyList())
    val catalog by app.appRepo.apps.collectAsState()
    val folderNames = folders.associate { it.id to it.name }
    val labelFor: (app.havenos.launcher.data.HomeItem) -> String = { item ->
        catalog.firstOrNull { it.key == item.payload }?.label ?: defaultItemLabel(item, folderNames)
    }
    val summaries = pageSummaries(
        homeItems,
        parseHiddenPages(state.hiddenPagesJson),
        state.defaultPage,
        labelFor,
    )
    // Coordination fix §3: the visible pages a grid commit reflows
    // under — the SAME formula the picker's ghost plan uses (non-hidden
    // pages, falling back to all pages), so the preview's promise and
    // the commit's result can never disagree.
    val visibleGridPages = remember(homeItems, state.hiddenPagesJson, state.pageCount) {
        val hidden = parseHiddenPages(state.hiddenPagesJson)
        val total = maxOf((homeItems.maxOfOrNull { it.page } ?: -1) + 1, state.pageCount, 1)
        (0 until total).filter { it !in hidden }.ifEmpty { (0 until total).toList() }
    }
    // Grid-commit target profiles (§1): the tab's grid is shared by
    // its orientation family; the current profile is rewritten, the
    // other family member is marked dirty for switch-time repair.
    fun gridTargets(landscape: Boolean): Set<WindowClass> =
        if (landscape) setOf(WindowClass.EXPANDED_LANDSCAPE, WindowClass.MEDIUM)
        else setOf(WindowClass.COMPACT, WindowClass.MEDIUM)

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {

        HubSection("Grid")
        // 3.1 A10: the quick-customize Layout item deep-links here.
        // 3.1 Phase E: Find's grid control lands on the same row.
        DeepLinkAnchor(field = "home") {
        DeepLinkAnchor(field = "grid") {
        GridPickerRow(
            state = state,
            onSpotlight = { onSpotlight("grid") },
            onCommit = { landscape, cols, rows ->
                // Coordination fix §3: preview + commit are one
                // transaction now — the commit reflows under the new
                // grid and records one undo entry. The host reflow
                // effect stays as a safety net only (it no-ops on the
                // already-valid result).
                scope.launch {
                    app.layoutStore.commitMutation(
                        LayoutMutation.GridChange(cols, rows, landscape),
                        targetProfiles = gridTargets(landscape),
                        visiblePages = visibleGridPages,
                    )
                }
            },
            onFollowDensity = { landscape ->
                scope.launch {
                    val densityGrid = state.densityGridCells(landscape)
                    app.layoutStore.commitMutation(
                        LayoutMutation.GridChange(
                            densityGrid.first, densityGrid.second, landscape, followDensity = true,
                        ),
                        targetProfiles = gridTargets(landscape),
                        visiblePages = visibleGridPages,
                    )
                }
            },
        )
        }
        }

        HubSwitchRow(
            title = "Snap to grid",
            checked = state.snapToGrid,
            info = "Icons snap to the grid when dragged.",
            onSpotlight = { onSpotlight("grid") },
            onChange = { on ->
                // Writes the new key AND placementMode, so both keys
                // stay in sync for every reader.
                scope.launch {
                    settings.setSnapToGrid(on)
                    settings.setPlacementMode(if (on) 0 else 1)
                }
            },
        )

        // M8 pages. The overview (PageOverviewDialog) is the editor for
        // add/hide/reorder/delete; this section holds only the three
        // settings that belong here. No scroll-home-only row exists in
        // this hub (see the section comment above): every other row
        // still applies to the paged home, so none was deleted.
        HubSection("Pages")
        HubSwitchRow(
            title = "Page indicator",
            checked = state.pageIndicatorOn,
            info = "Dots above the dock show the page.",
            onSpotlight = { onSpotlight("home") },
            onChange = { scope.launch { settings.setPageIndicatorOn(it) } },
        )
        HubSwitchRow(
            title = "Parallax",
            checked = state.parallaxOn,
            info = "Wallpaper shifts slightly when paging.",
            onSpotlight = { onSpotlight("home") },
            onChange = { scope.launch { settings.setParallaxOn(it) } },
        )
        DefaultPageRow(
            summaries = summaries,
            defaultPage = clampDefaultPage(
                state.defaultPage,
                (summaries.maxOfOrNull { it.page } ?: 0) + 1,
            ),
            onSpotlight = { onSpotlight("home") },
            onSelect = { scope.launch { settings.setDefaultPage(it) } },
        )

        HubSection("Icons")
        IconSizeRow(
            state = state,
            onSpotlight = { onSpotlight("icons") },
            onCommit = { scope.launch { settings.setIconSizeOverride(it) } },
        )
        HubSwitchRow(
            title = "Dock labels",
            checked = state.labelsOn,
            info = "Labels under home icons.",
            onSpotlight = { onSpotlight("labels") },
            onChange = { scope.launch { settings.setLabels(it, state.labelSize) } },
        )
        HubSliderRow(
            title = "Label size",
            valueText = { v -> "${v.roundToInt()} sp" },
            value = state.labelSize.toFloat(),
            range = 9f..16f,
            onSpotlight = { onSpotlight("labels") },
            onCommit = { scope.launch { settings.setLabels(state.labelsOn, it.roundToInt()) } },
        )
        HubSwitchRow(
            title = "Lock layout",
            checked = state.lockLayout,
            info = "Blocks moves, adds and removes.",
            onSpotlight = { onSpotlight("home") },
            onChange = { scope.launch { settings.setLockLayout(it) } },
        )

        HubSection("Dock")
        // 3.1 A10: the quick-customize Dock item deep-links here.
        DeepLinkAnchor(field = "dock") {
        HubSegmented(
            title = "Dock position",
            options = listOf("Bottom", "Left", "Right"),
            selected = state.dockPos.coerceIn(0, 2),
            onSpotlight = { onSpotlight("dock") },
            // Coordination fix §3: dock position through commit. No
            // grid is passed: dock position never changes hostGridCells
            // (it depends on size class, screen, and rail — not dock
            // position), so the reflow is a no-op by the doc's rule.
            onSelect = { scope.launch { app.layoutStore.commitMutation(LayoutMutation.DockPositionChange(it)) } },
        )
        }
        DockPreviewSketch(position = state.dockPos)
        DockSlotsRow(
            slots = state.dockMax,
            onSpotlight = { onSpotlight("dock") },
            onChange = { scope.launch { settings.setDockMax(it) } },
        )
        HubSegmented(
            title = "At a glance",
            options = listOf("Off", "Date", "Date+weather"),
            selected = state.atAGlance.coerceIn(0, 2),
            info = "Line shown above the dock.",
            onSpotlight = { onSpotlight("home") },
            onSelect = { scope.launch { settings.setAtAGlance(it) } },
        )

        // M7 Wave 2: Today + Calm live here (no better home exists; the
        // Gestures & Motion hub owns gestures, not Today/Calm content).
        // No "Use new default" row: there is no distinct Wave 2 grid
        // default to re-apply — the grid default is already "Follow
        // density" in the Grid picker above, which clears overrides
        // without touching any home item. A duplicate row would only
        // risk re-flowing an existing layout.
        HubSection("Today & Calm")
        HubSwitchRow(
            title = "Today section",
            checked = state.todayOn,
            info = "Shows the Today column on home.",
            onSpotlight = { onSpotlight("home") },
            // Coordination fix §3: Today on/off through commit — page
            // structure changes atomically with one undo entry, and
            // placements are untouched.
            onChange = { scope.launch { app.layoutStore.commitMutation(LayoutMutation.TodayToggle(it)) } },
        )
        HubSwitchRow(
            title = "Auto-rotate stack",
            checked = state.smartStackAutoRotate,
            info = "Today cards rotate by time of day.",
            onSpotlight = { onSpotlight("home") },
            onChange = { scope.launch { settings.setSmartStackAutoRotate(it) } },
        )
        val calmFavCount = remember(state.calmFavoritesJson) {
            parseCalmFavorites(state.calmFavoritesJson).size
        }
        HubRow(
            title = "Calm favorites",
            value = if (calmFavCount == 0) "None" else "$calmFavCount chosen",
            info = "Cleared list can be re-picked in Calm mode.",
        ) {
            if (calmFavCount > 0) {
                OutlinedButton(
                    onClick = {
                        onSpotlight("home")
                        scope.launch { settings.setCalmFavoritesJson("") }
                    },
                ) { Text("Clear") }
            }
        }
        val calmScheduleText = remember(state.calmScheduleJson) {
            val sched = parseCalmSchedule(state.calmScheduleJson)
            if (!sched.enabled || sched.days.isEmpty()) {
                "Off"
            } else {
                val dayNames = mapOf(
                    1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
                    5 to "Thu", 6 to "Fri", 7 to "Sat",
                )
                val days = sched.days.sorted().joinToString(", ") { dayNames[it] ?: "?" }
                val fmt = { m: Int -> "${m / 60}:${(m % 60).toString().padStart(2, '0')}" }
                "$days ${fmt(sched.startMinutes)}–${fmt(sched.endMinutes)}"
            }
        }
        HubRow(
            title = "Calm schedule",
            value = calmScheduleText,
            info = "Stored value only; the schedule editor is not built yet.",
        ) {}

        HubMore {
            HubSection("Dock extras")
            // M7 Wave 2: this row now writes dockRecentsOn (the OneDock
            // flag) and, via setDockRecentsOn, the legacy dockRecent
            // Home.kt still reads — one toggle, both readers agree.
            HubSwitchRow(
                title = "Recent apps",
                checked = state.dockRecentsOn,
                info = "Recent apps follow pinned icons.",
                onSpotlight = { onSpotlight("dock") },
                onChange = { scope.launch { settings.setDockRecentsOn(it) } },
            )
            HubSwitchRow(
                title = "Auto-hide",
                checked = state.dockAutoHide,
                info = "Dock hides until summoned.",
                onSpotlight = { onSpotlight("dock") },
                onChange = { scope.launch { settings.setDockAutoHide(it) } },
            )
            HubSwitchRow(
                title = "Search pill",
                checked = state.dockSearch,
                info = "Search pill beside the dock.",
                onSpotlight = { onSpotlight("dock") },
                onChange = { scope.launch { settings.setDockSearch(it) } },
            )
            HubSwitchRow(
                title = "Drawer button",
                checked = state.dockDrawerBtn,
                info = "Drawer button beside the dock.",
                onSpotlight = { onSpotlight("dock") },
                onChange = { scope.launch { settings.setDockDrawerBtn(it) } },
            )

            HubSection("Spacing")
            SpacingRow(
                state = state,
                onSpotlight = { onSpotlight("grid") },
                onCommit = { scope.launch { settings.setGapDp(it) } },
            )

            HubSection("Widgets")
            DeepLinkAnchor(field = "widgets") {
            WidgetFrameRows(
                state = state,
                onSpotlight = { onSpotlight("widgets") },
                onCommit = { radius, bg, opacity ->
                    scope.launch { settings.setWidgetFrame(radius, bg, opacity) }
                },
            )
            }

            HubSection("Dynamic Island")
            DeepLinkAnchor(field = "island") {
            HubSwitchRow(
                title = "Dynamic Island",
                checked = state.islandOn,
                info = "A pill at the top of home for live activity: charging, now playing, and new notifications. Home only, off until you turn it on. Media and notifications need notification access (already used for badges); charging needs nothing. Nothing shows when nothing is live.",
                onSpotlight = { onSpotlight("home") },
                onChange = { on ->
                    scope.launch { settings.setIslandOn(on) }
                },
            )
            }

            HubSection("Weather")
            WeatherRows(
                state = state,
                onSpotlight = { onSpotlight("weather") },
                onCity = { city, lat, lon ->
                    scope.launch { settings.setWeather(city, lat, lon) }
                },
                onUnits = { scope.launch { settings.setWeatherUnits(it) } },
                onNetwork = { scope.launch { settings.setWeatherNetwork(it) } },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Default-page picker: one row per page from pageSummaries() (the same
 * source the overview uses), "Page N · first-app label", with "(hidden)"
 * on hidden pages. When there are no pages yet (empty home) it offers
 * the single Page 1. Today on/off is NOT duplicated here: the
 * "Today section" row below already covers it.
 */
@Composable
private fun DefaultPageRow(
    summaries: List<app.havenos.launcher.ui.PageSummary>,
    defaultPage: Int,
    onSpotlight: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val options = summaries.ifEmpty {
        listOf(app.havenos.launcher.ui.PageSummary(0, 0, "Empty"))
    }
    val selected = options.firstOrNull { it.page == defaultPage } ?: options.first()
    HubRow(
        title = "Default page",
        value = "Page ${selected.page + 1}",
        info = "First page shown on home.",
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            for (summary in options) {
                val isSelected = summary.page == selected.page
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (!isSelected) {
                            onSpotlight()
                            onSelect(summary.page)
                        }
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HavenIconImage(
                        if (isSelected) HavenIcon.RadioOn else HavenIcon.RadioOff,
                        null,
                        tint = if (isSelected) scheme.primary else scheme.onSurface.copy(alpha = 0.5f),
                        size = 20.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Page ${summary.page + 1} · ${summary.firstLabel}" +
                            if (summary.hidden) " (hidden)" else "",
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
/**
 * Interactive grid picker: tap or drag to choose columns x rows within
 * the 3..12 range gridCells() supports. Draft only until Apply. Shrinking
 * shows a ghost overlay (dropped cells) plus "N icons will move" before
 * anything is committed, and a confirm dialog on Apply. One picker
 * applies per orientation; there is no tablet tab because gridCells()
 * has no tablet branch.
 */
/**
 * Coordination fix §6: grid-overflow destinations as pure data, shared
 * by the picker's ghost text and the confirm dialog (one component, so
 * the two can never disagree).
 *
 * Entries are "page {pos} ({count})" in grid-page order, where pos is
 * the pager position (grid page + 1, or + 2 while Today occupies
 * pager position 1). The COUNT line is never collapsed — only this
 * list collapses, past 8 entries, with a "+M more" row expanding
 * inline to the full list (no dialog, no navigation).
 */
internal fun moveDestinationEntries(destinations: Map<Int, Int>, todayOn: Boolean): List<String> =
    destinations.entries.sortedBy { it.key }.map { (page, count) ->
        "page ${page + if (todayOn) 2 else 1} ($count)"
    }

/** Visible slice of [entries]: all when <= 8 or expanded, else the first 8 plus the hidden count. */
internal fun visibleDestinationEntries(entries: List<String>, expanded: Boolean): Pair<List<String>, Int> {
    if (expanded || entries.size <= 8) return entries to 0
    return entries.take(8) to (entries.size - 8)
}

/**
 * 3.1 Phase C: the grid-shrink move summary as one sentence. Up to 8
 * destinations are named in the sentence; beyond that the sentence
 * stops at the count and the expandable list names the rest.
 */
internal fun moveSummarySentence(movedCount: Int, entries: List<String>): String {
    val noun = if (movedCount == 1) "1 item will move" else "$movedCount items will move"
    if (entries.isEmpty() || entries.size > 8) return "$noun."
    val joined = when (entries.size) {
        1 -> entries[0]
        else -> entries.dropLast(1).joinToString(", ") + " and " + entries.last()
    }
    return "$noun to $joined."
}

/** 3.1 Phase C: label + minus/value/plus stepper for one grid axis (48dp targets). */
@Composable
private fun GridStepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.IconButton(
            onClick = { onChange((value - 1).coerceIn(3, 12)) },
            modifier = Modifier.size(48.dp),
        ) { HavenIconImage(HavenIcon.Minus, "Decrease $label") }
        Text(
            "$value",
            fontSize = 16.sp,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.material3.IconButton(
            onClick = { onChange((value + 1).coerceIn(3, 12)) },
            modifier = Modifier.size(48.dp),
        ) { HavenIconImage(HavenIcon.Plus, "Increase $label") }
    }
}

@Composable
private fun MoveDestinationsText(
    destinations: Map<Int, Int>,
    todayOn: Boolean,
    fontSize: TextUnit,
    color: androidx.compose.ui.graphics.Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val entries = moveDestinationEntries(destinations, todayOn)
    if (entries.isEmpty()) return
    val (shown, more) = visibleDestinationEntries(entries, expanded)
    Text(
        "Moves to: " + shown.joinToString(" · "),
        fontSize = fontSize,
        color = color,
    )
    if (more > 0) {
        Text(
            "+$more more",
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { expanded = true },
        )
    }
}

@Composable
private fun GridPickerRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCommit: (landscape: Boolean, cols: Int, rows: Int) -> Unit,
    onFollowDensity: (landscape: Boolean) -> Unit,
) {
    val app = LocalContext.current.applicationContext as HavenApp
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    var tabLandscape by remember { mutableStateOf(false) }
    var draftEpoch by remember { mutableIntStateOf(0) }
    val eff = state.gridCells(tabLandscape)
    val effCols = eff.first
    val effRows = eff.second
    val overrideCols = if (tabLandscape) state.colsLand else state.colsPort
    val isCustom = overrideCols > 0
    var draftCols by remember(tabLandscape, draftEpoch) { mutableIntStateOf(effCols) }
    var draftRows by remember(tabLandscape, draftEpoch) { mutableIntStateOf(effRows) }
    val isShrink = draftCols < effCols || draftRows < effRows
    val dirty = draftCols != effCols || draftRows != effRows
    val draftLayer = LocalDraftLayer.current
    // Live preview behind the panel: while the picker draft differs
    // from the committed grid, home renders the draft grid (DraftLayer).
    LaunchedEffect(draftCols, draftRows, tabLandscape) {
        if (draftCols != effCols || draftRows != effRows) {
            draftLayer?.begin(state)
            draftLayer?.preview { draft ->
                if (tabLandscape) draft.copy(colsLand = draftCols, rowsLand = draftRows)
                else draft.copy(colsPort = draftCols, rowsPort = draftRows)
            }
        }
    }
    var movedCount by remember { mutableIntStateOf(0) }
    var moveDestinations by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var confirmPending by remember { mutableStateOf(false) }
    // 3.1 A9: Follow-density confirmation state (dialog below).
    var followDensityPending by remember { mutableStateOf(false) }

    // Debounced impact plan over the stored grid model (the SAME
    // reflow the host applies on commit, run as a preview); 0 and no
    // destinations when not shrinking.
    LaunchedEffect(draftCols, draftRows, tabLandscape) {
        if (!isShrink) {
            movedCount = 0
            moveDestinations = emptyMap()
            return@LaunchedEffect
        }
        delay(250)
        try {
            val items = app.db.homeItems().all()
            val hidden = parseHiddenPages(state.hiddenPagesJson)
            val total = maxOf((items.maxOfOrNull { it.page } ?: -1) + 1, state.pageCount, 1)
            val visible = (0 until total).filter { it !in hidden }.ifEmpty { (0 until total).toList() }
            val plan = gridChangePlan(items, draftCols, draftRows, visible)
            movedCount = plan.movedCount
            moveDestinations = plan.destinations
        } catch (t: Throwable) {
            movedCount = 0
            moveDestinations = emptyMap()
        }
    }

    // 3.1 Phase C: one-sentence move summary, shared by the preview
    // grouped section and the shrink confirm dialog.
    val moveEntries = moveDestinationEntries(moveDestinations, state.todayOn)
    val moveSentence = moveSummarySentence(movedCount, moveEntries)

    Column {
    HubRow(
        title = "Grid",
        value = "$draftCols × $draftRows",
        info = "Portrait and landscape grids are set separately.",
    ) {
        HubSegmented(
            title = "Orientation",
            options = listOf("Portrait", "Landscape"),
            selected = if (tabLandscape) 1 else 0,
            onSelect = { tabLandscape = it == 1 },
        )
        GridStepperRow("Columns", draftCols) { draftCols = it }
        GridStepperRow("Rows", draftRows) { draftRows = it }
        Text(
            if (isCustom) "Custom" else "Follows density",
            fontSize = 13.sp,
            color = scheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 4.dp),
        )
        val totalCols = maxOf(draftCols, effCols).coerceIn(3, 12)
        val totalRows = maxOf(draftRows, effRows).coerceIn(3, 12)
        val cell = 22.dp
        val pitchPx = with(density) { 25.dp.toPx() }
        fun setFrom(offsetX: Float, offsetY: Float) {
            draftCols = ((offsetX / pitchPx).toInt() + 1).coerceIn(3, 12)
            draftRows = ((offsetY / pitchPx).toInt() + 1).coerceIn(3, 12)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .pointerInput(totalCols, totalRows) {
                    detectDragGestures(
                        onDragStart = { setFrom(it.x, it.y) },
                        onDrag = { change, _ -> setFrom(change.position.x, change.position.y) },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (r in 0 until totalRows) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (c in 0 until totalCols) {
                        val inDraft = c < draftCols && r < draftRows
                        val ghost = !inDraft && c < effCols && r < effRows
                        Box(
                            Modifier.size(cell).clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        inDraft -> scheme.primary
                                        ghost -> scheme.error.copy(alpha = 0.35f)
                                        else -> scheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                )
                                .clickable {
                                    draftCols = (c + 1).coerceIn(3, 12)
                                    draftRows = (r + 1).coerceIn(3, 12)
                                },
                        )
                    }
                }
            }
        }
        // 3.1 Phase C: the button row scrolls instead of squeezing, and
        // the Follow-density button has a minimum width with no wrap,
        // so its label can never wrap letter-by-letter.
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                enabled = dirty,
                onClick = {
                    if (isShrink && movedCount > 0) {
                        confirmPending = true
                    } else {
                        draftLayer?.discard()
                        onSpotlight()
                        onCommit(tabLandscape, draftCols, draftRows)
                    }
                },
            ) { Text("Apply") }
            OutlinedButton(
                enabled = dirty,
                onClick = { draftLayer?.discard(); draftEpoch++ },
            ) { Text("Cancel") }
            OutlinedButton(
                modifier = Modifier.sizeIn(minWidth = 148.dp),
                onClick = {
                    // 3.1 A9: a custom grid for this orientation is
                    // a density-independent size — resetting it to
                    // density needs an explicit Keep/Reset choice,
                    // never a silent clear. No override → no-op.
                    if (isCustom) followDensityPending = true
                    else {
                        draftLayer?.discard()
                        onFollowDensity(tabLandscape)
                        draftEpoch++
                    }
                },
            ) { Text("Follow density", maxLines = 1, softWrap = false) }
        }
    }
    // 3.1 Phase C: the shrink preview is its own grouped section with
    // the move summary as one sentence + the expandable list past 8.
    if (isShrink && movedCount > 0) {
        Spacer(Modifier.height(12.dp))
        GroupedSection(
            label = "Grid change preview",
            rows = listOf({
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(minOf(movedCount, 12)) {
                            Box(
                                Modifier.size(14.dp).padding(end = 2.dp).clip(CircleShape)
                                    .background(scheme.onSurface.copy(alpha = 0.25f)),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        moveSentence,
                        fontSize = 13.sp,
                        color = scheme.onSurface.copy(alpha = 0.75f),
                    )
                    if (moveEntries.size > 8) {
                        Spacer(Modifier.height(4.dp))
                        MoveDestinationsText(
                            destinations = moveDestinations,
                            todayOn = state.todayOn,
                            fontSize = 13.sp,
                            color = scheme.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            }),
        )
    }
    }
    // 3.1 Phase C: Glass3 dialog (title + one-sentence summary +
    // expandable list + Cancel text button + one filled button).
    if (confirmPending) {
        val roles = LocalAccentRoles.current
        Dialog(onDismissRequest = { confirmPending = false }) {
            Column(
                Modifier.fillMaxWidth()
                    .glass(GlassLevel.GLASS3, HavenRadii.dialogShape())
                    .padding(24.dp),
            ) {
                Text("Shrink the grid?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(minOf(movedCount, 12)) {
                        Box(
                            Modifier.size(16.dp).padding(end = 2.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(moveSentence, fontSize = 14.sp)
                if (moveEntries.size > 8) {
                    Spacer(Modifier.height(4.dp))
                    MoveDestinationsText(
                        destinations = moveDestinations,
                        todayOn = state.todayOn,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { confirmPending = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            confirmPending = false
                            draftLayer?.discard()
                            onSpotlight()
                            onCommit(tabLandscape, draftCols, draftRows)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(roles.accent),
                            contentColor = Color(roles.onAccent),
                        ),
                    ) { Text("Apply", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    // 3.1 A9: density reset confirmation. Grids are per-orientation,
    // so one tap resets exactly one custom size (this tab's grid);
    // Keep is the default (dismiss), Reset commits through the same
    // undoable GridChange path, so toolbar Undo restores it.
    if (followDensityPending) {
        val roles = LocalAccentRoles.current
        Dialog(onDismissRequest = { followDensityPending = false }) {
            Column(
                Modifier.fillMaxWidth()
                    .glass(GlassLevel.GLASS3, HavenRadii.dialogShape())
                    .padding(24.dp),
            ) {
                Text("Follow density?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Density will reset 1 custom size. Keep it or reset?", fontSize = 14.sp)
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { followDensityPending = false }) {
                        Text("Keep", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            followDensityPending = false
                            draftLayer?.discard()
                            onFollowDensity(tabLandscape)
                            draftEpoch++
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(roles.accent),
                            contentColor = Color(roles.onAccent),
                        ),
                    ) { Text("Reset", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun IconSizeRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCommit: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isAuto = state.iconSizeOverride < 0
    val shown = if (isAuto) state.iconDp().toFloat() else state.iconSizeOverride.toFloat()
    var drag by remember(shown) { mutableFloatStateOf(shown) }
    HubRow(title = "Icon size", value = if (isAuto) "Auto" else "${state.iconSizeOverride} dp") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = drag,
                onValueChange = { drag = it },
                onValueChangeFinished = { onSpotlight(); onCommit(drag.roundToInt().coerceIn(40, 68)) },
                valueRange = 40f..68f,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onSpotlight(); onCommit(-1) }) {
                Text(
                    "Auto",
                    color = if (isAuto) scheme.primary else scheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun DockSlotsRow(
    slots: Int,
    onSpotlight: () -> Unit,
    onChange: (Int) -> Unit,
) {
    HubRow(title = "Dock slots", value = "$slots") {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = slots > 3,
                onClick = { onSpotlight(); onChange((slots - 1).coerceIn(3, 6)) },
            ) { Text("−") }
            Text("$slots", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            OutlinedButton(
                enabled = slots < 6,
                onClick = { onSpotlight(); onChange((slots + 1).coerceIn(3, 6)) },
            ) { Text("+") }
        }
    }
}

/** Schematic preview of the live dock geometry: bottom pill vs side rail. */
@Composable
private fun DockPreviewSketch(position: Int) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, scheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
    ) {
        when (position) {
            1 -> Box(
                Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                    .size(28.dp, 76.dp).clip(RoundedCornerShape(50))
                    .background(scheme.primary.copy(alpha = 0.35f)),
            )
            2 -> Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
                    .size(28.dp, 76.dp).clip(RoundedCornerShape(50))
                    .background(scheme.primary.copy(alpha = 0.35f)),
            )
            else -> Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                    .size(180.dp, 28.dp).clip(RoundedCornerShape(50))
                    .background(scheme.primary.copy(alpha = 0.35f)),
            )
        }
    }
}

/**
 * The one live spacing value: gapDp, the per-cell padding the home
 * and dock actually read. While dragging, a grid overlay shows the gap.
 */
@Composable
private fun SpacingRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCommit: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var previewGap by remember { mutableFloatStateOf(state.gapDp.toFloat()) }
    val scheme = MaterialTheme.colorScheme
    HubSliderRow(
        title = "Cell spacing",
        valueText = { v -> "${v.roundToInt()} dp" },
        value = state.gapDp.toFloat(),
        range = 0f..12f,
        info = "Padding inside each home cell.",
        onSpotlight = { onSpotlight() },
        onPreview = { previewGap = it; dragging = true },
        onCommit = {
            dragging = false
            onCommit(it.roundToInt().coerceIn(0, 12))
        },
    )
    if (dragging) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(previewGap.dp),
        ) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(previewGap.dp)) {
                    repeat(4) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(scheme.primary.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Widget frame: radius / background / transparency. Honest scope — these
 * keys are Partial in AUDIT-M6 (only a Settings sample reads them; no
 * real home widget does yet), so the preview is a live preview card and
 * the help text says so. Solid ignores transparency, so that slider disables.
 */
@Composable
private fun WidgetFrameRows(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCommit: (radius: Int, bg: Int, opacity: Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    HubSliderRow(
        title = "Frame radius",
        valueText = { v -> "${v.roundToInt()} dp" },
        value = state.widgetRadius.toFloat(),
        range = 0f..32f,
        onSpotlight = { onSpotlight() },
        onCommit = {
            onCommit(it.roundToInt().coerceIn(0, 32), state.widgetBg, state.widgetOpacity)
        },
    )
    HubSegmented(
        title = "Frame background",
        options = listOf("Follow look", "Solid", "Translucent"),
        selected = state.widgetBg.coerceIn(0, 2),
        onSpotlight = { onSpotlight() },
        onSelect = { onCommit(state.widgetRadius, it, state.widgetOpacity) },
    )
    HubSliderRow(
        title = "Frame transparency",
        valueText = { v -> "${v.roundToInt()}%" },
        value = state.widgetOpacity.toFloat(),
        range = 0f..100f,
        enabled = state.widgetBg != 1,
        info = "Solid ignores transparency.",
        onSpotlight = { onSpotlight() },
        onCommit = {
            onCommit(state.widgetRadius, state.widgetBg, it.roundToInt().coerceIn(0, 100))
        },
    )
    HubRow(
        title = "Frame preview",
        value = "",
        info = "Preview only; placed widgets keep their own frame.",
    ) {
        val base = scheme.surface
        val bg = when (state.widgetBg) {
            1 -> base
            2 -> base.copy(alpha = 0.6f)
            else -> base
        }.let { color ->
            if (state.widgetBg == 1) color
            else color.copy(alpha = color.alpha * (state.widgetOpacity / 100f))
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp)
                .clip(RoundedCornerShape(state.widgetRadius.dp))
                .background(bg)
                .border(1.dp, scheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(state.widgetRadius.dp))
                .padding(14.dp),
        ) {
            Column {
                Text("10:09", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Widget", fontSize = 13.sp, color = scheme.onSurface.copy(alpha = 0.65f))
            }
        }
    }
}

/**
 * Built-in clock + weather. Weather location and units appear only after
 * the location step: with no city set, only "Set location" shows. Weather
 * is the optional toggleable exception to offline (Open-Meteo).
 */
@Composable
private fun WeatherRows(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCity: (city: String, lat: String, lon: String) -> Unit,
    onUnits: (Int) -> Unit,
    onNetwork: (Boolean) -> Unit,
) {
    var cityOpen by remember { mutableStateOf(false) }
    HubRow(title = "Weather city", value = state.weatherCity.ifBlank { "Not set" }) {
        Button(onClick = { cityOpen = true }) {
            Text(if (state.weatherCity.isBlank()) "Set location" else "Change")
        }
    }
    if (state.weatherCity.isNotBlank()) {
        HubSegmented(
            title = "Weather units",
            options = listOf("°C", "°F"),
            selected = state.weatherUnits.coerceIn(0, 1),
            onSpotlight = { onSpotlight() },
            onSelect = { onUnits(it) },
        )
        HubSwitchRow(
            title = "Weather network",
            checked = state.weatherNetwork,
            info = "Off means cached data only.",
            onSpotlight = { onSpotlight() },
            onChange = { onNetwork(it) },
        )
    }
    if (cityOpen) {
        WeatherCityDialog(
            onDismiss = { cityOpen = false },
            onPick = { label, lat, lon ->
                cityOpen = false
                onSpotlight()
                onCity(label, lat.toString(), lon.toString())
            },
        )
    }
}

@Composable
private fun WeatherCityDialog(
    onDismiss: () -> Unit,
    onPick: (label: String, lat: Double, lon: Double) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    HavenGlassDialog(
        onDismiss = onDismiss,
        title = "Weather city",
        buttons = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = scheme.onSurface.copy(alpha = 0.6f))
            }
        },
        content = {
            Text(
                "Type a city — no location permission is used. Needs internet.",
                color = scheme.onSurface.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("City name", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                if (query.isBlank() || searching) return@TextButton
                searching = true
                scope.launch {
                    val found = withContext(Dispatchers.IO) { WeatherNet.geocode(query.trim(), 5) }
                    results = found
                    searched = true
                    searching = false
                }
            }) {
                Text(
                    if (searching) "Searching…" else "Search",
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (searched && results.isEmpty()) {
                Text(
                    "No matches — check the spelling or connection.",
                    color = scheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            for ((label, lat, lon) in results) {
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(label, lat, lon) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(label, fontSize = 14.sp)
                }
            }
        },
    )
}
