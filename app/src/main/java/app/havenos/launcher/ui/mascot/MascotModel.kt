package app.havenos.launcher.ui.mascot

/**
 * HavenOS v3 Track A — Mascot engine, pure logic (no Compose, no Android
 * framework). Every rule here is unit-testable on the JVM; the Compose
 * layer (MascotLayer.kt) only renders what this file decides.
 *
 * Spec: builder prompt §3/§4. Names live in string resources
 * (mascot_name_milo / mascot_name_mila) so renaming is a one-line change;
 * this file only carries the enum. NEVER Nova, NEVER Luna.
 */

enum class MascotCharacter { MILO, MILA }

/** Prompt §4.1 state machine. */
enum class MascotState { OFF, IDLE, ROAMING, REACTING, SLEEPING, GUIDING, DRAGGED, DISMISSED }

/** Prompt §3 expression set. */
enum class MascotExpression {
    IDLE, BLINK, HAPPY, CURIOUS, THINKING, SEARCHING,
    FOUND, CONFUSED, SURPRISED, CELEBRATE, SLEEPY, SAD,
}

/** Monochrome emote glyphs. Plain typographic characters only — never
 * emoji fonts (prompt §3). One emote at a time, ~1.2s, in the layer. */
fun emoteFor(expression: MascotExpression): String? = when (expression) {
    MascotExpression.HAPPY -> "!"
    MascotExpression.CURIOUS -> "?"
    MascotExpression.THINKING -> "..."
    MascotExpression.SEARCHING -> "?"
    // 3.1 Phase B4: FOUND/CELEBRATE were a check mark (U+2713,
    // emoji range); "!" / "!!" keep the positive read without one.
    MascotExpression.FOUND -> "!"
    MascotExpression.CONFUSED -> "?"
    MascotExpression.SURPRISED -> "!"
    MascotExpression.CELEBRATE -> "!!"
    MascotExpression.SLEEPY -> "zzz"
    MascotExpression.SAD -> "~"
    else -> null
}

/** Explicit transition table (prompt §4.1: transitions are explicit). */
fun MascotState.canTransitionTo(next: MascotState): Boolean {
    if (this == next) return true
    return when (this) {
        MascotState.OFF -> next == MascotState.IDLE
        MascotState.IDLE -> next in setOf(
            MascotState.ROAMING, MascotState.REACTING, MascotState.SLEEPING,
            MascotState.GUIDING, MascotState.DRAGGED, MascotState.DISMISSED,
            MascotState.OFF,
        )
        MascotState.ROAMING -> next in setOf(
            MascotState.IDLE, MascotState.REACTING, MascotState.SLEEPING,
            MascotState.GUIDING, MascotState.DRAGGED, MascotState.DISMISSED,
            MascotState.OFF,
        )
        MascotState.REACTING -> next in setOf(
            MascotState.IDLE, MascotState.GUIDING, MascotState.SLEEPING,
            MascotState.DISMISSED, MascotState.OFF,
        )
        MascotState.SLEEPING -> next in setOf(MascotState.IDLE, MascotState.DISMISSED, MascotState.OFF)
        MascotState.GUIDING -> next in setOf(
            MascotState.IDLE, MascotState.SLEEPING, MascotState.DISMISSED,
            MascotState.OFF,
        )
        MascotState.DRAGGED -> next in setOf(MascotState.IDLE, MascotState.OFF)
        MascotState.DISMISSED -> next in setOf(MascotState.IDLE, MascotState.OFF)
    }
}

/** Minutes of day in [start, end); wraps overnight (e.g. 22:00–07:00). */
fun isQuietHour(nowMin: Int, startMin: Int, endMin: Int): Boolean {
    val now = nowMin.mod(1440)
    val start = startMin.mod(1440)
    val end = endMin.mod(1440)
    if (start == end) return false
    return if (start < end) now in start until end else now >= start || now < end
}

/** Prompt §4.2 sleep rules, as pure inputs (the layer supplies the flags;
 * media/call detection without extra permissions is skipped and said so
 * in the status file — no new permission is ever requested for this). */
fun shouldSleep(
    userBusy: Boolean,
    quietNow: Boolean,
    homeVisible: Boolean,
    batterySaverOn: Boolean,
): Boolean = userBusy || quietNow || !homeVisible || batterySaverOn

/**
 * Roam planner (prompt §4.2): mostly still is the battery rule.
 * [stayRoll] in [0,1): stay put while < 0.65. Otherwise pick a perch
 * different from [current] via [pickRoll]. Returns the perch index.
 */
fun nextPerchIndex(current: Int, perchCount: Int, stayRoll: Float, pickRoll: Float): Int {
    if (perchCount <= 0) return 0
    if (perchCount == 1) return 0
    if (stayRoll < 0.65f) return current
    val span = perchCount - 1
    val pick = (pickRoll.coerceIn(0f, 0.999f) * span).toInt()
    return if (pick >= current) pick + 1 else pick
}

/**
 * Suggestion limiter (prompt §7.3): max one nudge per session, never the
 * same suggestion twice, global cooldown (default once per day), quiet
 * hours respected, never while the user is mid-gesture (the layer only
 * calls this while calm).
 */
fun canNudge(
    nudgesThisSession: Int,
    lastNudgeMs: Long,
    nowMs: Long,
    suggestionMuted: Boolean,
    quietNow: Boolean,
    cooldownMs: Long = 86_400_000L,
): Boolean {
    if (nudgesThisSession >= 1) return false
    if (suggestionMuted) return false
    if (quietNow) return false
    if (lastNudgeMs > 0L && nowMs - lastNudgeMs < cooldownMs) return false
    return true
}

/** Parse a JSON string-list (folder-style encoding); emptyList on error. */
fun parseMascotStringList(json: String): List<String> {
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    } catch (t: Throwable) {
        emptyList()
    }
}

/** Encode a string set as a JSON list. Sorted, so prefs diffs stay stable. */
fun encodeMascotStringList(values: Set<String>): String {
    val arr = org.json.JSONArray()
    values.sorted().forEach { arr.put(it) }
    return arr.toString()
}
