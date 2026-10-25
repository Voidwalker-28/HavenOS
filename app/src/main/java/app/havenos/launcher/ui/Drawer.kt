package app.havenos.launcher.ui

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.havenos.launcher.HavenApp
import app.havenos.launcher.R
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.CalEvent
import app.havenos.launcher.data.CalendarRepo
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.encodeLabelOverrides
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.evaluate
import app.havenos.launcher.data.formatCalcResult
import app.havenos.launcher.data.hiddenApps
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.parseAllowedCalendars
import app.havenos.launcher.data.parseLabelOverrides
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalTokens
import app.havenos.launcher.ui.settings.shell.InlineSlider
import app.havenos.launcher.ui.settings.shell.InlineSwitch
import app.havenos.launcher.ui.settings.shell.SearchEntry
import app.havenos.launcher.ui.settings.shell.SettingsSearchControls
import app.havenos.launcher.ui.settings.shell.TextFieldDialog
import app.havenos.launcher.ui.settings.shell.searchSettingsEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// M7 Wave 1 — Library (drawer) + Search card.
//
// This file owns the Library. The home is ONE continuous scroll (no pages,
// no page language anywhere here); the Library is a full-screen overlay
// on a dimmed wallpaper scrim, with All / Categories / List views.
// Clean-room: interaction patterns only, no copied code or assets.
// No new network calls: the only network use remains the existing
// weather feature. Usage ranking is local only.
//
// COORDINATOR PLUG-IN POINTS (things this file deliberately does NOT own):
// 1. Categories tab: [LibraryOverlay] takes `categoriesContent`. When it
//    is null the Categories tab is hidden. To plug the Categories
//    worker's view in, pass:
//      categoriesContent = {
//          CategoriesView(
//              apps = <visible apps>, appRepo = app.appRepo,
//              overridesJson = <categoryOverridesJson, coordinator-owned>,
//              expanded = <maxWidth >= 840.dp>,
//              onLaunch = { ... },
//              onMoveApp = { key, id -> /* persist via
//                  categoryOverridesWithMove */ },
//              onRenameCategory = { id, name -> /* persist via
//                  categoryOverridesWithRename */ },
//          )
//      }
// 2. Remembered view: the Library reports every view change through
//    `onViewChanged`. Persist the reported [LibraryView.key] under the
//    setting key [LIBRARY_DEFAULT_VIEW_KEY] ("libraryDefaultView",
//    String, "all"/"categories"/"list", default "all") and pass it back
//    as `initialView = LibraryView.fromKey(stored)` on the next open.
//    This file reads no such setting directly — SettingsState does not
//    carry it yet — so until the coordinator adds it, the view is
//    session-local (initialView, default ALL) and view changes are
//    reported but not persisted anywhere.
// 3. Drag-out drop contract (see [libraryDragSource]): the home / dock
//    drop target belongs to the coordinator. When a drag starts,
//    [LibraryOverlay] fires `onDragOut` with the payload; the
//    coordinator should dismiss or lower the Library overlay at that
//    point so its home/dock drop target can receive the platform drag
//    (window-scoped drag-and-drop keeps running across the overlay
//    change). The target must accept MIME [LIBRARY_DRAG_MIME] and
//    decode the ClipData item text with [decodeLibraryDragPayload]:
//      Modifier.dragAndDropTarget(
//          shouldStartDragAndDrop = {
//              it.clipDescription?.hasMimeType(LIBRARY_DRAG_MIME) == true
//          },
//          target = object : DragAndDropTarget {
//              override fun onDrop(event: DragAndDropEvent): Boolean {
//                  val payload = event.clipData.getItemAt(0).text
//                      ?.let(::decodeLibraryDragPayload) ?: return false
//                  // Resolve payload.appKey against the app catalog
//                  // (fallback: payload.pkg + payload.cls), then place
//                  // the app on the home / dock at the drop position.
//                  return true
//              }
//          },
//      )
//    UNVERIFIED on device: drag-across-overlay-dismissal, drop-target hit
//    testing under the overlay, and TalkBack announcements of the
//    scrubber all still need a Pad run (the coordinator owns the build).
// 4. Hosting: [SearchCard] is the standalone search surface for
//    swipe-down on empty home and the dock Search button. The Library's
//    own field renders the same grouped results inline (shared
//    [SearchResultsList], one scroll container, no duplicate lists).
// ---------------------------------------------------------------------------

/** The 14 M5 Settings category names, in registry order, used by drawer
 * search and by the ⋮ -> Drawer settings entry point. Home maps a name to
 * its registry id via indexOf + 1, so this list must stay in registry order. */
val SETTINGS_SECTIONS = listOf(
    "Style & Theme", "Home screen", "Wallpaper", "Dock", "App drawer",
    "Icons", "Folders", "Widgets", "Gestures", "Search & Smart",
    "Animations", "Privacy & Lock", "Backup & Profiles", "System",
)

val CATEGORY_NAMES: Map<String, String> = mapOf(
    "game" to "Games",
    "social" to "Social",
    "audio" to "Music & Audio",
    "video" to "Video",
    "image" to "Photos",
    "news" to "News",
    "maps" to "Maps & Travel",
    "productivity" to "Productivity",
    "other" to "Other",
)

private data class ContactHit(val name: String, val number: String, val uri: Uri)

/**
 * The drawer's legacy search ranking, kept verbatim for the settings
 * sample search pad, which runs the REAL ranking, not a copy of it.
 * The M7 Library/Search card rank with [rankLibraryApps] instead
 * (prefix > word > contains > fuzzy + launch-count/recency boost);
 * this function is unchanged so existing callers do not shift.
 * Order: label-starts-with, then contains, then subsequence-fuzzy; each
 * band sorted by launch count desc, then label. Never throws on bad input.
 */
fun rankSearchApps(
    apps: List<AppEntry>,
    query: String,
    stats: Map<String, Pair<Int, Long>>,
): List<AppEntry> {
    val lower = query.lowercase()
    val starts = apps.filter { it.label.lowercase().startsWith(lower) }
    val contains = apps.filter {
        it.label.contains(query, ignoreCase = true) && it !in starts
    }
    val fuzzy = apps.filter {
        it !in starts && it !in contains && isSubsequence(lower, it.label.lowercase())
    }
    val byCount = compareByDescending<AppEntry> { stats[it.key]?.first ?: 0 }
        .thenBy { it.label.lowercase() }
    return starts.sortedWith(byCount) + contains.sortedWith(byCount) +
        fuzzy.sortedWith(byCount)
}

