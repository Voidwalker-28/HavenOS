package app.havenos.launcher.ui.settings.shell

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.havenos.launcher.theme.LocalFont

// ---------------------------------------------------------------------------
// M6 search infra (v2 §8). Local fuzzy index only — no network, no
// analytics. Hub workers each expose their entries as a top-level list:
//
//   val LookSearchEntries: List<SearchEntry> = listOf(
//       SearchEntry("Dark mode", listOf("dark", "night", "theme"),
//           hub = "Look", route = "look"),
//       ...
//   )
//
// and the integration step passes every hub list to searchEntries():
//   searchEntries(LookSearchEntries, HomeSearchEntries, ...)
// Hubs may also self-register via SettingsSearchRegistry.register()
// (e.g. from their screen file's init path), but passing the lists
// explicitly is the primary path: a hub that has never been visited
// must still be searchable. Inline controls are registered the same
// way: SettingsSearchControls.register(route) { InlineSwitch(...) } —
// the control reads settings.state and writes via settings setters,
// so it works in place without navigating.
// ---------------------------------------------------------------------------

/**
 * One searchable setting (or action). [hub] is the hub path shown
 * under the title; [route] is the panel route the row body navigates
 * to (or, for actions, an "action:…" id the host handles). [isAction]
 * puts the entry in the Actions group instead of Settings.
 */
data class SearchEntry(
    val title: String,
    val synonyms: List<String>,
    val hub: String,
    val route: String,
    val isAction: Boolean = false,
)

/** Registry for hubs that self-register instead of being passed in. */
object SettingsSearchRegistry {
    private val registered = mutableListOf<List<SearchEntry>>()

    fun register(entries: List<SearchEntry>) {
        registered.add(entries)
    }

    fun entries(): List<SearchEntry> = registered.flatten()
}

/**
 * Aggregation point. Pass every hub's `XxxSearchEntries` list here;
 * registered lists are merged in, and duplicates by route are dropped.
 * Called with no arguments it returns only registered entries.
 */
fun searchEntries(vararg hubLists: List<SearchEntry>): List<SearchEntry> =
    (hubLists.flatMap { it } + SettingsSearchRegistry.entries())
        .distinctBy { it.route }

/** Inline control rendered inside a result row. Registered per route. */
typealias SearchControlContent = @Composable () -> Unit

object SettingsSearchControls {
    private val controls = mutableMapOf<String, SearchControlContent>()

    fun register(route: String, control: SearchControlContent) {
        controls[route] = control
    }

    fun controlFor(route: String): SearchControlContent? = controls[route]
}

/** v2 §8 synonym words, expanded before matching (single-word form). */
private val QUERY_EXPANSIONS: Map<String, List<String>> = mapOf(
    "bigger" to listOf("icon", "size"),
    "scale" to listOf("icon", "size"),
    "night" to listOf("dark"),
    "theme" to listOf("dark", "mode"),
    "blur" to listOf("material"),
    "glass" to listOf("material"),
    "frosted" to listOf("material"),
    "transparent" to listOf("material"),
    "hide" to listOf("hidden"),
    "private" to listOf("hidden"),
    "secret" to listOf("hidden"),
    "grid" to listOf("grid"),
    "rows" to listOf("grid"),
    "columns" to listOf("grid"),
    "layout" to listOf("grid"),
    "swipe" to listOf("gesture"),
    "gesture" to listOf("gesture"),
    "default" to listOf("default", "launcher"),
    "backup" to listOf("backup"),
    "export" to listOf("backup"),
    "restore" to listOf("backup"),
)

private fun normalize(text: String): String = text.lowercase().trim()

private fun isSubsequence(needle: String, haystack: String): Boolean {
    var i = 0
    for (ch in haystack) {
        if (i < needle.length && ch == needle[i]) i++
    }
    return i == needle.length
}

