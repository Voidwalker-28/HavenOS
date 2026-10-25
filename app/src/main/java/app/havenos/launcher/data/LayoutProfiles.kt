package app.havenos.launcher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Worker D (M9) — layout profiles, Doc 1 §5.10.
 *
 * STATE-LAYER API (other workers read positions from here, never from
 * HomeItem.page/col/row directly when the window class matters):
 * - [WindowClass] — the three size classes with profiles.
 * - [windowClassFor] — window width/landscape -> profile key.
 * - [LayoutProfiles] — canonical order + one placement map per class.
 * - [deriveProfile] — first use of a profile: reflow from canonical order.
 * - [LayoutProfiles.withAppAdded] / [withAppRemoved] — add/remove updates
 *   EVERY profile (Doc 1 §5.10), so no profile is ever stale.
 * - [syncLayoutsPreview] / [syncLayoutsFrom] — "Sync layouts" overflow
 *   action: re-derive other profiles from the current one, with preview.
 *
 * Coordinates are HALF-CELL units, the DB v2 convention (1 cell = 2
 * units): an app icon is 2x2, a 4x2 widget is 8x4. Grids are passed in
 * CELLS and converted inside, so callers never juggle units.
 *
 * Canonical order is the source of truth for derivation: a list of
 * (item id, canonical page) in display order. It is NOT a placement —
 * it only says "this item comes before that one, on this page". A
 * profile's own placements, once derived, are independent: editing in
 * landscape never touches the portrait map.
 */

/** The three window classes that own a layout profile (Doc 1 §5.10). */
enum class WindowClass {
    COMPACT, // phone portrait
    MEDIUM, // tablet portrait
    EXPANDED_LANDSCAPE, // tablet landscape
}

/**
 * Window size -> profile. Material size-class breakpoints: <600dp wide
 * is Compact, 600–839dp is Medium, >=840dp is Expanded; Expanded only
 * counts as its own profile in landscape (a wide portrait tablet still
 * uses Medium, matching how the home renders it).
 */
fun windowClassFor(widthDp: Int, landscape: Boolean): WindowClass = when {
    widthDp >= 840 && landscape -> WindowClass.EXPANDED_LANDSCAPE
    widthDp >= 600 -> WindowClass.MEDIUM
    else -> WindowClass.COMPACT
}

/** One item's placement inside one profile (half-cell units). */
data class ProfilePlacement(
    val page: Int,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
)

/** One canonical-order entry: item id + the page it canonically belongs to. */
data class CanonicalEntry(val id: String, val page: Int)

/**
 * Canonical order + one placement map per window class. An absent map
 * means "never used yet" — the host must call [deriveProfile] on first
 * use, never invent placements inline.
 */
data class LayoutProfiles(
    val canonical: List<CanonicalEntry> = emptyList(),
    val profiles: Map<WindowClass, Map<String, ProfilePlacement>> = emptyMap(),
) {
    fun placementFor(windowClass: WindowClass, itemId: String): ProfilePlacement? =
        profiles[windowClass]?.get(itemId)

    fun hasProfile(windowClass: WindowClass): Boolean = windowClass in profiles
}

/** Row-major free-spot scan on one page, in half-cell units. Null when full. */
fun firstFreeSpotInProfile(
    placed: Map<String, ProfilePlacement>,
    page: Int,
    colSpan: Int,
    rowSpan: Int,
    colsU: Int,
    rowsU: Int,
): Pair<Int, Int>? {
    if (colSpan <= 0 || rowSpan <= 0 || colSpan > colsU || rowSpan > rowsU) return null
    val occupied = placed.values.filter { it.page == page }
    for (r in 0..rowsU - rowSpan) {
        for (c in 0..colsU - colSpan) {
            val overlaps = occupied.any { o ->
                c < o.col + o.colSpan && o.col < c + colSpan &&
                    r < o.row + o.rowSpan && o.row < r + rowSpan
            }
            if (!overlaps) return c to r
        }
    }
    return null
}

/**
 * Derive a profile from canonical order by reflow.
 *
 * Items are grouped by canonical page, in canonical order, and placed
 * row-major on that page (spans clamped to the page width, never
 * dropped: an item wider than the page gets its own fresh page at
 * 0,0 with its span intact, like the M8 migration's oversize guard).
 * A group that overflows its page spills to the next pages in index
 * order; later groups start after the spill, so page order is stable.
 * [knownSpans] supplies each item's span (from the live layout); an
 * item with no known span defaults to one icon cell (2x2 units).
 */