private fun queryContacts(ctx: Context, q: String): List<ContactHit> = try {
    val out = mutableListOf<ContactHit>()
    val proj = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
    )
    ctx.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        proj,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
        arrayOf("%$q%"),
        null,
    )?.use { c ->
        val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val keyIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
        while (c.moveToNext() && out.size < 5) {
            val lookup = c.getString(keyIdx) ?: continue
            out += ContactHit(
                name = c.getString(nameIdx) ?: "",
                number = c.getString(numIdx) ?: "",
                uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup),
            )
        }
    }
    out
} catch (t: Throwable) {
    emptyList()
}

/**
 * Internal (not private) so the settings panel search (shell search
 * ranking) classifies matches with the drawer's exact rule.
 */
internal fun isSubsequence(needle: String, haystack: String): Boolean {
    if (needle.isEmpty()) return true
    var i = 0
    for (c in haystack) {
        if (c == needle[i]) {
            i++
            if (i == needle.length) return true
        }
    }
    return false
}

private fun openWebSearch(context: Context, query: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_WEB_SEARCH)
                .putExtra(SearchManager.QUERY, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (t: Throwable) {
        // No browser / search handler installed; nothing to open.
    }
}

/**
 * Legacy entry point, signature-compatible with the existing Home host.
 * Maps the old drawerMode (0 grid, 1 list, 2 categories, 3 search-first)
 * onto [LibraryView]; view changes are written back to the legacy
 * drawerMode key, and `categoriesContent` / `onDragOut` pass through to
 * [LibraryOverlay] for the coordinator to plug in. The M7 entry point
 * for new hosts is [LibraryOverlay] directly.
 */
@Composable
fun DrawerOverlay(
    app: HavenApp,
    state: SettingsState,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onAddToHome: (AppEntry) -> Unit,
    onOpenSettings: (String) -> Unit = {},
    categoriesContent: (@Composable () -> Unit)? = null,
    onDragOut: ((LibraryDragPayload) -> Unit)? = null,
    // Panel search preview: suppresses the search
    // auto-focus, so a mini drawer never pops the keyboard in Settings.
    preview: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val initialView = when (state.drawerMode) {
        0 -> LibraryView.ALL
        1 -> LibraryView.LIST
        2 -> LibraryView.CATEGORIES
        else -> LibraryView.ALL
    }
    LibraryOverlay(
        app = app,
        state = state,
        initialView = initialView,
        categoriesContent = categoriesContent,
        focusSearch = focusSearch || state.drawerMode == 3,
        onClose = onClose,
        onLaunch = onLaunch,
        onAddToHome = onAddToHome,
        onOpenSettings = onOpenSettings,
        onDragOut = onDragOut,
        onViewChanged = { view ->
            scope.launch {
                app.settings.setDrawerMode(
                    when (view) {
                        LibraryView.ALL -> 0
                        LibraryView.LIST -> 1
                        LibraryView.CATEGORIES -> 2
                    },
                )
            }
        },
        preview = preview,
    )
}

