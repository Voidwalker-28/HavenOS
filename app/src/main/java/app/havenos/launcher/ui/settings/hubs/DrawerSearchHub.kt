package app.havenos.launcher.ui.settings.hubs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.encodeCatMerges
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.hiddenApps
import app.havenos.launcher.data.parseCatMerges
import app.havenos.launcher.data.parseCatNames
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.ui.CATEGORY_NAMES
import app.havenos.launcher.ui.LibraryView
import kotlinx.coroutines.launch
import app.havenos.launcher.ui.settings.shell.SearchEntry

// Search index entries below use the shell's SearchEntry
// (ui/settings/shell/SettingsSearch.kt) — the one definition.

// ---------------------------------------------------------------------------
// Shared hub components (used by both hub files in this package).
// Titles stay short, values live on the right, help hides behind a tap.
// ---------------------------------------------------------------------------

@Composable
internal fun DsTopBar(onBack: () -> Unit, title: String) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
        Spacer(Modifier.width(4.dp))
        Text(title, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DsSection(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 20.dp, 16.dp, 8.dp),
    )
}

@Composable
internal fun HelpButton(help: String?) {
    if (help == null) return
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("ⓘ", fontSize = 16.sp) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            text = { Text(help) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("OK") } },
        )
    }
}

@Composable
internal fun DsRow(
    title: String,
    value: String,
    help: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, fontSize = 16.sp, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            trailing()
        } else {
            Text(
                value,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HelpButton(help)
    }
}