fun deriveProfile(
    canonical: List<CanonicalEntry>,
    knownSpans: Map<String, Pair<Int, Int>>,
    colsCells: Int,
    rowsCells: Int,
): Map<String, ProfilePlacement> {
    if (canonical.isEmpty() || colsCells <= 0 || rowsCells <= 0) return emptyMap()
    val colsU = colsCells * 2
    val rowsU = rowsCells * 2
    val out = LinkedHashMap<String, ProfilePlacement>()
    // Highest page already used, so oversize fresh pages never collide.
    var highPage = -1
    // Groups in canonical-page order; items inside a group stay in
    // canonical order. Each group starts on its canonical page and
    // scans forward for a free rect, so an earlier group's spill can
    // never overwrite a later group — it only pushes it further out.
    val groups = canonical.groupBy { it.page }.toSortedMap()
    for ((canonicalPage, entries) in groups) {
        var page = canonicalPage
        for (entry in entries) {
            val (spanW, spanH) = knownSpans[entry.id] ?: (2 to 2)
            val w = spanW.coerceIn(1, colsU)
            val h = spanH.coerceIn(1, rowsU)
            if (spanW > colsU || spanH > rowsU) {
                // Oversize: own fresh page, span kept (never resized away).
                page = highPage + 1
                out[entry.id] = ProfilePlacement(page, 0, 0, spanW, spanH)
                highPage = page
                continue
            }
            var spot = firstFreeSpotInProfile(out, page, w, h, colsU, rowsU)
            while (spot == null) {
                page++
                spot = firstFreeSpotInProfile(out, page, w, h, colsU, rowsU)
            }
            out[entry.id] = ProfilePlacement(page, spot.first, spot.second, w, h)
            if (page > highPage) highPage = page
        }
    }
    return out
}

/** Build canonical order from live items: page-major, then (row, col). */
fun canonicalOrderOf(items: List<HomeItem>): List<CanonicalEntry> =
    items.sortedWith(compareBy({ it.page }, { it.row }, { it.col }))
        .map { CanonicalEntry(it.id, it.page) }

/** Spans of live items, for [deriveProfile]. */
fun spansOf(items: List<HomeItem>): Map<String, Pair<Int, Int>> =
    items.associate { it.id to (it.colSpan to it.rowSpan) }

/**
 * Add an item to EVERY profile (Doc 1 §5.10): canonical gains the item
 * on [page] (default: last canonical page, or page 0 when empty), and
 * each existing profile gains it at its first free spot on that page
 * (spilling forward when full). Profiles that do not exist yet are
 * untouched — they derive from the updated canonical order on first
 * use, so the new item is included automatically.
 */
fun LayoutProfiles.withAppAdded(
    itemId: String,
    colSpan: Int,
    rowSpan: Int,
    page: Int? = null,
    gridCells: (WindowClass) -> Pair<Int, Int>,
): LayoutProfiles {
    if (itemId.isBlank() || canonical.any { it.id == itemId }) return this
    val targetPage = page ?: canonical.maxOfOrNull { it.page } ?: 0
    val newCanonical = canonical + CanonicalEntry(itemId, targetPage)
    val newProfiles = profiles.mapValues { (windowClass, placed) ->
        val (cols, rows) = gridCells(windowClass)
        val colsU = cols * 2
        val rowsU = rows * 2
        var p = targetPage
        var spot = firstFreeSpotInProfile(placed, p, colSpan, rowSpan, colsU, rowsU)
        while (spot == null) {
            p++
            spot = firstFreeSpotInProfile(placed, p, colSpan, rowSpan, colsU, rowsU)
            if (p > targetPage + 1000) break // corrupt grid guard; unreachable in practice
        }
        if (spot == null) placed else placed + (itemId to ProfilePlacement(p, spot.first, spot.second, colSpan, rowSpan))
    }
    return copy(canonical = newCanonical, profiles = newProfiles)
}

/** Remove an item from canonical order AND every profile. Never drops anything else. */
fun LayoutProfiles.withAppRemoved(itemId: String): LayoutProfiles =
    copy(
        canonical = canonical.filter { it.id != itemId },
        profiles = profiles.mapValues { (_, placed) -> placed.filterKeys { it != itemId } },
    )

/** One moved item in a "Sync layouts" preview. */
data class SyncPreviewEntry(
    val itemId: String,
    val windowClass: WindowClass,
    val old: ProfilePlacement?,
    val new: ProfilePlacement,
) {
    val moved: Boolean get() = old != new
}