/**
 * The M7 Library (app drawer): full-screen overlay on a dimmed scrim.
 *
 * Top: centred 56dp search pill (~560dp on Expanded) + segmented
 * control All · Categories · List (Categories hidden when
 * [categoriesContent] is null) + sort chip + a Suggested row of up to
 * 8 icons from local launch history (hidden when empty).
 *
 * All view: icon grid (~7 columns on Expanded) with section headers by
 * letter as small floating labels (12sp caps, muted, no background bar).
 * List view: 56dp rows at 16sp, sticky letter headers, two columns on
 * Expanded, one column on Compact. The scrubber (48dp strip, A–Z + #,
 * ~72dp bubble near the finger, haptic tick per letter, TalkBack
 * adjustable semantics) renders and drives scrolling in both views
 * whenever letter sections exist — the M6 stored-only scrubber is gone.
 *
 * Long-press on an icon starts a drag-out ([libraryDragSource]); the
 * icon menu moved to the ⋮ affordance on each row / grid cell so the
 * two gestures cannot fight. Recently installed, never-opened apps
 * carry a subtle accent "New" dot ([isNewApp]). The stuck
 * "Long-press an app for options" hint from the old drawer is deleted.
 *
 * @param initialView the view to open on; the coordinator passes
 *   `LibraryView.fromKey(<libraryDefaultView>)` once it owns the key.
 * @param onViewChanged fired on every view change; the coordinator
 *   persists the key (see the file header). Null = session-local only.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryOverlay(
    app: HavenApp,
    state: SettingsState,
    initialView: LibraryView = LibraryView.ALL,
    categoriesContent: (@Composable () -> Unit)? = null,
    focusSearch: Boolean = false,
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onAddToHome: (AppEntry) -> Unit,
    onOpenSettings: (String) -> Unit = {},
    onDragOut: ((LibraryDragPayload) -> Unit)? = null,
    onViewChanged: ((LibraryView) -> Unit)? = null,
    preview: Boolean = false,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val allApps by app.appRepo.apps.collectAsState()
    val stats by app.appRepo.stats.collectAsState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var view by remember(initialView) { mutableStateOf(initialView) }
    var menuEntry by remember { mutableStateOf<AppEntry?>(null) }
    var labelDialogEntry by remember { mutableStateOf<AppEntry?>(null) }
    var iconDialogEntry by remember { mutableStateOf<AppEntry?>(null) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Categories with no plugged content fall back to All: the tab is
    // hidden in that case, but a stale initialView must never strand
    // the user on an empty screen.
    val effectiveView = if (view == LibraryView.CATEGORIES && categoriesContent == null) {
        LibraryView.ALL
    } else {
        view
    }

    LaunchedEffect(focusSearch) {
        if (!preview && focusSearch) focusRequester.requestFocus()
    }

    fun closeLibrary() {
        focusManager.clearFocus()
        keyboard?.hide()
        onClose()
    }

    val hidden = remember(state.hiddenJson) { state.hiddenApps() }
    val visibleApps = remember(allApps, hidden) { allApps.filter { it.key !in hidden } }

    // Sorting (drawerSort). All view always groups alphabetically (its
    // letter headers require it); the sort chip reorders the List view
    // and the Suggested row's tie-breaks, not the All grid's sections.
    val sorted = remember(visibleApps, state.drawerSort, stats) {
        when (state.drawerSort) {
            1 -> visibleApps.sortedWith(
                compareByDescending<AppEntry> { stats[it.key]?.first ?: 0 }
                    .thenBy { it.label.lowercase() },
            )
            2 -> visibleApps.sortedWith(
                compareByDescending<AppEntry> { it.installTime }
                    .thenBy { it.label.lowercase() },
            )
            else -> visibleApps.sortedBy { it.label.lowercase() }
        }
    }
    val alphabetical = remember(visibleApps) { visibleApps.sortedBy { it.label.lowercase() } }
    val groups = remember(alphabetical) { groupAppsByLetter(alphabetical.map { it.toLibraryApp() }) }
    val groupLetters = remember(groups) { scrubberLetters(groups) }
    val gridHeaderIndex = remember(groups) { buildSectionHeaderIndex(groups, itemsPerRow = 1) }

    // List-view sections: alphabetical sort -> letter groups; other
    // sorts -> one flat, ungrouped list (no headers, no scrubber).
    val listGroups = remember(sorted, state.drawerSort) {
        if (state.drawerSort == 0) {
            groupAppsByLetter(sorted.map { it.toLibraryApp() })
        } else {
            emptyList()
        }
    }
    val appByKey = remember(visibleApps) { visibleApps.associateBy { it.key } }

    val suggestions = remember(sorted, stats, state.drawerSuggestionsOn) {
        if (!state.drawerSuggestionsOn) {
            emptyList()
        } else {
            sorted.filter { (stats[it.key]?.first ?: 0) > 0 }
                .sortedByDescending { stats[it.key]?.first ?: 0 }
                .take(8)
        }
    }
    val history = remember(state.searchHistoryJson) { parseMembers(state.searchHistoryJson) }

    val searching = query.isNotBlank()
    val q = query.trim()

    fun recordSearch(text: String) {
        if (text.isBlank()) return
        val next = (listOf(text) + history.filter { !it.equals(text, ignoreCase = true) }).take(10)
        scope.launch { app.settings.setSearchHistoryJson(encodeMembers(next)) }
    }

    fun launchFromLibrary(entry: AppEntry) {
        if (searching) recordSearch(q)
        onLaunch(entry)
    }

    fun selectView(next: LibraryView) {
        view = next
        onViewChanged?.invoke(next)
        scope.launch {
            gridState.scrollToItem(0)
            listState.scrollToItem(0)
        }
    }

    fun cycleSort() {
        scope.launch {
            app.settings.setDrawerSort((state.drawerSort + 1) % 3)
        }
    }
    val sortLabel = when (state.drawerSort) {
        1 -> "Most used"
        2 -> "Recent"
        else -> "A–Z"
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .background(palette.glass.copy(alpha = LocalTokens.current.glassAlpha))
            .statusBarsPadding()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    closeLibrary()
                    true
                } else {
                    false
                }
            },
    ) {
        val expanded = maxWidth >= 840.dp

        Column(Modifier.fillMaxSize()) {
            // ---- Top: centred search field + close / settings actions.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (expanded) Spacer(Modifier.weight(1f))
                LibrarySearchField(
                    query = query,
                    onQueryChange = { query = it },
                    focusRequester = focusRequester,
                    modifier = if (expanded) Modifier.width(560.dp) else Modifier.weight(1f),
                    onSubmit = {
                        // Enter launches the top ranked app, if any.
                        val top = rankLibraryApps(
                            alphabetical.map { it.toLibraryApp() }, q, stats,
                        ).firstOrNull()?.key?.let { appByKey[it] }
                        if (top != null) launchFromLibrary(top)
                    },
                )
                if (expanded) Spacer(Modifier.weight(1f))
                TextButton(onClick = { onOpenSettings("App drawer") }) {
                    Text("⋮", color = palette.onSurface, fontSize = 20.sp)
                }
                TextButton(onClick = { closeLibrary() }) {
                    Text("Close", color = palette.onSurface)
                }
            }

            // ---- Segmented control + sort chip (hidden while searching).
            if (!searching) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LibrarySegmentedControl(
                        options = buildList {
                            add("All" to LibraryView.ALL)
                            if (categoriesContent != null) {
                                add("Categories" to LibraryView.CATEGORIES)
                            }
                            add("List" to LibraryView.LIST)
                        },
                        selected = effectiveView,
                        onSelect = { selectView(it) },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.glass.copy(alpha = 0.6f))
                            .clickable { cycleSort() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text("Sort: $sortLabel", color = palette.onSurface, fontSize = 13.sp)
                    }
                }

                // ---- Suggested row (local launch history; hidden if empty).
                if (suggestions.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (entry in suggestions) {
                            LibraryGridCell(
                                app = app,
                                entry = entry,
                                isNew = isNewApp(entry.toLibraryApp(), stats),
                                onLaunch = { launchFromLibrary(entry) },
                                onMenu = { menuEntry = entry },
                                onDragOut = onDragOut,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ---- Content.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    searching -> {
                        SearchResultsList(
                            app = app,
                            state = state,
                            apps = visibleApps,
                            stats = stats,
                            query = q,
                            onLaunch = { launchFromLibrary(it) },
                            onOpenSettings = onOpenSettings,
                            onRecordSearch = { recordSearch(it) },
                        )
                    }
                    effectiveView == LibraryView.CATEGORIES -> {
                        categoriesContent?.invoke()
                    }
                    effectiveView == LibraryView.LIST -> {
                        if (listGroups.isNotEmpty()) {
                            LibraryListView(
                                app = app,
                                groups = listGroups,
                                appByKey = appByKey,
                                expanded = expanded,
                                listState = listState,
                                stats = stats,
                                onLaunch = { launchFromLibrary(it) },
                                onMenu = { menuEntry = it },
                                onDragOut = onDragOut,
                            )
                            AlphabetScrubber(
                                availableLetters = listGroups.map { it.first },
                                hapticsOn = state.pressFeedback && state.hapticStrength > 0,
                                onLetterSelected = { letter ->
                                    val perRow = if (expanded) 2 else 1
                                    val index = buildSectionHeaderIndex(listGroups, perRow)[letter]
                                    if (index != null) {
                                        scope.launch { listState.scrollToItem(index) }
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        } else {
                            // Flat list (most-used / recent sort): no
                            // letter sections, so no scrubber either.
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                                    .padding(horizontal = 4.dp),
                            ) {
                                items(sorted, key = { it.key }) { entry ->
                                    LibraryListRow(
                                        app = app,
                                        entry = entry,
                                        isNew = isNewApp(entry.toLibraryApp(), stats),
                                        onLaunch = { launchFromLibrary(entry) },
                                        onMenu = { menuEntry = entry },
                                        onDragOut = onDragOut,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // All view: grid with small floating letter labels.
                        LazyVerticalGrid(
                            state = gridState,
                            columns = if (expanded) {
                                GridCells.Fixed(7)
                            } else {
                                GridCells.Adaptive(76.dp)
                            },
                            modifier = Modifier.fillMaxSize()
                                .padding(start = 12.dp, end = 48.dp),
                        ) {
                            for ((letter, members) in groups) {
                                item(
                                    key = "header:$letter",
                                    span = { GridItemSpan(maxCurrentLineSpan) },
                                ) {
                                    FloatingLetterLabel(letter.toString())
                                }
                                gridItems(members, key = { it.key }) { libApp ->
                                    val entry = appByKey[libApp.key] ?: return@gridItems
                                    LibraryGridCell(
                                        app = app,
                                        entry = entry,
                                        isNew = isNewApp(libApp, stats),
                                        onLaunch = { launchFromLibrary(entry) },
                                        onMenu = { menuEntry = entry },
                                        onDragOut = onDragOut,
                                    )
                                }
                            }
                        }
                        AlphabetScrubber(
                            availableLetters = groupLetters,
                            hapticsOn = state.pressFeedback && state.hapticStrength > 0,
                            onLetterSelected = { letter ->
                                val index = gridHeaderIndex[letter]
                                if (index != null) {
                                    scope.launch { gridState.scrollToItem(index) }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }

            // ---- Bottom: Hidden chip only when hidden apps exist.
            // (The old stuck hint is intentionally gone.)
            if (hidden.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.glass.copy(alpha = 0.6f))
                            .clickable { onOpenSettings("Privacy & Lock") }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Hidden (${hidden.size})",
                            color = palette.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }

    // ---- Icon menu (same rows as the home icon menu).
    val entry = menuEntry
    if (entry != null) {
        AlertDialog(
            onDismissRequest = { menuEntry = null },
            title = { Text(entry.label) },
            text = {
                Column {
                    MenuRow("Launch") { menuEntry = null; launchFromLibrary(entry) }
                    MenuRow("Add to home") { menuEntry = null; onAddToHome(entry) }
                    when {
                        entry.key in state.dock -> MenuRow("Remove from dock") {
                            menuEntry = null
                            scope.launch { app.settings.setDock(state.dock - entry.key) }
                        }
                        state.dock.size < state.dockMax -> MenuRow("Add to dock") {
                            menuEntry = null
                            scope.launch { app.settings.setDock(state.dock + entry.key) }
                        }
                        else -> MenuRow("Dock is full") { menuEntry = null }
                    }
                    MenuRow("Edit label") {
                        menuEntry = null; labelDialogEntry = entry
                    }
                    MenuRow("Edit icon") {
                        menuEntry = null; iconDialogEntry = entry
                    }
                    MenuRow("Hide app") {
                        menuEntry = null
                        scope.launch {
                            app.settings.setHiddenJson(
                                encodeMembers((hidden + entry.key).toList()),
                            )
                        }
                    }
                    MenuRow("App info") {
                        menuEntry = null
                        try {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + entry.pkg),
                                ),
                            )
                        } catch (t: Throwable) {
                            // system activity unavailable
                        }
                    }
                    MenuRow("Uninstall") {
                        menuEntry = null
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
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuEntry = null }) { Text("Cancel") } },
        )
    }

    val labelEntry = labelDialogEntry
    if (labelEntry != null) {
        val overrides = remember(state.labelOverridesJson) {
            parseLabelOverrides(state.labelOverridesJson)
        }
        TextFieldDialog(
            title = "Edit label",
            label = "Label",
            initial = overrides[labelEntry.key] ?: labelEntry.label,
            confirmLabel = "Save",
            onDismiss = { labelDialogEntry = null },
            onConfirm = { raw ->
                val next = overrides.toMutableMap()
                val trimmed = raw.trim()
                if (trimmed.isBlank()) next.remove(labelEntry.key)
                else next[labelEntry.key] = trimmed
                labelDialogEntry = null
                scope.launch {
                    app.settings.setLabelOverridesJson(encodeLabelOverrides(next))
                    app.appRepo.refresh()
                }
            },
        )
    }

    val iconEntry = iconDialogEntry
    if (iconEntry != null) {
        AppIconPickerDialog(app, iconEntry, state) { iconDialogEntry = null }
    }
}

/**
 * Standalone search surface for swipe-down on empty home and the dock
 * Search button. The coordinator hosts it; the Library's own field
 * renders the same grouped results inline through [SearchResultsList].
 *
 * Expanded (≥ 840dp): floating glass card, ~640dp wide, centred near
 * the top, height-capped so the keyboard never covers the results.
 * Compact: full-width sheet that resizes with keyboard insets
 * ([imePadding]). One scroll container, grouped sections
 * (Apps / Calculator / Settings / Actions / Contacts / Events / Web),
 * no overlapping rows, no duplicate lists.
 *
 * Zero state: Suggested apps (local launch history) + recent searches
 * (local, clearable). Typing "bigger icons" shows the working icon-size
 * slider in place (the M6 SettingsSearch inline-control pattern:
 * [InlineSlider] wired straight to the settings setter). Ranking:
 * prefix > word > contains > fuzzy, boosted by local launch count +
 * recency ([rankLibraryApps]). Enter launches the top result.
 * Closing clears focus and hides the keyboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchCard(
    app: HavenApp,
    state: SettingsState,
    initialQuery: String = "",
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenSettings: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val allApps by app.appRepo.apps.collectAsState()
    val stats by app.appRepo.stats.collectAsState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun closeCard() {
        focusManager.clearFocus()
        keyboard?.hide()
        onClose()
    }

    val hidden = remember(state.hiddenJson) { state.hiddenApps() }
    val visibleApps = remember(allApps, hidden) { allApps.filter { it.key !in hidden } }
    val appByKey = remember(visibleApps) { visibleApps.associateBy { it.key } }
    val history = remember(state.searchHistoryJson) { parseMembers(state.searchHistoryJson) }
    val q = query.trim()
    val searching = q.isNotBlank()

    fun recordSearch(text: String) {
        if (text.isBlank()) return
        val next = (listOf(text) + history.filter { !it.equals(text, ignoreCase = true) }).take(10)
        scope.launch { app.settings.setSearchHistoryJson(encodeMembers(next)) }
    }

    val ranked = remember(visibleApps, q, stats) {
        if (!searching) emptyList() else rankLibraryApps(visibleApps.map { it.toLibraryApp() }, q, stats)
    }
    val calcResult = remember(q, state.searchCalc) {
        if (searching && state.searchCalc) evaluate(q) else null
    }
    val settingsHits = remember(q, state.searchSettings) {
        if (searching && state.searchSettings) searchSettingsEntries(q, librarySettingsEntries())
        else emptyList()
    }

    fun submit() {
        val top = ranked.firstOrNull()?.let { appByKey[it.key] }
        when {
            top != null -> {
                recordSearch(q)
                onLaunch(top)
            }
            settingsHits.isNotEmpty() -> {
                recordSearch(q)
                onOpenSettings(settingsEntrySection(settingsHits.first().route))
            }
            calcResult != null -> {
                clipboard.setText(AnnotatedString(formatCalcResult(calcResult)))
                recordSearch(q)
            }
            searching -> {
                recordSearch(q)
                openWebSearch(context, q)
            }
        }
    }

    val suggestions = remember(visibleApps, stats) {
        visibleApps.filter { (stats[it.key]?.first ?: 0) > 0 }
            .sortedByDescending { stats[it.key]?.first ?: 0 }
            .take(8)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .imePadding()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    closeCard()
                    true
                } else {
                    false
                }
            },
    ) {
        // Captured before the inner Box: BoxScope shadows the
        // BoxWithConstraints scope, so maxHeight is not reachable
        // unqualified inside the GlassSurface modifier below.
        val boxMaxHeight = maxHeight
        val expanded = maxWidth >= 840.dp
        Box(
            Modifier.fillMaxSize().padding(
                start = if (expanded) 0.dp else 8.dp,
                top = if (expanded) 64.dp else 8.dp,
                end = if (expanded) 0.dp else 8.dp,
                bottom = if (expanded) 0.dp else 8.dp,
            ),
            contentAlignment = if (expanded) Alignment.TopCenter else Alignment.TopStart,
        ) {
            GlassSurface(
                shape = RoundedCornerShape(28.dp),
                modifier = if (expanded) {
                    Modifier.width(640.dp).heightIn(max = boxMaxHeight * 0.78f)
                } else {
                    Modifier.fillMaxWidth().fillMaxHeight()
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LibrarySearchField(
                            query = query,
                            onQueryChange = { query = it },
                            focusRequester = focusRequester,
                            onSubmit = { submit() },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { closeCard() }) {
                            Text("Close", color = palette.onSurface)
                        }
                    }
                    if (!searching) {
                        // Zero state: Suggested + recent searches.
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            if (suggestions.isNotEmpty()) {
                                item { SectionHeader("Suggested") }
                                items(suggestions, key = { it.key }) { entry ->
                                    SearchZeroRow(
                                        app = app,
                                        entry = entry,
                                        onClick = { onLaunch(entry) },
                                    )
                                }
                            }
                            if (history.isNotEmpty()) {
                                item {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Recent searches",
                                            color = palette.accent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.weight(1f))
                                        TextButton(onClick = {
                                            scope.launch {
                                                app.settings.setSearchHistoryJson("[]")
                                            }
                                        }) {
                                            Text("Clear", color = palette.muted, fontSize = 13.sp)
                                        }
                                    }
                                }
                                item {
                                    FlowRow(
                                        Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        for (h in history) {
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(palette.glass.copy(alpha = 0.6f))
                                                    .clickable { query = h }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                            ) {
                                                Text(h, color = palette.onSurface, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            if (suggestions.isEmpty() && history.isEmpty()) {
                                item {
                                    Text(
                                        "Start typing to search apps, settings, contacts and more",
                                        color = palette.muted,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            SearchResultsList(
                                app = app,
                                state = state,
                                apps = visibleApps,
                                stats = stats,
                                query = q,
                                onLaunch = { entry ->
                                    recordSearch(q)
                                    onLaunch(entry)
                                },
                                onOpenSettings = onOpenSettings,
                                onRecordSearch = { recordSearch(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The one grouped results list, shared by [LibraryOverlay] (inline under
 * its field) and [SearchCard] (inside its card). Sections: Apps,
 * Calculator, Settings (with live inline controls), Actions, Contacts,
 * Events, Web. Exactly one LazyColumn; rows never overlap because every
 * section is an item in the same list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchResultsList(
    app: HavenApp,
    state: SettingsState,
    apps: List<AppEntry>,
    stats: Map<String, Pair<Int, Long>>,
    query: String,
    onLaunch: (AppEntry) -> Unit,
    onOpenSettings: (String) -> Unit,
    onRecordSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val appByKey = remember(apps) { apps.associateBy { it.key } }

    val ranked = remember(apps, query, stats) {
        rankLibraryApps(apps.map { it.toLibraryApp() }, query, stats)
    }
    val calcResult = remember(query, state.searchCalc) {
        if (state.searchCalc) evaluate(query) else null
    }
    val settingsHits = remember(query, state.searchSettings) {
        if (state.searchSettings) searchSettingsEntries(query, librarySettingsEntries())
        else emptyList()
    }
    val actionHits = remember(query) { libraryActions(query) }

    val contactsGranted = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val calendarGranted = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    }
    var contacts by remember { mutableStateOf<List<ContactHit>>(emptyList()) }
    LaunchedEffect(query, state.searchContacts, contactsGranted) {
        contacts = if (state.searchContacts && contactsGranted) {
            withContext(Dispatchers.IO) { queryContacts(context, query) }
        } else {
            emptyList()
        }
    }
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    LaunchedEffect(query, state.searchCalendar, calendarGranted, state.calendarsJson) {
        events = if (state.searchCalendar && calendarGranted) {
            val allowed = parseAllowedCalendars(state.calendarsJson)
            withContext(Dispatchers.IO) {
                CalendarRepo.upcoming(context, 20, allowed, null)
                    .filter { it.title.contains(query, ignoreCase = true) }
                    .take(5)
            }
        } else {
            emptyList()
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (ranked.isNotEmpty()) {
            item { SectionHeader("Apps") }
            items(ranked, key = { "app:${it.key}" }) { libApp ->
                val entry = appByKey[libApp.key] ?: return@items
                LibraryListRow(
                    app = app,
                    entry = entry,
                    isNew = isNewApp(libApp, stats),
                    onLaunch = { onLaunch(entry) },
                    onMenu = {},
                    onDragOut = null,
                    showMenuButton = false,
                )
            }
        }
        if (calcResult != null) {
            item { SectionHeader("Calculator") }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(formatCalcResult(calcResult)))
                            onRecordSearch(query)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "$query = ${formatCalcResult(calcResult)}",
                        color = palette.onSurface,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Tap to copy", color = palette.muted, fontSize = 11.sp)
                }
            }
        }
        if (settingsHits.isNotEmpty()) {
            item { SectionHeader("Settings") }
            items(settingsHits, key = { "setting:${it.route}" }) { hit ->
                LibrarySettingRow(
                    app = app,
                    state = state,
                    entry = hit,
                    onOpenSettings = { onOpenSettings(settingsEntrySection(hit.route)) },
                )
            }
        }
        if (actionHits.isNotEmpty()) {
            item { SectionHeader("Actions") }
            items(actionHits, key = { "action:${it.id}" }) { action ->
                SearchLinkRow(action.title) {
                    when (action.id) {
                        "open-settings" -> onOpenSettings("")
                        "clear-history" -> scope.launch {
                            app.settings.setSearchHistoryJson("[]")
                        }
                    }
                }
            }
        }
        if (contacts.isNotEmpty()) {
            item { SectionHeader("Contacts") }
            items(contacts, key = { "contact:${it.uri}" }) { hit ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, hit.uri)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (t: Throwable) {
                                // contacts app unavailable
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(hit.name, color = palette.onSurface, fontSize = 15.sp, maxLines = 1)
                        Text(hit.number, color = palette.muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        if (events.isNotEmpty()) {
            item { SectionHeader("Events") }
            items(events, key = { "event:${it.beginMs}:${it.title}" }) { ev ->
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(ev.title, color = palette.onSurface, fontSize = 15.sp, maxLines = 1)
                        Text(
                            SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
                                .format(Date(ev.beginMs)),
                            color = palette.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        // Web fallback: the user's own engine, via the system search
        // intent. This app makes no web request itself.
        item { SectionHeader("Web") }
        item {
            SearchLinkRow("Search the web for “$query”") {
                onRecordSearch(query)
                openWebSearch(context, query)
            }
        }
        if (ranked.isEmpty() && calcResult == null && contacts.isEmpty() &&
            events.isEmpty() && settingsHits.isEmpty() && actionHits.isEmpty()
        ) {
            item {
                Text(
                    "No app, setting or contact matches — try the web result below",
                    color = palette.muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Searchable settings for the Search card, ranked by the M6 shell ranker. */
