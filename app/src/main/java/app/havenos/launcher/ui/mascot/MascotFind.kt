package app.havenos.launcher.ui.mascot

import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.ui.LibraryApp
import app.havenos.launcher.ui.searchMatchBand
import java.text.Normalizer
import kotlin.math.min

/**
 * HavenOS v3 Track A — Find matcher (prompt §5), pure logic.
 *
 * ADAPTER, not a competing index: app-label matching reuses the project's
 * existing [searchMatchBand] from LibrarySearch.kt (the drawer search
 * ranking). This file only adds what that ranking lacks for Find:
 * an exact band, a synonym band, edit-distance fuzzy tolerance, a
 * confidence score, and the act-vs-choices decision. The app list itself
 * comes from AppRepository (LauncherApps via the manifest's <queries>
 * entry) — no second index is built, and no QUERY_ALL_PACKAGES is needed.
 *
 * Rank order (prompt §5.2): exact > prefix > token > synonym > fuzzy,
 * with a mild launch-count boost. Settings entries and actions are
 * indexed from the project's real Settings routes only.
 */

/** Match bands. Lower is better. */
object FindBand {
    const val EXACT = 0
    const val PREFIX = 1
    const val WORD = 2
    const val CONTAINS = 3
    const val SYNONYM = 4
    const val FUZZY = 5
}

/** Normalize: lowercase, strip diacritics/punctuation, collapse spaces. */
fun normalizeFindText(raw: String): String {
    val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
    val stripped = decomposed.filter { ch ->
        Character.getType(ch) != Character.NON_SPACING_MARK.toInt()
    }
    val sb = StringBuilder()
    var lastSpace = true
    for (ch in stripped.lowercase()) {
        if (ch.isLetterOrDigit()) {
            sb.append(ch)
            lastSpace = false
        } else if (!lastSpace) {
            sb.append(' ')
            lastSpace = true
        }
    }
    return sb.toString().trim()
}

/** Bounded Levenshtein distance; returns a value > [maxEdits] when over. */
fun editDistanceBounded(a: String, b: String, maxEdits: Int): Int {
    if (kotlin.math.abs(a.length - b.length) > maxEdits) return maxEdits + 1
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in a.indices) {
        curr[0] = i + 1
        var rowMin = curr[0]
        for (j in b.indices) {
            val cost = if (a[i] == b[j]) 0 else 1
            curr[j + 1] = min(min(prev[j + 1] + 1, curr[j] + 1), prev[j] + cost)
            rowMin = min(rowMin, curr[j + 1])
        }
        if (rowMin > maxEdits) return maxEdits + 1
        val tmp = prev
        prev = curr
        curr = tmp
    }
    return prev[b.length]
}

/**
 * Starter synonym map (prompt §5.1: "a small starter synonym map in a
 * resource file, easy to extend"). Key = word the user typed; value =
 * words that may appear in the app label. Deliberately small and
 * generic — it is a starter list, not a claim of coverage.
 */
val FIND_SYNONYMS: Map<String, List<String>> = mapOf(
    "photo" to listOf("camera"),
    "photos" to listOf("camera", "gallery"),
    "pictures" to listOf("camera", "gallery"),
    "pic" to listOf("camera"),
    "selfie" to listOf("camera"),
    "mail" to listOf("gmail", "email", "outlook"),
    "email" to listOf("gmail", "mail", "outlook"),
    "text" to listOf("messages", "messenger"),
    "sms" to listOf("messages"),
    "chat" to listOf("messages", "messenger"),
    "web" to listOf("browser", "chrome"),
    "internet" to listOf("browser", "chrome"),
    "music" to listOf("spotify", "music"),
    "songs" to listOf("spotify", "music"),
    "audio" to listOf("spotify", "music"),
    "video" to listOf("youtube", "videos"),
    "navigation" to listOf("maps"),
    "gps" to listOf("maps"),
    "time" to listOf("clock"),
    "alarm" to listOf("clock"),
    "events" to listOf("calendar"),
    "schedule" to listOf("calendar"),
    "options" to listOf("settings"),
    "preferences" to listOf("settings"),
    "documents" to listOf("files", "drive"),
    "bank" to listOf("banking"),
    "money" to listOf("banking"),
    "store" to listOf("play"),
    "shop" to listOf("play"),
)