@Composable
internal fun DsSwitchRow(
    title: String,
    checked: Boolean,
    help: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    DsRow(
        title = title,
        value = if (checked) "On" else "Off",
        help = help,
        trailing = {
            Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
internal fun DsSliderRow(
    title: String,
    valueText: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    help: String? = null,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().padding(16.dp, 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueText, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HelpButton(help)
        }
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
        )
    }
}

@Composable
internal fun DsSegmentedRow(
    title: String,
    options: List<String>,
    selected: Int,
    help: String? = null,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp, 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
            HelpButton(help)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, label ->
                val isSel = i == selected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).clickable { onSelect(i) },
                ) {
                    Box(Modifier.padding(8.dp, 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DsMoreOptions(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("More options", fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(if (expanded) "Hide" else "Show", fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (expanded) content()
}

/** Drawer styles that actually render. Pages are gone: no pages thumbnail. */
private enum class DrawerStyleThumb { GRID, LIST, AZ, CATEGORIES, SEARCH_FIRST }

private fun styleThumbFor(mode: Int, scrollbar: Int): DrawerStyleThumb = when (mode) {
    0 -> DrawerStyleThumb.GRID
    2 -> DrawerStyleThumb.CATEGORIES
    3 -> DrawerStyleThumb.SEARCH_FIRST
    else -> if (scrollbar == 1) DrawerStyleThumb.AZ else DrawerStyleThumb.LIST
}

@Composable
private fun StyleThumbnail(kind: DrawerStyleThumb, selected: Boolean, onClick: () -> Unit) {
    val animatedPulse by rememberInfiniteTransition(label = "thumb").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "thumbPulse",
    )
    val pulse = if (selected) animatedPulse else 1f
    val label = when (kind) {
        DrawerStyleThumb.GRID -> "Grid"
        DrawerStyleThumb.LIST -> "List"
        DrawerStyleThumb.AZ -> "A–Z"
        DrawerStyleThumb.CATEGORIES -> "Categories"
        DrawerStyleThumb.SEARCH_FIRST -> "Search first"
    }
    Column(
        Modifier.width(104.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(104.dp, 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(10.dp),
        ) {
            when (kind) {
                DrawerStyleThumb.GRID -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) {
                                Box(
                                    Modifier.size(16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                            RoundedCornerShape(4.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
                DrawerStyleThumb.LIST -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(3) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(14.dp).background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                    RoundedCornerShape(4.dp),
                                ),
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier.size(44.dp, 8.dp).background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp),
                                ),
                            )
                        }
                    }
                }
                DrawerStyleThumb.AZ -> Row {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(14.dp).background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                        RoundedCornerShape(4.dp),
                                    ),
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier.size(34.dp, 8.dp).background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp),
                                    ),
                                )
                            }
                        }
                    }
                    Box(
                        Modifier.width(10.dp).fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                RoundedCornerShape(5.dp),
                            ),
                    )
                }
                DrawerStyleThumb.CATEGORIES -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(2) {
                        Box(
                            Modifier.fillMaxWidth().height(20.dp).background(
                                MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                RoundedCornerShape(8.dp),
                            ),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) {
                            Box(
                                Modifier.size(16.dp).background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                    RoundedCornerShape(4.dp),
                                ),
                            )
                        }
                    }
                }
                DrawerStyleThumb.SEARCH_FIRST -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(18.dp).background(
                            MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                            RoundedCornerShape(9.dp),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) {
                            Box(
                                Modifier.size(16.dp).background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                                    RoundedCornerShape(4.dp),
                                ),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DrawerSearchHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as HavenApp).settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    var moreOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var mergeTarget by remember { mutableStateOf<String?>(null) }

    val catNames = remember(state.catNamesJson) { parseCatNames(state.catNamesJson) }
    val catHidden = remember(state.catHiddenJson) { parseMembers(state.catHiddenJson).toSet() }
    val catMerges = remember(state.catMergeJson) { parseCatMerges(state.catMergeJson) }
    val history = remember(state.searchHistoryJson) { parseMembers(state.searchHistoryJson) }
    val hiddenCount = remember(state.hiddenJson) { state.hiddenApps().size }
    val currentStyle = styleThumbFor(state.drawerMode, state.scrollbarStyle)

    val contactsGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED
    val calendarGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scope.launch {
            settings.setSearchSources(true, state.searchCalendar, state.searchSettings, state.searchCalc)
        }
    }
    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scope.launch {
            settings.setSearchSources(state.searchContacts, true, state.searchSettings, state.searchCalc)
        }
    }

    fun applyStyle(thumb: DrawerStyleThumb) {
        onSpotlight("drawer")
        scope.launch {
            when (thumb) {
                DrawerStyleThumb.GRID -> settings.setDrawerMode(0)
                DrawerStyleThumb.LIST -> {
                    settings.setDrawerMode(1)
                    if (state.scrollbarStyle == 1) settings.setScrollbarStyle(0)
                }
                DrawerStyleThumb.AZ -> {
                    settings.setDrawerMode(1)
                    settings.setScrollbarStyle(1)
                }
                DrawerStyleThumb.CATEGORIES -> settings.setDrawerMode(2)
                DrawerStyleThumb.SEARCH_FIRST -> settings.setDrawerMode(3)
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { DsTopBar(onBack, "Drawer & search") }

        item { DsSection("Drawer style") }
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StyleThumbnail(DrawerStyleThumb.GRID, currentStyle == DrawerStyleThumb.GRID) {
                    applyStyle(DrawerStyleThumb.GRID)
                }
                StyleThumbnail(DrawerStyleThumb.LIST, currentStyle == DrawerStyleThumb.LIST) {
                    applyStyle(DrawerStyleThumb.LIST)
                }
                StyleThumbnail(DrawerStyleThumb.AZ, currentStyle == DrawerStyleThumb.AZ) {
                    applyStyle(DrawerStyleThumb.AZ)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StyleThumbnail(DrawerStyleThumb.CATEGORIES, currentStyle == DrawerStyleThumb.CATEGORIES) {
                    applyStyle(DrawerStyleThumb.CATEGORIES)
                }
                StyleThumbnail(DrawerStyleThumb.SEARCH_FIRST, currentStyle == DrawerStyleThumb.SEARCH_FIRST) {
                    applyStyle(DrawerStyleThumb.SEARCH_FIRST)
                }
            }
        }

        item {
            DsSegmentedRow(
                title = "Sort",
                options = listOf("A–Z", "Most used", "Newest"),
                selected = state.drawerSort.coerceIn(0, 2),
                help = "Newest sorts by install date.",
                onSelect = { i ->
                    onSpotlight("drawer-sort")
                    scope.launch { settings.setDrawerSort(i) }
                },
            )
        }
        item {
            DsSwitchRow(
                title = "Swipe up",
                checked = state.swipeUpDrawer,
                help = "Swipe up on home opens the drawer.",
                onCheckedChange = { on ->
                    onSpotlight("drawer")
                    scope.launch { settings.setSwipeUpDrawer(on) }
                },
            )
        }
        item {
            DsSegmentedRow(
                title = "Scroll aid",
                options = listOf("None", "Letters", "Thin bar"),
                selected = state.scrollbarStyle.coerceIn(0, 2),
                help = "Letters show in list; bar elsewhere.",
                onSelect = { i ->
                    onSpotlight("drawer")
                    scope.launch { settings.setScrollbarStyle(i) }
                },
            )
        }
        item {
            // M7 Wave 2: the Library's remembered default view. This is
            // the Settings-side control; the Library itself also reports
            // every view change through onViewChanged (see the host call
            // pattern in the Wave 2 worker report), so the two stay in
            // sync whichever side changes last.
            DsSegmentedRow(
                title = "Library view",
                options = listOf("All", "Categories", "List"),
                selected = when (LibraryView.fromKey(state.libraryDefaultView)) {
                    LibraryView.CATEGORIES -> 1
                    LibraryView.LIST -> 2
                    else -> 0
                },
                help = "View the Library opens in.",
                onSelect = { i ->
                    onSpotlight("drawer")
                    scope.launch {
                        settings.setLibraryDefaultView(
                            when (i) {
                                1 -> "categories"
                                2 -> "list"
                                else -> "all"
                            },
                        )
                    }
                },
            )
        }

        item { DsSection("Search sources") }
        item {
            DsSwitchRow(
                title = "Contacts",
                checked = state.searchContacts && contactsGranted,
                help = "Search contacts from the drawer.",
                onCheckedChange = { on ->
                    if (on && !contactsGranted) {
                        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        scope.launch {
                            settings.setSearchSources(on, state.searchCalendar, state.searchSettings, state.searchCalc)
                        }
                    }
                },
            )
        }
        item {
            DsSwitchRow(
                title = "Calendar",
                checked = state.searchCalendar && calendarGranted,
                help = "Search calendar events here.",
                onCheckedChange = { on ->
                    if (on && !calendarGranted) {
                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    } else {
                        scope.launch {
                            settings.setSearchSources(state.searchContacts, on, state.searchSettings, state.searchCalc)
                        }
                    }
                },
            )
        }
        item {
            DsSwitchRow(
                title = "Settings",
                checked = state.searchSettings,
                help = "Find settings from the drawer.",
                onCheckedChange = { on ->
                    scope.launch {
                        settings.setSearchSources(state.searchContacts, state.searchCalendar, on, state.searchCalc)
                    }
                },
            )
        }
        item {
            DsSwitchRow(
                title = "Calculator",
                checked = state.searchCalc,
                help = "Answer sums typed in search.",
                onCheckedChange = { on ->
                    scope.launch {
                        settings.setSearchSources(state.searchContacts, state.searchCalendar, state.searchSettings, on)
                    }
                },
            )
        }

        item {
            DsMoreOptions(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
                DsSection("Suggestions")
                DsRow(
                    title = "Suggestions",
                    value = "Always on",
                    help = "Top launches show before you type.",
                )
                DsRow(
                    title = "History",
                    value = "${history.size} saved",
                    help = "Shown in the search-first drawer.",
                    trailing = {
                        TextButton(
                            onClick = { scope.launch { settings.setSearchHistoryJson("[]") } },
                            enabled = history.isNotEmpty(),
                        ) { Text("Clear") }
                    },
                )
                DsRow(
                    title = "Hidden apps",
                    value = "$hiddenCount hidden",
                    help = "The list itself lives in Privacy.",
                    onClick = { onSpotlight("privacy:hidden") },
                )
                DsSection("Categories")
                for (id in CATEGORY_NAMES.keys.sorted()) {
                    val shown = id !in catHidden
                    val mergedInto = catMerges[id]
                    val display = catNames[id] ?: CATEGORY_NAMES[id] ?: id
                    val value = when {
                        mergedInto != null ->
                            "In ${catNames[mergedInto] ?: CATEGORY_NAMES[mergedInto] ?: mergedInto}"
                        !shown -> "Hidden"
                        else -> "Shown"
                    }
                    DsRow(
                        title = display,
                        value = value,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { renameTarget = id }) { Text("Rename") }
                                TextButton(onClick = { mergeTarget = id }) { Text("Merge") }
                                Switch(
                                    checked = shown,
                                    onCheckedChange = { on ->
                                        scope.launch {
                                            val next = if (on) catHidden - id else catHidden + id
                                            settings.setCatHiddenJson(encodeMembers(next.sorted()))
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
                DsRow(
                    title = "Reset drawer",
                    value = "Defaults",
                    onClick = { scope.launch { settings.resetSection(5) } },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    // Rename dialog: empty name clears the override.
    renameTarget?.let { id ->
        var text by remember(id) {
            mutableStateOf(catNames[id] ?: CATEGORY_NAMES[id] ?: id)
        }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename category") },
            text = {
                TextField(value = text, onValueChange = { text = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val next = parseCatNames(state.catNamesJson).toMutableMap()
                        if (text.isBlank() || text == CATEGORY_NAMES[id]) next.remove(id)
                        else next[id] = text.trim()
                        settings.setCatNamesJson(
                            org.json.JSONObject(next as Map<*, *>).toString(),
                        )
                    }
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    // Merge dialog: pick a target, or unmerge.
    mergeTarget?.let { id ->
        val targets = CATEGORY_NAMES.keys.filter { it != id }.sorted()
        AlertDialog(
            onDismissRequest = { mergeTarget = null },
            title = { Text("Merge category") },
            text = {
                Column {
                    if (catMerges[id] != null) {
                        TextButton(onClick = {
                            scope.launch {
                                val next = parseCatMerges(state.catMergeJson).toMutableMap()
                                next.remove(id)
                                settings.setCatMergeJson(encodeCatMerges(next))
                            }
                            mergeTarget = null
                        }) { Text("Unmerge") }
                    }
                    targets.forEach { target ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    val next = parseCatMerges(state.catMergeJson).toMutableMap()
                                    next[id] = target
                                    settings.setCatMergeJson(encodeCatMerges(next))
                                }
                                mergeTarget = null
                            }.padding(0.dp, 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = catMerges[id] == target,
                                onClick = {
                                    scope.launch {
                                        val next = parseCatMerges(state.catMergeJson).toMutableMap()
                                        next[id] = target
                                        settings.setCatMergeJson(encodeCatMerges(next))
                                    }
                                    mergeTarget = null
                                },
                            )
                            Text(catNames[target] ?: CATEGORY_NAMES[target] ?: target)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mergeTarget = null }) { Text("Close") }
            },
        )
    }
}

val DrawerSearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Drawer style", listOf("drawer", "grid", "list", "categories"), "Drawer & Search", "drawer-search:style"),
    SearchEntry("Sort", listOf("sort", "alphabetical", "recent", "most", "used", "newest"), "Drawer & Search", "drawer-search:sort"),
    SearchEntry("Swipe up", listOf("swipe", "drawer", "gesture"), "Drawer & Search", "drawer-search:swipe-up"),
    SearchEntry("Scroll aid", listOf("scrollbar", "letters", "rail", "bar"), "Drawer & Search", "drawer-search:scroll-aid"),
    SearchEntry("Library view", listOf("library", "view", "all", "categories", "list", "default"), "Drawer & Search", "drawer-search:library-view"),
    SearchEntry("Contacts", listOf("search", "contacts", "people"), "Drawer & Search", "drawer-search:contacts"),
    SearchEntry("Calendar", listOf("search", "calendar", "events"), "Drawer & Search", "drawer-search:calendar"),
    SearchEntry("Settings", listOf("search", "settings"), "Drawer & Search", "drawer-search:settings"),
    SearchEntry("Calculator", listOf("search", "calculator", "sums", "maths"), "Drawer & Search", "drawer-search:calculator"),
    SearchEntry("Suggestions", listOf("search", "suggestions", "top", "launches"), "Drawer & Search", "drawer-search:suggestions"),
    SearchEntry("History", listOf("search", "history", "clear", "recent"), "Drawer & Search", "drawer-search:history"),
    SearchEntry("Hidden apps", listOf("hide", "apps", "private", "secret"), "Drawer & Search", "drawer-search:hidden"),
    SearchEntry("Categories", listOf("categories", "tabs", "rename", "merge", "hide"), "Drawer & Search", "drawer-search:categories"),
)