private fun librarySettingsEntries(): List<SearchEntry> = listOf(
    SearchEntry(
        "Icon size", listOf("icon", "size", "bigger", "scale", "large", "small"),
        "Icons", "icon-size",
    ),
    SearchEntry(
        "Dark mode", listOf("dark", "night", "theme", "mode"),
        "Style & Theme", "dark-mode",
    ),
    SearchEntry(
        "Grid density", listOf("grid", "density", "columns", "rows", "layout"),
        "Home screen", "density",
    ),
    SearchEntry(
        "Hide apps", listOf("hide", "hidden", "private"),
        "Privacy & Lock", "Privacy & Lock",
    ),
    SearchEntry(
        "Wallpaper dim", listOf("wallpaper", "dim", "blur", "background"),
        "Wallpaper", "Wallpaper",
    ),
    SearchEntry(
        "Dock apps", listOf("dock", "pinned"),
        "Dock", "Dock",
    ),
    SearchEntry(
        "App drawer", listOf("drawer", "library", "apps list"),
        "App drawer", "App drawer",
    ),
    SearchEntry(
        "Gestures", listOf("gesture", "swipe"),
        "Gestures", "Gestures",
    ),
    SearchEntry(
        "Backup", listOf("backup", "export", "restore"),
        "Backup & Profiles", "Backup & Profiles",
    ),
    SearchEntry(
        "Search sources", listOf("search", "contacts", "calendar", "calculator"),
        "Search & Smart", "Search & Smart",
    ),
    SearchEntry(
        "Widgets", listOf("widget"),
        "Widgets", "Widgets",
    ),
    SearchEntry(
        "Folders", listOf("folder"),
        "Folders", "Folders",
    ),
    SearchEntry(
        "Animations", listOf("animation", "motion", "speed"),
        "Animations", "Animations",
    ),
)