sealed class FindTarget {
    data class App(val key: String, val label: String, val pkg: String) : FindTarget()
    data class Setting(val route: String, val title: String, val controlKey: String) : FindTarget()
    data class Action(val id: String, val title: String) : FindTarget()
    /** 3.1 Phase E: a widget placed on home (frame is pointable). */
    data class Widget(val itemId: String, val label: String) : FindTarget()
}

/** A placed widget, for Find. [page] is the home grid page. */
data class WidgetFindEntry(val itemId: String, val label: String, val page: Int)

/** Widget kind -> picker label (mirrors the widget picker's names). */
fun widgetFindLabel(kind: Int): String = when (kind) {
    ItemKind.CLOCK -> "Clock"
    ItemKind.WEATHER -> "Weather"
    ItemKind.AGENDA -> "Agenda"
    ItemKind.CALENDAR -> "Calendar"
    ItemKind.NOW_PLAYING -> "Now Playing"
    ItemKind.STORAGE -> "Storage"
    ItemKind.COUNTDOWN -> "Countdown"
    ItemKind.TASKS -> "Tasks"
    ItemKind.NOTES -> "Notes"
    ItemKind.GALLERY -> "Gallery"
    ItemKind.QUICK_TILES -> "Quick Tiles"
    ItemKind.SUGGESTED -> "Suggested"
    ItemKind.BATTERY -> "Battery"
    ItemKind.QUICK_SEARCH -> "Quick search"
    else -> "Widget"
}

/** Rank placed widgets for [query] (same bands as apps). */
fun matchWidgets(widgets: List<WidgetFindEntry>, query: String): List<FindCandidate> {
    val queryNorm = normalizeFindText(query)
    if (queryNorm.isBlank()) return emptyList()
    return widgets.mapNotNull { widget ->
        val band = labelBand(widget.label, queryNorm) ?: return@mapNotNull null
        FindCandidate(
            FindTarget.Widget(widget.itemId, widget.label),
            band,
            confidenceFor(band, 0),
        )
    }.sortedBy { it.band }
}

data class FindCandidate(val target: FindTarget, val band: Int, val confidence: Double)

private fun confidenceFor(band: Int, launchCount: Int): Double {
    val base = when (band) {
        FindBand.EXACT -> 0.98
        FindBand.PREFIX -> 0.92
        FindBand.WORD -> 0.85
        FindBand.CONTAINS -> 0.72
        FindBand.SYNONYM -> 0.65
        else -> 0.55
    }
    return min(0.99, base + min(0.05, launchCount * 0.005))
}

private fun labelBand(label: String, queryNorm: String): Int? {
    if (queryNorm.isBlank()) return null
    val labelNorm = normalizeFindText(label)
    if (labelNorm.isEmpty()) return null
    if (labelNorm == queryNorm) return FindBand.EXACT
    // Adapter: the existing drawer-search band, remapped onto Find bands.
    val existing = searchMatchBand(label, queryNorm)
    val mapped = when (existing) {
        0 -> FindBand.PREFIX
        1 -> FindBand.WORD
        2 -> FindBand.CONTAINS
        3 -> FindBand.FUZZY
        else -> null
    }
    if (mapped != null && mapped <= FindBand.CONTAINS) return mapped
    // Synonym: any query token's synonym word appears in the label.
    val labelWords = labelNorm.split(' ').toSet()
    val queryTokens = queryNorm.split(' ').filter { it.isNotEmpty() }
    val synonymHit = queryTokens.any { token ->
        (FIND_SYNONYMS[token] ?: emptyList()).any { syn ->
            labelWords.any { w -> w == syn || w.contains(syn) }
        }
    }
    if (synonymHit) return FindBand.SYNONYM
    if (mapped == FindBand.FUZZY) return FindBand.FUZZY
    // Typo tolerance of 1–2 edits scaled by length (prompt §5.2).
    val maxEdits = if (queryNorm.length >= 6) 2 else 1
    val typoHit = labelWords.any { word ->
        editDistanceBounded(queryNorm, word, maxEdits) <= maxEdits
    } || (queryNorm.length >= 4 &&
        editDistanceBounded(queryNorm, labelNorm.filter { it != ' ' }, maxEdits) <= maxEdits)
    return if (typoHit) FindBand.FUZZY else null
}

