package app.havenos.launcher.ui.island

import android.media.session.MediaController

/**
 * Dynamic Island activity model (v3 Track B, ORIGINAL code — behavior
 * studied from the reference island, nothing copied from it).
 *
 * An activity is something the launcher can already observe with NO
 * new permission: charging state (sticky battery broadcast), the
 * now-playing media session (via the existing notification-listener
 * component, so it only exists once the user granted that access for
 * badges / Now Playing), and an aggregate notification summary
 * (existing BadgeStore counts — totals and per-app counts only, never
 * title/body text, which HavenOS's listener does not keep).
 *
 * Deliberately NOT modelled: other apps' timers (unobservable without
 * new permissions), calls (would need phone permissions), and
 * per-notification actions/replies (the listener keeps counts only).
 * Those are reported gaps, not silent omissions.
 */

/** One app's share of the notification summary. Label comes from the app catalog. */
data class IslandAppCount(
    val pkg: String,
    val label: String,
    val count: Int,
)

sealed interface IslandActivity {
    /** Live media session. [controller] is null only in previews/tests. */
    data class Media(
        val title: String,
        val artist: String,
        val playing: Boolean,
        val controller: MediaController?,
    ) : IslandActivity

    /** Device is charging right now (plugged + BATTERY_STATUS_CHARGING). */
    data class Charging(val percent: Int) : IslandActivity

    /** Aggregate summary: total active notifications + per-app counts. */
    data class Notifications(
        val total: Int,
        val apps: List<IslandAppCount>,
    ) : IslandActivity
}

/** Display priority, highest first — same order as the reference. */
fun islandPriority(activity: IslandActivity): Int = when (activity) {
    is IslandActivity.Notifications -> 0
    is IslandActivity.Media -> 1
    is IslandActivity.Charging -> 2
}

/**
 * How long a notification burst stays a live island activity after the
 * count last increased. After the window lapses the pill falls back to
 * the next activity (or hides), so stale unread counts never keep the
 * island lit forever. A fresh count increase re-arms the window.
 */
const val NOTIFICATION_WINDOW_MS = 6_000L

/**
 * 3.1 A11 flow 15: island feeds → pill state, pure. Mirrors
 * rememberIslandActivities exactly: a notification burst is live
 * only while its summary exists (total > 0), the burst is newer
 * than the dismissed one, and inside the 6s window; priority is
 * Notification, then Media, then Charging ([islandPriority]);
 * nothing live → null (pill hidden). The feed keeps the full list
 * for the expanded view; the pill is this function's result.
 */
fun pickIslandActivity(
    notifications: IslandActivity.Notifications?,
    lastIncreaseMs: Long,
    dismissedBurstAt: Long,
    media: IslandActivity.Media?,
    charging: IslandActivity.Charging?,
    nowMs: Long,
): IslandActivity? {
    if (notifications != null && notifications.total > 0 &&
        lastIncreaseMs > dismissedBurstAt &&
        nowMs - lastIncreaseMs < NOTIFICATION_WINDOW_MS
    ) return notifications
    if (media != null) return media
    return charging
}