/**
 * The Settings section name a result row navigates to. Inline-control
 * routes map to their section; plain section names pass through, so
 * hosts keep receiving the same contract [DrawerOverlay] always used
 * (a SETTINGS_SECTIONS name).
 */
private fun settingsEntrySection(route: String): String = when (route) {
    "icon-size" -> "Icons"
    "dark-mode" -> "Style & Theme"
    "density" -> "Home screen"
    else -> route
}

private data class LibraryAction(val title: String, val synonyms: List<String>, val id: String)

private fun libraryActions(query: String): List<LibraryAction> {
    val q = query.lowercase().trim()
    if (q.isEmpty()) return emptyList()
    val all = listOf(
        LibraryAction("Open Settings", listOf("settings", "configure", "options"), "open-settings"),
        LibraryAction("Clear recent searches", listOf("clear recent", "clear history"), "clear-history"),
    )
    return all.filter { action ->
        action.synonyms.any { syn -> q.contains(syn) || syn.contains(q) }
    }
}

/**
 * One settings result row. The title navigates to its section; a live
 * inline control below it (M6 SettingsSearch pattern) works in place:
 * the icon-size slider, dark-mode switch and density slider write
 * straight through the settings setters, and any control a hub
 * registered in [SettingsSearchControls] renders for its route.
 */