/** Rank installed apps for [query] via the AppRepository-backed list. */
fun matchApps(
    apps: List<LibraryApp>,
    query: String,
    stats: Map<String, Pair<Int, Long>>,
): List<FindCandidate> {
    val queryNorm = normalizeFindText(query)
    if (queryNorm.isBlank()) return emptyList()
    return apps.mapNotNull { app ->
        val band = labelBand(app.label, queryNorm) ?: return@mapNotNull null
        FindCandidate(
            FindTarget.App(app.key, app.label, app.pkg),
            band,
            confidenceFor(band, stats[app.key]?.first ?: 0),
        )
    }.sortedWith(
        compareBy<FindCandidate> { it.band }
            .thenByDescending { stats[(it.target as FindTarget.App).key]?.first ?: 0 }
            .thenBy { (it.target as FindTarget.App).label.lowercase() },
    )
}

/** Settings entries indexed for Find: real routes only (prompt §5.1). */
data class SettingIndexEntry(
    val title: String,
    val keywords: List<String>,
    val route: String,
    val controlKey: String,
)

val FIND_SETTING_INDEX: List<SettingIndexEntry> = listOf(
    SettingIndexEntry("Mascot", listOf("mascot", "companion", "milo", "mila"), "mascot", "mascot"),
    SettingIndexEntry("Accent color", listOf("accent", "color", "colour", "theme"), "look", "accent"),
    SettingIndexEntry("Wallpaper", listOf("wallpaper", "background"), "look", "wallpaper"),
    SettingIndexEntry("Look", listOf("look", "style", "theme", "icons"), "look", "look"),
    SettingIndexEntry("Home screen", listOf("home", "grid", "layout"), "home-dock", "home"),
    SettingIndexEntry("Dock", listOf("dock", "favorites"), "home-dock", "dock"),
    SettingIndexEntry("Widgets", listOf("widget", "widgets"), "home-dock", "widgets"),
    SettingIndexEntry("Folders", listOf("folder", "folders"), "home-dock", "folders"),
    SettingIndexEntry("App drawer", listOf("drawer", "library", "apps"), "drawer-search", "drawer"),
    SettingIndexEntry("Search", listOf("search"), "drawer-search", "search"),
    SettingIndexEntry("Gestures", listOf("gesture", "gestures", "swipe"), "gestures-motion", "gestures"),
    SettingIndexEntry("Animations", listOf("animation", "motion", "speed"), "gestures-motion", "animations"),
    SettingIndexEntry("Privacy", listOf("privacy", "lock", "hidden"), "privacy", "privacy"),
    SettingIndexEntry("Backup", listOf("backup", "restore"), "device-data", "backup"),
    SettingIndexEntry("Profiles", listOf("profile", "profiles"), "device-data", "profiles"),
    SettingIndexEntry("APIs", listOf("api", "apis", "online", "crypto", "scores"), "apis", "apis"),
)

