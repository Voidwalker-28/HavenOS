package app.havenos.launcher.ui

import app.havenos.launcher.data.AppEntry
import kotlin.math.abs

/**
 * M7 Wave 1 — Library (drawer) + Search card: pure logic.
 *
 * This file has NO Compose and NO Android-framework dependencies so the
 * ranking, scrubber and grouping rules are unit-testable on the JVM.
 * [LibraryApp] is the framework-free view of [AppEntry]; composables map
 * entries with [AppEntry.toLibraryApp] and map results back by [LibraryApp.key].
 *
 * Setting key referenced BY NAME only (the coordinator owns the store
 * entry; this file reads none of it directly):
 * - `libraryDefaultView` (String, "all" / "categories" / "list",
 *   default "all"). The Library composable takes its initial view as a
 *   parameter and reports changes through `onViewChanged`; the
 *   coordinator persists the reported key with [LibraryView.key].
 */

/** DataStore key name for the Library's remembered view. Coordinator-owned. */
const val LIBRARY_DEFAULT_VIEW_KEY = "libraryDefaultView"

/** The three Library views. No page language: these are views, not pages. */
enum class LibraryView(val key: String) {
    ALL("all"),
    CATEGORIES("categories"),
    LIST("list");

    companion object {
        /** Unknown, blank or null keys fall back to [ALL]. */
        fun fromKey(key: String?): LibraryView =
            values().firstOrNull { it.key == key } ?: ALL
    }
}

/** Framework-free app row for ranking / grouping / scrubber math. */
data class LibraryApp(
    val key: String,
    val label: String,
    val pkg: String,
    val installTime: Long = 0L,
)

fun AppEntry.toLibraryApp(): LibraryApp =
    LibraryApp(key = key, label = label, pkg = pkg, installTime = installTime)

// ---------------------------------------------------------------------------
// Search ranking: prefix > word > contains > fuzzy, boosted by local
// launch count, then by recency. All usage data is local only.
// ---------------------------------------------------------------------------