private fun tokenScore(token: String, entry: SearchEntry): Int {
    val title = normalize(entry.title)
    val hub = normalize(entry.hub)
    val syns = entry.synonyms.map { normalize(it) }
    var score = 0
    if (title.split(" ", "-", "_").any { it.startsWith(token) }) score += 50
    else if (title.contains(token)) score += 30
    else if (isSubsequence(token, title.replace(" ", ""))) score += 10
    if (hub.contains(token)) score += 10
    if (syns.any { it == token || it.contains(token) }) score += 25
    return score
}

/**
 * Ranked fuzzy search over [entries]. Every query token must match
 * somewhere (title, hub path, or synonyms, after synonym expansion);
 * results sort by score, then title. An empty query returns empty —
 * the caller shows suggestions + recent instead.
 */
fun searchSettingsEntries(
    query: String,
    entries: List<SearchEntry>,
): List<SearchEntry> {
    val rawTokens = normalize(query).split(" ", "-", "_").filter { it.isNotEmpty() }
    if (rawTokens.isEmpty()) return emptyList()
    val tokens = rawTokens.flatMap { listOf(it) + (QUERY_EXPANSIONS[it] ?: emptyList()) }
    return entries.mapNotNull { entry ->
        val score = tokens.sumOf { tokenScore(it, entry) }
        // Require every raw token to match on its own, not just via expansion.
        val allMatch = rawTokens.all { tokenScore(it, entry) > 0 }
        if (allMatch && score > 0) entry to score else null
    }.sortedWith(compareByDescending<Pair<SearchEntry, Int>> { it.second }.thenBy { it.first.title })
        .map { it.first }
}

/** Closest hub for the no-result state: most query-token overlap, else null. */
fun closestHub(query: String, entries: List<SearchEntry>): String? {
    val tokens = normalize(query).split(" ", "-", "_").filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    return entries.groupBy { it.hub }
        .mapValues { (_, group) ->
            tokens.count { token ->
                group.any { tokenScore(token, it) > 0 }
            }
        }
        .maxByOrNull { it.value }
        ?.takeIf { it.value > 0 }
        ?.key
}

/** Empty-query suggestions: query strings, tapped to run the search. */
val SEARCH_SUGGESTIONS: List<String> = listOf(
    "Bigger icons",
    "Dark mode",
    "Blur",
    "Hide apps",
    "Grid",
)

/** Recent searches, local only (SharedPreferences, clearable). */
class RecentSearchQueries(context: Context) {
    private val prefs = context.getSharedPreferences("havenos_shell_search", Context.MODE_PRIVATE)

    fun recent(): List<String> =
        prefs.getString(KEY, "").orEmpty().split("\n").filter { it.isNotEmpty() }

    fun record(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val next = (listOf(q) + recent()).distinct().take(8)
        prefs.edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "queries"
    }
}

@Composable
fun rememberRecentSearchQueries(): RecentSearchQueries {
    val context = LocalContext.current
    return remember { RecentSearchQueries(context.applicationContext) }
}

/** Search field (v2 §5 placeholder, verbatim). */
@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(text = "Search settings or try 'bigger icons'")
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Compact inline switch for a result row. */
@Composable
fun InlineSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    val tokens = LocalShellTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tokens.accent,
            ),
        )
        Text(
            text = label,
            style = ShellText.help.copy(fontFamily = LocalFont.current),
            color = tokens.textSecondary,
        )
    }
}

/** Compact inline segmented control for a result row. */
@Composable
fun InlineSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    ShellSegmentedControl(options = options, selected = selected, onSelect = onSelect)
}

/** Compact inline slider for a result row (continuous track, no ticks). */
@Composable
fun InlineSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    val tokens = LocalShellTokens.current
    var drag by remember { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = drag,
            onValueChange = { drag = it },
            onValueChangeFinished = { onCommit(drag) },
            valueRange = range,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = tokens.accent,
                activeTrackColor = tokens.accent,
                inactiveTrackColor = tokens.accent.copy(alpha = 0.24f),
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(ShellSpacing.s8))
        Text(
            text = valueText(drag),
            style = ShellText.help.copy(fontFamily = LocalFont.current),
            color = tokens.textSecondary,
        )
    }
}