/**
 * "Sync layouts" preview (Doc 1 §5.10): re-derive every profile except
 * [source] from the SOURCE profile's own order (its items in
 * page/row/col order), using each target's grid. Returns the preview
 * plus the profiles that applying it would produce; nothing is
 * committed by this call — the host shows the preview and calls
 * [syncLayoutsFrom] on Apply, or discards it on Cancel.
 */
fun syncLayoutsPreview(
    current: LayoutProfiles,
    source: WindowClass,
    gridCells: (WindowClass) -> Pair<Int, Int>,
): Pair<List<SyncPreviewEntry>, LayoutProfiles> {
    val sourceMap = current.profiles[source] ?: return emptyList<SyncPreviewEntry>() to current
    val sourceCanonical = sourceMap.entries
        .sortedWith(compareBy({ it.value.page }, { it.value.row }, { it.value.col }))
        .map { CanonicalEntry(it.key, it.value.page) }
    val sourceSpans = sourceMap.mapValues { (_, p) -> p.colSpan to p.rowSpan }
    val preview = mutableListOf<SyncPreviewEntry>()
    val newProfiles = current.profiles.toMutableMap()
    for ((windowClass, oldMap) in current.profiles) {
        if (windowClass == source) continue
        val (cols, rows) = gridCells(windowClass)
        val derived = deriveProfile(sourceCanonical, sourceSpans, cols, rows)
        for ((id, placement) in derived) {
            val old = oldMap[id]
            if (old != placement) preview += SyncPreviewEntry(id, windowClass, old, placement)
        }
        newProfiles[windowClass] = derived
    }
    val synced = current.copy(canonical = sourceCanonical, profiles = newProfiles)
    return preview to synced
}

/** Apply a previewed sync: returns the synced profiles (see [syncLayoutsPreview]). */
fun syncLayoutsFrom(
    current: LayoutProfiles,
    source: WindowClass,
    gridCells: (WindowClass) -> Pair<Int, Int>,
): LayoutProfiles = syncLayoutsPreview(current, source, gridCells).second

// ------------------------------------------------------------ persistence
// JSON form (never throws on either side; bad input -> empty profiles):
// {"canonical":[{"id":..,"page":..}],"profiles":{"COMPACT":{"id":{"page":..,"col":..,"row":..,"colSpan":..,"rowSpan":..}}}}

fun LayoutProfiles.toJson(): String {
    val root = JSONObject()
    val canonArr = JSONArray()
    for (e in canonical) canonArr.put(JSONObject().put("id", e.id).put("page", e.page))
    root.put("canonical", canonArr)
    val profObj = JSONObject()
    for ((windowClass, map) in profiles) {
        val mapObj = JSONObject()
        for ((id, p) in map) {
            mapObj.put(
                id,
                JSONObject().put("page", p.page).put("col", p.col).put("row", p.row)
                    .put("colSpan", p.colSpan).put("rowSpan", p.rowSpan),
            )
        }
        profObj.put(windowClass.name, mapObj)
    }
    root.put("profiles", profObj)
    return root.toString()
}

fun parseLayoutProfiles(json: String): LayoutProfiles {
    if (json.isBlank()) return LayoutProfiles()
    return try {
        val root = JSONObject(json)
        val canon = mutableListOf<CanonicalEntry>()
        val canonArr = root.optJSONArray("canonical")
        if (canonArr != null) {
            for (i in 0 until canonArr.length()) {
                val o = canonArr.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isNotBlank()) canon += CanonicalEntry(id, o.optInt("page", 0).coerceAtLeast(0))
            }
        }
        val profMap = mutableMapOf<WindowClass, Map<String, ProfilePlacement>>()
        val profObj = root.optJSONObject("profiles")
        if (profObj != null) {
            for (windowClass in WindowClass.entries) {
                val mapObj = profObj.optJSONObject(windowClass.name) ?: continue
                val map = mutableMapOf<String, ProfilePlacement>()
                for (key in mapObj.keys()) {
                    val o = mapObj.optJSONObject(key) ?: continue
                    map[key] = ProfilePlacement(
                        page = o.optInt("page", 0).coerceAtLeast(0),
                        col = o.optInt("col", 0).coerceAtLeast(0),
                        row = o.optInt("row", 0).coerceAtLeast(0),
                        colSpan = o.optInt("colSpan", 2).coerceAtLeast(1),
                        rowSpan = o.optInt("rowSpan", 2).coerceAtLeast(1),
                    )
                }
                profMap[windowClass] = map
            }
        }
        LayoutProfiles(canon, profMap)
    } catch (t: Throwable) {
        LayoutProfiles()
    }
}
