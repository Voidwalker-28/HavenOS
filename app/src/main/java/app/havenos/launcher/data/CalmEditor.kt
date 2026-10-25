package app.havenos.launcher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Worker D (M9) — Calm editor store, Doc 1 §5.9.
 *
 * Replaces the M8 "Choose apps" placeholder with a real editor:
 * pick up to 8 favorites, drag reorder, schedule (days/times), entry
 * options. Calm NEVER changes the layout — this store only writes the
 * Calm settings keys.
 *
 * Storage (existing keys, unchanged format — data must not import ui,
 * so the encode/decode here duplicates the CalmMode.kt JSON shape
 * deliberately, field for field):
 * - calmFavoritesJson: JSON array of app keys (["pkg/cls#serial", ...]).
 * - calmScheduleJson: {"enabled":bool,"days":[1..7],"start":min,"end":min}
 *   (days use Calendar.DAY_OF_WEEK: SUNDAY=1..SATURDAY=7; end<=start
 *   wraps past midnight; empty days never activates).
 * - calmEntryJson (NEW M9 key, default ""): {"autoEnter":bool,
 *   "dimPct":0..100}. autoEnter=false means manual entry only even
 *   when a schedule exists; dimPct overrides the derived Calm dim
 *   (CalmDimAlpha in LayoutCoordination.kt) when set (>0).
 *
 * Worker A owns the toolbar Calm button; this file + CalmEditorScreen
 * (ui/) own the editor screen and this store. The coordinator persists
 * via the existing setCalmFavoritesJson / setCalmScheduleJson setters
 * and the new setCalmEntryJson setter.
 */

/** Maximum Calm favorites (Doc 1 §5.9). */
const val CALM_EDITOR_MAX: Int = 8

/** Entry options for Calm mode. */
data class CalmEntryOptions(
    val autoEnter: Boolean = true,
    /** 0 = derived dim; otherwise an explicit dim percent 1..100. */
    val dimPct: Int = 0,
)

/** Whole editor state: favorites in order + schedule + entry options. */
data class CalmEditorState(
    val favorites: List<String> = emptyList(),
    val scheduleEnabled: Boolean = false,
    val days: Set<Int> = emptySet(),
    val startMinutes: Int = 540, // 09:00
    val endMinutes: Int = 1020, // 17:00
    val entry: CalmEntryOptions = CalmEntryOptions(),
) {
    val isFull: Boolean get() = favorites.size >= CALM_EDITOR_MAX
}

/** Schedule active check (same rule as CalmMode.kt's CalmSchedule.isActiveAt). */
fun CalmEditorState.isScheduleActiveAt(dayOfWeek: Int, minuteOfDay: Int): Boolean {
    if (!scheduleEnabled || days.isEmpty()) return false
    if (dayOfWeek !in days) return false
    if (startMinutes == endMinutes) return false
    return if (startMinutes < endMinutes) {
        minuteOfDay in startMinutes until endMinutes
    } else {
        minuteOfDay >= startMinutes || minuteOfDay < endMinutes
    }
}

/** Validation failures, in user-facing short form. Empty = valid. */
fun CalmEditorState.validate(): List<String> {
    val errors = mutableListOf<String>()
    if (favorites.size > CALM_EDITOR_MAX) errors += "Pick up to 8 favorites"
    if (favorites.distinct().size != favorites.size) errors += "Favorites must not repeat"
    if (favorites.any { it.isBlank() }) errors += "Favorites must not be blank"
    if (days.any { it !in 1..7 }) errors += "Schedule days must be Sunday–Saturday"
    if (startMinutes !in 0..1439 || endMinutes !in 0..1439) errors += "Schedule times must be within the day"
    if (entry.dimPct !in 0..100) errors += "Dim must be 0–100"
    return errors
}

fun CalmEditorState.isValid(): Boolean = validate().isEmpty()

/** Add a favorite: ignored when full, blank, or already present. */
fun CalmEditorState.withAdded(key: String): CalmEditorState {
    if (key.isBlank() || key in favorites || isFull) return this
    return copy(favorites = favorites + key)
}

/** Remove a favorite (no-op when absent). */
fun CalmEditorState.withRemoved(key: String): CalmEditorState =
    copy(favorites = favorites.filter { it != key })

/** Reorder: move the favorite at [from] to [to] (both clamped; no-op when out of range). */
fun CalmEditorState.withMoved(from: Int, to: Int): CalmEditorState {
    if (from !in favorites.indices || to !in favorites.indices || from == to) return this
    val list = favorites.toMutableList()
    val item = list.removeAt(from)
    list.add(to, item)
    return copy(favorites = list)
}

/** Toggle one schedule day. */
fun CalmEditorState.withDayToggled(day: Int): CalmEditorState {
    if (day !in 1..7) return this
    return copy(days = if (day in days) days - day else days + day)
}

// ------------------------------------------------------------ persistence

fun CalmEditorState.encodeFavorites(): String {
    val arr = JSONArray()
    for (k in favorites.take(CALM_EDITOR_MAX)) arr.put(k)
    return arr.toString()
}

fun parseCalmEditorFavorites(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "")
            if (v.isNotBlank() && v !in out) out += v
        }
        out.take(CALM_EDITOR_MAX)
    } catch (t: Throwable) {
        emptyList()
    }
}

fun CalmEditorState.encodeSchedule(): String {
    val root = JSONObject()
    root.put("enabled", scheduleEnabled)
    val arr = JSONArray()
    for (d in days.sorted()) arr.put(d)
    root.put("days", arr)
    root.put("start", startMinutes)
    root.put("end", endMinutes)
    return root.toString()
}

fun parseCalmEditorSchedule(json: String): CalmEditorState {
    if (json.isBlank()) return CalmEditorState()
    return try {
        val root = JSONObject(json)
        val days = mutableSetOf<Int>()
        val arr = root.optJSONArray("days")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val d = arr.optInt(i, -1)
                if (d in 1..7) days += d
            }
        }
        CalmEditorState(
            scheduleEnabled = root.optBoolean("enabled", false),
            days = days,
            startMinutes = root.optInt("start", 540).coerceIn(0, 1439),
            endMinutes = root.optInt("end", 1020).coerceIn(0, 1439),
        )
    } catch (t: Throwable) {
        CalmEditorState()
    }
}

fun CalmEntryOptions.encode(): String {
    val root = JSONObject()
    root.put("autoEnter", autoEnter)
    root.put("dimPct", dimPct.coerceIn(0, 100))
    return root.toString()
}

fun parseCalmEntryOptions(json: String): CalmEntryOptions {
    if (json.isBlank()) return CalmEntryOptions()
    return try {
        val root = JSONObject(json)
        CalmEntryOptions(
            autoEnter = root.optBoolean("autoEnter", true),
            dimPct = root.optInt("dimPct", 0).coerceIn(0, 100),
        )
    } catch (t: Throwable) {
        CalmEntryOptions()
    }
}

/** Decode the full editor state from the three stored keys. */
fun calmEditorStateFromKeys(favoritesJson: String, scheduleJson: String, entryJson: String): CalmEditorState {
    val schedule = parseCalmEditorSchedule(scheduleJson)
    return schedule.copy(
        favorites = parseCalmEditorFavorites(favoritesJson),
        entry = parseCalmEntryOptions(entryJson),
    )
}