private fun isSubsequenceOf(needle: String, haystack: String): Boolean {
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

/**
 * Match band for one [label] against a normalized (lowercase, trimmed)
 * [query]. Lower is better. Null means no match.
 * - 0 PREFIX: the label itself starts with the query.
 * - 1 WORD: a word inside the label starts with the query
 *   (e.g. query "map" matches "Google Maps").
 * - 2 CONTAINS: the query appears mid-word, or (multi-word queries)
 *   every query token appears somewhere in the label.
 * - 3 FUZZY: the query is a subsequence of the label with
 *   non-alphanumerics stripped (e.g. "mps" matches "Maps").
 */
fun searchMatchBand(label: String, query: String): Int? {
    val lower = label.lowercase()
    val q = query.lowercase().trim()
    if (q.isEmpty()) return null
    val tokens = q.split(" ", "-", "_").filter { it.isNotEmpty() }
    if (tokens.size > 1) {
        if (tokens.all { lower.contains(it) }) return 2
        val compactLabel = lower.filter { it.isLetterOrDigit() }
        val compactQuery = q.filter { it.isLetterOrDigit() }
        if (isSubsequenceOf(compactQuery, compactLabel)) return 3
        return null
    }
    if (lower.startsWith(q)) return 0
    // No stdlib split-by-predicate overload: scan explicitly, splitting
    // on any non-letter-or-digit and dropping empty runs — the exact
    // word list the old call intended.
    val words = mutableListOf<String>()
    val current = StringBuilder()
    for (ch in lower) {
        if (ch.isLetterOrDigit()) {
            current.append(ch)
        } else if (current.isNotEmpty()) {
            words += current.toString()
            current.clear()
        }
    }
    if (current.isNotEmpty()) words += current.toString()
    if (words.any { it.startsWith(q) }) return 1
    if (lower.contains(q)) return 2
    val compactLabel = lower.filter { it.isLetterOrDigit() }
    if (isSubsequenceOf(q.filter { it.isLetterOrDigit() }, compactLabel)) return 3
    return null
}

/**
 * Rank [apps] for [query]. Order: match band ascending, then local
 * launch count descending, then last-launch time descending, then label
 * ascending (case-insensitive). [stats] maps app key to
 * (launchCount, lastLaunchMs); missing entries count as never launched.
 * [nowMs] is accepted for symmetry with [isNewApp] but recency here is
 * the stored last-launch timestamp, so it is intentionally unused.
 */
fun rankLibraryApps(
    apps: List<LibraryApp>,
    query: String,
    stats: Map<String, Pair<Int, Long>>,
    @Suppress("UNUSED_PARAMETER") nowMs: Long = System.currentTimeMillis(),
): List<LibraryApp> {
    val q = query.lowercase().trim()
    if (q.isEmpty()) return emptyList()
    return apps.mapNotNull { app ->
        val band = searchMatchBand(app.label, q) ?: return@mapNotNull null
        app to band
    }.sortedWith(
        compareBy<Pair<LibraryApp, Int>> { it.second }
            .thenByDescending { stats[it.first.key]?.first ?: 0 }
            .thenByDescending { stats[it.first.key]?.second ?: 0L }
            .thenBy { it.first.label.lowercase() },
    ).map { it.first }
}

// ---------------------------------------------------------------------------
// Letter grouping + scrubber math.
// ---------------------------------------------------------------------------

/** First-letter section for a label: A–Z uppercase, '#' for anything else. */
fun letterOf(label: String): Char {
    val c = label.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * Group [apps] (already in display order) by first letter. Sections are
 * ordered A–Z with '#' last — the scrubber order — not raw char order.
 */
fun groupAppsByLetter(apps: List<LibraryApp>): List<Pair<Char, List<LibraryApp>>> {
    val grouped = apps.groupBy { letterOf(it.label) }
    val letters = ('A'..'Z').filter { it in grouped } + listOf('#').filter { it in grouped }
    return letters.map { it to grouped.getValue(it) }
}

/** Letters present in [groups], in scrubber order. */
fun scrubberLetters(groups: List<Pair<Char, List<LibraryApp>>>): List<Char> =
    groups.map { it.first }

/**
 * The letter at vertical position [y] in a strip of height [height].
 * [letters] is the full strip (A–Z + '#'); positions outside the strip
 * clamp to the first / last letter. Null when the strip is empty or
 * has no height.
 */
fun scrubberLetterForPosition(letters: List<Char>, y: Float, height: Float): Char? {
    if (letters.isEmpty() || height <= 0f) return null
    val fraction = (y / height).coerceIn(0f, 1f)
    val index = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
    return letters[index]
}

/**
 * Nearest letter in [available] to [letter] in scrubber order
 * (A–Z, then '#'). Used when the finger lands on a letter with no
 * section: the strip still shows A–Z + '#', but scrolling goes to the
 * closest real section. Null when nothing is available.
 */
fun nearestAvailableLetter(letter: Char, available: List<Char>): Char? {
    if (available.isEmpty()) return null
    if (letter in available) return letter
    val order = ('A'..'Z').toList() + '#'
    val target = order.indexOf(letter)
    if (target < 0) return available.first()
    return available.minByOrNull { abs(order.indexOf(it) - target) }
}

/**
 * Flat item index of each section header in a LazyColumn/LazyVerticalGrid
 * where a section occupies one header item plus its apps, laid out
 * [itemsPerRow] per row (1 = single-column list, 2 = two-column list
 * on Expanded; a grid also uses 1 because every app is one grid item
 * and a header spans the row as one item).
 */
fun buildSectionHeaderIndex(
    groups: List<Pair<Char, List<LibraryApp>>>,
    itemsPerRow: Int = 1,
): Map<Char, Int> {
    val perRow = itemsPerRow.coerceAtLeast(1)
    val out = LinkedHashMap<Char, Int>()
    var index = 0
    for ((letter, members) in groups) {
        out[letter] = index
        index += 1 + (members.size + perRow - 1) / perRow
    }
    return out
}

// ---------------------------------------------------------------------------
// "New" dot + drag payload.
// ---------------------------------------------------------------------------

/** A never-opened app counts as "New" for this long after install. */
const val NEW_APP_WINDOW_MS: Long = 30L * 24 * 60 * 60 * 1000

/**
 * Recently installed apps carry a subtle "New" dot until opened:
 * never launched (no stats entry, or count 0) AND installed within
 * [windowMs] of [nowMs]. Unknown install times (0) never show the dot.
 */
fun isNewApp(
    app: LibraryApp,
    stats: Map<String, Pair<Int, Long>>,
    nowMs: Long = System.currentTimeMillis(),
    windowMs: Long = NEW_APP_WINDOW_MS,
): Boolean {
    val count = stats[app.key]?.first ?: 0
    if (count > 0) return false
    if (app.installTime <= 0L || app.installTime > nowMs) return false
    return nowMs - app.installTime <= windowMs
}

/**
 * Drag-out payload (see the drop contract in Drawer.kt).
 * [MIME] is the ClipDescription MIME type; [encodeLibraryDragPayload]
 * is the ClipData item text: four tab-separated fields
 * (appKey, pkg, cls, label) with tabs/newlines in the label sanitized
 * to spaces, so decoding is a plain split.
 */
const val LIBRARY_DRAG_MIME = "application/vnd.havenos.app"

data class LibraryDragPayload(
    val appKey: String,
    val pkg: String,
    val cls: String,
    val label: String,
)

fun AppEntry.toLibraryDragPayload(): LibraryDragPayload =
    LibraryDragPayload(key, pkg, cls, label)

fun encodeLibraryDragPayload(payload: LibraryDragPayload): String {
    val safeLabel = payload.label.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    return listOf(payload.appKey, payload.pkg, payload.cls, safeLabel).joinToString("\t")
}

/** Inverse of [encodeLibraryDragPayload]; null on malformed text. */
fun decodeLibraryDragPayload(text: String): LibraryDragPayload? {
    val parts = text.split("\t")
    if (parts.size != 4) return null
    val (appKey, pkg, cls, label) = parts
    if (appKey.isBlank() || pkg.isBlank() || cls.isBlank()) return null
    return LibraryDragPayload(appKey, pkg, cls, label)
}
