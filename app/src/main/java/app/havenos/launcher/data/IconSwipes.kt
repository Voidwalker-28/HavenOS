package app.havenos.launcher.data

import org.json.JSONObject

/**
 * Per-icon swipe-up actions (Smart-inspired behavior, original code).
 * Stored as package name -> action id in SettingsState.iconSwipesJson,
 * in the same stale-type-safe JSON-map style as perAppIconJson.
 * "none" is never stored: picking it deletes the entry, so a missing
 * entry and "none" mean the same thing — no swipe action.
 *
 * Only actions Home already dispatches (see Home.performGestureAction)
 * are offered: drawer, search, quickPanel (quick settings), settings,
 * shade (notifications), none. Lock is deliberately NOT offered here:
 * a swipe-up on an icon must never lock the device by accident.
 */
val ICON_SWIPE_ACTIONS = listOf("drawer", "search", "quickPanel", "settings", "shade", "none")

fun iconSwipeActionName(action: String): String = when (action) {
    "drawer" -> "Open drawer"
    "search" -> "Search"
    "quickPanel" -> "Quick settings"
    "settings" -> "Open settings"
    "shade" -> "Notifications"
    else -> "None"
}

/** Parse the icon-swipes map. Unknown actions and "none" entries are dropped. Never throws. */
fun parseIconSwipes(json: String): Map<String, String> = try {
    val obj = JSONObject(json)
    val out = LinkedHashMap<String, String>()
    for (k in obj.keys()) {
        val v = obj.optString(k, "")
        if (v in ICON_SWIPE_ACTIONS && v != "none" && k.isNotBlank()) out[k] = v
    }
    out
} catch (t: Throwable) {
    emptyMap()
}

fun encodeIconSwipes(swipes: Map<String, String>): String {
    val obj = JSONObject()
    for ((k, v) in swipes) {
        if (k.isNotBlank() && v in ICON_SWIPE_ACTIONS && v != "none") obj.put(k, v)
    }
    return obj.toString()
}

/**
 * WIRING HOOK (for the coordinator / Home + PagedHome worker — do NOT
 * call this from Settings UI): in an icon's swipe-up handler, resolve
 * the action for that icon's package and, when non-null, dispatch it
 * through the same dispatcher home gestures use:
 *
 *     val action = swipeActionFor(state, entry.pkg)
 *     if (action != null) performGestureAction(action) // tap still launches
 *
 * Returns null when the package has no swipe action (tap behavior unchanged).
 */
fun swipeActionFor(swipesJson: String, packageName: String): String? =
    parseIconSwipes(swipesJson)[packageName]

fun swipeActionFor(state: SettingsState, packageName: String): String? =
    swipeActionFor(state.iconSwipesJson, packageName)