/**
 * One result row. The registered inline control (if any) works in
 * place below the title; tapping the row body navigates to the
 * screen and spotlights the setting via [onOpenEntry].
 */
@Composable
fun SearchResultRow(
    entry: SearchEntry,
    onOpenEntry: (SearchEntry) -> Unit,
) {
    val tokens = LocalShellTokens.current
    val control = SettingsSearchControls.controlFor(entry.route)
    ShellCard {
        Column(verticalArrangement = Arrangement.spacedBy(ShellSpacing.s8)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "${entry.title}, in ${entry.hub}"
                        role = Role.Button
                    }
                    .clickable { onOpenEntry(entry) },
            ) {
                Text(
                    text = entry.title,
                    style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.hub,
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (control != null) control()
        }
    }
}

/**
 * Full search content: field + empty state (5 suggestions + recent,
 * clearable) + grouped results (Settings, then Actions) + no-result
 * state suggesting the closest hub. The host owns [query] and records
 * it in [recentSearches] when a search is submitted or a result opens.
 */
@Composable
fun SettingsSearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    entries: List<SearchEntry>,
    recentSearches: RecentSearchQueries,
    onOpenEntry: (SearchEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalShellTokens.current
    var recentVersion by remember { mutableStateOf(0) }
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(ShellSpacing.cardGap),
    ) {
        SettingsSearchField(query = query, onQueryChange = onQueryChange)
        if (query.isBlank()) {
            ShellSectionLabel(text = "Suggestions")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
            ) {
                for (suggestion in SEARCH_SUGGESTIONS) {
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(ShellRadii.chip))
                            .background(tokens.cardSelected)
                            .clickable { onQueryChange(suggestion) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = suggestion,
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.accent,
                            maxLines = 1,
                        )
                    }
                }
            }
            // Re-read on clear: recentVersion is a composition key below.
            val recent by remember(recentVersion) { mutableStateOf(recentSearches.recent()) }
            if (recent.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShellSectionLabel(text = "Recent")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        recentSearches.clear()
                        recentVersion++
                    }) {
                        Text(
                            text = "Clear",
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.textSecondary,
                        )
                    }
                }
                for (item in recent) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onQueryChange(item) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = item,
                            style = ShellText.value.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            val results = searchSettingsEntries(query, entries)
            if (results.isEmpty()) {
                val hub = closestHub(query, entries)
                Text(
                    text = "No results",
                    style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                )
                if (hub != null) {
                    Text(
                        text = "Closest match: $hub",
                        style = ShellText.value.copy(fontFamily = LocalFont.current),
                        color = tokens.textSecondary,
                    )
                    val first = entries.firstOrNull { it.hub == hub }
                    if (first != null) {
                        TextButton(onClick = { onOpenEntry(first) }) {
                            Text(
                                text = "Open $hub",
                                style = ShellText.help.copy(fontFamily = LocalFont.current),
                                color = tokens.accent,
                            )
                        }
                    }
                }
            } else {
                val settingsResults = results.filter { !it.isAction }
                val actionResults = results.filter { it.isAction }
                if (settingsResults.isNotEmpty()) {
                    ShellSectionLabel(text = "Settings")
                    for (entry in settingsResults) {
                        SearchResultRow(entry = entry, onOpenEntry = onOpenEntry)
                    }
                }
                if (actionResults.isNotEmpty()) {
                    ShellSectionLabel(text = "Actions")
                    for (entry in actionResults) {
                        SearchResultRow(entry = entry, onOpenEntry = onOpenEntry)
                    }
                }
            }
        }
    }
}
