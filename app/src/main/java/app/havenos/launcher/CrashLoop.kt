package app.havenos.launcher

/**
 * 3.1.1 (item 3): crash-loop pure helpers. No Android APIs, so these are
 * unit-testable: a loop is two or more crashes whose timestamps fall
 * within one window (default 60 seconds).
 */

/** True when any two consecutive markers fall within [windowMs]. */
fun isCrashLoop(markersMs: List<Long>, windowMs: Long = 60_000L): Boolean {
    val sorted = markersMs.sorted()
    for (i in 1 until sorted.size) {
        if (sorted[i] - sorted[i - 1] <= windowMs) return true
    }
    return false
}

/** Keep only the newest [keepLast] markers (file stays small). */
fun pruneCrashMarkers(markersMs: List<Long>, keepLast: Int = 20): List<Long> =
    markersMs.sorted().takeLast(keepLast)

/** One recorded crash for Diagnostics (class, message, time, versionCode). */
data class LastCrash(
    val clazz: String,
    val message: String,
    val timeText: String,
    val versionCode: Long,
)