@Composable
private fun LibrarySettingRow(
    app: HavenApp,
    state: SettingsState,
    entry: SearchEntry,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column(
            Modifier.fillMaxWidth().clickable { onOpenSettings() },
        ) {
            Text(entry.title, color = palette.onSurface, fontSize = 15.sp, maxLines = 1)
            Text(entry.hub, color = palette.muted, fontSize = 12.sp, maxLines = 1)
        }
        when (entry.route) {
            "icon-size" -> {
                val effective = if (state.iconSizeOverride > 0) {
                    state.iconSizeOverride
                } else {
                    state.iconDp()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        InlineSlider(
                            value = effective.toFloat(),
                            range = 32f..72f,
                            valueText = { "${it.toInt()}dp" },
                            onCommit = { v ->
                                scope.launch { app.settings.setIconSizeOverride(v.toInt()) }
                            },
                        )
                    }
                    TextButton(onClick = {
                        scope.launch { app.settings.setIconSizeOverride(-1) }
                    }) {
                        Text("Auto", color = palette.muted, fontSize = 13.sp)
                    }
                }
            }
            "dark-mode" -> {
                InlineSwitch(
                    checked = state.darkMode == 2,
                    onCheckedChange = { checked ->
                        scope.launch { app.settings.setDarkMode(if (checked) 2 else 0) }
                    },
                    label = "Dark",
                )
            }
            "density" -> {
                InlineSlider(
                    value = state.density.toFloat(),
                    range = 0f..100f,
                    valueText = { "${it.toInt()}%" },
                    onCommit = { v ->
                        scope.launch { app.settings.setDensity(v.toInt()) }
                    },
                )
            }
            else -> {
                SettingsSearchControls.controlFor(entry.route)?.invoke()
            }
        }
    }
}