fun matchSettings(query: String): List<FindCandidate> {
    val queryNorm = normalizeFindText(query)
    if (queryNorm.isBlank()) return emptyList()
    return FIND_SETTING_INDEX.mapNotNull { entry ->
        val titleNorm = normalizeFindText(entry.title)
        val band = when {
            titleNorm == queryNorm -> FindBand.EXACT
            titleNorm.startsWith(queryNorm) -> FindBand.PREFIX
            entry.keywords.any { it == queryNorm } -> FindBand.WORD
            entry.keywords.any { it.contains(queryNorm) } ||
                titleNorm.contains(queryNorm) -> FindBand.CONTAINS
            else -> null
        } ?: return@mapNotNull null
        FindCandidate(
            FindTarget.Setting(entry.route, entry.title, entry.controlKey),
            band,
            confidenceFor(band, 0),
        )
    }.sortedBy { it.band }
}

/** Actions Find can trigger (prompt §5.1): each maps to a safe host action. */
val FIND_ACTIONS: List<FindTarget.Action> = listOf(
    FindTarget.Action("add-widget", "Add a widget"),
    FindTarget.Action("change-accent", "Change accent color"),
    FindTarget.Action("edit-mode", "Enter edit mode"),
    FindTarget.Action("open-drawer", "Open the app drawer"),
    FindTarget.Action("open-settings", "Open Settings"),
)

private val ACTION_KEYWORDS: Map<String, List<String>> = mapOf(
    "add-widget" to listOf("widget", "add widget"),
    "change-accent" to listOf("accent", "accent color", "change accent"),
    "edit-mode" to listOf("edit", "edit mode", "customize", "customise", "rearrange"),
    "open-drawer" to listOf("drawer", "app drawer", "library", "all apps"),
    "open-settings" to listOf("settings", "open settings"),
)

fun matchActions(query: String): List<FindCandidate> {
    val queryNorm = normalizeFindText(query)
    if (queryNorm.isBlank()) return emptyList()
    return FIND_ACTIONS.mapNotNull { action ->
        val keys = ACTION_KEYWORDS[action.id] ?: emptyList()
        val band = when {
            keys.any { it == queryNorm } -> FindBand.WORD
            keys.any { it.contains(queryNorm) } -> FindBand.CONTAINS
            else -> null
        } ?: return@mapNotNull null
        FindCandidate(FindTarget.Action(action.id, action.title), band, confidenceFor(band, 0))
    }.sortedBy { it.band }
}

/**
 * Combined ranked list: apps first by band, then settings, then actions,
 * each group internally ranked; the cross-group order is band, then
 * group (app/widget < setting < action), then label. Callers cap
 * display at 4. Widgets share the app group: a widget exact match
 * beats a setting prefix match, like an app would.
 */
fun rankFind(
    apps: List<LibraryApp>,
    query: String,
    stats: Map<String, Pair<Int, Long>>,
    widgets: List<WidgetFindEntry> = emptyList(),
): List<FindCandidate> {
    val appCandidates = matchApps(apps, query, stats)
    val widgetCandidates = matchWidgets(widgets, query)
    val settingCandidates = matchSettings(query)
    val actionCandidates = matchActions(query)
    val groupOf: (FindTarget) -> Int = { t ->
        when (t) {
            is FindTarget.App, is FindTarget.Widget -> 0
            is FindTarget.Setting -> 1
            is FindTarget.Action -> 2
        }
    }
    return (appCandidates + widgetCandidates + settingCandidates + actionCandidates).sortedWith(
        compareBy<FindCandidate> { it.band }
            .thenBy { groupOf(it.target) }
            .thenByDescending { it.confidence },
    )
}

sealed class FindDecision {
    data class Act(val candidate: FindCandidate) : FindDecision()
    data class Choices(val candidates: List<FindCandidate>) : FindDecision()
    object NoMatch : FindDecision()
}

/**
 * Prompt §5.2 thresholds: act on a single result, or on a strictly-best
 * first result; otherwise show up to 4 choices; nothing → confused path.
 */
fun decideFind(candidates: List<FindCandidate>): FindDecision {
    if (candidates.isEmpty()) return FindDecision.NoMatch
    if (candidates.size == 1) return FindDecision.Act(candidates[0])
    if (candidates[0].band < candidates[1].band) return FindDecision.Act(candidates[0])
    return FindDecision.Choices(candidates.take(4))
}
