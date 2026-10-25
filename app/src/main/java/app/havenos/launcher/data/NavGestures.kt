package app.havenos.launcher.data

import org.json.JSONObject
import kotlin.math.abs

/** Default navigation-gesture map (trigger -> action) for the accessibility edge-swipe layer. */
const val DEFAULT_NAV_MAP = """{"up":"home","upHold":"recents","left":"back","right":"back"}"""

/** Triggers recognised by the navigation-gesture layer. */
val NAV_TRIGGERS = listOf("up", "upHold", "left", "right")

/** Actions a navigation gesture can fire, in picker order. */
val NAV_ACTIONS = listOf("home", "back", "recents", "quickPanel", "none")

/** Intent extra carrying the fired nav action to the launcher activity. */
const val EXTRA_NAV_ACTION = "havenos.nav_action"

/**
 * Parse a nav-map JSON with per-key fallback to [DEFAULT_NAV_MAP].
 * Unknown triggers are ignored; unknown actions fall back per key. Never throws.
 */
fun parseNavMap(json: String): Map<String, String> {
    val defaults = try {
        JSONObject(DEFAULT_NAV_MAP)
    } catch (t: Throwable) {
        JSONObject()
    }
    val parsed = try {
        JSONObject(json)
    } catch (t: Throwable) {
        JSONObject()
    }
    val out = LinkedHashMap<String, String>()
    for (trigger in NAV_TRIGGERS) {
        val fallback = defaults.optString(trigger, "none")
        val value = parsed.optString(trigger, fallback)
        out[trigger] = if (value in NAV_ACTIONS) value else fallback
    }
    return out
}

/** Screen edge a navigation swipe starts from. */
enum class NavEdge { BOTTOM, LEFT, RIGHT }

/**
 * Classify a swipe that started at [edge] into a nav trigger, or null when it
 * is too short or too diagonal. BOTTOM swipes travel up; LEFT-edge swipes
 * travel right (inward); RIGHT-edge swipes travel left (inward).
 */
fun classifySwipe(edge: NavEdge, dx: Float, dy: Float, thresholdPx: Float): String? = when (edge) {
    NavEdge.BOTTOM -> if (dy < -thresholdPx && abs(dy) > abs(dx) * 1.5f) "up" else null
    NavEdge.LEFT -> if (dx > thresholdPx && abs(dx) > abs(dy) * 1.5f) "left" else null
    NavEdge.RIGHT -> if (dx < -thresholdPx && abs(dx) > abs(dy) * 1.5f) "right" else null
}

/** Swipe distance needed to fire: sensitivity 0 -> 56dp, 100 -> 20dp, linear. */
fun navThresholdPx(sensitivity: Int, density: Float): Float {
    val s = sensitivity.coerceIn(0, 100)
    return (56f - 36f * s / 100f) * density
}