/** Centred 56dp search pill. Width comes from the caller (560dp on
 * Expanded, weight/fill elsewhere); the field text is 17sp per spec. */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    GlassSurface(
        shape = RoundedCornerShape(50),
        modifier = modifier.height(56.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = palette.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search apps", color = palette.muted, fontSize = 17.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = palette.onSurface, fontSize = 17.sp),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("✕", color = palette.onSurface)
                }
            }
        }
    }
}

/** All · Categories · List segmented control. The Categories option is
 * only passed in when its content exists, so the tab hides otherwise. */
@Composable
private fun LibrarySegmentedControl(
    options: List<Pair<String, LibraryView>>,
    selected: LibraryView,
    onSelect: (LibraryView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(palette.glass.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((label, view) in options) {
            val isSelected = view == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) palette.accent.copy(alpha = 0.25f) else Color.Transparent,
                    )
                    .clickable { onSelect(view) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    label,
                    color = if (isSelected) palette.accent else palette.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * List view: sticky letter headers + 56dp rows at 16sp. Two columns on
 * Expanded (section apps chunked into pairs), one column on Compact.
 * The sticky header is a small floating label on a translucent glass
 * wash — never a dark bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryListView(
    app: HavenApp,
    groups: List<Pair<Char, List<LibraryApp>>>,
    appByKey: Map<String, AppEntry>,
    expanded: Boolean,
    listState: LazyListState,
    stats: Map<String, Pair<Int, Long>>,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onDragOut: ((LibraryDragPayload) -> Unit)?,
) {
    val palette = LocalPalette.current
    val perRow = if (expanded) 2 else 1
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(start = 4.dp, end = 48.dp),
    ) {
        for ((letter, members) in groups) {
            stickyHeader(key = "header:$letter") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(palette.glass.copy(alpha = LocalTokens.current.glassAlpha))
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        letter.toString(),
                        color = palette.muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
            val pairs = members.chunked(perRow)
            items(pairs, key = { pair -> "row:$letter:${pair.first().key}" }) { pair ->
                Row(Modifier.fillMaxWidth()) {
                    for (libApp in pair) {
                        val entry = appByKey[libApp.key]
                        if (entry != null) {
                            Box(Modifier.weight(1f)) {
                                LibraryListRow(
                                    app = app,
                                    entry = entry,
                                    isNew = isNewApp(libApp, stats),
                                    onLaunch = { onLaunch(entry) },
                                    onMenu = { onMenu(entry) },
                                    onDragOut = onDragOut,
                                )
                            }
                        }
                    }
                    if (pair.size < perRow) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 56dp, 16sp list row. Icon long-press drags; ⋮ opens the menu. */
@Composable
private fun LibraryListRow(
    app: HavenApp,
    entry: AppEntry,
    isNew: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onDragOut: ((LibraryDragPayload) -> Unit)?,
    showMenuButton: Boolean = true,
) {
    val palette = LocalPalette.current
    val bmp = rememberAppIcon(app.appRepo, entry)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .libraryDragSource(entry.toLibraryDragPayload(), onDragOut)
            .clickable { onLaunch(entry) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp)) {
            if (bmp != null) {
                Image(bmp, contentDescription = entry.label, modifier = Modifier.size(40.dp))
            } else {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(entry.label.firstOrNull()?.toString() ?: "?", color = palette.onSurface, fontSize = 18.sp)
                }
            }
            BadgeOverlay(entry.pkg)
            if (isNew) {
                Box(
                    Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    NewDot()
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            entry.label,
            color = palette.onSurface,
            fontFamily = LocalFont.current,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showMenuButton) {
            Box(
                Modifier.size(48.dp).clickable { onMenu(entry) },
                contentAlignment = Alignment.Center,
            ) {
                Text("⋮", color = palette.onSurface, fontSize = 20.sp)
            }
        }
    }
}

/**
 * All-grid cell (also the Suggested-row cell). Icon long-press drags
 * the app out; label long-press opens the menu; tap launches. Splitting
 * the gestures across icon and label keeps drag and menu from fighting
 * over one long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGridCell(
    app: HavenApp,
    entry: AppEntry,
    isNew: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onDragOut: ((LibraryDragPayload) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val bmp = rememberAppIcon(app.appRepo, entry)
    Column(
        modifier.padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .libraryDragSource(entry.toLibraryDragPayload(), onDragOut)
                .clickable { onLaunch(entry) },
        ) {
            if (bmp != null) {
                Image(bmp, contentDescription = entry.label, modifier = Modifier.size(48.dp))
            } else {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text(entry.label.firstOrNull()?.toString() ?: "?", color = palette.onSurface, fontSize = 20.sp)
                }
            }
            BadgeOverlay(entry.pkg)
            if (isNew) {
                Box(
                    Modifier.fillMaxSize().padding(1.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    NewDot()
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.combinedClickable(
                onClick = { onLaunch(entry) },
                onLongClick = { onMenu(entry) },
            ),
        ) {
            Text(
                entry.label,
                color = palette.onSurface,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Subtle accent dot marking a recently installed, never-opened app. */
@Composable
private fun NewDot() {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(palette.accent),
    )
}

/**
 * Scrubber: ≥ 40dp strip showing A–Z + '#'. Letters with no section
 * render dimmed; a tap/drag on one scrolls to the nearest real section
 * ([nearestAvailableLetter]). The ~72dp bubble sits beside the strip
 * near the finger and fades 650ms after the last letter change; a
 * haptic tick fires per letter change. TalkBack sees an adjustable
 * control via [ProgressBarRangeInfo] semantics (current letter index
 * over 0..26) plus a content description naming the current letter.
 */
@Composable
private fun AlphabetScrubber(
    availableLetters: List<Char>,
    hapticsOn: Boolean,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fullLetters = remember { ('A'..'Z').toList() + '#' }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var fingerFraction by remember { mutableStateOf(0f) }
    var lastTicked by remember { mutableStateOf<Char?>(null) }
    var stripHeightPx by remember { mutableStateOf(0) }

    LaunchedEffect(activeLetter) {
        if (activeLetter != null) {
            delay(650)
            activeLetter = null
        }
    }

    fun handlePosition(y: Float, height: Float) {
        val letter = scrubberLetterForPosition(fullLetters, y, height) ?: return
        val resolved = nearestAvailableLetter(letter, availableLetters) ?: return
        fingerFraction = (y / height).coerceIn(0f, 1f)
        activeLetter = resolved
        if (resolved != lastTicked) {
            lastTicked = resolved
            if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterSelected(resolved)
        }
    }

    // M10: 48dp strip (was 40dp) so the scrubber itself meets
    // the >=48dp touch-target rule; per-letter taps ride the drag.
    Box(modifier.fillMaxHeight().width(48.dp)) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(48.dp)
                .onSizeChanged { stripHeightPx = it.height }
                .pointerInput(availableLetters) {
                    detectTapGestures(onTap = { offset ->
                        handlePosition(offset.y, size.height.toFloat())
                    })
                }
                .pointerInput(availableLetters) {
                    detectVerticalDragGestures { change, _ ->
                        handlePosition(change.position.y, size.height.toFloat())
                    }
                }
                .semantics {
                    contentDescription =
                        "Alphabet scrubber, current letter ${activeLetter ?: ""}"
                    val index = activeLetter?.let { fullLetters.indexOf(it) } ?: 0
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = index.toFloat(),
                        range = 0f..(fullLetters.size - 1).toFloat(),
                    )
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (letter in fullLetters) {
                val available = letter in availableLetters
                Text(
                    letter.toString(),
                    color = if (available) palette.onSurface else palette.muted.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    fontWeight = if (available) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        val bubbleLetter = activeLetter
        if (bubbleLetter != null && stripHeightPx > 0) {
            val bubbleY = with(density) {
                (stripHeightPx * fingerFraction).toDp() - 36.dp
            }.coerceAtLeast(0.dp)
            GlassSurface(
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-48).dp, y = bubbleY)
                    .size(72.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        bubbleLetter.toString(),
                        color = palette.onSurface,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Drag-out source for Library icons (see the drop contract in the file
 * header). Long-press starts a platform drag carrying [LIBRARY_DRAG_MIME]
 * with the tab-separated payload text from [encodeLibraryDragPayload];
 * [onDragOut] fires first so the coordinator can lower the overlay and
 * arm its home/dock drop target while the drag is still running.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.libraryDragSource(
    payload: LibraryDragPayload,
    onDragStarted: ((LibraryDragPayload) -> Unit)? = null,
): Modifier = this.then(
    // Legacy suspend source form: the single-lambda form resolves to
    // the (Offset) -> TransferData overload, which cannot host the
    // long-press detector. Naming the block parameter selects the
    // DragAndDropSourceScope overload (the other candidate names its
    // second parameter transferData); its receiver supplies both
    // detectTapGestures (via PointerInputScope) and startTransfer.
    Modifier.dragAndDropSource(
        drawDragDecoration = {},
        block = {
            detectTapGestures(
                onLongPress = {
                    onDragStarted?.invoke(payload)
                    startTransfer(
                        DragAndDropTransferData(
                            clipData = ClipData(
                                ClipDescription(payload.label, arrayOf(LIBRARY_DRAG_MIME)),
                                ClipData.Item(encodeLibraryDragPayload(payload)),
                            ),
                        ),
                    )
                },
            )
        },
    ),
)

/** Small floating letter label for All-grid sections: text only, no bar. */
@Composable
private fun FloatingLetterLabel(text: String) {
    val palette = LocalPalette.current
    Text(
        text,
        color = palette.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
    )
}

/** Grouped-section header for search results: text only, no bar. */
@Composable
private fun SectionHeader(text: String) {
    val palette = LocalPalette.current
    Text(
        text,
        color = palette.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SearchLinkRow(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = palette.onSurface, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = palette.onSurface, fontSize = 15.sp)
    }
}

/** Zero-state Suggested row in [SearchCard]: icon + label, tap launches. */
@Composable
private fun SearchZeroRow(app: HavenApp, entry: AppEntry, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val bmp = rememberAppIcon(app.appRepo, entry)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp)) {
            if (bmp != null) {
                Image(bmp, contentDescription = entry.label, modifier = Modifier.size(40.dp))
            } else {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(entry.label.firstOrNull()?.toString() ?: "?", color = palette.onSurface, fontSize = 18.sp)
                }
            }
            BadgeOverlay(entry.pkg)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            entry.label,
            color = palette.onSurface,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
