package app.havenos.launcher.data

import org.json.JSONObject

/** Gesture triggers recognised on the home pager. */
val GESTURE_TRIGGERS = listOf("swipeUp", "swipeDown", "doubleTap", "pinchIn", "twoFingerDown")

/** Actions a gesture can fire. */
val GESTURE_ACTIONS = setOf("drawer", "search", "quickPanel", "settings", "lock", "shade", "none")

/**
 * Parse a gestures JSON map with per-key fallback to [DEFAULT_GESTURES].
 * Unknown triggers are ignored; unknown actions fall back per key. Never throws.
 */
fun parseGestures(json: String): Map<String, String> {
    val defaults = try {
        JSONObject(DEFAULT_GESTURES)
    } catch (t: Throwable) {
        JSONObject()
    }
    val parsed = try {
        JSONObject(json)
    } catch (t: Throwable) {
        JSONObject()
    }
    val out = LinkedHashMap<String, String>()
    for (trigger in GESTURE_TRIGGERS) {
        val fallback = defaults.optString(trigger, "none")
        val value = parsed.optString(trigger, fallback)
        out[trigger] = if (value in GESTURE_ACTIONS) value else fallback
    }
    return out
}
